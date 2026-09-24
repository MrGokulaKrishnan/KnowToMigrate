# KnowToMigrate - LAN Verification and Device Pairing Assistant
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " KnowToMigrate - Local Area Network and Pairing Diagnostics" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Detect IPv4 Addresses
$ips = Get-NetIPAddress -AddressFamily IPv4 | Where-Object { 
    $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" 
}

Write-Host ""
Write-Host "[1] Local Network Adapters and IPv4 Addresses:" -ForegroundColor Yellow
foreach ($ip in $ips) {
    Write-Host "  * Interface: $($ip.InterfaceAlias.PadRight(20)) IP: $($ip.IPAddress)" -ForegroundColor White
}

# 2. Check KnowToMigrate Running Process
$proc = Get-Process -Name "KnowToMigrate" -ErrorAction SilentlyContinue
Write-Host ""
Write-Host "[2] Windows Application Process:" -ForegroundColor Yellow
if ($proc) {
    $ws = [math]::Round($proc.WorkingSet64 / 1MB, 1)
    Write-Host "  OK: KnowToMigrate is ACTIVE (PID: $($proc.Id), Memory: $ws MB)" -ForegroundColor Green
} else {
    Write-Host "  NOTE: KnowToMigrate is not currently running." -ForegroundColor DarkYellow
    Write-Host "  Launch it via: .\releases\windows\KnowToMigrate-1.0.0-x64.exe" -ForegroundColor Gray
}

# 3. Port Listeners Check
Write-Host ""
Write-Host "[3] Network Port Binding Status:" -ForegroundColor Yellow
$udpListener = Get-NetUDPEndpoint -LocalPort 54123 -ErrorAction SilentlyContinue
$tcpListener = Get-NetTCPConnection -LocalPort 54124 -State Listen -ErrorAction SilentlyContinue

if ($udpListener) {
    Write-Host "  OK: UDP Port 54123 (Discovery) is BOUND" -ForegroundColor Green
} else {
    Write-Host "  INFO: UDP Port 54123 will bind when KnowToMigrate starts discovery." -ForegroundColor Gray
}

if ($tcpListener) {
    Write-Host "  OK: TCP Port 54124 (Transfer) is LISTENING" -ForegroundColor Green
} else {
    Write-Host "  INFO: TCP Port 54124 will listen when KnowToMigrate is running." -ForegroundColor Gray
}

# 4. Instructions for Android Pairing
$wifiItem = $ips | Where-Object { $_.InterfaceAlias -like "*Wi-Fi*" } | Select-Object -First 1
$primaryIp = if ($wifiItem) { $wifiItem.IPAddress } else { ($ips | Select-Object -First 1).IPAddress }

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " HOW TO TEST ANDROID <-> WINDOWS FILE TRANSFER:" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "1. Ensure both your PC and Android phone are connected to the SAME Wi-Fi network."
Write-Host "2. On Android:"
Write-Host "   - Install 'KnowToMigrate-1.0.0.apk' (from https://knowtomigrate.web.app)"
Write-Host "   - Open the app and grant Storage / Nearby Wi-Fi permissions."
Write-Host "3. Auto-Discovery:"
Write-Host "   - If your Wi-Fi router allows broadcast packets, the Windows PC and phone"
Write-Host "     will automatically appear in each other's 'Nearby Devices' list."
Write-Host "4. Direct Connect Fallback (Guaranteed to work even on isolated Wi-Fi):"
Write-Host "   - On Android: Go to 'Send Files' -> scroll down to 'Direct Connect by IP'"
Write-Host "     Enter PC IP: $primaryIp" -ForegroundColor Green
Write-Host "   - On Windows: Under 'Or Connect directly by IP', enter the Android phone's IP"
Write-Host "     (visible in Android Wi-Fi settings or in KnowToMigrate Android settings)."
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""
