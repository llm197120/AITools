---
name: 家庭AI小工具 - 实现计划
version: v1
---

# 家庭AI小工具 - 实现计划

> 本文档基于设计文档（`docs/design/`）和 API 文档（`docs/api/`）编写，描述从零开始实施的全部阶段和具体任务。

---

## 第一阶段：项目脚手架搭建（预计 3-5 天）

### 目标
搭建可运行的 JeecgBoot Maven 模块 + 管理端菜单 + 小程序框架

### 任务清单

- [x] 创建 `jeecg-boot-module-homeai` Maven 模块
- [x] 注册到 `jeecg-boot-module/pom.xml`
- [x] 添加依赖到 `jeecg-system-start/pom.xml`
- [x] 创建 `HomeaiMybatisPlusConfig`（分页 + 乐观锁）
- [x] 创建 Phase 1 实体：`WxUser`、`Family`、`FamilyMember`、`FamilyInviteCode`
- [x] 创建 Mapper + Service + Controller
- [x] 执行 DDL 建表（24 张表）
- [x] 管理端菜单权限配置
- [x] 小程序工程初始化

---

## 第二阶段：微信登录 + 家庭管理（预计 2 周）

### 目标
完成用户认证和家庭生命周期管理

### 任务清单

- [ ] 微信登录接口：`POST /homeai/user/login`（code 换 JWT）
- [ ] Token 刷新接口：`POST /homeai/user/refresh-token`
- [ ] 家庭 CRUD：创建、编辑、解散、转让管理员
- [ ] 邀请码生成 + 加入家庭
- [ ] 成员管理：列表、移除、修改角色
- [ ] 管理端用户管理 + 家庭管理页面
- [ ] 小程序首页九宫格 + 个人中心页面

### 关键 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/homeai/user/login` | 微信登录 |
| POST | `/homeai/family` | 创建家庭 |
| POST | `/homeai/family/invite-code` | 生成邀请码 |
| POST | `/homeai/family/members` | 加入家庭 |
| GET | `/homeai/family/members` | 成员列表 |
| PUT | `/homeai/family/member/{id}/role` | 修改角色 |
| DELETE | `/homeai/family/disband` | 解散家庭 |
| POST | `/homeai/family/transfer` | 转让管理员 |

---

## 第三阶段：AI 对话模块（预计 3 周）

### 目标
实现完整的 AI 对话能力，包括 SSE 流式、多模态上传、Token 配额

### 任务清单

- [ ] AI 密钥管理（管理端 CRUD，AES-256-GCM 加密）
- [ ] SSE 流式对话接口（发送消息、停止生成、历史记录）
- [ ] 小程序对话页面（消息气泡、Markdown 渲染、附件上传）
- [ ] Token 配额控制（日/月额度，Redis 原子计数）
- [ ] 对话历史管理（重命名、删除、搜索）
- [ ] 管理端 AI 配置页 + 统计概览

### 关键实体

`AiConversation`、`AiMessage`、`AiKeyConfig`、`AiQuotaLog`

---

## 第四阶段：资料存储 + Office 处理（预计 3-4 周）

### 目标
实现文件管理、Office 格式转换、AI 文件生成

### 任务清单

- [ ] 文件夹树形管理 + 可见性控制
- [ ] 文件上传（白名单 + 魔数校验）+ 下载 + 预览
- [ ] 搜索 + 收藏 + 批量操作
- [ ] Office 格式转换（jodconverter + LibreOffice）
- [ ] AI 文件生成（poi-tl 模板填充）
- [ ] 管理端 Office 模板/规则管理页

### 关键实体

`StorageFolder`、`StorageFile`、`OfficeConvertHistory`、`OfficeTemplate`、`ConvertRule`

---

## 第五阶段：账单模块（预计 2 周）

### 目标
实现个人/家庭账单记录、导入、统计

### 任务清单

- [ ] 记一笔 + 账单列表 + 编辑/删除
- [ ] 消费分类管理
- [ ] 批量导入（微信 CSV / Excel / AI 识别）
- [ ] 统计报表（饼图、趋势图）

### 关键实体

`BillEntry`、`BillCategory`、`BillImportRecord`

---

## 第六阶段：日常计划 + 微信提醒（预计 1.5 周）

### 目标
实现计划管理和微信消息推送

### 任务清单

- [ ] 计划 CRUD + 日历视图
- [ ] 重复计划预生成实例（90 天滚动，Quartz 定时任务）
- [ ] 状态切换 + 过期标记
- [ ] 微信订阅消息推送（ISendMsgHandle）
- [ ] 管理端计划查看 + 完成率统计

### 关键实体

`PlanMaster`、`PlanInstance`

---

## 第七阶段：烹饪指南 + 学习模块（预计 2-3 周）

### 目标
实现菜谱管理和学习记录

### 任务清单

- [ ] 菜谱 CRUD + 分类 + 步骤图文 + 做菜视频
- [ ] 食材管理
- [ ] 学习资料管理（视频/图片/PDF/文档）
- [ ] 学习记录（计时/手动双模式）
- [ ] 学习日历视图 + 进度统计

### 关键实体

`Recipe`、`RecipeIngredient`、`RecipeStep`、`LearnMaterial`、`LearnRecord`

---

## 第八阶段：安全加固 + 性能优化（预计 1 周）

### 目标
完成安全防护和性能调优

### 任务清单

- [ ] XSS 过滤配置（`XssFilter`）
- [ ] API 频率限制（`@RateLimit` / Redis Rate Limiter）
- [ ] 操作审计日志（`@AutoLog`）
- [ ] 文件白名单管理页
- [ ] 熔断降级（`@CircuitBreaker` / `@Retryable`）
- [ ] 数据备份定时任务（Quartz）
- [ ] 小程序分包优化
- [ ] 慢查询索引优化

---

## 当前进度

| 阶段 | 状态 |
|------|------|
| 第一阶段：脚手架搭建 | ✅ 已完成 |
| 第二阶段：微信登录 + 家庭管理 | ✅ 已完成 |
| 第三阶段：AI 对话 | ✅ 已完成 |
| 第四阶段：资料存储 + Office | ✅ 已完成 |
| 第五阶段：账单模块 | ✅ 已完成 |
| 第六阶段：日常计划 | ✅ 已完成 |
| 第七阶段：烹饪 + 学习 | ✅ 已完成 |
| 第八阶段：安全加固 | ⏳ 待开始 |
