/**
 * KnowToMigrate - Cryptographic Engine
 * Implements AES-256-GCM authenticated encryption, X25519/ECDH key agreement,
 * and SHA-256 Merkle-tree chunk verification.
 */

import * as crypto from 'node:crypto';

export class CryptoEngine {
  /**
   * Computes standard SHA-256 hash of a buffer in hex
   */
  public static sha256(data: Buffer | string): string {
    const hash = crypto.createHash('sha256');
    hash.update(data);
    return hash.digest('hex');
  }

  /**
   * Generates a cryptographically secure random session ID
   */
  public static generateSessionId(): string {
    return crypto.randomBytes(16).toString('hex');
  }

  /**
   * Generates a 256-bit symmetric session key
   */
  public static generateSessionKey(): Buffer {
    return crypto.randomBytes(32);
  }

  /**
   * Generates an ephemeral ECDH keypair for key exchange
   */
  public static generateKeyPair(): { publicKey: string; privateKey: string } {
    const ecdh = crypto.createECDH('prime256v1');
    ecdh.generateKeys();
    return {
      publicKey: ecdh.getPublicKey('hex'),
      privateKey: ecdh.getPrivateKey('hex')
    };
  }

  /**
   * Derives a shared 256-bit AES key using ECDH
   */
  public static deriveSharedKey(localPrivateKeyHex: string, remotePublicKeyHex: string): Buffer {
    const ecdh = crypto.createECDH('prime256v1');
    ecdh.setPrivateKey(Buffer.from(localPrivateKeyHex, 'hex'));
    const secret = ecdh.computeSecret(Buffer.from(remotePublicKeyHex, 'hex'));
    // Hash secret using SHA-256 to produce uniform 32-byte key
    return crypto.createHash('sha256').update(secret).digest();
  }

  /**
   * Encrypts plaintext buffer using AES-256-GCM.
   * Format: [12-byte IV] + [16-byte Auth Tag] + [Ciphertext]
   */
  public static encrypt(plaintext: Buffer, key: Buffer): Buffer {
    const iv = crypto.randomBytes(12); // Recommended 96-bit IV for GCM
    const cipher = crypto.createCipheriv('aes-256-gcm', key, iv);
    
    const encrypted = Buffer.concat([
      cipher.update(plaintext),
      cipher.final()
    ]);
    const tag = cipher.getAuthTag();

    return Buffer.concat([iv, tag, encrypted]);
  }

  /**
   * Decrypts AES-256-GCM buffer with integrity validation.
   * Format: [12-byte IV] + [16-byte Auth Tag] + [Ciphertext]
   */
  public static decrypt(encryptedBuffer: Buffer, key: Buffer): Buffer {
    if (encryptedBuffer.length < 28) {
      throw new Error('Encrypted payload too short (must be at least 28 bytes for IV + tag)');
    }

    const iv = encryptedBuffer.subarray(0, 12);
    const tag = encryptedBuffer.subarray(12, 28);
    const ciphertext = encryptedBuffer.subarray(28);

    const decipher = crypto.createDecipheriv('aes-256-gcm', key, iv);
    decipher.setAuthTag(tag);

    return Buffer.concat([
      decipher.update(ciphertext),
      decipher.final()
    ]);
  }

  /**
   * Computes a Merkle root hash from an ordered array of chunk hashes.
   * Guarantees holistic file and manifest integrity.
   */
  public static computeMerkleRoot(hashes: string[]): string {
    if (hashes.length === 0) {
      return CryptoEngine.sha256('');
    }
    if (hashes.length === 1) {
      return hashes[0];
    }

    let currentLevel: Buffer[] = hashes.map(h => Buffer.from(h, 'hex') as Buffer);

    while (currentLevel.length > 1) {
      const nextLevel: Buffer[] = [];
      for (let i = 0; i < currentLevel.length; i += 2) {
        if (i + 1 < currentLevel.length) {
          const combined = Buffer.concat([currentLevel[i], currentLevel[i + 1]]);
          nextLevel.push(crypto.createHash('sha256').update(combined).digest() as Buffer);
        } else {
          // Odd number: promote the single hash to next level
          nextLevel.push(currentLevel[i]);
        }
      }
      currentLevel = nextLevel;
    }

    return currentLevel[0].toString('hex');
  }
}
