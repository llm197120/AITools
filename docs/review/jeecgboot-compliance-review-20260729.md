---
name: JeecgBoot 合规性审查报告
version: v1
description: 基于 JeecgBoot 开发规范，对全部设计文档进行全面合规审查
---

# JeecgBoot 合规性审查报告

> 审查日期：2026-07-29
> 审查依据：JeecgBoot 开发规范（`.cursor/skills/jeecg-dev/SKILL.md`）
> 审查范围：架构总览、模块详细设计、AI与安全设计、流程图与数据库设计、API文档、线框图（共 6 份文档）

---

## 审查结果概览

| 文档 | 文件 | P0 | P1 | P2 | 合计 |
|------|------|----|----|----|----|
| 架构总览与路线图 | `architecture-overview.md` | 2 | - | - | 2 |
| 模块功能详细设计 | `module-details.md` | 1 | 13 | 10 | 24 |
| AI能力与安全设计 | `ai-security.md` | 2 | 11 | 4 | 17 |
| 流程图与数据库设计 | `database-flows.md` | 3 | 6 | 4 | 13 |
| API接口文档 | `homeai-api-v1.md` | 2 | - | - | 2 |
| 小程序线框图 | `ui-miniapp-v1.md` | 5 | 4 | - | 9 |
| **合计** | | **15** | **34** | **18** | **67** |

---

## P0 级问题（必须修复）

### 1. 架构总览 — 模块路径描述错误
- **文档**: `architecture-overview.md` §3.1
- **问题**: 文档将 `jeecg-boot-module-homeai` 放在 `jeecg-boot-module/` 目录下，但 JeecgBoot 标准中各业务模块是**平行**放在 `jeecg-boot/` 根目录下的同级目录
- **修复**: 修正为 `jeecg-boot/jeecg-boot-module-homeai/`

### 2. 架构总览 — 建表规范缺失
- **文档**: `architecture-overview.md`
- **问题**: 全文未提及建表规范（主键命名、标准审计字段、del_flag、字段注释要求），可能导致后续各模块表结构不统一
- **修复**: 在 §十三 或其他合适位置补充数据库设计规范说明

### 3. 模块详细设计 — 权限编码格式错误
- **文档**: `module-details.md` §6.1
- **问题**: 权限编码使用 `homeai:storage:*list,add,edit,delete*` 格式，通配符+逗号混合格式不被 `@RequiresPermissions` 支持
- **修复**: 拆分为独立权限点，如 `homeai:storage:list`、`homeai:storage:add`、`homeai:storage:delete` 等

### 4. AI与安全 — 加密模式选型风险
- **文档**: `ai-security.md` §8.4
- **问题**: 使用 AES-256-CBC 加密模式，存在已知安全风险（填充预言攻击）
- **修复**: 改为 AES-256-GCM（认证加密模式，自带完整性校验）

### 5. AI与安全 — 搜索方案性能隐患
- **文档**: `ai-security.md` §8.6
- **问题**: 使用 `LIKE %keyword%` 进行文件搜索，数据量增长后会触发全表扫描
- **修复**: 改为 MySQL FULLTEXT INDEX 或引入 ElasticSearch

### 6. 数据库设计 — 主键类型不一致
- **文档**: `database-flows.md` §15.2
- **问题**: 全部 24 张表使用 `varchar(36)`，但 JeecgBoot 标准主键为 `varchar(32)`
- **修复**: 统一改为 `varchar(32)`

### 7. 数据库设计 — 状态字段类型
- **文档**: `database-flows.md` §15.2
- **问题**: 大量使用 `TINYINT` 作为状态/类型字段（如 `is_enabled`、`status`、`is_default`），JeecgBoot 规范要求优先使用 `varchar(1)`/`varchar(2)`
- **修复**: 状态/类型字段改为 `varchar(1)`/`varchar(2)`，枚举值用字母描述

### 8. 数据库设计 — 字段顺序不符合规范
- **文档**: `database-flows.md` §15.2
- **问题**: JeecgBoot 规范的字段顺序为：业务字段 → 审计字段 → 删除标志 → 索引。当前设计存在大量交杂排列
- **修复**: 按标准顺序重新排列各表字段

### 9. API — 权限编码格式错误（同问题3）
- **文档**: `homeai-api-v1.md` 附录
- **问题**: 与模块文档相同的权限编码格式问题
- **修复**: 同步修复

### 10. API — 动词出现在URL路径中
- **文档**: `homeai-api-v1.md` §5.2
- **问题**: 大量 RESTful 违规路径，如 `/files/{id}/download`、`/files/{id}/rename`、`/entries/import/ai-parse` 等
- **修复**: 改为资源型 URL，如 `GET /files/{id}/download` 可保留（框架惯例），但操作型路径应遵循 HTTP 方法语义

### 11. 线框图 — 首页被错误划入分包
- **文档**: `ui-miniapp-v1.md` §三
- **问题**: 首页（tabBar 页面）不能放在分包中，必须位于主包
- **修复**: 将首页路由移除出分包配置

### 12. 线框图 — 分包二体积风险
- **文档**: `ui-miniapp-v1.md` §三
- **问题**: 分包二包含资料存储+Office+账单+计划+烹饪+学习+AI对话，页面数量多，可能接近 2MB 上限
- **修复**: 将资料存储和AI对话拆到细分包，或优化页面组件复用

### 13. 线框图 — 全文档未引用任何API端点
- **文档**: `ui-miniapp-v1.md`
- **问题**: 页面描述了大量操作但未标注对应的 API 接口，导致前后端对接缺乏依据
- **修复**: 在关键操作旁标注对应 API 端点

### 14. 线框图 — 流式对话传输方式未定义
- **文档**: `ui-miniapp-v1.md` §5.2
- **问题**: AI 对话需要 SSE 流式响应，但线框图中未体现数据加载状态和流式渲染方式
- **修复**: 补充 SSE 流式展示设计（打字机效果、"停止生成"按钮等）

### 15. 线框图 — "更多"按钮无目标页面
- **文档**: `ui-miniapp-v1.md` §1.1
- **问题**: 首页导航中的"更多"按钮点击后无明确目标页面，形成导航死胡同
- **修复**: 明确"更多"的目标页，或移除该按钮

---

## P1 级问题（建议修复）— 精选

### 模块详细设计（共13项，精选8项）
| # | 问题 | 说明 |
|---|------|------|
| 1 | 未引用 `jeecg-boot-module-airag` 现有能力 | SSE、poi-tl、多模态上传可复用，需明确"复用vs新增"边界 |
| 2 | 未定义 Java 包结构 | 应定义如 `org.jeecg.modules.homeai.*` 的完整包路径 |
| 3 | 页面路径跨文档不一致 | `module-details.md` 用 `pages-homeai/` 但线框图用 `pages-homeai-core/` |
| 4 | 缺少 Swagger/日志注解规范 | 需补充 `@Operation`、`@AutoLog` 注解使用约定 |
| 5 | 缓存键未定义常量类 | 缓存键格式应集中定义，避免硬编码 |
| 6 | LibreOffice 运维复杂度未评估 | 需要说明 LibreOffice 的安装、监控、故障恢复方案 |
| 7 | 乐观锁配置说明缺失 | 需说明 MyBatis-Plus 乐观锁插件的配置方式 |
| 8 | 文件大小限制标准未统一定义 | 各模块文件大小限制应统一管理 |

### AI与安全设计（共11项，精选6项）
| # | 问题 | 说明 |
|---|------|------|
| 1 | 审计日志未用 `@AutoLog` | 应使用 JeecgBoot 现有 `@AutoLog` 注解体系而非自建 |
| 2 | XSS 未用已有 `XssUtils` | JeecgBoot 已有 `XssFilter` 和 `XssHttpServletRequestWrapper` |
| 3 | 微信消息未接入 `ISendMsgHandle` | JeecgBoot 有统一消息发送接口体系 |
| 4 | AI 架构图未反映实际组件 | 流程图应展示 Spring Boot 层、LangChain4j 层、AI Provider 层的实际调用链 |
| 5 | Token 超额策略仅说明未实现 | 超额后是阻断新请求还是降级为慢速模式，需要明确 |
| 6 | 数据备份恢复缺乏自动化方案 | 当前仅描述了"应有备份"，缺乏自动调度和恢复演练方案 |

### 数据库设计（共6项，精选4项）
| # | 问题 | 说明 |
|---|------|------|
| 1 | 部分表索引覆盖不足 | `homeai_learn_record` 仅有 user_id/material_id 索引，缺 `(user_id, study_date)` 复合索引 |
| 2 | ER 图部分关系不准确 | `wx_user` → `homeai_family_member` 箭头方向应是 `||--o{`（一对多）已正确，但缺 `homeai_family_member` → `homeai_family` 的关系线 |
| 3 | 唯一约束命名已修复但索引命名仍不规范 | 索引 `idx_user_id` 等缺乏表名缩写前缀（应为 `idx_hwu_user_id`） |
| 4 | DDL 中 `ROW_FORMAT=DYNAMIC` 位置应在 COMMENT 之后 | 当前为 `COMMENT='...' ROW_FORMAT=DYNAMIC;` 符合规范 |

### 线框图（共4项）
| # | 问题 | 说明 |
|---|------|------|
| 1 | 表单校验规则缺失 | 只在部分页面标注了校验，应全文档统一覆盖 |
| 2 | 个人中心与家庭管理功能重叠 | "退出家庭"在个人中心和家庭管理页均有入口，需明确主入口 |
| 3 | 账单列表缺少快捷删除 | 当前仅有编辑入口，建议增加左滑删除 |
| 4 | 章节编号错乱 | §四-§十之间部分小节编号与 TOC 不一致 |

---

## 跨文档一致性问题

| # | 问题 | 涉及文档 |
|---|------|---------|
| 1 | 权限编码格式不统一 | `module-details.md`、`homeai-api-v1.md` |
| 2 | 页面路径不一致 | `module-details.md`、`ui-miniapp-v1.md` |
| 3 | API端点描述不完整 | `homeai-api-v1.md`（有完整定义，但线框图未引用） |
| 4 | 数据库表名差异 | `database-flows.md` 中 `homeai_wx_user` 但在架构总览概述中仍是 `wx_user` |

---

## 修复优先级建议

### 第一优先级（编码前必须完成）
1. 权限编码格式统一修复（影响3个文档）
2. 数据库主键类型 `varchar(36)` → `varchar(32)`（影响所有DDL）
3. 状态字段 `TINYINT` → `varchar(1)`/`varchar(2)`
4. AES-256-CBC → AES-256-GCM
5. 线框图分包策略修正
6. 线框图补充API引用

### 第二优先级（设计阶段完成）
7. 审计日志接入 `@AutoLog` 体系
8. XSS 防护接入 JeecgBoot 已有工具类
9. 微信消息接入 `ISendMsgHandle` 体系
10. 搜索方案改为 FULLTEXT INDEX
11. 模块路径描述修正
12. 建表规范补充到架构文档

### 第三优先级（编码阶段完成）
13. 索引命名规范化
14. Swagger/日志注解规范补充
15. 缓存键常量类定义
16. LibreOffice 运维方案
17. 乐观锁配置说明
18. 剩余P2优化项

---

## 结论

本次审查基于 JeecgBoot 开发规范（`.cursor/skills/jeecg-dev/SKILL.md`）对全部 6 份设计文档进行了逐项检查。共发现 **67 项问题/建议**，其中 **P0（必须修复）15 项**、**P1（建议修复）34 项**、**P2（可选优化）18 项**。

核心问题集中在：
1. **权限编码体系**与 JeecgBoot `@RequiresPermissions` 不兼容
2. **数据库设计**与 JeecgBoot 建表规范存在多处偏差（主键类型、状态字段类型、字段顺序）
3. **安全设计**未充分利用 JeecgBoot 已有能力（`@AutoLog`、`XssUtils`、`ISendMsgHandle`）
4. **线框图**与 API 文档割裂，分包策略存在技术风险
5. **跨文档一致性**需要系统性对齐

建议在进入编码阶段前，优先完成第一优先级的修复工作。
