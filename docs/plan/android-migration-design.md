---
name: HomeAI 小程序 → Android 端迁移方案
version: v3
status: 执行中
created: 2026-08-07
updated: 2026-08-18
owner: 项目组
---

# HomeAI 小程序 → Android 端迁移方案

> **文档状态：执行中**  
> v1 方案章节保留为设计基线；v2 起进入实施，认证体系（手机号 + 密码）、平台适配层、APP 登录页、内测路径等已落地；v3（2026-08-18）推送方案改为本地通知兜底、platform 层五文件结构落地、签名 APK 与隐私合规落地。备案完成前以侧载 APK 内测推进，后续迭代以本文档为基线更新。

相关路径索引：

| 领域 | 路径 |
|------|------|
| 小程序客户端 | `JeecgUniapp/` |
| HomeAI 业务层 | `JeecgUniapp/src/pages-homeai/`、`pages-homeai-ai/`、`pages-homeai-more/` |
| 微信登录 | `JeecgUniapp/src/pages-homeai/utils/homeaiAuth.ts` |
| App 构建配置 | `JeecgUniapp/manifest.config.ts`、`package.json`（`dev:app-android`） |
| 后端 homeai 模块 | `JeecgBoot/.../jeecg-boot-module-homeai/` |
| 小程序 UI 设计 | `docs/design/ui-miniapp-v1.md` |

---

## 1. 背景与目标

### 1.1 为什么要改

| 维度 | 小程序现状 | Android 独立 App 优势 |
|------|-----------|----------------------|
| **备案** | 依赖微信平台审核 + 小程序类目 | API/域名走 **ICP 备案 + APP 备案**，合规路径更清晰 |
| **部署** | 发布受微信审核节奏影响 | 自有 APK/应用商店渠道，版本迭代自主 |
| **能力** | 文件选择、推送等受微信 API 限制 | 系统通知、文件管理、权限更灵活 |
| **用户** | 必须安装微信 | 独立安装，不绑定微信生态 |

### 1.2 设计目标

1. **最大化复用**现有 `JeecgUniapp`（uni-app 3 + Vue3）业务代码与 `homeai` 后端 API
2. **不推倒重来**：优先 `app-plus` 编译 Android，而非新建 Kotlin 工程
3. **认证体系升级**：从「微信 openid 唯一身份」扩展为「通用 userId + 多种登录方式」
4. **合规可上线**：域名 ICP、APP 备案、隐私政策、权限说明一次性规划
5. **小程序可保留（可选）**：Android 与小程序共用后端，双端并行一段时间

---

## 2. 现状分析（项目基线）

### 2.1 当前架构

```
小程序客户端 (mp-weixin)
  ├─ wechatLogin → uni.login(weixin) → code
  ├─ POST /homeai/user/login?code=
  └─ 后续请求 Header: X-Access-Token

后端 (JeecgBoot homeai)
  ├─ jscode2session → openid
  ├─ HomeaiJwtUtil.sign(openid) + Redis
  └─ /homeai/* 业务 API + 阿里云 OSS 私有桶
```

### 2.2 已具备的多端基础

- `JeecgUniapp` 已配置 `app-plus`（Android minSdk 26、相机/网络权限）
- 构建脚本：`pnpm dev:app-android` / `pnpm build:app-android`
- 业务 API 统一 REST + JWT，与端无关部分可直接复用

### 2.3 必须改造的部分（Android 上线阻塞项）

| 模块 | 现状 | Android 问题 |
|------|------|-------------|
| 登录 | `uni.login({ provider: 'weixin' })` + `jscode2session` | App 无小程序 code；需手机号/密码或微信开放平台 OAuth |
| 用户身份 | JWT subject = **openid**，表 `homeai_wx_user` | 需抽象为通用 `userId`，openid 降为绑定字段 |
| 计划提醒 | `requestSubscribeMessage` + 微信订阅消息 | 需 Android 系统通知 + 厂商推送 |
| 文件选择 | `chooseMessageFile` | 需系统文件选择器 |
| 环境地址 | `getAccountInfoSync()` 按小程序版本切 API | App 用 `.env` / 构建 flavor 固定 |
| 相册权限 | `scope.writePhotosAlbum` | Android 13+ 运行时权限模型 |

---

## 3. 技术路线对比与推荐

### 3.1 方案对比

| 方案 | 工作量 | 复用率 | 备案/部署 | 推荐度 |
|------|--------|--------|-----------|--------|
| **A. uni-app 编译 Android（app-plus）** | 中 | **80%+** | 标准 APP 备案流程 | ⭐⭐⭐⭐⭐ |
| B. 原生 Android（Kotlin）重写 | 高 | ~30% | 同上 | ⭐⭐ |
| C. H5 套壳 WebView | 低 | 高 | 体验差、推送弱 | ⭐ |
| D. Flutter 重写 | 很高 | ~20% | 同上 | ⭐ |

### 3.2 推荐方案

**A — uni-app app-plus + 后端认证扩展**

- 项目已是 JeecgUniapp3，HomeAI 页面/API/Store 可直接编译到 Android
- 无需维护两套 UI
- 备案与部署按独立 App 走，不依赖微信小程序平台
- 后续可扩展 iOS

---

## 4. 目标架构

### 4.1 总体结构

```
Android App (uni-app app-plus)
  ├─ HomeAI 业务页面（pages-homeai*，尽量不改）
  ├─ platform/ 平台适配层（v3 已落地五文件）
  │    ├─ auth.ts          ✅ 手机号+密码登录（v2）
  │    ├─ filePicker.ts    ✅ 文件选择（v2）
  │    ├─ download.ts      ✅ 下载/相册权限适配（v3 新增）
  │    ├─ env.ts           ✅ 环境地址（v3 新增）
  │    └─ push.ts          ✅ 本地通知兜底（v3 新增）
  └─ pages/auth/ 登录注册页（Android 主入口）

后端（JeecgBoot 扩展）
  ├─ /homeai/auth/* 新认证 API
  ├─ /homeai/user/* 保留小程序登录
  ├─ /homeai/* 业务 API 基本不变
  └─ 推送：本地通知兜底（plus.push.createMessage，v3 弃用 EMAS）

合规层
  ├─ 域名 ICP 备案
  ├─ APP 备案
  ├─ 隐私政策 / 用户协议（v3 已落地）
  └─ 签名 APK（v3 已落地）
```

### 4.2 客户端分层原则

- 业务页只调用 `platform/*`，避免 `#ifdef` 散落
- `#ifdef MP-WEIXIN` 保留小程序微信登录路径
- `#ifdef APP-PLUS` 走手机号/密码登录与系统能力

---

## 5. 核心模块设计

### 5.1 认证体系（最高优先级）

> **v2 更新（2026-08-17）**：手机号 + 密码登录已落地（register / login / password），数据模型按现状修正（见下）。v1 中「手机号 + 验证码」等未实现项保留在「v1 规划参考」中，供后续迭代参考。

**现状（v2）**

```
小程序 code → jscode2session → openid → JWT(claim=openid) → Redis   （小程序路径，保留）
手机号 + 密码 → PasswordUtil 校验 → JWT(claim=userId, openid) → Redis  （Android 路径，已落地）
```

**目标（v2）**

```
登录方式（任选）→ 统一 userId → JWT(claim=userId) → Redis
```

| 登录方式 | Android | 小程序（保留） |
|----------|---------|----------------|
| 手机号 + 密码 | ✅ 已落地 | 可选 |
| 手机号 + 验证码 | 规划中（未启动） | 可选 |
| 微信开放平台 OAuth | 可选（未启动） | — |
| 小程序 code 登录 | — | ✅ 保留 |

**已落地后端 API（v2）**

```
POST /homeai/auth/register      注册（设密码，手机号用户）
POST /homeai/auth/login         手机号 + 密码登录
POST /homeai/auth/password      密码相关操作
POST /homeai/user/login         保留：小程序 code 登录
```

**数据模型（v2 已落地）**

`phone` 字段 WxUser 实体已有，无需新增；本轮新增以下三列，DDL 脚本见 `sql/alter_homeai_wx_user_android_login.sql`：

```sql
ALTER TABLE homeai_wx_user
  ADD COLUMN password VARCHAR(128) COMMENT '密码哈希',
  ADD COLUMN salt VARCHAR(64) COMMENT '每用户 8 位随机 salt',
  ADD COLUMN login_type VARCHAR(20) DEFAULT 'wechat' COMMENT 'wechat/phone，手机号用户为 phone';
```

- 密码存储：`PasswordUtil.encrypt(password, PasswordUtil.SALT, salt)`，`salt` 为每用户 8 位随机值
- `openid` 可空：手机号用户无 openid，JWT 以 userId 为 claim
- `login_type` 默认 `wechat`，手机号注册/登录用户为 `phone`

**JWT 改造要点（v2）**

- Claim：以 `userId`（主键）为主 claim；`HomeaiJwtUtil.sign(userId, openid, secret, clientType)` 重载 + `getUserId`，`HomeaiSecurityUtil.getWxUser` 双 claim 解析（兼容旧 openid token 与新 userId token）
- 拦截器：新认证端点已登记 `HomeaiAuthInterceptor.PUBLIC_PATHS`，小程序与 Android 同走 homeai JWT（`X-Access-Token`）
- Redis key：`homeai_token:{userId}`

**v1 规划参考（保留）**

v1 曾规划的以下接口暂未实现，列入后续迭代：

```
POST /homeai/auth/sms/send          发送验证码（规划）
POST /homeai/auth/login/phone       手机号+验证码登录（规划）
POST /homeai/auth/refresh           刷新 token（规划）
POST /homeai/user/bind-wechat       可选：绑定微信（规划）
```

v1 规划 SQL 中的 `password_hash`、`push_client_id` 字段命名以 v2 落地为准（`password` / `salt` / `login_type`，推送字段后续再补）。

### 5.2 推送与计划提醒（v3：本地通知兜底）

> **v3 更新（2026-08-18）**：EMAS 移动推送方案弃用，改为 `plus.push.createMessage` 延迟本地通知兜底。原因与落地见下。

**EMAS 弃用原因**

- EMAS 推送插件（id=7628/7629）已停止维护，无 Vue3 兼容性声明
- 官方替代方案仅支持 uni-app x，与当前 uni-app 3 + Vue3 工程不兼容
- 结论：不接入 UniPush/EMAS 厂商推送（后续如需离线推送，可评估 uni-app x 迁移或原生厂商通道，见 9. 待确认决策第 5 项）

**落地方案（v3）**

| 能力 | 小程序 | Android |
|------|--------|---------|
| 触发 | 用户订阅 + 微信订阅消息 | 本地延迟通知（`plus.push.createMessage`） |
| 前端 | `requestSubscribeMessage` | `platform/push.ts` 计算提醒时刻并创建本地通知 |
| 后端 | `HomeaiWxSubscribeServiceImpl` | `PlanInstance` 冗余展示 `startTime` / `remindMinutes`（`fillMasterInfo` 拷贝） |

- 小程序 `requestSubscribeMessage` 通道保持不变
- Android 端：`platform/push.ts` 依据计划实例的 `startTime` 与 `remindMinutes` 计算提醒时刻，调用 `plus.push.createMessage` 创建延迟本地通知
- 后端：`PlanInstance` 新增 `startTime` / `remindMinutes` 两个 `@TableField(exist=false)` 冗余展示字段，`fillMasterInfo` 从主计划拷贝，供客户端计算提醒时间（无 DDL）

> ⚠️ **局限（文档标注）**：本地通知仅在 App 存活/后台时生效，进程被杀后无法送达；不依赖厂商推送通道，无离线推送能力。

### 5.3 文件与存储

| 场景 | 小程序 | Android 适配 |
|------|--------|-------------|
| 选图片/视频 | `chooseMedia` | 同 API + 权限申请 |
| 选 PDF/文档 | `chooseMessageFile` | `uni.chooseFile` / `plus.io` |
| 下载/保存 | 相册 scope | Android 媒体权限 + `plus.gallery` |
| OSS 预签名 | 已实现 | **直接复用** |

`platform/filePicker.ts` 统一封装文件选择，`useHomeaiFilePick.ts` 只调 platform 层；`platform/download.ts` 统一封装下载/相册保存（APP-PLUS 分支 + 权限申请），`fileDownload.ts` 只调 platform 层。

### 5.4 API 与环境配置

| 项目 | 小程序 | Android |
|------|--------|---------|
| API 地址 | `VITE_SERVER_BASEURL__WEIXIN_*` | `VITE_SERVER_BASEURL` 固定生产域名 |
| 传输 | HTTPS（微信要求） | 备案域名 + SSL |
| 401 | 跳转个人中心 Tab | 跳转 `/pages/auth/login` |

生产示例：`https://api.yourdomain.com/jeecg-boot/homeai/...`

### 5.5 平台适配层（platform/*，v3 落地）

> **v3 更新（2026-08-18）**：platform 层补齐为五文件结构，业务层统一改调 platform/*，避免 `#ifdef` 散落。

| 文件 | 职责 | 落地 |
|------|------|------|
| `platform/auth.ts` | 手机号 + 密码登录（register/login/password） | ✅ v2 |
| `platform/filePicker.ts` | APP 端文件选择（替代小程序 `chooseMessageFile`） | ✅ v2 |
| `platform/download.ts` | 下载/相册保存（APP-PLUS 分支 + 权限申请） | ✅ v3 新增 |
| `platform/env.ts` | 环境地址（APP 端固定生产域名） | ✅ v3 新增 |
| `platform/push.ts` | 本地通知兜底（`plus.push.createMessage` 延迟通知） | ✅ v3 新增 |

**业务层调用关系（v3）**

- `fileDownload.ts` → `platform/download.ts`
- `request.ts` → `platform/env.ts`
- `useHomeaiFilePick.ts` → `platform/filePicker.ts`
- 计划提醒计算 → `platform/push.ts`（配合后端 `PlanInstance.startTime` / `remindMinutes`）

---

## 6. 备案与合规清单（中国大陆）

| 备案类型 | 是否必须 | 说明 |
|----------|----------|------|
| **域名 ICP 备案** | ✅ | API 服务器所用域名 |
| **APP 备案** | ✅ | 2023 年起上架/分发需完成 APP 备案 |
| 公安备案 | 视地区 | 部分省份要求 |
| 小程序备案 | 若保留小程序 | 与 App 备案并行 |
| OSS 自定义域名 | 若 App 直访 | 域名需 ICP；当前预签名经 API 可复用 |

**App 上架材料**：软著（部分商店）、隐私政策 URL、用户协议、权限说明、图标截图、主体资质。

**隐私合规要点**

1. 精简 `manifest.config.ts` 中非必要权限（如 `READ_PHONE_STATE`）
2. `targetSdkVersion` 建议升至 33+
3. 首次启动隐私政策同意弹窗
4. 敏感权限使用前说明用途

**部署架构建议**

```
Android App → HTTPS → Nginx → JeecgBoot → MySQL / Redis / 阿里云 OSS
管理端 Vue3 ────────────────┘
```

### 6.1 备案前内测路径（v2 新增，2026-08-17）

> 备案（域名 ICP / APP 备案）受阻期间，先通过**侧载 APK 内测**，备案完成后切换正式域名，不阻塞开发与真机验证。

- **现状**：备案尚未完成，App 暂不进入应用商店；先打包 APK 侧载到测试设备内测
- **环境地址**：`.env.production` 配置为局域网 / 云服务器 IP，如 `http://192.168.1.100:8080/jeecg-boot`
- **生产守卫**：`vite.config.ts` 生产守卫已放行非 mp-weixin 构建，App 内测构建不会被 localhost / HTTPS 校验中断
- **切换路径**：备案完成后，将 `.env.production` 中的 IP 替换为正式域名，重新构建签名 APK 即可上线

### 6.2 第二轮落地（v3，2026-08-18）

> 本轮完成签名 APK 与隐私合规两项发布前置工作，备案完成后即可按 6.1 切换正式域名重新构建签名 APK 上架。

**签名 APK**

- **现行出包**：`pnpm pack:apk:local`（uni-app H5 + Capacitor），指南见 `docs/guide/android-local-apk.md`、发布见 `docs/guide/app-release.md`
- 已弃用：HBuilderX 云打包；`pnpm pack:apk`（DCloud 离线 SDK）
- keystore 与 `android-pack.local.json` 不入库

**隐私合规**

- 隐私政策 / 用户协议内容页（`pages/privacy/*`）
- 三处入口：登录页、个人中心、设置页
- `App.vue` 首启隐私弹窗（同意后进入，拒绝则退出）
- `manifest.config.ts` 开启 `__usePrivacyCheck__`
- 相册 / 存储权限说明与申请（配合 `platform/download.ts`）

---

## 7. 实施阶段规划（恢复执行时参考）

> ⚠️ v1 阶段划分保留为实施路线图；v2 起已按 Phase 1 推进（认证体系、平台适配层、APP 登录页、内测路径已落地）；v3 起推进 Phase 2/3 部分项（下载/相册权限适配、platform 层补齐、隐私弹窗、签名 APK、推送改本地通知兜底），未完成项以对应 checkbox 状态为准。

### Phase 0：方案确认（约 1 周）

- [ ] 登录方式：手机号验证码 / 密码 / 是否保留微信 App 登录
- [ ] 推送服务商：UniPush / 极光 / 个推
- [ ] 发布渠道：自有 APK / 应用商店
- [ ] 备案主体与域名
- [ ] 是否保留小程序双端并行

### Phase 1：Android 可运行（约 2–3 周）

- [ ] `pnpm dev:app-android` 跑通 Tab + 分包
- [ ] 新增 `platform/env.ts`
- [ ] 登录页 + 手机号登录 API
- [ ] JWT 改为 userId（含兼容层）

### Phase 2：能力补齐（约 2–3 周）

- [ ] `platform/filePicker.ts`
- [ ] 下载/相册权限适配
- [ ] 推送 + 计划提醒双通道
- [ ] manifest 权限精简 + 隐私弹窗

### Phase 3：合规与发布（约 2–4 周，可与 Phase 2 并行）

- [ ] 域名 ICP、APP 备案
- [ ] 隐私政策/用户协议
- [ ] 生产 HTTPS 部署
- [ ] 签名 APK + 上架或侧载

### Phase 4：优化（持续）

- [ ] 热更新（`appUpdate.ts`）
- [ ] 性能与包体积
- [ ] 可选 iOS

---

## 8. 风险与对策

| 风险 | 影响 | 对策 |
|------|------|------|
| openid → userId 迁移 | 老用户数据 | 迁移脚本；支持绑定；兼容期双 token |
| uni-app App 兼容性 | 组件差异 | 真机回归清单；条件编译 |
| 备案周期 | 2–4 周 | 提前 ICP；测试域先行 |
| 推送到达率 | 厂商差异 | 厂商通道 + 本地 Alarm 兜底 |
| 商店审核 | 隐私/权限 | 精简权限、完善说明 |

---

## 9. 待确认决策（恢复执行前必填）

> **v2 更新（2026-08-17）**：第 2 项已确认「双端并行」：Android 与小程序共用后端，并行迭代一段时间，小程序保留维护。其余项沿用 v1 决策或待后续确认。  
> **v3 更新（2026-08-18）**：第 5 项已确认「不采用 DCloud UniPush/EMAS」——插件停止维护且无 Vue3 兼容性声明，改本地通知兜底（见 5.2）；后续如需离线推送再评估 uni-app x 或原生厂商通道。

1. **登录主方式**：手机号验证码（推荐）还是账号密码？
2. **是否保留微信小程序**：双端并行，还是 Android 替代后小程序仅维护？
3. **发布渠道**：仅企业内部分发 APK，还是上应用商店？
4. **备案主体**：个人还是公司？
5. **推送方案**：是否采用 DCloud UniPush？

---

## 10. 结论

**推荐路径**：在现有 `JeecgUniapp` 基础上，通过 **uni-app app-plus 编译 Android**，重点改造认证体系、平台适配层、推送；业务 API 与页面大面积复用。备案按 **域名 ICP + APP 备案 + 隐私合规** 独立推进，备案完成前走侧载内测（见 6.1）。

**当前状态：执行中。** v2 起认证体系（手机号 + 密码）、平台适配层（`platform/auth.ts`、`platform/filePicker.ts`）、APP 登录页、内测路径已落地；v3 起推送改本地通知兜底（EMAS 弃用）、platform 层补齐五文件（`download.ts` / `env.ts` / `push.ts`）、签名 APK 与隐私合规落地；后续按 Phase 2 剩余能力补齐与 Phase 3 备案上架推进。

---

## 变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-08-07 | v1 | 初版方案固定，状态：暂不执行 |
| 2026-08-17 | v2 | 状态转为执行中。关键落地：手机号+密码登录（register/login/password）、JWT userId claim 双解析、platform/auth.ts + platform/filePicker.ts、APP 登录页 pages/auth/login.vue、拦截器登记 PUBLIC_PATHS、manifest 权限精简、双端并行策略确认、新增「备案前内测路径」 |
| 2026-08-18 | v3 | 推送方案改本地通知兜底（EMAS 插件 id=7628/7629 停止维护、无 Vue3 兼容性声明、官方替代仅支持 uni-app x → 弃用；`plus.push.createMessage` 延迟本地通知，标注进程被杀无法送达局限；小程序 requestSubscribeMessage 通道不变；后端 `PlanInstance` 补 `startTime`/`remindMinutes` 冗余展示字段）；platform 层补齐五文件（`download.ts`/`env.ts` 新增，业务层 fileDownload.ts/request.ts/useHomeaiFilePick.ts 改调 platform/*）；第二轮落地：签名 APK（keystore + 签名文档 + .gitignore）、隐私合规（隐私政策/用户协议内容页 + 三处入口 + App.vue 首启弹窗 + `__usePrivacyCheck__` 开启 + 相册/存储权限） |
