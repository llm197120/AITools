@echo off
chcp 65001 >nul
setlocal EnableDelayedExpansion

:: ============================================================
::  AITools - Database Init Script (Windows)
:: ============================================================

set DB_HOST=127.0.0.1
set DB_PORT=3306
set DB_USER=root
set DB_PASSWORD=
set DB_NAME=jeecg

set SCRIPT_DIR=%~dp0
set PROJECT_ROOT=%SCRIPT_DIR%..
set BASE_SQL=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\db\jeecgboot-mysql-5.7.sql
set HOMEAI_TABLES=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\jeecg-boot-module\jeecg-boot-module-homeai\sql\init_homeai_tables.sql
set HOMEAI_MENUS=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\jeecg-boot-module\jeecg-boot-module-homeai\sql\init_homeai_menus.sql
set HOMEAI_ALTER_MENUS=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\jeecg-boot-module\jeecg-boot-module-homeai\sql\alter_homeai_menus_fix_component.sql
set HOMEAI_ALTER_LAYOUT=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\jeecg-boot-module\jeecg-boot-module-homeai\sql\alter_homeai_menus_fix_layout.sql
set HOMEAI_ALTER_MENUTYPE=%PROJECT_ROOT%\JeecgBoot\jeecg-boot\jeecg-boot-module\jeecg-boot-module-homeai\sql\alter_homeai_menus_fix_menutype.sql

set REDIS_HOST=127.0.0.1
set REDIS_PORT=6379

echo.
echo ========================================
echo   AITools Database Init
echo ========================================
echo.
echo   Database: %DB_HOST%:%DB_PORT%/%DB_NAME%
echo   User: %DB_USER%
echo   Project: %PROJECT_ROOT%
echo.

if "%DB_PASSWORD%"=="" (
    echo Enter MySQL password and press Enter:
    set /p DB_PASSWORD=
    echo.
)
set "MYSQL_CMD=mysql --default-character-set=utf8mb4 -h%DB_HOST% -P%DB_PORT% -u%DB_USER% -p%DB_PASSWORD%"

:: ==================== Step 1: Check MySQL ====================
echo [1/7] Check MySQL connection...
%MYSQL_CMD% -e "SELECT 1" >nul 2>&1
if !errorlevel! neq 0 (
    echo [FAIL] Cannot connect to MySQL
    echo Please verify: service is running, user/password correct, port %DB_PORT% free
    echo.
    pause
    exit /b 1
)
echo [OK] MySQL connected
echo.

:: ==================== Step 2: Create Database ====================
echo [2/7] Create database %DB_NAME% ...
%MYSQL_CMD% -e "CREATE DATABASE IF NOT EXISTS `%DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;" >nul 2>&1
if !errorlevel! neq 0 (
    echo [FAIL] Create database failed
    echo You can create it manually:
    echo   CREATE DATABASE IF NOT EXISTS `%DB_NAME%` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
    echo.
    pause
    exit /b 1
)
echo [OK] Database ready
echo.

:: ==================== Step 3: Import Base Tables ====================
echo [3/7] Import JeecgBoot base tables...
if not exist "%BASE_SQL%" (
    echo [SKIP] File not found: %BASE_SQL%
    echo This file is part of the JeecgBoot framework.
    echo Download it from the JeecgBoot repository and place it in the db folder.
    echo.
    goto :after_base
)

  echo   Importing 100+ tables ...
:: Use PowerShell to skip the first 2 lines (CREATE DATABASE + USE jeecg-boot)
:: because our target database is %DB_NAME%
powershell -NoProfile -Command "Get-Content -Encoding UTF8 '%BASE_SQL%' | Select-Object -Skip 2 | Out-File -Encoding UTF8 '%TEMP%\jeecg_base_skip.sql'; exit 0"
if !errorlevel! neq 0 (
    echo [FAIL] PowerShell pre-process failed. Is PowerShell installed?
    pause
    exit /b 1
)
%MYSQL_CMD% --force %DB_NAME% < "%TEMP%\jeecg_base_skip.sql"
set BASE_IMPORT_ERR=!errorlevel!
del "%TEMP%\jeecg_base_skip.sql" 2>nul
if !BASE_IMPORT_ERR! neq 0 (
    echo.
    echo [WARN] Some base table statements had errors ^(may be normal if tables existed^)
)

:: Verify the core sys_permission table exists
echo   Verifying core tables ...
%MYSQL_CMD% %DB_NAME% -e "SELECT 1 FROM `sys_permission` LIMIT 0;" >nul 2>&1
if !errorlevel! neq 0 (
    echo.
    echo ========================================
    echo [FAIL] sys_permission table NOT FOUND!
    echo The base SQL did not create the core tables.
    echo.
    echo Most likely cause: the SQL file contained statements
    echo that failed early, stopping all subsequent CREATE TABLE.
    echo Check the MySQL error output above for the root cause.
    echo.
    echo Quick fix: open the base SQL file and check if there
    echo are any characters before the first line that might
    echo break the import.
    echo ========================================
    echo.
    pause
    exit /b 1
)
echo [OK] Base tables imported and verified
:after_base
echo.

:: ==================== Step 4: Import HomeAI Tables ====================
echo [4/7] Import HomeAI business tables...
if not exist "%HOMEAI_TABLES%" (
    echo [FAIL] File not found: %HOMEAI_TABLES%
    pause
    exit /b 1
)
%MYSQL_CMD% --force %DB_NAME% < "%HOMEAI_TABLES%" >nul 2>&1
if !errorlevel! neq 0 (
    echo [FAIL] Import failed! Running again with full error output for diagnosis:
    %MYSQL_CMD% --force %DB_NAME% < "%HOMEAI_TABLES%"
    echo.
    echo [FAIL] Above are the MySQL errors. Please fix them and re-run this script.
    echo If tables already exist, you can ignore this error and continue manually:
    echo   Skip step 4 and run step 5 onwards.
    echo.
    pause
    exit /b 1
)
echo [OK] HomeAI business tables imported
echo.

:: ==================== Step 5: Import HomeAI Menus ====================
echo [5/7] Import HomeAI menu permissions...
if not exist "%HOMEAI_MENUS%" (
    echo [FAIL] File not found: %HOMEAI_MENUS%
    pause
    exit /b 1
)
%MYSQL_CMD% --force %DB_NAME% < "%HOMEAI_MENUS%" >nul 2>&1
if !errorlevel! neq 0 (
    echo [FAIL] Import failed! Running again with full error output:
    %MYSQL_CMD% --force %DB_NAME% < "%HOMEAI_MENUS%"
    echo.
    echo [FAIL] See MySQL errors above.
    echo.
    pause
    exit /b 1
)
:: Verify menus were inserted
echo   Verifying menu data ...
for /f %%i in ('%MYSQL_CMD% -sN %DB_NAME% -e "SELECT COUNT(1) FROM `sys_permission` WHERE id='homeai_menu_root';" 2^>nul') do set MENU_COUNT=%%i
if "%MENU_COUNT%"=="" set MENU_COUNT=0
if %MENU_COUNT% LEQ 0 (
    echo [FAIL] Menu permission data not found!
    echo The sys_permission table exists but HomeAI menu rows were not inserted.
    echo Try running the SQL manually:
    echo   %MYSQL_CMD% %DB_NAME% ^< "%HOMEAI_MENUS%"
    echo.
    pause
    exit /b 1
)
echo [OK] HomeAI menu permissions imported and verified (%MENU_COUNT% root menus)
echo.

:: ==================== Step 6: Incremental Scripts ====================
echo [6/7] Run incremental scripts...

if exist "%HOMEAI_ALTER_MENUS%" (
    echo   - alter_homeai_menus_fix_component.sql
    %MYSQL_CMD% %DB_NAME% < "%HOMEAI_ALTER_MENUS%" >nul 2>&1
    if !errorlevel! neq 0 echo     [WARN] had errors, continuing...
)

if exist "%HOMEAI_ALTER_LAYOUT%" (
    echo   - alter_homeai_menus_fix_layout.sql
    %MYSQL_CMD% %DB_NAME% < "%HOMEAI_ALTER_LAYOUT%" >nul 2>&1
    if !errorlevel! neq 0 echo     [WARN] had errors, continuing...
)

if exist "%HOMEAI_ALTER_MENUTYPE%" (
    echo   - alter_homeai_menus_fix_menutype.sql
    %MYSQL_CMD% %DB_NAME% < "%HOMEAI_ALTER_MENUTYPE%" >nul 2>&1
    if !errorlevel! neq 0 echo     [WARN] had errors, continuing...
)

echo [OK] Incremental scripts completed
echo.

:: ==================== Step 7: Flush Redis ====================
echo [7/7] Flush Redis cache...
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% FLUSHALL >nul 2>&1
if !errorlevel! equ 0 (
    echo [OK] Redis cache flushed
) else (
    echo [WARN] Redis flush failed ^(does not affect DB init^)
)
echo.

:: ==================== Done ====================
echo ========================================
echo   Initialization complete!
echo ========================================
echo.
echo Next steps:
echo   1. Start backend ^(JeecgSystemApplication^)
echo   2. Start frontend ^(pnpm dev^)
echo   3. Login, assign HomeAI menu to roles via Menu Management
echo.
pause
