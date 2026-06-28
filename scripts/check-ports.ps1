$ErrorActionPreference = "Stop"

function Read-PortValue {
    param([string]$Name, [int]$Default)
    $fromEnv = [Environment]::GetEnvironmentVariable($Name)
    if (-not [string]::IsNullOrWhiteSpace($fromEnv)) {
        return [int]$fromEnv
    }
    if (Test-Path ".env") {
        $match = Select-String -Path ".env" -Pattern "^$Name=(.+)$" | Select-Object -First 1
        if ($match) {
            return [int]$match.Matches[0].Groups[1].Value.Trim()
        }
    }
    return $Default
}

$ports = [ordered]@{
    MAIN_HOST_PORT = 8088
    FRONTEND_HOST_PORT = 5174
    MYSQL_HOST_PORT = 13306
    REDIS_HOST_PORT = 16379
    NACOS_HOST_PORT = 18848
    NACOS_GRPC_HOST_PORT = 19848
    AI_SERVICE_HOST_PORT = 18090
}

foreach ($entry in $ports.GetEnumerator()) {
    $port = Read-PortValue $entry.Key $entry.Value
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = ($connections | Select-Object -ExpandProperty OwningProcess -Unique) -join ", "
        Write-Host ("{0}={1} is occupied by PID {2}. Change {0} in .env." -f $entry.Key, $port, $pids) -ForegroundColor Yellow
    } else {
        Write-Host ("{0}={1} is free." -f $entry.Key, $port) -ForegroundColor Green
    }
}
