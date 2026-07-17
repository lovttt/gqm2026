$ErrorActionPreference = "Continue"
$log = "$env:TEMP\full.log"
function Log($m) { "$m" | Out-File -Append -Encoding utf8 $log }
Log "=== full start $(Get-Date) ==="

# 等网关端口就绪(最多200s) —— 用 TCP 探测，避免 HTTP 状态码误判
$ready = $false
for ($i = 0; $i -lt 200; $i++) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $tcp.Connect('localhost', 8080)
        if ($tcp.Connected) { $ready = $true; $tcp.Close(); Log "gateway ready after ${i}s"; break }
    } catch {}
    Start-Sleep -Seconds 1
}
if (-not $ready) { Log "GATEWAY NOT READY"; exit 1 }

$tok = (Invoke-RestMethod -Uri http://localhost:8080/api/auth/login -Method Post -ContentType "application/json" -Body '{"username":"admin","password":"admin123"}').token
Log "TOKEN_LEN=$($tok.Length)"
$h = @{ Authorization = "Bearer $tok" }

try {
    $sim = Invoke-RestMethod -Uri http://localhost:8080/api/student/applications/simulate -Method Post -Headers $h -TimeoutSec 300
    Log "SIMULATE_GENERATED=$($sim.generated)"
} catch {
    Log "SIMULATE_ERR=$($_.Exception.Response.StatusCode.value__) $($_.ErrorDetails.Message)"
}

try {
    $run = Invoke-RestMethod -Uri http://localhost:8080/api/admission/run/full -Method Post -Headers $h -TimeoutSec 300
    Log "RUN=" + ($run | ConvertTo-Json -Compress)
} catch {
    Log "RUN_ERR=$($_.Exception.Response.StatusCode.value__) $($_.ErrorDetails.Message)"
}

try {
    $stats = Invoke-RestMethod -Uri http://localhost:8080/api/admission/stats -Method Get -Headers $h -TimeoutSec 60
    Log "STATS=" + ($stats | ConvertTo-Json -Compress)
} catch {
    Log "STATS_ERR=$($_.Exception.Response.StatusCode.value__) $($_.ErrorDetails.Message)"
}

Log "=== full done $(Get-Date) ==="
