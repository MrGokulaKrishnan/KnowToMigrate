# KnowToMigrate — Windows Firewall Configuration Script
# Requires Administrator privileges to configure NetSecurity rules

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " KnowToMigrate — Windows Firewall Rule Setup" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) {
    Write-Warning "This script requires Administrator privileges to add Windows Firewall rules."
    Write-Host "Please right-click PowerShell and choose 'Run as Administrator', then run:" -ForegroundColor Yellow
    Write-Host "  powershell -ExecutionPolicy Bypass -File `"$PSCommandPath`"" -ForegroundColor White
    exit 1
}

try {
    # Port 54123 UDP (Discovery Beaconing)
    Write-Host "Configuring UDP Port 54123 (Discovery)..." -ForegroundColor Gray
    Remove-NetFirewallRule -DisplayName "KnowToMigrate Discovery (UDP 54123)" -ErrorAction SilentlyContinue
    New-NetFirewallRule -DisplayName "KnowToMigrate Discovery (UDP 54123)" `
        -Direction Inbound `
        -Protocol UDP `
        -LocalPort 54123 `
        -Action Allow `
        -Profile Private, Domain `
        -Description "Allows KnowToMigrate local peer discovery broadcasts" | Out-Null

    # Port 54124 TCP (High-Speed Chunked Streaming)
    Write-Host "Configuring TCP Port 54124 (Transfer)..." -ForegroundColor Gray
    Remove-NetFirewallRule -DisplayName "KnowToMigrate Transfer (TCP 54124)" -ErrorAction SilentlyContinue
    New-NetFirewallRule -DisplayName "KnowToMigrate Transfer (TCP 54124)" `
        -Direction Inbound `
        -Protocol TCP `
        -LocalPort 54124 `
        -Action Allow `
        -Profile Private, Domain `
        -Description "Allows KnowToMigrate high-speed streaming file transfers" | Out-Null

    Write-Host "`n✓ Firewall rules successfully created for Private & Domain networks!" -ForegroundColor Green
    Write-Host "  - Inbound UDP 54123 (Discovery)" -ForegroundColor Cyan
    Write-Host "  - Inbound TCP 54124 (Transfer Stream)" -ForegroundColor Cyan
} catch {
    Write-Error "Failed to configure firewall: $_"
}
