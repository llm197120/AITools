@echo off
chcp 65001 >nul
cd /d "%~dp0"
set "ARGS=%*"
if not "%~1"=="" goto run

echo.
echo  HomeAI 发布
echo  [1] 全部（后端 + 前端 + APP）
echo  [2] 仅后端
echo  [3] 仅前端（管理端）
echo  [4] 仅 APP
echo  [5] 后端 + 前端（不出 APP）
echo  [0] 取消
echo.
set /p SEL=请选择: 
if "%SEL%"=="1" set "ARGS=" & goto run
if "%SEL%"=="2" set "ARGS=-Backend" & goto run
if "%SEL%"=="3" set "ARGS=-Frontend" & goto run
if "%SEL%"=="4" set "ARGS=-App" & goto run
if "%SEL%"=="5" set "ARGS=-Backend -Frontend" & goto run
echo 已取消。
goto end

:run
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%~dp0publish-all.ps1" %ARGS%
echo.
pause
:end
