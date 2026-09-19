import { test } from 'node:test';
import * as assert from 'node:assert';
import { CryptoEngine } from '../crypto/crypto.js';

test('CryptoEngine - SHA-256 computation', () => {
  const hash = CryptoEngine.sha256('KnowToMigrate');
  assert.strictEqual(typeof hash, 'string');
  assert.strictEqual(hash.length, 64);
});

test('CryptoEngine - AES-256-GCM encryption and decryption', () => {
  const sessionKey = CryptoEngine.generateSessionKey();
  const secretData = Buffer.from('Sensitive KnowToMigrate Payload 2026', 'utf-8');

  const encrypted = CryptoEngine.encrypt(secretData, sessionKey);
  assert.notDeepStrictEqual(encrypted, secretData);

  const decrypted = CryptoEngine.decrypt(encrypted, sessionKey);
  assert.deepStrictEqual(decrypted.toString('utf-8'), secretData.toString('utf-8'));
});

test('CryptoEngine - Merkle root computation', () => {
  const hash1 = CryptoEngine.sha256('Chunk 1');
  const hash2 = CryptoEngine.sha256('Chunk 2');
  const hash3 = CryptoEngine.sha256('Chunk 3');

  const merkleRoot = CryptoEngine.computeMerkleRoot([hash1, hash2, hash3]);
  assert.strictEqual(typeof merkleRoot, 'string');
  assert.strictEqual(merkleRoot.length, 64);
});
