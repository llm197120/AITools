# 管理端 Nginx 静态部署方案

后端仍用 **Docker**；管理端为 **宿主机 Nginx + 静态 dist**（从 GitHub Actions Artifacts 下载）。

## 目录说明

| 文件 | 说明 |
|------|------|
| `deploy-backend.sh` | 拉取并启动后端 + 中间件 Docker |
| `deploy-static.sh` | 更新静态目录并安装 Nginx 配置 |
| `docker-compose.prd.yml` | 生产：仅后端 + 中间件 |
| `docker-compose.dev.yml` | 开发：仅后端 + 中间件 |
| `nginx/homeai-admin.prd.conf` | 生产 Nginx 配置 |
| `nginx/homeai-admin.dev.conf` | 开发 Nginx 配置 |
| `env.example` | 复制为 `.env` |

共用 HomeAI 配置：`../env.homeai.example` → 复制为本目录 `.env.homeai`

## 服务器目录（示例）

```text
/opt/homeai/
├── jeecg-boot/db/
└── frontend-nginx/
    ├── deploy-backend.sh
    ├── deploy-static.sh
    ├── docker-compose.prd.yml
    ├── nginx/
    ├── .env
    └── .env.homeai
```

## 依赖

```bash
yum install -y nginx    # 或 apt install nginx
systemctl enable nginx
```

## 首次初始化

```bash
cd /opt/homeai/frontend-nginx
cp env.example .env
cp ../env.homeai.example .env.homeai
chmod +x deploy-backend.sh deploy-static.sh

docker compose -f docker-compose.prd.yml up -d homeai-mysql homeai-redis homeai-pgvector
# scripts/init-db.sh
./deploy-backend.sh prd
```

## 日常发布

### 1. 更新后端

```bash
./deploy-backend.sh prd
./deploy-backend.sh prd prd-a1b2c3d
```

### 2. 更新管理端静态

从 GitHub Actions 下载 Artifact `admin-web-dist-prd`（或 `admin-web-dist-dev`）：

```bash
gh run download <run_id> -n admin-web-dist-prd -D /tmp/admin-dist
./deploy-static.sh prd /tmp/admin-dist
```

`deploy-static.sh` 会：同步 dist → `/var/www/homeai-admin`、安装 Nginx 配置、`nginx -s reload`。

## 对应 GitHub Workflow

使用 **`.github/workflows/build-push-nginx-*.yml`**（推送 `homeai-backend`，上传 `admin-web-dist-*` Artifact，**不**构建管理端 Docker 镜像）。

若采用 Docker 管理端方案，请删除 `build-push-nginx-*.yml`，改用 `build-push-docker-*.yml`。

## 访问

| 环境 | Nginx 端口 | 后端本机端口 | 静态目录 |
|------|------------|--------------|----------|
| prd | 80 | 8080 | `/var/www/homeai-admin` |
| dev | 8088 | 8081 | `/var/www/homeai-admin-dev` |

API 同域路径：`/jeecgboot/`
