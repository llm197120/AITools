# 服务主机远程控制（开关机 / 远控 / 带外恢复）

> 状态：方案已记录，待迁机时按清单执行（不改现网脚本默认行为）  
> 配合：[service-host-migration.md](./service-host-migration.md)（装机与发布）、[frp-home-deployment.md](./frp-home-deployment.md)（公网入口）  
> 原则：**用户流量走阿里云 frps + Nginx；人的运维通道不暴露到安全组。** 主机关机后 FRP 业务隧道会断，开机必须走带外手段（WoL 或智能插座）。

---

## 1. 要解决什么

迁机后业务跑在一台**非云服务器的 Windows** 上。开发机可能与它：


| 场景                   | 网络关系                | 远控怎么走                            |
| -------------------- | ------------------- | -------------------------------- |
| **同屋局域网**（迁机手册默认）    | 开发机 ↔ 主机同一 Wi-Fi/有线 | 现有 `host.env` + 局域网 SSH / RDP 即可 |
| **人在外、主机在家**         | 开发机走公网              | Tailscale（首选）或 FRP 管理隧道（备选）      |
| **主机放到另一处**（亲戚家、办公室） | 两地都有宽带              | 同上；开机必须该处有「叫醒者」或智能插座             |



| 能力              | 操作系统必须在线？ | 推荐                                |
| --------------- | --------- | --------------------------------- |
| 远程桌面、SSH、发布、看日志 | 是         | Tailscale 或 FRP 跳板；禁止公网直开 3389/22 |
| 远程关机 / 重启       | 是         | SSH：`shutdown /s` / `shutdown /r` |
| 远程开机（已正常关机）     | 否         | 同网 WoL，或智能插座 + BIOS 来电自启          |
| 死机 / 蓝屏 / 进不了系统 | 否         | 智能插座硬断电再上电（来电自启）；消费级一般没有 AMT      |


向日葵 / ToDesk 只作应急，不能替代关机后的开机，也不要把它们的端口映射到阿里云。

---



## 2. 架构（业务与运维分离）

```
用户 App / 浏览器
        │  HTTPS（liulm.top）
        ▼
阿里云 ECS
  Nginx :80/:443  →  127.0.0.1:18080
  frps :7000（安全组可放行；映射口仍绑 127.0.0.1）
        │
        │  业务隧道 homeai-web（已有）
        ▼
服务主机 Windows
  frpc → 本机 Nginx :8088 → Java / 管理端
  MySQL / Redis（仅本机）

运维（人）
  开发机 ── Tailscale ── 服务主机 :22 / :3389
       或 ── SSH 到阿里云 ── 本机转发 ── frpc 管理隧道（可选）

带外开机（OS 已关）
  开发机 → 智能插座云 App → 断电/通电 → BIOS 来电自启
  或 开发机 → Tailscale → 同网小盒子 → 发 WoL 魔术包
```

阿里云 **发不出** 家庭局域网的 Wake-on-LAN。安全组继续只放行 `22`（云主机自身 SSH）、`80`、`443`、`7000`。**不要**放行 `3389`、主机 `22`、`18022`、`13389`、`3306`、`8080`。

`frps.toml` 已设 `proxyBindAddr = "127.0.0.1"`：即使以后加了 SSH/RDP 的 `remotePort`，也只听在云服务器本地，外网连不上。

---



## 3. 怎么选（拍板）


| 优先级       | 方案                                  | 何时用                              |
| --------- | ----------------------------------- | -------------------------------- |
| **P0 必做** | 电源策略 + 登录自启业务 + BIOS 来电自启 + 智能插座    | 任何场景；停电/死机/人在外都能把电「重新插上」         |
| **P1 推荐** | Tailscale（开发机 + 服务主机）               | 人经常不在主机旁边                        |
| **P1 同网** | 现有 OpenSSH + `host.env`             | 开发机和服务主机同一局域网（迁机手册第 4.3 / 4.5 节） |
| **P2 可选** | 同网永远在线的软路由 / 树莓派发 WoL               | 想优雅开机、少硬断电                       |
| **P2 可选** | FRP 再映射 SSH/RDP 到云 `127.0.0.1`      | 不想装 Tailscale，用阿里云当跳板            |
| **不做**    | 公网映射 3389；路由器 WAN 放行 UDP 9 给任意人 WoL | 暴露面大                             |
| **一般不做**  | Intel AMT / 服务器 IPMI                | 家用小主机多数没有                        |


推荐组合：**日常 SSH/RDP（局域网或 Tailscale）+ 系统命令开关机；关机后用智能插座来电自启；有软路由再加 WoL。**

业务公网入口不变，不必改 App API 地址。

---



## 4. P0：主机自身（迁机当天一起做）

在**服务主机**上执行。与 [init-windows-host.ps1](./init-windows-host.ps1) 互补：脚本已禁睡眠、装 OpenSSH；下列项脚本不覆盖 BIOS 和插座。

### 4.1 电源与启动

1. **接通电源时永不睡眠**（初始化脚本会写电源计划；迁完后在「电源和睡眠」再确认显示器可关、电脑不睡眠）。
2. 管理员 PowerShell：

```powershell
powercfg /h off
powercfg /change standby-timeout-ac 0
powercfg /change standby-timeout-dc 0
```

1. 「控制面板 → 电源选项 → 选择电源按钮的功能」→ 取消「启用快速启动」（来电自启更稳）。
2. BIOS/UEFI（开机按 Del/F2，以主板为准）：
  - `Restore AC Power Loss` / `After Power Loss` / 「AC 电源恢复」→ **Power On**（来电开机）
  - 若用 WoL：`Wake on LAN` / 「PME」打开；用**有线网卡**，关机不断网线、不断主机电源（只按系统关机）。
3. 业务自启：主机执行 `JeecgBoot\deploy\frp\setup-local.ps1 -RegisterStartup`（登录后 nginx/frpc）。Windows 登录：可设自动登录给「服务账号」（本机密码强度要够；不要用管理员日常冲浪账号）。Java / Docker 按迁机手册保证开机后起来。



### 4.2 智能插座（远程开机的保底）

1. 插座只接**主机主机箱电源**（显示器、音箱不要绑同一路，避免误关外设即可）。
2. App 能远程通断电；先在现场试一次：断电 10 秒 → 通电 → 主机应自己 POST 进 Windows。
3. 日常关机仍用系统 `shutdown`；插座硬断电只用于：已关机、死机、人在外且 SSH 不通。
4. 硬断电可能让 MySQL 做 crash recovery，一般能起来；维护窗口尽量先 SSH 正常关机再断电。



### 4.3 防火墙（远控相关）

- OpenSSH：仅「专用网络」放行 22（迁机手册已要求主机网络配置文件为专用）。
- RDP：若启用，防火墙只放行专用网络或仅 Tailscale 网卡（见第 5.3 节）。
- 业务口 `3306` / `6379` / `8080` / `8088` / `3000` / `8012`：**不要**对公用网络开放；不要映射到路由器 WAN。

---



## 5. P1：系统在线时的远控



### 5.1 同局域网（迁机默认）

开发机 `docs/deploy/host.env` 填主机局域网 IP，日常：

```powershell
cd docs\deploy
.\check-remote.ps1
.\publish-remote.ps1
.\remote-host.ps1 -Action Start   # 或 Stop
```

手动：

```powershell
ssh 主机用户名@主机局域网IP
shutdown /r /t 0
shutdown /s /t 0
```

RDP：`mstsc` 连主机局域网 IP（主机「设置 → 远程桌面」打开）。

### 5.2 Tailscale（人在外首选）

1. 开发机、服务主机都安装并登录同一 Tailscale 账号（个人免费档通常够用）。
2. 两台都「已连接」后，记下主机的 `100.x.x.x`。
3. 开发机 `host.env` 的 `HOST_IP` 改为该地址（脚本只认 IP/主机名，不关心是不是局域网）。
4. 验证：`ssh 用户@100.x.x.x`，再跑 `check-remote.ps1`。
5. 可选：Tailscale ACL 只允许开发机访问主机的 22/3389。

关机后 Tailscale 不可用，开机回到第 4.2 / 第 6 节。

### 5.3 Windows 远程桌面收紧（可选）

```powershell
# 仅示例：先 ipconfig / all 看 Tailscale 适配器名再改
Get-NetFirewallRule -DisplayGroup "远程桌面" | Format-Table Name, Enabled, Profile
```

更稳妥：系统设置里打开远程桌面，防火墙配置文件只留「专用」；公用网络关掉 RDP。装了 Tailscale 后用 `100.x` 连即可。

### 5.4 开发机常用命令

```powershell
# 重启（等几分钟再 SSH；来电自启 + 登录自启 frpc 后公网应恢复）
ssh 用户@HOST_IP "shutdown /r /t 0"

# 关机（之后必须插座或 WoL 才能再开）
ssh 用户@HOST_IP "shutdown /s /t 0"

# 取消尚未执行的关机
ssh 用户@HOST_IP "shutdown /a"
```

---



## 6. P2：Wake-on-LAN

在智能插座之外提供「不拔电」的开机。

### 6.1 主机侧

1. 设备管理器 → 有线网卡 → 电源管理：允许计算机关闭此设备以节约电源（按网卡说明；部分卡需勾选「允许此设备唤醒计算机」「只允许幻数据包唤醒」）。
2. 记下有线 MAC。关机后网卡指示灯常仍微亮。
3. 无线网卡 WoL 不可靠，**不要作为唯一开机手段**。



### 6.2 谁发魔术包

必须与主机**同一二层网络**（同一路由 LAN）。候选：

- 家里已有的软路由 / 旁路由（OpenWrt 等带 WoL）
- 树莓派、小盒子，装 Tailscale，你在外 SSH 上去再发包

开发机在外网、中间只有阿里云时，**不能**直接对公网 IP 发 WoL。

示例（在同网 Linux 小盒子上，把 MAC 换成主机网卡）：

```bash
# Debian/Ubuntu 常有 wakeonlan 包
wakeonlan aa:bb:cc:dd:ee:ff
```

Windows 同网可用第三方小工具或 `Send-MagicPacket` 脚本；不把 UDP 9 从 WAN 暴露给全网。

---



## 7. P2：FRP 管理隧道（不装 Tailscale 时）

现网 `frpc.toml` 只有业务口 `homeai-web`（本机 8088 → 云 `127.0.0.1:18080`）。需要时在**服务主机** `C:\homeai\frp\frpc.toml` 追加，**不要**改 `proxyBindAddr`。

```toml
# 运维专用：只绑在云服务器 127.0.0.1（frps 全局 proxyBindAddr）
[[proxies]]
name = "homeai-ssh"
type = "tcp"
localIP = "127.0.0.1"
localPort = 22
remotePort = 18022

[[proxies]]
name = "homeai-rdp"
type = "tcp"
localIP = "127.0.0.1"
localPort = 3389
remotePort = 13389
```

阿里云安全组**不要**为 18022 / 13389 增加入站规则。

开发机先登云主机再转发：

```powershell
# 身份文件按你现有 ECS 密钥；用户按实际（root 或 ubuntu）
ssh -N -L 2222:127.0.0.1:18022 -L 13389:127.0.0.1:13389 root@116.62.115.226
```

另开窗口：

```powershell
ssh -p 2222 主机Windows用户@127.0.0.1
mstsc /v:127.0.0.1:13389
```

仓库里的 `JeecgBoot/deploy/frp/frpc.toml` 模板**默认不加**这两条，避免未看文档就多开管理口。主机上改完后重启 frpc（`start-local.ps1` 或任务管理器里重启进程）。

主机关机后这些隧道同样消失。

---



## 8. 故障怎么处理


| 现象                              | 先做什么                                                                                   |
| ------------------------------- | -------------------------------------------------------------------------------------- |
| 公网 502，SSH 主机还通                 | 主机上查 nginx/frpc/Java；`start-all.ps1 -Frontend -Backend`；看 `C:\homeai\logs\backend.log` |
| SSH / Tailscale 都不通，插座 App 显示有电 | 多半死机或睡眠：插座断电 10 秒再通电；起来后查电源计划                                                          |
| 通电不 POST                        | BIOS 来电自启未开、插座没接主机箱、电源键方案为「不动作」                                                        |
| 起来了但公网仍 502                     | 未自动登录、frpc 未注册启动、token 与云上不一致、本机开发机 frpc 抢隧道（迁机手册：本机必须 `stop-all`）                     |
| 只想应急、系统还在                       | 已装的向日葵/ToDesk 无人值守；用完检查不要升级成「任何人可连」                                                    |


回切开发机当临时 API：见迁机手册第 7 节（库已迁走则数据是旧的）。

---



## 9. 迁机当天验收（在第 7 节清单上追加）

现场（主机旁边）做完再离开：

- [ ] BIOS 来电自启已开；快速启动已关；接通电源不睡眠。
- [ ] 智能插座只控主机箱；断电再通电能进 Windows。
- [ ] 登录后 nginx/frpc（及 Java/Docker）会起来；公网入口恢复（可等 2～5 分钟）。
- [ ] 开发机局域网或 Tailscale：`check-remote.ps1` 通过。
- [ ] `ssh ... shutdown /r /t 0` 后能自己回来（不断插座）。
- [ ] （可选）同网 WoL：关机后发魔术包能开。
- [ ] （可选）FRP 管理隧道：仅经云主机 SSH 转发能连 22/3389；从家里 4G **直接**连云 IP 的 18022/13389 应失败。
- [ ] 安全组无 3389、无主机 22、无 18022/13389 公网入站。

---



## 10. 明确不做

- 不把服务主机 3389/22/3306 做路由器端口映射或阿里云 DNAT。
- 不把 WoL 端口对公网 `0.0.0.0/0` 放开。
- 不把向日葵等作为唯一运维通道（无带外开机、厂商账号风险）。
- 不在本文范围改 App 域名或把 Java 搬上 ECS（那是另一条部署路径）。

---



## 11. 文档与脚本索引


| 路径                                                                     | 用途                               |
| ---------------------------------------------------------------------- | -------------------------------- |
| [service-host-migration.md](./service-host-migration.md)               | 空系统初始化、SSH 免密、远程发布               |
| [host.env.example](./host.env.example)                                 | `HOST_IP` 填局域网或 Tailscale 地址     |
| [frp-home-deployment.md](./frp-home-deployment.md)                     | 业务隧道；`proxyBindAddr = 127.0.0.1` |
| [JeecgBoot/deploy/frp/frpc.toml](../../JeecgBoot/deploy/frp/frpc.toml) | 默认仅 `homeai-web`；管理代理见本文第 7 节    |
| [remote-host.ps1](./remote-host.ps1)                                   | 系统在线时远程启停业务（不是开机）                |


