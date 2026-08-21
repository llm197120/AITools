# HomeAI：FRP 反向穿透部署方案

> 状态：脚本已就绪，按快速清单落地  
> 公网 IP：**116.62.115.226**  
> **先看操作清单：** [JeecgBoot/deploy/frp/README.md](../../JeecgBoot/deploy/frp/README.md)  
> 适用：家庭/小范围内测。公网服务器只跑 **frps + Nginx**；**管理端 + 后端 + 数据库** 跑在本机。用户用手机下载侧载 APK。  
> 与现有 `JeecgBoot/deploy/`（ECS 上跑 Docker 全套）是**另一条路径**，互不替代。

| 谁执行 | 脚本 |
|--------|------|
| 本机 → 服务器一键装 | `JeecgBoot/deploy/frp/remote-install.ps1` |
| 仅在服务器上装 | `JeecgBoot/deploy/frp/install-server.sh` |
| 本机 frpc + Nginx | `JeecgBoot/deploy/frp/setup-local.ps1` |

下文保留架构说明；逐步命令以 README 为准。

---

## 1. 目标与边界

| 要达成 | 不做 / 不暴露 |
|--------|----------------|
| 手机 App 通过公网访问本机后端 API | 不把 MySQL `3306`、Redis `6379`、ComfyUI、frps Dashboard 打到公网 |
| 浏览器通过公网打开管理端 | 不把本机 `pnpm dev` 当生产入口 |
| 用户从下载页安装 APK | APK **放服务器本地**，不经家庭宽带下载（避免隧道被大文件占满） |
| 本机关机/断网时 App 提示连不上 | 不承诺 7×24 高可用 |

**流量瓶颈在本机上行带宽。** 资料文件继续走阿里云 OSS 预签名（`jeecg.uploadType=alioss`），不要改成本地盘再经 FRP 回源。

---

## 2. 总体架构

```
用户手机 App / 浏览器
        │  HTTP 或 HTTPS
        ▼
公网服务器（仅入口）
  Nginx :80 / :443
    ├─ /app/          → 本机静态目录（APK + 下载页）     ← 不走隧道
    └─ / 与 /jeecg-boot/ → 127.0.0.1:18080              ← 走隧道
              │
         frps :7000（仅控制面，token 鉴权）
              │ 加密隧道
              ▼
本机 Windows（业务全在这里）
  frpc
    └─ 映射 本机 8088 → 服务器 127.0.0.1:18080
         │
    本机 Nginx :8088
      ├─ /              → 管理端 dist
      └─ /jeecg-boot/   → 127.0.0.1:8080/jeecg-boot/
           │
      JeecgBoot :8080
      MySQL / Redis（仅本机）
      ComfyUI（仅本机，后端直连）
```

一条隧道同时带管理端和 API。App 的 API 基址与浏览器访问的是**同一个公网入口**。

### 地址约定（部署前填实）

| 占位符 | 含义 | 示例 |
|--------|------|------|
| `SERVER_IP` | 服务器公网 IP | `116.62.115.226` |
| `PUBLIC_BASE` | App / 浏览器入口 | `http://116.62.115.226` |
| `API_BASE` | App 写入 `.env.production` | `http://116.62.115.226/jeecg-boot` |
| `DOWNLOAD_URL` | 给用户的下载页 | `http://116.62.115.226/app/` |

无域名、未备案时走 **HTTP + 公网 IP** 即可（App 已开 `usesCleartextTraffic: true`）。有域名后再在**服务器 Nginx** 上终止 TLS，本机和 FRP 仍走明文 TCP（隧道本身有 token，不暴露到公网）。

---

## 3. 职责划分

| 位置 | 跑什么 | 公网是否可达 |
|------|--------|----------------|
| 服务器 | frps、Nginx、APK 静态目录 | 22 / 80 / 443 / 7000 |
| 本机 | MySQL、Redis、JeecgBoot、管理端 dist、本机 Nginx、frpc | 否（只出站连 frps） |
| 用户手机 | 侧载 APK | 访问 `PUBLIC_BASE` |

服务器防火墙：**只放行** `22`、`80`、`443`、`7000`。frp 的 `18080` 必须绑在 `127.0.0.1`，禁止对公网监听。

---

## 4. 服务器端部署

**推荐从本机一键安装**（生成 `secrets.env` 并 SSH 到 116.62.115.226）：

```powershell
cd JeecgBoot\deploy\frp
.\remote-install.ps1
```

不能 SSH 时，把 `install-server.sh` 拷到服务器，root 执行并传入与本机相同的 `FRP_TOKEN`。脚本安装 frps 0.71.0、Nginx、`/app/` 下载页，映射端口只绑 `127.0.0.1:18080`。

阿里云安全组放行 `22/80/443/7000`，不要放行 `18080`。有域名后在**服务器 Nginx** 上 certbot 即可。

以下为脚本未可用时的手工对照（与脚本等价）。安装目录 `/opt/homeai-frp/`。frps 与 frpc **版本必须一致**。

### 4.1 安装 frps

```bash
sudo mkdir -p /opt/homeai-frp /var/www/homeai-apk
cd /tmp
# 把 X.Y.Z 换成当前稳定版
wget https://github.com/fatedier/frp/releases/download/vX.Y.Z/frp_X.Y.Z_linux_amd64.tar.gz
tar xf frp_X.Y.Z_linux_amd64.tar.gz
sudo cp frp_X.Y.Z_linux_amd64/frps /opt/homeai-frp/
sudo cp /path/to/repo/JeecgBoot/deploy/frp/frps.toml /opt/homeai-frp/frps.toml
# 把 token 改成足够长的随机串
sudo nano /opt/homeai-frp/frps.toml
```

`frps.toml` 要点：

- `bindPort = 7000`：本机 frpc 连入
- `auth.token`：与 frpc 相同，视为密钥
- `proxyBindAddr = "127.0.0.1"`：映射端口只给本机 Nginx 用
- Dashboard 绑 `127.0.0.1:7500`，不要映射到公网

systemd 单元见 `JeecgBoot/deploy/frp/frps.service`。启用：

```bash
sudo cp JeecgBoot/deploy/frp/frps.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now frps
sudo systemctl status frps
```

### 4.2 安装 Nginx（入口 + APK）

```bash
sudo apt install -y nginx
sudo cp JeecgBoot/deploy/frp/nginx-server.conf /etc/nginx/sites-available/homeai
sudo ln -sf /etc/nginx/sites-available/homeai /etc/nginx/sites-enabled/homeai
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t && sudo systemctl reload nginx
```

无域名时 `nginx-server.conf` 监听 80 即可。有域名后用 certbot：

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d homeai.example.com
```

证书只装在服务器；本机不用申请证书。

### 4.3 防火墙

```bash
sudo ufw allow 22/tcp
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp
sudo ufw allow 7000/tcp
sudo ufw enable
```

确认 `ss -lntp` 里 **没有** `0.0.0.0:18080`。

### 4.4 下载页目录

```bash
sudo mkdir -p /var/www/homeai-apk
sudo cp JeecgBoot/deploy/frp/download/index.html /var/www/homeai-apk/
# APK 稍后用 scp 上传，见第 6 节
```

---

## 5. 本机部署

**推荐一键脚本**（下载 frpc / Windows Nginx，写 `C:\homeai\` 并启动）：

```powershell
cd JeecgBoot\deploy\frp
.\setup-local.ps1
# 需要公网打开管理端时再加：  .\setup-local.ps1 -BuildAdmin
# 改 APK 内嵌地址：        .\setup-local.ps1 -PatchAppEnv
# 登录 Windows 自动拉起：  .\setup-local.ps1 -RegisterStartup
.\start-local.ps1
.\stop-local.ps1
```

前置：MySQL、Redis、JeecgBoot 已在 `8080`。本机需长期开机（或家人要用 App 的时段），关闭睡眠/休眠。详细清单见 [frp/README.md](../../JeecgBoot/deploy/frp/README.md)。

### 5.0 手工对照（脚本已覆盖，可跳过）

### 5.1 中间件与后端

与平时开发一致：

1. MySQL、Redis 已在跑（或 Docker）。
2. 后端：`jeecg-system-start`，端口 **8080**，context-path **`/jeecg-boot`**。
3. 生产向运行用打包 jar + NSSM/计划任务，不要长期挂着 IDE。

后端建议：

- `jeecg.uploadType=alioss`（资料走 OSS，不经隧道）
- 管理端默认账号改强密码
- 本机防火墙：**不要**把 3306 / 6379 / 8080 映射到路由器公网

ComfyUI 只本机访问，不要进 frpc。

### 5.2 管理端静态包

不要用 `pnpm dev` 对外。在本机构建：

```bash
cd JeecgBoot/jeecgboot-vue3
pnpm install --frozen-lockfile
pnpm run build:docker:prod
```

构建后打开 `dist/_app.config.js`，把接口改成**同域相对路径**（经本机 Nginx 反代）：

- `VITE_GLOB_API_URL` = `/jeecg-boot`
- `VITE_GLOB_DOMAIN_URL` = `/jeecg-boot`

把 `dist/` 拷到本机 Nginx 根目录，例如 `C:\homeai\admin\`。

### 5.3 本机 Nginx

Windows 可装 [nginx for Windows](https://nginx.org/en/download.html) 或用 Docker 跑官方 nginx 镜像，监听 **8088**。配置见 `JeecgBoot/deploy/frp/nginx-home.conf`。

本机自测：

```text
http://127.0.0.1:8088/            → 管理端
http://127.0.0.1:8088/jeecg-boot/ → 后端（勿对公网开放 8088）
```

### 5.4 安装 frpc 并开机自启

1. 下载与服务器**同一版本**的 `frp_*_windows_amd64.zip`。
2. 解压到 `C:\homeai\frp\`，放入改好的 `frpc.toml`（`JeecgBoot/deploy/frp/frpc.toml`）。
3. `serverAddr` 填 `SERVER_IP`，`auth.token` 与 frps 相同。
4. 开机自启（任选其一）：
   - 任务计划程序：登录时运行 `C:\homeai\frp\frpc.exe -c C:\homeai\frp\frpc.toml`
   - [NSSM](https://nssm.cc/) 注册为 Windows 服务

本机启动顺序建议：MySQL → Redis → JeecgBoot → 本机 Nginx → frpc。

### 5.5 本机健康检查

服务器上：

```bash
curl -I http://127.0.0.1:18080/jeecg-boot/
curl -I http://SERVER_IP/jeecg-boot/
```

隧道通时应是后端响应，而不是 Nginx 502。502 = 本机 frpc / Nginx / 后端挂了。

---

## 6. APP 下载方案

### 6.1 打包前改 API 地址

编辑 `JeecgUniapp/env/.env.production`：

```ini
VITE_SERVER_BASEURL = 'http://116.62.115.226/jeecg-boot'
VITE_UPLOAD_BASEURL = 'http://116.62.115.226/jeecg-boot'
VITE_SERVER_BASEURL_APP = 'http://SERVER_IP/jeecg-boot'
VITE_UPLOAD_BASEURL_APP = 'http://SERVER_IP/jeecg-boot'
```

有 HTTPS 域名后四条都改成 `https://域名/jeecg-boot`。

**不要**再填局域网 IP（当前仓库里的 `192.168.222.157` 只适合家里 Wi-Fi 内测）。App 启动会探测 `10.0.2.2` / `127.0.0.1` / 缓存地址；真机上前两者会失败，最终落到上面的公网地址。若手机里缓存过旧的 `homeai_api_base`，让用户清 App 数据或在内测页改地址。

### 6.2 打签名包

**发版只走本机：** `pnpm pack:apk:local`（H5 + Capacitor），见 [`docs/guide/android-local-apk.md`](../guide/android-local-apk.md)。包名 `com.homeai.app`，证书不要换。keystore 与 `android-pack.local.json` 不要进 git。

HBuilderX 云打包已弃用。

### 6.3 上传到服务器（推荐）

在本机（PowerShell）：

```powershell
.\JeecgBoot\deploy\frp\upload-apk.ps1 -ApkPath 'JeecgUniapp\dist\apk\homeai-release.apk' -Version '1.0.0'
```

或手动 scp：

```powershell
scp .\JeecgUniapp\dist\apk\homeai-release.apk user@SERVER_IP:/tmp/homeai.apk
```

在服务器：

```bash
sudo mv /tmp/homeai.apk /var/www/homeai-apk/homeai-latest.apk
sudo chmod 644 /var/www/homeai-apk/homeai-latest.apk
# 可选：写版本号给下载页展示
echo "1.0.0" | sudo tee /var/www/homeai-apk/version.txt
```

用户打开 `{PUBLIC_BASE}/app/`，点「下载 APK」。大文件不走家庭宽带。

### 6.4 用户安装步骤（发给家人即可）

1. 用手机浏览器打开下载页（可把链接做成二维码印在纸上）。
2. 下载 `homeai-latest.apk`。
3. 允许「安装未知应用」（仅给该浏览器或文件管理器）。
4. 安装后打开，用已注册的手机号 + 密码登录。
5. 若提示无法连接：确认本机电脑已开机、frpc 在跑；过几分钟再试。

微信/QQ 内置浏览器有时拦 APK：改用 Chrome / 系统浏览器，或先保存到「文件」再点开。

### 6.5 发版节奏

完整步骤（改 `versionCode`、打 APK / H5 zip、管理端「APP版本」登记、强制更新）见 [`docs/guide/app-release.md`](../guide/app-release.md)。

| 步骤 | 谁做 |
|------|------|
| 改代码、测通本机 | 开发机 |
| 递增 `manifest.config.ts` 的 `versionCode` | 开发机 |
| 改 `.env.production` 后重打 APK（地址变了必须打 APK） | 开发机 |
| `pnpm pack:apk:local` 得到 APK 与 `homeai-h5-*.zip` | 开发机 |
| 管理端 **APP版本**：上传安装包、打开「对 APP 生效」 | 开发机 |
| `upload-apk.ps1` 覆盖下载页 `homeai-latest.apk` | 开发机 → 服务器 |
| 已装更新器的用户：下次启动 APP 内更新 | 用户 |
| 旧包 / 首次安装：进下载页再下一个新包 | 用户 |

第 69 轮之后的 Capacitor 包会在启动页探测版本。更早的包没有探测逻辑，仍须走下载页。API 地址变了必须重打 APK，不能只发热更新 zip。

---

## 7. 联调验收清单

- [ ] 本机 `http://127.0.0.1:8088/` 能登录管理端
- [ ] 本机 `http://127.0.0.1:8088/jeecg-boot/sys/randomImage/homeai-probe` 有响应
- [ ] `frps` / `frpc` 日志显示 proxy 在线
- [ ] 手机 **4G（不要连家里 Wi-Fi）** 打开 `{PUBLIC_BASE}/app/` 能下 APK
- [ ] 同一部手机用 App 登录、拉列表、上传一张图（OSS 预签名可打开）
- [ ] 关掉本机 frpc 后 App 失败；再开恢复
- [ ] 服务器 `ss -lntp`：18080 仅 127.0.0.1；3306/6379 不在服务器上

---

## 8. 日常运维

| 现象 | 处理 |
|------|------|
| App 连不上 | 看本机是否睡眠；`frpc` 服务是否在；服务器 `curl 127.0.0.1:18080` |
| 管理端 502 | 本机 Nginx 或后端没起来 |
| 下载 APK 慢/失败 | 查的是服务器磁盘与 Nginx，与隧道无关 |
| 上传资料失败但 API 正常 | 查 OSS 密钥与桶，不是 FRP |
| 公网 IP 变了（服务器） | 改 frpc `serverAddr` + App 环境变量并重打包 |
| 本机家宽 IP 变了 | **不用管**，这正是 FRP 要解决的 |

本机建议：电源「高性能」、禁用睡眠；frpc、Nginx、Java 做成开机服务。可选小 UPS，避免闪断电把数据库打坏。

---

## 9. 安全（最低限度）

1. frp `token` 当密码，不要用仓库示例值。
2. 改掉 Jeecg 默认 `admin` 密码；管理端仅家人使用。
3. SSH 密钥登录，禁止密码；可再限制 22 来源 IP。
4. 不要把 Knife4j / Druid 控制台映射到公网（若必须，只本机访问）。
5. 有域名后上 HTTPS，再把 App 的 `usesCleartextTraffic` 改为 `false` 并重打包。
6. 应用商店上架前仍要做域名 ICP + APP 备案；**侧载内测不走商店，不替代备案**。

---

## 10. 以后怎么演进

| 阶段 | 做法 |
|------|------|
| 现在 | 本文：服务器入口 + 本机业务 + 侧载 APK |
| 有域名 / 证书 | 只改服务器 Nginx + App 四条 URL，重打 APK |
| 不想再依赖本机开机 | 把 MySQL/Redis/后端/管理端迁到现有 `JeecgBoot/deploy/frontend-nginx`；服务器卸 frps；App 基址不变或只改域名 |

---

## 11. 相关路径

| 路径 | 用途 |
|------|------|
| `JeecgBoot/deploy/frp/` | frps/frpc/Nginx/下载页模板 |
| `JeecgUniapp/env/.env.production` | APK 内嵌 API 地址 |
| `JeecgUniapp/src/pages-homeai/platform/env.ts` | 真机探测与本地覆盖 |
| `docs/plan/android-migration-design.md` | 备案前侧载路径 |
| `JeecgBoot/deploy/README.md` | 另一条：ECS Docker 全托管 |
