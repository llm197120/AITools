#!/usr/bin/env bash
# 管理端 Nginx 方案 — 仅部署后端 Docker（MySQL/Redis/后端）
# 管理端静态文件请使用同目录 deploy-static.sh
# 用法：
#   ./deploy-backend.sh prd
#   ./deploy-backend.sh prd prd-a1b2c3d
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck disable=SC1091
source "$SCRIPT_DIR/../lib/common.sh"

ENV_NAME="${1:?用法: $0 <dev|prd> [image_tag]}"
IMAGE_TAG_ARG="${2:-}"

load_env "$SCRIPT_DIR"
export_defaults

DEFAULT_TAG="$(resolve_env "$ENV_NAME")"
export IMAGE_TAG="${IMAGE_TAG_ARG:-${IMAGE_TAG:-$DEFAULT_TAG}}"

case "$ENV_NAME" in
  dev) COMPOSE_FILE="$SCRIPT_DIR/docker-compose.dev.yml" ;;
  prd) COMPOSE_FILE="$SCRIPT_DIR/docker-compose.prd.yml" ;;
  *)   echo "未知环境: $ENV_NAME"; exit 1 ;;
esac

login_acr
docker compose -f "$COMPOSE_FILE" pull homeai-backend
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
docker image prune -f

echo "[完成] frontend-nginx 后端部署 env=$ENV_NAME tag=$IMAGE_TAG"
echo "后端监听 127.0.0.1:\${BACKEND_HTTP_PORT}，请确保宿主机 Nginx 已配置"
