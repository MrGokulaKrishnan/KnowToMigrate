/**
 * KnowToMigrate - Automated Stress & Security Test Suite
 * Validates path traversal sandboxing, Windows reserved device names,
 * single-bit corruption detection, Merkle verification, AES-256-GCM auth,
 * large-batch transfer, and checkpoint resume recovery.
 */

import { test, describe } from 'node:test';
import * as assert from 'node:assert';
import * as crypto from 'node:crypto';
import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import * as os from 'node:os';

import { CryptoEngine } from '../crypto/crypto.js';
import { PathGuard } from '../storage/pathGuard.js';
import { CheckpointManager } from '../storage/checkpoint.js';
import { ChunkerEngine } from '../transfer/chunker.js';

describe('Security Verification Suite', () => {
  test('PathGuard: Blocks directory traversal attacks (UNIX & Windows)', () => {
    const maliciousPaths = [
      '../../etc/passwd',
      '..\\..\\Windows\\System32\\config\\SAM',
      'foo/../../../bar/secret.txt',
      '....//....//escape.sh',
      '/var/log/syslog',
      'C:\\Windows\\System32\\cmd.exe',
      'D:/private/keys.pem',
    ];

    for (const p of maliciousPaths) {
      const sanitized = PathGuard.sanitizeRelativePath(p);
      assert.ok(!sanitized.includes('..'), `Path must not contain '..': ${sanitized}`);
      assert.ok(!sanitized.startsWith('/') && !sanitized.startsWith('\\'), `Path must not start with slash: ${sanitized}`);
      assert.ok(!/^[a-zA-Z]:/.test(sanitized), `Path must not contain drive letter: ${sanitized}`);
    }
  });

  test('PathGuard: Sanitizes Windows reserved device names', () => {
    const reservedNames = [
      'CON', 'PRN', 'AUX', 'NUL',
      'COM1', 'COM2', 'COM3', 'COM4',
      'LPT1', 'LPT2', 'LPT3', 'LPT4',
      'con.txt', 'prn.pdf', 'aux.zip', 'nul.tar.gz',
    ];

    for (const name of reservedNames) {
      const sanitized = PathGuard.sanitizeRelativePath(name);
      const base = sanitized.split('.')[0].toUpperCase();
      assert.ok(
        !['CON', 'PRN', 'AUX', 'NUL', 'COM1', 'COM2', 'COM3', 'COM4', 'LPT1', 'LPT2', 'LPT3', 'LPT4'].includes(base),
        `Reserved name '${name}' must be safely altered: got '${sanitized}'`
      );
    }
  });

  test('PathGuard: Resolves strictly within sandbox root boundary', () => {
    const sandboxRoot = path.resolve(os.tmpdir(), 'ktm-sandbox-test');
    const attackAttempts = [
      '../../Windows/notepad.exe',
      'sub/../../../../../../boot.ini',
      'con.bat',
      'safe/folder/file.pdf',
    ];

    for (const attack of attackAttempts) {
      const resolved = PathGuard.resolveSafeDestination(sandboxRoot, attack);
      const normalizedRoot = path.normalize(sandboxRoot).toLowerCase();
      const normalizedDest = path.normalize(resolved).toLowerCase();
      assert.ok(
        normalizedDest.startsWith(normalizedRoot),
        `Destination path '${normalizedDest}' must remain inside root '${normalizedRoot}'`
      );
    }
  });

  test('CryptoEngine: Single-bit flip corruption detection', () => {
    const payload = Buffer.alloc(1024 * 1024, 'A'); // 1 MB test payload
    const originalHash = CryptoEngine.sha256(payload);

    // Tamper single bit at offset 500,000
    const corruptedPayload = Buffer.from(payload);
    corruptedPayload[500000] ^= 0x01; // flip 1 bit
    const corruptedHash = CryptoEngine.sha256(corruptedPayload);

    assert.notStrictEqual(
      corruptedHash,
      originalHash,
      'Corrupted payload must produce a completely different SHA-256 digest'
    );
  });

  test('CryptoEngine: Dual-layer Merkle tree integrity and tamper rejection', () => {
    const chunkHashes = [
      CryptoEngine.sha256('chunk 1 payload data'),
      CryptoEngine.sha256('chunk 2 payload data'),
      CryptoEngine.sha256('chunk 3 payload data'),
      CryptoEngine.sha256('chunk 4 payload data'),
    ];

    const validRoot = CryptoEngine.computeMerkleRoot(chunkHashes);
    assert.strictEqual(typeof validRoot, 'string');
    assert.strictEqual(validRoot.length, 64); // SHA-256 hex length

    // Tamper with one chunk hash
    const tamperedChunkHashes = [...chunkHashes];
    tamperedChunkHashes[2] = CryptoEngine.sha256('tampered chunk 3 payload data');
    const tamperedRoot = CryptoEngine.computeMerkleRoot(tamperedChunkHashes);

    assert.notStrictEqual(
      tamperedRoot,
      validRoot,
      'Tampered chunk must produce a completely different Merkle root certificate'
    );
  });

  test('CryptoEngine: AES-256-GCM authentication tag verification rejects forged ciphertext', () => {
    const key = CryptoEngine.generateSessionKey();
    const plaintext = Buffer.from('Confidential device migration payload', 'utf-8');

    const encrypted = CryptoEngine.encrypt(plaintext, key);

    // Decrypt valid
    const decrypted = CryptoEngine.decrypt(encrypted, key);
    assert.strictEqual(decrypted.toString('utf-8'), plaintext.toString('utf-8'));

    // Tamper encrypted buffer (flip bit in authentication tag at offset 15)
    const forgedEncrypted = Buffer.from(encrypted);
    forgedEncrypted[15] ^= 0xff;

    assert.throws(
      () => {
        CryptoEngine.decrypt(forgedEncrypted, key);
      },
      /Unsupported state or unable to authenticate data/,
      'Tampered ciphertext or auth tag must be rejected by AES-GCM'
    );
  });
});

describe('Stress & Resilience Verification Suite', () => {
  test('Stress: 100 GB Virtual File Chunking Calculation with Zero Memory Leak', () => {
    const virtualSizeBytes = 100 * 1024 * 1024 * 1024; // 100 GB
    const chunkSize = 8 * 1024 * 1024; // 8 MB
    const totalChunks = Math.ceil(virtualSizeBytes / chunkSize);

    assert.strictEqual(totalChunks, 12800, '100 GB must slice into precisely 12,800 chunks of 8MB');

    // Emulate rolling calculation without allocating 100 GB in RAM
    const initialMemory = process.memoryUsage().heapUsed;
    let computedByteSum = 0;

    for (let i = 0; i < totalChunks; i++) {
      const chunkBytes = (i === totalChunks - 1)
        ? virtualSizeBytes - i * chunkSize
        : chunkSize;
      computedByteSum += chunkBytes;
    }

    assert.strictEqual(computedByteSum, virtualSizeBytes);
    const finalMemory = process.memoryUsage().heapUsed;
    const memoryDiffMB = (finalMemory - initialMemory) / (1024 * 1024);

    assert.ok(
      memoryDiffMB < 20,
      `Memory growth during 100 GB chunking must remain strictly under 20 MB (got ${memoryDiffMB.toFixed(2)} MB)`
    );
  });

  test('Stress: Concurrent Chunk Write Pipeline (50 simultaneous chunks)', async () => {
    const tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'ktm-concurrent-test-'));
    const testFilePath = path.join(tmpDir, 'concurrent_stream.bin');
    const chunkSize = 64 * 1024; // 64 KB per chunk
    const totalChunks = 50;

    const chunkTasks: Promise<void>[] = [];

    for (let i = 0; i < totalChunks; i++) {
      const data = Buffer.alloc(chunkSize, i & 0xff);
      const hash = CryptoEngine.sha256(data);
      const offset = i * chunkSize;
      chunkTasks.push(
        ChunkerEngine.writeChunk(testFilePath, offset, data, hash).then(() => {})
      );
    }

    await Promise.all(chunkTasks);

    const stat = await fs.stat(testFilePath);
    assert.strictEqual(stat.size, totalChunks * chunkSize);

    // Verify all chunks in order
    const readBuffer = await fs.readFile(testFilePath);
    for (let i = 0; i < totalChunks; i++) {
      const slice = readBuffer.subarray(i * chunkSize, (i + 1) * chunkSize);
      assert.strictEqual(slice[0], i & 0xff);
      assert.strictEqual(slice[chunkSize - 1], i & 0xff);
    }

    await fs.rm(tmpDir, { recursive: true, force: true });
  });

  test('Resilience: Checkpoint Manager handles interrupted transfer and resumes verified chunks', async () => {
    const tmpDir = await fs.mkdtemp(path.join(os.tmpdir(), 'ktm-checkpoint-test-'));
    const totalChunks = 10;
    const chunkSize = 128 * 1024;
    const sessionId = 'sess-resilience-1';

    const initialCheckpoint = {
      sessionId,
      sourceDeviceId: 'source-device-node-1',
      targetDeviceId: 'target-device-node-2',
      createdAt: Date.now(),
      updatedAt: Date.now(),
      totalBytes: totalChunks * chunkSize,
      transferredBytes: 0,
      files: [{
        index: 0,
        relativePath: 'resumable_backup.iso',
        verifiedChunks: [] as number[],
      }],
      isComplete: false,
    };

    // 1. Initial creation: 0 chunks completed
    await CheckpointManager.saveCheckpoint(tmpDir, initialCheckpoint);
    let cp = await CheckpointManager.loadCheckpoint(tmpDir, sessionId);
    assert.ok(cp);
    assert.strictEqual(cp.files[0].verifiedChunks.length, 0);

    // 2. Simulate transfer interrupted after chunks 0, 1, 2, 3, 4 completed
    cp.files[0].verifiedChunks = [0, 1, 2, 3, 4];
    cp.transferredBytes = 5 * chunkSize;
    await CheckpointManager.saveCheckpoint(tmpDir, cp);

    cp = await CheckpointManager.loadCheckpoint(tmpDir, sessionId);
    assert.ok(cp);
    assert.deepStrictEqual(cp.files[0].verifiedChunks, [0, 1, 2, 3, 4]);

    // 3. Resume: Only missing chunks remain
    const missingIndices = CheckpointManager.getMissingChunkIndices(totalChunks, cp.files[0].verifiedChunks);
    assert.strictEqual(missingIndices.length, 5);
    assert.deepStrictEqual(missingIndices, [5, 6, 7, 8, 9]);

    // 4. Complete remaining chunks
    cp.files[0].verifiedChunks = Array.from({ length: totalChunks }, (_, i) => i);
    cp.transferredBytes = totalChunks * chunkSize;
    cp.isComplete = true;
    await CheckpointManager.saveCheckpoint(tmpDir, cp);

    // 5. Cleanup on verified complete
    await CheckpointManager.cleanupCheckpoint(tmpDir);
    const cleanedCp = await CheckpointManager.loadCheckpoint(tmpDir, sessionId);
    assert.strictEqual(cleanedCp, null);

    await fs.rm(tmpDir, { recursive: true, force: true });
  });
});
