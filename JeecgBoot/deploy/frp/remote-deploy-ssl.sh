#!/bin/bash
# 公网 ECS：定位已上传的阿里云证书，写入系统 Nginx HTTPS 配置。
set -euo pipefail

echo "=== listen before ==="
ss -lntp | grep -E ':80|:443' || true

echo "=== find cert files (names only) ==="
mapfile -t FOUND < <(find /root /etc/nginx /www /opt /tmp /home /usr/local/nginx /var/www /ssl /data -xdev \
  \( -iname '*.pem' -o -iname '*.key' -o -iname '*.crt' \) 2>/dev/null | head -n 80)
printf '%s\n' "${FOUND[@]:-}"

PEM=""
KEY=""
for f in "${FOUND[@]:-}"; do
  [ -z "$f" ] && continue
  case "$f" in
    *privkey*|*private*|*.key) KEY="${KEY:-$f}" ;;
  esac
  case "$f" in
    *fullchain*|*.pem|*.crt) PEM="${PEM:-$f}" ;;
  esac
done
for f in "${FOUND[@]:-}"; do
  echo "$f" | grep -qi liulm || continue
  echo "$f" | grep -qiE '\.key$|priv' && KEY="$f"
  echo "$f" | grep -qiE '\.pem$|\.crt$|fullchain|cert' && PEM="$f"
done

echo "picked PEM=$PEM"
echo "picked KEY=$KEY"
if [ -z "$PEM" ] || [ -z "$KEY" ]; then
  echo "CERT_NOT_FOUND"
  exit 20
fi

install -d -m 755 /etc/nginx/ssl/liulm.top
cp -f "$PEM" /etc/nginx/ssl/liulm.top/fullchain.pem
cp -f "$KEY" /etc/nginx/ssl/liulm.top/privkey.key
chmod 644 /etc/nginx/ssl/liulm.top/fullchain.pem
chmod 600 /etc/nginx/ssl/liulm.top/privkey.key
chown root:root /etc/nginx/ssl/liulm.top/*

if [ -x /www/server/nginx/sbin/nginx ]; then
  /www/server/nginx/sbin/nginx -s stop 2>/dev/null || true
fi
if command -v bt >/dev/null 2>&1; then
  bt stop 2>/dev/null || true
fi
pkill -f '/www/server/nginx/sbin/nginx' 2>/dev/null || true
sleep 1

install -d /var/www/acme /etc/nginx/conf.d
if [ -f /etc/nginx/conf.d/homeai.conf ]; then
  cp -a /etc/nginx/conf.d/homeai.conf "/etc/nginx/conf.d/homeai.conf.bak.$(date +%Y%m%d%H%M)"
fi

cat > /etc/nginx/conf.d/homeai.conf <<'NGX'
# HomeAI 公网入口 HTTPS（由 remote-deploy-ssl.sh 写入）
server {
    listen 80;
    server_name liulm.top www.liulm.top;

    location ^~ /.well-known/acme-challenge/ {
        root /var/www/acme;
        default_type text/plain;
        allow all;
    }

    location / {
        return 301 https://$host$request_uri;
    }
}

server {
    listen 443 ssl;
    http2 on;
    server_name liulm.top www.liulm.top;

    ssl_certificate     /etc/nginx/ssl/liulm.top/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/liulm.top/privkey.key;
    ssl_session_timeout 1d;
    ssl_session_cache shared:SSL:10m;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_prefer_server_ciphers off;

    add_header Strict-Transport-Security "max-age=31536000" always;

    client_max_body_size 2g;

    location = /app {
        return 301 /app/;
    }
    location /app/ {
        alias /var/www/homeai-apk/;
        index index.html;
        autoindex off;
        add_header Cache-Control "no-store, must-revalidate" always;
        types {
            application/vnd.android.package-archive apk;
            text/html html;
            text/plain txt;
        }
    }

    location / {
        proxy_pass http://127.0.0.1:18080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_connect_timeout 10s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
        proxy_buffering off;
    }
}
NGX

if ! nginx -t 2>/tmp/nginx-t.err; then
  if grep -q 'http2' /tmp/nginx-t.err; then
    sed -i 's/listen 443 ssl;/listen 443 ssl http2;/' /etc/nginx/conf.d/homeai.conf
    sed -i '/http2 on;/d' /etc/nginx/conf.d/homeai.conf
    nginx -t
  else
    cat /tmp/nginx-t.err
    exit 21
  fi
fi

systemctl enable nginx
systemctl restart nginx
systemctl is-active nginx
echo "=== listen after ==="
ss -lntp | grep -E ':80|:443' || true
echo "=== curl ==="
curl -sI --max-time 8 http://127.0.0.1/ | head -n 8 || true
curl -skI --max-time 8 https://127.0.0.1/ | head -n 12 || true
echo SSL_DEPLOY_OK
