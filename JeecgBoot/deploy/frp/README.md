# HomeAI FRP 快速部署（本机操作清单）

公网 IP 已写入 `config.env`：**116.62.115.226**。密钥在 `secrets.env`（自动生成，勿提交）。

在 **PowerShell** 中执行（先进入本目录）：

```powershell
cd "C:\Users\57089\Desktop\AI project\AITools\JeecgBoot\deploy\frp"
```

若提示无法运行脚本：

```powershell
Set-ExecutionPolicy -Scope Process Bypass
```

---

## 1. 服务器（约 2–5 分钟）

本机能 SSH 到服务器时：

```powershell
.\remote-install.ps1
# 非 root 用户：
# .\remote-install.ps1 -SshUser ubuntu
# 指定密钥：
# .\remote-install.ps1 -SshIdentityFile $env:USERPROFILE\.ssh\AITools0820
# .\remote-install.ps1 -SshIdentityFile AITools0820
```

脚本只建立 **一次 SSH**。密码两种方式（二选一）：

1. 运行时输入一次（不配文件时）
2. 在已 gitignore 的 `secrets.env` 里加（推荐，之后不再询问）：

```
SSH_USER=root
SSH_PASSWORD=你的SSH密码
```

不要把 `secrets.env` 提交到 git。有密钥时用 `-SshIdentityFile`，不必写密码。

不能从本机 SSH 时：把 `install-server.sh` 拷到服务器，以 root 执行：

```bash
export FRP_TOKEN='填 secrets.env 里的同一串'
bash install-server.sh
```

**阿里云安全组**必须放行入方向 TCP：`22`、`80`、`443`、`7000`。不放行 `18080`。

完成后：下载页 `http://116.62.115.226/app/` 应能打开（业务入口此时可能 502，等本机 frpc）。

---

## 2. 本机（约 1–3 分钟，不含构建管理端）

前置：MySQL、Redis、JeecgBoot 后端已在 `8080` 运行（和平时开发一样）。

```powershell
.\setup-local.ps1
```

会下载 frpc / Windows Nginx，写入 `C:\homeai\`，并启动。App 只需要 `/jeecg-boot` 反代，**不必先构建管理端**。

常用参数：

| 命令 | 作用 |
|------|------|
| `.\setup-local.ps1 -BuildAdmin` | 构建并发布管理端到 `C:\homeai\admin`（几分钟） |
| `.\setup-local.ps1 -PatchAppEnv` | 把 UniApp 生产 API 改成 `http://116.62.115.226/jeecg-boot` |
| `.\setup-local.ps1 -RegisterStartup` | 登录 Windows 时自动启动 nginx/frpc |
| `.\start-local.ps1` / `.\stop-local.ps1` | 日常启停本机 nginx/frpc（**不**动 Java 后端） |
| 仓库总入口 `docs/deploy/publish-all.ps1` / `start-all.ps1` / `stop-all.ps1` | 一键发布 APP+前后端；启停含 Java |

本机自测：

- http://127.0.0.1:8088/jeecg-boot/sys/randomImage/homeai-probe
- 关掉睡眠；家里路由器**不要**映射 3306/6379/8080/8088

---

## 3. 验收

1. 电脑已开、后端 8080、nginx/frpc 在跑。
2. 手机 **4G** 打开 http://116.62.115.226/jeecg-boot/ （应有后端响应，不是 502）。
3. 管理端（若已 `-BuildAdmin`）：http://116.62.115.226/

---

## 4. 打 APK（给家人下载）

下载页能打开但点「下载」404，是因为服务器上还没有 `homeai-latest.apk`（目前只有说明页）。先签名打包再上传：

```powershell
.\setup-local.ps1 -PatchAppEnv
# 然后按现有流程签名打包，再上传：
.\upload-apk.ps1 -ApkPath 'D:\path\homeai-release.apk' -Version '1.0.0'
```

用户用系统浏览器打开 http://116.62.115.226/app/ 安装。微信内置浏览器常拦 APK。

---

完整说明（架构/安全/备案）：[docs/deploy/frp-home-deployment.md](../../../docs/deploy/frp-home-deployment.md)
