$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $MyInvocation.MyCommand.Path
$FrontendDir = Join-Path $Root "frontend"

$BackendPort = if ($env:BACKEND_PORT) { $env:BACKEND_PORT } else { "8080" }
$FrontendPort = if ($env:FRONTEND_PORT) { $env:FRONTEND_PORT } else { "5173" }
$SpringProfile = if ($env:SPRING_PROFILE) { $env:SPRING_PROFILE } else { "demo" }
$DefaultJavaHome = "C:\Users\WDL\.jdks\ms-17.0.18"
$PowerShellExe = Join-Path $env:SystemRoot "System32\WindowsPowerShell\v1.0\powershell.exe"

function Normalize-JavaHome([string] $javaHome) {
    if (-not $javaHome) {
        return $null
    }
    $normalized = $javaHome.Trim('"')
    if ((Split-Path -Leaf $normalized) -ieq "bin") {
        $normalized = Split-Path -Parent $normalized
    }
    if (Test-Path (Join-Path $normalized "bin\java.exe")) {
        return $normalized
    }
    return $null
}

$fixedJavaHome = Normalize-JavaHome $env:JAVA_HOME
if (-not $fixedJavaHome) {
    $fixedJavaHome = Normalize-JavaHome $DefaultJavaHome
}
if ($fixedJavaHome) {
    $env:JAVA_HOME = $fixedJavaHome
}

$backendCommand = @"
`$Host.UI.RawUI.WindowTitle = 'Tianshi Backend - Spring Boot'
Set-Location '$Root'
if (`$env:JAVA_HOME) {
    if ((Split-Path -Leaf `$env:JAVA_HOME) -ieq 'bin') {
        `$env:JAVA_HOME = Split-Path -Parent `$env:JAVA_HOME
    }
    `$javaBin = Join-Path `$env:JAVA_HOME 'bin'
    `$system32 = Join-Path `$env:SystemRoot 'System32'
    `$windows = `$env:SystemRoot
    `$windowsPowerShell = Join-Path `$env:SystemRoot 'System32\WindowsPowerShell\v1.0'
    `$env:Path = "`$javaBin;`$windowsPowerShell;`$system32;`$windows;`$env:Path"
}
Write-Host 'Starting backend: Spring profile=$SpringProfile, port=$BackendPort' -ForegroundColor Cyan
Write-Host 'If this is the first run, make sure MySQL database tianshiwebside exists.' -ForegroundColor Yellow
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=$SpringProfile" "-Dspring-boot.run.arguments=--server.port=$BackendPort"
"@

$frontendCommand = @"
`$Host.UI.RawUI.WindowTitle = 'Tianshi Frontend - Vue 3'
Set-Location '$FrontendDir'
`$env:VITE_DEV_PORT = '$FrontendPort'
Write-Host 'Starting frontend: Vite port=$FrontendPort, proxy=http://localhost:$BackendPort' -ForegroundColor Cyan
if (-not (Test-Path 'node_modules')) {
    Write-Host 'node_modules not found, running npm install first...' -ForegroundColor Yellow
    npm install
}
npm run dev
"@

Start-Process $PowerShellExe -ArgumentList "-NoExit", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $backendCommand
Start-Process $PowerShellExe -ArgumentList "-NoExit", "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command", $frontendCommand

Write-Host ""
Write-Host "Project startup commands have been opened in two new windows." -ForegroundColor Green
Write-Host "Backend : http://localhost:$BackendPort"
Write-Host "Frontend: http://localhost:$FrontendPort"
Write-Host ""
Write-Host "Default profile is demo. To change it before running:"
Write-Host '  $env:SPRING_PROFILE="dev"'
Write-Host '  .\start-project.ps1'
