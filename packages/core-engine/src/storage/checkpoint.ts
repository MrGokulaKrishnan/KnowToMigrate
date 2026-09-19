/**
 * KnowToMigrate - Checkpoint & Resume Engine
 * Persists verified chunk bitsets to disk for interrupted transfer recovery.
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import { TransferCheckpoint } from '@knowtomigrate/protocol-types';

export class CheckpointManager {
  private static readonly CHECKPOINT_FILENAME = '.ktm-checkpoint.json';

  /**
   * Generates or updates an on-disk checkpoint in the target staging directory
   */
  public static async saveCheckpoint(destinationDir: string, checkpoint: TransferCheckpoint): Promise<void> {
    const checkpointPath = path.join(destinationDir, CheckpointManager.CHECKPOINT_FILENAME);
    checkpoint.updatedAt = Date.now();
    const json = JSON.stringify(checkpoint, null, 2);
    await fs.writeFile(checkpointPath, json, 'utf-8');
  }

  /**
   * Loads an existing checkpoint if one exists for the given session ID
   */
  public static async loadCheckpoint(destinationDir: string, sessionId: string): Promise<TransferCheckpoint | null> {
    const checkpointPath = path.join(destinationDir, CheckpointManager.CHECKPOINT_FILENAME);
    try {
      const data = await fs.readFile(checkpointPath, 'utf-8');
      const checkpoint: TransferCheckpoint = JSON.parse(data);
      if (checkpoint.sessionId === sessionId) {
        return checkpoint;
      }
      return null;
    } catch {
      return null;
    }
  }

  /**
   * Deletes the checkpoint file upon successful transfer verification
   */
  public static async cleanupCheckpoint(destinationDir: string): Promise<void> {
    const checkpointPath = path.join(destinationDir, CheckpointManager.CHECKPOINT_FILENAME);
    try {
      await fs.unlink(checkpointPath);
    } catch {
      // Ignored if file does not exist
    }
  }

  /**
   * Calculates the set of chunk indices that still need to be transferred
   */
  public static getMissingChunkIndices(totalChunks: number, verifiedChunks: number[]): number[] {
    const verifiedSet = new Set(verifiedChunks);
    const missing: number[] = [];
    for (let i = 0; i < totalChunks; i++) {
      if (!verifiedSet.has(i)) {
        missing.push(i);
      }
    }
    return missing;
  }
}
