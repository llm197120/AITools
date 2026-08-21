# 部署文档

本目录存放环境搭建与部署运维相关文档，以及日常一键发布 / 启停脚本。底层实现仍以 `JeecgBoot/deploy/` 为准，这里只做总入口。

## 一键脚本（日常要用）

在 PowerShell 中执行（若提示无法运行脚本：`Set-ExecutionPolicy -Scope Process Bypass`）。也可双击同名 `.cmd`，会弹出「全部 / 仅后端 / 仅前端 / 仅 APP」菜单。

**不传目标 = 做全部**（启动/停止默认不含 APP，因为 APP 没有本机常驻进程）。

```powershell
cd "C:\Users\57089\Desktop\AI project\AITools\docs\deploy"

# 发布
.\publish-all.ps1                      # 后端 + 前端 + APP
.\publish-all.ps1 -Backend             # 只发后端（编译并重启 :8080）
.\publish-all.ps1 -Frontend            # 只发管理端（构建并发布到本机 Nginx）
.\publish-all.ps1 -App                 # 只打 APK
.\publish-all.ps1 -Backend -Frontend   # 前后端，不出包
.\publish-all.ps1 -Target backend,app  # 等价写法

# 启动 / 停止（不编译、不出包）
.\start-all.ps1 -Backend
.\start-all.ps1 -Frontend
.\stop-all.ps1 -Backend
.\stop-all.ps1 -Frontend
```

| 入口 | 作用 |
|------|------|
| [publish-all.ps1](./publish-all.ps1) | 发布。开关 `-Backend` / `-Frontend` / `-App`（可组合）。APP 可选 `-UploadApk`；Maven 可选 `-Offline` |
| [start-all.ps1](./start-all.ps1) | 启动。`-Backend` 拉起 JeecgBoot（`:8080`）；`-Frontend` 拉起本机 Nginx/frpc |
| [stop-all.ps1](./stop-all.ps1) | 停止。`-Backend` 停 Java；`-Frontend` 停 Nginx/frpc。不停 MySQL/Redis。APP 无本机进程 |

顺序与复用：

1. **后端**：先 `mvn -pl jeecg-system-start -am install`（失败则不停正在跑的 Java）→ 停旧进程 → 在 **启动模块目录** 执行 `mvn spring-boot:run`（不要在父工程加 `-am`，否则会跑到 `jeecg-boot-parent` 上找不到 main class）。日志 `C:\homeai\logs\backend.log`。
2. **管理端**：调用 `JeecgBoot/deploy/frp/setup-local.ps1 -BuildAdmin`（`pnpm run build:docker:prod`，复制到 `C:\homeai\admin`，拉起 nginx/frpc）
3. **APP**：调用 `JeecgUniapp` 的 `pnpm pack:apk:local`。默认不上传播放页；需要时加 `-UploadApk`

首次本机还没有 `C:\homeai\nginx` 时，发布管理端会顺带安装 frpc/Nginx。APP 出包仍需本机 JDK 17、Android SDK、`JeecgUniapp/android-pack.local.json`（勿提交）。发完 APK 后仍须在管理端 **APP版本** 登记，见 [`docs/guide/app-release.md`](../guide/app-release.md)。

## 文档清单

| 文件名 | 内容 | 状态 |
|--------|------|------|
| [frp-home-deployment.md](./frp-home-deployment.md) | **当前采用**：服务器 frps + 本机前后端 + 侧载 APK | 脚本已就绪，待在服务器/本机执行 |
| [github-actions-acr-cicd-design.md](./github-actions-acr-cicd-design.md) | GitHub Actions + 阿里云 ACR CI/CD（ECS 全托管备选） | 已实施配置；与 FRP 方案二选一 |

## 当前部署要点

### 路径 A：FRP 穿透（准备部署）

服务器只跑 frps 与 Nginx；管理端、后端、MySQL/Redis 在本机。APK 放服务器 `/app/` 供用户下载。应用内更新（管理端改库后启动页探测）见 [`docs/guide/app-release.md`](../guide/app-release.md)。操作手册与模板：

- 操作清单：`JeecgBoot/deploy/frp/README.md`
- 本机 → 服务器：`remote-install.ps1`；本机：`setup-local.ps1`
- 手册：`docs/deploy/frp-home-deployment.md`

### 路径 B：ECS 全托管（备选）

| 端 | 方式 | 说明 |
|----|------|------|
| 后端 | GitHub Actions → ACR → ECS Docker Compose | 见 `JeecgBoot/deploy/README.md` |
| 管理端 | ECS 本机 `pnpm run build:docker:prod` + Nginx | 见 `JeecgBoot/deploy/frontend-nginx/` |
| 小程序 | 本地/CI 构建上传微信 | 产品已暂缓上架 |

## 管理端构建对齐

- CI 与部署均使用：`pnpm run build:docker:prod`（读 `.env.docker.prod`，`VITE_GLOB_DOMAIN_URL=/jeecgboot`）
- 部署后核对 `dist/_app.config.js`

## 小程序上架前 Checklist

1. 填写 `env/.env.production` 真实 API（含微信 develop/trial/release 分环境变量）
2. 微信后台配置 request / uploadFile 合法域名
3. `manifest.config.ts`：正式版将 `urlCheck` 改为 `true`，开启 `__usePrivacyCheck__`
4. Android `targetSdkVersion` 建议 ≥ 34（源配置已调至 34）

## 文档分类

- **环境搭建**：开发/测试/生产环境搭建指南
- **部署手册**：应用部署步骤与注意事项
- **运维指南**：日常运维与故障处理
- **配置说明**：各环境配置参数说明

## 命名规范

`{type}-{env}-v{version}.md`

示例：`setup-production-env-v1.0.md`
