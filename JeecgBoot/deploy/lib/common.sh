#!/usr/bin/env bash
# 部署脚本公共函数
set -euo pipefail

load_env() {
  local dir="$1"
  if [[ -f "$dir/.env" ]]; then
    set -a
    # shellcheck disable=SC1091
    source "$dir/.env"
    set +a
  fi
}

resolve_env() {
  local env_name="$1"
  case "$env_name" in
    dev) echo "dev-latest" ;;
    prd) echo "prd-latest" ;;
    *) echo "未知环境: $env_name" >&2; return 1 ;;
  esac
}

login_acr() {
  local registry="${ACR_REGISTRY:-crpi-1is06jvzttfocl45-vpc.cn-hangzhou.personal.cr.aliyuncs.com}"
  local user="${ACR_USERNAME:-}"
  local pass="${ACR_PASSWORD:-}"
  user=$(printf '%s' "$user" | tr -d '[:space:]')
  pass=$(printf '%s' "$pass" | tr -d '\r\n')
  if [[ -z "$user" || -z "$pass" ]]; then
    echo "[提示] 未配置 ACR_USERNAME/ACR_PASSWORD，跳过 docker login"
    return 0
  fi
  echo "$pass" | docker login --username="$user" "$registry" --password-stdin
}

export_defaults() {
  export ACR_REGISTRY="${ACR_REGISTRY:-crpi-1is06jvzttfocl45-vpc.cn-hangzhou.personal.cr.aliyuncs.com}"
  export ACR_NAMESPACE="${ACR_NAMESPACE:-liulm}"
}
