---
name: HomeAI 小程序 → Android 端迁移方案
version: v1
status: 暂不执行（方案已固定，待决策恢复后再启动 Phase 0）
created: 2026-08-07
updated: 2026-08-07
owner: 项目组
---

# HomeAI 小程序 → Android 端迁移方案

> **文档状态：暂不执行**  
> 本文档为已固定的方案设计，用于后续部署与备案规划。**当前不启动开发、不修改代码**，恢复实施前需重新评审「待确认决策」一节。

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
  ├─ platform/ 平台适配层（新增）
  │    ├─ auth.ts
  │    ├─ filePicker.ts
  │    ├─ download.ts
  │    ├─ push.ts
  │    └─ env.ts
  └─ pages/auth/ 登录注册页（Android 主入口）

后端（JeecgBoot 扩展）
  ├─ /homeai/auth/* 新认证 API
  ├─ /homeai/user/* 保留小程序登录
  ├─ /homeai/* 业务 API 基本不变
  └─ 推送服务（UniPush/极光等）

合规层
  ├─ 域名 ICP 备案
  ├─ APP 备案
  └─ 隐私政策 / 用户协议
```

### 4.2 客户端分层原则

- 业务页只调用 `platform/*`，避免 `#ifdef` 散落
- `#ifdef MP-WEIXIN` 保留小程序微信登录路径
- `#ifdef APP-PLUS` 走手机号/密码登录与系统能力

---

## 5. 核心模块设计

### 5.1 认证体系（最高优先级）

**现状**

```
小程序 code → jscode2session → openid → JWT(claim=openid) → Redis
```

**目标**

```
登录方式（任选）→ 统一 userId → JWT(claim=userId) → Redis
```

| 登录方式 | Android | 小程序（可选保留） |
|----------|---------|-------------------|
| 手机号 + 验证码 | ✅ 主推荐 | 可选 |
| 手机号 + 密码 | ✅ | 可选 |
| 微信开放平台 OAuth | ✅ 可选 | — |
| 小程序 code 登录 | — | ✅ 保留 |

**建议新增后端 API**

```
POST /homeai/auth/sms/send          发送验证码
POST /homeai/auth/login/phone       手机号+验证码登录（自动注册）
POST /homeai/auth/login/password    手机号+密码登录
POST /homeai/auth/register          注册（设密码）
POST /homeai/auth/refresh           刷新 token
POST /homeai/user/login             保留：小程序 code 登录
POST /homeai/user/bind-wechat       可选：绑定微信
```

**数据模型扩展（示意）**

```sql
ALTER TABLE homeai_wx_user
  ADD COLUMN phone VARCHAR(20) UNIQUE COMMENT '手机号',
  ADD COLUMN password_hash VARCHAR(128) COMMENT '密码哈希',
  ADD COLUMN login_type VARCHAR(20) DEFAULT 'wechat' COMMENT 'wechat/phone',
  ADD COLUMN push_client_id VARCHAR(128) COMMENT '推送设备ID';
-- openid 改为可空；JWT sub 改为主键 userId
```

**JWT 改造要点**

- Claim：`sub = userId`（主键），不再用 openid
- 兼容期：拦截器同时支持旧 token（openid）与新 token（userId）
- Redis key：`homeai_token:{userId}`

### 5.2 推送与计划提醒

| 能力 | 小程序 | Android |
|------|--------|---------|
| 触发 | 用户订阅 + 微信订阅消息 | 通知权限 + 后端推送 |
| 前端 | `requestSubscribeMessage` | `uni.getPushClientId` + 系统通知 |
| 后端 | `HomeaiWxSubscribeServiceImpl` | 新增 `IHomeaiPushService` |

计划提醒：`PlanRemindScheduler` 改为双通道（微信订阅 + App 推送），保留小程序时可并行。

### 5.3 文件与存储

| 场景 | 小程序 | Android 适配 |
|------|--------|-------------|
| 选图片/视频 | `chooseMedia` | 同 API + 权限申请 |
| 选 PDF/文档 | `chooseMessageFile` | `uni.chooseFile` / `plus.io` |
| 下载/保存 | 相册 scope | Android 媒体权限 + `plus.gallery` |
| OSS 预签名 | 已实现 | **直接复用** |

新增 `platform/filePicker.ts` 统一封装，`useStorageUpload.ts` 只调 platform 层。

### 5.4 API 与环境配置

| 项目 | 小程序 | Android |
|------|--------|---------|
| API 地址 | `VITE_SERVER_BASEURL__WEIXIN_*` | `VITE_SERVER_BASEURL` 固定生产域名 |
| 传输 | HTTPS（微信要求） | 备案域名 + SSL |
| 401 | 跳转个人中心 Tab | 跳转 `/pages/auth/login` |

生产示例：`https://api.yourdomain.com/jeecg-boot/homeai/...`

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

---

## 7. 实施阶段规划（恢复执行时参考）

> ⚠️ 以下阶段**当前均不启动**，仅作后续实施路线图。

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

1. **登录主方式**：手机号验证码（推荐）还是账号密码？
2. **是否保留微信小程序**：双端并行，还是 Android 替代后小程序仅维护？
3. **发布渠道**：仅企业内部分发 APK，还是上应用商店？
4. **备案主体**：个人还是公司？
5. **推送方案**：是否采用 DCloud UniPush？

---

## 10. 结论

**推荐路径**：在现有 `JeecgUniapp` 基础上，通过 **uni-app app-plus 编译 Android**，重点改造认证体系、平台适配层、推送；业务 API 与页面大面积复用。备案按 **域名 ICP + APP 备案 + 隐私合规** 独立推进。

**当前动作：无。** 本文档已固定，标记为 **暂不执行**；决策确认后再从 Phase 0 启动，并更新本文档 `status` 字段。

---

## 变更记录

| 日期 | 版本 | 说明 |
|------|------|------|
| 2026-08-07 | v1 | 初版方案固定，状态：暂不执行 |
