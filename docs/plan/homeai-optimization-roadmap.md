---
name: 家庭AI小工具 - 迭代优化路线图
version: v1
status: 暂停（建议已归档，待恢复开发时按优先级执行）
updated: 2026-08-04
---

# 家庭AI小工具 - 迭代优化路线图

> 本文档汇总第 **1～12 轮**已完成的优化，以及 **菜谱 / 资料存储 / 学习** 等模块的后续优化建议。  
> **当前状态：优化暂停**，恢复开发时按本文档 P0 → P1 → P2 顺序推进即可。

相关路径索引：

| 领域 | 路径 |
|------|------|
| SQL 脚本 | `JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql/` |
| 后端模块 | `JeecgBoot/.../jeecg-boot-module-homeai/src/main/java/org/jeecg/modules/homeai/` |
| 管理端 | `JeecgBoot/jeecgboot-vue3/src/views/homeai/` |
| 小程序 | `JeecgUniapp/src/pages-homeai-more/` |
| DB 初始化 | `scripts/init-db.bat`（Windows）、`scripts/init-db.sh`（Linux/Mac） |

---

## 一、已完成迭代摘要（第 1～12 轮）

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

## 二、数据库迁移清单（已有库按序执行）

新库直接使用 `init_homeai_tables.sql` + `init_homeai_menus.sql` 即可。

### 必选（第 11～12 轮）

```text
alter_homeai_menus_iteration11.sql      # 操作审计菜单
alter_homeai_recipe_favorite.sql        # 菜谱收藏表
alter_homeai_learn_material_category_id.sql
alter_homeai_learn_record.sql
```

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

---

## 三、后续优化建议（按模块）

优先级说明：**P0** = 安全/数据正确性；**P1** = 体验与一致性；**P2** = 增强功能。

### 3.1 菜谱管理

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P0 | 列表/搜索可见性 | 小程序仅看本人 + 家庭共享 | ✅ 第 11 轮 |
| P0 | 详情鉴权 | `GET /recipe/{id}` 校验可见性 | ✅ 第 12 轮 |
| P0 | 难度字段统一 | 管理端 1～5 整数展示 | ✅ 第 11 轮 |
| P0 | 收藏 API | `homeai_recipe_favorite` + toggle/list | ✅ 第 12 轮 |
| P1 | 食材模型对齐 | 前端 `amount` 与 DB `quantity/unit` 统一 | ⬜ 待做 |
| P1 | 收藏列表入口 | 小程序「我的收藏」Tab / 列表页筛选 | ⬜ 待做 |
| P1 | 创建时 categoryId | 小程序 `add.vue` 与后端分类 ID 完全一致 | ⬜ 待核查 |
| P1 | Excel 导入 | 扩展食材/步骤子表，或文档说明「仅主表」 | ⬜ 待做 |
| P2 | 浏览计数 | `viewCount` 递增 + 热门排行 | ⬜ 待做 |
| P2 | 与计划联动 | 计划实例关联 `recipeId`，一键加入「今日菜谱」 | ⬜ 待做 |
| P2 | 智能推荐 | 按季节、家庭口味、历史做过次数推荐 | ⬜ 待做 |

### 3.2 资料（存储）管理

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P0 | 家庭文件夹树 | 合并 `visibility=family` | ✅ 第 11 轮 |
| P0 | 家庭文件读权限 | 文件夹内文件、搜索、详情 | ✅ 第 12 轮 |
| P0 | 上传 familyId | 创建/上传时绑定家庭 | ✅ 第 12 轮 |
| P1 | 共享目录 UI | 树节点标注「他人创建 / 家庭共享」，防误删 | ⬜ 部分（标签已有，缺上传者） |
| P1 | 文件级 visibility 继承 | 上传至家庭文件夹时默认 `family`（已实现），需文档化 | ✅ 逻辑已有 |
| P1 | 缩略图 | 图片/PDF 首帧 → `thumbnailUrl` | ⬜ 待做 |
| P2 | 回收站 | 软删除 + 恢复（文件/文件夹） | ⬜ 待做 |
| P2 | 用户空间配额 | 按用户/家庭上限 + 告警 | ⬜ 待做 |
| P2 | 小程序上传统一 | 白名单 + `chooseMessageFile` composable，支持非图片 | ⬜ 待做 |

### 3.3 学习管理

| 优先级 | 方向 | 说明 | 状态 |
|--------|------|------|------|
| P0 | 计时会话 Redis | 替代内存 Map，服务重启不丢 | ✅ 第 11 轮 |
| P0 | 小程序计时 UI | 进行中条 + `stop` | ✅ 第 11 轮 |
| P0 | categoryId 迁移 | 实体 + SQL + 管理端/小程序存 ID | ✅ 第 12 轮 |
| P0 | 学习记录表对齐 | `start_time/end_time/duration(秒)/notes` | ✅ 第 12 轮（需执行 alter） |
| P1 | 管理端趋势图 | 近 N 日记录数 + 时长 | ✅ 第 12 轮 |
| P1 | 按分类统计 | 各分类学习时长、资料数排行 | ⬜ 待做 |
| P1 | 资料详情页 | 小程序独立详情 + PDF/视频打开即学 | ⬜ 待做 |
| P2 | 学习提醒 | 每日目标 + 微信订阅消息 | ⬜ 待做 |
| P2 | 管理端多维图表 | 按用户、按分类、7 日/30 日切换 | ⬜ 待做 |

### 3.4 计划 / AI / 审计（简要）

| 模块 | 建议 | 优先级 |
|------|------|--------|
| 计划 | 完成率按 `yearMonth` 筛选 UI；实例清理定时任务 | P1 |
| 计划 | 与菜谱/学习交叉统计（「计划完成 + 学习时长」仪表盘） | P2 |
| AI | 更多场景配额（chat、office、recipe 生成）统一预检 | P1 |
| 审计 | 扩展 module（storage/recipe/learn）；独立权限码 | P1 |

---

## 四、建议恢复开发时的执行顺序

### 第 13 轮（推荐）

1. **菜谱**：小程序收藏列表页；食材 quantity/unit 对齐  
2. **资料**：共享文件夹「上传者」展示；缩略图 POC  
3. **学习**：按分类统计 API + 管理端卡片  

### 第 14 轮（可选）

1. 存储回收站 + 空间配额  
2. 学习资料详情页 + 打开即计时  
3. 计划与菜谱联动  

---

## 五、配置与环境提醒

```yaml
homeai:
  plan:
    repeat-horizon-days: 90
    instance-cleanup-days: 30
    remind-enabled: true
    ai-doc-polish-enabled: true
  wechat:
    plan-remind-template-id: xxx   # 小程序订阅消息模板 ID
  office:
    soffice-path: soffice
homeai.ai.key-encryption-key: xxx  # 必填，AI 密钥加密
```

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

---

*最后更新：2026-08-04 · 已归档第 1～12 轮 · 优化暂停*
