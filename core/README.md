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

## ktm_ffi.dll Status

The precompiled binary `ktm_ffi.dll` is checked into `apps/windows-wpf/` for the following reasons:
1. **Zero Native Build Toolchain Dependency for .NET Developers**: Building the WPF client does not require developers or standard CI runners to install a full Rust/Cargo/LLVM toolchain or MSVC C++ build tools on Windows.
2. **Deterministic Binary Linkage**: Ensures consistent P/Invoke symbols and ABI compatibility with the managed C# interop wrapper across both development and build machines.

### Binary Deletion Policy
**Note:** `apps/windows-wpf/ktm_ffi.dll` and `apps/windows-setup` are preserved per project safety constraints. **Only the repository owner decides on the retention, deletion, or dynamic compilation of checked-in native binaries.**
