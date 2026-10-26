#!/usr/bin/env node
// Generates every release reference from version.json and the release artifacts.
//
//   node scripts/release/site-release.mjs           write all generated files
//   node scripts/release/site-release.mjs --check   fail if any generated file is stale,
//                                                   or if website source hardcodes a version
//
// Generated: apps/website/public/update-manifest.json,
//            apps/website/public/updates/android/stable.json,
//            apps/website/src/generated/release.ts,
//            the rewrites and download headers in firebase.json.
// Inputs:    version.json and apps/website/public/download/<name>.bin for each artifact.
// Fails closed: a missing artifact stops the run; nothing is written partially.

import { readFileSync, writeFileSync, existsSync, mkdirSync, readdirSync, statSync } from 'node:fs'
import { createHash } from 'node:crypto'
import { dirname, join, resolve, relative } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..', '..')
const CHECK = process.argv.includes('--check')
const BASE = 'https://knowtomigrate.web.app'
const DOWNLOAD_DIR = join(ROOT, 'apps/website/public/download')

const version = JSON.parse(readFileSync(join(ROOT, 'version.json'), 'utf8'))
const v = version.version
const vc = version.versionCode
const releaseDate = version.publishedAt.slice(0, 10)

// Public file name (what users and the apps request) -> stored file (served via a rewrite).
if (process.argv.includes('--literals')) {
  // Needs no binaries, so main-branch CI can run it. Fails only on hardcoded release versions.
  const VL = /KnowToMigrate[\s-]?\d+\.\d+\.\d+|-\d+\.\d+\.\d+\.(apk|exe|msi)\b/
  const sources = []
  const collect = (dir) => {
    for (const name of readdirSync(dir)) {
      const p = join(dir, name)
      if (statSync(p).isDirectory()) { if (!['node_modules', 'generated', 'dist'].includes(name)) collect(p) }
      else if (/\.(tsx?|jsx?)$/.test(name)) sources.push(p)
    }
  }
  collect(join(ROOT, 'apps/website/src'))
  const hits = []
  for (const f of sources) readFileSync(f, 'utf8').split('\n').forEach((l, i) => { if (VL.test(l)) hits.push(`${relative(ROOT, f)}:${i + 1}: ${l.trim()}`) })
  if (hits.length) {
    console.error('Hardcoded release version in website source. Use RELEASE from src/generated/release.ts:')
    for (const h of hits) console.error('  ' + h)
    process.exit(1)
  }
  console.log('No hardcoded release versions in website source.')
  process.exit(0)
}

const ARTIFACTS = {
  winSetup: { raw: 'KnowToMigrate-Setup.exe' },
  winMsi:   { raw: `KnowToMigrate-${v}-x64.msi` },
  winExe:   { raw: `KnowToMigrate-${v}-x64.exe` },
  apk:      { raw: `KnowToMigrate-${v}.apk` },
}

function describe(a) {
  const file = join(DOWNLOAD_DIR, `${a.raw}.bin`)
  if (!existsSync(file)) {
    throw new Error(`Missing artifact ${relative(ROOT, file)}. Build it and copy it into the download folder first.`)
  }
  const bytes = readFileSync(file)
  return {
    raw:       a.raw,
    bin:       `${a.raw}.bin`,
    path:      `/download/${a.raw}`,
    url:       `${BASE}/download/${a.raw}`,
    sizeBytes: bytes.length,
    size:      `${(bytes.length / 1048576).toFixed(1)} MB`,
    sha256:    createHash('sha256').update(bytes).digest('hex'),
  }
}

const win = {
  setup: describe(ARTIFACTS.winSetup),
  msi:   describe(ARTIFACTS.winMsi),
  exe:   describe(ARTIFACTS.winExe),
}
const apk = describe(ARTIFACTS.apk)

// Merge a patch into an existing object so keys this script does not own are preserved.
function merge(target, patch) {
  for (const [k, val] of Object.entries(patch)) {
    if (val && typeof val === 'object' && !Array.isArray(val) && target[k] && typeof target[k] === 'object') {
      merge(target[k], val)
    } else {
      target[k] = val
    }
  }
  return target
}

function readJsonOr(path, fallback) {
  const full = join(ROOT, path)
  return existsSync(full) ? JSON.parse(readFileSync(full, 'utf8')) : fallback
}

const outputs = new Map() // relative path -> desired text

// 1. Single update manifest (Windows updater and website).
const manifest = merge(readJsonOr('apps/website/public/update-manifest.json', {}), {
  product:                  'KnowToMigrate',
  channel:                  'stable',
  version:                  v,
  versionCode:              vc,
  minimumSupportedVersion:  version.minimumSupportedVersion,
  publishedAt:              version.publishedAt,
  releaseNotes:             version.releaseNotes,
  windows: {
    architecture:  'x64',
    installerType: 'exe',
    version:       v,
    filename:      win.setup.raw,
    url:           win.setup.url,
    size:          win.setup.sizeBytes,
    sha256:        win.setup.sha256,
    msi:           { filename: win.msi.raw, url: win.msi.url, size: win.msi.sizeBytes, sha256: win.msi.sha256 },
    standalone:    { filename: win.exe.raw, url: win.exe.url, size: win.exe.sizeBytes, sha256: win.exe.sha256 },
  },
  android: {
    versionCode: vc,
    versionName: v,
    filename:    apk.raw,
    url:         apk.url,
    size:        apk.sizeBytes,
    sha256:      apk.sha256,
  },
})
// Remove stale legacy fields the script used to write
delete manifest.updateTimeText
delete manifest.mandatory
outputs.set('apps/website/public/update-manifest.json', JSON.stringify(manifest, null, 4) + '\n')

// 2. Android self-updater manifest (read first by the app).
const stable = merge(readJsonOr('apps/website/public/updates/android/stable.json', {}), {
  product:      'KnowToMigrate',
  platform:     'android',
  channel:      'stable',
  versionCode:  vc,
  versionName:  v,
  apkUrl:       apk.url,
  sizeBytes:    apk.sizeBytes,
  sha256:       apk.sha256,
  releaseDate,
  title:        `KnowToMigrate ${v}`,
  releaseNotes: version.releaseNotes,
})
outputs.set('apps/website/public/updates/android/stable.json', JSON.stringify(stable, null, 4) + '\n')

// 3. Website data: the only place the site gets version, names, sizes and hashes.
function entry(id, a, role) {
  return { id, role, filename: a.raw, path: a.path, version: v, size: a.size, sizeBytes: a.sizeBytes, sha256: a.sha256 }
}
const releaseTs = `// AUTO-GENERATED by scripts/release/site-release.mjs — do not edit by hand.\nexport const RELEASE = ${JSON.stringify({
  version:      v,
  versionCode:  vc,
  publishedAt:  version.publishedAt,
  releaseDate,
  releaseNotes: version.releaseNotes,
  windows: [
    entry('win-setup',    win.setup, 'installer'),
    entry('win-msi',      win.msi,   'enterprise'),
    entry('win-portable', win.exe,   'portable'),
  ],
  android: [entry('android-apk', apk, 'apk')],
}, null, 2)} as const\n`
outputs.set('apps/website/src/generated/release.ts', releaseTs)

// 4. Firebase: rewrites and headers for the current version only.
const fb = JSON.parse(readFileSync(join(ROOT, 'firebase.json'), 'utf8'))
const downloads = [win.setup, win.msi, win.exe, apk]
fb.hosting.rewrites = [
  ...downloads.map((a) => ({ source: a.path, destination: `/download/${a.bin}` })),
  { source: '**', destination: '/index.html' },
]
// Headers this script owns are removed before being regenerated (idempotent).
const MANAGED_SOURCES = ['/update-manifest.json', '/updates/**', '/index.html']
const keptHeaders = (fb.hosting.headers || []).filter(
  (h) => !h.source.startsWith('/download/') && !MANAGED_SOURCES.includes(h.source),
)
const downloadHeaders = downloads.map((a) => ({
  source: `${a.path}*`,
  headers: [
    ...(a.raw.endsWith('.apk') ? [{ key: 'Content-Type', value: 'application/vnd.android.package-archive' }] : []),
    { key: 'Content-Disposition', value: `attachment; filename="${a.raw}"` },
    { key: 'Cache-Control',       value: 'public, max-age=31536000, immutable' },
  ],
}))
const noCache = 'no-cache, must-revalidate'
fb.hosting.headers = [
  ...keptHeaders,
  ...downloadHeaders,
  { source: '/update-manifest.json', headers: [{ key: 'Cache-Control', value: noCache }] },
  { source: '/updates/**',           headers: [{ key: 'Cache-Control', value: noCache }] },
  { source: '/index.html',           headers: [{ key: 'Cache-Control', value: 'no-cache' }] },
]
outputs.set('firebase.json', JSON.stringify(fb, null, 2) + '\n')

// Write, or compare in --check mode.
const stale = []
for (const [rel, text] of outputs) {
  const full = join(ROOT, rel)
  if (CHECK) {
    const current = existsSync(full) ? readFileSync(full, 'utf8') : ''
    if (current !== text) stale.push(rel)
  } else {
    mkdirSync(dirname(full), { recursive: true })
    writeFileSync(full, text)
    console.log(`wrote ${rel}`)
  }
}

// Website source must not contain a hardcoded release version (outside the generated folder).
const VERSION_LITERAL = /KnowToMigrate[\s-]?\d+\.\d+\.\d+|-\d+\.\d+\.\d+\.(apk|exe|msi)\b/
function walk(dir, out = []) {
  for (const name of readdirSync(dir)) {
    const p = join(dir, name)
    if (statSync(p).isDirectory()) {
      if (name !== 'node_modules' && name !== 'generated') walk(p, out)
    } else if (/\.(tsx?|jsx?)$/.test(name)) out.push(p)
  }
  return out
}
const literalHits = []
for (const file of walk(join(ROOT, 'apps/website/src'))) {
  readFileSync(file, 'utf8').split('\n').forEach((line, i) => {
    if (VERSION_LITERAL.test(line)) literalHits.push(`${relative(ROOT, file)}:${i + 1}: ${line.trim()}`)
  })
}

if (CHECK) {
  if (stale.length) {
    console.error(`Stale generated release files: ${stale.join(', ')}`)
    console.error('Run: node scripts/release/site-release.mjs')
  }
  if (literalHits.length) {
    console.error('Hardcoded release version in website source. Use RELEASE from src/generated/release.ts:')
    for (const h of literalHits) console.error('  ' + h)
  }
  if (stale.length || literalHits.length) process.exit(1)
  console.log(`Release data is up to date for ${v} (${vc}).`)
} else {
  if (literalHits.length) {
    console.warn('\nWarning: hardcoded release version found in website source. These should import from src/generated/release.ts:')
    for (const h of literalHits) console.warn('  ' + h)
  }
  console.log(`\nDone. Release ${v} (${vc}) — all generated files written.`)
}
