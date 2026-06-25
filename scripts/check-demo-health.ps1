param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$RedisHost = "localhost",
    [int]$RedisPort = 6379
)

$ErrorActionPreference = "Stop"

function Test-Http($Name, $Url) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 8
        Write-Host "[OK] $Name $($response.StatusCode)" -ForegroundColor Green
    } catch {
        Write-Host "[FAIL] $Name $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "Academic-Nexus demo health check" -ForegroundColor Cyan

$redis = Test-NetConnection $RedisHost -Port $RedisPort -WarningAction SilentlyContinue
if ($redis.TcpTestSucceeded) {
    Write-Host ("[OK] Redis {0}:{1}" -f $RedisHost, $RedisPort) -ForegroundColor Green
} else {
    Write-Host ("[FAIL] Redis {0}:{1}" -f $RedisHost, $RedisPort) -ForegroundColor Red
}

Test-Http "Backend root" "$BaseUrl/"
Test-Http "Login page assets" "$BaseUrl/index.html"

Write-Host ""
Write-Host "Authenticated API checks require a browser login. After login, verify:" -ForegroundColor Yellow
Write-Host "  /admin/system-health"
Write-Host "  /admin/audit-logs"
Write-Host "  /admin/permission-matrix"
Write-Host "  /admin/course-selection-consistency"
