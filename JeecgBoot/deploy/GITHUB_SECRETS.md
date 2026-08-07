# GitHub Secrets 配置说明

> GitHub Actions **仅 CI**，不在 Actions 中 SSH 部署。  
> **切勿**将 ACR 密码写入仓库。

## 必填 Secrets（2 项）

| Secret | 说明 |
|--------|------|
| `ACR_USERNAME` | 阿里云 ACR 登录用户名 |
| `ACR_PASSWORD` | ACR 固定密码 |

## Workflow（Nginx 方案）

| 分支 | Workflow | CI 产出 |
|------|----------|---------|
| `dev` | `build-push-nginx-dev.yml` | `homeai-backend` 镜像 + Artifact `admin-web-dist-dev` |
| `prd` | `build-push-nginx-prd.yml` | `homeai-backend` 镜像 + Artifact `admin-web-dist-prd` |
| `main` / PR | `ci.yml` | 编译验证 |

## 服务器部署

目录：`JeecgBoot/deploy/frontend-nginx/`

```bash
./deploy-backend.sh prd
./deploy-static.sh prd /path/to/dist
```

ACR 账号密码配置在 `frontend-nginx/.env`（见 `env.example`）。

## ACR 仓库

命名空间 `liulm`：

- `homeai-backend`
