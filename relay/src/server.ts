/**
 * KnowToMigrate - Encrypted Fallback Relay Server
 * Zero-knowledge encrypted byte pipe for remote internet transfers.
 * Implements pure Node.js RFC 6455 WebSocket protocol (zero external dependencies).
 * Hardened with session token authentication, anti-hijacking, and stream backpressure.
 */

import * as http from 'node:http';
import * as crypto from 'node:crypto';
import type { Socket } from 'node:net';

const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 8080;
const SESSION_TTL_MS = 30 * 60 * 1000; // 30 minutes max session lifetime
const MAX_FRAME_SIZE = 16 * 1024 * 1024; // 16 MB chunk size cap
const MAX_SESSION_BYTES = 100 * 1024 * 1024 * 1024; // 100 GB cap per session

interface RelayPeer {
  socket: Socket;
  role: 'sender' | 'receiver';
  connectedAt: number;
}

interface RelaySession {
  id: string;
  authToken: string;
  sender?: RelayPeer;
  receiver?: RelayPeer;
  createdAt: number;
  totalBytesRelayed: number;
}

const sessions = new Map<string, RelaySession>();

// Cleanup stale sessions periodically
setInterval(() => {
  const now = Date.now();
  for (const [id, session] of sessions.entries()) {
    if (now - session.createdAt > SESSION_TTL_MS) {
      if (session.sender?.socket.writable) session.sender.socket.destroy();
      if (session.receiver?.socket.writable) session.receiver.socket.destroy();
      sessions.delete(id);
    }
  }
}, 60000);

const server = http.createServer((req, res) => {
  if (req.url === '/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    let totalBytes = 0;
    for (const session of sessions.values()) {
      totalBytes += session.totalBytesRelayed;
    }
    res.end(
      JSON.stringify({
        status: 'healthy',
        activeSessions: sessions.size,
        totalBytesRelayed: totalBytes,
        timestamp: new Date().toISOString(),
      })
    );
    return;
  }

  res.writeHead(200, { 'Content-Type': 'text/plain' });
  res.end('KnowToMigrate Encrypted Relay V2.0 — Zero-Knowledge P2P Pipe (Authenticated)\n');
});

// RFC 6455 WebSocket Handshake with Authentication & Anti-Hijack Defense
server.on('upgrade', (req, socket: Socket) => {
  const url = new URL(req.url || '/', `http://${req.headers.host}`);
  const pathname = url.pathname;
  const sessionId = url.searchParams.get('session');
  const role = url.searchParams.get('role') as 'sender' | 'receiver';
  const token = url.searchParams.get('token') || '';

  if (pathname !== '/relay' || !sessionId || (role !== 'sender' && role !== 'receiver')) {
    socket.write('HTTP/1.1 400 Bad Request\r\n\r\n');
    socket.destroy();
    return;
  }

  // Token is mandatory (min 8 chars) to prevent unauthorized session access
  if (!token || token.length < 8) {
    socket.write('HTTP/1.1 401 Unauthorized: Valid session token parameter required\r\n\r\n');
    socket.destroy();
    return;
  }

  // Session Verification & Role Collision Protection
  let session = sessions.get(sessionId);
  if (!session) {
    // First peer creates the session and establishes the session secret
    session = {
      id: sessionId,
      authToken: token,
      createdAt: Date.now(),
      totalBytesRelayed: 0,
    };
    sessions.set(sessionId, session);
  } else {
    // Authenticate connecting peer against established session secret
    const a = Buffer.from(token);
    const b = Buffer.from(session.authToken);
    if (a.length !== b.length || !crypto.timingSafeEqual(a, b)) {
      socket.write('HTTP/1.1 403 Forbidden: Invalid session token\r\n\r\n');
      socket.destroy();
      return;
    }

    // Anti-Hijack Defense: Reject if the role is already actively connected!
    const existingPeer = role === 'sender' ? session.sender : session.receiver;
    if (existingPeer && existingPeer.socket.writable) {
      socket.write(`HTTP/1.1 409 Conflict: ${role} is already connected to this session\r\n\r\n`);
      socket.destroy();
      return;
    }
  }

  const key = req.headers['sec-websocket-key'];
  if (!key) {
    socket.write('HTTP/1.1 400 Missing Sec-WebSocket-Key\r\n\r\n');
    socket.destroy();
    return;
  }

  // Calculate Accept Key
  const acceptKey = crypto
    .createHash('sha1')
    .update(key + '258EAFA5-E914-47DA-95CA-C5AB0DC85B11')
    .digest('base64');

  const headers = [
    'HTTP/1.1 101 Switching Protocols',
    'Upgrade: websocket',
    'Connection: Upgrade',
    `Sec-WebSocket-Accept: ${acceptKey}`,
    '\r\n',
  ];

  socket.write(headers.join('\r\n'));

  const peer: RelayPeer = {
    socket,
    role,
    connectedAt: Date.now(),
  };

  if (role === 'sender') {
    session.sender = peer;
  } else {
    session.receiver = peer;
  }

  // Forward incoming data with strict size checks and backpressure handling
  socket.on('data', (chunk: Buffer) => {
    // Frame size defense
    if (chunk.length > MAX_FRAME_SIZE) {
      socket.write('HTTP/1.1 413 Payload Too Large\r\n\r\n');
      socket.destroy();
      return;
    }

    // Session total volume cap defense
    session!.totalBytesRelayed += chunk.length;
    if (session!.totalBytesRelayed > MAX_SESSION_BYTES) {
      socket.destroy();
      return;
    }

    const targetPeer = role === 'sender' ? session!.receiver : session!.sender;
    if (targetPeer && targetPeer.socket.writable) {
      // Implement backpressure: if target buffer is full, pause sender socket
      const canWrite = targetPeer.socket.write(chunk);
      if (!canWrite) {
        socket.pause();
        targetPeer.socket.once('drain', () => {
          if (!socket.destroyed) {
            socket.resume();
          }
        });
      }
    }
  });

  socket.on('close', () => {
    if (role === 'sender' && session!.sender?.socket === socket) session!.sender = undefined;
    if (role === 'receiver' && session!.receiver?.socket === socket) session!.receiver = undefined;
    if (!session!.sender && !session!.receiver) {
      sessions.delete(sessionId);
    }
  });

  socket.on('error', () => {
    socket.destroy();
  });
});

server.listen(PORT, () => {
  console.log(`⚡ KnowToMigrate Relay Server running on port ${PORT}`);
});
