param(
    [string]$Host = "root@116.203.133.236",
    [string]$RemotePath = "/opt/gdrsheet/frontend/dist",
    [string]$LocalDist = "$PSScriptRoot\frontend\dist"
)

Set-StrictMode -Off
$ErrorActionPreference = "Stop"

Write-Host "==> Build frontend..." -ForegroundColor Cyan
Push-Location "$PSScriptRoot\frontend"
npm run build
if ($LASTEXITCODE -ne 0) { Write-Error "Build fallita"; exit 1 }
Pop-Location

Write-Host "==> Pulizia directory remota..." -ForegroundColor Cyan
ssh -p 22 $Host "rm -rf ${RemotePath} && mkdir -p ${RemotePath}"
if ($LASTEXITCODE -ne 0) { Write-Error "SSH cleanup fallito"; exit 1 }

Write-Host "==> Upload su $Host..." -ForegroundColor Cyan
# Usa "dist/." invece di "dist/*" per evitare problemi col glob di Windows
scp -P 22 -r "${LocalDist}\." "${Host}:${RemotePath}/"
if ($LASTEXITCODE -ne 0) { Write-Error "SCP fallito"; exit 1 }

Write-Host "==> Deploy completato!" -ForegroundColor Green
