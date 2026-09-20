# KnowToMigrate — Windows App

Native Windows desktop application built with **C# / WinUI 3 / Windows App SDK**.

## Requirements

| Requirement | Version |
|-------------|---------|
| .NET SDK | 8.0+ |
| Windows App SDK | 1.6+ |
| Windows | 10 1809+ (build 17763+) |
| Architecture | x64 |

## Build

### Prerequisites
```powershell
# Install .NET 8 SDK
winget install Microsoft.DotNet.SDK.8

# Install Windows App SDK (via NuGet — handled automatically by dotnet restore)
```

### Build Debug
```powershell
cd apps\windows
dotnet build
```

### Build Release
```powershell
dotnet build -c Release
```

### Run
```powershell
dotnet run
```

### Publish (self-contained single EXE)
```powershell
dotnet publish -c Release -r win-x64 --self-contained true -o ..\..\ releases\windows\
```

## Dependencies

- **ktm.dll** — The KnowToMigrate Rust core library. Must be built from `core/` and placed in the same directory as the EXE.
  ```powershell
  cd core
  cargo build --release -p ktm-ffi
  # Output: core\target\release\ktm.dll
  # Copy to: apps\windows\bin\Release\net8.0-windows10.0.22621.0\win-x64\publish\
  ```

## Architecture

```
apps\windows\
├── App.xaml / App.xaml.cs        — Application entry point
├── MainWindow.xaml / .cs         — Main window with sidebar navigation
├── Interop\
│   └── KtmInterop.cs             — P/Invoke bindings to ktm.dll
├── Services\
│   └── KtmService.cs             — Wrapper over Rust FFI with Observable collections
└── Views\
    ├── HomeView.xaml / .cs       — Drop zone, device radar, recent transfers
    ├── SendView.xaml / .cs       — File picker, device selector, send
    ├── ReceiveView.xaml / .cs    — QR code, incoming request
    ├── TransferView.xaml / .cs   — Active transfer progress
    ├── MigrationView.xaml / .cs  — 9-step migration wizard
    ├── HistoryView.xaml / .cs    — Transfer history
    └── SettingsView.xaml / .cs   — Settings
```

## Design System

The Windows app implements the KnowToMigrate design system:
- **Background**: AMOLED black `#000000`
- **Accent**: KM Orange `#FF5A00`
- **Gradient**: `#FF4D00 → #FF8A00`
- **Cards**: `rgba(255,255,255,0.04)` glass surfaces with `1px rgba(255,255,255,0.08)` borders
- **Typography**: Segoe UI Variable (system font)

## Offline Transfer Test

1. Disconnect internet from both Windows and Android devices
2. Connect both to the same local Wi-Fi
3. Open KnowToMigrate on both
4. Expected: Discovery → Pairing → Transfer → Verification all work without internet ✅
