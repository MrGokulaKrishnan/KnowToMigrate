# KnowToMigrate — Android Release Keystore Generator
Param(
    [string]$KeystorePath = "$PSScriptRoot\..\release-keystore.jks",
    [string]$Alias = "knowtomigrate",
    [string]$ValidityDays = "10000"
)

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " KnowToMigrate — Android Release Keystore Generator" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$Keytool = Get-Command "keytool" -ErrorAction SilentlyContinue
if (-not $Keytool) {
    # Check JAVA_HOME
    if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\keytool.exe")) {
        $Keytool = "$env:JAVA_HOME\bin\keytool.exe"
    } else {
        Write-Error "keytool not found in PATH or JAVA_HOME. Please ensure JDK 17+ is installed."
        exit 1
    }
}

if (Test-Path $KeystorePath) {
    Write-Warning "Keystore already exists at: $KeystorePath"
    $overwrite = Read-Host "Overwrite existing keystore? (y/N)"
    if ($overwrite -ne "y" -and $overwrite -ne "Y") {
        Write-Host "Aborted."
        exit 0
    }
    Remove-Item $KeystorePath -Force
}

Write-Host "Generating release keystore at: $KeystorePath" -ForegroundColor Yellow
Write-Host "Key Alias: $Alias" -ForegroundColor Yellow
Write-Host "Validity: $ValidityDays days" -ForegroundColor Yellow
Write-Host "Please enter secure passwords when prompted." -ForegroundColor Gray

& $Keytool -genkeypair -v `
    -keystore $KeystorePath `
    -alias $Alias `
    -keyalg RSA `
    -keysize 2048 `
    -validity $ValidityDays `
    -dname "CN=KnowToMigrate, OU=Engineering, O=KnowToMigrate Team, L=Global, ST=Global, C=US"

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n✓ Release Keystore successfully created at: $KeystorePath" -ForegroundColor Green
    Write-Host "`nTo build a signed release APK using this keystore:" -ForegroundColor Cyan
    Write-Host "  `$env:KTM_KEYSTORE_PATH = `"$KeystorePath`""
    Write-Host "  `$env:KTM_KEY_ALIAS = `"$Alias`""
    Write-Host "  `$env:KTM_KEYSTORE_PASSWORD = `"<your_password>`""
    Write-Host "  `$env:KTM_KEY_PASSWORD = `"<your_password>`""
    Write-Host "  cd apps\android && .\gradlew.bat assembleRelease"
} else {
    Write-Error "Failed to generate keystore."
}
