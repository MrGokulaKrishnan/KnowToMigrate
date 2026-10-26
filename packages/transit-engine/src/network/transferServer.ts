/**
 * KnowToMigrate - TCP Chunk Transfer Server (Receiver Engine)
 */

import * as net from 'node:net';
import { EventEmitter } from 'node:events';
import { PacketCodec, StreamPacketDecoder } from '../protocol/framing.js';
import { ChunkerEngine } from '../transfer/chunker.js';
import { CryptoEngine } from '../crypto/crypto.js';
import { PathGuard } from '../storage/pathGuard.js';
import { CheckpointManager } from '../storage/checkpoint.js';
import {
  MessageType,
  HelloPayload,
  PreflightReqPayload,
  PreflightRespPayload,
  ChunkDataHeader,
  ChunkAckPayload,
  FileItemManifest,
  TransferProgress
} from '@knowtomigrate/protocol-types';

export class TransferServer extends EventEmitter {
  private server: net.Server | null = null;
  private port: number;
  private saveDirectory: string;
  private activeManifests: Map<string, FileItemManifest[]> = new Map();
  private verifiedChunkHashes: Map<string, string[]> = new Map(); // sessionId -> array of chunk hashes
  private sockets: Set<net.Socket> = new Set();

  constructor(port: number = 54124, saveDirectory: string = './downloads') {
    super();
    this.port = port;
    this.saveDirectory = saveDirectory;
  }

  public async start(): Promise<number> {
    return new Promise((resolve, reject) => {
      this.server = net.createServer((socket) => {
        this.sockets.add(socket);
        socket.on('close', () => this.sockets.delete(socket));
        this.handleClientConnection(socket);
      });

      this.server.on('error', (err) => {
        this.emit('error', err);
        reject(err);
      });

      this.server.listen(this.port, () => {
        const address = this.server!.address() as net.AddressInfo;
        this.port = address.port;
        resolve(this.port);
      });
    });
  }

  public stop(): void {
    for (const socket of this.sockets) {
      socket.destroy();
    }
    this.sockets.clear();
    if (this.server) {
      this.server.close();
      this.server = null;
    }
  }

  private handleClientConnection(socket: net.Socket): void {
    const decoder = new StreamPacketDecoder();

    socket.on('data', async (chunk) => {
      try {
        const packets = decoder.feed(chunk);
        for (const packet of packets) {
          await this.processPacket(socket, packet.header, packet.payload);
        }
      } catch (err: any) {
        this.emit('error', err);
        socket.destroy();
      }
    });

    socket.on('error', () => {
      // Handled on disconnect
    });
  }

  private async processPacket(socket: net.Socket, header: any, payload: Buffer): Promise<void> {
    switch (header.type) {
      case MessageType.HELLO: {
        const hello: HelloPayload = JSON.parse(payload.toString('utf-8'));
        this.emit('handshake', hello);

        // Respond with HELLO_ACK
        const ackPayload = Buffer.from(JSON.stringify({
          status: 'ready',
          serverVersion: 2
        }), 'utf-8');

        const packet = PacketCodec.encodePacket(MessageType.HELLO_ACK, header.seq + 1, header.sessionId, ackPayload);
        socket.write(packet);
        break;
      }

      case MessageType.PREFLIGHT_REQ: {
        const preflight: PreflightReqPayload = JSON.parse(payload.toString('utf-8'));
        this.emit('incomingTransferRequest', {
          sessionId: preflight.sessionId,
          fileCount: preflight.fileCount,
          totalBytes: preflight.totalBytes,
          manifestHash: preflight.manifestHash
        });

        // Auto-approve or trigger approval event (Default: accept)
        const resp: PreflightRespPayload = {
          sessionId: preflight.sessionId,
          accepted: true,
          destinationAvailableBytes: 100 * 1024 * 1024 * 1024, // 100 GB placeholder
          savePath: this.saveDirectory
        };

        const respBuffer = Buffer.from(JSON.stringify(resp), 'utf-8');
        const packet = PacketCodec.encodePacket(MessageType.PREFLIGHT_RESP, header.seq + 1, header.sessionId, respBuffer);
        socket.write(packet);
        break;
      }

      case MessageType.MANIFEST_HEADER: {
        const manifest: FileItemManifest[] = JSON.parse(payload.toString('utf-8'));
        this.activeManifests.set(header.sessionId, manifest);
        this.verifiedChunkHashes.set(header.sessionId, []);
        this.emit('manifestReceived', { sessionId: header.sessionId, manifest });
        break;
      }

      case MessageType.CHUNK_DATA: {
        // Chunk format: [JSON Header (256 bytes fixed or dynamic length)] + [Binary raw chunk]
        const headerLen = payload.readUInt16BE(0);
        const chunkMetaJson = payload.toString('utf-8', 2, 2 + headerLen);
        const meta: ChunkDataHeader = JSON.parse(chunkMetaJson);
        const rawChunkData = payload.subarray(2 + headerLen);

        // Resolve safe target path using PathGuard
        const manifests = this.activeManifests.get(header.sessionId) || [];
        const fileItem = manifests[meta.fileIndex];
        const safeRel = fileItem ? fileItem.relativePath : `file_${meta.fileIndex}`;
        const targetPath = PathGuard.resolveSafeDestination(this.saveDirectory, safeRel);

        // Write and verify chunk
        await ChunkerEngine.writeChunk(targetPath, meta.offset, rawChunkData, meta.sha256);

        // Record verified chunk hash for Merkle tree
        const hashes = this.verifiedChunkHashes.get(header.sessionId) || [];
        hashes.push(meta.sha256);
        this.verifiedChunkHashes.set(header.sessionId, hashes);

        // Emit progress
        this.emit('chunkReceived', {
          sessionId: header.sessionId,
          fileIndex: meta.fileIndex,
          chunkIndex: meta.chunkIndex,
          bytes: rawChunkData.length
        });

        // Send CHUNK_ACK
        const ack: ChunkAckPayload = {
          sessionId: header.sessionId,
          fileIndex: meta.fileIndex,
          chunkIndex: meta.chunkIndex,
          verified: true
        };
        const ackBuffer = Buffer.from(JSON.stringify(ack), 'utf-8');
        const ackPacket = PacketCodec.encodePacket(MessageType.CHUNK_ACK, header.seq + 1, header.sessionId, ackBuffer);
        socket.write(ackPacket);
        break;
      }

      case MessageType.TRANSFER_VERIFY: {
        const chunkHashes = this.verifiedChunkHashes.get(header.sessionId) || [];
        const merkleRoot = CryptoEngine.computeMerkleRoot(chunkHashes);

        const completePayload = Buffer.from(JSON.stringify({
          verified: true,
          merkleRootHash: merkleRoot
        }), 'utf-8');

        const completePacket = PacketCodec.encodePacket(MessageType.TRANSFER_COMPLETE, header.seq + 1, header.sessionId, completePayload);
        socket.write(completePacket);

        // Cleanup temporary checkpoints
        await CheckpointManager.cleanupCheckpoint(this.saveDirectory);
        this.emit('transferComplete', { sessionId: header.sessionId, merkleRoot });
        break;
      }
    }
  }
}
