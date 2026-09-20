/**
 * KnowToMigrate - TCP Chunk Transfer Client (Sender Engine)
 */

import * as net from 'node:net';
import { EventEmitter } from 'node:events';
import { PacketCodec, StreamPacketDecoder } from '../protocol/framing.js';
import { ChunkerEngine } from '../transfer/chunker.js';
import { CryptoEngine } from '../crypto/crypto.js';
import {
  MessageType,
  HelloPayload,
  PreflightReqPayload,
  PreflightRespPayload,
  FileItemManifest,
  ChunkDataHeader,
  TransferProgress
} from '@knowtomigrate/protocol-types';

export class TransferClient extends EventEmitter {
  private socket: net.Socket | null = null;
  private isPaused: boolean = false;
  private isCancelled: boolean = false;
  private sessionId: string;
  private currentSeq: number = 1;

  constructor() {
    super();
    this.sessionId = CryptoEngine.generateSessionId();
  }

  public pause(): void {
    this.isPaused = true;
    this.emit('paused');
  }

  public resume(): void {
    this.isPaused = false;
    this.emit('resumed');
  }

  public cancel(): void {
    this.isCancelled = true;
    if (this.socket) {
      this.socket.destroy();
      this.socket = null;
    }
    this.emit('cancelled');
  }

  public async sendFiles(
    targetHost: string,
    targetPort: number,
    manifests: FileItemManifest[]
  ): Promise<string> {
    return new Promise((resolve, reject) => {
      this.socket = net.createConnection({ host: targetHost, port: targetPort }, async () => {
        try {
          const merkleRoot = await this.executeTransferPipeline(manifests);
          resolve(merkleRoot);
        } catch (err) {
          reject(err);
        }
      });

      this.socket.on('error', (err) => {
        this.emit('error', err);
        reject(err);
      });
    });
  }

  private async executeTransferPipeline(manifests: FileItemManifest[]): Promise<string> {
    const socket = this.socket!;
    const decoder = new StreamPacketDecoder();

    // Helper for request-response over socket
    const sendAndExpect = (packet: Buffer, expectedType: MessageType): Promise<Buffer> => {
      return new Promise((resolve, reject) => {
        const onData = (data: Buffer) => {
          try {
            const packets = decoder.feed(data);
            for (const p of packets) {
              if (p.header.type === expectedType) {
                socket.off('data', onData);
                return resolve(p.payload);
              }
            }
          } catch (e) {
            socket.off('data', onData);
            reject(e);
          }
        };
        socket.on('data', onData);
        socket.write(packet);
      });
    };

    // 1. HELLO Handshake
    const hello: HelloPayload = {
      protocolVersion: 2,
      deviceId: 'sender-device-id',
      deviceName: 'KnowToMigrate Sender',
      platform: 'desktop',
      ephemeralPublicKey: 'key_placeholder',
      preferredChunkSize: 8 * 1024 * 1024,
      capabilities: ['chunking_v2', 'sha256_merkle']
    };
    const helloPacket = PacketCodec.encodePacket(
      MessageType.HELLO,
      this.currentSeq++,
      this.sessionId,
      Buffer.from(JSON.stringify(hello), 'utf-8')
    );
    await sendAndExpect(helloPacket, MessageType.HELLO_ACK);

    // 2. PREFLIGHT
    const totalBytes = manifests.reduce((sum, f) => sum + f.size, 0);
    const preflight: PreflightReqPayload = {
      sessionId: this.sessionId,
      fileCount: manifests.length,
      totalBytes,
      isMigration: false,
      folderStructurePreserved: true,
      manifestHash: CryptoEngine.sha256(JSON.stringify(manifests))
    };
    const preflightPacket = PacketCodec.encodePacket(
      MessageType.PREFLIGHT_REQ,
      this.currentSeq++,
      this.sessionId,
      Buffer.from(JSON.stringify(preflight), 'utf-8')
    );
    const preflightRespRaw = await sendAndExpect(preflightPacket, MessageType.PREFLIGHT_RESP);
    const preflightResp: PreflightRespPayload = JSON.parse(preflightRespRaw.toString('utf-8'));

    if (!preflightResp.accepted) {
      throw new Error(`Transfer rejected by recipient: ${preflightResp.rejectionReason}`);
    }

    // 3. MANIFEST HEADER
    const manifestPacket = PacketCodec.encodePacket(
      MessageType.MANIFEST_HEADER,
      this.currentSeq++,
      this.sessionId,
      Buffer.from(JSON.stringify(manifests), 'utf-8')
    );
    socket.write(manifestPacket);

    // 4. STREAM CHUNKS WITH BACKPRESSURE & PROGRESS MONITORING
    let totalTransferred = 0;
    const startTime = Date.now();
    let lastTime = startTime;
    let lastTransferred = 0;
    const chunkHashes: string[] = [];

    for (let fIdx = 0; fIdx < manifests.length; fIdx++) {
      const fileItem = manifests[fIdx];
      if (!fileItem.absolutePath) continue;

      for (let cIdx = 0; cIdx < fileItem.chunks.length; cIdx++) {
        if (this.isCancelled) throw new Error('Transfer cancelled');

        // Handle pause state
        while (this.isPaused && !this.isCancelled) {
          await new Promise(r => setTimeout(r, 200));
        }

        const chunk = fileItem.chunks[cIdx];
        const { data: chunkData, sha256 } = await ChunkerEngine.readChunk(
          fileItem.absolutePath,
          chunk.offset,
          chunk.length
        );

        chunkHashes.push(sha256);

        // Build CHUNK_DATA payload
        const meta: ChunkDataHeader = {
          sessionId: this.sessionId,
          fileIndex: fIdx,
          chunkIndex: cIdx,
          offset: chunk.offset,
          length: chunk.length,
          sha256
        };
        const metaJson = Buffer.from(JSON.stringify(meta), 'utf-8');
        const headerLenBuf = Buffer.alloc(2);
        headerLenBuf.writeUInt16BE(metaJson.length, 0);

        const chunkPayload = Buffer.concat([headerLenBuf, metaJson, chunkData]);
        const chunkPacket = PacketCodec.encodePacket(
          MessageType.CHUNK_DATA,
          this.currentSeq++,
          this.sessionId,
          chunkPayload
        );

        // Send chunk and await ACK
        await sendAndExpect(chunkPacket, MessageType.CHUNK_ACK);

        totalTransferred += chunk.length;
        const now = Date.now();

        // Calculate metrics
        const elapsedSec = (now - startTime) / 1000 || 0.001;
        const intervalSec = (now - lastTime) / 1000 || 0.001;
        const currentSpeed = (totalTransferred - lastTransferred) / intervalSec;
        const speedMbps = (currentSpeed * 8) / (1024 * 1024);
        const remainingBytes = Math.max(0, totalBytes - totalTransferred);
        const etaSeconds = currentSpeed > 0 ? Math.round(remainingBytes / currentSpeed) : 0;

        lastTime = now;
        lastTransferred = totalTransferred;

        const progress: TransferProgress = {
          sessionId: this.sessionId,
          state: 'streaming',
          transport: 'direct_lan',
          currentFileIndex: fIdx,
          currentFileName: fileItem.relativePath,
          currentFileProgress: (cIdx + 1) / fileItem.chunks.length,
          totalFiles: manifests.length,
          completedFiles: fIdx,
          totalBytes,
          transferredBytes: totalTransferred,
          speedBytesPerSec: currentSpeed,
          speedMbps: Math.round(speedMbps * 10) / 10,
          etaSeconds,
          chunksTotal: manifests.reduce((acc, m) => acc + m.chunks.length, 0),
          chunksVerified: chunkHashes.length,
          failedChunks: 0
        };

        this.emit('progress', progress);
      }
    }

    // 5. VERIFY COMPLETE
    const verifyPacket = PacketCodec.encodePacket(
      MessageType.TRANSFER_VERIFY,
      this.currentSeq++,
      this.sessionId,
      Buffer.alloc(0)
    );
    const completeRaw = await sendAndExpect(verifyPacket, MessageType.TRANSFER_COMPLETE);
    const completeJson = JSON.parse(completeRaw.toString('utf-8'));

    return completeJson.merkleRootHash;
  }
}
