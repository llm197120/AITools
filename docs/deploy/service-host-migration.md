# 服务主机迁移手册（本机开发 + 另一台 Windows 长期跑服务）

> **计划窗口：2026 年 9 月末** 把家庭 AI 小工具的**运行时**迁到另一台只装了系统的电脑。  
> 本机（当前开发机）继续写代码、打 APK。公网入口仍是阿里云 FRP（`116.62.115.226`），只把隧道的「家里这一头」从本机换到服务主机。  
> 脚本已就绪；本文是月末落地时的单一对照清单。相关迭代：路线图第 152～155 轮。

---

## 1. 三台机器各干什么

| 机器 | 角色 | 跑什么 | 不跑什么 |
|------|------|--------|----------|
| **本机（长期使用）** | 开发 / 出包 | Git、JDK、Android SDK、日常改代码、`publish-all -App` | 迁完后不再当 7×24 的 API 主机 |
| **服务主机（9 月末新装）** | 家庭服务 | JeecgBoot、本机 Nginx、frpc、Docker（MySQL/Redis/Gotenberg/kkFileView） | 不装 Android SDK、不打 APK、不跑 ComfyUI（除非另做） |
| **阿里云** | 公网入口 | frps、入口 Nginx、APK 下载页 `/app/` | 不跑 Java / 不跑业务库 |

App 和浏览器仍然访问 `http://116.62.115.226/`，**不必改手机里的 API 地址**（除非你同时改了域名）。变的是：frpc 从哪台电脑连出去。

当前 FRP 细节仍以 [frp-home-deployment.md](./frp-home-deployment.md) 与 [JeecgBoot/deploy/frp/README.md](../../JeecgBoot/deploy/frp/README.md) 为准。ECS 整栈 Docker 是另一条备选路径，**本次不走**。

---

## 2. 已拍板的架构（不要临时改）

### 2.1 为什么不把前后端也装进 Docker

本机方案已经按 **Windows 原生** 写好：`mvn spring-boot:run`、Office COM / LibreOffice、`C:\homeai\nginx`、frpc。Gotenberg / kkFileView 是别人的 Linux 发行物，适合用镜像。

| 层 | 怎么跑 | 原因 |
|----|--------|------|
| 后端 Java、管理端静态资源、本机 Nginx、frpc | 宿主机进程 | 与现有 `start-all` / `publish-all` / `setup-local` 一致 |
| MySQL、Redis | Docker（仅服务主机） | 空系统不必再装原生库；端口仍是 `127.0.0.1:3306` / `:6379`，Java 配置不用改 |
| Gotenberg `:3000`、kkFileView `:8012` | Docker | 可选文档转换 / 管理端复杂预览 |
| 本机（开发机）上的 MySQL/Redis | **保持原生、不动** | 没有 `C:\homeai\use-docker-datastores` 时，`start-all` **不会**去拉 MySQL 容器，避免和现有库抢端口 |

标记文件：`C:\homeai\use-docker-datastores` 只由主机上的 `init-windows-host.ps1` 创建。

### 2.2 网络与安全（迁机后仍然有效）

- 路由器 / 云安全组：**不要**映射 `3306`、`6379`、`8080`、`8088`、`3000`、`8012`。SSH `22` 只在家里局域网，不要映射到公网。
- 主机防火墙：OpenSSH 仅「专用网络」放行 22；业务端口不要对「公用网络」开放。
- 文档转换：管理端「系统配置 → Office」只填本机/局域网地址（`http://127.0.0.1:3000`、`http://127.0.0.1:8012`）。公网 SaaS 地址会被拒绝。
- Gotenberg：Java 把文件 POST 到本机映射端口即可。
- kkFileView：会自己再拉「文件外链」。文件在 OSS 时容器出网即可；若外链是 `127.0.0.1`，容器里的 localhost 不是宿主机，须用局域网 IP 或 `host.docker.internal`（见 `JeecgBoot/deploy/docs-preview/docker-compose.yml` 注释）。
- 不要把 3000/8012 打进 FRP 公网。

---

## 3. 迁机前在本机先做完（9 月中下旬）

- [ ] 本机仓库在 `dev`，能 `git push` 到远程（主机靠 `git pull` 拿代码）。
- [ ] 阿里云 frps 已按 FRP 清单装好，下载页 `http://116.62.115.226/app/` 能开。
- [ ] 记下本机要带走的秘密（**不要提交 git**）：
  - `application-dev-local.yml`（OSS）
  - `JeecgBoot/deploy/frp/secrets.env`（FRP token、SSH 密码等）
  - 管理端账号；MySQL 若不是默认 `root/123456` 则记下真实密码
- [ ] 决定是否迁移旧库数据：需要则本机导出 `jeecg` 库（见第 6 节）；全新库则主机跑 `scripts\init-db.bat` 即可。
- [ ] 本机生成 SSH 密钥（已有 `id_ed25519` 可跳过）：

```powershell
ssh-keygen -t ed25519 -C "homeai-dev"
```

公钥在 `%USERPROFILE%\.ssh\id_ed25519.pub`，月末拷到主机。

- [ ] 复制 `docs/deploy/host.env.example` 为 `docs/deploy/host.env`（已 gitignore），`HOST_IP` 先空着，主机入网后再填局域网 IP。
- [ ] 确认服务主机：Windows 10/11 x64、能装 Docker Desktop（需虚拟化）、接电源长期开机、关掉睡眠；与本机同一局域网。

**不要在本机运行 `init-windows-host.ps1`。**

---

## 4. 月末当天：服务主机从空系统到可远程发布

工作目录约定：主机仓库 `C:\homeai\src\AITools`（与 `host.env.example` 默认 `REMOTE_REPO` 一致）。

### 4.1 把仓库放到主机

任选其一：

- 主机已能上网：安装 Git 后 `git clone` 到 `C:\homeai\src\AITools`（可先只装 Git，或拷 U 盘后再跑初始化）。
- 主机暂时没 Git：U 盘拷贝整个 `AITools` 到上述目录，初始化脚本会再装 Git。

### 4.2 管理员初始化（分两段，中间重启）

```powershell
cd C:\homeai\src\AITools\docs\deploy
Set-ExecutionPolicy -Scope Process Bypass
.\init-windows-host.ps1
```

或右键 `init-windows-host.cmd`「以管理员身份运行」。

会安装：Git、JDK 17、Maven、Node（pnpm 9）、LibreOffice、Docker Desktop、OpenSSH Server；启用长路径；接通电源时禁止睡眠；创建 `C:\homeai`；写入 `use-docker-datastores` 与 `JeecgBoot\deploy\runtime\.env`（默认 MySQL `root/123456`，与 `application-dev.yml` 一致）。

**重启** → 登录 Windows → 打开 Docker Desktop 等到就绪 → 再执行：

```powershell
cd C:\homeai\src\AITools\docs\deploy
.\init-windows-host.ps1 -Phase Datastores
```

拉起 MySQL `:3306`（库 `jeecg`）、Redis `:6379`、Gotenberg `:3000`、kkFileView `:8012`。

可选：`.\init-windows-host.ps1 -SkipLibreOffice`（只靠 Gotenberg）；`-HardenFirewall`（公用网络拦截业务入站）。

### 4.3 SSH 免密（本机 → 主机）

在**主机**当前登录用户下：

```powershell
New-Item -ItemType Directory -Force -Path "$env:USERPROFILE\.ssh" | Out-Null
# 把开发机 id_ed25519.pub 的整行追加进去
notepad "$env:USERPROFILE\.ssh\authorized_keys"
```

主机「设置 → 网络」使用**专用网络**。本机试连：

```powershell
ssh 主机用户名@主机局域网IP
```

### 4.4 配置、导库、FRP 本机端

1. 复制  
   `JeecgBoot\jeecg-boot\jeecg-module-system\jeecg-system-start\src\main\resources\application-dev-local.yml.example`  
   为 `application-dev-local.yml`，填 OSS（可从本机拷）。改了 MySQL 密码则同步改 yml 或 `runtime\.env`。
2. 导入库：`scripts\init-db.bat`，或导入本机备份（第 6 节）。
3. 把本机 `JeecgBoot\deploy\frp\secrets.env` 拷到主机同一路径（token 必须与云上 frps 一致）。
4. 主机执行：

```powershell
cd C:\homeai\src\AITools\JeecgBoot\deploy\frp
.\setup-local.ps1 -BuildAdmin -RegisterStartup
```

`-RegisterStartup`：Windows 登录时自动拉 nginx/frpc。再发布一次后端（可在主机本地 `docs\deploy\publish-all.ps1 -Backend -Frontend`，或本机 `publish-remote.ps1`）。

5. 管理端登录 → **系统配置 → Office**：  
   Gotenberg `http://127.0.0.1:3000`  
   kkFileView `http://127.0.0.1:8012`

6. **本机关掉**原来的 nginx/frpc/Java（避免两台 frpc 抢同一条隧道）：`docs\deploy\stop-all.ps1`。

### 4.5 开发机 `host.env`

```text
HOST_IP=<主机局域网IP>
SSH_USER=<主机 Windows 用户名>
SSH_PORT=22
SSH_IDENTITY=C:\Users\57089\.ssh\id_ed25519
REMOTE_REPO=C:\homeai\src\AITools
GIT_REMOTE=origin
GIT_BRANCH=dev
PUBLIC_BASE=http://116.62.115.226
```

本机验证：

```powershell
cd docs\deploy
.\check-remote.ps1
```

局域网探测 + 公网 `http://116.62.115.226/` + SSH 主机本机检查均应通过。手机 **4G** 打开公网入口，确认不是 502。

---

## 5. 迁完之后的日常（本机操作）

```powershell
cd docs\deploy

# 代码已 commit 并要上到主机
.\publish-remote.ps1              # push → 主机 pull → 远程 publish-all（后端+管理端）→ 检查

.\check-remote.ps1                # 只巡检
.\remote-host.ps1 -Action Start   # 远程 start-all
.\remote-host.ps1 -Action Stop
.\remote-host.ps1 -Action Pull    # 只拉代码不发布

# APK 仍在本机
.\publish-all.ps1 -App -UploadApk -RegisterVersion
```

未提交的改动**不会**同步到主机。只发后端或只发管理端：`.\publish-remote.ps1 -Backend` / `-Frontend`。跳过 push：`-SkipPush`。

主机侧脚本（应急时坐在那台电脑上）：`start-all.ps1` / `stop-all.ps1` / `publish-all.ps1` / `check-host-local.ps1`。`stop-all` **不停** MySQL/Redis 容器。

---

## 6. 数据从本机迁到主机

在**本机**（原生 MySQL）导出，在主机 Docker MySQL 导入。库名 `jeecg`。

```powershell
# 本机导出（按你的 root 密码改）
mysqldump -h127.0.0.1 -uroot -p --databases jeecg --default-character-set=utf8mb4 > $env:USERPROFILE\Desktop\jeecg-backup.sql
```

拷到主机后：

```powershell
# 主机：容器已在听 3306
Get-Content -Raw .\jeecg-backup.sql | docker exec -i homeai-mysql mysql -uroot -p123456 --default-character-set=utf8mb4
```

若密码不是 `123456`，以 `JeecgBoot\deploy\runtime\.env` 为准。导入后如有菜单/配置缓存，可按现有习惯清 Redis（`scripts\flush-redis.bat` 在主机执行，或 `docker exec homeai-redis redis-cli FLUSHALL`——会清掉配额等缓存，选维护窗口做）。

只迁代码、接受空业务数据：跳过 dump，只跑 `init-db.bat`。

---

## 7. 月末验收清单

- [ ] 主机开机后 Docker、sshd 在跑；登录后 nginx/frpc 能起来（若注册了开机启动）。
- [ ] 本机 `check-remote.ps1`：`3306/6379/8080/8088` 为 OK；`3000/8012` 可为 WARN（未填 Office 地址时转换仍回退本机 Office）。
- [ ] `http://127.0.0.1:8088/jeecg-boot/sys/randomImage/homeai-probe` 在主机本机有响应。
- [ ] 公网 `http://116.62.115.226/` 管理端、`/jeecg-boot/` API、4G 手机 App 能登录。
- [ ] 本机 frpc **已停止**，只有主机一条隧道。
- [ ] Office 两项本机地址已保存；抽测一次 PDF 转换或管理端预览（可选）。
- [ ] 本机仍能 `pnpm pack:apk:local` / `publish-all.ps1 -App`。

失败时：本机重新 `start-all.ps1` + `JeecgBoot\deploy\frp\start-local.ps1`，主机 `stop-all.ps1` 并停主机 frpc，隧道切回本机（库已迁走则本机 API 是旧数据，只作应急入口）。

---

## 8. 脚本与文件索引

| 路径 | 用途 |
|------|------|
| [init-windows-host.ps1](./init-windows-host.ps1) | 空系统主机装依赖 + OpenSSH；`-Phase Datastores` 拉数据面 |
| [JeecgBoot/deploy/runtime/docker-compose.yml](../../JeecgBoot/deploy/runtime/docker-compose.yml) | 主机 MySQL / Redis |
| [JeecgBoot/deploy/docs-preview/docker-compose.yml](../../JeecgBoot/deploy/docs-preview/docker-compose.yml) | Gotenberg / kkFileView |
| [host.env.example](./host.env.example) | 复制为 `host.env`（勿提交） |
| [publish-remote.ps1](./publish-remote.ps1) / [check-remote.ps1](./check-remote.ps1) / [remote-host.ps1](./remote-host.ps1) | 本机远程发布、巡检、启停 |
| [check-host-local.ps1](./check-host-local.ps1) | 主机本机健康检查（SSH 会调它） |
| [publish-all.ps1](./publish-all.ps1) / [start-all.ps1](./start-all.ps1) / [stop-all.ps1](./stop-all.ps1) | 单机发布与启停（主机本地或远程调用） |
| [frp-home-deployment.md](./frp-home-deployment.md) | 云服务器 frps + 家庭隧道 |

---

## 9. 明确不做（本次迁机）

- 不把本机开发环境改成 Docker 全栈。
- 不把 Gotenberg / kkFileView 改成本机 Windows 服务。
- 不在服务主机装 Android 出包链。
- 不把 SSH 或数据库端口暴露到公网。
- 不在本机执行 `init-windows-host.ps1`。
