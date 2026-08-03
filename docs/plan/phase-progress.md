---
name: 家庭AI小工具 - 阶段完成记录
version: v1
---

# 家庭AI小工具 - 阶段完成记录

> 本文档记录各阶段的完成情况、产出文件和关键决策。每阶段完成后更新。

---

## 第一阶段：项目脚手架搭建

> **完成日期**：2026-07-29
> **预计工期**：3-5 天
> **实际耗时**：1 天（含后续补全任务）

### 已完成任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1 | 创建 Maven 模块 | ✅ | `jeecg-boot-module-homeai`，注册到 `jeecg-boot-module/pom.xml` |
| 2 | 添加依赖引用 | ✅ | `jeecg-system-start/pom.xml` 添加 `jeecg-boot-module-homeai` |
| 3 | MyBatis-Plus 配置 | ✅ | `HomeaiMybatisPlusConfig.java`（分页插件 + 乐观锁插件） |
| 4 | 基础实体创建 | ✅ | `WxUser`、`Family`、`FamilyMember`、`FamilyInviteCode` |
| 5 | Mapper 接口 | ✅ | 4 个 Mapper + 4 个 XML |
| 6 | Service 接口+实现 | ✅ | 4 组 IService + ServiceImpl |
| 7 | Controller | ✅ | `WxUserController`、`FamilyController`（占位方法） |
| 8 | DDL 建表脚本 | ✅ | `sql/init_homeai_tables.sql`（24 张表 + 初始化分类数据） |
| 9 | 菜单权限 SQL | ✅ | `sql/init_homeai_menus.sql`（一级+二级+按钮权限） |
| 10 | 小程序分包注册 | ✅ | `pages.json` 添加 3 个分包 |
| 11 | 代码修改日志 | ✅ | `doc/代码修改日志` |

### 产出文件清单

#### 后端 Java 文件（25 个）

```
jeecg-boot-module-homeai/
├── pom.xml
├── sql/
│   ├── init_homeai_tables.sql
│   └── init_homeai_menus.sql
├── doc/
│   └── 代码修改日志
└── src/main/java/org/jeecg/modules/homeai/
    ├── config/
    │   └── HomeaiMybatisPlusConfig.java
    ├── user/
    │   ├── entity/WxUser.java
    │   ├── mapper/WxUserMapper.java
    │   ├── mapper/xml/WxUserMapper.xml
    │   ├── service/IWxUserService.java
    │   ├── service/impl/WxUserServiceImpl.java
    │   └── controller/WxUserController.java
    └── family/
        ├── entity/Family.java
        ├── entity/FamilyMember.java
        ├── entity/FamilyInviteCode.java
        ├── mapper/FamilyMapper.java
        ├── mapper/FamilyMemberMapper.java
        ├── mapper/FamilyInviteCodeMapper.java
        ├── mapper/xml/FamilyMapper.xml
        ├── mapper/xml/FamilyMemberMapper.xml
        ├── mapper/xml/FamilyInviteCodeMapper.xml
        ├── service/IFamilyService.java
        ├── service/IFamilyMemberService.java
        ├── service/IFamilyInviteCodeService.java
        ├── service/impl/FamilyServiceImpl.java
        ├── service/impl/FamilyMemberServiceImpl.java
        ├── service/impl/FamilyInviteCodeServiceImpl.java
        └── controller/FamilyController.java
```

#### 管理端前端文件（5 个）

```
jeecgboot-vue3/
├── src/router/routes/modules/homeai/homeai.ts
├── src/views/homeai/
│   ├── user/index.vue
│   ├── user/UserDrawer.vue
│   └── family/index.vue
└── src/api/homeai/index.ts
```

#### 小程序端文件（9 个）

```
JeecgUniapp/
├── src/pages.json（添加 pages-homeai/pages-homeai-ai/pages-homeai-more 分包）
└── src/pages-homeai/
    ├── pages.config.ts
    ├── api/
    │   ├── request.ts
    │   └── index.ts
    ├── stores/
    │   ├── user.ts
    │   └── family.ts
    └── utils/
        └── auth.ts
```

### 关键决策

1. **数据库规范**：严格遵循 JeecgBoot 建表规范（主键 VARCHAR(32)、审计字段四件套、del_flag 逻辑删除、`idx_hw_` 索引命名前缀）
2. **菜单编码**：权限码格式 `homeai:module:operation`（如 `homeai:user:edit`），统一风格
3. **分包策略**：首页 + 个人中心 + 家庭管理在主分包（pages-homeai），AI 对话独立分包（pages-homeai-ai），其余模块在更多分包（pages-homeai-more），满足微信小程序分包大小限制

---

## 第二阶段：微信登录 + 家庭管理

> **完成日期**：2026-07-29
> **预计工期**：2 周
> **实际耗时**：1 天（核心逻辑实现，不含联调测试）

### 已完成任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1 | `HomeaiJwtUtil` 工具类 | ✅ | 基于 openid 的独立 JWT 体系，支持 APP/PC 双时效，含 RefreshToken |
| 2 | 微信登录接口 | ✅ | `POST /homeai/user/login`（code → openid → 自动注册 → JWT 签发 → Redis 缓存） |
| 3 | Token 刷新接口 | ✅ | `POST /homeai/user/refresh-token` |
| 4 | 用户信息接口 | ✅ | `GET /homeai/user/info`（从 Token 解析 openid） |
| 5 | 创建家庭 | ✅ | `POST /homeai/family`（同时创建成员关系，设置管理员） |
| 6 | 修改家庭信息 | ✅ | `PUT /homeai/family`（校验管理员权限） |
| 7 | 生成邀请码 | ✅ | `POST /homeai/family/invite-code`（6位，排除易混淆字符，24h有效期） |
| 8 | 加入家庭 | ✅ | `POST /homeai/family/members`（校验邀请码有效性） |
| 9 | 成员列表 | ✅ | `GET /homeai/family/members`（关联用户信息） |
| 10 | 移除成员 | ✅ | `DELETE /homeai/family/member/{id}`（仅管理员） |
| 11 | 退出家庭 | ✅ | `DELETE /homeai/family/leave`（管理员需先转让） |
| 12 | 解散家庭 | ✅ | `DELETE /homeai/family/disband`（逻辑删除+30天保留期） |
| 13 | 修改成员角色 | ✅ | `PUT /homeai/family/member/{id}/role`（仅管理员） |
| 14 | 转让管理员 | ✅ | `POST /homeai/family/transfer`（原管理员降级为成员） |
| 15 | 管理端用户列表 | ✅ | `BasicTable` 分页，支持查看详情抽屉 |
| 16 | 管理端家庭列表 | ✅ | `BasicTable` 分页，显示解散状态 |
| 17 | 小程序首页九宫格 | ✅ | 渐变背景 + 用户信息 + 6个功能入口（AI/存储/账单/计划/烹饪/学习） |
| 18 | 小程序个人中心 | ✅ | 用户卡片 + 统计概览 + 菜单列表 + 退出登录 |
| 19 | 小程序家庭管理 | ✅ | 创建/加入(邀请码)/成员列表/转让/解散(输入确认)/退出 |
| 20 | 小程序请求封装 | ✅ | 自动注入 Token、401 跳转登录 |
| 21 | 小程序状态管理 | ✅ | Pinia stores（user + family） |

### 新增/更新的文件

#### 后端（Phase 2 新增 2 个，更新 3 个）

```
jeecg-boot-module-homeai/
└── src/main/java/org/jeecg/modules/homeai/
    ├── config/
    │   └── HomeaiJwtUtil.java              ★ 新增：独立 JWT 工具
    ├── user/
    │   ├── service/IWxUserService.java     ★ 更新：增加 login/refreshToken/getByOpenid
    │   ├── service/impl/WxUserServiceImpl.java ★ 更新：完整登录逻辑
    │   └── controller/WxUserController.java  ★ 更新：登录+CRUD
    └── family/
        ├── service/IFamilyService.java          ★ 更新：增加业务方法
        ├── service/IFamilyMemberService.java    ★ 更新：增加查询方法
        ├── service/IFamilyInviteCodeService.java ★ 更新：增加生成/校验方法
        ├── service/impl/FamilyServiceImpl.java  ★ 更新：创建/解散/转让
        ├── service/impl/FamilyMemberServiceImpl.java ★ 更新：成员查询
        ├── service/impl/FamilyInviteCodeServiceImpl.java ★ 更新：邀请码
        └── controller/FamilyController.java  ★ 更新：完整接口
```

#### 小程序端（13 个新文件）

```
JeecgUniapp/src/pages-homeai/
├── pages/index/index.vue      ★ 新增：首页九宫格
├── pages/profile/index.vue    ★ 新增：个人中心
├── pages/family/index.vue     ★ 新增：家庭管理
├── api/request.ts             ★ 新增：请求封装
├── api/index.ts               ★ 新增：API 导出
├── stores/user.ts             ★ 新增：用户状态
├── stores/family.ts           ★ 新增：家庭状态
├── utils/auth.ts              ★ 新增：Token 管理
└── pages.config.ts            ★ 新增：页面配置
```

### 关键 API 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/homeai/user/login` | 微信登录（code 换 JWT） |
| POST | `/homeai/user/refresh-token` | 刷新 Token |
| GET | `/homeai/user/info` | 获取当前用户信息 |
| GET | `/homeai/user/list` | 用户列表（管理端） |
| GET | `/homeai/user/{id}` | 用户详情（管理端） |
| PUT | `/homeai/user/{id}` | 编辑用户（管理端） |
| DELETE | `/homeai/user/{id}` | 注销用户 |
| PUT | `/homeai/user/{id}/status` | 启用/禁用 |
| GET | `/homeai/family/info` | 获取当前用户家庭信息 |
| POST | `/homeai/family` | 创建家庭 |
| PUT | `/homeai/family` | 修改家庭信息 |
| POST | `/homeai/family/invite-code` | 生成邀请码 |
| POST | `/homeai/family/members` | 通过邀请码加入家庭 |
| GET | `/homeai/family/members` | 家庭成员列表 |
| DELETE | `/homeai/family/member/{id}` | 移除成员 |
| DELETE | `/homeai/family/leave` | 退出家庭 |
| DELETE | `/homeai/family/disband` | 解散家庭 |
| PUT | `/homeai/family/member/{id}/role` | 修改角色 |
| POST | `/homeai/family/transfer` | 转让管理员 |
| GET | `/homeai/family/list` | 家庭列表（管理端） |

### 关键决策

1. **JWT 双体系**：小程序端使用基于 openid 的独立 JWT（`HomeaiJwtUtil`），与管理端 Shiro Realm 体系隔离，避免相互干扰
2. **微信登录 mock**：未配置微信 appid/secret 时自动使用 mock openid，方便开发调试
3. **解散验证**：小程序端强制输入"确认解散"文字验证，防止误操作
4. **邀请码安全**：排除易混淆字符（0/O/1/I）、24h 有效期、一次性使用
5. **管理员转让原子性**：原管理员降级 + 目标升级在同一事务中完成
6. **退出保护**：仅剩管理员一人时不禁止退出，但存在其他成员时要求先转让

---

## 第三阶段：AI 对话模块

> **完成日期**：2026-07-29
> **预计工期**：3 周
> **实际耗时**：1 天（核心代码实现，需接入环境联调；AIFlow 编排待服务启动后补做）

### 已完成任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1 | `AiConversation` 实体 + Mapper + Service | ✅ | 对话主表，支持创建/重命名/软删除/分页列表 |
| 2 | `AiMessage` 实体 + Mapper + Service | ✅ | 消息表，用户/AI角色，保存/查询消息 |
| 3 | `AiKeyConfig` 实体 + Mapper + Service + Controller | ✅ | AES-256-GCM 加密存储 API Key，管理端 CRUD |
| 4 | `AiQuotaLog` 实体 + Mapper | ✅ | Token 消耗持久化记录，含 SUM 聚合查询 |
| 5 | SSE 流式对话接口 | ✅ | `POST /homeai/ai/chat/send`，复用 `jeecg-boot-module-airag` 的 `IAiragChatService` |
| 6 | Token 配额控制 | ✅ | Redis 原子计数（日/月），DB 持久化，允许最多透支 1000 Token |
| 7 | 对话历史管理 | ✅ | 重命名/软删除/消息查询 |
| 8 | AI 密钥选择策略 | ✅ | 优先使用默认模型 → 第一个启用的模型 |
| 9 | 管理端密钥配置页 | ✅ | 列表/新增/编辑(抽屉)/启用停用/设为默认 |
| 10 | 管理端额度概览页 | ✅ | 默认配额展示 |
| 11 | 小程序对话列表 | ✅ | 时间倒序/左滑删除/空态引导(示例话题) |
| 12 | 小程序聊天页 | ✅ | 消息气泡(角色区分)/SSE流式/附件选择(拍照/相册)/停止生成/Token不足提醒 |
| 13 | 对话路由注册 | ✅ | `vue3` + 小程序分包路由 |

### 新增/更新的文件

#### 后端（Phase 3 新增 22 个）

```
jeecg-boot-module-homeai/
└── src/main/java/org/jeecg/modules/homeai/
    └── ai/
        ├── entity/
        │   ├── AiConversation.java          ★ 新增：对话主表
        │   ├── AiMessage.java               ★ 新增：消息表
        │   ├── AiKeyConfig.java             ★ 新增：密钥配置（AES加密）
        │   └── AiQuotaLog.java              ★ 新增：Token消耗日志
        ├── mapper/
        │   ├── AiConversationMapper.java    ★ 新增
        │   ├── AiMessageMapper.java         ★ 新增
        │   ├── AiKeyConfigMapper.java       ★ 新增
        │   └── AiQuotaLogMapper.java        ★ 新增（含 selectTotalTokens 聚合查询）
        ├── mapper/xml/
        │   ├── AiConversationMapper.xml     ★ 新增
        │   ├── AiMessageMapper.xml          ★ 新增
        │   ├── AiKeyConfigMapper.xml        ★ 新增
        │   └── AiQuotaLogMapper.xml         ★ 新增
        ├── service/
        │   ├── IAiConversationService.java  ★ 新增
        │   ├── IAiMessageService.java       ★ 新增
        │   ├── IAiKeyConfigService.java     ★ 新增
        │   ├── IAiQuotaService.java         ★ 新增
        │   └── IHomeaiChatService.java      ★ 新增：SSE流式对话服务
        ├── service/impl/
        │   ├── AiConversationServiceImpl.java  ★ 新增
        │   ├── AiMessageServiceImpl.java       ★ 新增
        │   ├── AiKeyConfigServiceImpl.java     ★ 新增（AES-256-GCM 加密/解密）
        │   ├── AiQuotaServiceImpl.java         ★ 新增（Redis原子计数+SUM聚合查询）
        │   └── HomeaiChatServiceImpl.java      ★ 新增（复用 airag SSE 流式能力）
        └── controller/
            ├── AiConversationController.java  ★ 新增：对话CRUD API
            ├── AiKeyConfigController.java     ★ 新增：密钥管理 API
            └── HomeaiChatController.java      ★ 新增：SSE流式对话/配额检查/停止生成
```

#### 管理端前端（Phase 3 新增 4 个）

```
jeecgboot-vue3/
├── src/router/routes/modules/homeai/ai.ts       ★ 新增：AI路由（密钥配置+Token额度）
└── src/views/homeai/ai/
    ├── keyConfig.vue               ★ 新增：密钥列表页
    ├── KeyConfigDrawer.vue         ★ 新增：密钥编辑抽屉
    └── quota.vue                   ★ 新增：Token额度概览页
```

#### 小程序端（Phase 3 新增 3 个）

```
JeecgUniapp/
└── src/pages-homeai-ai/
    ├── pages.config.ts                    ★ 新增：分包路由配置
    └── ai/
        ├── conversations.vue              ★ 新增：对话列表页（含空态引导）
        └── chat.vue                       ★ 新增：聊天页（SSE流式+附件+停止+配额预检）
```

### 关键 API 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/homeai/ai/chat/send` | SSE 流式发送消息 |
| POST | `/homeai/ai/chat/stop` | 停止生成 |
| GET | `/homeai/ai/chat/quota` | Token 配额检查 |
| GET | `/homeai/ai/conversations/list` | 对话列表（管理端分页） |
| GET | `/homeai/ai/conversations/user/{userId}` | 用户对话列表（小程序端） |
| POST | `/homeai/ai/conversations` | 创建新对话 |
| PUT | `/homeai/ai/conversations/{id}/rename` | 重命名对话 |
| DELETE | `/homeai/ai/conversations/{id}` | 删除对话 |
| GET | `/homeai/ai/conversations/{id}/messages` | 获取对话消息列表 |
| GET | `/homeai/ai/key-config/list` | 密钥列表 |
| POST | `/homeai/ai/key-config` | 新增密钥 |
| PUT | `/homeai/ai/key-config` | 编辑密钥 |
| DELETE | `/homeai/ai/key-config/{id}` | 删除密钥 |
| PUT | `/homeai/ai/key-config/{id}/status` | 启用/停用 |
| PUT | `/homeai/ai/key-config/{id}/default` | 设为默认模型 |
| GET | `/homeai/ai/key-config/quota/default` | 默认配额配置 |

### 关键决策

1. **AI 能力复用**：SSE 流式对话直接调用 `jeecg-boot-module-airag.IAiragChatService`，避免重新实现 LangChain4j 集成，只需包装自有对话管理和 Token 配额
2. **API Key 加密**：采用 AES-256-GCM 模式对 API Key 进行加密存储（加密密钥通过 `homeai.ai.key-encryption-key` 配置），GCM 模式提供认证加密，防止篡改
3. **Token 配额双存储**：Redis 做原子计数和快速查询（带过期时间），DB 做持久化审计记录。Redis 过期时间自动对齐日/月周期
4. **配额预检 + 透支机制**：发送消息前预估 Token 消耗并检查配额，允许最多透支 1000 Token 防止因回复过长而中断
5. **模型选择链**：优先使用用户指定的模型 → 默认模型 → 第一个启用的模型
6. **对话列表自动命名**：取用户消息前 30 字符作为对话标题（去换行），支持手动重命名
7. **小程序 SSE 实现**：使用 `uni.request` 的 `enableChunked` 模式接收流式响应，暂未接入 Markdown 渲染库（后续可集成 `mp-html` 或自定义 Markdown 渲染组件）
8. **AIFlow 编排延后**：jeecg-aiflow 脚本需连接运行中的 JeecgBoot 管理后台，当前开发阶段服务未启动。`HomeaiChatServiceImpl` 直接调用 `IAiragChatService.send()` 已具备完整 SSE 流式能力，等待服务启动后可通过 aiflow 脚本创建自定义编排流程

---

## 第四阶段：资料存储 + Office 处理

> **完成日期**：2026-07-29
> **预计工期**：3-4 周
> **实际耗时**：1 天（核心代码实现）

### 已完成任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1 | `StorageFolder` 实体 + 树形查询 | ✅ | 文件夹 CRUD、树形构建、嵌套层级管理 |
| 2 | `StorageFile` 实体 + 文件操作 | ✅ | 上传/软删除/收藏/搜索/文件类型图标 |
| 3 | `StorageConvertTask` 实体 + 异步任务 | ✅ | PENDING→PROCESSING→COMPLETED/FAILED 状态机 |
| 4 | `OfficeTemplate` 实体 + 模板管理 | ✅ | Word/Excel/PPT 模板，设为默认 |
| 5 | `ConvertRule` 实体 + 规则管理 | ✅ | 源格式→目标格式映射，启用/停用 |
| 6 | 文件夹管理接口 | ✅ | 树查询/创建/删除/子文件夹 |
| 7 | 文件管理接口 | ✅ | 上传/删除/收藏/搜索/详情/列表 |
| 8 | Office 转换接口 | ✅ | 提交转换任务/查询状态/历史记录 |
| 9 | AI 生成接口 | ✅ | 提交生成任务 |
| 10 | 管理端文件管理页 | ✅ | `BasicTable` 分页+搜索 |
| 11 | 管理端模板管理页 | ✅ | 列表+默认设置 |
| 12 | 管理端转换规则页 | ✅ | 列表+启用停用开关 |
| 13 | 管理端处理记录页 | ✅ | 状态标签+筛选 |
| 14 | 小程序文件夹页 | ✅ | 文件夹列表+新建/上传 |
| 15 | 小程序文件列表页 | ✅ | 图标区分/NEW角标/长按菜单 |
| 16 | 小程序格式转换页 | ✅ | 动态可选目标格式 |
| 17 | 小程序AI生成页 | ✅ | 文档类型选择+模板选择+描述输入 |
| 18 | 小程序处理历史页 | ✅ | 状态标签+时间线 |
| 19 | 小程序搜索页 | ✅ | 文件名模糊搜索 |
| 20 | 路由+菜单+分包 | ✅ | Vue3路由 / sys_permission菜单 / pages.json分包 |

### 新增/更新的文件

#### 后端（Phase 4 新增 25 个）

```
jeecg-boot-module-homeai/
└── storage/
    ├── entity/ (5个实体)
    ├── mapper/ (5个接口 + 5个 XML)
    ├── service/ (5个接口)
    ├── service/impl/ (5个实现)
    └── controller/ (4个控制器)
```

#### 管理端前端（5 个）

```
jeecgboot-vue3/src/views/homeai/storage/
├── fileList.vue
├── officeTemplate.vue
├── convertRule.vue
├── officeHistory.vue
└── router/routes/modules/homeai/storage.ts
```

#### 小程序端（6 个）

```
JeecgUniapp/src/pages-homeai-more/storage/
├── index.vue
├── files.vue
├── office-convert.vue
├── office-generate.vue
├── office-history.vue
└── search.vue
```

### 关键 API 清单

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/homeai/storage/folders` | 获取文件夹树 |
| POST | `/homeai/storage/folders` | 创建文件夹 |
| DELETE | `/homeai/storage/folders/{id}` | 删除文件夹 |
| GET | `/homeai/storage/folders/{folderId}/files` | 获取文件夹内文件 |
| POST | `/homeai/storage/files/upload` | 上传文件 |
| DELETE | `/homeai/storage/files/{id}` | 删除文件 |
| PUT | `/homeai/storage/files/{id}/favorite` | 收藏/取消 |
| GET | `/homeai/storage/files/search` | 搜索文件 |
| GET | `/homeai/storage/files/{id}` | 文件详情 |
| POST | `/homeai/storage/office/convert` | 提交格式转换 |
| POST | `/homeai/storage/office/generate` | 提交AI生成 |
| GET | `/homeai/storage/office/tasks/{id}` | 查询任务状态 |
| GET | `/homeai/storage/office/history` | 处理历史 |
| GET | `/homeai/storage/template/list` | 模板列表(管理端) |
| POST/ PUT/ DELETE | `/homeai/storage/template` | 模板CRUD |
| PUT | `/homeai/storage/template/{id}/default` | 设默认模板 |
| GET | `/homeai/storage/template/enabled` | 启用模板(小程序) |
| GET | `/homeai/storage/rule/list` | 规则列表(管理端) |
| POST/ PUT/ DELETE | `/homeai/storage/rule` | 规则CRUD |
| PUT | `/homeai/storage/rule/{id}/status` | 启用/停用规则 |
| GET | `/homeai/storage/rule/targets` | 可选目标格式(小程序) |

### 关键决策

1. **文件夹树形结构**：使用 `parent_id` + `level` 字段构建树，支持最多5级嵌套。`buildTree` 方法通过递归组装父子关系
2. **文件存储**：保留原始文件名 + UUID 防重名，存储路径 `/upload/homeai/{userId}/{uuid}.{ext}`
3. **Office 异步任务**：格式转换和 AI 生成采用异步模式（PENDING→PROCESSING→COMPLETED/FAILED），前端轮询查询状态
4. **文件类型图标**：后端提供扩展名→图标映射，支持 20+ 常见格式
5. **收藏功能**：通过 `is_favorite` 字段标记，前端切换
6. **格式转换规则**：管理端动态配置源格式→目标格式映射，前端根据规则动态展示可选目标
7. **文档模板**：支持 Word/Excel/PPT 三类，可设置默认模板供 AI 生成时使用

---

## 第五阶段：账单模块

> **完成日期**：2026-07-29
> **预计工期**：2 周

### 已完成任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1 | `BillEntry` 实体 + CRUD | ✅ | 账单记录，含乐观锁版本校验 |
| 2 | `BillCategory` 实体 | ✅ | 消费分类，系统默认+用户自定义 |
| 3 | `BillImportRecord` 实体 | ✅ | 导入记录追踪 |
| 4 | 月度概览接口 | ✅ | 总支出/总收入/结余/账单数 |
| 5 | 月度账单列表 | ✅ | 按月份查询 |
| 6 | 分类统计 | ✅ | 按分类汇总金额 |
| 7 | 管理端账单列表 | ✅ | 分页+筛选 |
| 8 | 管理端分类管理 | ✅ | CRUD |
| 9 | 小程序账单首页 | ✅ | 概览卡片+最近10条 |
| 10 | 小程序记一笔 | ✅ | 类型切换/分类网格/日期/支付方式 |
| 11 | API路由+菜单 | ✅ | Vue3路由/菜单SQL/pages.json |

### 新增文件（16个）

后端: BillEntry, BillCategory, BillImportRecord (3实体) + 3 Mapper + 3 XML + 2 Service接口 + 2实现 + BillController
管理端: bill/billList.vue + bill/billCategory.vue + bill.ts路由
小程序: bill/index.vue + bill/add.vue + bill/statistics.vue

### 关键 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/homeai/bill/entry` | 记一笔 |
| PUT | `/homeai/bill/entry/{id}` | 编辑 |
| DELETE | `/homeai/bill/entry/{id}` | 删除 |
| GET | `/homeai/bill/entries` | 账单列表 |
| GET | `/homeai/bill/statistics` | 月度统计 |
| GET | `/homeai/bill/categories` | 分类列表 |
| POST/PUT | `/homeai/bill/category` | 分类CRUD |
| GET | `/homeai/bill/list` | 管理端分页 |
| GET | `/homeai/bill/category-list` | 管理端分类分页 |

---

## 第六阶段：日常计划模块

> **完成日期**：2026-07-29
> **预计工期**：1.5 周

### 已完成任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1 | `PlanMaster` 实体 | ✅ | 主计划（标题/日期/优先级/分类/提醒） |
| 2 | `PlanInstance` 实体 | ✅ | 计划实例（关联主计划/日期/状态/已提醒标记） |
| 3 | 日历概览 | ✅ | 返回某月有计划标记的日期列表 |
| 4 | 某日计划查询 | ✅ | 查询指定日期的所有计划实例 |
| 5 | 状态切换 | ✅ | pending ↔ completed 双向切换 |
| 6 | 实例创建 | ✅ | 为主计划创建指定日期的实例 |
| 7 | 管理端计划列表 | ✅ | 分页+筛选 |
| 8 | 小程序日历视图 | ✅ | 月历+日期标记+点击查看 |
| 9 | 小程序新增计划 | ✅ | 标题/日期/优先级/分类/提醒 |
| 10 | API路由+菜单 | ✅ | |

### 新增文件（10个）

后端: PlanMaster + PlanInstance (2实体) + 2 Mapper + 2 XML + IPlanService + PlanServiceImpl + PlanController
管理端: plan/planList.vue + plan.ts路由
小程序: plan/index.vue

### 关键 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/homeai/plan` | 创建计划 |
| GET | `/homeai/plan/calendar` | 日历概览 |
| GET | `/homeai/plan/date/{date}` | 某日计划 |
| PUT | `/homeai/plan/instance/{id}/toggle` | 切换完成 |
| GET | `/homeai/plan/list` | 管理端分页 |

---

## 第七阶段：烹饪指南 + 学习模块

> **完成日期**：2026-07-29
> **预计工期**：2-3 周

### 已完成任务清单

| # | 任务 | 状态 | 说明 |
|---|------|------|------|
| 1 | `Recipe` 实体 | ✅ | 菜谱(名称/分类/封面/视频/烹饪时间/难度) |
| 2 | `RecipeIngredient` 实体 | ✅ | 食材(名称/用量/单位) |
| 3 | `RecipeStep` 实体 | ✅ | 步骤(序号/描述/图片) |
| 4 | `LearnMaterial` 实体 | ✅ | 学习资料(标题/类型/URL/时长) |
| 5 | `LearnRecord` 实体 | ✅ | 学习记录(计时/手动/笔记) |
| 6 | 菜谱CRUD | ✅ | 列表/详情/搜索/创建/编辑/删除 |
| 7 | 学习计时 | ✅ | 开始学习(stopwatch) → 停止 → 记录时长 |
| 8 | 学习记录 | ✅ | 按用户查询 |
| 9 | 管理端菜谱管理 | ✅ | 列表(含封面图) |
| 10 | 管理端学习资料 | ✅ | 列表 |
| 11 | 小程序菜谱列表 | ✅ | 网格展示+搜索 |
| 12 | 小程序菜谱详情 | ✅ | 封面+信息+描述 |
| 13 | API路由+菜单 | ✅ | |

### 新增文件（21个）

后端: Recipe + RecipeIngredient + RecipeStep + LearnMaterial + LearnRecord (5实体) + 5 Mapper + 5 XML + IRecipeService + ILearnService + RecipeServiceImpl + LearnServiceImpl + RecipeController + LearnController
管理端: recipe/recipeList.vue + learn/learnList.vue + recipe.ts路由 + learn.ts路由
小程序: recipe/index.vue + recipe/detail.vue + recipe/add.vue + learn/index.vue

### 关键 API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/homeai/recipe/list` | 菜谱列表 |
| GET | `/homeai/recipe/{id}` | 菜谱详情 |
| GET | `/homeai/recipe/search` | 搜索 |
| POST/PUT/DELETE | `/homeai/recipe` | 菜谱CRUD |
| GET | `/homeai/learn/materials` | 学习资料列表 |
| POST | `/homeai/learn/start` | 开始学习 |
| POST | `/homeai/learn/stop` | 停止学习 |
| GET | `/homeai/learn/records` | 学习记录 |
| POST/PUT/DELETE | `/homeai/learn/material` | 资料CRUD |
