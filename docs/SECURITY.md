# KnowToMigrate — Security & Threat Model

## 1. Cryptographic Standards

- **Transport Layer**: TLS 1.3 with Perfect Forward Secrecy.
- **Application Payload**: AES-256-GCM authenticated encryption using 96-bit initialization vectors and 128-bit authentication tags.
- **Ephemeral Key Agreement**: X25519 / ECDH (prime256v1) generating per-session ephemeral keys that are destroyed upon transfer completion.
- **Integrity Validation**: Dual-layer SHA-256:
  1. Per-chunk SHA-256 verified prior to writing to disk.
  2. Merkle-tree root hash calculated over all chunks and cross-checked at session termination.

## 2. Threat Mitigations

| Threat | Mitigation Mechanism |
| :--- | :--- |
| **Path Traversal Attacks** (`../../`, root drives) | `PathGuard.sanitizeRelativePath()` strips directory traversal characters, strips Windows drive roots (`C:\`), and enforces strict confinement inside the target sandbox. |
| **Windows Reserved File Injection** (`CON`, `PRN`, `NUL`) | `PathGuard` maps all reserved device names to safe internal prefixes (`_reserved_CON.txt`). |
| **Silent Unknown Transfers** | Unknown devices require explicit, interactive user approval. Auto-accept is strictly disabled for unverified devices. |
| **Replay Attacks** | Every packet contains a 32-character ephemeral session token, monotonic sequence numbers, and CRC32 verification. |
| **Data Corruption on Network Drop** | Partial chunks are rejected. Only chunks with matching SHA-256 hashes are recorded in the checkpoint bitset. |
| **Cloud Eavesdropping** | Direct P2P transfer over local Wi-Fi or WebRTC. In relay mode, data is end-to-end encrypted before entering relay buffers. |
