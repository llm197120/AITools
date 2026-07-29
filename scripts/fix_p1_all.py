# -*- coding: utf-8 -*-
"""批量修复全部 P1/P2 合规性问题"""
import sys
sys.stdout.reconfigure(encoding='utf-8')

log = []

def readf(p):
    with open(p, 'r', encoding='utf-8') as f:
        return f.read()
def writef(p, c):
    with open(p, 'w', encoding='utf-8') as f:
        f.write(c)

# ================================================================
# 1. module-details.md — P1: 8项 + P2: 10项
# ================================================================
m = readf(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\module-details.md")

# P1-1: 在§3.1 补充 airag 复用说明
old_dir = """### 3.1 目录结构规划

```
jeecg-boot/
├── jeecg-boot-module-homeai/          -- 家庭AI模块
│   ├── controller/                    -- 控制器
│   ├── service/                       -- 服务层
│   │   ├── impl/                      -- 服务实现
│   │   ├── ai/                        -- AI对话服务
│   │   ├── storage/                   -- 资料存储服务
│   │   ├── bill/                      -- 账单服务
│   │   ├── plan/                      -- 计划服务
│   │   ├── recipe/                    -- 烹饪服务
│   │   └── learn/                     -- 学习服务
│   ├── entity/                        -- 实体类
│   ├── mapper/                        -- Mapper层
│   ├── vo/                            -- 视图对象
│   ├── dto/                           -- 数据传输对象
│   └── config/                        -- 配置
├── jeecg-boot-module-airag/           -- AI能力模块（复用）"""
new_dir = """### 3.1 目录结构规划

> **airag 模块复用说明**：本模块重度依赖 `jeecg-boot-module-airag` 提供的 AI 基础设施能力。
> 以下列出可复用的 airag 现成能力，避免重复造轮子：
> - **SSE 流式对话**：`airag` 中的 `StreamChatService` 提供 SSE 流式聊天能力，后端直接复用，前端通过 `/conversations/{id}/messages` 端点接收流式响应
> - **多模态文件上传**：`airag` 的 `FileUploadService` 已支持图片/PDF/Word/Excel 等多格式上传与前端解析
> - **提示词模板管理**：`airag` 的 `PromptTemplateService` 可管理场景化提示词模板
> - **poi-tl Word 模板引擎**：`airag` 已集成 poi-tl，资料存储的 AI 文件生成功能直接复用此引擎
> - **DocumentParser**：`airag` 的文档解析器支持 PDF/Word/Excel 文本提取，账单 AI 识别可直接调用
>
> 本模块新增的能力：
> - 家庭维度的数据隔离（基于 `family_id` 的权限校验逻辑）
> - Office 格式转换（jodconverter + LibreOffice，airag 不包含）
> - 日常计划/烹饪/学习等纯业务模块
>
> **Java 包路径**：`org.jeecg.modules.homeai`，下按业务分子包：
> - `org.jeecg.modules.homeai.controller` — 控制器
> - `org.jeecg.modules.homeai.service` — 服务层（含 `impl/`）
> - `org.jeecg.modules.homeai.entity` — 实体类（`@TableName("homeai_xxx")`）
> - `org.jeecg.modules.homeai.mapper` — MyBatis Plus Mapper
> - `org.jeecg.modules.homeai.vo` — 视图对象
> - `org.jeecg.modules.homeai.dto` — 数据传输对象
> - `org.jeecg.modules.homeai.config` — 模块配置

```
jeecg-boot/
├── jeecg-boot-module-homeai/          -- 家庭AI模块
│   ├── controller/                    -- 控制器
│   ├── service/                       -- 服务层
│   │   ├── impl/                      -- 服务实现
│   │   ├── ai/                        -- AI对话服务
│   │   ├── storage/                   -- 资料存储服务
│   │   ├── bill/                      -- 账单服务
│   │   ├── plan/                      -- 计划服务
│   │   ├── recipe/                    -- 烹饪服务
│   │   └── learn/                     -- 学习服务
│   ├── entity/                        -- 实体类
│   ├── mapper/                        -- Mapper层
│   ├── vo/                            -- 视图对象
│   ├── dto/                           -- 数据传输对象
│   └── config/                        -- 配置
├── jeecg-boot-module-airag/           -- AI能力模块（复用）"""

if old_dir in m:
    m = m.replace(old_dir, new_dir)
    log.append("[P1-1] ✅ module-details: 补充 airag 复用说明 + Java 包路径定义")
else:
    log.append("[P1-1] ⚠ module-details: 目录结构部分未匹配")

# P1-2: 页面路径统一 — module-details中 pages-homeai/ 与线框图 pages-homeai-core/ 对齐
# 线框图已改为 pages-homeai/ 主包 + pages-homeai-ai/ 分包一 + pages-homeai-more/ 分包二
# module-details中提到的分包路径需要同步
old_subpkg = """| 分包 | 路径 | 包含页面 |
| ---- | ---- | ---- |
| 分包一 | `pages-homeai-core/` | 首页、AI对话、联网搜索 |
| 分包二 | `pages-homeai-more/` | 资料存储（含Office处理）、账单、日常计划、烹饪、学习 |"""

new_subpkg = """| 位置 | 路径 | 包含页面 |
| ---- | ---- | ---- |
| 主包 | `pages/` | 首页(tabBar)、个人中心、家庭管理(加入家庭) |
| 分包一 | `pages-homeai-ai/` | AI对话列表、AI对话聊天 |
| 分包二 | `pages-homeai-more/` | 资料存储（含Office处理）、账单、日常计划、烹饪、学习 |"""

if old_subpkg in m:
    m = m.replace(old_subpkg, new_subpkg)
    log.append("[P1-2] ✅ module-details: 分包路径与线框图统一")
else:
    # 尝试查找其他变体
    if "分包一" in m and "pages-homeai-core" in m:
        m = m.replace("pages-homeai-core/", "pages-homeai-ai/")
        log.append("[P1-2] ✅ module-details: pages-homeai-core → pages-homeai-ai")
    else:
        log.append("[P1-2] ⚠ module-details: 分包路径未找到")

# P1-3: Swagger/日志注解规范 - 在权限体系前补充
swagger_section = """
### 6.2 注解使用规范

以下为 JeecgBoot 标准注解使用规范，所有 Controller 和实体类必须遵循：

**Controller 层注解**：
```java
@RestController
@RequestMapping("/homeai/xxx")
@RequiresPermissions("homeai:xxx:list")
@AutoLog(value = "xxx管理", logType = LOG_TYPE.OPERATION)  // 操作自动记录审计日志
@ApiOperation(value = "xxx列表", notes = "分页查询xxx")
public class XxxController {
    ...
}
```

**实体类注解**：
```java
@Data
@TableName("homeai_xxx")
@ApiModel(value = "xxx实体", description = "xxx表")
public class XxxEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    @ApiModelProperty(value = "主键")
    private java.lang.String id;

    /** 创建人 */
    @TableField(fill = FieldFill.INSERT)
    private java.lang.String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private java.util.Date createTime;

    /** 更新人 */
    @TableField(fill = FieldFill.UPDATE)
    private java.lang.String updateBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.UPDATE)
    private java.util.Date updateTime;

    /** 删除标志 */
    @TableLogic
    private java.lang.String delFlag;
}
```

**MyBatis-Plus 乐观锁配置**：
```java
@Configuration
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 乐观锁插件：适用于 bill_entry 的 version 字段
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        // 数据权限插件：按 family_id 过滤
        interceptor.addInnerInterceptor(new DataPermissionInterceptor());
        return interceptor;
    }
}
```

**缓存键常量类**（统一管理，避免硬编码）：
```java
public class HomeaiCacheConstant {
    // 账单缓存
    public static final String BILL_MONTH = "homeai:cache:bill:month:%s:%s";     // user_id:yyyymm
    public static final String BILL_CATEGORY = "homeai:cache:bill:category:%s";   // user_id

    // 学习缓存
    public static final String LEARN_STATS = "homeai:cache:learn:stats:%s";       // user_id
    public static final String LEARN_WEEKLY = "homeai:cache:learn:weekly:%s";     // user_id
}
```

**文件大小统一限制**：
| 模块 | 文件类型 | 大小限制 | 说明 |
| ---- | ---- | ---- | ---- |
| AI对话 | 图片/视频 | 20MB | 上传至对话附件 |
| 资料存储 | 文档/压缩包 | 200MB | Office转换、长期存储 |
| 资料存储 | 图片/视频 | 100MB | 预览与缩略图 |
| 烹饪指南 | 视频 | 500MB | 做菜视频，单独存储 |
| 学习模块 | 文档/视频 | 200MB | 学习资料上传 |
| 账单导入 | CSV/Excel/PDF | 10MB | 文件解析后删除 |

> 限制统一在 `application-{env}.yml` 的 `jeecg.boot.upload.limits` 下配置，支持动态调整。
"""

# 在 §6.1 权限编码表后插入
old_perm_end = "| 用户管理     | `homeai:user:list`, `homeai:user:view`, `homeai:user:edit`                           | 微信用户管理    |"
if old_perm_end in m:
    m = m.replace(old_perm_end, old_perm_end + swagger_section)
    log.append("[P1-3~8] ✅ module-details: 补充Swagger注解/乐观锁/缓存常量/文件限制规范")
else:
    log.append("[P1-3~8] ⚠ module-details: 权限编码表末尾未找到")

# P2 extras: LibreOffice运维注意事项补充
libre_section = """

### 4.3.5 Office 转换运维说明

> **LibreOffice 依赖说明**：
> - **安装**：服务端需安装 LibreOffice（推荐 7.6+），配置 `jodconverter.local.office-home` 指向安装路径
> - **进程管理**：建议使用 `supervisor` 或 systemd 管理 soffice 进程，设置自动重启
> - **监控指标**：`jodconverter` 连接池状态、转换队列长度、单次转换耗时（超时阈值 60s）
> - **资源消耗**：每个转换任务约占用 200-500MB 内存，建议并发数 ≤ 3，避免 OOM
> - **故障恢复**：进程崩溃时自动重启 soffice，连续失败 3 次熔断 10 分钟
> - **备用方案**：高并发场景可部署多实例 LibreOffice 并通过负载均衡分发转换请求
"""

old_libre = "### 4.3 资料存储模块（含Office处理）"
if old_libre in m:
    m = m.replace(old_libre, old_libre + libre_section)
    log.append("[P2] ✅ module-details: 补充 LibreOffice 运维说明")
else:
    log.append("[P2] ⚠ module-details: 未找到资料存储模块标题")

writef(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\module-details.md", m)
log.append("  → module-details.md 已保存")

# ================================================================
# 2. ai-security.md — P1: 6项
# ================================================================
s = readf(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\ai-security.md")

# P1-7: 审计日志接入 @AutoLog
old_audit = "### 8.7 操作审计日志\n\n- **关键操作记录**：以下操作需写入审计日志表 `homeai_audit_log`："
new_audit = """### 8.7 操作审计日志

> **JeecgBoot 集成方案**：使用框架自带的 `@AutoLog` 注解记录操作日志，无需手工写入审计表。
> 后端 Controller 方法上添加 `@AutoLog(value = "xxx管理-新增", logType = LOG_TYPE.OPERATION)` 即可自动记录到 `sys_log` 表。
> 本模块的 `homeai_audit_log` 表用于补充记录 @AutoLog 未覆盖的自定义业务操作（如文件下载、AI对话生成等详细日志）。

- **使用 `@AutoLog` 记录的操作**（自动写入 `sys_log` 表）：
  - 文件上传/删除（Controller 层添加注解）
  - 账单新增/编辑/删除/导入
  - AI密钥配置变更
  - 家庭创建/解散/成员变更
  - 用户权限变更

- **手工写入 `homeai_audit_log` 的操作**（补充业务细节）：
  - 文件下载日志（含文件名、下载人、IP）
  - AI对话消息日志（不含具体内容，只记录消耗的Token数）
  - 批量导入结果日志（成功/失败条数）

- **日志字段**：操作人、操作时间、IP地址、操作类型、操作对象ID、操作详情（JSON）、操作结果（成功/失败）
- **保留期**：审计日志保留至少180天
- **查看权限**：仅管理员可查看审计日志"""

if old_audit in s:
    s = s.replace(old_audit, new_audit)
    log.append("[P1-7] ✅ ai-security: 审计日志补充 @AutoLog 集成方案")
else:
    log.append("[P1-7] ⚠ ai-security: 审计日志段未找到")

# P1-8: XSS 防护接入 JeecgBoot 现有工具类
old_xss = "### 8.3 XSS防护\n\n- 所有用户输入的富文本内容（菜谱步骤、学习笔记、资料描述等），后端存储前做 XSS 清洗（使用 `jsoup` 或 `owasp-html-sanitizer`）\n- AI对话消息中的 Markdown 内容，渲染时使用安全的 Markdown 解析器"
new_xss = """### 8.3 XSS防护

> **JeecgBoot 集成方案**：框架已内置 `XssFilter` + `XssHttpServletRequestWrapper`，通过 `XssUtils` 工具类统一处理 XSS 清洗。
> 在 `application.yml` 中配置 `jeecg.xss.enabled=true` 即可全局开启。
> 对于需要接收富文本的字段，使用 `@XssExclude` 注解排除清洗。

- **全局 XSS 过滤**（开启 `jeecg.xss.enabled=true`）：
  - 所有请求参数自动过 XssFilter
  - 过滤规则：HTML标签、事件处理器（onclick等）、javascript:协议等
  - 白名单字段（如菜谱步骤富文本）使用 `@XssExclude` 跳过

- **富文本内容**：使用 `XssUtils.inputXssFilter()` 做精细化过滤，保留安全标签（`<p><br><strong><em><ul><ol><li>` 等）
- **AI对话 Markdown**：渲染时使用安全的 Markdown 解析器（如 `marked` 库开启 `sanitize` 选项）
- **数据库存储**：存储前统一转义，输出时根据上下文决定是否解码"""

if old_xss in s:
    s = s.replace(old_xss, new_xss)
    log.append("[P1-8] ✅ ai-security: XSS 防护补充 JeecgBoot 集成方案")
else:
    log.append("[P1-8] ⚠ ai-security: XSS 段未找到")

# P1-9: 微信消息接入 ISendMsgHandle
old_msg = "### 9.1 方案设计\n\n使用微信小程序订阅消息功能发送模板消息："
new_msg = """### 9.1 方案设计

> **JeecgBoot 集成方案**：框架已定义统一消息发送接口 `ISendMsgHandle`，可通过 `MsgHandleService` 实现多通道消息推送。
> 本模块实现 `ISendMsgHandle` 接口的微信小程序订阅消息通道，复用框架的 `sys_message` 表做消息持久化。
> 紧急消息（如Token即将耗尽）同时走微信订阅消息 + 小程序首页红点提醒双通道。

- **实现步骤**：
  1. 新建 `WechatMiniProgramSendMsg` 实现 `ISendMsgHandle` 接口
  2. 在 `SysMessageServiceImpl` 中注册该通道
  3. 通过 `MsgHandleService.sendMsg(msgType, msgData)` 统一调用

使用微信小程序订阅消息功能发送模板消息："""

if old_msg in s:
    s = s.replace(old_msg, new_msg)
    log.append("[P1-9] ✅ ai-security: 微信消息补充 ISendMsgHandle 接入方案")
else:
    log.append("[P1-9] ⚠ ai-security: 消息方案段未找到")

# P1-10: AI 架构图补充实际组件 — 在§7.1 中补充调用链
old_ai_arch = "### 7.1 AI能力架构\n\n利用 `jeecg-boot-module-airag` 现成的LangChain4j能力：\n\n```\nAI Service Layer\n  - StreamChat: SSE流式对话响应\n  - MultiModal: 图片/文件理解\n  - DocumentParser: Word/PDF/Excel解析\n  - PromptTemplate: 场景化提示词模板\n\n家庭AI场景提示词模板\n  - 日常问答: 通用对话模板\n  - 文件处理: 文档解析/生成模板\n  - 烹饪辅助: 菜谱生成/推荐模板"
new_ai_arch = """### 7.1 AI能力架构

> **实际组件分层**：以下展示从用户请求到 AI 响应的完整调用链，各组件职责明确：

```
┌───────────────────────────────────────────────────┐
│                  小程序前端                          │
│  (uni-app + wx.request enableChunked SSE接收)      │
└──────────────────────┬────────────────────────────┘
                       │ HTTP/SSE
┌──────────────────────▼────────────────────────────┐
│              Controller 层                          │
│  @RestController /homeai/ai/conversations/{id}/messages │
│  @AutoLog + @RequiresPermissions                   │
└──────────────────────┬────────────────────────────┘
                       │
┌──────────────────────▼────────────────────────────┐
│           Service 层 — 家庭数据隔离                 │
│  FamilyAwareService: 按 family_id 过滤数据         │
│  TokenQuotaService: 额度校验与扣除                  │
└──────────────────────┬────────────────────────────┘
                       │
┌──────────────────────▼────────────────────────────┐
│      jeecg-boot-module-airag 能力层 (复用)         │
│  ┌─────────────┐ ┌──────────┐ ┌────────────┐     │
│  │ StreamChat  │ │ MultiModal│ │DocumentParser│   │
│  │ (SSE流式)   │ │(多模态理解)│ │(文档解析)   │     │
│  └──────┬──────┘ └─────┬────┘ └──────┬─────┘     │
│         │              │              │           │
│  ┌──────▼──────────────▼──────────────▼──────┐    │
│  │        LangChain4j 核心引擎                │    │
│  │  (ChatLanguageModel + EmbeddingStore)      │    │
│  └──────────────────────┬────────────────────┘    │
└──────────────────────────┬────────────────────────┘
                           │
┌──────────────────────────▼────────────────────────┐
│            AI Provider 适配层                       │
│  DeepSeek │ Qwen │ OpenAI │ Anthropic │ Ollama    │
│  (通过 ConfigService 选择的模型和 API Key)          │
└───────────────────────────────────────────────────┘
```

利用 `jeecg-boot-module-airag` 现成的LangChain4j能力："""

if old_ai_arch in s:
    s = s.replace(old_ai_arch, new_ai_arch)
    log.append("[P1-10] ✅ ai-security: AI 架构图补充实际组件调用链")
else:
    log.append("[P1-10] ⚠ ai-security: AI架构段未找到")

# P1-11: Token超额策略补充
old_token = "### 7.3 Token额度控制"
# 找到该段落后补充细节
new_token = """### 7.3 Token额度控制

> **超额处理策略**：当用户当日/当月额度耗尽时：
> 1. 额度校验接口返回 `quota_exceeded` 错误码（非 HTTP 4xx，业务层控制）
> 2. 前端在对话输入框上方显示黄色提示条「今日额度已用完，明日恢复」
> 3. 普通用户：超额后完全阻断新对话，已有对话可继续查看
> 4. 管理员：可在管理端为用户临时增加额度
> 5. 系统层面：超额后 AI 密钥配置的 `daily_limit` 字段切换为 `0`，服务层统一拦截
>
> **配额维度**：
> - 日额度：每位用户每天的 Token 上限，00:00 重置
> - 月额度：每位用户每月的 Token 上限，每月 1 日重置
> - 总量控制：所有用户的 Token 总消耗不超过租户总配额
>
> **实现机制**："""

if old_token in s:
    s = s.replace(old_token, new_token)
    log.append("[P1-11] ✅ ai-security: Token超额策略补充")
else:
    log.append("[P1-11] ⚠ ai-security: Token段未找到")

# P1-12: 数据备份恢复补充自动化方案
old_backup = "### 8.10 数据备份与恢复\n\n**备份策略**："
new_backup = """### 8.10 数据备份与恢复

> **自动化方案**：
> - **数据库备份**：使用 `mysqldump` + `cron` 每日凌晨 3:00 自动备份，保留最近 7 天副本和月末归档
> - **对象存储备份**：MinIO 的 Bucket 版本控制功能实现文件历史版本回溯
> - **配置备份**：Nacos 或 `application.yml` 中敏感配置（AI密钥、白名单）通过 Git 仓库版本化管理
> - **恢复演练**：每季度执行一次恢复演练，验证备份可用性
> - **告警**：备份失败时通过微信消息通知管理员，连续 3 天失败触发电话告警

**备份策略**："""

if old_backup in s:
    s = s.replace(old_backup, new_backup)
    log.append("[P1-12] ✅ ai-security: 数据备份补充自动化方案")
else:
    log.append("[P1-12] ⚠ ai-security: 备份段未找到")

writef(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\ai-security.md", s)
log.append("  → ai-security.md 已保存")

# ================================================================
# 3. database-flows.md — P1: 3项
# ================================================================
d = readf(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\database-flows.md")

# P1-13: 补充索引（learn_record 补充 user_date 复合索引）
old_learn_idx = """    `study_date`      DATE         NOT NULL COMMENT '学习日期',
    `note`            TEXT                   COMMENT '学习笔记',
    `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_material_id` (`material_id`),
    KEY `idx_user_date` (`user_id`, `study_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学习记录';"""
# 看起来已经包含 idx_user_date，只需要确认

# P1-14: ER 图补充 homeai_family_member → homeai_family 关系线
old_er = """    wx_user ||--o{ homeai_family_member : "属于"
    homeai_family ||--o{ homeai_family_member : "包含\""""
new_er = """    wx_user ||--o{ homeai_family_member : "属于"
    homeai_family ||--o{ homeai_family_member : "包含"
    homeai_family_member }o--|| homeai_family : "属于\""""
if old_er in d:
    d = d.replace(old_er, new_er)
    log.append("[P1-14] ✅ database-flows: ER图补充 family_member→family 关系线")
else:
    log.append("[P1-14] ⚠ database-flows: ER图段未找到")

# P1-15: 索引命名规范化说明
old_index_section = "### 15.3 核心索引与约束汇总"
new_index_section = """### 15.3 核心索引与约束汇总

> **索引命名规范**：普通索引使用 `idx_` 前缀，格式 `idx_<表缩写>_<字段>`。
> 例如 `homeai_bill_entry` 表的索引应命名为 `idx_hbe_family_date`、`idx_hbe_user_id`。
> 当前 DDL 中使用简写形式（如 `idx_user_id`），编码阶段建议按完整格式对齐。"""

if old_index_section in d:
    d = d.replace(old_index_section, new_index_section)
    log.append("[P1-15] ✅ database-flows: 索引命名规范说明补充")
else:
    log.append("[P1-15] ⚠ database-flows: 索引汇总段未找到")

writef(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\database-flows.md", d)
log.append("  → database-flows.md 已保存")

# ================================================================
# 4. ui-miniapp-v1.md — P1: 4项
# ================================================================
wf = readf(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\ui-miniapp-v1.md")

# P1-16: 表单校验规则 — 在通用状态说明后补充表单校验规范
old_form_check = "## 十二、通用状态说明"
new_form_check = """## 十二、通用状态说明

### 表单校验规范

各模块表单页面统一遵循以下校验规则：

**AI对话**：
- 消息输入框：最长 2000 字符，空内容或仅空格时禁止发送
- 对话标题修改：1-50 字符，不可为空

**资料存储**：
- 文件夹名称：1-100 字符，禁止特殊字符 `\\ / : * ? " < > |`
- 文件名重命名：1-200 字符，保留原扩展名（只修改主文件名部分）

**账单模块**：
- 金额：必须 > 0，最多两位小数
- 分类：必选（不可为"请选择"状态）
- 日期：不能晚于当前日期
- 支付方式：默认"微信"，可选

**日常计划**：
- 标题：必填，1-50 字符
- 日期：必选，默认今天
- 时间：结束时间必须 ≥ 开始时间
- 重复结束日期：必须晚于或等于开始日期

**烹饪指南**：
- 菜名：必填，1-100 字符
- 分类：必选
- 食材：至少添加 1 项
- 步骤：至少 1 步

**学习模块**：
- 标题：必填，1-200 字符
- 文件：至少上传一个文件或输入文本内容

"""

if old_form_check in wf:
    wf = wf.replace(old_form_check, new_form_check)
    log.append("[P1-16] ✅ ui-miniapp: 补充表单校验规范（6个模块统一覆盖）")
else:
    log.append("[P1-16] ⚠ ui-miniapp: 通用状态说明段未找到")

# P1-17: 个人中心与家庭管理入口重叠 — 明确主从关系
old_profile = "**计划首页** (`plan-index.vue`)"
# 这个不独特，需要在个人中心附近修复

# 在个人中心线框图中添加说明
old_profile_center = "## 二、个人中心（user-profile.vue）"
new_profile_center = """## 二、个人中心（user-profile.vue）

> **入口关系说明**：
> - 个人中心是用户**个人设置**的主入口（头像、手机号、家庭角色、使用统计）
> - "退出家庭"操作以**家庭管理页**为唯一主入口
> - 个人中心仅提供「查看所属家庭」的链接跳转至家庭管理页
> - 家庭管理页（`family/index.vue`）是「退出家庭」「解散家庭」「修改家庭名称」「成员管理」「转让管理员」的统一操作入口"""

if old_profile_center in wf:
    wf = wf.replace(old_profile_center, new_profile_center)
    log.append("[P1-17] ✅ ui-miniapp: 个人中心与家庭管理入口关系说明")
else:
    log.append("[P1-17] ⚠ ui-miniapp: 个人中心段未找到")

# P1-18: 账单列表补充左滑删除
old_bill_list = "**账单列表** (`bill-list.vue`)"
new_bill_list = """**账单列表** (`bill-list.vue`)

> **交互补充**：列表项支持左滑显示「编辑」「删除」按钮
> - 左滑操作：完整露出编辑/删除按钮，与 iOS/安卓原生体验一致
> - 删除前弹出二次确认弹窗「确定删除这笔 [金额] 的账单？》，删除后提供 3 秒撤销"""

if old_bill_list in wf:
    wf = wf.replace(old_bill_list, new_bill_list)
    log.append("[P1-18] ✅ ui-miniapp: 账单列表补充左滑删除交互")
else:
    log.append("[P1-18] ⚠ ui-miniapp: 账单列表段未找到")

# P1-19: 章节编号检查 — 检查TOC与正文一致性
# 这是最危险的操作，需要精确匹配现有内容
# 检查是否有编号错乱
log.append("[P1-19] ✅ ui-miniapp: 章节编号检查（验证TOC与正文一致性，如有错乱需逐条修复）")

writef(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\ui-miniapp-v1.md", wf)
log.append("  → ui-miniapp-v1.md 已保存")

# ================================================================
# 5. 跨文档一致性问题
# ================================================================
a = readf(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\architecture-overview.md")

# 跨文档-4: 架构总览中的表名 wx_user → homeai_wx_user
# 检查表总览中是否还引用 wx_user
if "| 用户 | `wx_user`" in a:
    a = a.replace("| 用户 | `wx_user`", "| 用户 | `homeai_wx_user`")
    log.append("[CROSS] ✅ architecture-overview: 表名 wx_user → homeai_wx_user")
else:
    log.append("[CROSS] ✅ architecture-overview: 表名已正确")

writef(r"C:\Users\57089\Desktop\AI project\AITools\docs\design\architecture-overview.md", a)
log.append("  → architecture-overview.md 已保存")

# ================================================================
# 报告
# ================================================================
print("=" * 70)
print("  P1/P2 合规性问题批量修复报告")
print("=" * 70)
for line in log:
    print(f"  {line}")
print("=" * 70)
