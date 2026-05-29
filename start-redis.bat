@echo off
cd /d "%~dp0"
set "PS_EXE=%SystemRoot%\System32\WindowsPowerShell\v1.0\powershell.exe"
if exist "%PS_EXE%" (
  "%PS_EXE%" -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-redis.ps1"
) else (
  powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0start-redis.ps1"
)
