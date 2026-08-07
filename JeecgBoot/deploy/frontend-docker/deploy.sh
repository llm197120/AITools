#!/usr/bin/env bash
# 管理端 Docker 部署：拉取 ACR 全栈镜像（MySQL/Redis/后端/管理端容器）并启动
# 用法：
#   ./deploy.sh prd              # 使用 .env 中 IMAGE_TAG 或 prd-latest
#   ./deploy.sh prd prd-a1b2c3d  # 指定镜像 tag
#   ./deploy.sh dev
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
docker compose -f "$COMPOSE_FILE" pull
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans
docker image prune -f

echo "[完成] frontend-docker 部署 env=$ENV_NAME tag=$IMAGE_TAG"
echo "访问端口见 .env 中 ADMIN_HTTP_PORT（prd 默认 80，dev 默认 8088）"
