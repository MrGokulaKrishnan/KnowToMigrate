# KnowToMigrate — Development & Production Status

**Last Updated:** 01 October 2026  
**Status:** PRODUCTION READY (Engine, UI, Motion, Packaging) · AWAITING COMMERCIAL CODE-SIGNING CERTIFICATE

---

## 1. Project Overview

| Attribute | Value |
| :--- | :--- |
| **Project Name** | KnowToMigrate |
| **Tagline** | Move Anything. Anywhere. Seamlessly. |
| **Transfer Engine** | Pluto Engine (Pluto v2 Multi-Transport) |
| **Current Version** | 1.0.0 |
| **Official Website** | [https://knowtomigrate.web.app](https://knowtomigrate.web.app) |
| **Hosting Platform** | Firebase Hosting (Project ID: `knowtomigrate`) |
| **Repository** | `MrGokulaKrishnan/KnowToMigrate` (`main` branch) |

---

## 2. Platform Architecture & Build Systems

### Android Application
* **Framework:** Kotlin + Jetpack Compose (Material3 + Custom Liquid Glass Design System)
* **SDK:** Target SDK 35, Compile SDK 35, Min SDK 26 (Android 8.0+)
* **Build System:** Gradle 8.7 with Android Gradle Plugin 8.7.0 & Kotlin 2.0.21
* **Packaging:** Single universal release APK (`KnowToMigrate-1.0.0.apk`, ~11.0 MB)
* **Key Integrations:**
  * System Sharesheet integration (`ACTION_SEND` and `ACTION_SEND_MULTIPLE`) via `content://` URIs and `ClipData` with automatic `FLAG_GRANT_READ_URI_PERMISSION` handling.
  * Direct flow: Gallery $\rightarrow$ Share $\rightarrow$ KnowToMigrate $\rightarrow$ Pre-loaded Launch Transfer screen $\rightarrow$ Select Target $\rightarrow$ Transfer (zero manual file re-selection).
  * 10-Phase Cinematic Hardware-Accelerated Startup Motion Graphics on AMOLED `#000000`.

### Windows Application
* **Framework:** WPF (.NET 8.0, `win-x64`, C# 12)
* **Window Styling:** Custom WindowChrome with native hardware acceleration, 15% corner radius master logo badge, AMOLED Liquid Glass cards, responsive layout.
* **Transfer Server & Discovery:** Background TCP server (port 54124) and UDP discovery service (port 54123).
* **Transfer Acceptance:** Native Liquid Glass dialog (`ModalIncomingTransfer`) with device identity, grouped PIN (`354 446`), connection verification indicator, and `[Accept Transfer]` / `[Decline]` actions (legacy Win32 `MessageBox` eliminated).

### Windows Packaging & Installer Technology
* **Standalone Executable:** Single-file self-contained compressed binary (`KnowToMigrate.exe`, ~68.9 MB compressed payload).
* **Enterprise MSI Package:** Built with WiX Toolset v4 (`Package.wxs`) targeting `ProgramFiles64Folder` (`KnowToMigrate-1.0.0-x64.msi`, 63.0 MB).
* **Setup Installer Bootstrapper:** Built with .NET C# compiler (`apps/windows-setup/SetupBootstrapper.cs`) embedding the WiX MSI (`KnowToMigrate-Setup.exe`, 63.0 MB).
* **Shell Registration:** Start Menu shortcut (`Programs -> KnowToMigrate`), Desktop shortcut, and `App Paths` registration.

---

## 3. Current Security & Signing Status

```
============================================================
CODE SIGNING STATUS: BLOCKED — COMMERCIAL CERTIFICATE REQUIRED
============================================================
```

### Observed Windows Security Events:
1. **Windows Defender SmartScreen:**
   * *Message:* "Microsoft Defender SmartScreen prevented an unrecognized app from starting. Publisher: Unknown publisher."
   * *Root Cause:* The installer binary is currently distributed without an EV or OV Authenticode digital certificate issued by a public Microsoft-trusted Certificate Authority (CA) such as DigiCert, Sectigo, or GlobalSign.
2. **Web Browser Download Alert:**
   * *Message:* "Suspicious download blocked" (Chromium / Edge / Chrome).
   * *Root Cause:* Chromium's download reputation protection flags newly generated binary executables hosted on newly registered domains until sufficient positive download telemetry is accumulated or the file is signed with a high-reputation EV code-signing certificate.

---

## 4. Release Engineering & Action Plan

1. **Maintain Integrity Verification:**
   * All binary releases publish authoritative SHA-256 checksums on [`https://knowtomigrate.web.app/download`](https://knowtomigrate.web.app/download).
   * Users can verify binary integrity using PowerShell: `Get-FileHash -Algorithm SHA256 KnowToMigrate-Setup.exe`.
2. **Commercial Code Signing Acquisition:**
   * Follow the complete steps in `RELEASE_SIGNING.md` to acquire and apply an Authenticode certificate with RFC 3161 timestamping.
3. **Distribution Standards:**
   * All binary downloads use HTTPS with `Content-Type: application/octet-stream` and `Content-Disposition: attachment`.
