# 部署文档

本目录存放环境搭建与部署运维相关文档，以及日常一键发布 / 启停脚本。底层实现仍以 `JeecgBoot/deploy/` 为准，这里只做总入口。

**9 月末把服务迁到另一台电脑：** 先看 [service-host-migration.md](./service-host-migration.md)（架构、空系统初始化、远程发布/巡检、导库、验收）。人在外开关机、SSH/RDP、智能插座/WoL 见 [service-host-remote-control.md](./service-host-remote-control.md)。日常脚本说明见下文。

## 一键脚本（日常要用）

在 PowerShell 中执行（若提示无法运行脚本：`Set-ExecutionPolicy -Scope Process Bypass`）。也可双击同名 `.cmd`（仅 ASCII 启动器，菜单在 PowerShell 里弹出，避免 cmd 按 GBK 拆碎 UTF-8 中文）。

**不传目标 = 做全部**（启动/停止默认不含 APP，因为 APP 没有本机常驻进程；默认会顺带启停文档预览 Docker：Gotenberg `:3000`、kkFileView `:8012`）。

首次把仓库拷到**只装了 Windows 的新电脑**时，先管理员运行 [init-windows-host.ps1](./init-windows-host.ps1)（装 Git / JDK 17 / Maven / Node / LibreOffice / Docker，并可选拉起 MySQL+Redis 容器）。现有已装原生 MySQL 的开发机不要跑它。

```powershell
cd "C:\Users\57089\Desktop\AI project\AITools\docs\deploy"

# 发布
.\publish-all.ps1                      # 后端 + 前端 + APP + 文档预览
.\publish-all.ps1 -Backend             # 只发后端（编译并重启 :8080，并拉起文档预览）
.\publish-all.ps1 -Frontend            # 只发管理端（构建并发布到本机 Nginx）
.\publish-all.ps1 -App                 # 只打 APK
.\publish-all.ps1 -Backend -Frontend   # 前后端，不出包
.\publish-all.ps1 -Backend -SkipDocsPreview
.\publish-all.ps1 -Target backend,app  # 等价写法

# 启动 / 停止（不编译、不出包）
.\start-all.ps1 -Backend
.\start-all.ps1 -Frontend
.\start-all.ps1 -DocsPreview
.\start-all.ps1 -Backend -SkipDocsPreview
.\stop-all.ps1 -Backend
.\stop-all.ps1 -Frontend
.\stop-all.ps1 -DocsPreview
.\stop-all.ps1 -Backend -KeepDocsPreview
```

| 入口 | 作用 |
|------|------|
| [init-windows-host.ps1](./init-windows-host.ps1) | **空系统新主机**一键装依赖。`-Phase Apps` 装软件；重启并打开 Docker 后 `-Phase Datastores` 拉起 MySQL/Redis + 文档预览。会写入 `C:\homeai\use-docker-datastores`，之后 `start-all` 才会管这两个容器 |
| [check-remote.ps1](./check-remote.ps1) / [publish-remote.ps1](./publish-remote.ps1) | **开发机**远程检查 / 发布到服务主机。先复制 [host.env.example](./host.env.example) 为 `host.env`。日常：这台电脑开发 → `publish-remote`（push + 远程 pull + 远程 publish-all）。APK 仍在本机打 |
| [publish-all.ps1](./publish-all.ps1) | 发布。开关 `-Backend` / `-Frontend` / `-App` / `-DocsPreview`（可组合）。发后端时默认 `docker compose up` 文档预览；不要容器时加 `-SkipDocsPreview`。APP 可选 `-UploadApk`；Maven 可选 `-Offline` |
| [start-all.ps1](./start-all.ps1) | 启动。`-Backend` 拉起 JeecgBoot（`:8080`）并默认拉起文档预览；`-Frontend` 拉起本机 Nginx/frpc；`-DocsPreview` 只起 Gotenberg/kkFileView |
| [stop-all.ps1](./stop-all.ps1) | 停止。`-Backend` 停 Java 并默认 `docker compose stop` 文档预览；`-KeepDocsPreview` 只停 Java。`-Frontend` 停 Nginx/frpc。不停 MySQL/Redis。APP 无本机进程 |

顺序与复用：

1. **后端**：先 `mvn -pl jeecg-system-start -am install`（失败则不停正在跑的 Java）→ 停旧进程 → 在 **启动模块目录** 执行 `mvn spring-boot:run`（不要在父工程加 `-am`，否则会跑到 `jeecg-boot-parent` 上找不到 main class）。日志 `C:\homeai\logs\backend.log`。
2. **文档预览（可选）**：`JeecgBoot/deploy/docs-preview/docker-compose.yml` → Gotenberg `http://127.0.0.1:3000`、kkFileView `http://127.0.0.1:8012`。未装 Docker 时跳过，不阻断后端。启用转换/管理端预览须在管理端「系统配置 → Office」填写上述本机地址（公网地址会被拒绝）。
3. **管理端**：调用 `JeecgBoot/deploy/frp/setup-local.ps1 -BuildAdmin`（`pnpm run build:docker:prod`，复制到 `C:\homeai\admin`，拉起 nginx/frpc）
4. **APP**：调用 `JeecgUniapp` 的 `pnpm pack:apk:local`。默认不上传播放页；需要时加 `-UploadApk`

首次本机还没有 `C:\homeai\nginx` 时，发布管理端会顺带安装 frpc/Nginx。APP 出包仍需本机 JDK 17、Android SDK、`JeecgUniapp/android-pack.local.json`（勿提交）。发完 APK 后仍须在管理端 **APP版本** 登记，见 [`docs/guide/app-release.md`](../guide/app-release.md)。

## 开发机远程发布 / 检查（服务跑在另一台 Windows）

步骤、验收与导库见 **[service-host-migration.md](./service-host-migration.md)**。摘要：

1. **主机**跑过 `init-windows-host.ps1`（含 OpenSSH Server），仓库 `git clone` 到 `C:\homeai\src\AITools`。把开发机 `id_ed25519.pub` 写入主机用户 `~\.ssh\authorized_keys`。
2. **开发机**复制 `docs/deploy/host.env.example` → `host.env`，填局域网 `HOST_IP`。
3. 日常：`git commit` 后执行 `.\publish-remote.ps1`。APK 仍用本机 `publish-all.ps1 -App`。
4. 只检查：`.\check-remote.ps1`。启停：`.\remote-host.ps1 -Action Start|Stop`。

## 文档清单

| 文件名 | 内容 | 状态 |
|--------|------|------|
| [service-host-migration.md](./service-host-migration.md) | **9 月末迁机**：本机开发 + 另一台 Windows 跑服务；初始化、远程发布、导库、验收 | 方案已记录，脚本已就绪，待月末执行 |
| [service-host-remote-control.md](./service-host-remote-control.md) | 服务主机远程控制：Tailscale / FRP 跳板、关机重启、智能插座来电自启、WoL | 方案已记录，待迁机时按清单执行 |
| [frp-home-deployment.md](./frp-home-deployment.md) | 服务器 frps + 家庭隧道 + 侧载 APK | 脚本已就绪；隧道「家里一头」迁机后改挂服务主机 |
| [startup-verify.md](./startup-verify.md) | 开机自启验证清单：重启前后检查项、故障对照表 | 已就绪 |
| [enable-autologon.ps1](./enable-autologon.ps1) | 开启 / 关闭 Windows 自动登录（无人值守自启·方式一） | 已就绪 |
| [enable-boot-tasks.ps1](./enable-boot-tasks.ps1) | 计划任务改开机触发 + S4U 无人会话（无人值守自启·方式二·无需密码） | 已就绪 |
| [github-actions-acr-cicd-design.md](./github-actions-acr-cicd-design.md) | GitHub Actions + 阿里云 ACR CI/CD（ECS 全托管备选） | 已实施配置；与 FRP 方案二选一 |

## 当前部署要点

### 路径 A：FRP 穿透（准备部署）

服务器只跑 frps 与 Nginx；**迁机前**管理端、后端、MySQL/Redis 在本机。**9 月末起**改到另一台服务主机，见 [service-host-migration.md](./service-host-migration.md)。APK 仍放服务器 `/app/`。应用内更新见 [`docs/guide/app-release.md`](../guide/app-release.md)。

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
