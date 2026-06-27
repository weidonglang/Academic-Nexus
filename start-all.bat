@echo off
setlocal

cd /d "%~dp0"

set "ROOT=%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
set "DEFAULT_JAVA_HOME=C:\Users\WDL\.jdks\ms-17.0.18"

if not exist "%PS_EXE%" (
  set "PS_EXE=powershell"
)

if "%BACKEND_PORT%"=="" set "BACKEND_PORT=8080"
if "%FRONTEND_PORT%"=="" set "FRONTEND_PORT=5173"
if "%AI_SERVICE_PORT%"=="" set "AI_SERVICE_PORT=8090"
if "%SPRING_PROFILE%"=="" set "SPRING_PROFILE=demo"
if "%OLLAMA_ENABLED%"=="" set "OLLAMA_ENABLED=true"
if "%OLLAMA_CHAT_MODEL%"=="" set "OLLAMA_CHAT_MODEL=qwen3:8b"
if "%OLLAMA_SQL_MODEL%"=="" set "OLLAMA_SQL_MODEL=qwen2.5-coder:7b"
if "%NACOS_ADDR%"=="" set "NACOS_ADDR=127.0.0.1:8848"
if "%NACOS_DISCOVERY_ENABLED%"=="" set "NACOS_DISCOVERY_ENABLED=true"
if "%NACOS_REGISTER_ENABLED%"=="" set "NACOS_REGISTER_ENABLED=true"
if "%AI_SERVICE_DISCOVERY_ENABLED%"=="" set "AI_SERVICE_DISCOVERY_ENABLED=true"
if "%AI_SERVICE_NAME%"=="" set "AI_SERVICE_NAME=academic-ai-service"
if "%START_NACOS%"=="" set "START_NACOS=true"

if "%JAVA_HOME%"=="" (
  if exist "%DEFAULT_JAVA_HOME%\bin\java.exe" set "JAVA_HOME=%DEFAULT_JAVA_HOME%"
)

echo Starting Tianshi system...
echo.
echo Redis       : start-redis.ps1
echo Nacos       : %NACOS_ADDR%  discovery=%NACOS_DISCOVERY_ENABLED%
echo AI service  : http://localhost:%AI_SERVICE_PORT%  OLLAMA_ENABLED=%OLLAMA_ENABLED%
echo Backend     : http://localhost:%BACKEND_PORT%     profile=%SPRING_PROFILE%
echo Frontend    : http://localhost:%FRONTEND_PORT%
echo.

start "Tianshi Redis" "%PS_EXE%" -NoExit -NoProfile -ExecutionPolicy Bypass -Command ^
  "$Host.UI.RawUI.WindowTitle='Tianshi Redis'; Set-Location '%ROOT%'; .\start-redis.ps1"

if /I "%START_NACOS%"=="true" (
  echo Starting Nacos with Docker Compose...
  docker compose up -d nacos
)

start "Tianshi AI Service" "%PS_EXE%" -NoExit -NoProfile -ExecutionPolicy Bypass -Command ^
  "$Host.UI.RawUI.WindowTitle='Tianshi AI Service'; Set-Location '%ROOT%ai-service'; $env:JAVA_HOME='%JAVA_HOME%'; $env:AI_SERVICE_PORT='%AI_SERVICE_PORT%'; $env:OLLAMA_ENABLED='%OLLAMA_ENABLED%'; $env:OLLAMA_CHAT_MODEL='%OLLAMA_CHAT_MODEL%'; $env:OLLAMA_SQL_MODEL='%OLLAMA_SQL_MODEL%'; $env:NACOS_ADDR='%NACOS_ADDR%'; $env:NACOS_DISCOVERY_ENABLED='%NACOS_DISCOVERY_ENABLED%'; $env:NACOS_REGISTER_ENABLED='%NACOS_REGISTER_ENABLED%'; $system32=Join-Path $env:SystemRoot 'System32'; $windowsPs=Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0'; $env:Path=$windowsPs + ';' + $system32 + ';' + $env:Path; if ($env:JAVA_HOME) { $env:Path=(Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path }; if ('%NACOS_DISCOVERY_ENABLED%' -eq 'true') { Write-Host 'Waiting for Nacos port 8848...'; for ($i = 0; $i -lt 120; $i++) { if (Test-NetConnection -ComputerName 'localhost' -Port 8848 -InformationLevel Quiet) { break }; Start-Sleep -Seconds 1 } }; ..\mvnw.cmd spring-boot:run"

start "Tianshi Backend" "%PS_EXE%" -NoExit -NoProfile -ExecutionPolicy Bypass -Command ^
  "$Host.UI.RawUI.WindowTitle='Tianshi Backend'; Set-Location '%ROOT%'; $env:JAVA_HOME='%JAVA_HOME%'; $env:SERVER_PORT='%BACKEND_PORT%'; $env:AI_SERVICE_URL='http://localhost:%AI_SERVICE_PORT%'; $env:AI_SERVICE_NAME='%AI_SERVICE_NAME%'; $env:AI_SERVICE_DISCOVERY_ENABLED='%AI_SERVICE_DISCOVERY_ENABLED%'; $env:NACOS_ADDR='%NACOS_ADDR%'; $env:NACOS_DISCOVERY_ENABLED='%NACOS_DISCOVERY_ENABLED%'; $env:NACOS_REGISTER_ENABLED='%NACOS_REGISTER_ENABLED%'; $system32=Join-Path $env:SystemRoot 'System32'; $windowsPs=Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0'; $env:Path=$windowsPs + ';' + $system32 + ';' + $env:Path; if ($env:JAVA_HOME) { $env:Path=(Join-Path $env:JAVA_HOME 'bin') + ';' + $env:Path }; Write-Host 'Waiting for Redis port 6379...'; for ($i = 0; $i -lt 60; $i++) { if (Test-NetConnection -ComputerName 'localhost' -Port 6379 -InformationLevel Quiet) { break }; Start-Sleep -Seconds 1 }; if ('%NACOS_DISCOVERY_ENABLED%' -eq 'true') { Write-Host 'Waiting for Nacos port 8848...'; for ($i = 0; $i -lt 120; $i++) { if (Test-NetConnection -ComputerName 'localhost' -Port 8848 -InformationLevel Quiet) { break }; Start-Sleep -Seconds 1 } }; .\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=%SPRING_PROFILE%'"

start "Tianshi Frontend" "%PS_EXE%" -NoExit -NoProfile -ExecutionPolicy Bypass -Command ^
  "$Host.UI.RawUI.WindowTitle='Tianshi Frontend'; Set-Location '%ROOT%frontend'; $env:VITE_DEV_PORT='%FRONTEND_PORT%'; $system32=Join-Path $env:SystemRoot 'System32'; $windowsPs=Join-Path $env:SystemRoot 'System32\WindowsPowerShell\v1.0'; $env:Path=$windowsPs + ';' + $system32 + ';' + $env:Path; Write-Host 'Waiting for backend port %BACKEND_PORT%...'; for ($i = 0; $i -lt 90; $i++) { if (Test-NetConnection -ComputerName 'localhost' -Port %BACKEND_PORT% -InformationLevel Quiet) { break }; Start-Sleep -Seconds 1 }; if (-not (Test-Path 'node_modules')) { npm install }; npm run dev"

echo All startup windows have been opened.
echo.
echo Visit:
echo   Frontend: http://localhost:%FRONTEND_PORT%
echo   Backend : http://localhost:%BACKEND_PORT%
echo.
echo To force local fallback before running:
echo   set OLLAMA_ENABLED=false
echo   start-all.bat
echo.
echo To override Ollama models before running:
echo   set OLLAMA_CHAT_MODEL=qwen3:8b
echo   set OLLAMA_SQL_MODEL=qwen2.5-coder:7b
echo   start-all.bat
echo.
echo To disable Spring Cloud discovery before running:
echo   set NACOS_DISCOVERY_ENABLED=false
echo   set NACOS_REGISTER_ENABLED=false
echo   set AI_SERVICE_DISCOVERY_ENABLED=false
echo   start-all.bat
echo.

endlocal
