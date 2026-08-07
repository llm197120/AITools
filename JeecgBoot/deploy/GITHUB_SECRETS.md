# GitHub Secrets 配置说明

> GitHub Actions **仅构建并推送后端镜像**，不在 Actions 中部署或构建前端静态包。

## 必填 Secrets（2 项）

| Secret | 说明 |
|--------|------|
| `ACR_USERNAME` | 阿里云 ACR 登录用户名 |
| `ACR_PASSWORD` | ACR 固定密码 |

## Workflow

| 分支 | Workflow | 产出 |
|------|----------|------|
| `dev` | `build-push-dev.yml` | `homeai-backend` 镜像 |
| `prd` | `build-push-prd.yml` | `homeai-backend` 镜像 |
| `main` / PR | `ci.yml` | 编译验证（含前端 build 检查） |

## 服务器部署

```bash
# 后端（从 ACR 拉取）
./deploy-backend.sh prd

# 管理端（在服务器本机构建 dist 后部署）
cd JeecgBoot/jeecgboot-vue3 && pnpm install && pnpm run build:docker:prod
./deploy-static.sh prd /path/to/jeecgboot-vue3/dist
```

ACR 账号密码配置在 `frontend-nginx/.env`。

## ACR 仓库

命名空间 `liulm`：`homeai-backend`
