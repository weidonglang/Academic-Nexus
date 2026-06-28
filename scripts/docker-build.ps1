param(
    [ValidateSet("central", "aliyun", "tencent", "huawei", "custom")]
    [string]$MavenMirror = "central",
    [string]$CustomMirrorUrl = "",
    [string]$Service = "",
    [switch]$NoCache
)

$ErrorActionPreference = "Stop"

function Resolve-MirrorUrl {
    param([string]$Name, [string]$CustomUrl)
    switch ($Name) {
        "central" { "https://repo.maven.apache.org/maven2" }
        "aliyun" { "https://maven.aliyun.com/repository/public" }
        "tencent" { "https://mirrors.cloud.tencent.com/nexus/repository/maven-public/" }
        "huawei" { "https://repo.huaweicloud.com/repository/maven/" }
        "custom" {
            if ([string]::IsNullOrWhiteSpace($CustomUrl)) {
                throw "Custom mirror requires -CustomMirrorUrl."
            }
            $CustomUrl
        }
    }
}

docker info *> $null
docker compose version *> $null

if (-not (Test-Path ".env")) {
    Write-Host ".env not found. Copy .env.example to .env before a reproducible Docker demo:" -ForegroundColor Yellow
    Write-Host "  copy .env.example .env"
}

$env:DOCKER_BUILDKIT = "1"
$env:COMPOSE_DOCKER_CLI_BUILD = "1"
$env:MAVEN_MIRROR_URL = Resolve-MirrorUrl $MavenMirror $CustomMirrorUrl

$args = @("compose", "build")
if ($NoCache) {
    $args += "--no-cache"
}
if (-not [string]::IsNullOrWhiteSpace($Service)) {
    $args += $Service
}

Write-Host "Using Maven mirror: $env:MAVEN_MIRROR_URL"
try {
    & docker @args
    if ($LASTEXITCODE -ne 0) {
        throw "docker compose build failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Host "Docker build failed. If Maven reports bad_record_mac or Central transfer failures, retry or switch mirror:" -ForegroundColor Red
    Write-Host "  .\scripts\docker-build.ps1 -MavenMirror aliyun"
    Write-Host "  docker builder prune"
    Write-Host "  docker compose build --no-cache academic-main"
    throw
}
