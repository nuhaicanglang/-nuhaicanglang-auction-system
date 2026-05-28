# Day 31 - final smoke test for public APIs and optional authenticated checks
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$Username = "",
    [string]$Password = "",
    [switch]$SkipAuth
)

$ErrorActionPreference = "Stop"

function Invoke-Check {
    param(
        [string]$Name,
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [object]$Body = $null,
        [bool]$ExpectBusinessSuccess = $true
    )

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $args = @{
            Uri = $Url
            Method = $Method
            Headers = $Headers
            TimeoutSec = 20
        }
        if ($Body -ne $null) {
            $args.ContentType = "application/json"
            $args.Body = ($Body | ConvertTo-Json -Depth 10)
        }
        $resp = Invoke-RestMethod @args
        $sw.Stop()
        $ok = $true
        if ($ExpectBusinessSuccess -and $resp.PSObject.Properties.Name -contains "code") {
            $ok = ($resp.code -eq 0)
        }
        if ($ok) {
            Write-Host "PASS [$Name] $($sw.ElapsedMilliseconds)ms" -ForegroundColor Green
        }
        else {
            Write-Host "FAIL [$Name] code=$($resp.code) msg=$($resp.msg)" -ForegroundColor Red
            return $false
        }
        return $resp
    }
    catch {
        $sw.Stop()
        Write-Host "FAIL [$Name] $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

Write-Host "=== Day 31 Final Smoke Test ===" -ForegroundColor Cyan
Write-Host "BaseUrl=$BaseUrl"

$failCount = 0

$checks = @(
    @{ Name = "Ping"; Method = "GET"; Url = "$BaseUrl/api/ping" },
    @{ Name = "Actuator Health"; Method = "GET"; Url = "$BaseUrl/actuator/health"; ExpectBusinessSuccess = $false },
    @{ Name = "Category Tree"; Method = "GET"; Url = "$BaseUrl/api/categories/tree" },
    @{ Name = "Item List"; Method = "GET"; Url = "$BaseUrl/api/items?page=1&size=10&status=3" },
    @{ Name = "Search Items"; Method = "GET"; Url = "$BaseUrl/api/search/items?keyword=%E5%9B%BD%E7%94%BB&page=1&size=10&status=3" },
    @{ Name = "Search Suggest"; Method = "GET"; Url = "$BaseUrl/api/search/suggest?prefix=%E5%9B%BD%E7%94%BB&size=5" }
)

foreach ($c in $checks) {
    $expect = $true
    if ($c.ContainsKey("ExpectBusinessSuccess")) { $expect = [bool]$c.ExpectBusinessSuccess }
    $result = Invoke-Check -Name $c.Name -Method $c.Method -Url $c.Url -ExpectBusinessSuccess $expect
    if ($result -eq $false) { $failCount++ }
}

if (-not $SkipAuth) {
    if ([string]::IsNullOrWhiteSpace($Username) -or [string]::IsNullOrWhiteSpace($Password)) {
        Write-Host "SKIP authenticated checks: provide -Username and -Password, or pass -SkipAuth" -ForegroundColor Yellow
    }
    else {
        $login = Invoke-Check -Name "Login" -Method "POST" -Url "$BaseUrl/api/system/users/login" -Body @{ username = $Username; password = $Password }
        if ($login -eq $false) {
            $failCount++
        }
        else {
            $token = $login.data.accessToken
            if (-not $token) { $token = $login.data.token }
            if (-not $token) {
                Write-Host "FAIL [LoginToken] missing token in login response" -ForegroundColor Red
                $failCount++
            }
            else {
                $headers = @{ Authorization = "Bearer $token" }
                $me = Invoke-Check -Name "Current User" -Method "GET" -Url "$BaseUrl/api/system/users/me" -Headers $headers
                if ($me -eq $false) { $failCount++ }

                $wallet = Invoke-Check -Name "My Wallet" -Method "GET" -Url "$BaseUrl/api/me/wallet" -Headers $headers
                if ($wallet -eq $false) { $failCount++ }

                $history = Invoke-Check -Name "Search History" -Method "GET" -Url "$BaseUrl/api/search/history" -Headers $headers
                if ($history -eq $false) { $failCount++ }
            }
        }
    }
}

Write-Host "`n=== Summary ===" -ForegroundColor Cyan
if ($failCount -eq 0) {
    Write-Host "All smoke checks passed." -ForegroundColor Green
    exit 0
}
else {
    Write-Host "$failCount smoke checks failed." -ForegroundColor Red
    exit 1
}
