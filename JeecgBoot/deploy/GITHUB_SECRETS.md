# GitHub Secrets 配置说明

> GitHub Actions **仅构建并推送后端镜像**，不在 Actions 中部署或构建前端静态包。

## 必填 Secrets（2 项）

在 GitHub 仓库 **Settings → Secrets and variables → Actions → New repository secret** 中添加：

| Secret | 值来源 |
|--------|--------|
| `ACR_USERNAME` | ACR 控制台 **访问凭证 → 登录用户名**（个人版实例页） |
| `ACR_PASSWORD` | ACR 控制台 **访问凭证 → 固定密码**（需先「设置固定密码」） |

### 重要：不是阿里云账号密码

`ACR_PASSWORD` 必须是容器镜像服务里单独设置的 **Docker 登录固定密码**，与阿里云官网登录密码不同。

### 获取步骤（个人版）

1. 打开 [容器镜像服务 ACR 控制台](https://cr.console.aliyun.com/)
2. 进入个人版实例（地域杭州）
3. 左侧 **访问凭证** → **设置固定密码**（若未设置）
4. 记录 **登录用户名** 和 **固定密码**，填入 GitHub Secrets

### 本地验证（推荐先做）

在任意已安装 Docker 的机器上测试，成功后再写入 GitHub：

```bash
docker login crpi-1is06jvzttfocl45.cn-hangzhou.personal.cr.aliyuncs.com \
  -u "你的登录用户名" \
  -p "你的固定密码"
```

应显示 `Login Succeeded`。若本地也报 `unauthorized`，说明用户名/密码有误，需在 ACR 控制台重置固定密码。

### 常见错误

| 现象 | 原因 |
|------|------|
| `unauthorized: authentication required` | 用户名或固定密码错误；或用了阿里云账号密码 |
| Secret 名拼写错误 | 必须 exactly `ACR_USERNAME`、`ACR_PASSWORD` |
| 密码含特殊字符 | 复制时勿带首尾空格/换行 |

## Workflow

| 分支 | Workflow | 产出 |
|------|----------|------|
| `dev` | `build-push-dev.yml` | `homeai-backend` 镜像 |
| `prd` | `build-push-prd.yml` | `homeai-backend` 镜像 |
| `main` / PR | `ci.yml` | 编译验证 |

## ACR 仓库

命名空间 `liulm` 下需已创建：`homeai-backend`

完整镜像地址示例：

```
crpi-1is06jvzttfocl45.cn-hangzhou.personal.cr.aliyuncs.com/liulm/homeai-backend:dev-latest
```

## 服务器部署

ACR 账号同样配置在 `frontend-nginx/.env`，见 [frontend-nginx/README.md](./frontend-nginx/README.md)。
