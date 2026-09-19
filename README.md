# KNOW TO MIGRATE ⚡
> **"Move Anything. Anywhere. Seamlessly."**

[![License](https://img.shields.io/badge/license-Apache--2.0-orange.svg)](LICENSE)
[![Protocol](https://img.shields.io/badge/protocol-KTM%20v2.0-FF5A00.svg)](docs/PROTOCOL.md)
[![Design](https://img.shields.io/badge/design-Liquid%20Glass%20%2B%20AMOLED-000000.svg)](packages/design-system)

**KnowToMigrate** is an extreme-grade, cross-platform file sharing and complete device migration platform. It unites the frictionless proximity discovery of **Apple AirDrop**, the deep Windows/Android background integration of **Google Quick Share**, and the raw folder and unbounded file streaming of **Blip** with an exclusive **10-Step Smart Device Migration Wizard** and a signature **Liquid Glass & Glossy Electric Orange** visual design system.

---

## Key Features

- ⚡ **Zero-Compression Streaming**: Never buffers entire files into memory; streams 8MB chunks with backpressure. 1TB+ tested.
- 📁 **Deep Folder Structure Preservation**: Recursively preserves directory hierarchies, timestamps, and attributes without requiring ZIP compression.
- 🔁 **Cryptographic Chunk Resume**: Verified chunks are committed to `.ktm-checkpoint.json`. Transfers resume from the exact last verified chunk without re-transmitting previous data.
- 🔒 **Ironclad Security**: TLS 1.3 transport, per-session AES-256-GCM authenticated encryption, and dual-layer SHA-256 Merkle root verification.
- 🛡️ **Path Traversal Sandboxing**: `PathGuard` eliminates directory traversal escapes (`../`) and Windows reserved filename injections (`CON`, `PRN`, `AUX`, `NUL`).
- 🧙‍♂️ **Smart Device Migration Wizard**: Guided 10-step wizard scanning Photos, 4K Videos, Documents, Downloads, Music, and Contacts with pre-flight storage validation.
- 🌐 **Zero-Install Web Receiver**: Drop files to colleagues using only a browser through WebRTC DataChannels and the Web Crypto API.
- 📶 **Dynamic Network Pathing**: Direct Wi-Fi 6 P2P $\to$ Internet P2P (WebRTC STUN) $\to$ Zero-knowledge Encrypted Fallback Relay.

---

## Monorepo Structure

```
KnowToMigrate/
├── packages/
│   ├── protocol-types/    # KTM v2 binary and JSON framed message definitions
│   ├── core-engine/       # Streaming chunker, AES-256-GCM crypto, UDP discovery, TCP server/client
│   └── design-system/     # Liquid Glass tokens, KM orange palette, and animations
├── apps/
│   ├── desktop/           # Windows Desktop app (React 18 + Vite + Tailwind CSS + Electron)
│   ├── android/           # Native Android app (Kotlin + Jetpack Compose + Foreground Service)
│   └── web-receiver/      # Zero-install WebRTC browser receiver
├── relay/                 # Containerized encrypted fallback relay server
├── assets/                # KM logo assets (Monogram, lightning bolts, migration arrows)
└── docs/                  # Architecture, Protocol, and Security specifications
```

---

## Quick Start

### Prerequisites
- Node.js v20+ (Node.js v22 recommended)
- npm v10+

### 1. Install Dependencies
```bash
npm install
```

### 2. Run Automated Verification Tests
```bash
npm test
```

### 3. Run Real End-to-End P2P Transfer Simulation
```bash
node packages/core-engine/dist/tests/run_mock_transfer.js
```
*Spawns an active receiver server and sender client, streams 16MB of chunked test data across local TCP sockets, calculates live speed and ETA, and validates 100% SHA-256 Merkle integrity.*

### 4. Launch Desktop Application (Dev Mode)
```bash
npm --workspace=@knowtomigrate/desktop run dev
```

---

## Brand & Visual System

- **Background**: AMOLED True Black (`#000000`)
- **Accent**: KM Electric Orange (`#FF5A00`), Bright Orange (`#FF6A00`), Amber (`#FF8A00`), Soft Orange (`#FFB066`)
- **Surfaces**: Liquid Glass (`rgba(255, 255, 255, 0.035)`, `backdrop-filter: blur(24px)`, 1px glass border, soft orange rim glow)
- **Actions**: Glossy buttons with 135-degree orange gradients and tactile bevel highlights.

---

## Security & Privacy Policy

1. **Zero Cloud Storage**: Transfers are strictly direct peer-to-peer. In relay mode, data is end-to-end encrypted; relay operators see only random ciphertext.
2. **Explicit Consent**: Unknown devices require interactive acceptance. No background writes occur without confirmation.
3. **Audit Trails**: Transfer history records metadata only (timestamps, byte count, speed, and SHA-256 verification hash). File contents are never indexed or logged.
