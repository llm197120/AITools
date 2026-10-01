# 开机自启验证清单（服务主机）

> 目的：确认服务主机**重启后无人登录**也能自动拉起 MySQL/Redis、后端、Nginx 与 FRP 隧道，App 正常可用。
> 架构：本机服务 → frpc 业务隧道 → 阿里云 frps → 公网 `https://liulm.top`。详见 [`frp-home-deployment.md`](./frp-home-deployment.md)。
> 前置：已运行 [`enable-autologon.ps1`](./enable-autologon.ps1) 开启 Windows 自动登录。

---

## 一、重启前确认（一次即可）

在服务主机上执行：

```powershell
# 1) 自动登录已开启（应为 1）
(Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon').AutoAdminLogon

# 2) 三个计划任务存在（登录时触发）
Get-ScheduledTask -TaskName 'HomeAI-Backend','HomeAI-FRP','HomeAI-FRP-Admin' |
    Select-Object TaskName,State | Format-Table -AutoSize

# 3) 记录当前基线（公网应返回 200）
(Invoke-WebRequest 'https://liulm.top/jeecg-boot/sys/randomImage/homeai-probe' -UseBasicParsing -TimeoutSec 10).StatusCode
```

- [ ] `AutoAdminLogon = 1`
- [ ] 三个任务都在（`HomeAI-Backend` / `HomeAI-FRP` / `HomeAI-FRP-Admin`）
- [ ] 公网基线为 `200`
- [ ] 代码为修复后版本（`start-local.ps1` 含 `frpc.toml` 判定）

---

## 二、执行重启

- [ ] 重启服务主机
- [ ] **重启后不要手动登录、不要动鼠标键盘**，等待 3～5 分钟让「自动登录 → 任务触发 → 服务就绪」

---

## 三、重启后检查（无人登录状态下操作）

### 3.1 一键体检（首选）

在服务主机上执行：

```powershell
cd 'D:\project\AITools\docs\deploy'
.\check-host-local.ps1
```

**预期结果（关键项全部 `[OK]`）：**

| 检查项 | 预期 |
|---|---|
| MySQL :3306 | OK |
| Redis :6379 | OK |
| JeecgBoot :8080 | OK |
| Nginx :8088 | OK |
| 进程 nginx | OK |
| **frpc 业务隧道 (frpc.toml -> 18080)** | **OK** ← 今天故障的关键项 |
| frpc 运维隧道 (frpc-admin.toml) | OK（WARN 可接受） |
| 本机探测 `127.0.0.1:8088/jeecg-boot/...` | OK (200) |
| 公网探测 `https://liulm.top/jeecg-boot/...` | OK (200) |
| 无 `失败：` 汇总行 | — |

### 3.2 公网可达（从任意设备）

```powershell
(Invoke-WebRequest 'https://liulm.top/jeecg-boot/sys/randomImage/homeai-probe' -UseBasicParsing -TimeoutSec 10).StatusCode
```

- [ ] 返回 `200`（若 `502`，见「四」）

### 3.3 App 验证

- [ ] 打开 APP，能进入首页 / 正常登录（不再「加载失败」）
- [ ] 个人中心 → **服务器状态** 显示 🟢

---

## 四、故障对照表

| 现象 / 检查项 | 可能原因 | 处理 |
|---|---|---|
| 公网 502，且「frpc 业务隧道」FAIL | 业务隧道没起来（历史 bug：被运维隧道误判为已运行而跳过） | 手动 `.\start-all.ps1 -Frontend`；确认 `start-local.ps1` 为修复版；必要时重启 `HomeAI-FRP` 任务 |
| 所有项 FAIL / 根本没自动登录 | `AutoAdminLogon` 未生效或密码错 | 重新运行 `.\enable-autologon.ps1` 设置密码 |
| 任务根本没触发 | 任务被禁用 / 触发器非登录触发 | `Get-ScheduledTask ... \| Select TaskName,State`；`Start-ScheduledTask -TaskName 'HomeAI-FRP'` |
| JeecgBoot :8080 FAIL | 后端起不来（环境/端口/依赖） | 看 `C:\homeai\logs\backend.log` 末尾；确认 JDK/Maven 可用 |
| Nginx :8088 FAIL | nginx 未起或端口被占 | 看 `C:\homeai\nginx\logs\error.log` |
| MySQL/Redis FAIL | 数据库/缓存服务未起 | 确认对应服务（或 Docker 容器）已启动 |
| 「frpc 运维隧道」WARN | 运维隧道未起（不影响 App） | 确认 `HomeAI-FRP-Admin` 任务；或手动 `frpc.exe -c C:\homeai\frp\frpc-admin.toml` |

> 排查后**务必再跑一次 `check-host-local.ps1`**，直到无 `失败：`。

---

## 五、日常手动操作（备查）

```powershell
cd 'D:\project\AITools\docs\deploy'

.\start-all.ps1 -Backend            # 只起后端（含文档预览，无则跳过）
.\start-all.ps1 -Frontend           # 只起 Nginx + 业务 frpc + 运维 frpc
.\start-all.ps1                     # 全部（不含 APP，APP 无本机进程）
.\stop-all.ps1 -Frontend            # 停 Nginx + 业务隧道（保留运维隧道）
.\stop-all.ps1 -Backend             # 停后端

.\enable-autologon.ps1              # 开启自动登录（管理员 + 输入密码）
.\enable-autologon.ps1 -Disable     # 关闭自动登录并清除密码
```

**远程巡检**（开发机，需先配 `host.env`）：`.\check-remote.ps1`、`.\publish-remote.ps1`，见 [`service-host-migration.md`](./service-host-migration.md)。

---

## 六、相关文件

| 文件 | 作用 |
|---|---|
| [`enable-autologon.ps1`](./enable-autologon.ps1) | 开启/关闭 Windows 自动登录 |
| [`check-host-local.ps1`](./check-host-local.ps1) | 本机端口/进程/隧道/公网体检 |
| `JeecgBoot/deploy/frp/start-local.ps1` | 启动 Nginx + 业务隧道（按 `frpc.toml` 判定） |
| `JeecgBoot/deploy/frp/stop-local.ps1` | 停止 Nginx + 业务隧道（保留运维隧道） |
| `C:\homeai\logs\backend.log` | 后端启动日志 |
| `C:\homeai\nginx\logs\error.log` | Nginx 错误日志 |
| `C:\homeai\frp\frpc.log` | 业务隧道日志 |
