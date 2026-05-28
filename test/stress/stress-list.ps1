# Day 28 - item list API stress test (PS 5.1 compatible)
param(
    [string]$BaseUrl = "http://localhost:8080",
    [int]$Concurrent = 100,
    [int]$TotalRequests = 1000,
    [int]$PageSize = 10
)

Add-Type -AssemblyName System.Net.Http

function Get-Percentile($values, [double]$ratio) {
    $sorted = @($values | Sort-Object)
    if ($sorted.Count -eq 0) { return 0 }
    $idx = [math]::Floor(($sorted.Count - 1) * $ratio)
    return $sorted[$idx]
}

Write-Host "=== Day 28 List Stress Test ===" -ForegroundColor Cyan
Write-Host "BaseUrl=$BaseUrl Concurrent=$Concurrent TotalRequests=$TotalRequests PageSize=$PageSize"

$handler = New-Object System.Net.Http.HttpClientHandler
$handler.MaxConnectionsPerServer = [Math]::Max($Concurrent, 200)
$client = New-Object System.Net.Http.HttpClient($handler)
$client.Timeout = [TimeSpan]::FromSeconds(60)

$rows = New-Object System.Collections.ArrayList
$globalSw = [System.Diagnostics.Stopwatch]::StartNew()
$sent = 0

while ($sent -lt $TotalRequests) {
    $batch = [Math]::Min($Concurrent, $TotalRequests - $sent)
    $taskMap = @{}
    $timerMap = @{}
    $urlMap = @{}

    for ($i = 0; $i -lt $batch; $i++) {
        $seq = $sent + $i + 1
        $page = (($seq - 1) % 10) + 1
        $status = 3
        if ($seq % 5 -eq 0) { $status = 2 }
        $sort = "createdAt"
        if ($seq % 3 -eq 0) { $sort = "currentPrice" }
        $url = "$BaseUrl/api/items?page=$page&size=$PageSize&status=$status&sort=$sort"

        $msg = New-Object System.Net.Http.HttpRequestMessage([System.Net.Http.HttpMethod]::Get, $url)
        $timerMap[$seq] = [System.Diagnostics.Stopwatch]::StartNew()
        $taskMap[$seq] = $client.SendAsync($msg)
        $urlMap[$seq] = $url
    }

    [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]@($taskMap.Values))

    foreach ($seq in $taskMap.Keys) {
        $timerMap[$seq].Stop()
        $lat = $timerMap[$seq].ElapsedMilliseconds
        $ok = $false
        $httpStatus = 0
        $code = -999
        $msgText = ""
        try {
            $resp = $taskMap[$seq].Result
            $httpStatus = [int]$resp.StatusCode
            $body = $resp.Content.ReadAsStringAsync().Result
            $json = $body | ConvertFrom-Json
            $code = $json.code
            $msgText = $json.msg
            $ok = ($httpStatus -eq 200 -and $code -eq 0)
        }
        catch {
            $msgText = $_.Exception.Message
        }
        [void]$rows.Add([pscustomobject]@{ Seq=$seq; OK=$ok; Http=$httpStatus; Code=$code; Msg=$msgText; Ms=$lat; Url=$urlMap[$seq] })
    }

    $sent += $batch
    Write-Host "  completed $sent / $TotalRequests"
}

$globalSw.Stop()
$client.Dispose()

$okRows = @($rows | Where-Object { $_.OK })
$failRows = @($rows | Where-Object { -not $_.OK })
$latencies = @($rows | Select-Object -ExpandProperty Ms)
$elapsed = $globalSw.Elapsed.TotalSeconds

Write-Host "`n=== Result ===" -ForegroundColor Green
Write-Host "Total:   $($rows.Count)"
Write-Host "Success: $($okRows.Count)"
Write-Host "Failed:  $($failRows.Count)"
Write-Host "Time:    $([math]::Round($elapsed, 2))s"
if ($elapsed -gt 0) { Write-Host "QPS:     $([math]::Round($rows.Count / $elapsed, 1))" }
Write-Host "Latency: AVG=$([math]::Round(($latencies | Measure-Object -Average).Average, 1))ms P50=$(Get-Percentile $latencies 0.50)ms P95=$(Get-Percentile $latencies 0.95)ms P99=$(Get-Percentile $latencies 0.99)ms"

if ($failRows.Count -gt 0) {
    Write-Host "`nFailure breakdown:" -ForegroundColor Yellow
    $failRows | Group-Object Msg | Sort-Object Count -Descending | ForEach-Object {
        Write-Host "  [$($_.Count)] $($_.Name)"
    }
}

Write-Host "Done." -ForegroundColor Green
