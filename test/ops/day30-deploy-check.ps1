# Day 30 - deployment config validation without starting containers
param(
    [string]$ProjectRoot = (Resolve-Path (Join-Path $PSScriptRoot "..\..")).Path,
    [switch]$SkipMaven
)

$ErrorActionPreference = "Stop"

function Require-File([string]$Path) {
    if (-not (Test-Path $Path)) {
        throw "Missing required file: $Path"
    }
    Write-Host "OK file: $Path" -ForegroundColor Green
}

function Require-Command([string]$Name) {
    $cmd = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $cmd) {
        throw "Missing required command: $Name"
    }
    Write-Host "OK command: $Name" -ForegroundColor Green
}

Write-Host "=== Day 30 Deploy Config Check ===" -ForegroundColor Cyan
Write-Host "ProjectRoot=$ProjectRoot"

$deployDir = Join-Path $ProjectRoot "deploy"
$backendDir = Join-Path $ProjectRoot "auction-backend"

Require-File (Join-Path $backendDir "Dockerfile")
Require-File (Join-Path $backendDir ".dockerignore")
Require-File (Join-Path $backendDir "auction-admin\src\main\resources\application-prod.yml")
Require-File (Join-Path $deployDir "docker-compose.yml")
Require-File (Join-Path $deployDir "docker-compose.middleware.yml")
Require-File (Join-Path $deployDir ".env.example")
Require-File (Join-Path $deployDir "nginx\nginx.conf")
Require-File (Join-Path $deployDir "nginx\conf.d\auction.conf")

Require-Command "docker"

Write-Host "`n=== docker compose config ===" -ForegroundColor Cyan
Push-Location $deployDir
try {
    docker compose --env-file .env.example -f docker-compose.yml config | Out-Null
    Write-Host "OK docker-compose.yml config" -ForegroundColor Green

    docker compose --env-file .env.example -f docker-compose.middleware.yml config | Out-Null
    Write-Host "OK docker-compose.middleware.yml config" -ForegroundColor Green
}
finally {
    Pop-Location
}

if (-not $SkipMaven) {
    Require-Command "mvn"
    Write-Host "`n=== Maven package check ===" -ForegroundColor Cyan
    Push-Location $backendDir
    try {
        mvn -pl auction-admin -am package -DskipTests -q
        Write-Host "OK Maven package" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
}
else {
    Write-Host "Skip Maven package check" -ForegroundColor Yellow
}

Write-Host "`nAll deploy checks passed." -ForegroundColor Green
