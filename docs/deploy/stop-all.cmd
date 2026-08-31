@echo off
cd /d "%~dp0"
if "%~1"=="" (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1" -Interactive
) else (
  powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1" %*
)
echo.
pause
