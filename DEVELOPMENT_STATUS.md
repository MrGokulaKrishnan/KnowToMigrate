# KnowToMigrate — Development & Production Status

**Last Updated:** 01 October 2026  
**Status:** PRODUCTION READY (Engine, UI, Motion, Packaging, Bug Fix Pass) · AWAITING COMMERCIAL CODE-SIGNING CERTIFICATE

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

## 2. Production Bug Fix Pass (Summary of Resolved Issues)

### Bug 1: Selected File Check/Tick Icon
* **Issue:** The selected-file screen on Android presented a heavy, oversized basic green circle checkmark that looked generic and unbalanced.
* **Resolution:** Implemented `KmSuccessCheckBadge` in [KmComponents.kt](file:///c:/KnowToMigrate/apps/android/app/src/main/java/com/knowtomigrate/app/ui/components/KmComponents.kt) — a 34dp rounded container with emerald green `#0D2818` background, `#22C55E` border, and crisp vector checkmark (`KmCheckIcon`). Replaced in [SendScreen.kt](file:///c:/KnowToMigrate/apps/android/app/src/main/java/com/knowtomigrate/app/ui/screens/SendScreen.kt).

### Bug 2: Transport Pipeline Large Empty Gap
* **Issue:** In `SendScreen.kt`, the transport selector contained a large empty region and uneven button widths, with "Wi-Fi Direct" wrapping into two lines and stretching "Bluetooth" awkwardly.
* **Resolution:** Restructured using a responsive `BoxWithConstraints` layout:
  * **Compact/Mobile Screens ($< 420\,\text{dp}$):** Responsive 2x2 grid (`Adaptive Route` + `Wi-Fi`, `Wi-Fi Direct` + `Bluetooth`) with equal flex weights (`Modifier.weight(1f)`), preventing text wrapping and eliminating the empty vertical void.
  * **Tablet/Desktop Screens ($\ge 420\,\text{dp}$):** Single clean row with equal button heights (40dp) and optical spacing.

### Bug 3: Windows & Download Button Text Clipping
* **Issue:** WPF buttons had fixed `Height="42"` that clipped text ascenders/descenders at higher DPI scaling levels (125%–200%).
* **Resolution:**
  * In WPF [App.xaml](file:///c:/KnowToMigrate/apps/windows-wpf/App.xaml), upgraded `KmPrimaryButton`, `KmGlassButton`, and `KmNavButton` styles to use `MinHeight="42"` with generous padding (`Padding="20,10"` and `Padding="18,10"`), centered text alignment, and removed fixed clipping heights.
  * In [MainWindow.xaml](file:///c:/KnowToMigrate/apps/windows-wpf/MainWindow.xaml), adjusted `BtnTransfer` (`MinHeight="46"`, `Padding="26,10"`), `Change Folder` button (`MinHeight="36"`, `Padding="16,8"`), and transport buttons (`MinHeight="36"`, `Padding="14,6"`).
  * In website [DownloadPage.tsx](file:///c:/KnowToMigrate/apps/website/src/pages/DownloadPage.tsx), applied `min-h-[46px] px-4 py-2.5 leading-snug whitespace-normal break-words` to ensure button labels never clip at any scale.

### Bug 4: Generic Tick Symbols Replaced Across Entire App
* **Issue:** Multiple disparate checkmark characters (Unicode `✓`, `✔`, Material default icons) were scattered across screens.
* **Resolution:** Replaced all checkmarks across Android Compose and Windows WPF with unified vector paths:
  * Android Compose: `KmCheckIcon` and `KmSuccessCheckBadge` used in [SendScreen.kt](file:///c:/KnowToMigrate/apps/android/app/src/main/java/com/knowtomigrate/app/ui/screens/SendScreen.kt) ("Ready", security badge) and [TransferScreen.kt](file:///c:/KnowToMigrate/apps/android/app/src/main/java/com/knowtomigrate/app/ui/screens/TransferScreen.kt) ("SHA-256 Verified", "File Saved", transfer complete badge).
  * Windows WPF: `KmCheckIconGeometry` (`M 4.5,12 L 9.5,17 L 19.5,7`) in [App.xaml](file:///c:/KnowToMigrate/apps/windows-wpf/App.xaml), wired into `PanelCopyFeedback` and completion modal in [MainWindow.xaml](file:///c:/KnowToMigrate/apps/windows-wpf/MainWindow.xaml).

### Bug 5: Windows Title Bar KM Logo vs Lightning Symbol
* **Issue:** The title bar of the Windows application used a lightning bolt path (`⚡`), which violated approved branding.
* **Resolution:** Replaced with the official KnowToMigrate application logo image (`ImgTitleBarLogo`) contained in a 15% corner radius container with `#FF5A00` subtle border and glow, maintaining exact aspect ratio without cropping or stretching.

### Bug 6: Dedicated Transient Incoming Transfer Window
* **Issue:** Incoming transfer dialogs were confined inside `MainWindow`, meaning if a user was working in Chrome, VS Code, or Notepad, they would not see incoming transfer prompts without manually focusing the app.
* **Resolution:** Implemented [IncomingTransferWindow.xaml](file:///c:/KnowToMigrate/apps/windows-wpf/Views/IncomingTransferWindow.xaml) & [IncomingTransferWindow.xaml.cs](file:///c:/KnowToMigrate/apps/windows-wpf/Views/IncomingTransferWindow.xaml.cs):
  * Dedicated transient top-level window shown above the desktop context.
  * Win32 non-intrusive taskbar flashing (`FlashWindowEx` with `FLASHW_TRAY | FLASHW_TIMERNOFG`) without permanently locking `TopMost = true`.
  * Entrance animation (opacity 0 $\rightarrow$ 1, scale 0.96 $\rightarrow$ 1.0 with `CubicEaseOut`).
  * Full queue management (`_requestQueue`) supporting multiple simultaneous requests with pending counter badge.
  * 60-second auto-expiry timeout and graceful sender cancellation handling.

### Window Startup Bounds Clamping
* **Resolution:** In [WindowBoundsManager.cs](file:///c:/KnowToMigrate/apps/windows-wpf/Services/WindowBoundsManager.cs), clamped `targetTop` against `workArea.Top` so the window title bar can never be pushed above the visible screen or opened behind other apps. Added `window.Activate()` to guarantee visibility.

---

## 3. Authoritative Release Artifacts & SHA-256 Hashes

All release binaries have been rebuilt, verified, and published to the website download center:

| Artifact | Platform | Format | Size | SHA-256 Checksum |
| :--- | :--- | :--- | :--- | :--- |
| **`KnowToMigrate-Setup.exe`** | Windows | Bootstrapper Setup Installer | 63.0 MB | `153fdc0c8f583a82df9837574b97557246cb616756bc43ddb06a025f340b2ad5` |
| **`KnowToMigrate-1.0.0-x64.msi`** | Windows | WiX v4 Enterprise MSI | 63.0 MB | `580e23419ad5f4753f9ac8a3fbcfe39e2e198f4367c2e8d6d294c733e5547a09` |
| **`KnowToMigrate.exe`** | Windows | Portable Self-Contained | 68.9 MB | `6293bb1b45259b95f7cb184461e0b451e3431ce5876ad8a56903b771bf0c8bde` |
| **`KnowToMigrate-1.0.0.apk`** | Android | Universal Release APK | 11.0 MB | `35909c8904a169c7f09ac0cc0926877f8000366d07f450d3b3ce862686ab37f5` |

> [!NOTE]
> All binary artifacts are strictly under the 100 MB GitHub repository limit.

---

## 4. Code Signing & Security Verification

```
============================================================
CODE SIGNING STATUS: READY FOR CA CERTIFICATE APPLICATION
============================================================
```

* Detailed instructions for Microsoft Authenticode signing with RFC 3161 timestamps are preserved in `RELEASE_SIGNING.md`.
* Once an OV/EV Code Signing Certificate (`.pfx`) is provisioned, execute `scripts/sign-windows.ps1` to sign `KnowToMigrate.exe`, `KnowToMigrate-1.0.0-x64.msi`, and `KnowToMigrate-Setup.exe`.

---

## 5. Deployment Status

* **Firebase Hosting:** Successfully deployed to `https://knowtomigrate.web.app` (16 files updated including new hashed binaries).
* **Git Status:** All bug fixes and build artifacts committed to `main` branch.
