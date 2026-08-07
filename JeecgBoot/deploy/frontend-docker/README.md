# 管理端 Docker 部署方案（备选，当前未启用 CI）

> **当前项目使用 [frontend-nginx](../frontend-nginx/) 方案。**  
> 本目录仅作全 Docker 参考，GitHub Actions 未配置对应 workflow。

## 目录说明

| 文件 | 说明 |
|------|------|
| `deploy.sh` | 一键拉取 ACR 镜像并启动全栈 |
| `docker-compose.prd.yml` | 生产 compose |
| `docker-compose.dev.yml` | 开发 compose |
| `env.example` | 复制为 `.env` |

共用 HomeAI 配置：`../env.homeai.example` → 复制为本目录 `.env.homeai`

## 服务器目录（示例）

```text
/opt/homeai/
├── jeecg-boot/db/              # MySQL 首次 build 用
└── frontend-docker/            # 本目录全部文件
    ├── deploy.sh
    ├── docker-compose.prd.yml
    ├── .env
    └── .env.homeai
```

## 首次初始化

```bash
cd /opt/homeai/frontend-docker
cp env.example .env
cp ../env.homeai.example .env.homeai
chmod +x deploy.sh
# 编辑 .env：ACR 账号、IMAGE_TAG、ADMIN_HTTP_PORT

# 仅启动中间件（首次）
docker compose -f docker-compose.prd.yml up -d homeai-mysql homeai-redis homeai-pgvector
# 执行仓库 scripts/init-db.sh 初始化 HomeAI 表
```

## 日常发布

GitHub push `prd` / `dev` 后，ACR 会更新镜像。在 ECS 执行：

```bash
./deploy.sh prd                  # 使用 prd-latest
./deploy.sh prd prd-a1b2c3d      # 指定 tag
./deploy.sh dev
```

## CI（未启用）

需在本地自行 `pnpm build` + `docker build` 推送 ACR，或恢复 `build-push-docker-*.yml` workflow。

## 访问

| 环境 | 默认端口 |
|------|----------|
| prd | 80 |
| dev | 8088 |

API 同域路径：`/jeecgboot/`
