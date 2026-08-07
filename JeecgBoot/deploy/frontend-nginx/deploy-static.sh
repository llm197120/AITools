#!/usr/bin/env bash
# 管理端 Nginx 方案 — 更新宿主机静态目录并重载 Nginx
# 用法：
#   ./deploy-static.sh prd /path/to/dist
#   ./deploy-static.sh dev /tmp/admin-web-dist-dev
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/../lib/common.sh"

ENV_NAME="${1:?用法: $0 <dev|prd> <dist目录>}"
DIST_PATH="${2:?请指定 dist 目录，例如 /tmp/admin-web-dist-prd}"

load_env "$SCRIPT_DIR"

if [[ ! -d "$DIST_PATH" ]]; then
  echo "dist 目录不存在: $DIST_PATH"
  exit 1
fi

case "$ENV_NAME" in
  prd)
    WEB_ROOT="${WEB_ROOT:-/var/www/homeai-admin}"
    NGINX_CONF="${NGINX_CONF:-$SCRIPT_DIR/nginx/homeai-admin.prd.conf}"
    ;;
  dev)
    WEB_ROOT="${WEB_ROOT_DEV:-/var/www/homeai-admin-dev}"
    NGINX_CONF="${NGINX_CONF_DEV:-$SCRIPT_DIR/nginx/homeai-admin.dev.conf}"
    ;;
  *)
    echo "未知环境: $ENV_NAME"
    exit 1
    ;;
esac

if [[ ! -f "$NGINX_CONF" ]]; then
  echo "Nginx 配置不存在: $NGINX_CONF"
  exit 1
fi

sudo mkdir -p "$WEB_ROOT"
sudo rsync -a --delete "${DIST_PATH%/}/" "$WEB_ROOT/"
sudo cp "$NGINX_CONF" /etc/nginx/conf.d/homeai-admin.conf
sudo nginx -t
sudo nginx -s reload

echo "[完成] frontend-nginx 静态部署 env=$ENV_NAME"
echo "静态目录: $WEB_ROOT"
echo "Nginx 配置: /etc/nginx/conf.d/homeai-admin.conf"
