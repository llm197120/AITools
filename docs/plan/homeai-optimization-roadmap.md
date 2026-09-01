---
name: 家庭AI小工具 - 迭代优化路线图
version: v4
status: 进行中（第 13～123 轮已落地；ComfyUI 专项见仓库根目录 ComfyUI/）
updated: 2026-08-31
---

# 家庭AI小工具 - 迭代优化路线图

> 本文档汇总第 **1～12 轮**业务迭代，以及 **第 13～120 轮**工程化/业务/视觉/安全优化落地内容。  
> ComfyUI 本地路线（第 41～42 轮）已拆分至独立目录 [`ComfyUI/`](../../ComfyUI/README.md)（路线图：[comfyui-roadmap.md](../../ComfyUI/docs/comfyui-roadmap.md)）。  
> 业务模块后续建议见第三节。

相关路径索引：

| 领域 | 路径 |
|------|------|
| SQL 脚本 | `JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql/` |
| 后端模块 | `JeecgBoot/.../jeecg-boot-module-homeai/src/main/java/org/jeecg/modules/homeai/` |
| 管理端 | `JeecgBoot/jeecgboot-vue3/src/views/homeai/` |
| 小程序 | `JeecgUniapp/src/pages-homeai-more/` |
| DB 初始化 | `scripts/init-db.bat`（Windows）、`scripts/init-db.sh`（Linux/Mac） |

---

## 一、已完成迭代摘要（第 1～13 轮）

### 第 1 轮：存储 / 计划 / 学习基础体验修复

| 能力 | 要点 |
|------|------|
| **文件上传文件夹选择** | 上传弹窗、新建文件夹的「上级文件夹」改为 `a-tree-select`；打开上传时预填当前选中文件夹 |
| **文档模板文件上传** | 移除手动填 URL；`POST /homeai/storage/template/create-with-file` 一步创建 + 上传 |
| **上传弹窗关闭** | 上传/新建/编辑文件夹成功后调用 `closeModal()` |
| **Office 处理按钮** | 文件列表增加「格式转换」「转 PDF」；转换规则 API 驱动目标格式选择 |
| **计划分类独立维护** | `homeai_plan_category` 表 + CRUD；`planCategory.vue`；计划表单/搜索分类下拉 |
| **学习资料文件上传** | `LearnDrawer` 改为文件选择；`POST /homeai/learn/upload` 预上传；编辑走 `materials/{id}/upload` |
| **鉴权路径登记** | `HomeaiAuthInterceptor` 补全计划分类、学习上传、模板上传等管理端路径 |

**迁移 SQL：**

```text
alter_homeai_plan_category.sql
```

---

### 第 2 轮：文件夹 / 菜谱 / 学习后端校验

| 能力 | 要点 |
|------|------|
| **文件夹循环引用** | `validateParentNotCycle()` + `updateFolder()`；禁止自引用、禁止移入子孙目录；变更父级时递归刷新 `level` |
| **菜谱分类后台维护** | `IRecipeCategoryService` CRUD；名称唯一；删除前检查菜谱引用；`RecipeController` 校验 `categoryId` |
| **学习资料格式校验** | `validateFileFormat(type, file)` 按资料类型校验扩展名；`POST /learn/upload` 增加 `type` 参数 |

**API（菜谱分类）：** `GET /recipe/category/list|all`，`POST/PUT/DELETE /recipe/category`

**迁移 SQL：**

```text
alter_homeai_menus_recipe_category.sql   # 已有库补菜单（新库见 init_homeai_menus.sql）
init_homeai_recipe_category.sql          # 默认分类数据（按需）
```

---

### 第 3 轮：Agent-Team 审查 — 菜单 / 路由 / 页面梳理

参照 `docs/design/module-details.md` 与设计评审文档，对照设计方案完成后台梳理：

| 角色 | 优化 |
|------|------|
| **菜单结构** | Layout 父菜单 + 子页面；补 AI 对话、账单统计/导入、学习记录；分类页归入对应模块 |
| **路由对齐** | `homeai.ts` 重构；AI 统一为 `/homeai/ai/*`；各模块父路由补 `LAYOUT` |
| **后端规则** | 文件夹最多 **5 级**（`MAX_FOLDER_DEPTH`）；菜谱分类 Service 层校验 |
| **前端体验** | `RecipeDrawer` / `recipeList` 分类下拉 + 名称展示；`LearnDrawer` 上传带 `type` |

**迁移 SQL：**

```text
alter_homeai_menus_optimize.sql
```

**第 3 轮评审遗留（后于第 5 轮落地）：** 学习分类 CRUD、文件白名单页、Office 异步引擎、计划日历视图、AI 对话权限分配。

---

### 第 4 轮：鉴权重构 + 家庭状态 + 初始化对齐

第 3 轮后用户触发「继续迭代」前的**基础加固**，为第 5 轮批量能力铺路：

| 能力 | 要点 |
|------|------|
| **鉴权架构** | `HomeaiAuthFilter` 废弃 → `HomeaiAuthInterceptor` + `HomeaiMvcConfig`；拦截器在 Shiro 链之后执行，管理端 `@RequiresPermissions` 生效 |
| **鉴权白名单** | 随第 1～3 轮新增 API 持续完善 `ADMIN_PREFIXES`（存储、计划、菜谱、学习、AI 等） |
| **家庭状态字段** | `homeai_family.status`：`normal`（正常）/ `disbanded`（已解散，保留数据） |
| **DB 初始化** | `init-db.sh` / `init-db.bat` 纳入早期 `alter_homeai_*.sql` 自动导入列表 |

**迁移 SQL：**

```text
alter_homeai_family_status.sql
```

---

### 第 5 轮：学习分类 + 白名单 + Office 异步 + 计划日历

| 能力 | 要点 |
|------|------|
| **学习分类** | `LearnCategory` CRUD；`learnCategory.vue`；`LearnDrawer` / `learnList` 分类下拉 |
| **文件白名单** | `homeai_file_whitelist` 表；`GET/PUT /homeai/config/file-whitelist`；`fileWhitelist.vue`；上传走 DB 白名单 + Redis 缓存 |
| **Office 异步转换** | `StorageOfficeConvertExecutorImpl`：`PENDING→PROCESSING→COMPLETED/FAILED`；LibreOffice `soffice`；30 秒兜底扫描 |
| **计划日历（管理端）** | `GET /plan/admin/calendar`、`GET /plan/admin/date/{date}`；`planList.vue` 日历 Tab |
| **菜单权限** | 学习分类、文件白名单、AI 对话按钮权限 |

**迁移 SQL：**

```text
alter_homeai_learn_category.sql
alter_homeai_file_whitelist.sql
alter_homeai_menus_iteration5.sql
```

**配置：** `homeai.office.soffice-path`（LibreOffice 路径）

---

### 第 6 轮：小程序对齐 + 上传安全

| 能力 | 要点 |
|------|------|
| **文件魔数校验** | `HomeaiFileMagicUtil`；接入存储 / AI 附件 / 学习资料上传，防扩展名伪造 |
| **计划过期任务** | `PlanExpireScheduler` 每日 00:05 将过期 pending 实例标为 `expired` |
| **学习 API** | `POST /learn/material` 返回含 `id` 的完整对象；`POST /learn/upload` 改为登录用户可用 |
| **小程序白名单** | `pages-homeai/utils/fileWhitelist.ts`（5 分钟缓存）；存储 / AI 上传前校验 |
| **小程序计划** | `plan/index.vue` 日历视图；`plan/add.vue` 分类下拉 + 带日期跳转 |
| **小程序学习** | `learn/index.vue` 分页/统计修复；新增 `learn/add.vue` |
| **API 集中** | `pages-homeai/api/index.ts`：`planApi`、`learnApi`、`configApi` |

---

### 第 7 轮：重复计划 + 订阅提醒 + AI 生成 + 学习日历

| 能力 | 要点 |
|------|------|
| **重复计划** | `PlanRepeatUtil`（none/daily/weekly/monthly）；创建时预生成 90 天实例；`PlanRepeatScheduler` 滚动补齐；`PlanInstanceCleanupScheduler` 清理 30 天前实例 |
| **微信订阅提醒** | `HomeaiWxSubscribeService` + `PlanRemindScheduler`（每分钟扫描）；未配置模板时模拟推送 |
| **AI 文档生成** | `StorageAiGenerateService`（poi-tl / POI）；`StorageOfficeConvertExecutorImpl` 接入 `ai_generate`；`instruction` 字段 |
| **学习日历** | `GET /learn/calendar?yearMonth=`；小程序 `learn/index.vue` 日历 Tab |
| **其他** | 首页待办仅统计今日 `pending` 计划 |

**迁移 SQL：**

```text
alter_homeai_office_convert_instruction.sql
```

**配置：**

```yaml
homeai:
  wechat:
    appid: your-appid
    secret: your-secret
    plan-remind-template-id: your-template-id  # 可选
```

---

### 第 8 轮：计划 + AI 文档

- 计划配置服务（Redis + yml）、`planConfig.vue`、日历 API（含过期/待办日期）
- 小程序订阅消息、过期灰色打点
- AI 文档润色、计划相关菜单 SQL

### 第 9 轮：管理端计划日历

- 管理端日历基于 **PlanInstance**
- 重复计划补跑 `POST /homeai/plan/admin/repeat/roll-forward`
- 管理端日历过期打点、补跑按钮

### 第 10 轮：审计 + 配额

- 日历用户筛选、补跑审计日志（`homeai_audit_log`）
- AI 生成配额预检、小程序 Token 预估展示

### 第 11 轮：可见性 + 审计页

- **菜谱**：列表/搜索可见性过滤（本人 + 家庭共享）
- **存储**：文件夹树合并家庭共享；创建文件夹写入 `familyId`
- **学习**：计时会话迁 Redis；小程序计时条 + 结束学习
- **计划**：完成率统计用户筛选；操作审计页 `auditLog.vue`

### 第 12 轮：菜谱 / 资料 / 学习（进行中已落地部分）

| 模块 | 已完成 |
|------|--------|
| **菜谱** | 详情鉴权；`familyId` 写入；收藏表 + API；小程序详情收藏按钮 |
| **资料** | 家庭文件夹/文件读权限；搜索合并家庭文件；上传写 `familyId`；管理端/小程序 UI 标记 |
| **学习** | `categoryId` 字段 + 迁移 SQL；分类校验；近 30 日趋势 API + ECharts |

---

### 第 13 轮：前端工程化（Agent-Team 评审 · P0 / P1 / P2）

> 来源：2026-08-11 agent-team 全项目前端检查。重点：运行时正确性、双端一致性、构建/CI、包体积与体验。

#### P0 — 运行时正确性

| 项 | 落地 | 关键路径 |
|----|------|----------|
| 生产 API 不再指向 localhost | `VITE_GLOB_DOMAIN_URL=/jeecgboot`（同域反代）；注释说明以 `_app.config.js` 覆盖为准 | `jeecgboot-vue3/.env.production` |
| 关闭非必要微前端开关 | `VITE_GLOB_APP_OPEN_QIANKUN=false` | `jeecgboot-vue3/.env` |
| 菜单加载失败可见反馈 | catch 中 `createMessage.error('菜单加载失败，请刷新重试')` | `src/store/modules/permission.ts` |
| homeai 路由单一来源说明 | BACK 模式以后台菜单为准；前端 `homeai.ts` 作开发对照 | `src/router/routes/modules/homeai/homeai.ts` |
| 小程序鉴权/路由统一 | router 改用 HomeAI store；去掉已删 login/OAuth2/flow；`@/store/user` 桥接 token | `JeecgUniapp/src/router/index.ts`、`store/user.ts`、`interceptors/route.ts` |

#### P1 — 可维护性与一致性

| 项 | 落地 | 关键路径 |
|----|------|----------|
| Vite manualChunks 补全 | antd / vxe / echarts 独立 vendor chunk | `jeecgboot-vue3/vite.config.ts` |
| 冗余依赖清理 | 移除 `vue-cropper`/`vue-cropperjs`；`mockjs` → devDependencies | `package.json` |
| 删除备份文件 | `Tree_backup/`、`useMessage.tsx_backup`、`notify/index_old.vue` | — |
| homeai API 类型化 | 新增 `types.ts`；`index.ts` 去掉大量 `any` | `src/api/homeai/` |
| CRUD 抽象 + 大页拆分 | `useHomeaiCrud`；`planCategory` 接入；`useFileFolderTree` / `PlanCalendarTab` | `views/homeai/hooks/`、`storage/`、`plan/` |
| 双端主题对齐 | 小程序主色 `#0960bd`；替换原紫色渐变 | `layouts/default.vue`、`pages.json`、业务页 |
| 品牌文案 | manifest/README →「家庭AI小工具」 | `JeecgUniapp/` |
| CI 管理端构建 | `frontend-admin` job：`pnpm install` + `pnpm test` + `pnpm build` | `.github/workflows/ci.yml` |

#### P2 — 体积 / 体验 / 质量门禁

| 项 | 落地 | 关键路径 |
|----|------|----------|
| 排除 demo 动态页 | `dynamicPages` glob 排除 `views/demo/**` | `src/utils/dynamicPages.ts` |
| Swagger UI 懒加载 | 动态 `import('swagger-ui-dist/...')` | `views/openapi/SwaggerUI.vue` |
| homeai API 冒烟测试 | `tests/homeai/api-types.spec.ts` + `pnpm test` | `jeecgboot-vue3/tests/` |
| 删除小程序 Online/service 死代码 | 移除 `components/online/`、`src/service/` | `JeecgUniapp/src/` |
| 移除未用依赖 | `echarts`、`@tanstack/vue-query`；`main.ts` 去掉 VueQueryPlugin | `package.json`、`main.ts` |
| 学习列表服务端分页 | pageSize=20 + onReachBottom / 加载更多 | `pages-homeai-more/learn/index.vue` |
| 账单客户端分页 | 整月数据本地步进 20 条展示 | `pages-homeai-more/bill/index.vue` |
| AI SSE 降级 | 检测 `onChunkReceived`；不支持则非流式；fail 可重试 | `pages-homeai-ai/ai/chat.vue` |

**部署注意（P0）：** ECS/Nginx 部署后请确认 `dist/_app.config.js` 中 `VITE_GLOB_DOMAIN_URL` 与实际 API 域名一致。

---

### 第 14 轮：Agent-Team 第二轮检查与优化（2026-08-12）

> 复查第 13 轮回归 + 路线图遗留项。共识：先修 CI/数据正确性与配置，再做 CRUD 统一与死代码清理。

#### P0

| 项 | 落地 | 关键路径 |
|----|------|----------|
| homeai API TS / PATCH | `getById` 未用参数改为 `_id`；文件夹可见性改 `defHttp.request(PATCH)` | `jeecgboot-vue3/src/api/homeai/index.ts` |
| CI 测试可跑通 | 补 `jest-environment-jsdom@29`；冒烟测试改为只测 `types`（不拉 axios 链） | `package.json`、`tests/homeai/api-types.spec.ts` |
| 菜谱食材 quantity/unit | 小程序工具函数 + add/detail；管理端 RecipeDrawer 同步 | `JeecgUniapp/.../recipeIngredient.ts`、`recipe/add|detail.vue`、`RecipeDrawer.vue` |
| TabBar 源配置紫色残留 | `pages.config.ts` → `#0960bd`（避免 rebuild 覆盖 pages.json） | `JeecgUniapp/pages.config.ts` |
| UniApp 生产 API | 去掉 demo 域名；`NODE_ENV=production`；占位 `YOUR_DOMAIN` | `JeecgUniapp/env/.env.production` |
| 管理端 docker.prod DOMAIN | `/jeecgboot` 与 Nginx / `.env.production` 对齐 | `.env.docker.prod` |
| CI 构建对齐部署 | `pnpm run build:docker:prod` | `.github/workflows/ci.yml` |

#### P1

| 项 | 落地 | 关键路径 |
|----|------|----------|
| Category 全面接入 useHomeaiCrud | learn / recipe / bill + recipe/bill category API 封装 | `views/homeai/*Category.vue`、`api/homeai/index.ts` |
| 401 跳转个人中心 | `switchTab` → profile | `pages-homeai/api/request.ts` |
| 深链鉴权补洞 | 拦截 `redirectTo` / `reLaunch` | `interceptors/homeaiRoute.ts` |
| 菜谱提交校验 categoryId | submit 前拦截 | `recipe/add.vue` |
| SSE 流式成功避免重复解析 | `handleChatSuccess(..., usedChunked)` | `pages-homeai-ai/ai/chat.vue` |
| 删除 Jeecg 遗留组件 | SelectUser/Popup/PageLayout 等 + work.ts + demo layout | `JeecgUniapp/src/components/*` |
| 调试日志 DEV 包裹 | permission / permissionGuard | `jeecgboot-vue3/src/store|router` |
| 清理 orphan tanstack 包 | match-sorter-utils / query-core | `JeecgUniapp/package.json` |

**部署注意（第 14 轮）：**

1. 将 `JeecgUniapp/env/.env.production` 中 `YOUR_DOMAIN` 换成真实 API 域名  
2. 两端改过 `package.json` 后需本地 `pnpm install` 刷新 lock  
3. 管理端服务器构建请用 `pnpm run build:docker:prod`（与 CI 一致）

---

### 第 15 轮：Agent-Team 第三轮（业务 + 回归修复 · 2026-08-12）

> 共识：修 fileList 可见性回归 → 上传者可读名 → 学习按分类统计 → 小程序守卫/打开即学 → 文档与死依赖。

#### P0

| 项 | 落地 | 关键路径 |
|----|------|----------|
| 编辑文件夹丢失 visibility | 传入 `visibility` + `familyIds` | `fileList.vue` |
| 上传者可读名 | `userApi.options` 映射昵称（树/列表/Top5） | `fileList.vue` |
| recipeApi.getById | Drawer 改用 API；失败 toast；`HomeaiRecipeIngredient` 类型 | `api/homeai`、`RecipeDrawer.vue` |
| keyConfig 删除确认 | 接入 `keyConfigApi` + createConfirm | `keyConfig.vue` |

#### P1

| 项 | 落地 | 关键路径 |
|----|------|----------|
| 学习按分类统计 | `GET /homeai/learn/admin/stats/category`（权限 `homeai:learn:material:list`，时长分钟） | `LearnServiceImpl`、`LearnController`、`learnRecord.vue` |
| 鉴权登记 | `ADMIN_PREFIXES` 含 `/homeai/learn/admin` | `HomeaiAuthInterceptor` |
| 子包登录守卫 | `useHomeaiPageGuard` | bill/plan/recipe/learn/storage/chat |
| 打开即学 | `autoStart=1` | `learn/index.vue`、`detail.vue` |
| 菜谱 Excel 说明 | 文档化「仅主表」 | `docs/guide/recipe-excel-import.md` |
| 部署文档对齐 | CI/CD 已实施；Artifacts 表述修正；上架 checklist | `docs/deploy/README.md`、`frontend-nginx/README.md` |

#### P2

| 项 | 落地 | 关键路径 |
|----|------|----------|
| 删 enquire.js / lodash.get | package.json + trace 可选链 | `jeecgboot-vue3` |
| DEV 动态页 console | `import.meta.env.DEV` | `utils/index.ts` |
| 遗留清理 | `http.ts` / `usePageList`；SelectUser 等确认已删 | `JeecgUniapp` |
| targetSdk 34 + 上架注释 | 不强制改 urlCheck | `manifest.config.ts` |

**无新增 SQL。** 小程序生产仍须手动将 `YOUR_DOMAIN` 换成真实 API。

---

### 第 16 轮：双端 UI 视觉统一（暖灰 + 深蓝 · 2026-08-12）

> 共识：小程序「健身 App 风」暖灰底 `#F3F2EE` + 深蓝 `#1B4F8A`；管理端同源语义色、保持表格密度（Same Brand, Different Density）。

#### 小程序

| 项 | 落地 | 关键路径 |
|----|------|----------|
| 设计令牌 | `--hai-*` + `.hai-page/.hai-card/.hai-fab`；wot 主题色对齐 | `style/homeai-theme.scss`、`layouts/default.vue` |
| 首页改版 | 问候/AI 英雄区/六宫格快捷入口/今日计划/双卡；胶囊安全区 | `pages/homeai/index.vue` |
| Tab/模块页 | 计划账单菜谱学习存储 AI/家庭/我的 统一暖灰卡片 | `pages-homeai*` |
| 表单容器 | `HomeFormCard` + 共享字段类；plan/learn/bill/recipe add（及 bill edit）迁入 | `components/HomeFormCard.vue` |
| 空态/骨架 | `HomeEmpty`、`HomeSkeleton` | `components/` |
| TabBar | 自制 PNG + `selectedColor #1B4F8A`；家庭 Tab 脱离 demo 图标 | `static/tabbar/`、`pages.config.ts` |
| 触控扫尾 | FAB/底栏 safe-area；计划勾选热区；存储箭头色 `#A39E94` | 多页 |

#### 管理端

| 项 | 落地 | 关键路径 |
|----|------|----------|
| 主题令牌 | `homeai-theme.less` + 金额语义色工具类 | `design/homeai-theme.less` |
| 列表壳 | ~23 页 `PageWrapper dense` | `views/homeai/**` |
| 可读名 | `useUserLabel` 昵称替代裸 userId | plan/bill/conversation/fileList 等 |
| 计划完善 | PlanDrawer 重复规则/全天/起止/提醒；日历今日高亮与图例 | `PlanDrawer`、`PlanCalendarTab` |
| 账单 | 导入空态引导；支出/收入语义色 | `billImport`、`billList` |

**无新增 SQL。** 后续可选：更多页面硬编码 hex → `var(--hai-*)`；TabBar 图标精细化；对话左滑删除。

#### 第 16 轮续（列表令牌 + TabBar · 同日）

| 项 | 落地 |
|----|------|
| 列表/详情令牌化 | bill / learn / recipe / plan.detail / storage* / all-functions / family / profile / 首页 style 批量迁 `var(--hai-*)`；增补 `--hai-danger-soft`、`--hai-on-primary` |
| TabBar | 重绘 home/family/user（选中实心、未选中描边）；`tabBar.color` → `#8A857C` |
| 组件色 | `all-functions` 的 `wd-icon` 改为 `haiPrimary` / `haiTertiary` 常量 |

**无新增 SQL。** 后续可选：AI 对话左滑删除；管理端更多页接入语义工具类。

#### 第 17 轮：UI 复审 Wave 1（硬伤 + 规范止血 · 2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 小程序 | 首页 CSS | 修复 `var(--hai-shadow))` / `var(--hai-border))` |
| 小程序 | wot 圆角 | `.wd-button.is-round` → `999rpx` |
| 小程序 | 页壳 | 主列表/详情改 `.hai-page` / `--fab`；增补 `--hai-warning` |
| 管理端 | officeTemplate | 修正错乱闭合标签 |
| 管理端 | 页壳 | 定义并全量挂载 `.homeai-page-body` |
| 管理端 | 工具栏 | 导入/导出/下载/恢复降为 default，仅「新增」primary |
| 管理端 | 账单语义 | billImport 类型/金额着色；billList 类型改 hai 文本色 |

**无新增 SQL。** 下一波：空态/FAB 统一、useUserLabel 扩展、Drawer 体验。

#### 第 18 轮：UI 复审 Wave 2（组件语言收敛 · 2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 小程序 | HomeEmpty | 支持 `iconName` + `#actions` 插槽；默认 inbox 线图标 |
| 小程序 | 空态 | AI 对话 / 家庭无家庭态改用 HomeEmpty |
| 小程序 | FAB/底栏 | 学习/菜谱 FAB 用 wd-icon；账单 `.hai-bottom-bar`；存储 fab 修复 CSS |
| 小程序 | 详情样板 | recipe/detail 分区卡片 + 衬线标题 + 线图标操作；plan/learn 标题衬线 |
| 管理端 | 可读名 | recipe/learn/family/fileList/quota 接入 `useUserLabel`；筛选改 Select |
| 管理端 | 状态色 | `homeaiStatusColors.ts`；plan 优先级 / recipe 难度 / quota 进度 |
| 管理端 | Drawer | BillDrawer 分类下拉；RecipeDrawer 40% + 食材/步骤 a-card |

**无新增 SQL。** 下一波（Wave 3）：导航统一、触控热区、B 端主色决策、微交互。

#### 第 19 轮：UI 复审 Wave 3（导航 / 触控 / 微交互 · 2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 小程序 | 存储双标题 | 去掉「我的资料库」，改为搜索条入口（系统导航栏保留「资料存储」） |
| 小程序 | 触控热区 | 首页计划入口 / 快捷图标加大；家庭移除按钮 ≥64rpx；计划日历格 `min-height:72rpx` |
| 小程序 | 按压反馈 | 全局 `.hai-press`；首页快捷入口 / AI 对话行启用 |
| 小程序 | AI 左滑 | 对话列表左滑露出「重命名 / 删除」（替代长按菜单） |
| 管理端 | 主色决策 | **仅语义同源**：chrome 保持 Ant `#1890ff`；写入 `homeai-theme.less` 注释 |
| 管理端 | fileList | 工具栏改 `.homeai-toolbar`；仅「新增文件夹」primary，「上传」default |

**无新增 SQL。** UI 三波已完成；后续按业务需求增量打磨。

#### 第 20 轮：P1 业务体验收尾（昵称 / 完成率月筛选 / 冷启动 · 2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 小程序 | 资料上传者昵称 | `useMemberLabel`（缓存 `/family/members`）；文件夹/文件「他人」改为昵称 |
| 管理端 | 计划完成率月份 | `planList` 增加月选择器，传 `yearMonth`；卡片标题随月份变化 |
| 小程序 | 首页冷启动 | 首次 `onShow` 前展示 `HomeSkeleton`，家庭+今日计划就绪后再渲染 |

**无新增 SQL。** 依赖既有 `/family/members` 与 `GET /homeai/plan/admin/completion?yearMonth=`。

#### 第 21 轮：资料缩略图 + 列表分页（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 图片缩略图 POC | 上传 jpg/png/gif/bmp 时生成 ≤200px JPEG，写入 `thumbnailUrl`；`applyAccessUrl` 同步解析 |
| 后端 | 文件列表分页 | `/files/root`、`/folders/{id}/files` 支持 `pageNo/pageSize`（未传仍返回全量 List） |
| 小程序 | 列表分页 UI | `useStorageBrowser` pageSize=20 + `onReachBottom` /「加载更多」 |
| 小程序 | 缩略图展示 | 有 `thumbnailUrl` 显示 `<image>`，否则 `HomeFileIcon` |
| 管理端 | 预览列 + 分页 | `fileList` 文件夹内表增加预览列；表格分页 20 条 |
| 计划 | 实例清理 | 验收：`PlanInstanceCleanupScheduler` + `instance-cleanup-days` **此前已实现** |
| 微信 | 上架清单 | `.env.production` / `manifest` 注释强化；域名与 `urlCheck` / 隐私合规仍须上架前手工切换 |

**无新增 SQL**（`thumbnail_url` 字段已在 init 表）。PDF 首帧缩略图、Excel 子表导入延后。

#### 第 22 轮：资料回收站 + PDF 缩略图 + 审计扩展（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 文件回收站 | `recycleBin` / `restore` / `deletePermanently`；软删保留家庭关联；彻底删除清对象+关联 |
| 管理端 | 回收站 Tab | `fileList.vue`「文件管理 / 回收站」；恢复与彻底删除 |
| 后端 | PDF 首帧 | 引入 PDFBox 2.0.31；上传 pdf 时渲染首页 → `thumbnailUrl` |
| 审计 | 独立权限 | `homeai:audit:list`；SQL `alter_homeai_menus_iteration22.sql` |
| 审计 | 埋点扩展 | storage 移入/恢复/彻底删；recipe/learn 彻底删；`auditLog.vue` 筛选项补齐 |
| 跳过 | Excel 子表 / 文件夹回收 / 微信上架改域名 | 仍按第 23 轮与上架 checklist |

**需执行 SQL：** `alter_homeai_menus_iteration22.sql`（刷新菜单缓存并给角色授权）。

#### 第 23 轮：文件夹回收站 + 存储配额 + 计划菜谱联动（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 文件夹回收站 | `deleted_at`；软删保留家庭关联；`recycleBin?type=folder`；恢复/彻底删除级联子树 |
| 管理端 | 回收站文件夹 Tab | `fileList.vue` 回收站分「文件 / 文件夹」 |
| 后端 | 用户空间配额 | Redis 配置 `homeai:config:storage`（默认 1GB）；上传前校验；`/stats` 附带 limit/告警 |
| 管理端 | 配额展示 | 空间统计卡片显示默认配额与 Top5 进度条 |
| 计划 | recipeId | `homeai_plan_master.recipe_id`；`fillMasterInfo` 透传 `recipeName` |
| 管理端 | PlanDrawer | 「关联菜谱」下拉 |
| 小程序 | 今日下厨 | 首页读取今日带 `recipeId` 的计划，跳转菜谱详情 |
| 跳过 | Excel 子表 / 家庭维度配额表 / 微信上架改域名 | 仍按第 24 轮与上架 checklist |

**需执行 SQL：**
- `alter_homeai_storage_folder_recycle.sql`
- `alter_homeai_plan_recipe_iteration23.sql`

#### 第 24 轮：菜谱 Excel 子表 + 小程序回收站 + UniApp CI（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 菜谱 Excel 子表 | 导入解析「食材\|数量\|单位;…」与步骤文本；导出回填子表文本；`@Excel` 列标注 |
| 文档 | 导入说明 | 更新 `docs/guide/recipe-excel-import.md` |
| 后端 | 用户侧回收站 | `/storage/my/recycleBin|restore|deletePermanently`（仅本人） |
| 小程序 | 回收站页 | `storage/recycle.vue` + 资料首页入口 |
| CI | UniApp | `.github/workflows/uniapp-mp-weixin.yml`（nightly + PR 路径触发 + artifact） |
| 跳过 | 家庭配额表 / 微信正式域名上架 | 本地仍临时 127.0.0.1；上架 checklist 手工切换 |

**无新增 SQL。**

#### 第 25 轮：AI 配额统一预检 + 小程序上传统一（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 场景常量 | `HomeaiAiQuotaScene`（chat / storage:ai_generate / recipe:ai_generate） |
| 后端 | 统一预检 | `IHomeaiAiQuotaPrecheckService` + `GET /homeai/ai/quota/precheck` |
| 后端 | 挂点 | Chat 发送/配额、Office 生成、`HomeaiLlmServiceImpl` 防御性 `assertAllowed` |
| 小程序 | 配额 API | `aiApi.quotaPrecheck`；对话页按文本预检 |
| 小程序 | 上传统一 | `useHomeaiFilePick`（白名单 + chooseImage/MessageFile）；chat / recipe/add / learn/add |
| 跳过 | 家庭维度配额表 / 微信正式域名上架 | 仍按上架 checklist 手工切换 |

**无新增 SQL。**

#### 第 26 轮：菜谱热门 + 计划/学习综合统计（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 浏览计数 | 小程序详情可见后原子 `view_count+1` |
| 后端 | 热门排行 | `GET /homeai/recipe/hot`（可见性过滤 + view_count 降序） |
| 小程序 | 热门 Tab | 菜谱列表「热门」；详情展示浏览次数 |
| 后端 | 综合统计 | `GET /homeai/dashboard/plan-learn` 聚合计划完成率 + 学习 KPI/趋势/分类 |
| 管理端 | 综合统计页 | `dashboard/crossStats.vue` + 菜单 `homeai:dashboard:view` |
| 跳过 | 家庭配额表 / 微信正式域名上架 | 继续本地联调配置 |

**需执行 SQL：**
- `alter_homeai_menus_iteration26.sql`

#### 第 27 轮：账单导入 + 资料上传统一选文件（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 小程序 | `useHomeaiFilePick` 增强 | `pickMediaImages` / `pickVideo` / `showStoragePickMenu` |
| 小程序 | 账单导入 | `bill/import.vue` 走 `pickFiles` + 扩展名白名单收窄 |
| 小程序 | 资料上传 | `showStorageUploadMenu` 改为统一选文件入口 |
| 小程序 | 通用上传 | `useHomeUpload.chooseAndUpload` 同步接入 |
| 跳过 | 家庭配额表 / 微信正式域名上架 | 继续本地联调配置 |

**无新增 SQL。**

#### 第 28 轮：家庭存储配额 + 菜谱轻量推荐（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 家庭配额 | Redis 扩展 `defaultFamilyLimitBytes`（默认 5GB）；上传叠加家庭校验 |
| 后端 | 用量/统计 | `sumUsedBytesByFamily`；`/storage/stats` 增加 `perFamily` |
| 管理端 | 统计卡片 | fileList 展示家庭默认配额 + 家庭 Top5 |
| 后端 | 推荐 API | `GET /homeai/recipe/recommend`（今日计划/收藏/家庭收藏/季节加权热门） |
| 小程序 | 推荐区 | 菜谱首页横向「为你推荐」 |
| 跳过 | 微信正式域名上架 | 继续本地联调 |

**无新增 SQL**（家庭配额走 Redis，与用户配额一致）。

#### 第 29 轮：学习多维图表 + 学习提醒（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 多维统计 | `learnRecord`：7/30/90 日切换；按分类 / 按用户排行；趋势图 |
| 后端 | 统计 API | `/learn/admin/stats`、`/category`、`/user` 支持 `days`/`userId` |
| 后端 | 每日目标 | `GET/PUT /homeai/learn/goal`；Redis `homeai:learn:goal:{userId}` |
| 后端 | 提醒调度 | `LearnRemindScheduler` 每日 20:00；订阅消息 stub |
| 配置 | 模板 ID | `homeai.wechat.learn-remind-template-id` + `/config/wechat-public` |
| 小程序 | 目标 UI | 学习首页今日进度条；设置目标时拉起订阅授权 |
| 跳过 | 微信正式域名上架 | 继续本地联调 |

**无新增 SQL。**

#### 第 30 轮：家庭配额覆盖 + 新菜尝鲜（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 家庭级覆盖 | Redis `homeai:config:storage:family:{id}`；`getFamilyLimitBytes` |
| 后端 | API | `GET/PUT/DELETE /homeai/config/storage/family/{familyId}` |
| 后端 | 上传/统计 | 上传与 `perFamily` 使用覆盖配额；`customLimit` 标记 |
| 管理端 | 设置入口 | 家庭列表「存储配额」；资料 Top5 家庭点击设置 |
| 后端 | 新菜 API | `GET /homeai/recipe/new`（近 N 日优先，不足回退全局最新） |
| 小程序 | 独立区块 | 菜谱首页「新菜尝鲜」横向区（与「为你推荐」并列） |
| 跳过 | 微信正式域名上架 | 按产品选择暂不处理 |

**无新增 SQL**（家庭覆盖走 Redis，不建独立配额表）。

#### 第 31 轮：学习提醒模板对齐 + 统计导出（2026-08-12）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 模板字段 | `thing1/number2/number3/time4` 可配置；按字段类型截断 |
| 后端 | 联调 API | `GET /homeai/config/wechat-learn-remind` 返回字段映射与样例 data |
| 配置 | YAML | `application-dev.yml` 增加 `homeai.wechat.learn-remind-*-field` |
| 后端 | 统计导出 | `GET /homeai/learn/admin/stats/export`（汇总/分类/用户/趋势 四表） |
| 管理端 | 导出按钮 | `learnRecord`「导出统计」；打开页时控制台打印模板元数据 |
| 跳过 | 微信正式域名上架 | 暂不处理 |

**无新增 SQL。**

#### 第 32 轮：做过次数加权 + 家庭配额运营看板（2026-08-13）

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 做过次数 | 完成计划实例 JOIN 主计划 `recipe_id` 计数；推荐分 +1.5/次 |
| 后端 | 推荐补位 | 收藏之后按做过次数插入，`reason=cooked`，返回 `cookCount` |
| 小程序 | 标签 | 「做过多次」 |
| 后端 | 配额看板 | `GET /homeai/config/storage/families` 全量列表 + 告警/自定义筛选 |
| 后端 | 批量调整 | `PUT /homeai/config/storage/families/batch`（设覆盖 / 恢复默认） |
| 管理端 | 看板页 | `storage/familyQuota.vue`；文件管理 Top5「配额看板」入口 |
| 跳过 | 微信正式域名上架 | 暂不处理 |

**需执行 SQL：** `alter_homeai_menus_iteration32.sql`

#### 第 24 轮补强：全量缺口对齐（2026-08-13）

原第 24 轮（菜谱 Excel 子表 / 小程序回收站 / UniApp CI）已落地。本轮为调研后的对齐补洞，不覆盖原摘要。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 拦截器 | `/homeai/bill/admin`、`/homeai/plan/admin/repeat`、`/homeai/recipe/exportTemplate` 须控制台 JWT |
| 后端 | init SQL | `homeai_plan_master` / `homeai_plan_instance` 与实体对齐（仅影响新库） |
| 后端 | 模拟推送 | 未配微信模板时 `return false`，避免 Redis dedupe 跳过真发 |
| 后端 | 菜谱模板 | `GET /homeai/recipe/exportTemplate`，列与 `@Excel` 一致 |
| 后端 | 计划列表 | 批量填充 `recipeName` |
| 管理端 | 下载模板 | 菜谱「下载模板」改为 xlsx，不再生成 CSV |
| 管理端 | 学习记录 | 顶部用户筛选同步明细表 |
| 管理端 | 计划 | 列表列 + 日历日详情展示关联菜谱名 |
| 小程序 | 格式转换 | `office-convert.vue` 补 `onLoad` 导入 |
| 小程序 | 计划 | 新建/详情支持 `recipeId`，可跳菜谱详情 |
| 小程序 | 路由 | `pages.json` 去掉 `StorageBrowser` 误注册 |
| 小程序 | 推荐 | 卡片展示「做过 N 次」 |
| 跳过 | 微信上架 / 家庭配额建表 / 实例表 recipe_id 迁移 / 全模块 v-auth | 明确不做 |

**无新增 SQL（现网库已执行过计划表 alter）。**

---

### 第 33 轮：双端上传体验统一（2026-08-13）

> 新增/编辑弹窗的图片/视频/文件上传统一为「选择/拖拽 + 进度 + 大预览 + 更换/删除」。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 管理端 | 通用上传组件 | image/video/file 三模式；a-upload 拖拽 + 进度条 + 大预览 + 更换/删除；`isReturnResponse` 正确读取 `result` | `views/homeai/components/HomeaiMediaUpload.vue` |
| 管理端 | 菜谱弹窗 | 封面/做菜视频/步骤图接入组件，移除旧隐藏 file input | `RecipeDrawer.vue` |
| 管理端 | 学习资料弹窗 | 文件上传接入组件；`link` 类型直接填写 URL | `LearnDrawer.vue` |
| 小程序 | 通用上传组件 | tap 选择 + `onProgressUpdate` 进度 + 预览 + 更换/删除；复用文件白名单 | `pages-homeai/components/HomeMediaUpload.vue` |
| 小程序 | 菜谱新增/编辑 | 封面/步骤图/视频接入组件 | `recipe/add.vue` |
| 小程序 | 学习资料新增 | 文件改走 `/homeai/learn/upload` 预上传，保存时以 `fileUrl` 落库 | `learn/add.vue` |

**无新增 SQL。**

### 第 34 轮：全项目巡检 + P0 安全/正确性修复（2026-08-13）

> Agent 三端巡检（后端 / 管理端 / 小程序）输出问题清单，落地全部 P0 项。

**鉴权与上传安全**

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 学习预上传 401 回归 | `/homeai/learn/upload` 移出 `ADMIN_PREFIXES`（小程序 + 管理端均可用） | `HomeaiAuthInterceptor` |
| 后端 | 菜谱上传加固 | 扩展名白名单 + 大小限制（图 10MB / 视频 200MB）+ 魔数校验；objectKey 拒绝 `..` 段（纵深防路径穿越） | `RecipeServiceImpl`、`HomeaiFileStorageServiceImpl` |
| 后端 | 孤儿文件 | `uploadVideo/uploadCover` 先校验菜谱存在，避免上传成功但 DB 未落 | `RecipeServiceImpl` |
| 后端 | IDOR 越权 | office `tasks/{id}`、recipe `{id}/video` / `{id}/cover`、learn `materials/{id}/upload` 补登录 + 归属校验 | `StorageOfficeController`、`RecipeController`、`LearnController` |

**管理端上传返回 BUG（根因修复）**

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 管理端 | `Axios.uploadFile` 根因 | 失败不再误报成功；无回调时返回完整响应体（`isReturnResponse` 读 `result`） | `Axios.ts` |
| 管理端 | 账单导入预览 | `billImport` 修复：文件真正上传 + 读 `result`（原「解析预览」功能整体不可用） | `billImport.vue` |
| 管理端 | 模板上传 | `officeTemplate` 修复：模板 URL 落库；失败不再提示"新增成功" | `officeTemplate.vue` |
| 管理端 | 文件上传 | `fileList` 失败不再 unhandled rejection | `fileList.vue` |

**无新增 SQL。**

### 第 35 轮：时区 / 登录态 / 事务 / 性能 / 安全加固（2026-08-13）

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 小程序 | UTC 时区 | 新增 `utils/date.ts`（`localDateStr`/`localMonthStr`），替换 8 处 `toISOString().substring`（修复账单统计「下月」按钮失效等） | `bill/statistics|add|edit`、`plan/index|add|detail`、`profile` |
| 小程序 | 401 登录态残留 | `request.ts` 401 改 `useUserStore().logout()`，同步清 storage + Pinia store | `request.ts` |
| 后端 | 收藏并发计数 | `toggleFavorite` 计数改 SQL 原子递增/递减 + `@Transactional` | `RecipeServiceImpl` |
| 后端 | 多表写入事务 | storage 建文件夹/删除/上传/改可见性、文件夹级联删除、family 成员/新增/彻底删、user 设置家庭 补 `@Transactional` | `StorageController`、`StorageFolderServiceImpl`、`FamilyController`、`WxUserController` |
| 后端 | 配额性能 | `sumUsedBytesByUser/Family` 改 SQL `COALESCE(SUM(file_size),0)` 聚合 | `StorageFileServiceImpl` |
| 后端 | 文件夹树 | 文件数 N+1 → 单条 `GROUP BY` 批量统计 | `StorageFolderServiceImpl` |
| 后端 | 安全加固 | JWT 默认密钥启动告警；非 dev/test 环境未配微信配置拒绝 mock 登录 | `HomeaiJwtUtil`、`WxUserServiceImpl` |

**无新增 SQL。**

### 第 36 轮：体验与死代码清理（2026-08-13）

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 审计日志 | detail JSON 超 8000 字符截断为合法 JSON | `HomeaiAuditLogServiceImpl` |
| 后端 | XFF 伪造 | `clientIp` 改取最右侧代理追加 IP + 格式校验 | `StorageController` |
| 后端 | AI 配额预检 | 新增 `POST /homeai/ai/quota/precheck`（text 走请求体） | `HomeaiAiQuotaController` |
| 后端 | 账单 | 新增 `GET /homeai/bill/entry/{id}` | `BillController` |
| 小程序 | chat 预检 | 改 POST，长文本不再进 URL | `chat.vue` |
| 小程序 | bill 编辑 | 改按 id 传参，不再整条 JSON 塞 URL | `bill/index|edit.vue`、`bill.ts` |
| 小程序 | 图片懒加载 | recipe/storage/family/chat 列表 `<image lazy-load>` | 多页 |
| 小程序 | storage 双请求 | 子组件 `onMounted` 重复 refresh 移除 | `StorageBrowser.vue` |
| 双端 | 死代码清理 | 删除 `useHomeUpload.ts`、`api/ai.ts`、`HomeNetworkBar.vue`、`useFileFolderTree.ts`；`storageVisibility` 死导出；`StorageBrowser` 误注册页面移除 | — |

**无新增 SQL。**

### 第 37 轮：菜谱导入支持封面图片（2026-08-13）

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 模板/导入封面列 | `coverUrl` 增加 `@Excel("封面图片地址")`，导出模板与导入自动识别该列（支持 http/https、/upload、data:image） | `Recipe.java`、`RecipeController` |
| 后端 | 批量导入封面入口 | `POST /recipe/import-covers`：多文件上传，按文件名（去扩展名）匹配菜谱名称批量写入封面，返回成功/未匹配报表；登记管理端白名单 | `RecipeController`、`HomeaiAuthInterceptor` |
| 管理端 | 批量导入 UI | 菜谱页「批量导入封面」按钮 + 弹窗（多选图片、缩略预览、开始导入、结果报表） | `recipeList.vue` |
| 文档 | 导入说明 | 补充封面地址列与批量封面导入用法 | `docs/guide/recipe-excel-import.md` |

**无新增 SQL。**

### 第 38 轮：P0 加固 + P1 体验收尾 + 工程化收敛（2026-08-13）

> 五视角审查后的集中落地：P0 安全/正确性 + P1 体验/健壮性 + 工程化（API 收敛/CRUD 去重/类型化）。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 双通道 userId 不一致 | 新增 `canModifyRecipe`：创建者本人 **或家庭共享菜谱的家庭成员** 可编辑/删除/上传媒体，解决 console 建的家庭菜谱（userId 为空）小程序端不可维护 | `IRecipeService`、`RecipeServiceImpl`、`RecipeController`（mini edit/delete、checkRecipeOwner） |
| 后端 | 回归测试 | 新增 `RecipeVisibilityTest`（6 用例：可见性/可修改性授权，覆盖 console 建菜谱场景） | `src/test/java/.../RecipeVisibilityTest.java` |
| 小程序 | 生产构建守卫 | `vite.config.ts` 生产构建检测 `VITE_SERVER_BASEURL` / `WEIXIN_RELEASE` 仍为本机地址或非 HTTPS 直接中止构建 | `vite.config.ts` |
| 小程序 | AI 图片失败静默丢图 | chat 图片上传失败：toast 提示 + 回滚临时消息与输入 + 中止发送，不再"少图"静默发出 | `chat.vue` |
| 管理端 | 无效导出按钮 | keyConfig「导出」移除（无后端接口；导出 API 密钥有安全风险） | `keyConfig.vue` |
| 管理端 | 难度档位统一 | RecipeDrawer 难度改为 1-5 档（与列表一致），修复编辑难度 2/4 时无匹配项 | `RecipeDrawer.vue` |
| 小程序 | 重发误删消息 | `resendLastMessage` 仅当最后一条为流式占位时才回滚消息，避免误删已完成 AI 回复 | `chat.vue` |
| 小程序 | H5 文本预览兼容 | `preview.vue` 的 `getFileSystemManager` 不存在时优雅降级提示 | `preview.vue` |
| 管理端 | loadData 兜底 | `planConfig` / `fileWhitelist` 加载补 try/catch，消除 unhandled rejection | `planConfig.vue`、`fileWhitelist.vue` |
| 管理端 | 账单统计体验 | `billStatistics` 补 loading 状态 + 错误提示 + 移入 `onMounted` | `billStatistics.vue` |
| 管理端 | 批量删除兜底 | `conversationList` 批量删除失败计数，部分失败给出明确提示 | `conversationList.vue` |
| 管理端 | API 层死代码收敛 | 移除 `api/homeai` 17 个无引用导出（`chatApi`、`storageApi.folderTree/createFolder/deleteFolder/checkGenerateQuota`、`conversationApi.getById/rename`、`quotaApi.logList`、`configApi.getWechatPublic`、`userApi.getById`、`recipeApi.uploadVideo/deleteVideo/favorites/toggleFavorite/hot/recommend/newest`），逐一核验无调用方 | `api/homeai/index.ts` |
| 小程序 | 附件上传空 URL | chat 附件上传响应缺 URL 时不再 push 脏数据，给出失败提示 | `chat.vue` |
| 小程序 | 批量上传闪烁 | `useStorageBrowser` 多文件上传改为批量计数，最后一个完成后统一 refresh | `useStorageBrowser.ts` |
| 小程序 | 首页统计缓存 | profile 三统计接口抽 `loadStats()` + 30s TTL，避免频繁切 Tab 重复拉取 | `profile.vue` |
| 管理端 | API 层收敛 | 补齐 `storageOfficeApi`/`storageTemplateApi`/`storageRuleApi` 封装 + `storageApi` 补全（folderTree/folderFiles/createFolder/updateFolder/deleteFolder/stats）+ `billApi.adminStats`；`fileList`/`officeTemplate`/`officeHistory`/`convertRule`/`ConvertRuleDrawer`/`KeyConfigDrawer`/`billStatistics` 7 页裸调 defHttp 全部收敛到 api 层（保留 multipart uploadFile 调用） | `api/homeai/index.ts`、7 个页面 |
| 管理端 | 回收站 CRUD 去重 | 新增 `useHomeaiRecycleBin` hook（移入/恢复/彻底删除单条+批量 + rowSelection，可配 entityName/nameField/permanentWarn/confirmWithName）；6 个主列表页（user/family/bill/plan/recipe/learn）复用，每页净减约 60 行 | `hooks/useHomeaiRecycleBin.ts`、6 个列表页 |
| 小程序 | 首页 onShow 缓存 | learn 统计/目标 30s TTL（列表/进行中会话保持实时）；recipe 推荐/新菜 60s TTL（主列表实时），减少频繁返回首页重复请求 | `learn/index.vue`、`recipe/index.vue` |
| 管理端 | 账单搜索表单 | billList 分类/用户筛选由纯 Input 改为 Select（与 recipe/learn/plan 一致），分类选项经 `billApi.categoryList` 加载 | `billList.vue` |
| 管理端 | CSV 模板去重 | 新增 `utils/csvTemplate.ts`（BOM+Blob 下载），billList/planList/learnList 三处模板下载复用 | `utils/csvTemplate.ts`、3 个列表页 |
| 管理端 | 文件大小格式化去重 | fileList 删除重复的 `formatFileSize`（与 `formatSize` 仅 units 差异），统一用 `formatSize` | `fileList.vue` |
| 管理端 | 类型化·契约层 | `types.ts` 修正错位：`HomeaiFileWhitelistItem` 对齐后端（extension/category/sortOrder/isEnabled）、`HomeaiPlanConfig` 对齐 Redis DTO（repeatHorizonDays/instanceCleanupDays/remindEnabled/aiDocPolishEnabled）；新增 `HomeaiPageResult<T>`、`HomeaiConvertRule`、`HomeaiOfficeTemplate`、`HomeaiConvertTask`、`HomeaiAuditLog` | `api/homeai/types.ts` |
| 管理端 | 类型化·API 层 | `api/homeai/index.ts` 16 处返回类型补全（各模块 list → `HomeaiPageResult<T> \| T[]`、`recipeApi.getById`、`getFileWhitelist`→`{items}`、`getPlanConfig`→`HomeaiPlanConfig`、`auditApi.logs` 等） | `api/homeai/index.ts` |
| 管理端 | 类型化·视图层（recipe 模块） | 新增 `HomeaiRecipeStep`/`HomeaiRecipeDetail` 类型并修正 `getById` 返回（详情映射而非单实体）；`recipeList` 的 `api` 参数/`getTableAction`/分类列表/封面导入结果类型化；`RecipeDrawer` 的 `useDrawerInner` 参数与详情加载类型化 | `types.ts`、`index.ts`、`recipeList.vue`、`RecipeDrawer.vue` |
| 管理端 | 类型化·视图层（bill 模块） | `billList`/`BillDrawer` 分类加载与 `api` 参数/`getTableAction` 类型化；`billImport` 新增 `BillImportRow` 行类型（含 duplicate 标记）；`billApi.adminStats` 补返回类型并同步 `billStatistics` | `billList.vue`、`BillDrawer.vue`、`billImport.vue`、`billStatistics.vue`、`index.ts` |
| 管理端 | 类型化·视图层（learn/storage/plan/ai/family/user） | learn：`learnList`/`LearnDrawer`/`learnRecord`（新增 LearnCategoryStat/LearnUserStat 行类型）；storage：`fileList`（HomeaiStorageFile/Folder）、`fileWhitelist`（HomeaiFileWhitelistItem）、`convertRule`/`ConvertRuleDrawer`、`officeHistory`（HomeaiConvertTask）、`officeTemplate`（HomeaiOfficeTemplate）、`familyQuota`（新增 `HomeaiFamilyQuotaItem` + API 返回类型）；plan：`PlanDrawer`/`auditLog`/`planConfig`；ai：`conversationList`（新增 `HomeaiConversation`）、`quota`（新增 `HomeaiQuotaRecord`，清理未用 resetFields）；family/user：`api` 参数与 `getTableAction` 类型化 | 13 个视图文件、`types.ts`、`index.ts` |
| 清理 | 调试残留与 BOM | 移除 learnRecord `showLearnRemindMeta` 调试代码（console.info + 无 api 封装请求）；去除 storage/index.vue、files.vue 的 UTF-8 BOM（消除 eslint unicode-bom） | `learnRecord.vue`、`storage/index.vue`、`storage/files.vue` |
| 小程序 | 健壮性收尾 | family 成员加载 / storage 搜索 / chat 消息加载补 try-catch（消除 unhandled rejection）；office-convert 提交失败由静默吞错改为 toast 提示 | `family.vue`、`search.vue`、`chat.vue`、`office-convert.vue` |
| 双端 | 难度语义统一 | 小程序菜谱新增改为 5 档（入门/简单/中等/较难/困难），detail/index 的 diffLabel 统一为规范映射 {1:入门,2:简单,3:中等,4:较难,5:困难}，与后端/管理端完全一致 | `recipe/add.vue`、`recipe/detail.vue`、`recipe/index.vue` |
| 后端 | 双套接口返回统一 | 管理端新增/编辑统一返回实体：`POST /recipe/add`、`PUT /recipe/{id}`、`POST /bill/add`、`PUT /bill/{id}` 由返回字符串改为返回保存后的对象（与小程序端 `POST /recipe`、`PUT /recipe`、`/bill/entry` 一致）；已验证消费方（RecipeDrawer/BillDrawer）仅 await 不使用返回值 | `RecipeController`、`BillController` |

**无新增 SQL。**

> 说明：`/homeai/**` 在 Shiro 链为 anon（双 token 架构所限，改 authc 会破坏小程序登录），PUBLIC_PATHS 仅 3 个白名单路径、MVC 拦截器覆盖全部 /homeai 请求，风险评估为**可接受**；如需纵深建议在 Nginx 层做路径级 ACL（见第 40 轮建议）。小程序端暂未引入 jest/vitest 测试基建，时区工具单测待后续补充。

---

### 第 39 轮：Android 迁移启动（手机号+密码登录）（2026-08-17）

> 背景：微信小程序备案受阻，改为 **Android APP 双端并行**（保留 MP-WEIXIN 小程序通道）。本轮为迁移启动：扩展账号体系支持手机号+密码登录，为 APP 端提供登录入口与文件选择能力。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 账号字段扩展 | `WxUser` 新增 `password` / `salt` / `login_type`（`wechat` / `phone`）三列 | `WxUser` 实体 |
| 后端 | 注册 / 登录 | `HomeaiAuthController` 新增 `register` / `login` / `password`（设置/修改密码） | `HomeaiAuthController` |
| 后端 | JWT userId 双解析 | `getUserId` / `getWxUser` 兼容旧 openid token，新 token 以 `userId` claim 为准 | `HomeaiJwtUtil` |
| 后端 | 拦截器登记 | 注册/登录等公共路径登记进 `PUBLIC_PATHS` | `HomeaiAuthInterceptor` |
| 后端 | 手机号唯一性 | 注册与换绑时校验手机号唯一 | 注册/登录 Service |
| 客户端 | 登录 API 封装 | `platform/auth.ts`（手机号+密码登录） | `JeecgUniapp/src/platform/` |
| 客户端 | APP 文件选择 | `platform/filePicker.ts`（APP 端文件选择，替代小程序 `chooseMessageFile`） | `JeecgUniapp/src/platform/` |
| 客户端 | 登录页 | `pages/auth/login.vue` | `JeecgUniapp/pages/auth/login.vue` |
| 客户端 | 路由拦截 | 拦截器检测 APP 端未登录跳转登录页 | `JeecgUniapp/src/interceptors/` |
| 客户端 | 内测配置 | `.env.production` 指向内测服务器 IP（侧载 APK 联调用） | `JeecgUniapp/env/.env.production` |
| 客户端 | manifest 权限精简 | 移除小程序专属权限，适配 APP 包 | `JeecgUniapp/manifest.config.ts` |

**发布策略：** 先侧载 APK 内测（手机号+密码登录），待小程序备案完成后正式上架。

**需执行 SQL：** `alter_homeai_wx_user_android_login.sql`（`homeai_wx_user` 新增 password/salt/login_type 三列，已登记至迁移清单）

**状态：** 已落地。

---

### 第 40 轮：Android 迁移第二轮（权限适配 + platform 层 + 隐私合规 + 签名 APK + 本地通知兜底）（2026-08-18）

> 背景：Android APP 手机号用户无微信 openid，微信订阅消息通道不可用。原计划接入阿里云 EMAS 移动推送，但 EMAS 插件（id=7628/7629）已停止维护、无 Vue3 兼容性声明、官方替代仅支持 uni-app x，故弃用，改为 `plus.push.createMessage` 延迟本地通知兜底。本轮同时补齐下载/相册权限适配、platform 层、隐私合规与签名 APK。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 客户端 | 下载/相册权限适配 | `fileDownload.ts` 增加 APP-PLUS 分支（权限申请 + 保存到相册/文件），manifest 声明相册/存储权限 | `JeecgUniapp/src/pages-homeai/utils/fileDownload.ts`、`manifest.config.ts` |
| 客户端 | platform 层补齐 | 新增 `platform/download.ts`（下载/相册保存）、`platform/env.ts`（环境地址）；业务层 `fileDownload.ts` / `request.ts` / `useHomeaiFilePick.ts` 改调 platform/* | `JeecgUniapp/src/platform/` |
| 客户端 | 隐私弹窗合规 | 隐私政策/用户协议内容页 + 三处入口（登录页/个人中心/设置页）+ `App.vue` 首启弹窗 + manifest 开启 `__usePrivacyCheck__` | `JeecgUniapp/pages/privacy/`、`App.vue`、`manifest.config.ts` |
| 客户端 | 签名 APK | `keytool` 生成 keystore + 签名文档 `docs/guide/android-signing.md` + `.gitignore` 排除 keystore | `JeecgUniapp/manifest.config.ts`、`docs/guide/android-signing.md` |
| 客户端 | 推送改本地通知兜底 | 新增 `platform/push.ts`：依据计划实例 `startTime` / `remindMinutes` 计算提醒时刻，`plus.push.createMessage` 创建延迟本地通知（仅 App 存活/后台生效，进程被杀无法送达） | `JeecgUniapp/src/platform/push.ts` |
| 后端 | PlanInstance 补字段 | `PlanInstance` 新增 `startTime` / `remindMinutes` 两个 `@TableField(exist=false)` 冗余展示字段，`fillMasterInfo` 从主计划拷贝，支撑客户端计算提醒时间 | `PlanInstance` 实体、`PlanServiceImpl` |

**EMAS 弃用原因：** 插件（id=7628/7629）停止维护、无 Vue3 兼容性声明、官方替代仅支持 uni-app x；不接入 UniPush/EMAS 厂商推送（后续如需离线推送再评估 uni-app x 或原生厂商通道）。

**无新增 SQL**（`PlanInstance` 两个字段均为 `@TableField(exist=false)` 冗余展示字段，无 DDL；原 EMAS 方案 `alter_homeai_wx_user_emas_push.sql` 不再需要执行）。

**状态：** 已落地。

---

### 第 41～42 轮：ComfyUI 本地路线（已拆分独立文档）

> ComfyUI 本地部署与照片精修能力建设（第 41 轮：双模型验证；第 42 轮：常用模型 + 启停脚本 + 人脸修复插件）已拆分至独立目录：`ComfyUI/`（路线图 `ComfyUI/docs/comfyui-roadmap.md`）。

**状态：** 已落地。

---

### 第 44 轮：后端纯逻辑单测扩展（2026-08-18）

> 沿 `RecipeVisibilityTest` 模式为 homeai 模块纯逻辑类补齐 JUnit 5 单元测试（无 Spring 上下文、无 Mockito），覆盖上传魔数校验、文件名清洗、重复计划日期匹配、AI Token 估算、菜谱可见性/Excel 解析等核心纯逻辑。

| 测试类 | 覆盖内容 | 用例数 |
|--------|----------|--------|
| `HomeaiFileMagicUtilTest` | 魔数判定（jpg/png/gif/pdf/zip/OLE/mp4/未知扩展名）+ validate 上传校验（空参/MZ 可执行拦截/内容不匹配） | 16 |
| `StorageFileNameUtilTest` | 对象路径拼接、原始文件名清洗截断（路径穿越/非法字符/控制字符/超长保留扩展名）、微信临时名判定、扩展名提取 | 14 |
| `PlanRepeatUtilTest` | 重复计划规则判定 + none/daily/weekly/monthly/未知规则日期匹配 | 9 |
| `HomeaiAiQuotaEstimateUtilTest` | 输入/输出 token 估算公式（含 trim、整数除法、500 下限） | 8 |
| `RecipeServiceImplPureLogicTest` | applyFamilyOnSave / applyAdminVisibilityOnSave / parseIngredientsFromExcel / parseStepsFromExcel / RecipeVisibility.isValid | 20 |
| `HomeaiFileUrlUtilTest` | 绝对/相对地址转换、危险扩展名黑名单（大小写不敏感） | 10 |

**验证：** 临时翻转父 pom `skipTests` 后 `mvn test -pl jeecg-boot-module/jeecg-boot-module-homeai` 全量 **91 用例通过（新增 77 + 既有 14 无回归）**，跑完已还原 pom。

**无新增 SQL。**

**状态：** 已落地。

---

### 第 45 轮：Android 未登录引导 + 注册入口（2026-08-20）

> 真机测试：个人中心无注册按钮；点注册提示失败；未登录进 APP 提示「登录失败」而非进入登录页。根因是个人中心仍走微信 `uni.login`，且路由白名单未放行 `/pages/auth/login`，`uni-mini-router` 会把登录页拦回个人中心。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 客户端 | 个人中心登录/注册 | APP 显示登录+注册，跳转 `/pages/auth/login`（`?mode=register`）；不再调用微信登录 | `pages/homeai/profile.vue` |
| 客户端 | 未登录引导 | 启动/Tab/401/退出登录均 `reLaunch` 到手机号登录页，不 toast「登录失败」 | `App.vue`、`homeaiAuth.ts`、`request.ts`、`router/index.ts` |
| 客户端 | 路由白名单 | APP 放行登录页、用户协议、隐私政策；拦截后 `navType=replaceAll`（登录页非 Tab） | `router/index.ts` |
| 客户端 | 注册校验 | 密码至少 6 位；注册失败不再误报「登录失败」 | `pages/auth/login.vue` |
| 客户端 | 网络权限 | 自定义 permissions 补回 `INTERNET`；内测允许 HTTP 明文（`usesCleartextTraffic`） | `manifest.config.ts` |
| 后端 | 手机号注册 | 占位 `openid=phone_{手机号}`，避免旧库 NOT NULL/UNIQUE 插入失败；响应剔除 password/salt | `HomeaiAuthController` |
| 后端 | Token 校验 | APP JWT 优先按 `userId` 对 Redis `homeai_token:` 校验 | `HomeaiAuthInterceptor` |

**无新增 SQL。** 若注册仍报字段错误，请确认已执行 `alter_homeai_wx_user_android_login.sql` 与 `alter_homeai_wx_user_openid_nullable.sql`。

**状态：** 已落地。

---

### 第 46 轮：Android 账号闭环 + 启动/回跳/资料编辑（2026-08-20）

> 走查结论：登录进功能已通，卡住的是账号闭环（忘密/改密/改资料）和新用户无建家引导；启动会先闪首页再踢登录。本轮按优先级落地。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 客户端 | 启动中转 | `pages/launch/index` 为 APP 首页：未登录 `reLaunch` 登录页，已登录进首页；首启隐私不同意则退出 | `pages/launch/index.vue`、`pages.json` |
| 客户端 | 注册建家 | 注册成功可选创建/加入家庭，家庭 Tab 自动打开对应弹窗 | `login.vue`、`family.vue`、`homeaiAuth.ts` |
| 客户端 | 改密/资料 | 个人中心进入修改密码、编辑昵称/头像；去掉「微信用户」兜底 | `change-password.vue`、`profile-edit.vue`、`profile.vue` |
| 客户端 | 401 回跳 | 登录成功回到原 Tab/功能页；登录页显示密码、注册确认密码、须勾选协议 | `request.ts`、`login.vue` |
| 客户端 | Android 体验 | 首页家庭入口未登录进登录页；APP 不预留微信胶囊；学习提醒改为本地通知文案；账单可切月份 | `index.vue`、`learn/index.vue`、`bill/index.vue` |
| 后端 | 改密 | `POST /homeai/auth/change-password`（需登录，不进 PUBLIC_PATHS） | `HomeaiAuthController` |
| 后端 | 资料 | `GET/PUT /homeai/user/info` 按 JWT `userId` 解析并剔除密码盐；`POST /homeai/user/info/avatar` 白名单+魔数 | `WxUserController` |

**无新增 SQL。** 需重启后端；启动页变更后请重新编译/安装 APP（自定义基座若未含 launch 页也需重打）。

**状态：** 已落地。

### 第 47 轮：手机号 JWT 身份解析统一 + 退出作废 token（2026-08-20）

> 走查结论：个人中心/改密已通，家庭/账单/计划等仍 toast「未登录」（HTTP 200）。根因是手机号登录 `HomeaiJwtUtil.sign(user.getId(), secret, APP)` 把 userId 与 openid claim 都写成主键 UUID，库内 `openid` 是 `phone_手机号`；拦截器按 userId 放行 Redis，业务控制器却 `getOpenid + getByOpenid` 查不到人。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 身份解析 | 新增 `HomeaiSecurityUtil.getWxUserId`；`getCurrentUserId` 控制台优先再回退 APP；家庭/账单/对话用 `getWxUserId`，菜谱/计划/学习/资料用 `getCurrentUserId` | `HomeaiSecurityUtil`、`FamilyController`、`BillController`、`HomeaiChatController`、`RecipeController`、`PlanController`、`LearnController`、`StorageController`、`StorageOfficeController` |
| 后端 | 退出作废 | `POST /homeai/auth/logout`（需登录，不进 PUBLIC_PATHS）；同时删 `homeai_token/refresh` 的 userId 与 openid 两把 Redis 钥匙 | `HomeaiAuthController`、`invalidateWxUserTokens` |
| 客户端 | 退出登录 | 先打 `/auth/logout` 再清本地；401 只清本地避免循环 | `platform/auth.ts`、`stores/user.ts`、`profile.vue`、`request.ts` |
| 测试 | 纯逻辑 | 手机号占位 openid ≠ 主键；签发后两 claim 均为 userId；解析顺序 getById 再 getByOpenid | `HomeaiJwtIdentityTest` |

**无新增 SQL。** 需重启后端；退出登录改动后请重新编译/安装 APP。

**补记（同日）：** 真机/侧载 APP 不能把 API 写成 `localhost`/`127.0.0.1`（那是手机自己）。开发环境增加 `VITE_SERVER_BASEURL_APP`（当前电脑 WLAN `192.168.222.157`），启动页会探测模拟器 `10.0.2.2` / `adb reverse` 的 `127.0.0.1`。已有库若缺列：须执行 `alter_homeai_wx_user_android_login.sql`、`alter_homeai_storage_folder_recycle.sql`（或一次性 `alter_homeai_deleted_at_schema_gap.sql`）。

**状态：** 已落地。

### 第 48 轮：APP 表单选择器体验 + 学习记录 study_date（2026-08-20）

> 计划添加页选项挤成多张小卡片、原生 `<picker>` 仍是微信滚轮；结束学习 insert 因旧库 `study_date` NOT NULL 无默认值失败；家庭邀请码 `maxlength="6"` 触发 wot Number 类型告警。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| APP | 分组选择 | 计划/学习/账单添加页改为单卡片 + 行分割；底部列表选择替代原生 picker | `HomePickerCell`、`HomeDateCell`、`plan/add`、`learn/add`、`bill/add`、`bill/edit`、`recipe/add` |
| APP | 操作按钮 | 胶囊芯片、wot 按钮；邀请码 `:maxlength="6"` | `family.vue`、`learn/detail.vue`、`recipe/add.vue` |
| 后端 | 学习记录 | 实体写入 `studyDate`；旧库列改为可空并回填 | `LearnRecord`、`LearnServiceImpl`、`alter_homeai_learn_record_study_date.sql` |

**有 SQL：** 已有库执行 `alter_homeai_learn_record_study_date.sql`。需重启后端；APP 选择器改动后请重新编译/运行自定义基座。

**状态：** 已落地。

### 第 49 轮：越权修补 + 页面守卫（2026-08-20）

> 四视角审查：账单删除无所有权；存储上传可挂到他人文件夹；解散后邀请码仍可用；改密不作废 token；学习资料列表全量可见；菜谱详情缺 id、金额校验不严。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 账单删除 | APP 须登录且只能删自己的；管理端回收站 `userId=null` 仍可软删 | `BillEntryAccess`、`BillController.delete`、`BillEntryServiceImpl.softDelete` |
| 后端 | 存储上传 | 指定 `folderId` 须存在且 `canWriteFolder`（管理端跳过） | `StorageController.uploadFile` |
| 后端 | 家庭邀请 | 仅管理员生成码；解散作废未使用码；加入校验未解散；`tryOccupy` 原子占用 | `FamilyController`、`FamilyServiceImpl`、`FamilyInviteCodeServiceImpl` |
| 后端 | 改密 / 学习 | 改密后 `invalidateWxUserTokens`；APP `GET /learn/materials` 按 `user_id` 过滤 | `HomeaiAuthController`、`LearnController` |
| 后端 | 菜谱详情 | 返回 `canModify`，对齐 `canModifyRecipe` | `RecipeServiceImpl.getDetailWithRelations` |
| APP | 页面守卫 | 菜谱缺 id 拦截；资料加载失败不 autoStart；账单金额须有限且 >0；改密后清会话回登录 | `recipe/detail`、`learn/detail`、`bill/add`、`bill/edit`、`change-password` |
| 测试 | 纯逻辑 | 账单删除所有权三用例 | `BillEntryAccessTest` |

**无新增 SQL。** 需重启后端；改密/邀请/学习列表改动后请重新编译/安装 APP。

**状态：** 已落地。

### 第 50 轮：计划提醒 + 学习先预览 + UI 收口（2026-08-20）

> 审查剩余 P1：计划无开始时间导致本地通知被跳过；学习点列表即计时；20:00 文案无实现；计时条挡内容；家庭仍用微信弹层；菜谱表单散装、列表一次拉全量；日历全表扫描。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| APP | 计划提醒 | 非全天可选开始时间+提前提醒；全天隐藏提醒；创建当天立刻调度本地通知 | `HomeTimeCell`、`plan/add.vue`、`push.ts` |
| APP | 学习 | 列表进详情后手动开始；未达标调度当晚 20:00 本地通知；目标用 wot 底栏 | `learn/index.vue`、`scheduleLearnGoalRemind` |
| APP | 计时条 | 学习中页面加底留白，避免挡住视频/文档 | `learn/detail.vue` `.hai-page--timer` |
| APP | 家庭弹层 | 移除/退出用 message-box；改名/解散用 popup；转让用 action-sheet | `family.vue` |
| APP | 菜谱 | 基本信息改分组卡片；全部 tab 分页触底加载 | `recipe/add.vue`、`recipe/index.vue` |
| 后端 | 学习日历 | 按 `study_date` 月份区间查库，不再全表内存过滤 | `LearnServiceImpl.getLearnCalendarDates` |

**无新增 SQL。** 学习日历改动需重启后端；计划/学习/家庭/菜谱改动后请重新编译/安装 APP。进程被杀时本地通知仍可能收不到。

**状态：** 已落地。

### 第 51 轮：计划改删 + 资料弹层 + 账单底栏 + 启动协议（2026-08-20）

> 审查剩余体验 P1：计划详情无 content、不能改删；资料仍用微信原生弹层；账单操作条在文档流底部；启动页隐私弹窗无法打开协议全文。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 计划 APP 改删 | `GET/PUT/DELETE /plan/instance/{id}` 校验 master 归属；软删主计划；`fillMasterInfo` 补 content | `PlanController`、`PlanServiceImpl`、`PlanInstance.content` |
| APP | 计划详情 | 展示内容/开始时间/提醒；编辑复用 add 页；删除需确认（重复计划提示整条消失） | `plan/detail.vue`、`plan/add.vue`、`plan.ts` |
| APP | 资料弹层 | 新建/重命名/删除/管理改为 wot popup 与 action-sheet | `useStorageBrowser.ts`、`StorageBrowser.vue`、`storage/search.vue` |
| APP | 账单底栏 | `.hai-bottom-bar` 改为 fixed，列表加底留白 | `homeai-theme.scss`、`bill/index.vue` |
| APP | 启动协议 | 自定义弹窗可点开《用户协议》《隐私政策》 | `pages/launch/index.vue` |

**无新增 SQL。** 计划改删需重启后端；APP 需重新编译/安装。删除计划是软删整条主计划（含重复实例），编辑不改日期与重复规则。

**状态：** 已落地。

### 第 52 轮：后台家庭删除后仍出现在列表（2026-08-20）

> 管理端家庭点「移入回收站」后记录仍在列表，状态仍显示「正常」。根因是 `homeai_family.del_flag` 与业务 `status`（normal/disbanded）语义不同：删除只置 `del_flag=1`，实体未加 `@TableLogic`，列表/导出也未排除已删除行。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 逻辑删除 | `Family.delFlag` 补 `@TableLogic`；列表/导出加 `del_flag=0` | `Family.java`、`FamilyController` |
| 后端 | 关联查询 | 存储可分配家庭/配额看板去掉手动 `eq(DelFlag)`，避免 MP 3.5 逻辑删除字段入条件 | `StorageController`、`StorageFileServiceImpl` |
| 管理端 | 详情状态 | 抽屉「状态」改读 `status`，不再把 `delFlag` 当成已解散 | `FamilyDrawer.vue` |

**状态：** 已落地。

### 第 53 轮：邀请分享 + 内测 API + 单测与 P2 抛光（2026-08-20）

> 审查剩余：邀请只能复制；内测换网段无改 API 入口；homeai 单测默认不跑；表单按钮、账单箭头、学习链接、协议文案不一致；计划 byDate N+1。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| APP | 邀请 | 生成后可选复制或系统分享 | `family.vue` `shareWithSystem` |
| APP | 内测 API | 个人中心「服务器地址」写入 `homeai_api_base` | `profile.vue`、`platform/env.ts` |
| APP | UI/文案 | 保存按钮 round；统计箭头与列表一致；APP 打开链接走浏览器；菜单「隐私政策」；协议去掉微信账号 | add 页、`bill/statistics`、`learn/detail`、`agreement` |
| APP | 忘记密码 | 登录页提示联系管理员后台重置（无短信） | `login.vue` |
| 后端 | 计划列表 | `fillMasterInfo` 批量查 master/recipe | `PlanServiceImpl` |
| 测试 | homeai | 模块 `skipTests=false`；结束学习 `study_date`/`duration` 纯逻辑单测 | `jeecg-boot-module-homeai/pom.xml`、`LearnRecordAssemblerTest` |

**无新增 SQL。** 计划列表与结束学习改动需重启后端；APP 需重新编译/安装。全仓其它模块仍 skipTests。只跑 homeai：`mvn test -pl jeecg-boot-module/jeecg-boot-module-homeai`（surefire 已设 UTF-8，避免中文密码用例乱码）。

**状态：** 已落地。

### 第 54 轮：Android 本地签名 APK 打包（2026-08-21）

> HBuilderX 不再支持本机编 APK。`pnpm build:app-android` 只出资源包。本轮落地 CLI 资源 + DCloud 离线 SDK + Gradle 的可重复出包流程。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| APP | 模块/权限 | `modules` 补 Gallery + Push；`POST_NOTIFICATIONS`；Android 13+ 运行时申请通知权限 | `manifest.config.ts`、`push.ts` |
| 脚本 | 初始化 | overlay：`dcloud_control.xml`、明文 HTTP、签名 gradle、Manifest 权限/AppKey | `scripts/android-offline/init-offline-project.ps1` |
| 脚本 | 出包 | `pnpm pack:apk` → `dist/apk/homeai-release.apk`，可选 `-Upload` | `pack-apk.ps1`、`android-pack.local.json.example` |
| 文档 | 指南 | 版本对齐、AppKey、模块裁剪、发版与排障 | `docs/guide/android-local-apk.md` |

**无新增 SQL。** 离线 SDK / keystore / AppKey 在开发机，不入库。第一次需 Android Studio + 对齐版本的离线 SDK，见指南。

**状态：** 脚本与清单已落地；本机装 SDK / 申请 AppKey / 打出第一包需在开发机执行。

### 第 55 轮：后台新增用户漏写 salt，App 登录 NPE（2026-08-21）

> 管理端新增/导入 `homeai_wx_user` 未写 `salt`/`password`。App 密码登录调用 `PasswordUtil.encrypt(..., user.getSalt())`，盐为空即 NPE。控制台 `SysUser` 新增已正确加盐，问题仅在 HomeAI。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 新增/导入 | 默认密码 123456 + per-user 盐；手机号校验与占位 openid | `WxUserServiceImpl.prepareAdminCreatedUser`、`WxUserController.add/importExcel` |
| 后端 | 登录 | salt/密码为空返回明确错误；禁用账号拒绝登录 | `HomeaiAuthController.loginByPassword` |
| 后端 | 重置密码 | `PUT /homeai/user/{id}/resetPassword`，作废 Redis token | `WxUserController.resetPassword` |
| 管理端 | 提示/操作 | 新增提示默认密码；列表「重置密码」；补 `handleSuccess` 刷新 | `UserDrawer.vue`、`user/index.vue` |
| 测试 | 纯逻辑 | 默认密码可校验；缺盐不得加密 | `HomeaiAuthServiceTest` |

**无新增 SQL。** 已用后台加过、尚不能登录的用户：在用户列表点「重置密码」，再用 `123456` 登录。需重启后端；管理端刷新即可。

**状态：** 已落地。

### 第 56 轮：云打包缺 Push 模块导致启动红屏（2026-08-21）

> 云打包未打入原生 Push 时，启动即访问 `plus.push` 会抛「缺少push模块」。`typeof plus.push` 同样会抛，必须 try-catch。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 缺模块不崩 | `push.ts` `getPush()` 兜底；`App.vue` 初始化包 try |
| 清单 | 云打包带上 aps | `modules.Push` + `distribute.sdkConfigs.push`（不要 UniPush） |
| 文档 | 排障 | `android-local-apk.md` 红屏一行 |

**无新增 SQL。** 已装的包仍缺原生模块，需按新清单 **重新云打包并勾选 Push** 后重装；代码兜底后至少能进应用。

**状态：** 已落地。

### 第 57 轮：长久本机出包改为 H5 + Capacitor（2026-08-21）

> 云打包继续撑内测。长久路径脱离 DCloud 原生 SDK：uni-app 编 H5，Capacitor 官方 Android + Gradle 签名（同一 keystore 可覆盖安装）。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 方案 | 目标架构 | 现云打包 / 后 `pnpm pack:apk:local` | `docs/plan/android-capacitor-local-pack.md` |
| APP | 运行时抽象 | 独立 App 判断、退出、外链 | `platform/runtime.ts`；启动页/学习链接改调 platform |
| APP | H5 能力 | 文件选择 / 下载打开走 H5，供 Capacitor WebView | `filePicker.ts`、`download.ts`、`env.ts` |
| 脚本 | 出包 | build:h5 → cap sync → assembleRelease | `scripts/capacitor/pack-apk-local.ps1` |

**无新增 SQL。** 第一次本机出包还需 `pnpm add @capacitor/*` 与 `pnpm pack:apk:local -- -InitAndroid`。

**状态：** 管道与 platform 适配已落地；`android/` 原生工程首次 `cap add` 后提交。

### 第 58 轮：本机打出 Capacitor 签名 APK（2026-08-21）

> 脱离 DCloud SDK 的长久出包已跑通：`pnpm pack:apk:local` → `JeecgUniapp/dist/apk/homeai-release.apk`（约 4.5MB）。包名 `com.homeai.app`，证书与云打包相同。

| 端 | 项 | 落地 |
|----|----|------|
| 原生 | `cap add android` | `JeecgUniapp/android/`，Gradle 8.2.1 + 阿里云 Maven |
| 脚本 | 出包 | H5 → cap sync → 本机 Gradle zip（避开 Java PKIX）→ assembleRelease |
| APP | 本地通知 | H5 壳走 `@capacitor/local-notifications`；云打包仍走 plus.push |

**无新增 SQL。** 内测可先侧载该 APK 对照云打包。发版在真机回归前可继续云打包。

**状态：** 已落地。

### 第 59 轮：Capacitor 相册/分享/返回键（2026-08-21）

> 长久壳对齐云打包常用原生能力：相册保存、文档系统分享、外链浏览器、返回键再按一次退出、状态栏不遮挡。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 相册 | `@capacitor-community/media`；H5 浏览器仍走 `<a download>` |
| APP | 文档 | Filesystem 缓存 + Share 系统面板 |
| APP | 壳 | StatusBar、`backButton`、Browser.open |
| 清单 | 权限 | READ_MEDIA_* / READ_EXTERNAL_STORAGE |

**无新增 SQL。** 请侧载新的 `dist/apk/homeai-release.apk` 验证保存到相册与返回键。

**状态：** 已落地。

### 第 60 轮：管理端菜谱封面批量导入（2026-08-21）

> 菜谱 Excel 导入不含本地图片文件。管理端「批量导入封面」支持单张/多选、选择整个菜谱文件夹，以及小体积 zip。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 入口 | 单张/多选 + 选择文件夹；列表增加封面缩略图 |
| 后端 | 匹配 | 文件名或父目录名对齐菜谱名；同菜多图择优；zip 展开（防 zip-slip） |
| 校验 | 上传 | 扩展名白名单 + 魔数；单张 ≤10MB |

**无新增 SQL。** 接口仍为 `POST /homeai/recipe/import-covers`。

**状态：** 已落地。

### 第 61 轮：菜谱导入去重 / 分类纠正 / 封面入口（2026-08-21）

> 同名 Excel 重复导入会翻倍；文件夹分类把粥/面误标成汤羹；封面按钮挤在表头不易发现。

| 端 | 项 | 落地 |
|----|----|------|
| 导入 | 同名去重 | 已存在同名菜谱则覆盖更新，不再新增 |
| 导入 | 分类 | 支持 ID 或中文名；按菜名纠正（粥/饭/面→主食，××汤→汤羹） |
| 管理端 | 封面入口 | 列表上方提示条 + 工具栏「导入封面」 |

**无新增 SQL。**

**状态：** 已落地。

### 第 63 轮：资料与学习多类型上传、预览与播放（2026-08-21）

> 资料/学习几乎只能下载或系统打开；无音频；Spring multipart 10MB 卡住视频。本轮按设计做页内预览：图片/音视频/文本直出，PDF 页内看，Office 转 PDF 后缓存再看。

| 端 | 项 | 落地 |
|----|----|------|
| 上传 | 上限 | Spring multipart 200MB；Nginx `client_max_body_size 200m`；业务分类上限（视频 200MB / 压缩包 100MB / 音频与文档 50MB / 图片 20MB / 文本 10MB） |
| 白名单 | 类型 | 补 webp、webm、mp3/wav/m4a/aac；魔数校验同步；学习 `type=audio` |
| 后端 | 预览 API | `GET/POST .../files/{id}/preview[-pdf]`、`GET/POST .../materials/{id}/preview[-pdf]`；LibreOffice 结果回写 `preview_pdf_url` |
| 管理端 | 预览弹窗 | `HomeaiFilePreviewModal`；资料列表/学习列表/抽屉可预览；按类型切换上传 mode |
| APP | 预览页 | `storage/preview` 补音频、PDF.js、Office 轮询转 PDF；打开文档走 `openLocalDocument`；学习详情页内音频；添加页补齐类型与链接 |

**迁移 SQL：**

```text
alter_homeai_file_whitelist_audio_webp.sql
alter_homeai_preview_pdf_url.sql
```

**状态：** 已落地。须重启后端；已有库执行上述 SQL；本机 Nginx/frp 需 reload 使 200m 生效。Office 预览依赖本机 LibreOffice / MS Office。

### 第 64 轮：APP 改删闭环 + 学习归属校验（2026-08-21）

> 计划/账单/资料存储第 51 轮已能改删，菜谱只能编辑、学习资料只能新增。`POST /learn/start|stop|record` 不校验归属，猜到 ID 即可刷时长。本轮补齐 APP 入口，并堵住计时越权。

| 端 | 项 | 落地 | 关键路径 |
|----|----|------|----------|
| 后端 | 学习计时归属 | start/stop/record 先校验资料存在且 APP 为创建者；Service 层再次拒绝他人/无主资料 | `LearnMaterialAccess`、`LearnController`、`LearnServiceImpl` |
| APP | 菜谱删除 | 详情 `canModify` 时显示删除；家庭共享额外提示；确认后 `DELETE /recipe/{id}` | `recipe.ts`、`recipe/detail.vue` |
| APP | 学习改删 | 详情底栏编辑/删除；`add.vue?id=` 编辑后 PUT | `learn.ts`、`learn/detail.vue`、`learn/add.vue` |
| 原生 | 相机权限 | Capacitor `AndroidManifest` 补 `CAMERA`（不接新插件） | `JeecgUniapp/android/app/src/main/AndroidManifest.xml` |
| 测试 | 纯逻辑 | 本人可用、他人/无主/资料不存在拒绝 | `LearnMaterialAccessTest` |

**无新增 SQL。** 需重启后端；APP 需重新编译/安装。忘记密码短信、控制台预览鉴权收紧未纳入本轮。

**状态：** 已落地。

### 第 66 轮：管理端菜谱列表间距与封面导入弹窗（2026-08-21）

> 「未选中任何数据」与工具栏按钮重叠；封面导入弹窗选完文件后有时被点穿关闭，窗口偏小且不能拉宽拉高，导入过程看不到进度。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 列表间距 | 工具栏与选择提示条拉开距离 |
| 管理端 | 导入弹窗 | 默认约 70% 宽 / 72% 高；禁用点遮罩关闭；导入中禁止关闭；右下角拖动改宽高 |
| 管理端 | 导入进度 | 进度条 + 当前文件/上传百分比 + 每条成功/失败状态 |

**无新增 SQL。**

**状态：** 已落地。

### 第 67 轮：APP 文件上传格式支持（2026-08-21）

> Capacitor/H5 选文件时 `accept` 只有扩展名或为空，Android WebView 常变成只选图片；`type=file` 选不到音频；本地白名单缺 webp/音频，相册 blob 无扩展名会被「不支持该文件格式」拦掉。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 选择器 | `fileAccept.ts`：MIME+扩展名；`type=all` 显式 `*/*`；音频走 `audio/*`；APP-PLUS 音频改 `type=all` |
| APP | 白名单 | 离线默认补 webp/webm/mp3/wav/m4a/aac；无扩展名时用 MIME/场景兜底 |
| APP | 权限 | Capacitor / 云打包补 `READ_MEDIA_AUDIO` |
| 后端 | init | `init_homeai_tables.sql` 默认白名单与第 63 轮 alter 对齐（已有库仍跑 alter） |

**无新增 SQL**（已有库继续用 `alter_homeai_file_whitelist_audio_webp.sql`）。需重装/侧载 APK。

**状态：** 已落地。

### 第 68 轮：OSS 封面文件名丢失扩展名（2026-08-21）

> 私有上传把完整 objectKey 丢进 `StrAttackFilter`，`.` 和 `_` 被清掉，OSS 上变成 `1787294773016d0a054c2png`。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | objectKey | 只过滤目录段，文件名保留扩展名与下划线（与官方 `upload(file, fileDir)` 一致） |

**无新增 SQL。** 须重启后端。已上传的无后缀对象不会自动改名，需重新导入封面。

**状态：** 已落地。

### 第 69 轮：APP 系统控制更新（覆盖安装 + 热更新）（2026-08-21）

> 版本号存库，管理端指定更新方式；APP 启动发现本地 `versionCode` **小于** 服务端才更新。不用 Jeecg 自带 `/sys/version`（Redis + classpath JSON，要控制台 JWT）。

| 端 | 项 | 落地 |
|----|----|------|
| 库 | `homeai_app_version` | 单行 `id=current`；种子 `1.0.0` / `100`，`enabled=0` |
| 后端 | 公开 GET | `GET /homeai/app/version` 加入 `PUBLIC_PATHS` |
| 后端 | 管理端 | `GET/PUT /homeai/app/version/admin`、`POST .../upload`（apk/zip 魔数 + 大小；APK≤200MB，zip≤80MB） |
| 管理端 | APP版本 | 方式（apk/resource）、强制、上传 APK / H5 zip |
| APP | 启动页 | 隐私同意后探测；可选「稍后」；强制不可跳过 |
| Native | Capacitor | `HomeaiUpdate` 插件下载/校验/解压（防 zip-slip）/安装 APK；`REQUEST_INSTALL_PACKAGES` |
| 出包 | H5 zip | `pack-apk-local.ps1` 额外打 `dist/apk/homeai-h5-{version}.zip`（根目录含 `index.html`） |

比较用整数 `versionCode`，热更新另存 `homeai_web_version`。壳 `versionCode` 低于 `min_shell_code` 时热更新改走 APK。覆盖安装新 APK 时 Capacitor 会清掉已持久化的 Web 根路径。发版只打 Capacitor 包；不要再走 HBuilderX 云打包。

**须执行 SQL。** 重启后端；管理端刷新菜单缓存；侧载新 APK 后启动页才会检测。

**状态：** 已落地。

### 第 70 轮：弃用云打包，发版统一本机 Capacitor（2026-08-21）

> 不再用 HBuilderX 云打包内测/发版；DCloud 离线 `pnpm pack:apk` 入口改为直接报错。唯一出包：`pnpm pack:apk:local`。

| 端 | 项 | 落地 |
|----|----|------|
| 约定 | AGENTS.md | 发版命令改为 `pack:apk:local`；云打包与离线 SDK 标已弃用 |
| 文档 | 发布/出包/FRP | 去掉「现在用云打包」；热更新只针对 Capacitor 壳 |
| 脚本 | `pack-apk.ps1` | 调用即抛错，提示改用 `pack:apk:local` |

已装的旧云打包 APK 仍可被同一证书覆盖安装；`plus.*` 分支仅作旧壳兼容，不要再出那种包。

**无新增 SQL。**

**状态：** 已落地。

### 第 71 轮：本机出包同步 HA 品牌资源（2026-08-21）

> Capacitor `android/` 仍是 `cap add` 默认图标/启动图/Material 色。打包时从 `src/static/app/icons/1024x1024.png` 生成自适应图标、启动图、通知小白标和 favicon，并补齐 minSdk / 权限 / 主题色。

| 端 | 项 | 落地 |
|----|----|------|
| 脚本 | 品牌资源 | `sync-android-branding.ps1`：mipmap 自适应图标 + splash + `ic_stat_homeai` + favicon |
| 出包 | 接入 | `pack-apk-local.ps1` 在 H5 前写 favicon；`patch-android.ps1` 写 Android 资源 |
| 清单 | 对齐 uni-app | minSdk 26；`ACCESS_WIFI_STATE` / `ACCESS_NETWORK_STATE` / `WAKE_LOCK`；相机 autofocus |
| 壳 | 观感 | WebView `#F3F2EE`；通知小图标与主色 `#1B4F8A` |

**无新增 SQL。** 需重新 `pnpm pack:apk:local` 后覆盖安装，桌面图标才会变。

**状态：** 已落地。

### 第 72 轮：栈底系统返回退出 APP（2026-08-21）

> 真机侧载后，系统返回键 / 全面屏返回手势在无可返回时不退出。Capacitor 挂了 `backButton` 监听后不再走系统默认 finish；原先「再按一次退出」在 Tab/启动页栈上等于没退出，且 `navigateBack` 可能闪回启动页再被 `switchTab` 拉回首页。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 返回键 | 有业务页栈则 `navigateBack`；Tab / 登录 / 启动页直接 `App.exitApp()` |

**无新增 SQL。** 请重新 `pnpm pack:apk:local` 后覆盖安装验证。

**状态：** 已落地。

### 第 73 轮：管理端菜谱封面导入入口与选文件（2026-08-21）

> 菜谱列表有两个「导入封面」按钮；弹窗里「选择图片 / 选择文件夹」点击无反应。根因是第 66 轮把 `.scroll-container > div` 设成 `height:100%`，把横向滚动条轨道拉成整层遮罩，挡住了按钮。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 入口 | 只留工具栏「导入封面」；提示条改为说明；Excel 单独成按钮 |
| 管理端 | 选文件 | 原生 `input[type=file]` 触发选择；滚动条样式不再盖住内容 |

**无新增 SQL。** 刷新管理端菜谱列表即可验证。

**状态：** 已落地。

### 第 74 轮：本机 Gradle 不再直连 Google Maven（2026-08-21）

> `pnpm pack:apk:local` 在 `assembleRelease` 配置 `:capacitor-android` 时失败：子工程 `buildscript` 直连 `dl.google.com`，本机 JDK TLS 握手被断开。根 `allprojects` 阿里云镜像覆盖不到插件 classpath。

| 端 | 项 | 落地 |
|----|----|------|
| 出包 | 镜像 | `init-aliyun.gradle` + `patch-android.ps1` 给 Capacitor 子工程 `buildscript` 插入阿里云 |

**无新增 SQL。** 重新 `pnpm pack:apk:local`（H5 已编过可用 `-- -SkipBuild`）。

**状态：** 已落地。

### 第 75 轮：前端页面功能检查与修复（2026-08-21）

> 现行发版是 H5 + Capacitor，不是 `APP-PLUS`。鉴权/个人中心/401/路由白名单仍用编译宏，侧载后退出登录、Token 过期、点「登录」会走微信小程序分支，回不到手机号登录页；API 地址覆盖也不生效。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 鉴权 | `usesPhoneLogin()`（非微信小程序）替代 `#ifdef APP-PLUS`：登录/退出/401/路由白名单 |
| APP | 个人中心 | 注册、改密、服务器地址对 Capacitor 可见；登录进手机号页 |
| APP | 其它 | 账单编辑切换收支重置分类；学习切换类型清空文件；首页今日计划走 `localDateStr` |
| 管理端 | 学习 | 新增必填归属用户；非 link/note 必须上传文件；切换类型清空文件 |
| 管理端 | 家庭下拉 | 排除已解散/回收站；资料「家庭可见」未选家庭不可提交 |

**无新增 SQL。** APP 鉴权改动需重新 `pnpm pack:apk:local`（或热更新 zip）后验证：退出登录应回到手机号登录页。

**状态：** 已落地。

### 第 76 轮：菜谱封面 OSS 上传 Connection reset（2026-08-21）

> 管理端上传封面时 Aliyun OSS 报 `SocketException / Connection reset by peer`，`RequestId: Unknown`（请求未到达 OSS 业务层）。根因是私有上传未带 Content-Length（Multipart 流走 chunked 易被 RST），且每次上传先 `doesBucketExist`；空闲连接池也会被对端掐掉。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | OSS 客户端 | HTTPS、超时/重试、空闲连接 30s；连接重置时 `resetClient` |
| 后端 | 私有上传 | 带 Content-Length/Content-Type；去掉每次探测 Bucket；瞬时失败重试 3 次 |
| 管理端 | 封面导入 | 单张失败不中断整批；失败自动再试一次 |

**无新增 SQL。** 需**重启后端**后再试封面上传。

**状态：** 已落地。

### 第 77 轮：一键发布 / 停止脚本（2026-08-21）

> 日常发版要分别跑 APP 出包、管理端 `build:docker:prod`、后端重启，停服务时 `stop-local.ps1` 又不动 Java。需要仓库总入口。

| 入口 | 作用 |
|------|------|
| `docs/deploy/publish-all.ps1` | 后端编译并重启 → 管理端构建发布到本机 Nginx → `pack:apk:local`。可跳过任一段；可选 `-UploadApk` |
| `docs/deploy/start-all.ps1` | 只启动前后端（新窗口 `mvn spring-boot:run` + nginx/frpc） |
| `docs/deploy/stop-all.ps1` | 停止 JeecgBoot（:8080）与本机 Nginx/frpc。不停 MySQL/Redis |

复用现有管道，不另造出包流程。双击同目录 `.cmd` 亦可。

**无新增 SQL。**

**状态：** 已落地。

### 第 78 轮：一键脚本支持分别发布/停止（2026-08-21）

> 发布后 `:8080` 未拉起导致 Nginx 反代 502；且不能单独发/停某一端。

| 项 | 落地 |
|----|------|
| 目标开关 | `-Backend` / `-Frontend` / `-App` 可组合；双击 `.cmd` 出菜单 |
| 后端启动 | `-am spring-boot:run` 写 `C:\homeai\logs\backend.log`；端口未起来视为失败 |
| 停止 | 可只停 Java 或只停 Nginx/frpc；APP 无本机常驻进程 |

**无新增 SQL。**

**状态：** 已落地。

### 第 79 轮：一键停止 .cmd 乱码与 taskkill 误报（2026-08-22）

> 双击 `stop-all.cmd` 出现 `'"'` / `'eAI'` / `'cho'` / `'/p'` 不是内部命令；随后 `taskkill` 报找不到已退出的 Java PID。

| 项 | 落地 |
|----|------|
| `.cmd` | 改为仅 ASCII 启动器，菜单改到 PowerShell `-Interactive`（cmd 按 GBK 解析 UTF-8 中文会拆碎 `echo`/`set /p`） |
| 停止 | `taskkill /T` 先杀掉整棵树后跳过已退出进程；经 cmd 吞掉「没有找到进程」，避免 `$ErrorActionPreference=Stop` 升成异常 |

**无新增 SQL。**

**状态：** 已落地。

### 第 80 轮：APP 文档原文件打开、下载修复、图片压缩（2026-08-22）

> 文档预览仍走 Office 转 PDF；Capacitor WebView `uni.downloadFile` 被 OSS CORS 拦导致下载失败；列表封面拉原图费流量。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 文档 | `storage/preview` 不再轮询转 PDF；Office / 其它文档用系统应用打开原文件 |
| APP | 下载 | Capacitor `Filesystem.downloadFile` 原生拉取；`access-url` / 学习 preview 刷新原图地址 |
| 后端 | 展示图 | 预签名带 `x-oss-process`（列表 480 / 详情 1080）；下载接口仍签原图。GIF 不处理 |

**无新增 SQL。** 须重启后端；APP 需重新 `pnpm pack:apk:local`（或热更新 zip）。

**状态：** 已落地。

### 第 81 轮：APP 下载/打开文档改走原生 ACTION_VIEW（2026-08-22）

> 第 80 轮仍失败：`Share.share` 不是系统查看；WebView/`Filesystem` 拉 OSS 预签名仍容易失败。

| 端 | 项 | 落地 |
|----|----|------|
| 原生 | 打开 | `HomeaiUpdate.openFile`：`FileProvider` + `ACTION_VIEW` + 选择器 |
| 原生 | 下载 | 复用 `HomeaiUpdate.download`，可带 `X-Access-Token` |
| 后端 | 原文件 | `GET /homeai/storage/files/{id}/content`、`GET /homeai/learn/materials/{id}/content` |
| APP | 入口 | 有文件 id 时走鉴权流；PDF/文本页内预览也走原生下载 |

**无新增 SQL。** 须重启后端，并 **重打 APK**（`pnpm pack:apk:local`）；仅热更新 H5 不够，因为新增了原生方法。

**状态：** 已落地。

### 第 82 轮：资料下载三端对齐与体验精简（2026-08-22）

> 第 81 轮补上原生打开后，content URL / 拒绝响应 / 按钮文案仍重复；管理端还在 `window.open` 预签名，失败无反馈。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 耦合 | `contentUrl.ts` / `accessToken.ts` 统一鉴权地址与 Token；全平台 `downloadToTemp` 带 Header |
| APP | 使用者 | 文档操作改称「打开文件」；失败提示区分登录过期 / 无权 / 不存在 |
| 管理端 | 管理者 | 列表与预览弹窗走 `/content` 带 JWT 下载；学习资料补「下载原文件」 |
| 后端 | 精简 | `HomeaiHttpDeny` 统一 401/403/404 |

**无新增 SQL。** 须重启后端。APP 文案/失败提示可热更新；原生打开仍依赖第 81 轮 APK。

**状态：** 已落地。

### 第 83 轮：账单/计划/学习可操作度与菜谱 API 收敛（2026-08-22）

> 资料下载对齐后，账单导入 URL 过长、计划失败永远「加载中」、学习结束误触、菜谱绕过 API 封装。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 账单 | 导入预览改 `storage`；列表区分加载失败与「本月暂无」 |
| APP | 计划 | 详情失败展示空状态+返回，不再卡在加载中 |
| APP | 学习 | 结束需确认；记录页 loading/失败；`/learn/records?yearMonth=` |
| APP | 菜谱 | 详情/新增/分类统一走 `recipeApi` |
| 管理端 | 菜谱 | 模板下载改 `handleExportXls`，有失败提示 |
| APP | 我的 | 统计失败可点重试，不再静默显示 0 |

**无新增 SQL。** 须重启后端（学习按月查询）。APP 体验可热更新。

**状态：** 已落地。

### 第 84 轮：家庭角色可信度与 AI 配额可见性（2026-08-22）

> 管理员判断依赖成员列表、转让无确认、对话页不显示配额、空会话无引导；管理端配额进度条用默认限额、升管理员无确认。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 家庭 | `isAdmin` 改用 `/family/info` 的 `myRole`；转让管理员二次确认 |
| APP | 家庭 | 邀请码复制/加入/分享补充「24 小时内有效，使用后失效」 |
| APP | AI | 对话页 `onShow`/`blur` 预检配额；空会话示例话术只填入不发送 |
| APP | AI | 对话列表区分加载失败与空态；创建失败 toast |
| 管理端 | 配额 | 进度条分母优先 `record.dailyLimit` / `monthlyLimit` |
| 管理端 | 家庭 | 升为管理员先确认；用户/家庭导入模板改 `handleExportXls` |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 85 轮：列表失败不再当空 + Office 入口补齐（2026-08-22）

> 菜谱/计划/资料/学习/回收站/搜索失败仍显示「暂无」；Office 转换/生成页面已注册但无入口。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 菜谱/计划 | 列表失败可重试；计划勾选失败 toast |
| APP | 资料 | 浏览器/回收站/搜索区分失败与空态 |
| APP | 学习 | 资料列表首屏 skeleton + 失败重试 |
| APP | Office | 资料顶栏「AI生成」「处理记录」；Office 文件「格式转换」；提交后进历史 |
| APP | Office | 处理历史失败可重试；已完成记录可点打开 |
| 管理端 | Office | 下载结果改 `downloadByUrl`，失败有提示 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 86 轮：详情/编辑失败态与首页计划反馈（2026-08-22）

> 详情与编辑页失败仍显示空壳或空表单；首页计划失败被当成「暂无待办」；改密不能显密。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 详情 | 菜谱/学习/资料预览失败可重试，隐藏空操作栏 |
| APP | 编辑 | 账单/计划/菜谱加载失败挡表单，避免误保存 |
| APP | 首页 | 今日计划失败显示「点此重试」，不改写成 0 项 |
| APP | 账号 | 微信登录按钮显示「登录中…」；改密可显示/隐藏密码 |
| APP | 次要页 | 账单统计、菜谱分类失败与空态分开 |

**无新增 SQL。** 无后端 Java 变更。APP 可热更新。

**状态：** 已落地。

### 第 87 轮：管理端看板失败可见 + 表单反馈补齐（2026-08-22）

> 跨模块看板失败被当成零数据；学习 Drawer 下载仍走 OSS 裸链；学习编辑/记账/家庭成员失败无挡板。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 看板 | `crossStats` / 学习统计失败保留旧数据并提示，不再归零 |
| 管理端 | 上传 | `HomeaiMediaUpload` 编辑态走 `/content` 鉴权下载 |
| 管理端 | Drawer | 账单/计划/学习分类加载失败 warning |
| APP | 学习 | 编辑资料失败挡表单 |
| APP | 账单 | 记一笔防连点；分类失败可重试 |
| APP | 家庭 | 成员列表失败可重试 |
| APP | 设置 | 服务器地址保存带 loading |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 88 轮：连点防护 + 返回刷新 + 默认家庭共享（2026-08-22）

> 计划/对话连点会重复提交；个人中心与学习统计 TTL 导致刚结束学习或改目标后数字陈旧；管理端新建菜谱默认公开。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 计划 | 新增/编辑防连点；详情标记完成防连点 |
| APP | 对话 | 创建会话期间禁用 FAB |
| APP | 个人中心 | 去掉统计 30s TTL，每次进入刷新 |
| APP | 学习 | 结束学习 / 改目标后强制刷新统计 |
| APP | 菜谱 | 分类页返回后刷新列表 |
| 管理端 | 菜谱 | 新建默认可见性改为家庭共享 |
| 管理端 | 配额 | 看板失败保留旧数据；单户恢复默认需确认 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 89 轮：家庭/导入/Office 防连点 + 菜谱返回刷新（2026-08-22）

> 创建家庭、导入账单、Office 转换/生成连点会出双份；菜谱编辑后返回详情仍是旧内容；新菜 60s TTL 看不到刚保存的菜。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 家庭 | 创建/加入/改名/解散防连点 |
| APP | 账单 | 导入确认防连点 |
| APP | Office | 格式转换、AI 生成防连点 |
| APP | 表单 | 菜谱/学习/账单编辑、资料保存补齐连点守卫 |
| APP | 菜谱 | 详情返回静默刷新；新菜列表每次进入刷新 |
| APP | 计划 | 列表勾选完成防连点；详情删除防连点 |
| 管理端 | 家庭 | 添加成员时用户列表失败给出提示 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 90 轮：回收站/学习计时/存储弹窗防连点（2026-08-22）

> 回收站恢复、账单解析、学习开始/结束连点会重复提交；资料新建/删除弹窗可连点出双份；管理端回收站失败被清空。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 回收站 | 恢复 / 彻底删除防连点 |
| APP | 账单 | 导入解析防连点 |
| APP | 家庭 | 生成邀请码防连点 |
| APP | 学习 | 开始 / 结束计时防连点 |
| APP | 资料 | 新建文件夹、重命名、删除防连点；搜索空关键词提示 |
| 管理端 | 资料 | 回收站失败保留旧数据并提示 |
| 管理端 | 列表 | 学习 / 账单分类加载失败 warning |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 91 轮：失败当空白 + 模板/分类错位（2026-08-22）

> 格式转换失败显示成「没有规则」；AI 生成切 Word/Excel 不换模板；分类接口挂掉时编辑计划整页中断；账单统计刷新失败被清空。

| 端 | 项 | 落地 |
|----|----|------|
| APP | Office | 转换规则失败可重试，与「暂无规则」分开 |
| APP | Office | 切换文档类型重新拉模板；额度查询失败不再悄悄放行 |
| APP | 菜谱/计划/学习 | 分类加载失败可点重试；计划分类失败不再阻断编辑 |
| APP | 账单 | 统计刷新失败保留旧数据 |
| 管理端 | 资料 | 文件夹树 / 家庭下拉失败提示，不再静默清空 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 92 轮：无家庭默认共享 + 对话附件/失败态（2026-08-22）

> 没加入家庭时新建菜谱默认「家庭共享」，填完才报错；只附图发不出去；对话加载失败看起来像空会话；首页下厨失败被当成「没计划」。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 菜谱 | 无家庭时默认仅自己；点家庭共享会提示 |
| APP | 对话 | 只附图/附件可发送；消息失败可重试，不再当成空会话 |
| APP | 对话 | 额度查询失败保留提示 |
| APP | 首页 | 今日下厨失败显示重试 |
| APP | 学习 | 日历加载失败提示 |
| 管理端 | 用户 | 家庭下拉失败 warning |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 93 轮：处理中自动刷新 + 空导入/关联失败可见（2026-08-22）

> 转换提交后历史页一直停在「处理中」；解析出 0 条仍进入确认页；计划关联菜谱失败看起来像没有菜；资料选家庭失败被当成没家庭。

| 端 | 项 | 落地 |
|----|----|------|
| APP | Office | 等待/处理中每 3 秒刷新；失败记录点开看原因 |
| APP | 账单 | 解析结果为空不再进入确认页 |
| APP | 计划 | 关联菜谱加载失败可重试 |
| APP | 资料 | 家庭列表失败与「暂无家庭」分开 |
| APP | 对话 | 重命名空名称会提示 |
| APP | Office | 无模板时提示可不选直接生成 |
| 管理端 | Office | 处理记录每 5 秒自动刷新 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

### 第 94 轮：协议勾选记住 + 邀请码/公开确认（2026-08-22）

> 登录勾选协议下次还要再勾；邀请码带空格加不进去；过去日期计划无确认；公开菜谱无提示；管理端强制更新一键生效。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 登录 | 同意协议写入本地，下次不用再勾 |
| APP | 家庭 | 邀请码去空格并转大写，粘贴不易失败 |
| APP | 计划 | 过去日期创建需确认；标题去空格 |
| APP | 菜谱 | 设为公开需确认；烹饪时间/份数须大于 0 |
| 管理端 | 版本 | 开启强制更新保存前需确认 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

---

### 第 95 轮：菜谱搜索不被冲掉 + 学习链接校验（2026-08-22）

> 搜菜谱进详情再返回会被整表刷新冲掉；搜不到仍写「暂无菜谱」并引导添加；关于页写死 v1.0；学习链接随便填也能存。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 菜谱 | 搜索结果返回后仍按关键词重搜；无结果写「未找到」并可清空；切 Tab 清搜索 |
| APP | 菜谱 | 推荐/新菜刷新失败保留旧数据 |
| APP | 学习 | 标题去空格；外链须以 http(s) 开头 |
| APP | 个人中心 | 「关于」展示系统版本号（不再写死 v1.0） |
| APP | 资料 | 保存/打开文件防连点 |
| 管理端 | 家庭 | 家庭名称去空格，空名称不可保存 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

---

### 第 96 轮：账单月份文案 + 未来记账确认（2026-08-22）

> 翻到往月仍写「本月暂无账单」；未来日期记账无确认；金额可填三位小数；资料搜索无法清空；对话页标题一直是「AI对话」。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 账单 | 非本月显示对应年月，空态不再误写「本月」 |
| APP | 账单 | 未来日期需确认；金额最多两位小数；备注去空格 |
| APP | 资料 | 搜索关键词去空格，可清除/清空 |
| APP | 对话 | 导航栏显示会话标题；首条消息后即时更新 |
| APP | 菜谱 | 菜名/食材/步骤去空格，空白行不再当内容保存 |
| 管理端 | 学习 | 标题去空格；外链须 http(s) |
| 管理端 | 账单 | 金额校验并保留两位小数 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

---

### 第 97 轮：学习日历可点当天 + 分类空态（2026-08-22）

> 学习日历绿点只能看不能点；记录页翻月仍写「本月暂无」；分类页没菜谱不能直接添加；没分类时还写「该分类暂无菜谱」。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 学习 | 日历点日期进入当天记录；记录页可筛选/看全月 |
| APP | 学习 | 非本月空态写对应年月 |
| APP | 菜谱 | 分类为空与「该分类暂无」分开；空分类可添加并带上当前分类 |
| APP | 计划 | 标题/内容去空格后保存 |
| 管理端 | 计划 | 标题去空格，空标题不可保存 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

---

### 第 98 轮：导入切类型清文件 + 空清单/计划失败态（2026-08-22）

> 账单导入切 CSV/Excel 仍拿着旧文件去解析；菜谱没食材/步骤显示空卡片；计划网络失败被写成「已删除」；首页学习卡没有今日进度。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 账单 | 切换导入类型会清空已选文件 |
| APP | 菜谱 | 无食材/步骤写明空态，可编辑时提供「去补充」 |
| APP | 计划 | 加载失败可重试，与「不存在/已删除」分开 |
| APP | 首页 | 学习卡展示今日目标进度 |
| APP | 资料 | 格式转换页显示文件名 |
| 管理端 | 菜谱 | 菜名/食材/步骤去空格 |

**无新增 SQL。** 无后端 Java 变更。APP / 管理端可热更新。

**状态：** 已落地。

---

### 第 99 轮：审查 A — Office 读权限 + 家庭可见求交（2026-08-22）

> 格式转换/AI 生成提交不校验源文件可读；家庭可见可填任意 familyIds；对话附件上传前再拦一层白名单。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | Office | 转换/生成提交：文件须未软删；APP 须 canAccessFile；控制台可操作未删文件 |
| 后端 | 资料 | APP 家庭可见只保留自己所在家庭；无家庭不能设家庭可见 |
| APP | 对话 | 附件上传前再次 `validateUploadFile` |
| 测试 | 纯逻辑 | `StorageAccessUtilTest` 覆盖软删与 familyIds 求交 |

**无新增 SQL。** 后端需重新发布。APP 对话附件可热更新。

**状态：** 已落地。

---

### 第 100 轮：审查 B — 业务归属 / 配额原子扣减 / APP 存储用量（2026-08-22）

> 控制台 JWT 打 APP 写接口会把 `sys_user.id` 写成业务归属；Token 扣减只 incr 不回看限额；预检展示默认 10000/200000；APP 资料页看不到已用空间。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 鉴权 | 学习/菜谱/计划/配额预检只认 `getWxUserId`；资料/Office 优先 HomeAI 用户，管理端控制台仍可操作 |
| 后端 | 配额 | 去掉日限额 +1000 透支；`incr` 后比较限额，超额 `decr` 回滚；过期个性化配额回落默认；预检展示真实限额 |
| 后端 | 资料 | `GET /homeai/storage/my/usage` 返回个人/家庭已用与上限（APP 路径，不进 `ADMIN_PREFIXES`） |
| APP | 资料 | 资料首页用量条；上传失败含「存储空间」时 toast 更久 |
| 测试 | 纯逻辑 | `AiQuotaLimitUtilTest` 覆盖过期回落与 Redis Long 计数 |

**无新增 SQL。** 后端需重新发布。APP 用量条可热更新。

**状态：** 已落地。

---

### 第 101 轮：审查 C — 账单分类随类型 / SSE 卸载 / 账单分页（2026-08-22）

> 管理端收入单能选到支出分类；离开对话页流式请求不停；账单按月一次拉全量，加载更多只是前端切片。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 账单 | BillDrawer 按 type 重载分类，切换收支清空分类 |
| APP | 对话 | `onUnload` abort + `/ai/chat/stop`；回来同一会话提示「连接中断，点击继续」 |
| 后端 | 账单 | `GET /bill/entries` 传 `pageNo+pageSize` 返回分页；不传仍返回全量 |
| APP | 账单 | 首页按页拉取，加载更多打下一页 |

**无新增 SQL。** 后端需重新发布。管理端 Drawer 与 APP 对话/账单可热更新。

**状态：** 已落地。

---

### 第 102 轮：审查 C 收口 — 列表失败横幅 / 下拉刷新 / 分包预加载（2026-08-22）

> 管理端主列表失败会变成空表；APP 主列表只能靠 onShow 刷新；进记账/AI 首次分包发白。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 账单/计划/菜谱/学习 | 列表请求失败出横幅+重试，与交叉统计对齐 |
| APP | 账单/计划/菜谱/学习 | 打开下拉刷新 |
| APP | 分包 | 首页 `preloadRule` 预加载 `pages-homeai-more` 与 `pages-homeai-ai` |

**无新增 SQL。** 无后端 Java 变更。管理端与 APP 可热更新；`preloadRule` 须随 H5/APK 一起发。

**状态：** 已落地。

---

### 第 103 轮：审查 D — 鉴权路径单测 / 菜谱白名单 / SQL 有序清单（2026-08-22）

> `isAdminPath` 是 private 难测；菜谱上传只认本地扩展名集合；init-db 按文件名 glob 增量 SQL；资料/对话不宜盲目加 `@TableLogic`。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 鉴权 | `HomeaiAdminPathUtil` 抽出路径判定；`HomeaiAdminPathUtilTest` 覆盖 APP/管理端边界 |
| 后端 | 菜谱 | 封面/视频/步骤图上传在本地类型集合之外再过 `IHomeaiFileWhitelistService` |
| 后端 | 资料/对话 | **不加** `@TableLogic`：回收站与手写软删依赖 `del_flag`/`deleted_at`；实体注释写明原因 |
| 工程 | SQL | `sql/alter-order.txt` 为已有库显式顺序；`smoke_homeai_schema.sql` 列存在性检查；init-db 按清单执行 |
| 测试 | 存储 | `StorageAccessUtilTest` 补 `canAccessFile` / `canWriteFile` |
| APP | 工具 | Vitest 覆盖 `date.ts` 与 `recipeIngredient.ts`（`pnpm test:utils`） |

**无新增 DDL。** 后端需重新发布。APP 工具函数测试不进发版包。

**状态：** 已落地。

---

### 第 104 轮：审查跨端收口 — 学习默认 / 食材算法 / 交叉统计昵称（2026-08-22）

> 管理端新建学习默认 video，APP 是 note；学习表单带着库里没有的 visibility；食材解析两端各写一份；交叉统计表只显示 raw userId。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | 学习 | LearnDrawer 默认类型改为 `note` |
| APP | 学习 | 去掉 visibility 死字段，不再随保存提交 |
| 管理端 | 菜谱 | RecipeDrawer 改用 `views/homeai/utils/recipeIngredient.ts`，与 APP 同算法 |
| 管理端 | 看板 | 交叉统计用户列走 `useUserLabel`，筛选改为用户下拉 |
| APP | 表单 | 账单/计划/菜谱/学习 add 页挂 `useHomeaiPageGuard` |
| 测试 | 工具 | 管理端 jest 覆盖食材解析；APP Vitest 覆盖可见性下标映射 |

**无新增 SQL。** 无后端 Java 变更。管理端与 APP 可热更新。

**状态：** 已落地。

---

### 第 105 轮：审查跨端 — 账单 / 计划 / 学习检索（2026-08-22）

> 只有菜谱和资料能搜，记账多了不好找，计划/学习列表同样只能靠翻页。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | 账单 | `GET /bill/entries` 可选 `keyword`，匹配备注、支付方式、分类名 |
| 后端 | 学习 | `GET /learn/materials` 可选 `keyword`，按标题 like |
| APP | 账单/学习 | 搜索栏沿用菜谱样式；空态可清空 |
| APP | 计划 | 当日标题前端过滤（当天条数少，不新开接口） |

**无新增 SQL。** 后端需重新发布。APP 可热更新。

**状态：** 已落地。

### 第 106 轮：APP 上传改走系统文件选择器（2026-08-23）

> Capacitor 壳里点击上传只会弹出相册视频：H5 `<input accept=video/*>` 和 `uni.chooseVideo` 都被 Android WebView 收成媒体库。

| 端 | 项 | 落地 |
|----|----|------|
| Android | `HomeaiUpdate.pickFile` | `ACTION_OPEN_DOCUMENT` 系统文档选择器，拷到 cache 后回传 path |
| APP | `filePicker` / `pickVideo` | Capacitor 优先调原生；旧壳无方法时退回 `<input accept=*/*>` |

**无新增 SQL。** 原生插件有改动，需重新 `pnpm pack:apk:local`，热更新不够。

**状态：** 已落地。

### 第 107 轮：APP 审查收口 — 超时 / 401 / 守卫 / 分享（2026-08-23）

> 全面检查后先改会让用户觉得「坏了」的项：全局 10 秒掐断对话与上传，旁路 401 不登出，详情缺守卫，邀请不能系统分享。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 超时 | 拦截器不再覆盖已有 timeout；HomeAI 请求 30s、上传 120s、对话 180s |
| APP | 401 | HTTP/body 401 统一清会话；上传与对话旁路复用 |
| APP | 守卫 | 详情/Office/导入/改密等页挂 pageGuard；学习/计划改删按归属隐藏 |
| APP | 其它 | 家庭邀请走 Capacitor Share；H5 PDF 带头；preloadRule；去掉 StorageBrowser 死路由 |

**无新增 SQL。** 超时与分享可热更新；系统文件选择器仍须重打 APK（第 106 轮）。

**状态：** 已落地。

### 第 108 轮：APP 资料刷新 / 日历日期 / 相册权限 / FileProvider（2026-08-23）

> 审查剩余项里用户能直接感到的：资料与对话不能下拉刷新、计划日历打点偶发错一天、个人中心账单条数拉全月、相册拒绝没提示、FileProvider 暴露整盘外部存储。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 下拉刷新 | 资料首页/文件夹/回收站、对话列表挂 `useHomeaiPullRefresh` |
| APP | 日期 | `toDateStr`：纯日历日原样；带时区 ISO 按本地换算。计划日历打点、资料文件日期使用 |
| APP | 个人中心 | 本月账单条数改走 `billApi.summary` 的 `count`，不再拉全月明细 |
| APP | 相册 | 拒绝权限 toast「未获得相册权限」；Capacitor 走 Media `requestPermissions` |
| Android | FileProvider | `file_paths.xml` 只暴露 cache / files，去掉 `<external-path path="." />` |

**无新增 SQL。** 下拉刷新、日期、账单条数、相册提示可热更新；FileProvider 收口须重打 APK（与第 106 轮一并出包）。

**状态：** 已落地。

### 第 109 轮：次级列表刷新 + 分类分页 + 记录日期（2026-08-23）

> 审查主列表刷新已齐。次级页仍靠 onShow：处理历史转完要等轮询、分类浏览被后端默认 `pageSize=10` 截断、学习记录按 `startsWith` 筛日期。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 菜谱分类 | `pageSize=20` + 加载更多 / 触底；下拉刷新。不再只显示前 10 道 |
| APP | 下拉刷新 | 处理历史、资料搜索、学习记录、账单统计 |
| APP | 日期 | `toDateTimeStr`：处理历史/学习记录时间；学习日历与按日过滤走 `toDateStr` |
| APP | 其它 | 资料搜索出骨架；`auth.getUser` 坏 JSON 不再抛 |

**无新增 SQL。** 均可热更新。

**状态：** 已落地。

### 第 110 轮：三个 Tab 下拉 / 学习回前台对时 / 账单编辑骨架（2026-08-23）

> 次级列表刷新已齐。停在首页、家庭、个人中心只能切 Tab 才刷新；学习切走再回来视频重载、计时不准；编辑账单先闪空表单。

| 端 | 项 | 落地 |
|----|----|------|
| APP | Tab 下拉 | 首页 / 家庭 / 个人中心 `useHomeaiPullRefresh` |
| APP | 学习详情 | 回前台用服务端 `elapsedSeconds` 对时；学习中不重拉媒体 URL，避免视频从头播 |
| APP | 账单编辑 | 数据未到先出骨架，不再闪 ¥0.00 |
| APP | 家庭 | 标题人数跟成员列表走；移除后再拉家庭信息 |
| APP | 个人中心 | 对话/学习/账单 `allSettled`，一个失败不再三个数字一起挂 |

**无新增 SQL。** 均可热更新。对话长回复非流式空白、对话列表分页未做（Capacitor 流式与后端分页要单独排）。

**状态：** 已落地。

### 第 111 轮：对话边到边出字 / 列表分页 / 导入确认分页（2026-08-23）

> Capacitor 上 `uni.request` 不支持 chunked，长回复只剩省略号；对话列表一次拉全量；账单导入确认上百条全量渲染、底部按钮要滑很久。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 对话 | H5/Capacitor 用 `fetch` + `ReadableStream` 读 SSE，边生成边出字；小程序仍走 `onChunkReceived` |
| 后端 | `/ai/conversations/mine` | 传 `pageNo`+`pageSize` 返回分页；不传仍是全量 List |
| APP | 对话列表 | 20 条一页，触底/加载更多；个人中心对话数读 `total` |
| APP | 导入确认 | 每页 50 条 + 本页全选；确认按钮吸底 |

**无新增 SQL。** APP 热更新即可；对话分页需后端一并发布。

**状态：** 已落地。

### 第 112 轮：回收站分页 / 计划编辑权 / 表单骨架 / 停止空气泡（2026-08-23）

> 回收站只拉 100 条，更早删除的条目回不来；计划缺 `userId` 时误显编辑；计划页和编辑表单先闪空；对话没出字就停止会留下空 AI 气泡。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 回收站 | `pageSize=20` + 触底加载，不再静默截断 |
| APP | 计划详情 | `canEdit` 仅 `userId` 等于当前用户，缺字段不显示改删 |
| APP | 计划列表 | 首次骨架；已有数据时 onShow 静默刷新，不闪「当日暂无」 |
| APP | 编辑页 | 计划/学习编辑数据未到先出骨架 |
| APP | 对话 | 停止生成时去掉空白 AI 占位；已有部分字则保留 |

**无新增 SQL。** 均可热更新。Office 处理历史全量分页未做。

**状态：** 已落地。

### 第 113 轮：Office 历史分页 / 生成后跳转 / 菜谱编辑骨架（2026-08-23）

> 处理历史一次拉全量，转换多了首屏卡；AI 生成用 `navigateTo` 叠一层，返回还能重复提交；编辑菜谱先闪空表单。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | `/storage/office/history` | 传 `pageNo`+`pageSize` 返回分页；不传仍是全量 List |
| APP | 处理历史 | 20 条一页；轮询只合并第一页状态，不冲掉已加载更多 |
| APP | AI 生成 | 提交后 `redirectTo` 历史，与格式转换一致 |
| APP | 菜谱编辑 | 数据未到先出骨架 |

**无新增 SQL。** APP 热更新即可；历史分页需后端一并发布。

**状态：** 已落地。

### 第 114 轮：切月不串月 / 计划刷新骨架 / 首页家庭轮询 / 收藏分页（2026-08-23）

> 学习记录换月仍显示上月列表；计划从编辑返回先闪旧内容；停在首页被移出家庭仍显示旧家庭名；收藏 Tab 一次拉全量且不能触底。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 学习记录 | 换月立刻清空并出骨架；`loadSeq` 丢弃乱序响应 |
| APP | 计划详情 | `onShow` 刷新进骨架，不再先闪旧标题 |
| APP | 首页 | 停留时每 45s 轻量拉家庭信息；网络失败不清家庭 |
| 后端 | `/recipe/favorites` | 传 `pageNo`+`pageSize` 返回分页；不传仍是全量 List |
| APP | 菜谱收藏 | 20 条一页 + 触底加载更多 |

**无新增 SQL。** 收藏分页需后端一并发布。

**状态：** 已落地。

### 第 115 轮：静默刷新 / 个人中心家庭轮询 / 移除防连点（2026-08-23）

> 从子页返回学习记录/计划详情/菜谱列表会闪骨架；个人中心 Tab 被移出家庭感知滞后；移除成员可连点。

| 端 | 项 | 落地 |
|----|----|------|
| APP | 学习记录 | `onShow` 有数据时静默刷新，失败保留旧列表 |
| APP | 计划详情 | 从编辑返回静默刷新，不再全页骨架 |
| APP | 烹饪指南 | 列表/搜索 `onShow` 静默刷新，对齐账单页 |
| APP | 个人中心 | 抽 `useFamilyPoll`；停留每 45s 拉家庭信息 |
| APP | 家庭 | 移除成员加 `familyBusy` 防连点 |

**无新增 SQL。** 无后端 Java 变更。APP 可热更新。

**状态：** 已落地。

### 第 116 轮：学习记录按月对齐 / 静默刷新收口 / 家庭 Tab 轮询（2026-08-23）

> 日历按 `study_date`、列表按 `start_time` 导致打点与记录不一致；对话/计划静默刷新失败仍清空或误报；家庭 Tab 无轮询。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | `/learn/records` | 按月查询与日历统一走 `study_date`；无 `study_date` 回退 `start_time` |
| APP | 对话列表 | `onShow` 静默刷新；失败保留已有对话 |
| APP | 计划列表 | 静默刷新失败不误报「加载失败」 |
| APP | 学习详情 | 返回刷新失败不盖掉已加载内容 |
| APP | 家庭 Tab | 接入 `useFamilyPoll`；转让/退出防连点 |

**无新增 SQL。** 学习记录按月过滤需后端发布；其余可热更新。

**状态：** 已落地。

### 第 117 轮：学习按日过滤 / 静默刷新扩展 / 对话首屏骨架（2026-08-23）

> 学习记录选日仍拉整月前端筛；对话首进闪「暂无」；资料/分类/回收站返回闪骨架。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | `/learn/records` | 可选 `studyDate` 按日过滤，与日历 `study_date` 对齐 |
| APP | 学习记录 | 选日/看全月走后端参数；切换日期重新拉取 |
| APP | 学习首页 | `onShow` 静默刷新资料列表 |
| APP | 对话列表 | 首屏骨架；`loading` 初始 true |
| APP | 资料存储 | `refresh(silent)`；子页返回静默刷新 |
| APP | 分类/回收站 | `onShow` 有数据时静默刷新 |

**无新增 SQL。** `studyDate` 需后端发布；其余可热更新。

**状态：** 已落地。

### 第 118 轮：三端安全契约 / 数据一致性 / 性能索引（2026-08-23）

> 管理端无权限可看全量菜谱/资料；账单导入缺归属用户；计划完成率月份 UTC 错月；搜索无上限；家庭网络失败误显「无家庭」。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | `HomeaiSecurityUtil.canConsoleViewAll` | 管理端 list/search 须对应 `homeai:*:list` 权限才看全量 |
| 后端 | 菜谱/资料搜索 | 单次最多 50 条 |
| 后端 | 学习今日进度 | 按 `study_date` 统计 |
| 后端 | 菜谱难度 | create/edit/admin 统一 1～5 校验 |
| 后端 | Office 模板上传 | 扩展名白名单 + 魔数校验 |
| 后端 | 账单导入 | 管理端 `import/confirm` 必填 `userId` |
| 后端 | 性能索引 | `alter_homeai_perf_indexes.sql`（账单按月、学习按日） |
| 管理端 | 账单导入/抽屉 | 必选归属用户 |
| 管理端 | 计划抽屉 | 必选归属用户 |
| 管理端 | 计划完成率 | `dayjs` 本地月，修复 UTC 错月 |
| 管理端 | 用户/家庭列表 | `useHomeaiListLoad` 失败横幅 + 重试 |
| APP | 账单/学习/菜谱 | 静默刷新、难度 Number、学习统计失败不阻断会话 |
| APP | 家庭 Store | `familyLoadFailed` / `familyInfoLoaded`；失败不误显无家庭 |

**迁移 SQL：** `alter_homeai_perf_indexes.sql`（见第二节清单）。

**状态：** 已落地。

---

### 第 119 轮：APP 文件上传修复（Capacitor 原生路径适配）（2026-08-31）

> APP 是 H5 构建 + Capacitor 壳，`uni.uploadFile` 走 uni-h5 的 Web 实现（XHR + FormData）。原生文件选择器（`HomeaiUpdate.pickFile`）返回文件系统绝对路径（`/data/...`），直接上传会被 `getRealPath` 拼成 origin 下 URL → 404；即使路径可读，multipart 文件名也会退化成 `file-<时间戳>`（丢扩展名）导致后端按扩展名校验失败。表现为「选择文件/视频/音频」必败，「拍照/相册」正常。

| 端 | 项 | 落地 |
|----|----|------|
| APP | `platform/uploadPath.ts` 新增 | `toUploadablePath()` 绝对路径转 `Capacitor.convertFileSrc` 代理 URL；`toUploadParams()` 取回带原始文件名的 `File`（保扩展名），blob:/data:/http 原样放行 |
| APP | 7 处 `uni.uploadFile` 调用点接入 | `HomeMediaUpload`（菜谱封面/视频/学习资料）、`useStorageUpload`（资料存储）、`chat.vue` 两处（对话图片/文件）、`bill.ts`（账单导入，`importPreview` 透传 fileName）、`useUpload` 旧钩子、`online-image/online-file`（低代码表单） |

**无迁移 SQL。**

**状态：** 已落地。

---

### 第 120 轮：管理端生产构建修复（_app.config.js 无法解析）（2026-08-31）

> `cf6c306` 误在 html.ts 给注入的 `_app.config.js` 脚本标签加了 `type:'module'`，而该文件是 postBuild 阶段才生成的运行时配置。Vite 8 + rolldown 的 `vite:build-html` 对 module 脚本 src 做解析，找不到文件即硬报错 `Failed to resolve /_app.config.js?v=...`，管理端 `build:docker:prod` 全挂。

| 端 | 项 | 落地 |
|----|----|------|
| 管理端 | `build/vite/plugin/html.ts` | 移除 `type: 'module'`，恢复经典脚本注入（head 内同步执行，bundle 前可用），并修正 `tags` 缩进 |
| 验证 | `pnpm run build:docker:prod` | 完整跑通：vite build → postBuild 生成 `dist/_app.config.js` → copyChat，EXIT=0 |

**无迁移 SQL。**

**状态：** 已落地。

---

### 第 121 轮：APP 更新 APK 下载修复（OSS ApkDownloadForbidden 代理下载）（2026-08-31）

> 「立即更新」报错。根因：阿里云 OSS **默认域名禁止预签名 URL 直链分发 .apk**（`ApkDownloadForbidden`；实测 query 签名被拦 → 400，Header 签名 / SDK 拉流放行 → 206；`.zip` 不受限）。App 端原生下载 apkUrl 得到 HTTP 400 → toast「下载失败: HTTP 400」。无自定义域名可用，采用**后端代理下载（SDK 拉流）**绕开，存量数据与 App 端零改动。

| 端 | 项 | 落地 |
|----|----|------|
| 后端 | `HomeaiAppVersionController` 新增 `GET /homeai/app/version/package/download` | `resolveLocalPath`（OSS 走 SDK `getObject`，Header 签名实测放行）→ `HomeaiFileMime.writeLocalFile` 流式返回 |
| 后端 | `HomeaiAppVersionServiceImpl` | `toPublic`/`toAdminView` 的 `apkUrl` 统一改为代理下载地址（`HomeaiFileUrlUtil.toAbsoluteUrl` 拼当前域名）；`uploadPackage` 存储 key 保持 `.apk` |
| 后端 | `HomeaiAuthInterceptor.PUBLIC_PATHS` | 放行 `/homeai/app/version/package/download`（所有用户需下载更新包） |
| 兼容 | 存量 `.apk` 对象 | 直接可用，无需重新上传；App 端 `updater.ts` 无需改动（apkUrl 指向后端接口，verifySha/installApk 链路不变） |

**无迁移 SQL。**

**状态：** 已落地。

---

### 第 123 轮：APP 离线可用与缓存优化（2026-08-31）

> 后台临时不可用时，已浏览内容本地可看；计划/账单/学习/菜谱离线可用基本功能；离线写操作入队，恢复后缓慢同步（1 条/批、5s 间隔、单条 24h 限 20 次，后台可配置）；个人中心显示服务器状态。

| 端 | 项 | 落地 |
|----|----|------|
| APP | `offline/` 基建 | `conn`（连接状态/退避重试）、`cache`（读缓存+版本比对）、`syncQueue`（写队列+限频同步引擎）、`imageCache`（IndexedDB 磁盘 4GB LRU）、`dataAccess`（readList/readDetail/mutate）、`senders`（模块同步执行器）、`pendingUpload`（离线文件暂存补传）、`OfflineImage`/`OfflineBanner` |
| APP | 个人中心 | 服务器地址 → **服务器状态**（🟢可用/🔴不可用/⏳检测中，点击重测，改地址收进设置弹窗） |
| APP | 模块接入 | 计划（列表/详情读缓存+toggle/增删改入队）、账单（列表读缓存+记一笔/改删入队）、学习（材料/统计读缓存+目标/停止入队）、菜谱（列表读缓存+封面 OfflineImage+增改入队+离线图片 pending 编排）；资料上传离线暂存 `pendingUpload`；`HomeMediaUpload` 离线存 pending 返回 `pending://key` |
| APP | AI 历史 | 会话列表 + 消息 readList 缓存，离线可查看历史对话（不能发起新对话） |
| APP | 学习补记 | 记录页新增「手动补记」入口（时长输入），走 `learn/addRecord`（recordType=manual）入队，离线可用 |
| 后端 | `image-proxy` | OSS 预签名 URL 无 CORS → 后端同域代理（host 白名单防 SSRF），供图片 blob 缓存；实测 200 字节完整 |
| 后端 | `sync config` | `homeai_sync_config` 表 + `GET /homeai/config/sync`（公开拉取，实测 200 默认值）+ `PUT /admin`（管理端修改 batchSize/intervalMs/maxRetriesPerDay/imageCacheLimitMB）；App 启动拉取 |
| 管理端 | `syncConfig.vue` | 同步/缓存参数表单页；菜单挂载 SQL `alter_homeai_menus_sync_config.sql`（挂「APP版本」下，权限 homeai:app:version:edit） |
| SQL | `alter_homeai_sync_config.sql` + `alter_homeai_menus_sync_config.sql` | 配置表 + 默认行；菜单挂载 |

**迁移 SQL：** `alter_homeai_sync_config.sql`（见第二节清单）。

**状态：** 已落地（SQL 均已执行：配置表 + 菜单「同步配置」menu_type=1；安全修复：image-proxy 白名单收紧至当前 OSS 桶域名（伪造桶 403 已验证）、sync 配置 PUT 登记管理端路径（控制台 JWT 200 已验证）；待办：**HTTPS 上线**（当前公网 HTTP 明文，P0）、生产管理端改密码）。

---

### 第 122 轮：发布脚本自动登记 APP 版本（2026-08-31）

> `publish-all.ps1 -App` 此前只打包，登记版本需手动去管理端。新增 `-RegisterVersion` 一键闭环：打包后自动上传 APK + H5 zip 并更新后台版本号（enabled=1 即刻推送）。

| 端 | 项 | 落地 |
|----|----|------|
| 脚本 | `pack-apk-local.ps1` | 打包完成后写 `dist\apk\last-version.json`（apk/zip/versionName/versionCode），作产物元数据契约 |
| 脚本 | `publish-all.ps1` | 新增 `-RegisterVersion` / `-UpdateMode` 参数；`Invoke-HomeaiAppVersionRegister` 读 `last-version.json` → 调 `register-app-version.mjs`（登录→传 APK→传 zip→保存版本号）；打包失败不登记、登记失败单独计入失败清单 |
| 脚本 | `register-app-version.mjs` | 复用（零改动）：AES 密码 + redis-cli 验证码登录；`enabled=1` |
| 默认 | 更新模式 | `resource`（H5 热更新）；壳/原生改动时用 `-UpdateMode apk` |
| 验证 | `-App -SkipAppBuild -RegisterVersion` | 全链路跑通：产物 → last-version.json → 双上传 → 保存；后端 `updateMode=resource`、`enabled=true`、apkUrl 走代理下载 |

**无迁移 SQL。**

**状态：** 已落地。

---

## 二、数据库迁移清单（已有库按序执行）

**权威顺序：** [`sql/alter-order.txt`](../../JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql/alter-order.txt)。新库只跑 `init_homeai_*.sql`；已有库按该清单补跑。执行后可用 `smoke_homeai_schema.sql` 检查关键列。`sql/legacy/` 不自动执行。

新库直接使用 `init_homeai_tables.sql` + `init_homeai_menus.sql` 即可。

### 必选（第 11～12 轮）

```text
alter_homeai_menus_iteration11.sql      # 操作审计菜单
alter_homeai_recipe_favorite.sql        # 菜谱收藏表
alter_homeai_learn_material_category_id.sql
alter_homeai_learn_record.sql
```

### 第 22 轮（已有库）

```text
alter_homeai_menus_iteration22.sql      # 审计独立权限 + 资料回收站按钮
```

### 第 23 轮（已有库）

```text
alter_homeai_storage_folder_recycle.sql # 文件夹 deleted_at
alter_homeai_plan_recipe_iteration23.sql # 计划关联 recipe_id
```

### 第 47 轮补记（已有库，按缺列执行）

```text
alter_homeai_wx_user_android_login.sql     # password/salt/login_type（若未跑过）
alter_homeai_deleted_at_schema_gap.sql     # 旧表补 deleted_at（可重复执行）
```

### 第 48 轮（已有库）

```text
alter_homeai_learn_record_study_date.sql   # 学习记录 study_date 可空并对齐回填
```

### 第 63 轮（已有库）

```text
alter_homeai_file_whitelist_audio_webp.sql # 白名单补 webp/webm/音频
alter_homeai_preview_pdf_url.sql           # storage/learn 的 preview_pdf_url；convert_type 扩至 32
```

### 第 69 轮（已有库）

```text
alter_homeai_app_version.sql           # APP 版本表 + 白名单 apk
alter_homeai_menus_iteration69.sql     # 管理端「APP版本」菜单
```

### 第 118 轮（已有库）

```text
alter_homeai_perf_indexes.sql          # 账单 user+date、学习 user+study_date 复合索引
```

### 第 123 轮（已有库）

```text
alter_homeai_sync_config.sql           # 离线同步/缓存配置表 + 默认行
alter_homeai_menus_sync_config.sql     # 管理端「同步配置」菜单（挂 APP版本 下）
```

### 第 26 轮（已有库）

```text
alter_homeai_menus_iteration26.sql      # 综合统计菜单
```

### 第 32 轮（已有库）

```text
alter_homeai_menus_iteration32.sql      # 家庭配额运营看板菜单
```

### 第 39 轮（已有库）

```text
alter_homeai_wx_user_android_login.sql  # WxUser 新增 password/salt/login_type（Android 手机号+密码登录）
```

### 第 40 轮（已有库）

> **第 40 轮：无新增 SQL**（`PlanInstance` 新增 `startTime` / `remindMinutes` 均为 `@TableField(exist=false)` 冗余展示字段，无 DDL）。原 EMAS 方案已弃用，`alter_homeai_wx_user_emas_push.sql` 不再需要执行。

### 历史增量（第 1～7 轮，若从未执行按需补跑）

建议按编号/依赖顺序执行：

```text
# 第 1～4 轮
alter_homeai_plan_category.sql
alter_homeai_menus_recipe_category.sql
alter_homeai_menus_optimize.sql
alter_homeai_family_status.sql

# 第 5～7 轮
alter_homeai_learn_category.sql
alter_homeai_file_whitelist.sql
alter_homeai_menus_iteration5.sql
alter_homeai_office_convert_instruction.sql
alter_homeai_menus_iteration8.sql

# 第 8～12 轮及补丁（见 sql 目录其余 alter_homeai_*.sql）
```

执行后：**刷新菜单缓存**，并为角色分配新菜单权限。

> 第 13～16、19～21 轮主要为前端/能力增强；**第 22 轮**需执行 `alter_homeai_menus_iteration22.sql`（审计权限 + 资料回收站按钮）。

---

## 三、后续优化建议（按模块）

优先级说明：**P0** = 安全/数据正确性；**P1** = 体验与一致性；**P2** = 增强功能。

### 3.0 前端工程化（第 13～14 轮）

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P0 | 生产 DOMAIN_URL / qiankun | 同域 `/jeecgboot`；关闭微前端 | ✅ 第 13 轮 |
| P0 | 菜单失败反馈 / 路由文档 | permission catch + homeai.ts 注释 | ✅ 第 13 轮 |
| P0 | 小程序鉴权双轨统一 | HomeAI store + 简化 router | ✅ 第 13 轮 |
| P1 | 分包 / 依赖 / CRUD / 类型 / 主题 / CI | 见第 13 轮 P1 表 | ✅ 第 13 轮 |
| P2 | demo 排除 / 死代码 / 分页 / SSE / 测试 | 见第 13 轮 P2 表 | ✅ 第 13 轮 |
| P0 | CI 测试 / PATCH / 生产 env / docker.prod | 见第 14 轮 P0 表 | ✅ 第 14 轮 |
| P1 | 小程序遗留 SelectUser/Popup | 整批删除 | ✅ 第 14 轮 |
| P1 | 管理端 Category 接入 `useHomeaiCrud` | learn/recipe/bill | ✅ 第 14 轮 |
| P1 | 菜谱 quantity/unit + categoryId 校验 | 两端对齐 | ✅ 第 14 轮 |
| P2 | CI 增加 UniApp `build:mp-weixin`（可选） | 耗时长，可 nightly；先填实生产 API | ⬜ 待做 |
| P2 | 微信 urlCheck / targetSdk 34 / 隐私合规 | 上架前必做 | ⬜ 待做 |
| P2 | 首页冷启动闪屏（entry=index） | 首次进入骨架屏（家庭+今日计划就绪后） | ✅ 第 20 轮 |
| P1 | APP 启动先进登录 | launch 中转页，去掉未登录闪首页 | ✅ 第 46 轮 |
| P1 | 账号闭环（改密/改资料/建家引导/401 回跳） | 登录后可改密码与昵称头像；注册引导家庭 | ✅ 第 46 轮 |
| P0 | 手机号 JWT 身份解析 | 业务接口统一 `getWxUser` / `getWxUserId`，禁止只按 openid 查库 | ✅ 第 47 轮 |
| P0 | 退出登录作废 Redis token | `/auth/logout` 删 userId + openid 两把钥匙 | ✅ 第 47 轮 |

### 3.0b 视觉与体验（第 16 轮）

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P1 | 小程序设计令牌 | `--hai-*` + 页面/卡片/FAB 工具类 | ✅ 第 16 轮 |
| P1 | 首页 / Tab / 表单统一 | 暖灰+深蓝；`HomeFormCard`（plan/learn/bill/recipe） | ✅ 第 16 轮 |
| P1 | 管理端同源语义色 | `homeai-theme.less`；账单金额类；PageWrapper | ✅ 第 16 轮 |
| P2 | 硬编码 hex 全面迁令牌 | 主列表/详情/存储子页 style → `var(--hai-*)`；route 导航色与 wd-icon 实色保留 | ✅ 第 16 轮续 |
| P2 | TabBar 图标精细化 | 选中实心 / 未选中描边；色 `#1B4F8A` / `#8A857C` | ✅ 第 16 轮续 |
| P1 | Wave 1 硬伤止血 | 首页 CSS、officeTemplate、wot 圆角、`.hai-page` / `.homeai-page-body`、工具栏分级 | ✅ 第 17 轮 |
| P1 | Wave 2 组件收敛 | HomeEmpty/FAB、详情样板、useUserLabel 扩展、状态色、Bill/Recipe Drawer | ✅ 第 18 轮 |
| P1 | Wave 3 体验打磨 | 存储双标题、触控热区、`.hai-press`、AI 左滑、B 端主色决策文档化 | ✅ 第 19 轮 |
| P2 | AI 对话左滑删除等微交互 | 体验增强 | ✅ 第 19 轮 |

### 3.1 菜谱管理

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P0 | 列表/搜索可见性 | 小程序仅看本人 + 家庭共享 | ✅ 第 11 轮 |
| P0 | 详情鉴权 | `GET /recipe/{id}` 校验可见性 | ✅ 第 12 轮 |
| P0 | 难度字段统一 | 管理端 1～5 整数展示 | ✅ 第 11 轮 |
| P0 | 收藏 API | `homeai_recipe_favorite` + toggle/list | ✅ 第 12 轮 |
| P1 | 食材模型对齐 | 前端 `amount` 与 DB `quantity/unit` 统一 | ✅ 第 14 轮 |
| P1 | 收藏列表入口 | 小程序「我的收藏」Tab | ✅ 已有（第 12/复查确认） |
| P1 | 创建时 categoryId | 小程序 submit 前必填校验 | ✅ 第 14 轮 |
| P1 | Excel 导入 | 主表 + 食材/步骤文本列（子表）；同名覆盖；分类按菜名纠正 | ✅ 第 24 / 61 轮 |
| P1 | 封面批量导入 | 单张/文件夹/zip，按文件名或父目录匹配 | ✅ 第 60 轮 |
| P1 | APP 详情删除 | 对齐计划详情：确认弹层 + `canModify` | ✅ 第 64 轮 |
| P2 | 浏览计数 | `viewCount` 递增 + 热门排行 | ✅ 第 26 轮 |
| P2 | 与计划联动 | Master 关联 `recipeId`；首页「今日下厨」 | ✅ 第 23 轮 |
| P2 | 智能推荐 | 按季节、家庭口味、历史做过次数推荐 | ✅ 第 28 轮轻量版（计划/收藏/季节加权热门）；✅ 第 30 轮「新菜尝鲜」独立区块；✅ 第 32 轮做过次数加权 |

### 3.2 资料（存储）管理

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P0 | 家庭文件夹树 | 合并 `visibility=family` | ✅ 第 11 轮 |
| P0 | 家庭文件读权限 | 文件夹内文件、搜索、详情 | ✅ 第 12 轮 |
| P0 | 上传 familyId | 创建/上传时绑定家庭 | ✅ 第 12 轮 |
| P1 | 共享目录 UI | 管理端上传者昵称（树/列表/Top5）；小程序昵称（`useMemberLabel`） | ✅ 管理端第 15 轮 / 小程序第 20 轮 |
| P1 | 文件级 visibility 继承 | 上传至家庭文件夹时默认 `family`（已实现），需文档化 | ✅ 逻辑已有 |
| P1 | 缩略图 | 图片 + PDF 首页 → `thumbnailUrl` | ✅ 第 21/22 轮 |
| P1 | 列表分页 | 小程序/接口 `pageNo/pageSize`；管理端表格分页 | ✅ 第 21 轮 |
| P1 | 回收站 | 文件 + 文件夹；管理端 + 小程序自助恢复 | ✅ 文件第 22 / 文件夹第 23 / 小程序第 24 轮 |
| P2 | 用户空间配额 | 默认用户上限（Redis）+ 上传校验 + 告警展示；家庭维度表另排期 | ✅ 用户默认第 23 轮 / ✅ 家庭默认第 28 轮 / ✅ 家庭级覆盖第 30 轮（Redis，无独立表） / ✅ 运营看板第 32 轮 |
| P2 | 小程序上传统一 | 白名单 + `chooseMessageFile` composable，支持非图片 | ✅ 第 25 轮 |
| P1 | 多类型预览播放 | 页内图片/音视频/PDF；Office 转 PDF 缓存 | ✅ 第 63 轮 |

### 3.3 学习管理

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P0 | 计时会话 Redis | 替代内存 Map，服务重启不丢 | ✅ 第 11 轮 |
| P0 | 小程序计时 UI | 进行中条 + `stop` | ✅ 第 11 轮 |
| P0 | categoryId 迁移 | 实体 + SQL + 管理端/小程序存 ID | ✅ 第 12 轮 |
| P0 | 学习记录表对齐 | `start_time/end_time/duration(秒)/notes` | ✅ 第 12 轮（需执行 alter） |
| P1 | 管理端趋势图 | 近 N 日记录数 + 时长 | ✅ 第 12 轮 |
| P1 | 按分类统计 | 各分类学习时长、资料数排行 | ✅ 第 15 轮 |
| P1 | 资料详情页 | 小程序独立详情 + PDF/视频打开即学 | ✅ 第 15 轮（autoStart） |
| P2 | 学习提醒 | 每日目标 + 微信订阅消息 | ✅ 第 29 轮（模板未配时 stub）；✅ 第 31 轮字段可配置对齐 |
| P2 | 管理端多维图表 | 按用户、按分类、7 日/30 日切换 | ✅ 第 29 轮；✅ 第 31 轮 Excel 导出 |
| P2 | 列表分页 | 小程序 pageSize=20 + 加载更多 | ✅ 第 13 轮 |
| P1 | 多类型预览播放 | 音频类型；页内 PDF/音视频；Office 转 PDF | ✅ 第 63 轮 |
| P1 | APP 资料改删 | 详情编辑/删除；`add.vue?id=` 复用表单 | ✅ 第 64 轮 |
| P0 | 计时归属校验 | start/stop/record 拒绝他人/无主资料 | ✅ 第 64 轮 |

### 3.4 计划 / AI / 审计（简要）

| 模块 | 建议 | 优先级 | 状态 |
|------|------|--------|------|
| 计划 | 完成率按 `yearMonth` 筛选 UI；实例清理定时任务 | P1 | ✅ 筛选 UI 第 20 轮 / ✅ 清理任务验收第 21 轮 |
| 计划 | APP 详情展示内容并支持编辑/软删 | P1 | ✅ 第 51 轮 |
| 菜谱 | APP 详情删除（`canModify`） | P1 | ✅ 第 64 轮 |
| 学习 | APP 资料编辑/删除 + 计时归属 | P0/P1 | ✅ 第 64 轮 |
| 计划 | 与菜谱/学习交叉统计（「计划完成 + 学习时长」仪表盘） | P2 | ✅ 第 26 轮 |
| AI | 更多场景配额（chat、office、recipe 生成）统一预检 | P1 | ✅ 第 25 轮（recipe 场景预留） |
| AI | SSE `enableChunked` 降级 | P2 | ✅ 第 13 轮 |
| AI | 流式成功避免重复解析 | P1 | ✅ 第 14 轮 |
| 审计 | 扩展 module（storage/recipe/learn）；独立权限码 | P1 | ✅ 第 22 轮 |

---

## 四、建议下一轮执行顺序

### 第 117 轮（已落地）

学习 records 按日过滤；学习首页/资料/分类/回收站静默刷新；对话首屏骨架。需发布后端。

### 第 116 轮（已落地）

学习 records 按月与日历对齐；对话/计划/学习详情静默失败收口；家庭 Tab 轮询 + 转让退出防连点。需发布后端。

### 第 115 轮（已落地）

学习/计划/菜谱返回静默刷新；个人中心家庭轮询；移除成员防连点。

### 第 114 轮（已落地）

学习记录切月防串月；计划详情刷新出骨架；首页 45s 刷新家庭；菜谱收藏分页。需发布后端。

### 第 113 轮（已落地）

Office 处理历史分页；AI 生成提交后 redirect 历史；编辑菜谱出骨架。需发布后端。

### 第 112 轮（已落地）

回收站分页；计划编辑权收紧；计划/学习编辑骨架；停止生成去掉空气泡。

### 第 111 轮（已落地）

对话 H5 流式出字；对话列表分页；账单导入确认分页 + 吸底按钮。需发布后端。

### 第 110 轮（已落地）

三个 Tab 可下拉；学习回前台对时且不重载视频；账单编辑出骨架；家庭人数与统计失败不再连带。

### 第 109 轮（已落地）

菜谱分类分页；处理历史/搜索/学习记录/账单统计下拉刷新；学习记录按本地日期过滤。

### 第 108 轮（已落地）

资料/对话下拉刷新；计划日历 `toDateStr`；个人中心账单走 summary；相册拒绝有提示；FileProvider 收口。FileProvider 与文件选择器须重打 APK。

### 第 107 轮（已落地）

拆掉全局 10s 超时；401 与 pageGuard 收口；邀请走系统分享。文件选择器仍须重打 APK。

### 第 106 轮（已落地）

APP 文件上传走系统文档选择器，不再只出相册视频。需重打 APK。

### 第 105 轮（已落地）

账单按月、学习资料可搜；计划搜当日标题。空态可清空，不冲掉已有搜索词。

### 第 104 轮（已落地）

学习默认类型两端统一 note；去掉 APP 学习 visibility 死字段；食材解析共用同一文件；交叉统计显示昵称；表单页挂登录守卫。

### 第 103 轮（已落地）

抽出管理端路径判定并单测；菜谱上传走全局白名单；SQL 显式顺序 + schema smoke；资料/对话不加 TableLogic。

### 第 102 轮（已落地）

管理端主列表失败出横幅可重试；账单/计划/菜谱/学习支持下拉刷新；首页预加载业务分包。

### 第 101 轮（已落地）

管理端账单分类随收支切换；离开对话页停止 SSE；账单按月改为服务端分页。

### 第 100 轮（已落地）

APP 写路径不再用控制台 JWT 当业务 userId；Token 扣减超额回滚；预检展示真实限额；资料首页显示存储用量。

### 第 99 轮（已落地）

Office 提交补读权限；家庭可见只许选自己的家；对话附件上传前再过白名单。

### 第 98 轮（已落地）

导入切类型清空文件；菜谱空清单可补充；计划失败与已删除分开；首页显示今日学习进度。

### 第 97 轮（已落地）

学习日历可点开当天记录；分类空态可添加；计划标题去空格。

### 第 96 轮（已落地）

账单翻月不再误写「本月」；未来记账需确认；资料搜索可清空；对话页显示会话标题。

### 第 95 轮（已落地）

菜谱搜索返回不被冲掉；搜不到可清空；学习外链须 http(s)；关于页用真实版本号。

### 第 94 轮（已落地）

登录协议勾选会记住；邀请码去空格；过去日期/公开菜谱需确认；管理端强制更新需确认。

### 第 93 轮（已落地）

Office 处理中自动刷新；空导入不再进入确认；计划关联菜谱失败可重试；资料选家庭失败与没家庭分开。

### 第 92 轮（已落地）

无家庭不再默认共享菜谱；对话可只发图片；消息/下厨失败不再被当成空数据。

### 第 91 轮（已落地）

转换规则失败与空态分开；AI 生成切类型换模板；分类失败可重试且不阻断编辑；账单统计失败保留旧数据。

### 第 90 轮（已落地）

回收站/学习计时/存储弹窗防连点；账单解析防连点；管理端回收站失败不再清空。

### 第 89 轮（已落地）

家庭/账单导入/Office 防连点；菜谱详情与新菜返回刷新；计划勾选与删除防连点。

### 第 88 轮（已落地）

计划/对话防连点；个人中心与学习统计及时刷新；分类页返回刷新；管理端新建菜谱默认家庭共享；配额看板失败保留旧数据。

### 第 87 轮（已落地）

管理端看板失败不再归零；学习资料编辑走鉴权下载；记账/家庭成员/改服务器地址补齐反馈。

### 第 86 轮（已落地）

详情/编辑失败可重试；首页计划失败不再当「暂无」；改密可显密；统计/分类失败可重试。

### 第 85 轮（已落地）

菜谱/计划/资料/学习失败可重试；资料顶栏补齐 Office 入口；处理历史可打开已完成结果。

### 第 84 轮（已落地）

家庭角色改用 `myRole`；转让/升管理员需确认；AI 对话显示配额与空态引导；对话列表失败可重试。

### 第 83 轮（已落地）

账单导入改本地缓存；计划/账单/学习失败可重试；学习结束需确认；菜谱请求收进 `recipeApi`。

### 第 82 轮（已落地）

资料下载三端对齐：统一 content URL / Token、管理端 JWT 下载、文档按钮改称「打开文件」。

### 第 81 轮（已落地）

APP 下载/打开文档改走原生 `ACTION_VIEW` + 后端鉴权原文件流。须重打 APK。

### 第 80 轮（已落地）

APP 文档不再转 PDF；下载走原生 HTTP；列表/详情图片走 OSS 压缩图。

### 第 79 轮（已落地）

双击 `.cmd` 菜单改走 PowerShell；停止后端时不再把已退出的 Java PID 当成错误。

### 第 78 轮（已落地）

一键脚本可分别发布/停止前端、后端、APP；后端启动失败不再假装成功。

### 第 77 轮（已落地）

`docs/deploy/` 一键发布（APP + 管理端 + 后端）与一键停止前后端。

### 第 76 轮（已落地）

菜谱封面走 OSS 时带 Content-Length 并重试，避免 Connection reset 导致上传失败。需重启后端。

### 第 75 轮（已落地）

H5 + Capacitor 走手机号登录（不再误走小程序微信登录）；管理端学习资料必填归属用户，家庭下拉排除已解散。

### 第 74 轮（已落地）

本机出包给 Capacitor 子工程补阿里云 Maven，避免 JDK 直连 Google TLS 失败。

### 第 73 轮（已落地）

管理端菜谱列表只保留一个「导入封面」入口；弹窗选图/选文件夹改为原生文件选择，并去掉会挡住按钮的滚动条遮罩。

### 第 72 轮（已落地）

系统返回键/手势在 Tab、登录、启动页直接退出 APP，不再「再按一次」。须重新出包覆盖安装。

### 第 71 轮（已落地）

本机 Capacitor 出包写入 HA 自适应图标、启动图、通知小标；minSdk/权限/主题色与 uni-app 对齐。覆盖安装后桌面图标才会更新。

### 第 70 轮（已落地）

发版只走 `pnpm pack:apk:local`。HBuilderX 云打包与 `pnpm pack:apk`（DCloud 离线 SDK）已弃用。

### 第 69 轮（已落地）

管理端「APP版本」控制更新方式与安装包；APP 启动页按 `versionCode` 比较后覆盖安装 APK 或热更新 H5 zip。种子记录默认关闭。须跑 SQL 并侧载新包。

### 第 65 轮（建议）

1. 真机侧载 Capacitor APK：登录、HTTP、计划通知、相册、返回键、拍照选图、菜谱/学习改删、**资料/学习多格式上传**  
2. 忘记密码短信验证码（需阿里云模板；当前仍走管理员后台重置）  
3. （可选）`isStorageAdmin` 按权限码收紧，避免任意控制台 JWT 预览全部资料  

### 第 68 轮（已落地）

OSS 私有上传不再把文件名里的 `.` `_` 清掉；新封面会带 `.png` 等后缀。旧文件需重新导入。

### 第 67 轮（已落地）

APP 文件选择按 MIME 过滤（含音频/文档）；白名单对齐 webp/音频；Android 13+ `READ_MEDIA_AUDIO`。

### 第 68 轮（已落地）

OSS 私有上传 objectKey 保留扩展名与下划线（`StrAttackFilter` 只过滤目录段）。

### 第 66 轮（已落地）

管理端菜谱列表：选择提示条与工具栏拉开间距；封面导入弹窗加大、可拖动改尺寸、显示导入进度，并避免选文件后误关。

### 第 64 轮（已落地）

APP 菜谱删除、学习资料改删；学习 start/stop/record 归属校验；Capacitor `CAMERA` 权限。

### 第 62 轮（部分并入第 64 轮）

1. 真机侧载新 Capacitor APK：登录、HTTP API、计划通知、保存相册、返回键 — **仍待验收**  
2. 忘记密码短信验证码 — **仍待做**（无短信模板）  
3. APP 学习资料 / 菜谱删除与编辑 — **【第 64 轮已落地】**  

### 第 63 轮（已落地）

资料与学习多类型上传、管理端/APP 页内预览与播放；Office 转 PDF 缓存。

### 第 61 轮（已落地）

菜谱 Excel 同名覆盖、分类按菜名纠正、封面导入入口前置。

### 第 60 轮（已落地）

管理端菜谱封面支持单张/多选与整文件夹导入，按文件名或父目录匹配。

Capacitor 相册 Media、文档 Share、外链 Browser、返回键与状态栏。

### 第 58 轮（已落地）

本机 `pnpm pack:apk:local` 打出签名 APK；H5 + Capacitor 6，通知插件已接。

### 第 57 轮（已落地）

长久本机出包改为 H5 + Capacitor；云打包仍用于当前内测。

### 第 56 轮（已落地）

云打包缺 Push 时启动不再红屏；清单补 `sdkConfigs.push`，须重新云打包并勾选 Push。

### 第 55 轮（已落地）

后台新增/导入 App 用户写入默认密码 123456 与 salt；登录缺盐不再 NPE；管理端可重置密码。

### 第 54 轮（已落地）

Android 离线 SDK 脚本（第 54 轮）已归档；发版已改为 `pnpm pack:apk:local`（第 70 轮）。

### 第 53 轮（已落地）

邀请系统分享、内测改 API、homeai 模块跑单测、表单/协议/学习链接抛光、计划 byDate 批量填充。

### 第 52 轮（已落地）

后台家庭移入回收站后从主列表消失；`status`（已解散）与 `delFlag`（回收站）不再混用。

### 第 51 轮（已落地）

计划详情编辑删除、资料 wot 弹层、账单底栏 fixed、启动页隐私协议可点开。

### 第 39 轮（历史建议，部分仍待做）

1. 学习提醒真机订阅联调验收  
2. AI 场景配额运营报表  
3. 家庭配额 modal 去重（family/index 与 fileList 各一份，抽公共 hook）  
4. ~~后端纯逻辑单测扩展（沿 `RecipeVisibilityTest` 模式；注意父 pom `skipTests` 需临时翻转）~~ **【第 44 轮已落地】**  

### 第 43 轮（可选）

1. ~~（可选）微信正式上架：`VITE_*_RELEASE` 填真实域名；隐私协议弹窗（`__usePrivacyCheck__`）与微信后台合法域名~~ **【已暂缓，按产品决定不上架】**；生产构建守卫（第 38 轮）已就位，后续如需上架按此清单执行即可  
2. 菜谱推荐多样性（近期做过降权，避免总推同一道）  
3. （评估）`/homeai/**` 在 Shiro 链为 `anon`，纵深依赖 MVC 拦截器 + `@RequiresPermissions`，如需更强隔离可在 Nginx 层做路径级 ACL  
4. 小程序双重 toast 收敛（request 层与页面 catch 去重，微信 toast 替换式显示、影响小）

---

## 五、配置与环境提醒

```yaml
homeai:
  plan:
    repeat-horizon-days: 90
    instance-cleanup-days: 30
    remind-enabled: true
    ai-doc-polish-enabled: true
  learn:
    remind-enabled: true
    remind-cron: "0 0 20 * * ?"
  wechat:
    plan-remind-template-id: xxx
    learn-remind-template-id: xxx
    # 与微信后台模板关键词对齐（字段名置空则跳过）
    learn-remind-title-field: thing1
    learn-remind-progress-field: number2
    learn-remind-goal-field: number3
    learn-remind-date-field: time4
    learn-remind-title-text: 每日学习目标
  office:
    soffice-path: soffice
  upload:
    limits:
      video: 209715200      # 200MB
      audio: 52428800       # 50MB
      image: 20971520       # 20MB
      document: 52428800    # 50MB
      archive: 104857600    # 100MB
      text: 10485760        # 10MB
homeai.ai.key-encryption-key: xxx
```

**上传体积（第 63 轮）：** Spring `multipart.max-file-size/max-request-size` 已改为 **200MB**。反向代理须同步：`deploy/frp/nginx-home.conf`、`nginx-server.conf`、`deploy/frontend-nginx/nginx/homeai-admin.*.conf` 的 `client_max_body_size 200m`，改完后 **nginx reload**。

**管理端生产前端：**

- 构建：`pnpm run build:docker:prod`（`VITE_GLOB_DOMAIN_URL=/jeecgboot`）
- 部署后务必核对 `dist/_app.config.js`

**小程序生产（上架 checklist）——【已暂缓：按产品决定暂不上架】**：

1. `JeecgUniapp/env/.env.production`：把 `VITE_SERVER_BASEURL__WEIXIN_RELEASE` / `VITE_UPLOAD_BASEURL__WEIXIN_RELEASE` 改为正式 HTTPS  
2. `manifest.config.ts` → `mp-weixin.setting.urlCheck: true`（已开启；本地连 `127.0.0.1` 时请在微信开发者工具勾选「不校验合法域名」）  
3. 取消注释 `__usePrivacyCheck__: true`，并完善隐私协议弹窗  
4. 微信公众平台配置 request / uploadFile 合法域名

> 注：生产构建守卫（第 38 轮）已就位——`mode=production` 时若域名仍为本机/非 HTTPS 会直接中止构建；后续如需上架，按上述 4 步执行即可。

---

## 六、Windows 数据库初始化说明

### 6.1 正确用法

在 **CMD** 中执行（路径含空格时必须加引号）：

```bat
cd /d "C:\Users\57089\Desktop\AI project\AITools\scripts"
init-db.bat
```

或使用 Git Bash / WSL 执行 `init-db.sh`。

### 6.2 常见报错与原因

| 报错现象 | 原因 | 处理 |
|----------|------|------|
| `'USER' 不是内部或外部命令` | 批处理 `%变量%` 与 **LF 换行** 冲突，吞掉下一行首字符 | 使用已修复的 `init-db.bat`（CRLF + `!变量!` 延迟展开） |
| `'cho' 不是内部或外部命令` | 同上，`echo` 的 `e` 被吞 | 同上 |
| `'eai_recipe_category.sql' 不是...` | 路径含空格未加引号，或变量展开截断 | 同上；勿在资源管理器中「打开方式」用 cmd 跑 `.sh` |
| `系统找不到指定的路径` | 项目路径错误或 JeecgBoot 基础 SQL 缺失 | 确认 `jeecgboot-mysql-5.7.sql` 存在于 `JeecgBoot/jeecg-boot/db/` |

### 6.3 脚本已做优化（2026-08-04）

- 全程使用 `EnableDelayedExpansion` 与 `!VAR!`  
- 路径一律加引号，支持 `AI project` 等含空格目录  
- 增加 `where mysql` / `where redis-cli` 检测  
- SQL 导入抽取 `:ImportSqlFile` 子程序，避免 for 循环解析错误  

### 6.4 手动导入备选

若脚本仍失败，可在 MySQL 客户端中逐文件执行：

```sql
SOURCE /path/to/init_homeai_tables.sql;
SOURCE /path/to/init_homeai_menus.sql;
-- 再按需 SOURCE alter_homeai_*.sql
```

---

## 七、文档维护约定

- 每完成一轮迭代：在 **第一节** 追加摘要，在 **第三节** 更新状态列（✅ / ⬜）  
- 新增 SQL：文件名遵循 `alter_homeai_<主题>.sql`，并在 **第二节** 登记  
- 恢复开发前：先执行未跑过的 alter 脚本，再启动后端验证  
- 前端工程化改动：同步更新 **3.0** 与 CI/部署相关说明  

---

*最后更新：2026-08-21 · 第 1～76 轮已归档 · ComfyUI 专项（第 41～43 轮）已拆至 `ComfyUI/`*
