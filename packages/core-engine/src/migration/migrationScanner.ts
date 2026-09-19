/**
 * KnowToMigrate - Smart Device Migration Scanner
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import {
  MigrationCategorySummary,
  MigrationCategoryType,
  PreflightStorageCheck
} from '@knowtomigrate/protocol-types';

export class MigrationScanner {
  private static readonly EXTENSIONS: Record<MigrationCategoryType, string[]> = {
    photos: ['.jpg', '.jpeg', '.png', '.heic', '.webp', '.raw', '.cr2', '.nef', '.dng'],
    videos: ['.mp4', '.mov', '.mkv', '.avi', '.webm', '.m4v', '.wmv'],
    documents: ['.pdf', '.doc', '.docx', '.xls', '.xlsx', '.ppt', '.pptx', '.txt', '.md', '.csv', '.zip', '.tar', '.gz'],
    downloads: [], // All files in user Downloads folder
    music: ['.mp3', '.flac', '.wav', '.aac', '.m4a', '.ogg', '.wma'],
    contacts: ['.vcf', '.csv'],
    calendar: ['.ics'],
    custom_folders: []
  };

  /**
   * Scans a target directory recursively and buckets files into categories
   */
  public static async scanDirectory(basePath: string): Promise<Record<MigrationCategoryType, { count: number; bytes: number; paths: string[] }>> {
    const results: Record<MigrationCategoryType, { count: number; bytes: number; paths: string[] }> = {
      photos: { count: 0, bytes: 0, paths: [] },
      videos: { count: 0, bytes: 0, paths: [] },
      documents: { count: 0, bytes: 0, paths: [] },
      downloads: { count: 0, bytes: 0, paths: [] },
      music: { count: 0, bytes: 0, paths: [] },
      contacts: { count: 0, bytes: 0, paths: [] },
      calendar: { count: 0, bytes: 0, paths: [] },
      custom_folders: { count: 0, bytes: 0, paths: [] }
    };

    const isDownloads = basePath.toLowerCase().includes('downloads');

    async function walk(dir: string) {
      try {
        const entries = await fs.readdir(dir, { withFileTypes: true });
        for (const entry of entries) {
          const fullPath = path.join(dir, entry.name);
          if (entry.isDirectory()) {
            // Skip hidden or system folders
            if (entry.name.startsWith('.') || entry.name === 'node_modules') continue;
            await walk(fullPath);
          } else if (entry.isFile()) {
            try {
              const stat = await fs.stat(fullPath);
              const ext = path.extname(entry.name).toLowerCase();

              if (isDownloads) {
                results.downloads.count++;
                results.downloads.bytes += stat.size;
                results.downloads.paths.push(fullPath);
              }

              for (const [cat, exts] of Object.entries(MigrationScanner.EXTENSIONS)) {
                if (exts.includes(ext)) {
                  const c = cat as MigrationCategoryType;
                  results[c].count++;
                  results[c].bytes += stat.size;
                  results[c].paths.push(fullPath);
                  break;
                }
              }
            } catch {}
          }
        }
      } catch {}
    }

    await walk(basePath);
    return results;
  }

  /**
   * Performs Pre-flight storage check against target device free space
   */
  public static evaluateStorageHeadroom(
    sourceTotalBytes: number,
    destinationFreeBytes: number,
    destinationTotalBytes: number
  ): PreflightStorageCheck {
    const safetyMarginBytes = 2 * 1024 * 1024 * 1024; // 2 GB safety margin
    const requiredBytes = sourceTotalBytes + safetyMarginBytes;
    const hasSufficientSpace = destinationFreeBytes >= requiredBytes;

    // Estimate transfer duration assuming 80 MB/s average LAN speed
    const estimatedDurationSeconds = Math.ceil(sourceTotalBytes / (80 * 1024 * 1024)) || 1;

    return {
      sourceTotalBytes,
      destinationFreeBytes,
      destinationTotalBytes,
      hasSufficientSpace,
      safetyMarginBytes,
      estimatedDurationSeconds,
      recommendedTransport: 'Direct Wi-Fi 6 P2P (TLS 1.3)'
    };
  }
}
