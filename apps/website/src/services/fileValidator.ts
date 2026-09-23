/**
 * KnowToMigrate - File Validation & PathGuard Engine
 * Sanitizes file names, guards against path traversal attacks,
 * detects Windows reserved device names, and validates file sizes & MIME types.
 */

const WINDOWS_RESERVED_NAMES = new Set([
  'CON', 'PRN', 'AUX', 'NUL',
  'COM1', 'COM2', 'COM3', 'COM4', 'COM5', 'COM6', 'COM7', 'COM8', 'COM9',
  'LPT1', 'LPT2', 'LPT3', 'LPT4', 'LPT5', 'LPT6', 'LPT7', 'LPT8', 'LPT9',
]);

const ILLEGAL_CHARS_REGEX = /[<>:"/\\|?*\x00-\x1F]/g;

export interface ValidatedFileItem {
  id: string;
  originalName: string;
  sanitizedName: string;
  sizeBytes: number;
  sizeFormatted: string;
  mimeType: string;
  category: 'video' | 'audio' | 'image' | 'doc' | 'archive' | 'folder' | 'other';
  rawFile?: File;
  isValid: boolean;
  validationError?: string;
}

export class FileValidator {
  /** Maximum safe file payload per single item (500 GB) */
  public static readonly MAX_FILE_SIZE = 500 * 1024 * 1024 * 1024;

  public static sanitizeFilename(inputName: string): string {
    if (!inputName || typeof inputName !== 'string') {
      return `ktm_unnamed_${Date.now()}`;
    }

    // 1. Strip directory traversal sequences
    let clean = inputName
      .replace(/^[a-zA-Z]:[/\\]/g, '') // Strip drive letters e.g. C:\
      .replace(/\.\.[/\\]/g, '')         // Strip ../ or ..\
      .replace(/[/\\]/g, '_')            // Convert remaining slashes to underscore
      .trim();

    // 2. Remove forbidden filesystem characters
    clean = clean.replace(ILLEGAL_CHARS_REGEX, '_');

    // 3. Check Windows reserved base names (e.g. CON.txt -> _CON.txt)
    const parts = clean.split('.');
    const baseName = (parts[0] || '').toUpperCase();
    if (WINDOWS_RESERVED_NAMES.has(baseName)) {
      parts[0] = `_${parts[0]}`;
      clean = parts.join('.');
    }

    // 4. Fallback if empty after sanitization
    if (!clean || clean === '.' || clean === '..') {
      clean = `ktm_file_${Date.now()}`;
    }

    return clean;
  }

  public static categorizeFile(filename: string, mimeType: string): ValidatedFileItem['category'] {
    const ext = filename.split('.').pop()?.toLowerCase() || '';

    if (
      ['mp4', 'mkv', 'mov', 'avi', 'webm', 'm4v'].includes(ext) ||
      mimeType.startsWith('video/')
    ) {
      return 'video';
    }

    if (
      ['mp3', 'flac', 'wav', 'aac', 'ogg', 'm4a'].includes(ext) ||
      mimeType.startsWith('audio/')
    ) {
      return 'audio';
    }

    if (
      ['jpg', 'jpeg', 'png', 'gif', 'webp', 'svg', 'heic', 'raw', 'cr2'].includes(ext) ||
      mimeType.startsWith('image/')
    ) {
      return 'image';
    }

    if (
      ['pdf', 'docx', 'doc', 'xlsx', 'pptx', 'txt', 'md', 'csv', 'json'].includes(ext) ||
      mimeType.includes('document') || mimeType.includes('pdf') || mimeType.startsWith('text/')
    ) {
      return 'doc';
    }

    if (['zip', 'tar', 'gz', '7z', 'rar', 'bz2'].includes(ext)) {
      return 'archive';
    }

    if (filename.endsWith('/') || filename.endsWith('\\')) {
      return 'folder';
    }

    return 'other';
  }

  public static validateFile(file: File): ValidatedFileItem {
    const sanitized = this.sanitizeFilename(file.name);
    const category = this.categorizeFile(file.name, file.type);
    let isValid = true;
    let validationError: string | undefined;

    if (file.size > this.MAX_FILE_SIZE) {
      isValid = false;
      validationError = `File exceeds 500GB limit (${this.formatBytes(file.size)})`;
    }

    return {
      id: 'file-' + Math.random().toString(36).substring(2, 9),
      originalName: file.name,
      sanitizedName: sanitized,
      sizeBytes: file.size,
      sizeFormatted: this.formatBytes(file.size),
      mimeType: file.type || 'application/octet-stream',
      category,
      rawFile: file,
      isValid,
      validationError,
    };
  }

  public static formatBytes(bytes: number): string {
    if (bytes <= 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    const idx = Math.min(units.length - 1, Math.floor(Math.log(bytes) / Math.log(1024)));
    const value = bytes / Math.pow(1024, idx);
    return `${value.toFixed(value >= 10 || idx === 0 ? 1 : 2)} ${units[idx]}`;
  }
}
