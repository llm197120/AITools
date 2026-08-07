# GitHub Secrets 配置说明

> GitHub Actions **仅 CI**，不在 Actions 中 SSH 部署。  
> **切勿**将 ACR 密码写入仓库。

## 必填 Secrets（2 项）

| Secret | 说明 |
|--------|------|
| `ACR_USERNAME` | 阿里云 ACR 登录用户名 |
| `ACR_PASSWORD` | ACR 固定密码 |

## Workflow 与方案对应（二选一）

请只保留所采用方案的一组 workflow，**删除另一组**，避免同一 push 重复构建：

| 管理端方案 | dev | prd | CI 产出 |
|------------|-----|-----|---------|
| **Docker** | `build-push-docker-dev.yml` | `build-push-docker-prd.yml` | `homeai-backend` + `homeai-admin-web` 镜像 |
| **Nginx 静态** | `build-push-nginx-dev.yml` | `build-push-nginx-prd.yml` | `homeai-backend` 镜像 + `admin-web-dist-*` Artifact |

`ci.yml`（main / PR）与方案无关，始终保留。

## 服务器部署（不在 GitHub Secrets）

| 方案 | 目录 | 脚本 |
|------|------|------|
| Docker | `JeecgBoot/deploy/frontend-docker/` | `./deploy.sh prd` |
| Nginx | `JeecgBoot/deploy/frontend-nginx/` | `./deploy-backend.sh prd` + `./deploy-static.sh prd /path/to/dist` |

ACR 账号密码配置在**各方案目录**的 `.env` 中（见各目录 `env.example`）。

## ACR 仓库

命名空间 `liulm`：

- `homeai-backend`（两种方案都需要）
- `homeai-admin-web`（仅 Docker 方案需要）
