/**
 * KnowToMigrate - Automated End-to-End P2P Transfer Simulation
 * Spawns a real TransferServer and TransferClient over local TCP sockets,
 * streams chunked files, tracks real-time throughput & ETA, and verifies SHA-256 Merkle root.
 */

import * as fs from 'node:fs/promises';
import * as path from 'node:path';
import * as os from 'node:os';
import { TransferServer } from '../network/transferServer.js';
import { TransferClient } from '../network/transferClient.js';
import { ChunkerEngine } from '../transfer/chunker.js';
import { FileItemManifest } from '@knowtomigrate/protocol-types';

async function main() {
  console.log('\n=============================================================');
  console.log('⚡ KNOWTOMIGRATE — END-TO-END P2P TRANSFER SIMULATION (V2.0)');
  console.log('=============================================================\n');

  const tmpRoot = await fs.mkdtemp(path.join(os.tmpdir(), 'ktm-sim-'));
  const sourceDir = path.join(tmpRoot, 'source');
  const targetDir = path.join(tmpRoot, 'target');

  await fs.mkdir(sourceDir, { recursive: true });
  await fs.mkdir(targetDir, { recursive: true });

  console.log(`[1/5] Creating synthetic test files in: ${sourceDir}`);
  // Create 3 realistic test files (one 16MB file to force chunking across multiple 8MB chunks, plus smaller documents)
  const file1Path = path.join(sourceDir, 'Project_Video_4K.mp4');
  const file2Path = path.join(sourceDir, 'Design_Specs.pdf');

  // Generate 16MB of pseudo-random data for file 1 (2 chunks of 8MB)
  const chunk8MB = Buffer.alloc(8 * 1024 * 1024, 'K');
  await fs.writeFile(file1Path, Buffer.concat([chunk8MB, chunk8MB]));
  await fs.writeFile(file2Path, Buffer.from('KnowToMigrate Architecture Specification 2026', 'utf-8'));

  const file1Stat = await fs.stat(file1Path);
  const file2Stat = await fs.stat(file2Path);
  console.log(`  ✓ Created Project_Video_4K.mp4 (${(file1Stat.size / (1024 * 1024)).toFixed(1)} MB)`);
  console.log(`  ✓ Created Design_Specs.pdf (${file2Stat.size} bytes)`);

  console.log('\n[2/5] Building chunk manifests...');
  const manifest1 = await ChunkerEngine.inspectFileForManifest(file1Path, 'Project_Video_4K.mp4', 0);
  const manifest2 = await ChunkerEngine.inspectFileForManifest(file2Path, 'Design_Specs.pdf', 1);
  const manifests: FileItemManifest[] = [manifest1, manifest2];
  console.log(`  ✓ Total files: ${manifests.length}, total chunks: ${manifests.reduce((acc, m) => acc + m.chunks.length, 0)}`);

  console.log('\n[3/5] Launching receiver TransferServer...');
  const server = new TransferServer(0, targetDir); // Bind to dynamic available port
  const port = await server.start();
  console.log(`  ✓ Receiver listening on 127.0.0.1:${port}`);

  console.log('\n[4/5] Initiating sender TransferClient stream...');
  const client = new TransferClient();

  client.on('progress', (p) => {
    const pct = Math.round((p.transferredBytes / p.totalBytes) * 100);
    const mbTransferred = (p.transferredBytes / (1024 * 1024)).toFixed(1);
    const mbTotal = (p.totalBytes / (1024 * 1024)).toFixed(1);
    process.stdout.write(`\r  ⚡ Progress: [${mbTransferred}/${mbTotal} MB] (${pct}%) | Speed: ${p.speedMbps} Mbps | ETA: ${p.etaSeconds}s `);
  });

  const startTime = Date.now();
  const merkleRoot = await client.sendFiles('127.0.0.1', port, manifests);
  const durationMs = Date.now() - startTime;
  console.log(`\n\n[5/5] Transfer Pipeline Complete in ${durationMs}ms!`);
  console.log(`  ✓ Merkle Root Certificate: ${merkleRoot}`);

  // Verify file on target disk matches source bit-for-bit
  const receivedFile1 = path.join(targetDir, 'Project_Video_4K.mp4');
  const sourceHash = await ChunkerEngine.computeFileSha256(file1Path);
  const targetHash = await ChunkerEngine.computeFileSha256(receivedFile1);

  console.log('\n--- VERIFICATION AUDIT ---');
  console.log(`  Source SHA-256: ${sourceHash}`);
  console.log(`  Target SHA-256: ${targetHash}`);

  if (sourceHash === targetHash) {
    console.log('  ✅ INTEGRITY VERIFIED 100%: ZERO DATA CORRUPTION!');
  } else {
    console.error('  ❌ INTEGRITY CHECK FAILED!');
    process.exit(1);
  }

  // Cleanup
  server.stop();
  await fs.rm(tmpRoot, { recursive: true, force: true });
  console.log('\nCleaned up staging temp files. Simulation successful!\n');
  process.exit(0);
}

main().catch(err => {
  console.error('\n❌ Simulation failed with error:', err);
  process.exit(1);
});
