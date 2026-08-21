@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "ARGS=%*"
if not "%~1"=="" goto run

echo.
echo  HomeAI 停止
echo  [1] 全部（后端 + 前端）
echo  [2] 仅后端
echo  [3] 仅前端（Nginx / frpc）
echo  [0] 取消
echo.
set /p SEL=请选择: 
if "%SEL%"=="1" set "ARGS=" & goto run
if "%SEL%"=="2" set "ARGS=-Backend" & goto run
if "%SEL%"=="3" set "ARGS=-Frontend" & goto run
echo 已取消。
goto end

:run
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0stop-all.ps1" %ARGS%
echo.
pause
:end
