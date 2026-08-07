@echo off
chcp 65001 >nul 2>&1
setlocal EnableExtensions EnableDelayedExpansion

:: ============================================================
::  AITools - Database Init Script (Windows)
::  请在本文件所在目录双击运行，或在 CMD 中执行:
::    cd /d "项目路径\scripts"
::    init-db.bat
:: ============================================================

set "DB_HOST=127.0.0.1"
set "DB_PORT=3306"
set "DB_USER=root"
set "DB_PASSWORD="
set "DB_NAME=jeecg"

set "SCRIPT_DIR=%~dp0"
set "PROJECT_ROOT=%SCRIPT_DIR%.."
set "BASE_SQL=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\db\jeecgboot-mysql-5.7.sql"
set "HOMEAI_SQL_DIR=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\jeecg-boot-module\jeecg-boot-module-homeai\sql"
set "HOMEAI_TABLES=%HOMEAI_SQL_DIR%\init_homeai_tables.sql"
set "HOMEAI_MENUS=%HOMEAI_SQL_DIR%\init_homeai_menus.sql"
set "HOMEAI_RECIPE_CAT=%HOMEAI_SQL_DIR%\init_homeai_recipe_category.sql"
set "HOMEAI_USER_QUOTA=%HOMEAI_SQL_DIR%\init_homeai_user_quota.sql"

set "REDIS_HOST=127.0.0.1"
set "REDIS_PORT=6379"

echo.
echo ========================================
echo   AITools Database Init
echo ========================================
echo.
echo   Database: !DB_HOST!:!DB_PORT!/!DB_NAME!
echo   User: !DB_USER!
echo   Project: !PROJECT_ROOT!
echo.

where mysql >nul 2>&1
if errorlevel 1 (
    echo [FAIL] mysql 未加入 PATH，请先安装 MySQL 客户端或将 mysql.exe 所在目录加入环境变量
    echo.
    pause
    exit /b 1
)

if "!DB_PASSWORD!"=="" (
    set /p "DB_PASSWORD=Enter MySQL password (empty if none): "
    echo.
)

set "MYSQL_PWD=!DB_PASSWORD!"

:: ==================== Step 1: Check MySQL ====================
echo [1/7] Check MySQL connection...
mysql --default-character-set=utf8mb4 -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -e "SELECT 1" >nul 2>&1
if errorlevel 1 (
    echo [FAIL] Cannot connect to MySQL
    echo Please verify: service is running, user/password correct, port !DB_PORT! free
    echo.
    pause
    exit /b 1
)
echo [OK] MySQL connected
echo.

:: ==================== Step 2: Create Database ====================
echo [2/7] Create database !DB_NAME! ...
mysql --default-character-set=utf8mb4 -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -e "CREATE DATABASE IF NOT EXISTS !DB_NAME! DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >nul 2>&1
if errorlevel 1 (
    echo [FAIL] Create database failed
    echo You can create it manually:
    echo   CREATE DATABASE IF NOT EXISTS `!DB_NAME!` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    echo.
    pause
    exit /b 1
)
echo [OK] Database ready
echo.

:: ==================== Step 3: Import Base Tables ====================
echo [3/7] Import JeecgBoot base tables...
if not exist "!BASE_SQL!" (
    echo [SKIP] File not found: !BASE_SQL!
    echo This file is part of the JeecgBoot framework.
    echo Download it from the JeecgBoot repository and place it in the db folder.
    echo.
    goto :after_base
)

echo   Importing base tables ...
set "TEMP_BASE=%TEMP%\jeecg_base_skip_%RANDOM%.sql"
powershell -NoProfile -Command "Get-Content -LiteralPath '!BASE_SQL!' -Encoding UTF8 | Select-Object -Skip 2 | Set-Content -LiteralPath '!TEMP_BASE!' -Encoding UTF8"
if errorlevel 1 (
    echo [FAIL] PowerShell pre-process failed. Is PowerShell installed?
    pause
    exit /b 1
)
mysql --default-character-set=utf8mb4 -h!DB_HOST! -P!DB_PORT! -u!DB_USER! --force "!DB_NAME!" < "!TEMP_BASE!"
set "BASE_IMPORT_ERR=!errorlevel!"
del "!TEMP_BASE!" 2>nul
if not "!BASE_IMPORT_ERR!"=="0" (
    echo.
    echo [WARN] Some base table statements had errors (may be normal if tables existed)
)

echo   Verifying core tables ...
mysql --default-character-set=utf8mb4 -h!DB_HOST! -P!DB_PORT! -u!DB_USER! "!DB_NAME!" -e "SELECT 1 FROM sys_permission LIMIT 0;" >nul 2>&1
if errorlevel 1 (
    echo.
    echo ========================================
    echo [FAIL] sys_permission table NOT FOUND!
    echo The base SQL did not create the core tables.
    echo Check MySQL error output above for the root cause.
    echo ========================================
    echo.
    pause
    exit /b 1
)
echo [OK] Base tables imported and verified
:after_base
echo.

:: ==================== Step 4: Import HomeAI Tables ====================
echo [4/7] Import HomeAI init scripts...
call :ImportSqlFile "!HOMEAI_TABLES!"
call :ImportSqlFile "!HOMEAI_RECIPE_CAT!"
call :ImportSqlFile "!HOMEAI_USER_QUOTA!"
echo [OK] HomeAI business tables imported
echo.

:: ==================== Step 5: Import HomeAI Menus ====================
echo [5/7] Import HomeAI menu permissions...
if not exist "!HOMEAI_MENUS!" (
    echo [FAIL] File not found: !HOMEAI_MENUS!
    pause
    exit /b 1
)
call :ImportSqlFile "!HOMEAI_MENUS!"

echo   Verifying menu data ...
set "MENU_COUNT=0"
for /f "usebackq delims=" %%i in (`mysql --default-character-set=utf8mb4 -h!DB_HOST! -P!DB_PORT! -u!DB_USER! -sN "!DB_NAME!" -e "SELECT COUNT(1) FROM sys_permission WHERE id='homeai_menu_root';" 2^>nul`) do set "MENU_COUNT=%%i"
if "!MENU_COUNT!"=="" set "MENU_COUNT=0"
if !MENU_COUNT! LEQ 0 (
    echo [FAIL] Menu permission data not found!
    echo Try running manually:
    echo   mysql -u!DB_USER! -p "!DB_NAME!" ^< "!HOMEAI_MENUS!"
    echo.
    pause
    exit /b 1
)
echo [OK] HomeAI menu permissions imported and verified
echo.

:: ==================== Step 6: Incremental Scripts ====================
echo [6/7] Run incremental scripts (skip sql/legacy/)...
set "ALTER_FOUND=0"
for %%F in ("!HOMEAI_SQL_DIR!\alter_homeai_*.sql") do (
    set "ALTER_FOUND=1"
    call :ImportSqlFile "%%~fF"
)
if "!ALTER_FOUND!"=="0" (
    echo   [SKIP] no alter scripts found
)
echo   Note: legacy migrations are in sql/legacy/ and are NOT auto-run on fresh install
echo [OK] Incremental scripts completed
echo.

:: ==================== Step 7: Flush Redis ====================
echo [7/7] Flush Redis cache...
where redis-cli >nul 2>&1
if errorlevel 1 (
    echo [WARN] redis-cli not found, skip flush
) else (
    redis-cli -h !REDIS_HOST! -p !REDIS_PORT! FLUSHALL >nul 2>&1
    if errorlevel 1 (
        echo [WARN] Redis flush failed (does not affect DB init)
    ) else (
        echo [OK] Redis cache flushed
    )
)
echo.

:: ==================== Done ====================
echo ========================================
echo   Initialization complete!
echo ========================================
echo.
echo Next steps:
echo   1. Start backend (JeecgSystemApplication)
echo   2. Start frontend (pnpm dev)
echo   3. Login, assign HomeAI menu to roles via Menu Management
echo.
pause
exit /b 0

:: ---------- Import one SQL file ----------
:ImportSqlFile
set "SQL_FILE=%~1"
if not exist "!SQL_FILE!" (
    echo   [SKIP] not found: !SQL_FILE!
    exit /b 0
)
echo   - %~nx1
mysql --default-character-set=utf8mb4 -h!DB_HOST! -P!DB_PORT! -u!DB_USER! --force "!DB_NAME!" < "!SQL_FILE!" >nul 2>&1
if errorlevel 1 (
    echo     [WARN] had errors, continuing...
)
exit /b 0

