@echo off
cd /d "%~dp0"
net session >nul 2>&1
if errorlevel 1 (
  echo Please right-click and Run as administrator.
  echo 请右键「以管理员身份运行」。
  pause
  exit /b 1
)
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0init-windows-host.ps1" %*
echo.
pause
