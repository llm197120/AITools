# HomeAI 部署总览

GitHub Actions **仅 CI + 推 ACR**；**ECS 本机**选择以下一种管理端方案部署（二选一）。

## 方案对比

| | [frontend-docker](./frontend-docker/) | [frontend-nginx](./frontend-nginx/) |
|---|--------------------------------------|-------------------------------------|
| 管理端 | Docker 容器（内置 Nginx） | 宿主机 Nginx + 静态 dist |
| 部署脚本 | `deploy.sh` | `deploy-backend.sh` + `deploy-static.sh` |
| CI Workflow | `build-push-docker-*.yml` | `build-push-nginx-*.yml` |
| ACR 镜像 | `homeai-backend` + `homeai-admin-web` | 仅 `homeai-backend` |
| CI 产物 | Docker 镜像 | 后端镜像 + Artifacts 静态包 |

后端两种方案均使用 Docker。

## 快速选择

**方案 A — 全 Docker（推荐入门）**

→ 阅读 [frontend-docker/README.md](./frontend-docker/README.md)

**方案 B — Nginx 静态（更轻、便于改 Nginx）**

→ 阅读 [frontend-nginx/README.md](./frontend-nginx/README.md)

## 共用文件

| 文件 | 说明 |
|------|------|
| [env.homeai.example](./env.homeai.example) | HomeAI 运行时密钥，复制到各方案目录 `.env.homeai` |
| [lib/common.sh](./lib/common.sh) | 部署脚本公共函数 |
| [GITHUB_SECRETS.md](./GITHUB_SECRETS.md) | GitHub Actions Secrets（仅 ACR） |

## GitHub Workflow 说明

仓库内提供两套 workflow，**请只保留你选用方案的一组**（删除或重命名另一组 `.yml`，避免重复构建）：

| 方案 | dev | prd |
|------|-----|-----|
| Docker | `build-push-docker-dev.yml` | `build-push-docker-prd.yml` |
| Nginx | `build-push-nginx-dev.yml` | `build-push-nginx-prd.yml` |

`ci.yml`（main / PR）与方案无关，始终保留。

## 服务器公共准备

1. 安装 Docker / Compose
2. 复制 `JeecgBoot/jeecg-boot/db` 到服务器 `../jeecg-boot/db`（相对各方案目录）
3. 首次启动中间件后执行 `scripts/init-db.sh`
