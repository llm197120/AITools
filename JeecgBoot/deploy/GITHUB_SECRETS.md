# GitHub Secrets 配置（与 ACR 控制台「访问凭证」对齐）

## 控制台参数（已确认）

| 项 | 值 |
|----|-----|
| 登录用户名 | `liulim` |
| 公网 Registry（GitHub CI login/push） | `crpi-1is06jvzttfocl45.cn-hangzhou.personal.cr.aliyuncs.com` |
| VPC Registry（ECS 同 VPC 拉取） | `crpi-1is06jvzttfocl45-vpc.cn-hangzhou.personal.cr.aliyuncs.com` |
| 命名空间 | `liulm` |
| 仓库名 | `homeai-backend` |

完整 push 路径：

```
crpi-1is06jvzttfocl45.cn-hangzhou.personal.cr.aliyuncs.com/liulm/homeai-backend:<tag>
```

## GitHub Secrets

**Settings → Secrets and variables → Actions**

| Secret | 值 |
|--------|-----|
| `ACR_PASSWORD` | 访问凭证页固定密码（workflow 用户名已固定为 `liulim`） |

> `ACR_USERNAME` 可不配置；CI 已硬编码 `liulim` 与控制台一致。  
> **勿**通过 shell/`GITHUB_ENV` 中转密码（特殊字符会被破坏导致本地成功、CI 失败）。

## 与控制台命令对照

控制台：

```bash
docker login --username=liulim crpi-1is06jvzttfocl45.cn-hangzhou.personal.cr.aliyuncs.com
docker push crpi-1is06jvzttfocl45.cn-hangzhou.personal.cr.aliyuncs.com/liulm/homeai-backend:<tag>
```

CI workflow 与之完全一致（公网域名 + liulm + homeai-backend）。

login 报错中的 `.../v2/` 是 Docker Registry API，**正常现象**。

## 本地验证

```bash
docker login --username=liulim crpi-1is06jvzttfocl45.cn-hangzhou.personal.cr.aliyuncs.com
# 输入访问凭证固定密码 → Login Succeeded
```

本地成功后再写入 GitHub Secrets。注意复制密码时**不要带首尾空格或换行**。

## ECS 拉取（VPC 内网）

ECS 与 ACR 同地域同 VPC 时，`.env` 使用内网域名：

```bash
ACR_REGISTRY=crpi-1is06jvzttfocl45-vpc.cn-hangzhou.personal.cr.aliyuncs.com
ACR_USERNAME=liulim
ACR_PASSWORD=<同上固定密码>
```

镜像路径不变：`.../liulm/homeai-backend:prd-latest`

## Workflow

| 分支 | 文件 | 行为 |
|------|------|------|
| dev | `build-push-dev.yml` | login 公网 → push `homeai-backend:dev-*` |
| prd | `build-push-prd.yml` | login 公网 → push `homeai-backend:prd-*` |

## 仍报 unauthorized 时

1. 在 ACR **访问凭证** 页重置固定密码
2. 本地 `docker login` 验证新密码
3. 更新 GitHub Secret `ACR_PASSWORD`（只需这一项；用户名 workflow 已写死 `liulim`）
4. 若密码含 `$` `%` `&` 等符号，务必用 Secret 直接注入，不要写入 workflow 明文
