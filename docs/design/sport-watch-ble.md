# 运动模块（蓝牙手表）方案

> 状态：方案（未实现）  
> 范围（本期）：Android App 连接手表 + **息屏仍收 BLE** + 单次运动结束后汇总落库  
> 设备 SDK：`sdk/uniapp-sdk.rar`（Starmax 协议 JS）+ `sdk/uniapp-sdk-demo.rar`（uni-app Demo）  
> 日期：2026-09-08

## 1. 目标与非目标

### 本期做

1. 在 **现行发版壳**（uni-app H5 + Capacitor，包名 `com.homeai.app`）上扫描、连接、协议配对指定品牌手表。
2. **手机息屏、应用退到后台时仍保持 GATT 并收下手表 notify**（运动结束包不因灭屏丢失）。
3. 识别「一次运动结束」，把该次 **统计摘要** 上报后台并持久化。
4. App 内提供最小页面：设备绑定/连接状态、最近运动记录列表。

### 本期不做

- 日步数 / 睡眠 / 心率曲线 / 血压血氧等健康史同步（SDK 已有 `get*History`，二期再接）。
- 手机端实时陪跑、轨迹地图、表盘/固件升级。
- 微信小程序 BLE（H5 通道无系统蓝牙；小程序不上架）。
- 浏览器 H5 直连蓝牙。
- 管理端完整运营后台（本期最多只读列表，可后置）。

## 2. 关键结论：SDK 能用，但 Demo 的蓝牙层不能原样搬

Starmax SDK **不是** 原生 AAR，而是纯 JS 协议栈：

| 层 | 职责 | 能否进 Capacitor |
|---|---|---|
| `starmaxSDK`（`pair` / `getSportHistory` / `notify`） | 把业务编成 `ArrayBuffer`，把 notify 解成对象 | **能**，与运行时无关 |
| Demo 的 `uni.openBluetoothAdapter` / `writeBLECharacteristicValue` | uni-app 原生 / 小程序 BLE | **不能** 用在当前 H5 WebView |
| Demo `uni_modules/bluetooth-pair`（UTS） | Android/iOS 系统级配对 | Capacitor 下不可用，本期尽量不依赖 |

GATT 约定（Demo）：

- 服务 UUID 前缀：`6e400001`（Nordic UART 一类）
- 写特征：`properties.write`
- 通知特征：`properties.notify`
- 扫描过滤：广播里含 `0x00 0x01`；可选名称含 `GTS`
- 连接后：开 notify → `sdk.pair()` 或 `pairGts10` → 再发业务指令
- 写分包：MTU 默认按 247，chunk = MTU-3，间隔约 20ms

**因此架构必须拆成「协议」与「传输」两层。** 禁止业务页直接调 `uni.*` 蓝牙 API。

## 3. 总体架构

息屏时 Activity `onPause`，Capacitor 默认会暂停 WebView 定时器与 JS。只把 `@capacitor-community/bluetooth-le` 接到页面里，**灭屏后 JS 收不到包、部分机型还会拆掉 GATT**。因此传输必须落在 **进程级前台服务**，不能绑在页面生命周期上。

```
手表 BLE
    │  GATT（Service 线程，与 Activity 无关）
    ▼
HomeaiWatchService（前台服务，connectedDevice）
    ├─ 持有 BluetoothGatt、写特征、notify
    ├─ 原始帧写入本地 journal（息屏也不丢）
    └─ 断线指数退避重连
    │
    ▼
HomeaiWatchPlugin（对标 HomeaiUpdate）
    │  事件：notify / 连接状态（retainUntilConsumed）
    ▼
pages-homeai/platform/ble.ts     ← 业务唯一入口
    ├─ Starmax 协议（vendor，JS 解码）
    └─ 上报 POST /homeai/sport/session
            ▼
     homeai 后端 + MySQL
```

离线上报复用现有 `pages-homeai/offline/syncQueue.ts`（`module=sport`）。解码后的 DTO 同时交给插件 `enqueueUpload`，由 **Service 用 OkHttp 直传**，避免灭屏后 JS `uni.request` 被挂起。

## 4. 传输层选型（息屏保活）

**不采用**「仅社区 BLE 插件 + 页面订阅」作为连接态方案。扫描可以暂时用社区插件或同一套原生扫描；**一旦用户绑定成功，GATT 立刻交给 `HomeaiWatchService`。**

对齐现有 `HomeaiUpdatePlugin`：新建 `com.homeai.app.watch`（Java），`MainActivity.registerPlugin(HomeaiWatchPlugin.class)`。

### 4.1 前台服务（硬性）

| 项 | 约定 |
|---|---|
| 类型 | Android 14+ `foregroundServiceType="connectedDevice"` |
| 权限 | `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_CONNECTED_DEVICE`、BLE 扫描/连接 |
| 通知 | 常驻：「家庭 AI 正在连接手表」，点按回到运动页；用户点「断开」才停服务 |
| 生命周期 | **绑定成功后一直跑**，直到手动断开/解绑/登出；不要随页面销毁而停 |
| 重连 | `onConnectionStateChange` 非主动断开则退避重连（2s→4s→…上限 30s），`autoConnect` 作辅助 |

通知渠道与 `@capacitor/local-notifications` 分开，避免被用户关掉本地提醒时误杀手表通道。

### 4.2 息屏时 JS 怎么办

两层同时做，缺一不可：

1. **原生必收**：Service 在 `BluetoothGattCallback.onCharacteristicChanged` 里立刻把帧写入 journal（目录如 `files/watch-journal/`，按序编号）。这与 WebView 是否暂停无关，满足「息屏仍获取蓝牙数据」。
2. **尽量不断解码**：绑定期间 `MainActivity` **不要**对 WebView 调 `onPause()` / `pauseTimers()`（仅在手表服务运行时跳过）。这样 Starmax 的 `notify()` 仍可在灭屏时跑。部分国产 ROM 仍会冻 JS，故 journal 是底线。
3. **上报不依赖页面**：JS 解出一次运动摘要后调用插件入队；Service 在有网时 POST。JS 被冻住时，等解冻或进程内已入队的请求继续发。journal 未解码的帧：WebView 恢复或服务内短唤醒时 drain。

不把整份 Starmax 移植到 Kotlin（体积与跟版成本高）。不引入第二套隐藏 WebView / QuickJS。

### 4.3 省电与系统限制（产品必须交代）

- 首次绑定引导：**忽略电池优化**（`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`），小米/华为等还需「自启动 / 后台运行」。做设置页跳转，不要静默失败。
- 前台服务会耗电，文案写清：要息屏同步就必须常驻通知；可手动断开。
- **不承诺**用户划掉任务或系统强杀后仍收包；被杀后靠下次启动重连 + `getSportHistory` 补洞。
- 不做无通知的后台保活（Android 会杀，也违反政策）。

系统 Bond（Demo UTS `pair`）本期不接。先 GATT + `sdk.pair()`。H5 无真机时 `platform/ble.ts` mock。

## 5. App 模块划分

| 路径 | 职责 |
|---|---|
| `JeecgUniapp/android/.../HomeaiWatchPlugin.java` | Capacitor 桥：权限、扫描、连接、write、事件、入队上报 |
| `JeecgUniapp/android/.../HomeaiWatchService.java` | 前台服务：GATT、journal、重连、原生 HTTP 上报 |
| `JeecgUniapp/src/pages-homeai/platform/ble.ts` | JS 唯一蓝牙入口；`isCapacitorNative()` 外给出明确错误 |
| `JeecgUniapp/src/pages-homeai/sport/sdk/` | 从 `sdk/dist` 纳入的 Starmax 文件 |
| `JeecgUniapp/src/pages-homeai/sport/watch.ts` | 连接状态机：过滤 → 连上 → 6e400001 → notify → pair → 对时；启动/停止保活 |
| `JeecgUniapp/src/pages-homeai/sport/session.ts` | 解码 + 组装 DTO；drain journal |
| `JeecgUniapp/src/pages-homeai/api/sport.ts` | 列表查询等（上报优先走插件） |
| `pages-homeai-more/sport/index.vue` | 连接 + 最近记录 + 保活状态 |

入口：全部功能 + 首页快捷（六宫格目前已满，本期可只进「全部功能」，避免挤掉现有模块）。

绑定信息本地存：`deviceId`、名称、上次连接时间（`uni.setStorageSync`），账号维度 key 带 `userId`。

## 6. 「运动结束」如何判定

手表侧有两类通道（Demo 均已覆盖）：

1. **主动推送** `NotifyType.SportSyncFromDevice`（110）  
   字段：`sportType / sportStatus / steps / calorie / paceTime / cadence / heartRate / sportSeconds`  
   Demo 向手表同步时使用 `sportStatus: 4`，本期把 **status=4（或厂商文档确认的结束码）** 视为结束。实机联调时用 log 校准枚举，不要写死未验证的中间态。

2. **拉取历史** `getSportHistory(resend)` → `getSportHistoryResponse`  
   含开始时刻、时长、类型、步数、距离、配速、卡路里、平均心率等。  
   **App 每次连上或进入运动页时拉一次**，补齐「运动时 App 被杀 / 未连接」的记录。

本期策略：

- **绑定后 Service 常驻**：息屏也收 `SportSyncFromDevice`，解码后入队上报。
- 连接成功（含重连成功）再拉 `getSportHistory`，`clientId` 去重（强杀、漏解码时的补洞）。
- journal 超过体积上限（如 2MB）丢最旧帧并打日志，避免撑满磁盘。

心率序列、GPS `locations` 本期 **不上报**。

## 6.1 息屏验收（实现后必须过）

真机、已忽略电池优化：

1. 连上手表，按灭屏，等待 ≥10 分钟，通知栏服务仍在，回前台连接未断（或已自动重连成功）。
2. 灭屏期间在手表上结束一节运动，亮屏后（或灭屏期间已上传）后台能看到该条记录，且不重复。
3. 关掉电池优化豁免或划掉任务：允许丢实时包，但再次打开 App 能靠 history 补上。

## 7. 上报字段与幂等

`clientId`（客户端生成，服务端唯一）：

```
sha1(userId + '|' + deviceId + '|' + startAtIso + '|' + sportType)
```

`startAt` 用手表给出的年月日时分秒，按时区 **本地墙钟** 解释（与 `pages-homeai/utils/date.ts` 同一原则，禁止 `toISOString` 截日期）。

App → `POST /homeai/sport/session`

```json
{
  "clientId": "...",
  "deviceId": "AA:BB:...",
  "deviceName": "GTS10-xxxx",
  "sportType": 3,
  "startAt": "2026-09-08 07:12:03",
  "durationSec": 1840,
  "steps": 3200,
  "distanceM": 2100,
  "calorie": 180,
  "paceTime": 0,
  "stepFrequency": 0,
  "heartRateAvg": 128,
  "source": "watch"
}
```

同一 `clientId` 重复提交：返回已有记录，不插第二条。

## 8. 后端

包：`org.jeecg.modules.homeai.sport`  
鉴权：App JWT（`getWxUserId`）。**不要**把上报接口登记进 `ADMIN_PREFIXES`。若后续做管理端列表，再单独加 `/homeai/sport/admin/**`。

### 表 `homeai_sport_device`（绑定）

| 列 | 说明 |
|---|---|
| id | 主键 |
| user_id | HomeAI 用户 |
| device_id | BLE deviceId / MAC |
| device_name | 展示名 |
| last_seen_at | 最近一次成功连接或同步 |
| 标准审计列 | create_time / del_flag 等 |

同一用户同一 `device_id` 唯一。换绑即更新。

### 表 `homeai_sport_session`（一次运动）

| 列 | 说明 |
|---|---|
| id | 主键 |
| user_id | 归属 |
| family_id | 可空；写入时带当前家庭，便于以后家庭看板 |
| device_id | 手表 |
| client_id | **UNIQUE** 幂等 |
| sport_type | 手表枚举，原样存 int；展示用码表映射 |
| start_at | datetime |
| duration_sec | |
| steps / distance_m / calorie / pace_time / step_frequency / heart_rate_avg | 摘要，可空 |
| source | `watch` |
| 标准审计列 | |

接口（本期）：

- `POST /homeai/sport/device/bind` 绑定
- `GET /homeai/sport/device` 当前绑定
- `POST /homeai/sport/session` 上报（幂等）
- `GET /homeai/sport/session/list` 本人分页，按 `start_at` 倒序

SQL：`alter_homeai_sport.sql` + 登记 `init_homeai_tables.sql` / 迁移清单。无菜单也可先不上管理端。

`sport_type` 映射：实现时对照实机与 Demo `setSportModes` 枚举，未知类型显示「运动」。

## 9. 页面流程（本期）

1. 打开「运动」→ 未绑定：蓝牙/附近设备权限 → 扫描 → 点选连接。
2. 连接成功：拉起前台服务 → 协议 pair、对时、拉 history；引导关闭电池优化。
3. 已绑定：进页或冷启动由 Service 按上次 `deviceId` 重连（不必先打开运动页）。
4. 通知栏可断开；登出必须停服务。
5. 列表：类型、开始时间、时长、距离、卡路里。

断开、失败要有可读错误（未开蓝牙、拒绝权限、未找到服务、未忽略电池优化导致息屏中断）。

## 10. 权限与合规

AndroidManifest（插件文档 + 下列服务相关）：

- `BLUETOOTH_SCAN`（`neverForLocation` 若只用于手表）、`BLUETOOTH_CONNECTED`/`BLUETOOTH_CONNECT`
- 旧版 `ACCESS_FINE_LOCATION`（按 targetSdk 判断）
- `FOREGROUND_SERVICE`、`FOREGROUND_SERVICE_CONNECTED_DEVICE`
- `POST_NOTIFICATIONS`（已有）
- 可选请求：忽略电池优化（运行时弹窗，拒绝则提示「息屏可能中断」）

隐私政策补充：连接手表、后台读取运动数据、常驻通知、上传服务器。侧载内测也要有文案。

## 11. 实施顺序（通过方案后再编码）

1. 落地 `HomeaiWatchPlugin` + `HomeaiWatchService`：扫描 → GATT 交服务 → 灭屏 journal 有帧；拷贝 Starmax；`platform/ble.ts`。
2. 绑定期间跳过 WebView pause；pair / `getPower` 通。
3. 实机确认结束码与单位；灭屏结束一节运动能入队。
4. 后端表 + 幂等接口；Service OkHttp 上报（token 来自登录后写入原生存储）。
5. 运动页 UI + 电池优化引导。
6. 跑第 6.1 节验收；路线图追加一轮。

## 12. 风险

| 风险 | 应对 |
|---|---|
| 灭屏 WebView 冻住 | GATT+journal 在 Service；上报走原生队列 |
| 国产 ROM 杀后台 | 忽略电池优化 + 自启动说明；history 补洞 |
| 划掉任务 | 不假装还活着；下次启动重连 + 拉历史 |
| 单位/枚举不一致 | 实机 log 再写转换 |
| 必须系统 Bond | Service 内 `createBond`，不改发版管道 |
| 多用户共用手表 | 绑定挂当前用户；登出停服务 |

## 13. 与现有约定对齐

- 业务不写 `#ifdef`，蓝牙只通过 `platform/ble.ts`；原生细节在 `HomeaiWatch*`，与 `HomeaiUpdate` 同一模式。
- 日期用本地日期工具。
- 后端 `Result.OK/error`；写操作事务；Java 改动包 `update-begin/end`。
- 新管理端接口才进 `ADMIN_PREFIXES`（本期上报不是管理端）。
