#!/bin/bash
# ============================================================
#  AITools 项目 - 数据库初始化脚本 (Linux / Mac / Docker)
#
#  执行顺序：
#    1. 创建数据库（如未创建）
#    2. 导入 JeecgBoot 基础表
#    3. 导入 HomeAI 业务表
#    4. 导入 HomeAI 菜单权限
#    5. 执行增量修改脚本
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
HOMEAI_TABLES="$PROJECT_ROOT/JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql/init_homeai_tables.sql"
HOMEAI_MENUS="$PROJECT_ROOT/JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql/init_homeai_menus.sql"
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
echo ""

# ---------- 密码处理 ----------
if [ -z "$DB_PASSWORD" ]; then
    read -sp "请输入 MySQL 密码: " DB_PASSWORD
    echo ""
    echo ""
fi

# ---------- MySQL 命令 ----------
# 通过 MYSQL_PWD 传递密码（只输入一次、全部命令复用，且避免特殊字符转义问题）
# 密码为空时（MySQL 无密码）不加 -p，避免 mysql 对每条命令重新提示输入
export MYSQL_PWD="$DB_PASSWORD"
MYSQL_CMD="mysql -h$DB_HOST -P$DB_PORT -u$DB_USER"

# ---------- 检查 MySQL 连接 ----------
echo "[1/7] 检查 MySQL 连接..."
if ! $MYSQL_CMD -e "SELECT 1" > /dev/null 2>&1; then
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
$MYSQL_CMD -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
echo "[成功] 数据库已就绪"

# ---------- 导入 JeecgBoot 基础表 ----------
echo ""
echo "[3/7] 导入 JeecgBoot 基础表..."
if [ ! -f "$BASE_SQL" ]; then
    echo "[跳过] 基础 SQL 文件不存在: $BASE_SQL"
else
    $MYSQL_CMD "$DB_NAME" < "$BASE_SQL" || echo "[警告] 导入基础表时出现错误（可能已有旧数据），继续..."
    echo "[成功] 基础表导入完成"
fi

# ---------- 导入 HomeAI 业务表 ----------
echo ""
echo "[4/7] 导入 HomeAI 初始化脚本..."
for init_sql in "$HOMEAI_TABLES" "$HOMEAI_RECIPE_CAT" "$HOMEAI_USER_QUOTA"; do
    if [ -f "$init_sql" ]; then
        echo "  - $(basename "$init_sql")"
        $MYSQL_CMD "$DB_NAME" < "$init_sql" || echo "    [警告] 执行出现错误，继续..."
    else
        echo "  [跳过] 文件不存在: $init_sql"
    fi
done
echo "[成功] HomeAI 业务表导入完成"

# ---------- 导入 HomeAI 菜单权限 ----------
echo ""
echo "[5/7] 导入 HomeAI 菜单权限..."
if [ ! -f "$HOMEAI_MENUS" ]; then
    echo "[失败] 文件不存在: $HOMEAI_MENUS"
    exit 1
fi
$MYSQL_CMD "$DB_NAME" < "$HOMEAI_MENUS" || echo "[警告] 菜单导入出现错误，继续..."
echo "[成功] 菜单权限导入完成"

# 验证菜单是否导入成功
MENU_COUNT=$($MYSQL_CMD -sN "$DB_NAME" -e "SELECT COUNT(1) FROM sys_permission WHERE id='homeai_menu_root';" 2>/dev/null || echo 0)
if [ "$MENU_COUNT" -le 0 ]; then
    echo ""
    echo "[失败] 菜单权限数据未找到！"
    echo "sys_permission 表存在但 HomeAI 菜单行没有成功插入。"
    echo "请手动执行 SQL："
    echo "  $MYSQL_CMD $DB_NAME < $HOMEAI_MENUS"
    exit 1
fi
echo "[成功] 菜单权限已验证（${MENU_COUNT} 条一级菜单）"

# ---------- 执行增量修改脚本 ----------
echo ""
echo "[6/7] 执行增量修改脚本..."
for alter_sql in "$HOMEAI_SQL_DIR"/alter_homeai_*.sql; do
    if [ -f "$alter_sql" ]; then
        echo "  - $(basename "$alter_sql")"
        $MYSQL_CMD "$DB_NAME" < "$alter_sql" || echo "    [警告] 执行出现错误，继续..."
    fi
done
echo "[成功] 增量修改脚本执行完成"

# ---------- 清空 Redis ----------
echo ""
echo "[7/7] 清空 Redis 缓存..."
if command -v redis-cli &> /dev/null; then
    redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" FLUSHALL > /dev/null 2>&1 && \
        echo "[成功] Redis 缓存已清空" || \
        echo "[警告] Redis 清空失败（不影响数据库初始化，请手动清空）"
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
