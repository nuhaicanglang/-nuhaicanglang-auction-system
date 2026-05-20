# Day 15 - 100 concurrent bid stress test (PS 5.1 compatible)
param(
    [string]$BaseUrl   = "http://localhost:8080",
    [string]$ItemId    = "313631694601076736",
    [int]$UserCount    = 100,
    [string]$Password  = "Test123456",
    [decimal]$StartBid = 110,
    [decimal]$Increment = 10
)

Add-Type -AssemblyName System.Net.Http

# ---- Phase 1: Login ----
Write-Host "=== Phase 1: Login $UserCount users ===" -ForegroundColor Cyan
$tokens = @{}
for ($i = 1; $i -le $UserCount; $i++) {
    $username = "bidder" + $i.ToString("D2")
    $loginBody = '{"username":"' + $username + '","password":"' + $Password + '"}'
    try {
        $resp = Invoke-RestMethod -Uri "$BaseUrl/api/system/users/login" -Method POST -ContentType "application/json" -Body $loginBody -TimeoutSec 10
        if ($resp.code -eq 0) {
            $tk = $resp.data.accessToken
            if (!$tk) { $tk = $resp.data.token }
            if ($tk) { $tokens[$i] = $tk }
        }
        else {
            Write-Host "  WARN $username code=$($resp.code)" -ForegroundColor Yellow
        }
    }
    catch {
        Write-Host "  FAIL $username" -ForegroundColor Red
    }
    if ($i % 20 -eq 0) { Write-Host "  logged in $i / $UserCount ..." }
}
Write-Host "  OK: $($tokens.Count) / $UserCount"
if ($tokens.Count -eq 0) { Write-Host "No tokens, abort" -ForegroundColor Red; exit 1 }

# ---- Phase 2: Concurrent bids ----
Write-Host "`n=== Phase 2: Fire concurrent bids ===" -ForegroundColor Cyan

$handler = New-Object System.Net.Http.HttpClientHandler
$handler.MaxConnectionsPerServer = 200
$httpClient = New-Object System.Net.Http.HttpClient($handler)
$httpClient.Timeout = [TimeSpan]::FromSeconds(60)

$bidUrl = "$BaseUrl/api/items/$ItemId/bids"
$taskMap  = @{}
$timerMap = @{}
$priceMap = @{}

$sw = [System.Diagnostics.Stopwatch]::StartNew()

foreach ($idx in $tokens.Keys) {
    $p = $StartBid + ($idx - 1) * $Increment
    $priceMap[$idx] = $p
    $idem = [guid]::NewGuid().ToString()

    $msg = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Post, $bidUrl)
    $msg.Headers.Add("Authorization", "Bearer $($tokens[$idx])")
    $msg.Headers.Add("X-Idempotent-Key", $idem)
    $json = '{"price":' + $p.ToString() + '}'
    $msg.Content = New-Object System.Net.Http.StringContent($json, [System.Text.Encoding]::UTF8, "application/json")

    $timerMap[$idx] = [System.Diagnostics.Stopwatch]::StartNew()
    $taskMap[$idx]  = $httpClient.SendAsync($msg)
}

[System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]@($taskMap.Values))
$sw.Stop()

Write-Host "  All done in $([math]::Round($sw.Elapsed.TotalSeconds,2))s"

# ---- Phase 3: Collect results ----
Write-Host "`n=== Phase 3: Results ===" -ForegroundColor Green

$rows = @()
foreach ($idx in $taskMap.Keys) {
    $timerMap[$idx].Stop()
    $lat = $timerMap[$idx].ElapsedMilliseconds
    $p   = $priceMap[$idx]
    $code = -999
    $m = ""
    $ok = $false
    try {
        $resp = $taskMap[$idx].Result
        $bodyStr = $resp.Content.ReadAsStringAsync().Result
        $d = $bodyStr | ConvertFrom-Json
        $code = $d.code
        $m = $d.msg
        $ok = ($code -eq 0)
    }
    catch {
        $m = $_.Exception.Message
    }
    $rows += [pscustomobject]@{ User=$idx; Price=$p; Code=$code; Msg=$m; OK=$ok; Ms=$lat }
}

$okRows   = @($rows | Where-Object { $_.OK })
$failRows = @($rows | Where-Object { -not $_.OK })
$elapsed  = $sw.Elapsed.TotalSeconds

Write-Host "  Total:   $($rows.Count)"
Write-Host "  Success: $($okRows.Count)"
Write-Host "  Failed:  $($failRows.Count)"
Write-Host "  Time:    $([math]::Round($elapsed,2))s"
if ($elapsed -gt 0) { Write-Host "  QPS:     $([math]::Round($rows.Count / $elapsed, 1))" }

# Latency percentiles
$sorted = @($rows | Sort-Object Ms | Select-Object -ExpandProperty Ms)
if ($sorted.Count -gt 0) {
    $avg = [math]::Round(($sorted | Measure-Object -Average).Average, 1)
    $p50 = $sorted[[math]::Floor($sorted.Count * 0.5)]
    $p95 = $sorted[[math]::Floor($sorted.Count * 0.95)]
    $p99 = $sorted[[math]::Floor($sorted.Count * 0.99)]
    Write-Host "  Latency: AVG=${avg}ms P50=${p50}ms P95=${p95}ms P99=${p99}ms"
}

# Failure reasons
if ($failRows.Count -gt 0) {
    Write-Host "`n  Failure breakdown:"
    $failRows | Group-Object Msg | Sort-Object Count -Descending | ForEach-Object {
        Write-Host "    [$($_.Count)] $($_.Name)"
    }
}

# Max successful price
$maxOkPrice = 0
if ($okRows.Count -gt 0) {
    $maxOkPrice = ($okRows | Measure-Object -Property Price -Maximum).Maximum
}
Write-Host "`n  Max OK price: $maxOkPrice"

# ---- Phase 4: Verify ----
Write-Host "`n=== Phase 4: Verify ===" -ForegroundColor Cyan

$redisP = docker exec auction-redis redis-cli GET "auction:price:$ItemId"
Write-Host "  Redis price:  $redisP"

$dbP = docker exec auction-mysql mysql -uroot -proot123456 -N -e "SELECT current_price FROM biz_auction_item WHERE id=$ItemId" auction
Write-Host "  MySQL price:  $dbP"
Write-Host "  Max OK price: $maxOkPrice"

$dbBids = docker exec auction-mysql mysql -uroot -proot123456 -N -e "SELECT COUNT(*) FROM biz_bid WHERE item_id=$ItemId" auction
Write-Host "  DB bid rows:  $dbBids"
Write-Host "  Script OK:    $($okRows.Count)"

# Top 10
Write-Host "`n=== Top 10 bids ==="
$rows | Sort-Object Price -Descending | Select-Object -First 10 | Format-Table User, Price, OK, Ms, Msg -AutoSize

$httpClient.Dispose()
Write-Host "Done." -ForegroundColor Green
