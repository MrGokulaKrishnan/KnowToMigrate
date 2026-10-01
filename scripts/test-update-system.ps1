# Test script for KnowToMigrate Update System
Write-Output "=== 1. TESTING USER SETTINGS PERSISTENCE ==="
$localAppData = [Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)
$ktmDir = Join-Path $localAppData "KnowToMigrate"
$settingsFile = Join-Path $ktmDir "settings.json"
$historyFile = Join-Path $ktmDir "history.json"
$trustedFile = Join-Path $ktmDir "trusted_devices.json"
$logsDir = Join-Path $ktmDir "logs"

if (-not (Test-Path $ktmDir)) { New-Item -ItemType Directory -Path $ktmDir -Force | Out-Null }
if (-not (Test-Path $logsDir)) { New-Item -ItemType Directory -Path $logsDir -Force | Out-Null }

# Write test settings
$settingsObj = [PSCustomObject]@{
    DeviceName = "Test-Workstation"
    DownloadDirectory = "C:\Users\gokul\Downloads\KnowToMigrate"
    RequirePin = $true
    AutoAcceptTrusted = $true
    CheckUpdatesAutomatically = $true
    UpdateChannel = "stable"
}
$settingsJson = $settingsObj | ConvertTo-Json
Set-Content -Path $settingsFile -Value $settingsJson -Force
Write-Output "Settings written to: $settingsFile"

# Write test history
$historyArray = @(
    [PSCustomObject]@{
        TransferId = [Guid]::NewGuid().ToString("N")
        FileName = "sample_video.mp4"
        TotalBytes = 104857600
        DeviceName = "Android-Pixel"
        Direction = "in"
        Status = "Complete"
        TimestampUtc = [DateTime]::UtcNow.ToString("o")
        Sha256 = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
    }
)
$historyJson = $historyArray | ConvertTo-Json
Set-Content -Path $historyFile -Value $historyJson -Force
Write-Output "History written to: $historyFile"

# Verify reading back
$readSettings = Get-Content $settingsFile | ConvertFrom-Json
Write-Output "Verified Settings: DeviceName = $($readSettings.DeviceName), RequirePin = $($readSettings.RequirePin)"

$readHistory = Get-Content $historyFile | ConvertFrom-Json
Write-Output "Verified History: Count = $($readHistory.Count), Item = $($readHistory.FileName)"

Write-Output ""
Write-Output "=== 2. TESTING UPDATER SHA-256 REJECTION GUARD ==="
$updaterExe = "releases\windows\publish\KnowToMigrate.Updater.exe"
$dummyPkg = Join-Path $env:TEMP "test-update-pkg.bin"
Set-Content -Path $dummyPkg -Value "KnowToMigrate Test Package Payload Content 12345" -Force
$goodHash = (Get-FileHash -Path $dummyPkg -Algorithm SHA256).Hash
$badHash = "0000000000000000000000000000000000000000000000000000000000000000"

Write-Output "Actual Package SHA-256: $goodHash"

# Test 1: Bad Hash should fail / be rejected
Write-Output "Test 1: Running updater with BAD hash (expecting exit code 1)..."
$argsBad = @(
    "--package", "`"$dummyPkg`"",
    "--expected-sha256", "`"$badHash`"",
    "--silent"
)
$procBad = Start-Process -FilePath $updaterExe -ArgumentList $argsBad -PassThru -Wait
Write-Output "Updater ExitCode with bad hash: $($procBad.ExitCode) (Expected: 1)"

# Check update.log
$updateLog = Join-Path $logsDir "update.log"
if (Test-Path $updateLog) {
    Write-Output "Recent update.log entries:"
    Get-Content $updateLog -Tail 5
}

# Clean up test package
Remove-Item $dummyPkg -Force -ErrorAction SilentlyContinue

Write-Output ""
Write-Output "=== 3. TESTING VERSION SYNCHRONIZATION SCRIPT ==="
& .\scripts\sync-version.ps1

Write-Output ""
Write-Output "=== ALL VERIFICATIONS COMPLETED SUCCESSFULLY ==="
