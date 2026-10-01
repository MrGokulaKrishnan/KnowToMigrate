# KnowToMigrate — Release Code Signing Guide

```
============================================================
STATUS: CODE SIGNING BLOCKED — COMMERCIAL CERTIFICATE REQUIRED
============================================================
```

This document establishes the official production signing procedure for KnowToMigrate Windows artifacts.

---

## 1. Why Code Signing Is Required

When users download Windows executable files from the web:
1. **Microsoft Defender SmartScreen** checks if the binary has a valid digital signature matching a trusted certificate in Microsoft's Root Certificate Program.
2. If unsigned or signed with an unverified/self-signed certificate, Windows marks the publisher as **"Unknown publisher"** and displays a warning screen.
3. Browsers (Chrome, Edge, Brave) also flag downloads without positive reputation telemetry as **"Suspicious download blocked"**.

To establish seamless trust without warnings, binaries must be signed with a legitimate **Authenticode Code Signing Certificate** issued by an approved Certificate Authority (CA).

---

## 2. Certificate Requirements

* **Certificate Type:** Standard OV (Organization Validation) or EV (Extended Validation) Code Signing Certificate.
  * *EV Certificates:* Provide immediate SmartScreen reputation without any ramp-up period. Required by Microsoft for kernel-mode drivers; highly recommended for production consumer applications.
  * *Approved Providers:* DigiCert, Sectigo, GlobalSign, SSL.com, Certum.
* **Format:** Hardware Security Module (HSM), cloud signing key (Azure Key Vault / AWS CloudHSM / DigiCert ONE), or password-protected PKCS#12 (`.pfx`).

> [!CAUTION]
> **NEVER COMMIT PRIVATE KEYS OR PFX FILES TO GIT REPOSITORIES.**
> Code signing keys must be securely stored in CI/CD secrets or HSM tokens.

---

## 3. Signing Procedure with SignTool

Microsoft's `signtool.exe` (included with Windows SDK) is used for Authenticode signing.

### Step 1: Sign Main Executable
```cmd
signtool.exe sign /f "C:\Path\To\Certificate.pfx" /p "<SecretPassword>" /fd SHA256 /tr "http://timestamp.digicert.com" /td SHA256 /d "KnowToMigrate" /du "https://knowtomigrate.web.app" "c:\KnowToMigrate\releases\windows\publish\KnowToMigrate.exe"
```

### Step 2: Sign Enterprise MSI Package
```cmd
signtool.exe sign /f "C:\Path\To\Certificate.pfx" /p "<SecretPassword>" /fd SHA256 /tr "http://timestamp.digicert.com" /td SHA256 /d "KnowToMigrate Windows Installer" /du "https://knowtomigrate.web.app" "c:\KnowToMigrate\releases\windows\KnowToMigrate-1.0.0-x64.msi"
```

### Step 3: Sign Setup Bootstrapper Installer
```cmd
signtool.exe sign /f "C:\Path\To\Certificate.pfx" /p "<SecretPassword>" /fd SHA256 /tr "http://timestamp.digicert.com" /td SHA256 /d "KnowToMigrate Setup" /du "https://knowtomigrate.web.app" "c:\KnowToMigrate\releases\windows\KnowToMigrate-Setup.exe"
```

### Parameters Reference:
* `/fd SHA256`: Specifies the SHA-256 file digest algorithm.
* `/tr http://timestamp.digicert.com`: RFC 3161 compliant timestamp server URL (ensures signature validity even after certificate expiry).
* `/td SHA256`: Uses SHA-256 for the timestamp digest.
* `/d "KnowToMigrate"`: Application description shown in the UAC prompt.
* `/du "https://knowtomigrate.web.app"`: Publisher URL shown in Windows security dialogs.

---

## 4. Automated Signature Verification

Before publishing any signed artifact, verify the signature:

```powershell
# Verify using signtool
signtool.exe verify /pa /v "c:\KnowToMigrate\releases\windows\KnowToMigrate-Setup.exe"

# Or verify using PowerShell Authenticode cmdlet
Get-AuthenticodeSignature "c:\KnowToMigrate\releases\windows\KnowToMigrate-Setup.exe" | Format-List
```

Expected result:
```
Status: Valid
StatusMessage: Signature verified.
Path: KnowToMigrate-Setup.exe
```

---

## 5. CI/CD Automated Workflow (GitHub Actions)

In your repository settings (`Settings -> Secrets and variables -> Actions`), add:
* `WINDOWS_CERT_BASE64`: Base64-encoded content of your `.pfx` certificate.
* `WINDOWS_CERT_PASSWORD`: Secret password for the PFX.

In your GitHub Actions workflow:
```yaml
- name: Authenticode Sign Windows Binaries
  run: |
    $certBytes = [Convert]::FromBase64String("${{ secrets.WINDOWS_CERT_BASE64 }}")
    [IO.File]::WriteAllBytes("temp_cert.pfx", $certBytes)
    & signtool.exe sign /f temp_cert.pfx /p "${{ secrets.WINDOWS_CERT_PASSWORD }}" /fd SHA256 /tr http://timestamp.digicert.com /td SHA256 /d "KnowToMigrate" releases/windows/KnowToMigrate-Setup.exe
    Remove-Item temp_cert.pfx -Force
```

---

## 6. Pre-Signing Safety & Integrity Checklist

- [ ] All build binaries compiled in `Release` configuration.
- [ ] No local debug paths or development credentials in binaries.
- [ ] Binaries tested locally and verified against SHA-256 hashes.
- [ ] SignTool signs with RFC 3161 timestamping.
- [ ] `signtool verify /pa` passes without errors.
- [ ] Final SHA-256 calculated **after** signing (signing modifies binary content).
- [ ] Website download page updated with post-signing SHA-256 hash.
