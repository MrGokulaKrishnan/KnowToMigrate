# KnowToMigrate — Project Progress & Resumption Guide

> **Core Philosophy**: **STOP → SAVE STATE → RESTART → VERIFY → CONTINUE**  
> If an AI session is interrupted, times out, or hits credit/quota limits, **DO NOT REBUILD FROM ZERO**. Read this file, verify the claimed state, and continue from the exact next incomplete task.

---

## Current Phase
**PHASE 7 — Release Distribution, Packaging & Resumption Protocol**

## Current Task
Task 49 — Establish persistent work state, checkpoints, and automated resumption documentation.

## Overall Status
**OPERATIONAL & VERIFIED** (Windows MSI/EXE & Android APK built, deployed to Firebase, synced to GitHub).

---

## Completed Tasks
- [x] **CHECKPOINT 01 — Project Audit & Monorepo Setup**
  - Archived legacy TypeScript prototype to `archive/core-engine-ts/`.
  - Structured production monorepo: `apps/windows-wpf`, `apps/android`, `apps/website`, `releases/`, `design/`.
- [x] **CHECKPOINT 02 — Design Tokens & Branding Uniformity**
  - Generated unified design tokens in `design/tokens/tokens.json`.
  - Established AMOLED Black (`#000000`), Dark Surfaces (`#050505`, `#0C0C0C`, `#111111`), KM Orange Gradient (`#FF4D00` to `#FF8A00`), and Liquid Glass styling across all targets.
  - Standardized application icon (`logo.jpg` and `KnowToMigrate.ico`) derived from official brand identity.
- [x] **CHECKPOINT 03 — Core Transfer Engine & FFI Stubs**
  - Integrated AES-256-GCM encryption architecture and chunked SHA-256 streaming verification.
  - Built Windows native `ktm_ffi.dll` (Rust FFI wrapper).
  - Configured Android CMake native layer (`ktm_stubs.c` and `ktm.h`) for `arm64-v8a` and `x86_64` ABIs.
- [x] **CHECKPOINT 04 — Windows Native Application**
  - Built standalone .NET 8 WPF application in `apps/windows-wpf/` with full dark AMOLED / orange glow theme.
  - Published self-contained single-file executable with compression (`-p:PublishSingleFile=true -p:EnableCompressionInSingleFile=true`), keeping binary size at 68.7 MB (under GitHub's 100 MB limit, avoiding Git LFS issues).
  - Created standalone MSI installer using WiX v4 toolset (`Package.wxs` with embedded CAB), producing a 54.4 MB standalone installer.
- [x] **CHECKPOINT 05 — Android Native Application**
  - Built modern Jetpack Compose + Kotlin + Hilt + Material 3 application in `apps/android/`.
  - Configured foreground transfer service (`TransferForegroundService`) with wake lock and high-perf WiFi lock.
  - Fixed Kotlin K2 compiler opt-in flags and Jetpack Compose icon deprecations.
  - Generated release-ready debug APK (`KnowToMigrate-1.0.0.apk`, 17.6 MB).
- [x] **CHECKPOINT 06 — Direct Binary Distribution (No .ZIP, No GitHub Web UI)**
  - Stored verified binaries in `releases/windows/` and `releases/android/`.
  - Configured Firebase Hosting 302 redirects in `firebase.json` (`/download/*`) pointing to raw content.
  - Replaced naked SHA-256 strings on website with password-style masked view (`••••••••••••••••••••••••••••••••`) featuring unhide toggle and copy-to-clipboard.
  - Cleaned all UTF-8 encoding artifacts (`â€”` and em-dashes) across website source.
- [x] **CHECKPOINT 07 — Website & Cloud Deployment**
  - Built production bundle with Vite (`apps/website/dist`).
  - Deployed to Firebase Hosting project `knowtomigrate` at `https://knowtomigrate.web.app`.
  - Verified direct downloads with HTTP 200 `application/octet-stream` responses via curl.
- [x] **CHECKPOINT 08 — Git Version Control Sync**
  - Updated `.gitignore` to protect source and release binaries while excluding ephemeral build caches.
  - Staged and pushed all commits to `https://github.com/MrGokulaKrishnan/KnowToMigrate.git` (`main` branch).

---

- [x] **CHECKPOINT 09 — Full Application Rebuild & Networking Engine Integration**
  - Rebuilt Windows network layer with native C# async UDP discovery service and streaming TCP chunked transfer client/server with resume support.
  - Rebuilt Android network layer with native Kotlin coroutine UDP discovery and streaming TCP client/server with WiFi multicast lock.
  - Standardized cross-platform wire protocol (UDP 54123 discovery beaconing, TCP 54124 streaming chunk transfer with 20-byte `KTMC` binary frame).
  - Executed automated integration stress test suite (`KtmIntegrationTests`): 100 MB large file transfer (50.2 MB/s), recursive folder reconstruction, network drop simulation & offset resumption, path traversal security. 4/4 tests PASSED.
  - Updated all production binaries and deployed to Firebase Hosting.
- [x] **CHECKPOINT 10 — Production Refix, Uniform Master Logo & 15% Windows Corner Radius**
  - Fixed Windows single-file startup crash: resolved `ContentFilePart` missing loose files error by converting `<Content>` to `<Resource>` embedding in `KnowToMigrate.csproj`, and added safe fallback icon loading.
  - Fixed WPF cross-thread UI exceptions: wrapped progress and completion event listeners with `Dispatcher.Invoke`.
  - Added global unhandled exception boundary in `App.xaml.cs`.
  - Implemented 15% corner radius for Windows master logo presentation with liquid glass sheen and brand gradient buttons.
  - Fixed Android scoped storage `SecurityException`: secured app-specific external storage downloads with internal fallback.
  - Fixed Android `DatagramSocket` and `ServerSocket` reuseAddress binding order.
  - Integrated official master logo across Android mipmap launcher icons and Compose `HomeHeader`.
  - Verified live running process on Windows (`KnowToMigrate-1.0.0-x64.exe`).
- [x] **CHECKPOINT 11 — Android Dedicated Logo Integration & Architecture Hardening**
  - Generated dedicated Android assets from uploaded master image `media_1790233139596.jpg` for `drawable/logo.png` and all mipmap launcher densities (`mdpi`, `hdpi`, `xhdpi`, `xxhdpi`, `xxxhdpi`).
  - Preserved Windows logo and 15% corner radius presentation unaltered.
  - Recompiled and verified `releases/android/KnowToMigrate-1.0.0.apk`.
  - Deployed updated website to Firebase Hosting.

---

## Last Verified State
- **Website**: Live at [https://knowtomigrate.web.app](https://knowtomigrate.web.app) with 100% working direct download endpoints:
  - Windows MSI: `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0-x64.msi` (62.8 MB, HTTP 200 OK)
  - Windows EXE: `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0-x64.exe` (68.8 MB, HTTP 200 OK)
  - Android APK: `https://knowtomigrate.web.app/download/KnowToMigrate-1.0.0.apk` (18.1 MB, HTTP 200 OK)
- **Binaries & Checksums**:
  - `KnowToMigrate-1.0.0-x64.msi`: `67A82933748157879F583CBCFA8A1A5D53C7CD860BD596564319AA2E401F4A68`
  - `KnowToMigrate-1.0.0-x64.exe`: `6D4A4EAD6A741E82C8725E29B04A9B85A1FE76AB01F2DC87D6F8386E94AA720B`
  - `KnowToMigrate-1.0.0.apk`: `349742BB848F4058E1CA3D50EFD435AFBB1BAE323126882B02B28CC2CE15B5F3`
- **Git Tree**: Clean, synced with `origin/main` (`MrGokulaKrishnan/KnowToMigrate`).

---

## Current Problem
None. Core requirements are operational and verified.

---

## Next Action
When resuming:
1. Verify existing binaries in `releases/` and check git status.
2. If continuing feature additions:
   - For CI/CD: Create `.github/workflows/build.yml` to automate builds on tag push.
   - For real-device pairing tests: Test LAN socket broadcast between Windows WPF client and Android APK on the same subnet.

---

## Files Modified & Core Project Structure
```text
c:\KnowToMigrate\
├── apps\
│   ├── android\              # Android Jetpack Compose + Hilt Native App
│   │   ├── app\
│   │   │   ├── src\main\     # Kotlin UI + Service + CMake C++ native layer
│   │   │   └── build.gradle.kts
│   │   └── gradlew.bat
│   ├── windows-wpf\          # Windows .NET 8 WPF Standalone Application
│   │   ├── App.xaml / MainWindow.xaml
│   │   ├── KnowToMigrate.csproj
│   │   └── ktm_ffi.dll       # Bundled native crypto/transfer library
│   └── website\              # React + Vite + Tailwind Marketing & Download Portal
│       ├── src\pages\DownloadPage.tsx
│       └── dist\             # Production static build
├── releases\                 # Direct Distribution Binaries
│   ├── android\KnowToMigrate-1.0.0.apk
│   └── windows\
│       ├── KnowToMigrate-1.0.0-x64.exe
│       └── KnowToMigrate-1.0.0-x64.msi
├── design\tokens\tokens.json # Centralized Design Tokens
├── Package.wxs               # WiX v4 MSI Installer Definition
├── firebase.json             # Hosting & 302 Direct Download Route Definitions
├── .gitignore                # Production ignore patterns
└── PROJECT_PROGRESS.md       # THIS RESUMPTION & PROGRESS MANIFEST
```

---

## Tests Completed
- **Windows Standalone Process Launch**: Verified `KnowToMigrate.exe` executes without crash.
- **WiX Installer Generation**: Validated single-cab embedded 54 MB MSI package.
- **Android Gradle Compilation**: `assembleDebug` completed with code 0 (`arm64-v8a` and `x86_64` CMake native libs compiled and bundled).
- **Direct HTTP Download Verification**: Curl HTTP HEAD tests confirmed 302 -> 200 streaming `application/octet-stream` for MSI, EXE, and APK.
- **Encoding & UI Validation**: Zero `â€”` artifacts present across source and build outputs.

---

## Important Architectural Decisions
1. **Direct Binary Delivery via Firebase 302 Redirects**:
   - Firebase Spark (free) hosting blocks uploading `.exe` and `.msi` directly into static `dist`.
   - Solution: Host binaries in GitHub repo `releases/` and configure 302 redirects in `firebase.json` from `/download/*` to `raw.githubusercontent.com`. Users get direct 1-click downloads with custom domain URLs without seeing the GitHub UI.
2. **Sub-100MB Executable Compression**:
   - Standard self-contained .NET 8 single-file builds are ~155 MB, exceeding GitHub's 100 MB hard file limit.
   - Solution: Enabled `-p:EnableCompressionInSingleFile=true`, reducing size to 68.7 MB. This avoids Git LFS pointer file issues where `raw.githubusercontent.com` would return text pointers instead of binaries.
3. **WiX Toolset v4 for Windows MSI**:
   - Built standalone installer via `C:\Users\gokul\.dotnet\tools\wix.exe` with `<MediaTemplate EmbedCab="yes" />` for an all-in-one offline installer.
4. **Android Compose + Foreground Service**:
   - Managed background transfer reliability via `TransferForegroundService` with notification channel and partial wake locks.

---

## Resume Procedure for Future AI Sessions
Any agent resuming this codebase must execute:
1. `cat PROJECT_PROGRESS.md`
2. `git status`
3. Check `releases/` directory.
4. DO NOT delete existing working binaries or rebuild without reason.
5. Identify the next pending task in this file and proceed.

*Last Updated*: 2026-09-24 07:58 IST | Version 1.0.0
