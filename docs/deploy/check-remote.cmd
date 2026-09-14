@echo off
cd /d "%~dp0"
if "%~1"=="" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0remote-host.ps1" -Action Check
) else (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0remote-host.ps1" %*
)
echo.
pause
