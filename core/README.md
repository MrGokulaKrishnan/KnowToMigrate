# KnowToMigrate Core (Rust Native Modules)

## Overview
This directory contains the native Rust workspace for KnowToMigrate, providing high-performance cryptographic operations, protocol framing, discovery primitives, and migration utilities.

### Crates
- `ktm-crypto`: Constant-time authenticated hashing, Merkle tree construction, and key agreement primitives.
- `ktm-protocol`: Packet header serialization, framing, CRC32 integrity checksums, and length-prefixed protocol definitions.
- `ktm-discovery`: UDP beacon broadcast and multicast device discovery across local area networks.
- `ktm-transfer`: High-throughput chunk streaming, backpressure control, and socket pump.
- `ktm-migration`: Local disk preflight space calculations, directory tree scanning, and migration batching.
- `ktm-ffi`: C-ABI foreign function interface exposing native functions for consumption by native desktop host runtimes.

---

## Native Engine Architecture

Per architectural decision on October 27, 2026:
1. **Windows WPF**: Runs on 100% native managed C# (.NET 8) with zero-allocation socket streaming and asynchronous pipelines. The unused `ktm_ffi.dll` binary has been retired from `apps/windows-wpf/`, eliminating native P/Invoke dependencies and MinGW linker requirements.
2. **Rust Core (`core/`)**: Retained in the repository as the single-source-of-truth reference implementation for cross-platform unified engine work, targeting standard `x86_64-pc-windows-msvc`.
