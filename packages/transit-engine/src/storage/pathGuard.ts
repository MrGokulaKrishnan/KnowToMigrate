/**
 * KnowToMigrate - Path Traversal Guard & Filesystem Sandbox
 */

import * as path from 'node:path';

// Reserved Windows file/device names
const WINDOWS_RESERVED_NAMES = new Set([
  'CON', 'PRN', 'AUX', 'NUL',
  'COM1', 'COM2', 'COM3', 'COM4', 'COM5', 'COM6', 'COM7', 'COM8', 'COM9',
  'LPT1', 'LPT2', 'LPT3', 'LPT4', 'LPT5', 'LPT6', 'LPT7', 'LPT8', 'LPT9'
]);

export class PathGuard {
  /**
   * Sanitizes relative incoming paths to guarantee they remain strictly
   * inside the user-approved destination sandbox directory.
   */
  public static sanitizeRelativePath(unsafePath: string): string {
    // 1. Normalize slashes
    let normalized = unsafePath.replace(/\\/g, '/');

    // 2. Remove leading slashes and drive letters (e.g. "C:/")
    normalized = normalized.replace(/^[a-zA-Z]:\/?/, '');
    normalized = normalized.replace(/^\/+/, '');

    // 3. Deconstruct segments and filter out '.' and '..'
    const segments = normalized.split('/').filter(seg => {
      const clean = seg.trim();
      return clean.length > 0 && clean !== '.' && clean !== '..';
    });

    if (segments.length === 0) {
      throw new Error(`Invalid or empty sanitized relative path: "${unsafePath}"`);
    }

    // 4. Validate each segment against reserved names and illegal characters
    const sanitizedSegments = segments.map(seg => {
      // Remove illegal characters: < > : " / \ | ? * and control characters
      let safe = seg.replace(/[<>:"/\\|?*\x00-\x1F]/g, '_');
      // Trim trailing spaces and dots (Windows compatibility)
      safe = safe.replace(/[. ]+$/, '');

      // Check against Windows reserved device names
      const baseName = safe.split('.')[0].toUpperCase();
      if (WINDOWS_RESERVED_NAMES.has(baseName)) {
        safe = `_reserved_${safe}`;
      }

      return safe.length > 0 ? safe : '_unnamed_';
    });

    return sanitizedSegments.join('/');
  }

  /**
   * Resolves a sanitized relative path inside a destination directory,
   * guaranteeing that the resolved path is strictly within the sandbox root.
   */
  public static resolveSafeDestination(sandboxRoot: string, unsafePath: string): string {
    const safeRel = PathGuard.sanitizeRelativePath(unsafePath);
    const resolvedPath = path.resolve(sandboxRoot, safeRel);
    const resolvedRoot = path.resolve(sandboxRoot);

    // Guard: Resolved path MUST start with sandbox root
    if (!resolvedPath.startsWith(resolvedRoot + path.sep) && resolvedPath !== resolvedRoot) {
      throw new Error(`Security Violation: Path "${unsafePath}" escaped sandbox root "${sandboxRoot}"`);
    }

    return resolvedPath;
  }
}
