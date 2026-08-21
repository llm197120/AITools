#!/usr/bin/env bash
# HomeAI 服务器一键部署：frps + Nginx（入口 + APK 下载页）
# 用法（在服务器上以 root 执行）：
#   FRP_TOKEN='你的token' bash install-server.sh
# 从本机一键推送：
#   JeecgBoot/deploy/frp/remote-install.ps1
set -euo pipefail

FRP_VERSION="${FRP_VERSION:-0.71.0}"
SERVER_IP="${SERVER_IP:-116.62.115.226}"
FRP_BIND_PORT="${FRP_BIND_PORT:-7000}"
FRP_REMOTE_PORT="${FRP_REMOTE_PORT:-18080}"
FRP_DASHBOARD_PORT="${FRP_DASHBOARD_PORT:-7500}"
FRP_DASHBOARD_USER="${FRP_DASHBOARD_USER:-admin}"
INSTALL_DIR="${INSTALL_DIR:-/opt/homeai-frp}"
APK_DIR="${APK_DIR:-/var/www/homeai-apk}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

need_root() {
  if [[ "$(id -u)" -ne 0 ]]; then
    echo "请用 root 执行：sudo FRP_TOKEN=... bash $0"
    exit 1
  fi
}

rand_hex() {
  local n="${1:-24}"
  if command -v openssl >/dev/null 2>&1; then
    openssl rand -hex "$n"
  else
    tr -dc 'a-f0-9' </dev/urandom | head -c $((n * 2))
    echo
  fi
}

ensure_token() {
  if [[ -z "${FRP_TOKEN:-}" && -f "$SCRIPT_DIR/secrets.env" ]]; then
    # shellcheck disable=SC1091
    set -a
    source "$SCRIPT_DIR/secrets.env"
    set +a
  fi
  if [[ -z "${FRP_TOKEN:-}" && -f "$INSTALL_DIR/secrets.env" ]]; then
    # shellcheck disable=SC1091
    set -a
    source "$INSTALL_DIR/secrets.env"
    set +a
  fi
  if [[ -z "${FRP_TOKEN:-}" ]]; then
    FRP_TOKEN="$(rand_hex 24)"
    echo "[提示] 未传入 FRP_TOKEN，已生成新 token（请复制到本机 secrets.env）"
  fi
  if [[ -z "${FRP_DASHBOARD_PASSWORD:-}" ]]; then
    FRP_DASHBOARD_PASSWORD="$(rand_hex 12)"
  fi
  if [[ ! "$FRP_TOKEN" =~ ^[0-9a-fA-F]{16,}$ ]]; then
    echo "FRP_TOKEN 请使用至少 16 位十六进制（脚本生成的格式），避免 TOML 转义问题"
    exit 1
  fi
}

detect_pkg() {
  if command -v apt-get >/dev/null 2>&1; then
    PKG=apt
  elif command -v dnf >/dev/null 2>&1; then
    PKG=dnf
  elif command -v yum >/dev/null 2>&1; then
    PKG=yum
  else
    echo "不支持的系统：需要 apt / dnf / yum"
    exit 1
  fi
}

install_pkgs() {
  echo "[1/7] 安装 nginx / curl ..."
  case "$PKG" in
    apt)
      export DEBIAN_FRONTEND=noninteractive
      apt-get update -y
      apt-get install -y nginx curl ca-certificates tar gzip
      ;;
    dnf)
      dnf install -y nginx curl ca-certificates tar gzip
      ;;
    yum)
      yum install -y nginx curl ca-certificates tar gzip
      ;;
  esac
}

download_file() {
  local dest="$1"
  shift
  local url
  for url in "$@"; do
    echo "  尝试 $url"
    if command -v curl >/dev/null 2>&1; then
      if curl -fL --retry 3 --retry-delay 2 --connect-timeout 20 -o "$dest" "$url"; then
        return 0
      fi
    elif command -v wget >/dev/null 2>&1; then
      if wget -O "$dest" "$url"; then
        return 0
      fi
    fi
  done
  return 1
}

install_frps() {
  echo "[2/7] 安装 frps ${FRP_VERSION} ..."
  mkdir -p "$INSTALL_DIR"
  if [[ -x "$INSTALL_DIR/frps" ]]; then
    echo "  已存在 $INSTALL_DIR/frps ，跳过下载"
    return 0
  fi
  local arch arch_tag
  arch="$(uname -m)"
  case "$arch" in
    x86_64|amd64) arch_tag="linux_amd64" ;;
    aarch64|arm64) arch_tag="linux_arm64" ;;
    *) echo "不支持的 CPU 架构: $arch"; exit 1 ;;
  esac

  local name="frp_${FRP_VERSION}_${arch_tag}"
  local tar="${name}.tar.gz"
  local gh="https://github.com/fatedier/frp/releases/download/v${FRP_VERSION}/${tar}"
  local tmp
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' RETURN

  if ! download_file "$tmp/$tar" \
      "$gh" \
      "https://ghfast.top/${gh}" \
      "https://ghproxy.net/${gh}"; then
    echo "下载 frp 失败。可在能访问 GitHub 的机器下载后把 frps 放到 $INSTALL_DIR/frps"
    exit 1
  fi

  tar -xf "$tmp/$tar" -C "$tmp"
  mkdir -p "$INSTALL_DIR"
  install -m 0755 "$tmp/$name/frps" "$INSTALL_DIR/frps"
}

write_frps_config() {
  echo "[3/7] 写入 frps 配置 ..."
  umask 077
  cat > "$INSTALL_DIR/frps.toml" <<EOF
bindAddr = "0.0.0.0"
bindPort = ${FRP_BIND_PORT}
proxyBindAddr = "127.0.0.1"

auth.method = "token"
auth.token = "${FRP_TOKEN}"

webServer.addr = "127.0.0.1"
webServer.port = ${FRP_DASHBOARD_PORT}
webServer.user = "${FRP_DASHBOARD_USER}"
webServer.password = "${FRP_DASHBOARD_PASSWORD}"

log.to = "/var/log/frps.log"
log.level = "info"
log.maxDays = 7
EOF
  chmod 600 "$INSTALL_DIR/frps.toml"

  cat > "$INSTALL_DIR/secrets.env" <<EOF
FRP_TOKEN=${FRP_TOKEN}
FRP_DASHBOARD_USER=${FRP_DASHBOARD_USER}
FRP_DASHBOARD_PASSWORD=${FRP_DASHBOARD_PASSWORD}
EOF
  chmod 600 "$INSTALL_DIR/secrets.env"
}

write_systemd() {
  echo "[4/7] 配置 systemd ..."
  cat > /etc/systemd/system/frps.service <<EOF
[Unit]
Description=HomeAI frps
After=network.target

[Service]
Type=simple
ExecStart=${INSTALL_DIR}/frps -c ${INSTALL_DIR}/frps.toml
Restart=on-failure
RestartSec=5
LimitNOFILE=1048576

[Install]
WantedBy=multi-user.target
EOF
  systemctl daemon-reload
  systemctl enable --now frps
}

write_download_page() {
  mkdir -p "$APK_DIR"
  if [[ -f "$SCRIPT_DIR/download/index.html" ]]; then
    cp "$SCRIPT_DIR/download/index.html" "$APK_DIR/index.html"
  else
    cat > "$APK_DIR/index.html" <<'HTML'
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>家庭AI小工具 · 下载</title>
  <style>
    body { font-family: system-ui, sans-serif; max-width: 28rem; margin: 3rem auto; padding: 0 1.25rem; color: #1f2937; }
    h1 { font-size: 1.4rem; }
    p { line-height: 1.6; color: #4b5563; }
    a.btn { display: inline-block; margin-top: 1rem; padding: 0.75rem 1.25rem; background: #2563eb; color: #fff; text-decoration: none; border-radius: 8px; }
    a.btn.disabled { background: #9ca3af; pointer-events: none; }
    ol { padding-left: 1.25rem; color: #4b5563; }
    .hint { font-size: 0.85rem; color: #6b7280; margin-top: 2rem; }
    .warn { font-size: 0.9rem; color: #b45309; margin-top: 0.75rem; display: none; }
  </style>
</head>
<body>
  <h1>家庭AI小工具</h1>
  <p>Android 内测版。仅供家庭成员侧载安装，不上架应用商店。</p>
  <p><a class="btn" id="apkBtn" href="./homeai-latest.apk">下载 APK</a></p>
  <p class="warn" id="apkMissing">安装包还未上传到服务器。请在电脑上打好签名 APK 后执行：<br><code>.\upload-apk.ps1 -ApkPath 你的签名包.apk</code></p>
  <ol>
    <li>若微信/QQ 无法下载，请用系统浏览器打开本页。</li>
    <li>允许该浏览器「安装未知应用」。</li>
    <li>安装后用手机号和密码登录。</li>
    <li>连不上时请确认家里电脑已开机。</li>
  </ol>
  <p class="hint">版本见同目录 <code>version.txt</code>（若有）。覆盖安装请勿更换签名证书。</p>
  <script>
    (function () {
      var btn = document.getElementById('apkBtn');
      var warn = document.getElementById('apkMissing');
      fetch('./homeai-latest.apk', { method: 'HEAD', cache: 'no-store' }).then(function (r) {
        if (r.ok) return;
        btn.className = 'btn disabled';
        btn.removeAttribute('href');
        btn.textContent = 'APK 尚未上传';
        warn.style.display = 'block';
      }).catch(function () {});
    })();
  </script>
</body>
</html>
HTML
  fi
  chmod 755 /var/www "$APK_DIR" 2>/dev/null || true
  chmod 644 "$APK_DIR/index.html" 2>/dev/null || true
  if command -v chcon >/dev/null 2>&1; then
    chcon -Rt httpd_sys_content_t "$APK_DIR" 2>/dev/null || true
  fi
}

write_nginx() {
  echo "[5/7] 配置 Nginx ..."
  mkdir -p /etc/nginx/conf.d /var/log/nginx

  if [[ -f /etc/nginx/nginx.conf ]] && ! grep -q 'homeai-frp-minimal' /etc/nginx/nginx.conf; then
    cp -a /etc/nginx/nginx.conf "/etc/nginx/nginx.conf.bak.homeai.$(date +%s)"
    cat > /etc/nginx/nginx.conf <<'NGX'
# homeai-frp-minimal
user nginx;
worker_processes auto;
error_log /var/log/nginx/error.log;
pid /run/nginx.pid;

events {
    worker_connections 1024;
}

http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    keepalive_timeout 65;
    include /etc/nginx/conf.d/*.conf;
}
NGX
  fi

  shopt -s nullglob
  for f in /etc/nginx/conf.d/*.conf; do
    if [[ "$(basename "$f")" != "homeai.conf" ]]; then
      mv -f "$f" "${f}.disabled" || true
    fi
  done
  shopt -u nullglob
  rm -f /etc/nginx/sites-enabled/default || true

  cat > /etc/nginx/conf.d/homeai.conf <<EOF
server {
    listen 80;
    listen [::]:80;
    server_name _;
    client_max_body_size 50m;

    types {
        application/vnd.android.package-archive apk;
    }

    location /app/ {
        alias ${APK_DIR}/;
        index index.html;
        autoindex off;
    }

    location / {
        proxy_pass http://127.0.0.1:${FRP_REMOTE_PORT};
        proxy_http_version 1.1;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
        proxy_set_header Upgrade \$http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 10s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
        proxy_buffering off;
    }
}
EOF

  nginx -t
  systemctl enable nginx
  systemctl stop httpd 2>/dev/null || true
  systemctl stop nginx 2>/dev/null || true
  pkill -TERM nginx 2>/dev/null || true
  sleep 1
  pkill -9 nginx 2>/dev/null || true
  sleep 1
  if ! systemctl start nginx; then
    echo "nginx start failed, journal:"
    journalctl -u nginx -n 50 --no-pager || true
    ss -lntp | grep ':80' || true
    exit 1
  fi
}

configure_firewall() {
  echo "[6/7] 配置主机防火墙（若有）..."
  if command -v ufw >/dev/null 2>&1 && ufw status 2>/dev/null | grep -qi 'Status: active'; then
    ufw allow 22/tcp || true
    ufw allow 80/tcp || true
    ufw allow 443/tcp || true
    ufw allow "${FRP_BIND_PORT}/tcp" || true
  fi
  if command -v firewall-cmd >/dev/null 2>&1 && firewall-cmd --state 2>/dev/null | grep -qi running; then
    firewall-cmd --permanent --add-service=ssh || true
    firewall-cmd --permanent --add-service=http || true
    firewall-cmd --permanent --add-service=https || true
    firewall-cmd --permanent --add-port="${FRP_BIND_PORT}/tcp" || true
    firewall-cmd --reload || true
  fi
}

print_status() {
  echo "[7/7] 检查服务 ..."
  systemctl --no-pager --full status frps | head -n 20 || true
  echo
  echo "========== 部署完成 =========="
  echo "入口（隧道通后才有业务）：http://${SERVER_IP}/"
  echo "APK 下载页：              http://${SERVER_IP}/app/"
  echo "frps 控制端口：           ${FRP_BIND_PORT}"
  echo "本机回环映射：            127.0.0.1:${FRP_REMOTE_PORT}"
  echo "token 已写入：            ${INSTALL_DIR}/secrets.env （权限 600）"
  echo
  echo "必须在阿里云安全组放行入方向：TCP 22 / 80 / 443 / ${FRP_BIND_PORT}"
  echo "不要放行 ${FRP_REMOTE_PORT}。"
  echo
  echo "当前 80 在本机未上线时会 502，属正常（等本机 frpc 连上）。"
  echo "验证 frps：systemctl status frps"
  ss -lntp | grep -E ":${FRP_BIND_PORT}|:80|:${FRP_REMOTE_PORT}" || true
}

need_root
ensure_token
detect_pkg
install_pkgs
install_frps
write_frps_config
write_systemd
write_download_page
write_nginx
configure_firewall
print_status
