param(
    [string]$Profile = "demo"
)

$ErrorActionPreference = "Stop"
$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path

Write-Host "Academic-Nexus demo data reset helper" -ForegroundColor Cyan
Write-Host "This project restores demo accounts and demo data through Spring profile '$Profile'." -ForegroundColor Yellow
Write-Host "The script starts the backend once so Flyway and DataInitializer can repair missing demo data."
Write-Host ""

Push-Location $Root
try {
    .\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$Profile" "-Dspring-boot.run.arguments=--server.port=8080"
} finally {
    Pop-Location
}
