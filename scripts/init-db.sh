#!/bin/bash
# ============================================================
#  AITools 项目 - 数据库初始化脚本 (Linux / Mac / Docker)
#
#  执行顺序：
#    1. 创建数据库（如未创建）
#    2. 导入 JeecgBoot 基础表
#    3. 导入 HomeAI 业务表
#    4. 导入 HomeAI 菜单权限
#    5. 执行增量修改脚本（跳过 sql/legacy/ 旧库专用脚本）
#    6. 清空 Redis 缓存
#
#  用法：
#    chmod +x init-db.sh
#    ./init-db.sh
#    DB_PASSWORD=123456 ./init-db.sh
# ============================================================
# ---------- 数据库配置 ----------
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
DB_USER="${DB_USER:-root}"
DB_PASSWORD="${DB_PASSWORD:-}"
DB_NAME="${DB_NAME:-jeecg}"

# ---------- 项目路径 ----------
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"
BASE_SQL="$PROJECT_ROOT/JeecgBoot/jeecg-boot/db/jeecgboot-mysql-5.7.sql"
HOMEAI_SQL_DIR="$PROJECT_ROOT/JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql"
HOMEAI_TABLES="$HOMEAI_SQL_DIR/init_homeai_tables.sql"
HOMEAI_MENUS="$HOMEAI_SQL_DIR/init_homeai_menus.sql"
HOMEAI_RECIPE_CAT="$HOMEAI_SQL_DIR/init_homeai_recipe_category.sql"
HOMEAI_USER_QUOTA="$HOMEAI_SQL_DIR/init_homeai_user_quota.sql"

# ---------- Redis 配置 ----------
REDIS_HOST="${REDIS_HOST:-127.0.0.1}"
REDIS_PORT="${REDIS_PORT:-6379}"

echo ""
echo "========================================"
echo "  AITools 项目数据库初始化"
echo "========================================"
echo ""
echo "  数据库: $DB_HOST:$DB_PORT/$DB_NAME"
echo "  用户: $DB_USER"
echo "  项目: $PROJECT_ROOT"
echo ""

# ---------- 检查 mysql 客户端 ----------
if ! command -v mysql >/dev/null 2>&1; then
    echo "[失败] 未找到 mysql 客户端，请安装 MySQL 客户端或将 mysql 加入 PATH"
    exit 1
fi

# ---------- 密码处理 ----------
if [ -z "$DB_PASSWORD" ]; then
    read -rsp "请输入 MySQL 密码（无密码直接回车）: " DB_PASSWORD
    echo ""
    echo ""
fi

export MYSQL_PWD="$DB_PASSWORD"
MYSQL_CMD=(mysql --default-character-set=utf8mb4 -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER")

import_sql() {
    local sql_file="$1"
    if [ ! -f "$sql_file" ]; then
        echo "  [跳过] 文件不存在: $sql_file"
        return 0
    fi
    echo "  - $(basename "$sql_file")"
    if ! "${MYSQL_CMD[@]}" --force "$DB_NAME" < "$sql_file"; then
        echo "    [警告] 执行出现错误，继续..."
    fi
}

# ---------- 检查 MySQL 连接 ----------
echo "[1/7] 检查 MySQL 连接..."
if ! "${MYSQL_CMD[@]}" -e "SELECT 1" >/dev/null 2>&1; then
    echo "[失败] 无法连接到 MySQL，请检查："
    echo "  - MySQL 服务是否启动"
    echo "  - 用户名/密码是否正确"
    echo "  - 端口 $DB_PORT 是否被占用"
    exit 1
fi
echo "[成功] MySQL 连接正常"

# ---------- 创建数据库 ----------
echo ""
echo "[2/7] 创建数据库 $DB_NAME..."
if ! "${MYSQL_CMD[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"; then
    echo "[失败] 创建数据库失败"
    exit 1
fi
echo "[成功] 数据库已就绪"

# ---------- 导入 JeecgBoot 基础表 ----------
echo ""
echo "[3/7] 导入 JeecgBoot 基础表..."
if [ ! -f "$BASE_SQL" ]; then
    echo "[跳过] 基础 SQL 文件不存在: $BASE_SQL"
    echo "       请从 JeecgBoot 仓库获取 jeecgboot-mysql-5.7.sql 并放入 db 目录"
else
    echo "  导入基础表..."
    TEMP_BASE="$(mktemp)"
    # 跳过文件前两行 CREATE DATABASE / USE，避免与脚本指定库冲突
    tail -n +3 "$BASE_SQL" > "$TEMP_BASE"
    if ! "${MYSQL_CMD[@]}" --force "$DB_NAME" < "$TEMP_BASE"; then
        echo "[警告] 导入基础表时出现错误（可能已有旧数据），继续..."
    fi
    rm -f "$TEMP_BASE"

    echo "  验证核心表..."
    if ! "${MYSQL_CMD[@]}" "$DB_NAME" -e "SELECT 1 FROM sys_permission LIMIT 0;" >/dev/null 2>&1; then
        echo ""
        echo "========================================"
        echo "[失败] sys_permission 表不存在！"
        echo "基础 SQL 未成功创建 JeecgBoot 核心表，请检查上方 MySQL 报错。"
        echo "========================================"
        exit 1
    fi
    echo "[成功] 基础表导入完成并已验证"
fi

# ---------- 导入 HomeAI 业务表 ----------
echo ""
echo "[4/7] 导入 HomeAI 初始化脚本..."
for init_sql in "$HOMEAI_TABLES" "$HOMEAI_RECIPE_CAT" "$HOMEAI_USER_QUOTA"; do
    import_sql "$init_sql"
done
echo "[成功] HomeAI 业务表导入完成"

# ---------- 导入 HomeAI 菜单权限 ----------
echo ""
echo "[5/7] 导入 HomeAI 菜单权限..."
if [ ! -f "$HOMEAI_MENUS" ]; then
    echo "[失败] 文件不存在: $HOMEAI_MENUS"
    exit 1
fi
import_sql "$HOMEAI_MENUS"

MENU_COUNT=$("${MYSQL_CMD[@]}" -sN "$DB_NAME" -e "SELECT COUNT(1) FROM sys_permission WHERE id='homeai_menu_root';" 2>/dev/null || echo 0)
if [ "${MENU_COUNT:-0}" -le 0 ]; then
    echo ""
    echo "[失败] 菜单权限数据未找到！"
    echo "请手动执行: mysql -u$DB_USER -p $DB_NAME < $HOMEAI_MENUS"
    exit 1
fi
echo "[成功] 菜单权限已验证"

# ---------- 按 alter-order.txt 执行增量（排除 legacy） ----------
echo ""
echo "[6/7] 按 alter-order.txt 执行增量修改脚本..."
ALTER_FOUND=0
ALTER_ORDER="$HOMEAI_SQL_DIR/alter-order.txt"
if [ -f "$ALTER_ORDER" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
        case "$line" in
            ''|\#*) continue ;;
        esac
        ALTER_FOUND=1
        import_sql "$HOMEAI_SQL_DIR/$line"
    done < "$ALTER_ORDER"
else
    echo "  [警告] 缺少 alter-order.txt，回退为 glob"
    shopt -s nullglob
    for alter_sql in "$HOMEAI_SQL_DIR"/alter_homeai_*.sql; do
        ALTER_FOUND=1
        import_sql "$alter_sql"
    done
    shopt -u nullglob
fi
if [ "$ALTER_FOUND" -eq 0 ]; then
    echo "  [跳过] 未找到增量脚本"
fi
if [ -f "$HOMEAI_SQL_DIR/smoke_homeai_schema.sql" ]; then
    echo "  - smoke_homeai_schema.sql"
    if "${MYSQL_CMD[@]}" "$DB_NAME" < "$HOMEAI_SQL_DIR/smoke_homeai_schema.sql" >/dev/null 2>&1; then
        echo "    [成功] schema smoke 通过"
    else
        echo "    [警告] schema smoke 失败，请检查缺列"
    fi
fi
echo "[成功] 增量修改脚本执行完成"

# ---------- 清空 Redis ----------
echo ""
echo "[7/7] 清空 Redis 缓存..."
if command -v redis-cli >/dev/null 2>&1; then
    if redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" FLUSHALL >/dev/null 2>&1; then
        echo "[成功] Redis 缓存已清空"
    else
        echo "[警告] Redis 清空失败（不影响数据库初始化，请手动清空）"
    fi
else
    echo "[跳过] redis-cli 未安装，请手动清空 Redis"
fi

# ---------- 完成 ----------
echo ""
echo "========================================"
echo "  初始化完成!"
echo "========================================"
echo ""
echo "后续步骤："
echo "  1. 启动后端服务 (JeecgSystemApplication)"
echo "  2. 启动前端 (pnpm dev)"
echo "  3. 登录系统，为角色分配 HomeAI 菜单权限"
echo "     （菜单管理 -> 角色管理 -> 勾选「家庭AI小工具」）"
echo ""
