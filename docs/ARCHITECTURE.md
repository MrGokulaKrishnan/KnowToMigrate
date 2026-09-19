# KnowToMigrate — System Architecture Documentation
**Version:** 2.0  
**Tagline:** *Move Anything. Anywhere. Seamlessly.*

---

## 1. High-Level Architecture Overview

```
+-------------------------------------------------------------------------+
|                        KNOWTOMIGRATE PLATFORM                           |
+-------------------------------------------------------------------------+
|  User Presentation Layer                                                |
|  - Desktop (Windows): Electron + React 18 + Vite + Tailwind CSS         |
|  - Mobile (Android): Kotlin + Jetpack Compose + Foreground Service      |
|  - Web Receiver: Zero-install WebRTC DataChannel + Web Crypto API       |
+-------------------------------------------------------------------------+
|  Domain & Core Protocol Engine (@knowtomigrate/core-engine)             |
|  - Chunking Engine: 8MB streaming with positional I/O & backpressure    |
|  - Cryptographic Engine: AES-256-GCM + ECDH/X25519 + SHA-256 Merkle     |
|  - State Machine: Event-driven session lifecycle & progress emitter     |
|  - Checkpoint & Resume: Bitset chunk persistence (.ktm-checkpoint.json) |
|  - Path Traversal Guard: Sandbox isolation & Windows reserved handling  |
|  - Smart Migration Scanner: Deep media scanning & preflight headroom    |
+-------------------------------------------------------------------------+
|  Transport & Network Layer                                              |
|  - Local P2P: Direct TCP framing over Wi-Fi 6 / Wi-Fi Direct (Port 54124)|
|  - Discovery: UDP broadcast/multicast beacons (Port 54123) + BLE        |
|  - Internet P2P: WebRTC STUN/ICE traversal                              |
|  - Relay Fallback: Ephemeral encrypted zero-knowledge WebSocket relay   |
+-------------------------------------------------------------------------+
```

---

## 2. Directory Monorepo Layout

- `packages/protocol-types`: Type definitions, message enums, manifests, and checkpoints.
- `packages/core-engine`: High-performance streaming chunker, crypto, discovery, and TCP server/client.
- `packages/design-system`: Liquid Glass AMOLED tokens, gradients, and component styles.
- `apps/desktop`: Full desktop client application with Home, Send, Receive, Transfer, Migration, History, and Settings views.
- `apps/android`: Complete Android native Kotlin project with Share Sheet intent filters and background sync service.
- `apps/web-receiver`: Standalone HTML/TypeScript client for zero-install browser receiving.
- `relay`: Dockerized fallback relay server.
- `docs`: Architecture, protocol, security, and developer guides.
