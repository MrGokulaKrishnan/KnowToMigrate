import { test } from 'node:test';
import * as assert from 'node:assert';
import { PathGuard } from '../storage/pathGuard.js';

test('PathGuard - Sanitizes path traversal attempts', () => {
  const unsafe = '../../etc/passwd';
  const sanitized = PathGuard.sanitizeRelativePath(unsafe);
  assert.strictEqual(sanitized, 'etc/passwd');
});

test('PathGuard - Sanitizes Windows drive root paths', () => {
  const unsafe = 'C:\\Windows\\System32\\cmd.exe';
  const sanitized = PathGuard.sanitizeRelativePath(unsafe);
  assert.strictEqual(sanitized, 'Windows/System32/cmd.exe');
});

test('PathGuard - Replaces Windows reserved device names', () => {
  const unsafe = 'CON.txt';
  const sanitized = PathGuard.sanitizeRelativePath(unsafe);
  assert.strictEqual(sanitized.startsWith('_reserved_'), true);
});

test('PathGuard - Resolves strictly within sandbox root', () => {
  const sandbox = 'C:/SafeFolder';
  const resolved = PathGuard.resolveSafeDestination(sandbox, 'Photos/2026/img.jpg');
  assert.strictEqual(resolved.replace(/\\/g, '/').includes('SafeFolder/Photos/2026/img.jpg'), true);
});
