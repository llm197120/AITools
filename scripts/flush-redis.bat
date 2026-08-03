@echo off
chcp 65001 >nul
echo ========================================
echo   Redis 清空脚本
echo ========================================
echo.

set REDIS_HOST=127.0.0.1
set REDIS_PORT=6379
set REDIS_PASSWORD=

echo 连接地址: %REDIS_HOST%:%REDIS_PORT%
echo.

echo 正在清空 Redis 所有数据...
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% FLUSHALL

if %errorlevel% equ 0 (
    echo [成功] Redis 已清空
) else (
    echo [失败] 请检查 Redis 服务是否启动
)

echo.
echo 验证...
redis-cli -h %REDIS_HOST% -p %REDIS_PORT% DBSIZE

pause
