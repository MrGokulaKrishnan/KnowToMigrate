/**
 * KnowToMigrate - Encrypted Fallback Relay Server
 * Zero-knowledge encrypted byte pipe for remote internet transfers.
 * Implements pure Node.js RFC 6455 WebSocket protocol (zero external dependencies).
 */

import * as http from 'node:http';
import * as crypto from 'node:crypto';
import type { Socket } from 'node:net';

const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 8080;
const SESSION_TTL_MS = 15 * 60 * 1000; // 15 minutes max session lifetime

interface RelayPeer {
  socket: Socket;
  role: 'sender' | 'receiver';
  connectedAt: number;
}

interface RelaySession {
  id: string;
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
  res.end('KnowToMigrate Encrypted Relay V2.0 — Zero-Knowledge P2P Pipe\n');
});

// RFC 6455 WebSocket Handshake
server.on('upgrade', (req, socket: Socket) => {
  const url = new URL(req.url || '/', `http://${req.headers.host}`);
  const pathname = url.pathname;
  const sessionId = url.searchParams.get('session');
  const role = url.searchParams.get('role') as 'sender' | 'receiver';

  if (pathname !== '/relay' || !sessionId || (role !== 'sender' && role !== 'receiver')) {
    socket.write('HTTP/1.1 400 Bad Request\r\n\r\n');
    socket.destroy();
    return;
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

  // Register in Session Room
  let session = sessions.get(sessionId);
  if (!session) {
    session = {
      id: sessionId,
      createdAt: Date.now(),
      totalBytesRelayed: 0,
    };
    sessions.set(sessionId, session);
  }

  const peer: RelayPeer = {
    socket,
    role,
    connectedAt: Date.now(),
  };

  if (role === 'sender') {
    if (session.sender?.socket.writable) session.sender.socket.destroy();
    session.sender = peer;
  } else {
    if (session.receiver?.socket.writable) session.receiver.socket.destroy();
    session.receiver = peer;
  }

  // Forward incoming data directly to opposing peer (zero-knowledge)
  socket.on('data', (chunk: Buffer) => {
    session!.totalBytesRelayed += chunk.length;
    const targetPeer = role === 'sender' ? session!.receiver : session!.sender;
    if (targetPeer && targetPeer.socket.writable) {
      targetPeer.socket.write(chunk);
    }
  });

  socket.on('close', () => {
    if (role === 'sender') session!.sender = undefined;
    if (role === 'receiver') session!.receiver = undefined;
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
