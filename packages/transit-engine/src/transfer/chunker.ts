/**
 * KnowToMigrate - High-Throughput Chunking Engine
 * Streams files in 8MB chunks with backpressure and positional disk I/O.
 */

import * as fs from 'node:fs/promises';
import { constants } from 'node:fs';
import * as path from 'node:path';
import { CryptoEngine } from '../crypto/crypto.js';
import { ChunkInfo, FileItemManifest, DEFAULT_CHUNK_SIZE } from '@knowtomigrate/protocol-types';

export class ChunkerEngine {
  /**
   * Scans a file and creates its chunk manifest without loading content into RAM
   */
  public static async inspectFileForManifest(
    absolutePath: string,
    relativePath: string,
    fileIndex: number,
    chunkSize: number = DEFAULT_CHUNK_SIZE
  ): Promise<FileItemManifest> {
    const stat = await fs.stat(absolutePath);
    const totalSize = stat.size;
    const chunkCount = Math.ceil(totalSize / chunkSize) || 1;
    const chunks: ChunkInfo[] = [];

    let offset = 0;
    for (let i = 0; i < chunkCount; i++) {
      const length = Math.min(chunkSize, totalSize - offset);
      chunks.push({
        index: i,
        offset,
        length,
        sha256: '', // Computed lazily or during streaming
        verified: false,
        attempts: 0
      });
      offset += length;
    }

    return {
      index: fileIndex,
      relativePath,
      absolutePath,
      size: totalSize,
      sha256: '', // Computed when all chunks are ready or verified
      isDir: stat.isDirectory(),
      mtime: stat.mtimeMs,
      mode: stat.mode,
      chunks,
      status: 'pending'
    };
  }

  /**
   * Reads a single chunk directly from disk at the given byte offset.
   * Keeps memory footprint strictly bounded to chunk size (default 8MB).
   */
  public static async readChunk(
    filePath: string,
    offset: number,
    length: number
  ): Promise<{ data: Buffer; sha256: string }> {
    const handle = await fs.open(filePath, constants.O_RDONLY);
    try {
      const buffer = Buffer.allocUnsafe(length);
      const { bytesRead } = await handle.read(buffer, 0, length, offset);
      
      const chunkData = bytesRead === length ? buffer : buffer.subarray(0, bytesRead);
      const sha256 = CryptoEngine.sha256(chunkData);
      return { data: chunkData, sha256 };
    } finally {
      await handle.close();
    }
  }

  /**
   * Writes a verified chunk directly to disk at the given byte offset.
   * Allocates/prepares destination file if not already created.
   */
  public static async writeChunk(
    targetFilePath: string,
    offset: number,
    data: Buffer,
    expectedSha256?: string
  ): Promise<boolean> {
    // 1. Verify chunk hash if expected hash provided
    if (expectedSha256) {
      const computedSha256 = CryptoEngine.sha256(data);
      if (computedSha256 !== expectedSha256) {
        throw new Error(`Chunk SHA-256 mismatch: expected ${expectedSha256}, got ${computedSha256}`);
      }
    }

    // 2. Ensure parent directory exists
    const dir = path.dirname(targetFilePath);
    await fs.mkdir(dir, { recursive: true });

    // 3. Open with read/write (create if not exist without truncating)
    const handle = await fs.open(targetFilePath, constants.O_CREAT | constants.O_RDWR);
    try {
      await handle.write(data, 0, data.length, offset);
      return true;
    } finally {
      await handle.close();
    }
  }

  /**
   * Computes full SHA-256 hash of an existing file on disk by streaming chunks
   */
  public static async computeFileSha256(filePath: string): Promise<string> {
    const stat = await fs.stat(filePath);
    const handle = await fs.open(filePath, constants.O_RDONLY);
    const hash = crypto.createHash('sha256');
    const buffer = Buffer.allocUnsafe(DEFAULT_CHUNK_SIZE);

    try {
      let offset = 0;
      while (offset < stat.size) {
        const toRead = Math.min(DEFAULT_CHUNK_SIZE, stat.size - offset);
        const { bytesRead } = await handle.read(buffer, 0, toRead, offset);
        if (bytesRead === 0) break;
        hash.update(buffer.subarray(0, bytesRead));
        offset += bytesRead;
      }
      return hash.digest('hex');
    } finally {
      await handle.close();
    }
  }
}

import * as crypto from 'node:crypto';
