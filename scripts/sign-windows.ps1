# KnowToMigrate — Windows Authenticode Signing Script
Param(
    [string]$CertPath = $env:KTM_CERT_PATH,
    [string]$CertPassword = $env:KTM_CERT_PASSWORD,
    [string]$TimestampServer = "http://timestamp.digicert.com"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " KnowToMigrate — Windows Authenticode Signing" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$SignTool = Get-Command "signtool.exe" -ErrorAction SilentlyContinue
if (-not $SignTool) {
    # Check Windows Kits paths
    $kits = Get-ChildItem "C:\Program Files (x86)\Windows Kits\10\bin\*\x64\signtool.exe" -ErrorAction SilentlyContinue
    if ($kits) {
        $SignTool = $kits[-1].FullName
    } else {
        Write-Error "signtool.exe not found. Please install the Windows 10/11 SDK or run from Developer Command Prompt."
        exit 1
    }
}

Write-Host "Using SignTool: $SignTool" -ForegroundColor Gray

$Targets = @(
    "$PSScriptRoot\..\releases\windows\KnowToMigrate-1.0.0-x64.exe",
    "$PSScriptRoot\..\releases\windows\KnowToMigrate-1.0.0-x64.msi"
)

if (-not $CertPath -or -not (Test-Path $CertPath)) {
    Write-Warning "No certificate path supplied via -CertPath or `$env:KTM_CERT_PATH."
    Write-Host "To self-sign for internal testing, generate a test PFX cert:" -ForegroundColor Yellow
    Write-Host "  New-SelfSignedCertificate -Type CodeSigningCert -Subject 'CN=KnowToMigrate Team' -CertStoreLocation Cert:\CurrentUser\My"
    Write-Host "To sign production binaries with a PFX certificate:" -ForegroundColor Yellow
    Write-Host "  .\sign-windows.ps1 -CertPath 'C:\path\to\cert.pfx' -CertPassword 'password'"
    exit 0
}

foreach ($target in $Targets) {
    if (Test-Path $target) {
        Write-Host "Signing: $target ..." -ForegroundColor Cyan
        $signArgs = @("sign", "/fd", "SHA256", "/tr", $TimestampServer, "/td", "SHA256", "/f", $CertPath)
        if ($CertPassword) {
            $signArgs += @("/p", $CertPassword)
        }
        $signArgs += $target

        & $SignTool $signArgs
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✓ Successfully signed $target" -ForegroundColor Green
        } else {
            Write-Error "Failed to sign $target"
        }
    } else {
        Write-Warning "Target file not found: $target"
    }
}
