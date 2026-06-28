param(
    [string]$Server = "root@116.203.133.236",
    [switch]$FeOnly,
    [switch]$BeOnly
)

$ErrorActionPreference = "Stop"
$Root = $PSScriptRoot

function Step($msg) { Write-Host "`n==> $msg" -ForegroundColor Cyan }
function Ok($msg)   { Write-Host "    $msg" -ForegroundColor Green }
function Fail($msg) { Write-Host "ERRORE: $msg" -ForegroundColor Red; exit 1 }

$buildFe = -not $BeOnly
$buildBe = -not $FeOnly

# ---------- BACKEND ----------
if ($buildBe) {
    Step "Build backend (mvn clean package)..."
    Push-Location "$Root\backend"
    mvn clean package -q
    if ($LASTEXITCODE -ne 0) { Pop-Location; Fail "Maven build fallita" }
    Pop-Location
    Ok "Backend compilato"
}

# ---------- FRONTEND ----------
if ($buildFe) {
    Step "Build frontend (npm run build)..."
    Push-Location "$Root\frontend"
    npm run build
    if ($LASTEXITCODE -ne 0) { Pop-Location; Fail "npm build fallita" }
    Pop-Location
    Ok "Frontend compilato"
}

# ---------- DEPLOY FRONTEND ----------
if ($buildFe) {
    Step "Upload frontend → $Server..."
    ssh -p 22 $Server "find /opt/gdrsheet/frontend/dist -mindepth 1 -delete"
    if ($LASTEXITCODE -ne 0) { Fail "Pulizia cartella remota fallita" }

    scp -P 22 -r "$Root\frontend\dist\." "${Server}:/opt/gdrsheet/frontend/dist/"
    if ($LASTEXITCODE -ne 0) { Fail "SCP frontend fallito" }

    ssh -p 22 $Server "find /opt/gdrsheet/frontend/dist -type d -exec chmod 755 {} +"
    if ($LASTEXITCODE -ne 0) { Fail "chmod fallito" }

    Ok "Frontend deployato"
}

# ---------- DEPLOY BACKEND ----------
if ($buildBe) {
    Step "Upload backend + restart container..."
    scp -P 22 "$Root\backend\target\backend-0.0.1-SNAPSHOT.jar" "${Server}:/opt/gdrsheet/backend/target/app.jar"
    if ($LASTEXITCODE -ne 0) { Fail "SCP backend fallito" }

    ssh -p 22 $Server "DOCKER_API_VERSION=1.43 docker restart gdr_java"
    if ($LASTEXITCODE -ne 0) { Fail "Docker restart fallito" }

    Ok "Backend deployato e riavviato"
}

Write-Host "`nDeploy completato!" -ForegroundColor Green
