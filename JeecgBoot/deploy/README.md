# HomeAI 部署总览

GitHub Actions **仅 CI + 推 ACR**；**ECS 本机**使用 **Nginx 静态方案**部署管理端。

## 当前方案：frontend-nginx

| 组件 | 方式 |
|------|------|
| 后端 | Docker 镜像 `homeai-backend` |
| 管理端 | 宿主机 Nginx + 静态 dist（GitHub Artifacts） |
| MySQL / Redis | Docker |

→ 详细步骤：[frontend-nginx/README.md](./frontend-nginx/README.md)

## CI Workflow

| 分支 | Workflow | 产出 |
|------|----------|------|
| `dev` | `build-push-nginx-dev.yml` | 后端镜像 + `admin-web-dist-dev` |
| `prd` | `build-push-nginx-prd.yml` | 后端镜像 + `admin-web-dist-prd` |
| `main` / PR | `ci.yml` | 编译验证 |

## 服务器发布

```bash
cd frontend-nginx
./deploy-backend.sh prd
./deploy-static.sh prd /path/to/dist
```

## 共用文件

| 文件 | 说明 |
|------|------|
| [env.homeai.example](./env.homeai.example) | HomeAI 运行时密钥 → 复制为 `frontend-nginx/.env.homeai` |
| [lib/common.sh](./lib/common.sh) | 部署脚本公共函数 |
| [GITHUB_SECRETS.md](./GITHUB_SECRETS.md) | GitHub Actions Secrets |

## 服务器公共准备

1. 安装 Docker / Compose、Nginx
2. 复制 `JeecgBoot/jeecg-boot/db` 到服务器 `../jeecg-boot/db`（相对 `frontend-nginx` 目录）
3. 首次启动中间件后执行 `scripts/init-db.sh`

## 备选：frontend-docker

`frontend-docker/` 目录保留作全 Docker 参考方案，**当前 CI 未启用**，需自行本地构建镜像。
