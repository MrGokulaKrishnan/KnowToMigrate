# Changelog

All notable changes to the KnowToMigrate application are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.1] - Planned / Next Release
### Added
- Integrated Windows Update Center with automated and manual update checking
- Standalone updater executable (`KnowToMigrate.Updater.exe`) with atomic installation and rollback
- HTTPS Update Manifest verification (`update-manifest.json`)
- Resumable update downloads with live progress, speed (MB/s), and ETA calculation
- Real-time SHA-256 cryptographic package verification before installation
- Pluto Engine active transfer protection preventing restarts during active file migrations
- Settings and transfer history persistence in `%LOCALAPPDATA%\KnowToMigrate`

### Improved
- Memory efficiency during high-speed local Wi-Fi transfers
- Transient incoming transfer notification with taskbar flashing
- Responsive transport pipeline grid on mobile viewports

### Security
- Anti-downgrade validation on incoming update packages
- Restricted temporary update staging directory with secure ACLs

---

## [1.0.0] - 2026-10-01
### Added
- Initial production release of KnowToMigrate for Windows and Android
- Pluto Engine v2 with multi-transport pipeline (Wi-Fi LAN, Wi-Fi Direct, Bluetooth)
- End-to-end encryption with AES-256-GCM and X25519 Diffie-Hellman key agreement
- SHA-256 per-chunk verification with resumable `.ktm-checkpoint` engine
- Android Sharesheet direct integration (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`)
- Windows native Liquid Glass incoming transfer dialog and bounds validation
- Official WiX Toolset v4 MSI installer and self-extracting Setup Bootstrapper
- Product download website with live SHA-256 verification hashes
