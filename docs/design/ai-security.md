---
name: AI能力与安全设计
version: v1
---

> 本文档摘自 [architecture-design.md](architecture-design.md) 拆分，内容同步 v8 版本

# AI能力与安全设计

## 目录

- [七、AI能力集成](#七ai能力集成)
  - [7.1 AI能力架构](#7.1-ai能力架构)
  - [7.2 支持模型](#7.2-支持模型)
  - [7.3 Token额度控制](#7.3-token额度控制)
- [八、安全设计](#八安全设计)
  - [8.1 动态配置](#8.1-动态配置)
  - [8.2 文件上传安全](#8.2-文件上传安全)
  - [8.3 XSS防护](#8.3-xss防护)
  - [8.4 敏感信息脱敏与数据加密](#8.4-敏感信息脱敏与数据加密)
  - [8.5 API 频率限制](#8.5-api-频率限制)
  - [8.6 搜索方案](#8.6-搜索方案)
  - [8.7 操作审计日志](#8.7-操作审计日志)
  - [8.8 网络异常处理](#8.8-网络异常处理)
  - [8.9 依赖降级与熔断策略](#8.9-依赖降级与熔断策略)
  - [8.10 数据备份与恢复](#8.10-数据备份与恢复)
- [九、微信消息提醒](#九微信消息提醒)
  - [9.1 方案设计](#9.1-方案设计)
  - [9.2 消息模板](#9.2-消息模板)
  - [9.3 备选方案](#9.3-备选方案)

---

## 七、AI能力集成

### 7.1 AI能力架构

利用 `jeecg-boot-module-airag` 现成的LangChain4j能力：

```
┌─────────────────────────────────────────────┐
│              小程序/管理端                    │
│    HTTP POST + SSE (enableChunked)           │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│        Controller 层 (Spring Boot)          │
│   @RestController + @RequestMapping("/v1")   │
│   参数校验 → 权限校验 → 调用 Service         │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│    AI Service 层 (jeecg-boot-module-airag)  │
│  ┌─────────────────────┐                    │
│  │ StreamChat          │ ← SSE流式对话响应  │
│  │  - EventSource      │   打字机效果       │
│  │  - enableChunked    │   停止生成         │
│  ├─────────────────────┤                    │
│  │ MultiModal          │ ← 图片/文件理解    │
│  │  - 图片分析         │   多模态上传       │
│  │  - 文件内容提取     │                    │
│  ├─────────────────────┤                    │
│  │ DocumentParser      │ ← Word/PDF/Excel   │
│  │  - Apache POI       │   文档解析         │
│  │  - PDFBox           │                    │
│  ├─────────────────────┤                    │
│  │ PromptTemplate      │ ← 场景化提示词     │
│  │  - LiteFlow编排     │   模板管理          │
│  └─────────────────────┘                    │
└──────────────┬──────────────────────────────┘
               │
┌──────────────▼──────────────────────────────┐
│        AI Provider 层                       │
│  DeepSeek │ Qwen │ OpenAI │ Anthropic │ Ollama
│  (通过统一的 LangChain4j ChatLanguageModel) │
└─────────────────────────────────────────────┘
```

**家庭AI场景提示词模板**：
- 日常问答: 通用对话模板，提供基础问答能力
- 文件处理: 文档解析/生成模板，支持 word/pdf/excel 格式
- 烹饪辅助: 菜谱生成/推荐模板，结合食材和口味偏好
- 学习辅助: 知识问答/内容总结模板，支持图片/文档输入
- 账单分析: 消费数据分析模板，按分类/时间维度分析

### 7.2 支持模型


| 提供商        | 适用场景       | 推荐模型           |
| ---------- | ---------- | -------------- |
| DeepSeek   | 日常对话/文件处理  | deepseek-chat  |
| 通义千问(Qwen) | 多模态理解/图片分析 | qwen-vl-plus   |
| OpenAI     | 通用能力       | GPT-4o-mini    |
| Anthropic  | 长文本处理      | Claude 3 Haiku |
| Ollama     | 本地部署(可选)   | 自定义            |


### 7.3 Token额度控制

- 每个用户独立的日/月 **Token 消耗** 限额
- 默认日限额：10,000 Token（约10-20次普通对话）
- 默认月限额：200,000 Token
- 管理后台可随时调整
- 每次对话完成后记录实际 Token 消耗
- **超额策略**：月度额度用完后，当月 AI 对话不可用，下月自动重置
  - 日额度完成 100% → 弹出提示「今日对话次数已用完，明天再来吧」
  - 月额度完成 100% → 弹出提示「本月额度已用完，下月自动重置」
  - 超额后所有 AI 请求返回 `429 Too Many Requests`，前端发送按钮置灰不可用

> **代码实现：集成 `jeecg-boot-module-airag`**
>
> 在 `pom.xml` 中添加以下依赖即可启用 AI 编排与对话能力：
>
> ```xml
> <!-- AI 编排流程模块，提供流程编排、知识库管理、文档解析等能力 -->
> <dependency>
>     <groupId>org.jeecgframework.boot</groupId>
>     <artifactId>jeecg-boot-module-airag</artifactId>
>     <version>${jeecgBootVersion}</version>
> </dependency>
> <!-- AI Starter，封装 LangChain4j 与各模型提供商的统一集成 -->
> <dependency>
>     <groupId>org.jeecgframework.boot</groupId>
>     <artifactId>jeecg-boot-starter-ai</artifactId>
>     <version>${jeecgBootVersion}</version>
> </dependency>
> ```
>
> 引入以上依赖后，通过 `AiModelFactory` 工厂类即可获取配置好的 `ChatLanguageModel` / `StreamingChatLanguageModel` 实例，无需手动管理 API Key 和模型连接池。
>
> **代码实现：SSE 流式对话**
>
> 利用 Spring Boot `SseEmitter` 实现打字机效果的流式响应。Controller 层直接返回 `SseEmitter`，框架自动处理 `text/event-stream` 类型的响应：
>
> ```java
> @RestController
> @RequestMapping("/ai")
> public class AiChatController {
>
>     @Resource
>     private AiChatService aiChatService;
>
>     /**
>      * SSE 流式对话接口
>      * 客户端通过 EventSource 或 fetch + ReadableStream 消费
>      */
>     @PostMapping(value = "/conversations/{id}/messages", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
>     public SseEmitter sendMessage(@PathVariable String id, @RequestBody ChatRequest request) {
>         // 创建 SseEmitter，超时时间 5 分钟
>         SseEmitter emitter = new SseEmitter(300_000L);
>
>         // 通过 LangChain4j StreamingChatLanguageModel 流式调用
>         StreamingChatLanguageModel model = AiModelFactory.getStreamingChatModel(request.getModel());
>         model.generate(request.getMessages(), new StreamingResponseHandler<AiMessage>() {
>             @Override
>             public void onNext(String token) {
>                 try {
>                     emitter.send(SseEmitter.event()
>                             .name("message")
>                             .data(token));
>                 } catch (IOException e) {
>                     emitter.completeWithError(e);
>                 }
>             }
>
>             @Override
>             public void onComplete() {
>                 emitter.complete();
>             }
>
>             @Override
>             public void onError(Throwable error) {
>                 emitter.completeWithError(error);
>             }
>         });
>
>         return emitter;
>     }
> }
> ```
>
> **代码实现：Token 额度控制**
>
> 通过 AOP 切面 + Redis 原子性递增实现每日/每月额度拦截，无需修改业务代码：
>
> ```java
> @Aspect
> @Component
> public class TokenQuotaAspect {
>
>     @Resource
>     private RedisTemplate<String, Object> redisTemplate;
>
>     @Around("@annotation(org.jeecgframework.boot.ai.annotation.CheckTokenQuota)")
>     public Object checkQuota(ProceedingJoinPoint joinPoint) throws Throwable {
>         String userId = SecurityUtils.getCurrentUserId();
>         String dailyKey = "token:quota:daily:" + userId + ":" + LocalDate.now();
>         String monthlyKey = "token:quota:monthly:" + userId + ":" + YearMonth.now();
>
>         // Redis INCR 原子递增，首次创建时设置 TTL 自动过期
>         Long dailyUsed = redisTemplate.opsForValue().increment(dailyKey);
>         if (dailyUsed == 1) {
>             redisTemplate.expire(dailyKey, 1, TimeUnit.DAYS);
>         }
>         Long monthlyUsed = redisTemplate.opsForValue().increment(monthlyKey);
>         if (monthlyUsed == 1) {
>             redisTemplate.expire(monthlyKey, 30, TimeUnit.DAYS);
>         }
>
>         int dailyLimit = SystemConfigCache.getInt("ai.token.daily.limit", 10000);
>         int monthlyLimit = SystemConfigCache.getInt("ai.token.monthly.limit", 200000);
>
>         if (dailyUsed > dailyLimit || monthlyUsed > monthlyLimit) {
>             throw new QuotaExceededException("429 Too Many Requests: 额度已用完");
>         }
>
>         return joinPoint.proceed();
>     }
> }
> ```
>
> 在需要额度控制的 Service 方法上添加 `@CheckTokenQuota` 注解即可。额度阈值通过系统配置表动态管理，修改后即时生效。

## 八、安全设计

### 8.1 动态配置

以下关键参数支持运行期动态修改（不重启服务器，配置存储在 Redis，前端通过 API 获取最新值）：

| 配置项 | 示例值 | 说明 |
| ---- | ---- | ---- |
| AI 默认模型 | deepseek-chat | 管理端切换，对话启动时读取最新配置 |
| 每日 Token 限额 | 10000 | 修改后即时生效 |
| 每月 Token 限额 | 200000 | 修改后即时生效 |
| 文件格式白名单 | .jpg,.png,.pdf,.docx,.doc,.xlsx,.xls,.pptx,.ppt,.mp4,.avi,.mov,.zip,.rar | 前端启动时同步，缓存5分钟 |
| 单文件大小上限 | 100MB（视频200MB） | 前端同步校验 |
| 密钥轮换 | 90天轮换，保留旧密钥30天 | 双密钥机制 |

**白名单同步机制**：前端每次进入文件上传页前调 `/homeai/config/file-whitelist` 获取最新白名单，动态设置上传组件的 `accept` 属性和前端校验规则。本地缓存 5 分钟。

> **代码实现：JeecgBoot 动态配置热加载**
>
> 使用 `@ConfigurationProperties` + `@RefreshScope` 实现运行期动态刷新，配置变更后调用 `ContextRefresher.refresh()` 即可触发 Bean 重新绑定：
>
> ```java
> @Component
> @ConfigurationProperties(prefix = "homeai.config")
> @RefreshScope
> public class HomeAiConfig {
>     private String defaultAiModel = "deepseek-chat";
>     private int dailyTokenLimit = 10000;
>     private int monthlyTokenLimit = 200000;
>     private String fileWhitelist = ".jpg,.png,.pdf,.docx";
>     private long singleFileSizeMB = 100;
>     // Lombok @Data 生成 getter / setter
> }
> ```
>
> 配置值存储于 Redis（通过 JeecgBoot 的 `SystemConfigCache` 读取），管理端页面修改配置时调用 API 将新值写入 Redis 并触发 `ConfigRefreshContext.refresh()`。前端通过 `/homeai/config/query` 接口获取最新配置，本地缓存 5 分钟。

### 8.2 文件上传安全

- **白名单机制**：管理端配置允许的文件扩展名白名单
- 默认白名单：`jpg, jpeg, png, gif, bmp, pdf, doc, docx, xls, xlsx, ppt, pptx, zip, rar, txt, csv, md`
- 严格禁止：可执行文件、脚本文件、库文件、配置文件
- 上传时做双重校验：前端扩展名检查 + 后端文件头魔数校验（防止扩展名伪造）

> **代码实现：JeecgBoot 文件上传安全**
>
> 使用 JeecgBoot 的 `CommonUtil.upload()` 方法，该方法内置了扩展名校验 + 文件头魔数校验双重防护。通过 `UploadConfig` 配置类管理白名单：
>
> ```java
> @Configuration
> @ConfigurationProperties(prefix = "jeecg.upload")
> public class UploadConfig {
>     /** 单文件大小上限（MB） */
>     private Long maxFileSize = 100L;
>     /** 允许的扩展名白名单 */
>     private List<String> allowedExtensions = Arrays.asList(
>         "jpg", "jpeg", "png", "gif", "bmp",
>         "pdf", "doc", "docx", "xls", "xlsx",
>         "ppt", "pptx", "zip", "rar", "txt", "csv", "md"
>     );
>     // getter / setter ...
> }
> ```
>
> 实际上传时调用 `CommonUtil.upload(file, "homeai/upload")`，框架内部通过 `FileMagicNumberUtil` 读取文件头魔数判断真实类型，与扩展名比对，双重防止扩展名伪造攻击。上传成功后返回文件存储路径和 URL。

### 8.3 XSS防护

- 所有用户输入的富文本内容（菜谱步骤、学习笔记、资料描述等），后端存储前做 XSS 清洗：使用 JeecgBoot 内置的 `XssFilter` + `XssHttpServletRequestWrapper` 自动过滤，无需手动调用。需要跳过 XSS 过滤的字段加 `@XssIgnore` 注解
- AI对话消息中的 Markdown 内容，渲染时使用安全的 Markdown 解析器

> **代码实现：JeecgBoot XSS 过滤注册**
>
> JeecgBoot 默认通过 `XssFilter` + `XssHttpServletRequestWrapper` 对所有 HTTP 请求参数进行 XSS 清洗。通过 `FilterRegistrationBean` 注册（已内置于 `XssFilterConfig`）：
>
> ```java
> @Bean
> public FilterRegistrationBean<XssFilter> xssFilterRegistration() {
>     FilterRegistrationBean<XssFilter> registration = new FilterRegistrationBean<>();
>     registration.setFilter(new XssFilter());
>     registration.addUrlPatterns("/*");
>     registration.setName("xssFilter");
>     registration.setOrder(1);
>     registration.addInitParameter("exclusions", "/ai/conversations/*"); // 排除 AI 流式对话接口（消息含特殊字符）
>     return registration;
> }
> ```
>
> 对于需要存储富文本的字段（如菜谱步骤、学习笔记），在实体属性上添加 `@XssIgnore` 注解即可跳过 XSS 过滤，框架保留原始 HTML 标签。富文本在前端渲染时交由安全 Markdown 解析器处理，防止 XSS 注入：
>
> ```java
> public class Recipe {
>     @XssIgnore  // 跳过 XSS 过滤，保留 HTML 格式
>     private String steps;
>
>     private String name;  // 普通字段保持 XSS 过滤
> }
> ```

**操作反馈规范**：

- **成功操作**：绿色浅底 Toast「操作成功」，2秒自动消失（适用于：收藏/取消、复制、保存等）
- **失败操作**：红色浅底 Toast「操作失败：[原因]」（适用于：网络错误、权限不足、数据冲突等）
- **批量操作**：完成后汇总通知「X项成功，Y项失败」，失败项可查看详情
- **按钮防重复**：所有触发异步请求的按钮点击后立即变为 loading 态（按钮内 spinner + 文案变更），操作完成前不可重复点击
- **删除操作**：所有删除操作弹出二次确认弹窗（确认/取消），删除成功后提供 3 秒撤销窗口

### 8.4 敏感信息脱敏与数据加密

- 管理端查看对话记录时，自动检测并脱敏：手机号、身份证号、银行卡号、邮箱
- AI密钥在前端脱敏显示（`sk-****xxxx`），API返回时截断
- 文件下载日志记录但不抓取文件内容
- **对话内容加密存储**：对话消息表中的 `content` 字段使用 AES-256-GCM 加密后存储（GCM 为认证加密模式，自带完整性校验，安全性优于 CBC）（使用独立的加密密钥，与 AI 密钥密钥隔离）。管理端查看对话时解密显示，API 返回给小程序时解密。加密密钥由系统配置文件管理，支持定期轮换

> **代码实现：JeecgBoot 加解密工具**
>
> 使用 JeecgBoot 提供的 `AesUtil` 或 `EncryptUtil` 工具类对敏感字段进行加密存储。建议封装 MyBatis-Plus TypeHandler，在 ORM 层面自动完成加解密，业务代码无感知：
>
> ```java
> /**
>  * 自定义 MyBatis TypeHandler，写入时自动加密，读取时自动解密
>  */
> public class AesEncryptHandler implements TypeHandler<String> {
>
>     private static final String KEY = EncryptionKeyHolder.getKey();
>
>     @Override
>     public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
>         ps.setString(i, AesUtil.encrypt(parameter, KEY));
>     }
>
>     @Override
>     public String getResult(ResultSet rs, String columnName) throws SQLException {
>         String value = rs.getString(columnName);
>         return value == null ? null : AesUtil.decrypt(value, KEY);
>     }
>
>     @Override
>     public String getResult(ResultSet rs, int columnIndex) throws SQLException {
>         String value = rs.getString(columnIndex);
>         return value == null ? null : AesUtil.decrypt(value, KEY);
>     }
>
>     @Override
>     public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
>         String value = cs.getString(columnIndex);
>         return value == null ? null : AesUtil.decrypt(value, KEY);
>     }
> }
> ```
>
> 在实体字段上标注使用该 TypeHandler：
>
> ```java
> @TableName("homeai_conversation_message")
> public class ConversationMessage {
>     @TableField(typeHandler = AesEncryptHandler.class)
>     private String content;  // 对话内容，写入自动 AES-256-GCM 加密，读取自动解密
> }
> ```
>
> 加密密钥通过 `jeecg.encrypt.key` 配置项管理，由 `EncryptionKeyHolder` 持有。轮换时采用双密钥机制：新密钥加密新写入数据，旧密钥保留 30 天用于解密历史数据。

### 8.5 API 频率限制

- 对关键写操作端点启用基于 Redis 的令牌桶限流，防止滥用：
  - 文件上传接口：每分钟最多 10 次
  - 账单录入接口：每分钟最多 30 次
  - 数据导入接口：每分钟最多 3 次
  - **邀请码校验接口**：每分钟最多 5 次/IP，连续 10 次校验失败后封禁该 IP 30 分钟
- 超过限制返回 `429 Too Many Requests`，前端显示友好提示

> **代码实现：JeecgBoot API 频率限制**
>
> **方式一（内置注解）**：在方法上添加 `@RateLimit` 注解（若框架已集成）：
>
> ```java
> @RateLimit(limit = 10, timeUnit = TimeUnit.MINUTES, message = "文件上传过于频繁，请稍后再试")
> @PostMapping("/upload")
> public Result<String> upload(MultipartFile file) { ... }
> ```
>
> **方式二（Redisson 分布式限流器）**：使用 Redisson 的 `RRateLimiter` 实现分布式令牌桶，适合多实例部署场景：
>
> ```java
> @Component
> public class DistributedRateLimiter {
>     @Resource
>     private RedissonClient redissonClient;
>     private static final Map<String, RateLimiterConfig> LIMIT_CONFIG = Map.of(
>         "UPLOAD",   new RateLimiterConfig(10, 60),   // 文件上传：每分钟 10 次
>         "BILL_IN",  new RateLimiterConfig(30, 60),   // 账单录入：每分钟 30 次
>         "IMPORT",   new RateLimiterConfig(3, 60),    // 数据导入：每分钟 3 次
>         "INVITE",   new RateLimiterConfig(5, 60)     // 邀请码校验：每分钟 5 次
>     );
>
>     public boolean tryAcquire(String key, String userId, String ip) {
>         RateLimiterConfig config = LIMIT_CONFIG.get(key);
>         if (config == null) return true;
>         // 按 IP 维度限流（也可按用户维度）
>         String rateKey = "rate:limit:" + key + ":" + ip;
>         RRateLimiter limiter = redissonClient.getRateLimiter(rateKey);
>         limiter.trySetRate(RateType.OVERALL, config.rate, config.interval, RateIntervalUnit.SECONDS);
>         return limiter.tryAcquire();
>     }
> }
> ```
>
> **邀请码防刷**：对于邀请码校验接口，连续 10 次失败将 IP 加入 Redis 封禁集合（`blocked:ip`），TTL 30 分钟，封禁期间直接返回 `429` 拒绝服务。

### 8.6 搜索方案

- **资料存储搜索**：文件名模糊匹配使用 MySQL FULLTEXT INDEX（建表时为 `original_name` 字段添加全文索引 `FULLTEXT KEY idx_hw_file_name_fulltext (original_name) WITH PARSER ngram`），支持更高效的中文分词搜索。数据量超10万后可引入 Elasticsearch
- **账单搜索**：按日期+分类+备注组合查询，使用数据库索引优化
- **菜谱搜索**：按菜名使用 FULLTEXT INDEX 搜索，配合分类筛选
- **扩展方案**：后期数据量增大后可引入 Elasticsearch 或 MeiliSearch 实现全文检索
- **搜索安全**：所有搜索关键词使用 MyBatis-Plus 参数化查询（`QueryGenerator` 自动处理参数绑定），防止 SQL 注入攻击；查询结果在前端渲染时由 XssFilter 做输出编码，防止 XSS 攻击

> **代码实现：JeecgBoot 搜索支持**
>
> 日常搜索使用 JeecgBoot 的 `QueryGenerator` 工具类，前端通过 URL 参数（`?column=value`）自动拼装 MyBatis-Plus 查询条件，无需手写 SQL。文件名模糊搜索利用 MySQL FULLTEXT INDEX：
>
> ```java
> @Service
> public class StorageFileService {
>
>     public IPage<StorageFile> search(String keyword, Page<StorageFile> page) {
>         LambdaQueryWrapper<StorageFile> wrapper = new LambdaQueryWrapper<>();
>         wrapper.apply("MATCH(original_name) AGAINST({0} IN BOOLEAN MODE)", keyword);
>         return baseMapper.selectPage(page, wrapper);
>     }
> }
> ```
>
> 数据量超过 10 万后可引入 `jeecg-boot-module-elasticsearch` 模块，将文件元数据同步至 ES 实现毫秒级全文检索。所有搜索关键词在进入 SQL 前已由 `XssFilter` 过滤。

### 8.7 操作审计日志

- **关键操作记录**：以下操作需记录审计日志：
  - 文件上传/删除/下载
  - 账单新增/编辑/删除/导入
  - AI密钥配置变更
  - 家庭创建/解散/成员变更
  - 用户权限变更
- **实现方式**：使用 JeecgBoot 内置 `@AutoLog(value="xxx", logType=LOG_TYPE.OPERATION)` 注解自动记录到 `sys_log` 表。
  同时自定义写 `homeai_audit_log` 表存储业务字段详情（操作对象ID、摘要等），两者配合使用
- **日志字段**：操作人、操作时间、IP地址、操作类型、操作对象ID、操作详情（JSON）、操作结果（成功/失败）
- **保留期**：审计日志保留至少180天
- **查看权限**：仅管理员可查看审计日志

> **代码实现：JeecgBoot 操作审计日志**
>
> 在 Controller 方法上添加 `@AutoLog` 注解即可自动记录到 `sys_log` 表，无需手写日志代码：
>
> ```java
> @AutoLog(value = "文件上传", logType = LOG_TYPE.OPERATION)
> @PostMapping("/upload")
> public Result<String> upload(MultipartFile file) { ... }
> ```
>
> 如需记录详细的业务字段（操作对象 ID、操作摘要等），通过自定义 AOP 切面将业务详情写入 `homeai_audit_log` 扩展表。`@AutoLog` 注解支持在 `doEnd` 回调中获取方法返回值，实现 `sys_log` 与业务扩展日志的关联：
>
> ```java
> @Component
> public class AuditLogAspect {
>
>     @AfterReturning(value = "@annotation(autoLog)", returning = "result")
>     public void doAfterReturning(JoinPoint point, AutoLog autoLog, Object result) {
>         // 获取返回值中的业务主键，写入扩展日志表
>         String bizId = extractBizId(result);
>         auditLogService.saveExtLog(bizId, autoLog.value(), getDetail(point));
>     }
> }
> ```

### 8.8 网络异常处理

微信小程序运行在移动网络环境下，网络切换、断连是常态场景，需设计完整的网络异常处理策略：

- **全局网络状态监听**：`wx.onNetworkStatusChange` 监听网络变化
  - 断网时：页面顶部显示红色横条「网络连接已断开」，所有写操作按钮置灰禁点
  - 恢复时：显示绿色横条「网络已恢复」3秒后消失，自动重试失败队列中的请求
- **请求超时处理**：所有 API 请求超时时间 15 秒，超时后显示 Toast「请求超时，请检查网络后重试」
- **大文件上传**：上传超过 20MB 的文件前检测网络类型，WiFi 直接上传，移动网络弹出确认提示
- **SSE 流式对话**：连接中断时保留已接收内容，显示「连接中断，点击继续」按钮；连续 3 次重连失败后提示用户重新发送
- **离线缓存**：账单录入、计划创建等高频操作支持离线暂存（本地 Storage），网络恢复后自动同步

> **代码实现：JeecgBoot 重试与健康检测**
>
> 后端使用 Spring `@Retryable` 注解实现 API 调用的自动重试，配合指数退避策略避免雪崩：
>
> ```java
> @Service
> public class AiApiService {
>
>     @Retryable(
>         retryFor = {IOException.class, TimeoutException.class},
>         maxAttempts = 3,
>         backoff = @Backoff(delay = 1000, multiplier = 2)
>     )
>     public String callAiModel(String prompt) {
>         // 调用 AI 模型 API，可能抛出 IOException 或 TimeoutException
>     }
>
>     @Recover
>     public String recover(Throwable e, String prompt) {
>         log.error("AI 调用重试耗尽: {}", e.getMessage());
>         return "AI 服务暂不可用，请稍后再试";
>     }
> }
> ```
>
> 配合 Spring Boot Actuator 的健康检查端点 `/actuator/health`，前端可轮询检测后端服务状态，网络恢复后自动重试失败队列中的离线请求。

### 8.9 依赖降级与熔断策略

关键外部依赖不可用时的降级方案：

| 依赖 | 不可用场景 | 降级策略 |
| ---- | ---- | ---- |
| AI 模型 API（全部） | 所有配置模型均返回错误或超时 | 返回友好提示「AI 服务暂不可用，请稍后再试」；前端展示静态提示页，记录告警 |
| 微信登录 API | `wx.login()` 长时间无响应 | 已登录用户凭缓存 JWT 继续使用（7天内有效）；新用户显示「微信登录服务异常，请稍后重试」 |
| LibreOffice 转换 | 进程崩溃、OOM 或端口被占用 | 前端提示「文件转换服务异常」，自动重启 soffice 进程；连续失败3次则熔断10分钟 |
| 微信订阅消息 | 频率限制导致推送失败 | 失败消息入重试队列（指数退避，最多3次）；超限消息降级为小程序首页弹窗提醒 |
| 对象存储（MinIO） | 上传/下载失败 | 文件上传暂存本地临时目录，网络恢复后自动同步；文件下载提示「文件服务异常，请稍后重试」 |

- 集成 Resilience4j CircuitBreaker 实现核心 API 调用的熔断保护
- 熔断状态变更时通过 WebSocket 通知管理员

> **代码实现：JeecgBoot 熔断降级**
>
> 集成 Resilience4j，使用 `@CircuitBreaker` 注解声明熔断规则：
>
> ```java
> @Service
> public class AiModelService {
>
>     @CircuitBreaker(name = "aiModelApi", fallbackMethod = "fallback")
>     public String callModel(String modelName, String prompt) {
>         // 调用外部 AI 模型 API
>     }
>
>     public String fallback(String modelName, String prompt, Throwable t) {
>         log.warn("AI 模型 [{}] 熔断降级: {}", modelName, t.getMessage());
>         return "AI 服务暂不可用，请稍后再试";
>     }
> }
> ```
>
> 在 `application.yml` 中配置熔断参数：
>
> ```yaml
> resilience4j.circuitbreaker:
>   instances:
>     aiModelApi:
>       slidingWindowSize: 10            # 滑动窗口大小
>       minimumNumberOfCalls: 5           # 最少调用次数
>       failureRateThreshold: 50          # 失败率阈值（%）
>       waitDurationInOpenState: 10m      # 熔断持续时间
>       permittedNumberOfCallsInHalfOpenState: 3  # 半开状态允许的调用数
> ```
>
> 熔断状态变更事件通过 Spring `ApplicationEventPublisher` 发布，由 WebSocket 实时推送到管理端告警面板。管理者可在管理后台手动重置熔断器。

### 8.10 数据备份与恢复

**备份策略**：

- **数据库**：每日全量备份 + 每 6 小时增量备份，保留 30 天（使用系统 crontab 或云数据库自动备份功能）
- **对象存储（MinIO/OSS）**：启用版本控制（bucket versioning），支持文件级历史回滚
- **备份验证**：每月进行一次恢复演练，验证备份可用性

**自动化方案**：
- **自动调度**：通过 Quartz 定时任务实现 `DatabaseBackupJob`（每日凌晨2点执行 `mysqldump`），`FileBackupJob`（增量+全量文件同步）
- **恢复脚本**：`restore.sh` 脚本支持一键恢复指定时间点（`./restore.sh --date=2026-07-28 --point-in-time=true`），含前置检查（备份文件完整性校验、磁盘空间检查）
- **告警配置**：备份失败 → 企业微信/钉钉机器人告警；连续 3 天备份失败 → 电话告警值班人员

**误删除恢复机制**：

- **核心数据软删除**：以下数据表增加 `is_deleted`（tinyint）、`deleted_at`（datetime）、`deleted_by`（varchar）字段：
  - 账单表（bill_entry）
  - 文件表（storage_file）
  - 菜谱表（recipe）
  - 每日计划表（plan_instance）
  - AI 对话表（ai_conversation）
- 删除操作仅标记 `is_deleted=1`，物理删除延迟 30 天（由定时任务清理）
- 管理端提供「回收站」页面，支持按模块查看和恢复 30 天内删除的数据
- **家庭解散数据保留**：复用软删除标记机制，家庭解散时将共享数据的 `family_id` 置空并将 `is_deleted` 标记为 `2`（特殊值，表示"因家庭解散而删除"）；30 天后随常规软删除清理一起物理删除
  - 注意：家庭解散 30 天保留期与软删除 30 天使用**同一套定时任务**，不额外增加维护复杂度
  - 保留期结束前 7 天向所有成员推送微信订阅消息：「您的家庭数据即将到期删除，请及时备份」

> **代码实现：JeecgBoot 定时备份任务**
>
> 使用 JeecgBoot 内置的 Quartz 调度框架创建数据库备份定时任务。实现 `org.quartz.Job` 接口：
>
> ```java
> @Component
> public class DatabaseBackupJob implements Job {
>
>     @Value("${spring.datasource.dynamic.datasource.master.url}")
>     private String jdbcUrl;
>     @Value("${spring.datasource.dynamic.datasource.master.username}")
>     private String dbUser;
>     @Value("${spring.datasource.dynamic.datasource.master.password}")
>     private String dbPass;
>
>     @Override
>     public void execute(JobExecutionContext context) throws JobExecutionException {
>         // 从 JDBC URL 解析数据库名
>         String dbName = extractDbName(jdbcUrl);
>         String backupFile = String.format("/backup/db_%s.sql",
>                 LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")));
>
>         String cmd = String.format("mysqldump -h%s -u%s -p%s %s --routines --triggers > %s",
>                 extractHost(jdbcUrl), dbUser, dbPass, dbName, backupFile);
>
>         try {
>             Process process = Runtime.getRuntime().exec(cmd);
>             int exitCode = process.waitFor();
>             if (exitCode != 0) {
>                 // 推送告警：企业微信 / 钉钉机器人
>                 AlertUtil.send("数据库备份失败", "退出码: " + exitCode);
>             }
>         } catch (Exception e) {
>             throw new JobExecutionException(e);
>         }
>     }
> }
> ```
>
> 在 `sys_quartz_job` 表中注册 Job 类，通过管理端「定时任务」页面可视化配置触发规则（Cron 表达式）。文件备份同样以 Quartz Job 实现，通过 `@Scheduled` 注解配合 MinIO SDK 完成增量同步。

## 九、微信消息提醒

### 9.1 方案设计

通过微信小程序订阅消息实现计划提醒。使用 JeecgBoot 统一消息接口 `ISendMsgHandle`（`WxSubscribeMsgHandle` 实现类），发送前调用 `wx.requestSubscribeMessage` 检查用户订阅状态：

1. 创建带提醒的计划时，引导用户授权订阅
2. 后端Quartz定时任务每分钟扫描需提醒的计划实例
3. 调用微信订阅消息API推送
4. 标记已提醒，避免重复

### 9.2 消息模板


| 模板类型 | 触发时机     | 消息内容             |
| ---- | -------- | ---------------- |
| 计划提醒 | 计划时间前N分钟 | 计划标题 + 时间 + 内容摘要 |
| 成员变更 | 新成员加入/成员退出 | [成员名称] 已[加入/退出] [家庭名称] |
| Token不足 | 额度低于20% | 您今日AI额度即将用完，剩余XX Token |
| 账单提醒 | 月末未记账提醒（可选） | 本月还有N天，记得记账哦 |


### 9.3 备选方案

- 用户打开小程序时，首页弹窗展示今日未完成的待办计划
- 服务号模板消息（需要申请服务号并与小程序绑定）

> **代码实现：JeecgBoot 统一消息推送体系**
>
> JeecgBoot 通过 `ISendMsgHandle` 接口抽象了多通道消息发送能力，`WxSubscribeMsgHandle` 是其微信订阅消息的具体实现：
>
> ```java
> public interface ISendMsgHandle {
>     void sendMsg(String receiver, String title, String content);
> }
> ```
>
> 在消息发送端，通过 `MessageRouter` 根据消息类型（`TemplateType` 枚举）自动选择推送渠道：
>
> ```java
> @Service
> public class MessageRouter {
>     @Resource
>     private WxSubscribeMsgHandle wxSubscribeMsgHandle;  // 微信订阅消息实现
>
>     public void route(TemplateType type, String openId, String title, String content) {
>         // 发送前检查用户是否已订阅该模板
>         // 若未订阅，降级为小程序首页弹窗提醒
>         switch (type) {
>             case PLAN_REMINDER:
>             case MEMBER_CHANGE:
>             case TOKEN_LOW:
>             case BILL_REMINDER:
>                 wxSubscribeMsgHandle.sendMsg(openId, title, content);
>                 break;
>             // 其他消息类型可扩展邮件、站内信等渠道
>             default:
>                 log.warn("未知消息类型: {}", type);
>         }
>     }
> }
> ```
>
> 消息消费由 Quartz 定时任务触发，每分钟扫描 `sys_sms_template` 表中待发送的记录，调用 `ISendMsgHandle` 实际推送。发送完成后更新状态为「已发送」，失败消息进入重试队列（指数退避，最多 3 次）。管理端可在「消息管理」页面查看发送记录、失败原因和重试日志。
> 
