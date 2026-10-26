# Changelog

All notable changes to the KnowToMigrate application are documented in this file.
The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [1.0.5] - 2026-10-22
### Fixed
- Android APK Installer: Resolved "Can't install app - There's a problem with the app file" installation failure by creating permanent production release keystore, enabling v2/v3 signatures with full 4-byte zip-alignment, and replacing blob-based web downloads with direct native browser streaming on Android.
- Download delivery: Added explicit `application/vnd.android.package-archive` MIME type and `attachment` disposition in `firebase.json` for `.apk` distribution.
- Independent APK Self-Updater: Upgraded APK Downloader to verify SHA-256 and invoke Android `PackageInstaller` session flow cleanly without Google Play dependencies.
- Navigation and Controls: Fixed Android Control Center and action buttons from clipping text; updated to Liquid Glass design.

### Security
- Hardened Windows Web Share server: removed wildcard CORS, enforced per-session cryptographically random tokens, and made clipboard access strictly opt-in.
- Hardened update service: enforced mandatory fail-closed 64-hex SHA-256 verification in both app and updater.
- Hardened receiver validation: enforced per-chunk file index matching, offset and boundary validation against declared file size, and mandatory SHA-256 integrity verification.
- Hardened PathGuard: sanitized Windows reserved device names (CON, PRN, AUX, NUL, COM1-9, LPT1-9) and enforced sandbox root boundary resolution across C# and TypeScript.
- Authenticated WebSocket relay: added secret session tokens, role hijack protection (409 Conflict), 16MB frame limit, and socket backpressure control.
- Replaced all non-cryptographic `Math.random` uses for security and pairing PINs with `crypto.getRandomValues`.

### Improved
- Cleaned download filenames: removed `.bin` suffixes from release packages while maintaining fallback aliases.
- Removed dead Android native JNI stubs (`ktm_stubs.c`).
- Aligned GitHub Actions workflows to dynamically extract versions from `version.json`.
- Configured `npm test` to automatically build dependencies on clean checkouts.

---

## [1.0.1] - 2026-10-02
### Added
- Integrated Windows Update Center with automated and manual update checking
- Standalone updater executable (`KnowToMigrate.Updater.exe`) with atomic installation and rollback
- HTTPS Update Manifest verification (`update-manifest.json`)
- Resumable update downloads with live progress, speed (MB/s), and ETA calculation
- Real-time SHA-256 cryptographic package verification before installation
- Transit Engine active transfer protection preventing restarts during active file migrations
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
- Transit Engine v2 with multi-transport pipeline (Wi-Fi LAN, Wi-Fi Direct, Bluetooth)
- End-to-end encryption with AES-256-GCM and X25519 Diffie-Hellman key agreement
- SHA-256 per-chunk verification with resumable `.ktm-checkpoint` engine
- Android Sharesheet direct integration (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`)
- Windows native Liquid Glass incoming transfer dialog and bounds validation
- Official WiX Toolset v4 MSI installer and self-extracting Setup Bootstrapper
- Product download website with live SHA-256 verification hashes
