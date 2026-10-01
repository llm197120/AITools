# 开机自启验证清单（服务主机）

> 目的：确认服务主机**重启后无人登录**也能自动拉起 MySQL/Redis、后端、Nginx 与 FRP 隧道，App 正常可用。
> 架构：本机服务 → frpc 业务隧道 → 阿里云 frps → 公网 `https://liulm.top`。详见 [`frp-home-deployment.md`](./frp-home-deployment.md)。

## 前置：无人值守自启方式（二选一，均需管理员运行一次）

| 方式 | 脚本 | 特点 |
|---|---|---|
| **方式二（推荐）** | [`enable-boot-tasks.ps1`](./enable-boot-tasks.ps1) | 计划任务改为「开机触发 + S4U 无人会话」。**不需要密码、不保持登录**。前置：JAVA_HOME/Maven 在系统级环境变量（本机已满足）。 |
| 方式一 | [`enable-autologon.ps1`](./enable-autologon.ps1) | 开启 Windows 自动登录，重启后照旧走「登录触发」。需要输入账户密码（明文存注册表）。 |

> 当前三个任务是「登录时触发（Interactive）」，所以**必须**先启用上面其中之一，否则重启后无人登录时不会自动拉起。

---

## 一、重启前确认（一次即可）

在服务主机上执行：

```powershell
# 1) 已启用无人值守自启：任务触发器应为「At startup」
Get-ScheduledTask -TaskName 'HomeAI-Backend','HomeAI-FRP','HomeAI-FRP-Admin' |
    Select-Object TaskName,State | Format-Table -AutoSize
(Get-ScheduledTask -TaskName 'HomeAI-FRP').Triggers.CimClass.CimClassName

# 2) 记录当前基线（公网应返回 200）
(Invoke-WebRequest 'https://liulm.top/jeecg-boot/sys/randomImage/homeai-probe' -UseBasicParsing -TimeoutSec 10).StatusCode
```

- [ ] 三个任务都在（`HomeAI-Backend` / `HomeAI-FRP` / `HomeAI-FRP-Admin`）
- [ ] 触发器为开机触发（方式二：`MSFT_TaskBootTrigger`）或自动登录已开启（方式一：`AutoAdminLogon=1`）
- [ ] 公网基线为 `200`
- [ ] 代码为修复后版本（`start-local.ps1` 含 `frpc.toml` 判定）

---

## 二、执行重启

- [ ] 重启服务主机
- [ ] **重启后不要手动登录、不要动鼠标键盘**，等待 3～5 分钟让「开机触发（或自动登录）→ 任务运行 → 服务就绪」

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
| 所有项 FAIL / 根本没自动启动 | 自启方式没生效 | 方式二：重跑 `enable-boot-tasks.ps1`；方式一：重跑 `enable-autologon.ps1`。再确认任务触发器是否正确 |
| 方式二 S4U 下 java/mvn 找不到 | JAVA_HOME/Maven 不在系统级环境变量 | 把二者加到系统级环境变量（`setx /M`），或改用方式一自动登录 |
| 任务根本没触发 | 任务被禁用 / 触发器不对 | `Get-ScheduledTask ... \| Select TaskName,State`；`Start-ScheduledTask -TaskName 'HomeAI-FRP'` |
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

.\enable-boot-tasks.ps1             # 计划任务改「开机触发 + S4U」（管理员；无需密码）
.\enable-boot-tasks.ps1 -Restore    # 还原为「登录触发」
.\enable-autologon.ps1              # 开启自动登录（管理员；输入密码）
.\enable-autologon.ps1 -Disable     # 关闭自动登录并清除密码
```

**远程巡检**（开发机，需先配 `host.env`）：`.\check-remote.ps1`、`.\publish-remote.ps1`，见 [`service-host-migration.md`](./service-host-migration.md)。

---

## 六、相关文件

| 文件 | 作用 |
|---|---|
| [`enable-boot-tasks.ps1`](./enable-boot-tasks.ps1) | 计划任务改「开机触发 + S4U」（推荐，无需密码） |
| [`enable-autologon.ps1`](./enable-autologon.ps1) | 开启/关闭 Windows 自动登录 |
| [`check-host-local.ps1`](./check-host-local.ps1) | 本机端口/进程/隧道/公网体检 |
| `JeecgBoot/deploy/frp/start-local.ps1` | 启动 Nginx + 业务隧道（按 `frpc.toml` 判定） |
| `JeecgBoot/deploy/frp/stop-local.ps1` | 停止 Nginx + 业务隧道（保留运维隧道） |
| `C:\homeai\logs\backend.log` | 后端启动日志 |
| `C:\homeai\nginx\logs\error.log` | Nginx 错误日志 |
| `C:\homeai\frp\frpc.log` | 业务隧道日志 |
