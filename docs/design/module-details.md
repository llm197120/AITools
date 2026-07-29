---
name: 模块功能详细设计
version: v1
---

> 本文档摘自 [architecture-design.md](architecture-design.md) 拆分，内容同步 v8 版本

# 模块功能详细设计

## 目录

- [四、模块功能详细说明](#四模块功能详细说明)
  - [4.1 小程序首页 + 微信用户管理](#4.1-小程序首页-+-微信用户管理)
    - [小程序端](#小程序端)
    - [管理端](#管理端)
  - [4.2 AI对话模块](#4.2-ai对话模块)
    - [功能概述](#功能概述)
    - [小程序端](#小程序端)
    - [管理端](#管理端)
    - [业务规则](#业务规则)
  - [4.3 资料存储模块（含Office处理）](#4.3-资料存储模块含office处理)
    - [功能概述](#功能概述)
    - [技术实现路径（Office处理部分）](#技术实现路径office处理部分)
    - [小程序端](#小程序端)
    - [管理端](#管理端)
    - [业务规则](#业务规则)
  - [4.4 账单汇总与记录模块](#4.4-账单汇总与记录模块)
    - [功能概述](#功能概述)
    - [小程序端](#小程序端)
    - [管理端](#管理端)
    - [业务规则](#业务规则)
  - [4.5 日常计划模块](#4.5-日常计划模块)
    - [功能概述](#功能概述)
    - [小程序端](#小程序端)
    - [管理端](#管理端)
    - [重复计划的实现策略](#重复计划的实现策略)
    - [微信消息提醒](#微信消息提醒)
    - [业务规则](#业务规则)
  - [4.6 烹饪指南模块](#4.6-烹饪指南模块)
    - [功能概述](#功能概述)
    - [小程序端](#小程序端)
    - [管理端](#管理端)
    - [业务规则](#业务规则)
  - [4.7 学习模块](#4.7-学习模块)
    - [功能概述](#功能概述)
    - [小程序端](#小程序端)
    - [管理端](#管理端)
    - [业务规则](#业务规则)
- [五、接口设计 (API)](#五接口设计-(api))
  - [5.1 接口规范速览](#5.1-接口规范速览)
  - [5.2 按模块API数量统计](#5.2-按模块api数量统计)
  - [5.3 完整API文档](#5.3-完整api文档)
- [六、权限体系](#六权限体系)
  - [6.1 管理端菜单与权限编码](#6.1-管理端菜单与权限编码)
  - [6.2 小程序端权限](#6.2-小程序端权限)

---

## 四、模块功能详细说明
> **页面路径对齐说明**：本节描述的页面路径（如 `pages-homeai/`）为开发阶段代码路径。
> 实际分包配置请以 [`ui-miniapp-v1.md`](../design/ui-miniapp-v1.md) 的 §十一（分包与路由规划）为准：
> - 首页 → 主包 `pages/`
> - AI对话 → 分包一 `pages-homeai-ai/`
> - 资料存储/账单/计划/烹饪/学习 → 分包二 `pages-homeai-more/`


### 4.1 小程序首页 + 微信用户管理

#### 小程序端

**入口首页** (`pages-homeai/index.vue`)

- 九宫格/网格布局展示所有可用模块入口
- 每个模块显示：图标 + 名称
- 顶部显示当前登录用户微信昵称、头像和所属家庭名称，点击进入**个人中心**
- 底部显示今日待办计划数量（若有）
- 页面加载时调取 `/homeai/user/login` 自动完成微信登录静默授权
- 首次使用弹出"用户须知"弹窗，包含功能简介和隐私说明
- 各功能模块首次进入时有独立的功能引导（气泡提示）：
  - AI对话页：首次进入提示"试试点击 📎 上传图片或文件"
  - 格式转换页：首次打开展示步骤引导「选择一个目标格式开始转换」
  - 学习资料页：首次看到「开始学习」按钮时提示"点击开始学习，自动计时"
- 引导仅展示一次（本地存储标记），可在设置中重置

**个人中心页** (`user-profile.vue`)

- 展示用户头像、微信昵称
- 编辑个人信息：手机号、家庭角色
- 查看所属家庭信息（名称、成员数）
- 各模块使用统计概览（对话次数、文件数、账单数等）

**微信登录流程**

- 首次进入小程序时弹出隐私协议授权弹窗（`wx.requirePrivacyAuthorize`），用户同意后方可获取个人信息
- 进入首页时调用 `wx.login()` 获取临时 code
- 后端通过 code 换取 openid
- 若为新用户：自动注册微信身份 -> 返回 JWT Token
- 若已存在用户：更新最后登录时间 -> 返回 JWT Token
- Token 存入小程序 Storage，后续请求携带在请求头

#### 管理端

**微信用户列表页** (`views/homeai/user/WxUserList.vue`)

- 表格展示所有微信用户：头像、昵称、所属家庭、openid（脱敏）、手机号、家庭角色、注册时间、最后登录时间、状态
- 支持按昵称搜索
- 支持按家庭筛选
- 可禁用某个用户的登录权限（黑名单）

**用户详情/编辑页** (`WxUserDrawer.vue`)

- 编辑用户信息：家庭角色（爸爸/妈妈/孩子/其他）、手机号、备注
- 查看该用户在各模块的使用数据概览（对话Token消耗、文件数、账单数等）

**家庭管理页** (`FamilyList.vue`)（新增）

- 表格展示所有家庭：家庭名称、创建者、成员数、创建时间
- 可查看每个家庭的成员详情
- 支持按家庭筛选用户

### 4.2 AI对话模块

#### 功能概述

> **AI能力集成**：AI 对话的底层架构（StreamChat/MultiModal/DocumentParser）和模型配置详见 [`ai-security.md`](../design/ai-security.md) §七。开发时优先复用 `jeecg-boot-module-airag` 的 SSE 流式对话和多模态理解能力，减少重复实现。

提供类似主流AI助手应用的对话体验，支持纯文本对话和多媒体内容理解，满足日常家庭问答、知识查询等场景。

#### 小程序端

**对话列表页** (`ai-conversations.vue`)

- 按时间倒序展示历史对话列表，每条显示：对话标题（自动取第一句话）、消息数、最后消息时间
- 支持左滑删除对话
- 底部固定"新建对话"按钮
- 点击进入对话详情页
- 若对话列表为空，显示引导文案和示例话题按钮（如"帮我写一份食谱"、"今天晚餐推荐"）

**对话聊天页** (`ai-chat.vue`)

- 顶部：对话标题（可点击编辑重命名），返回按钮、停止生成按钮
- 消息列表：从旧到新排列，采用气泡式布局
  - 用户消息：右对齐，蓝色气泡，显示文字内容、上传的图片缩略图
  - AI回复：左对齐，白色/灰色气泡，显示AI头像，文字支持Markdown渲染（代码块高亮、列表、表格）
  - 支持长按消息复制文本
- 输入区域：多行文本输入框 + 发送按钮 + 附件按钮
  - 附件按钮支持：拍照、从相册选择图片/视频（多选，最多9张）、选择文件
  - 选中的图片/文件以缩略图平铺在输入框上方，可点击删除
- 对话过程中显示"AI正在输入..."动画
- 支持SSE流式输出（逐字展示AI回复），用户可随时点击"停止生成"中断响应
- 底部安全区域适配（iOS/Android）

**上传文件到对话**

- 用户选择图片/视频后，先上传到服务器返回URL
- 将URL连同文字问题一起发送给AI
- AI回复对上传内容的理解和处理结果
- 图片/视频在消息列表中可点击放大预览

**Token不足时提示**

- 当用户剩余 Token 不足以发送消息时，弹出提示："今日Token额度即将用完，请联系管理员调整或明天再来"
- 仍可查看历史对话，但发送按钮变灰不可用

#### 管理端

**AI密钥配置页** (`AiKeyConfig.vue`)

- 表格展示所有已配置的AI密钥：提供商、模型名、API地址、是否启用、备注
- 新增/编辑弹出抽屉：选择提供商（DeepSeek / 通义千问 / OpenAI / Anthropic / Ollama 自定义）、输入模型名、API Key（输入时显示明文，保存后脱敏显示`sk-****xxxx`）、API地址（可选，默认使用官方地址）、备注说明
- API Key 在存储时进行 AES 加密
- 支持启用/停用某个密钥（停用后对话模块不再使用该模型）
- 可设置默认使用的模型（对话优先使用默认模型）

**用户额度管理页** (`AiUserQuota.vue`)

- 表格展示所有用户的额度配置：用户昵称、每日Token限额、今日已消耗、每月Token限额、本月已消耗
- 支持编辑某个用户的额度：日Token限额、月Token限额、有效期限
- 提供使用统计概览：总Token消耗、活跃用户数、各模型使用占比

#### 业务规则

- 配额以 Token 为单位计量（每次对话完成后记录实际 Token 消耗），而非对话次数
- AI模型选择顺序：优先使用用户指定的模型 -> 默认模型 -> 第一个启用的模型
- **上下文窗口管理**：按 Token 总量从最早的消息开始逐条移除，直到窗口内剩余 Token 数在模型限制以下。移除策略以消息条为单位（含该消息的所有附件图片），确保上下文保持连贯
- 所有对话记录永久保留（除非用户手动删除）
- 每次AI回复生成完成后，后端记录 Token 消耗并从用户额度中扣除
- **Token预检机制**：用户发送消息前，后端估算本次输入 Token + 预计输出 Token，若超过剩余配额则提前拦截并提示"Token可能不足，请缩短消息或等待额度重置"
  - 预检仅做一次性拦截（防止发送空消耗请求），实际消耗以后端 API 返回值为准
  - 若预检通过但实扣超限（如AI回复远超预期），允许最多透支 1000 Token，超额部分从次日额度扣除
- **额度计算口径**：Token 额度按用户维度计算（家庭成员各自独立额度），管理员可查看每位成员的消耗明细

<details>
<summary><b>AI对话模块 — 实体设计</b></summary>

**AiConversation（对话主表）** — `homeai_conversation`

```java
@Data
@TableName("homeai_conversation")
@EqualsAndHashCode(callSuper = true)
public class AiConversation extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String title;
    private String modelName;
    private String provider;
    @TableField("message_count")
    private Integer messageCount;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `family_id` | varchar(32) | 所属家庭（数据隔离） |
| `user_id` | varchar(32) | 创建用户 |
| `title` | varchar(200) | 对话标题（自动取首句） |
| `model_name` | varchar(50) | 使用的模型名 |
| `provider` | varchar(50) | AI 提供商 |
| `message_count` | int | 消息总数 |
| `message_count` | int | 消息数量 |

**AiMessage（消息表）** — `homeai_message`

```java
@Data
@TableName("homeai_message")
@EqualsAndHashCode(callSuper = true)
public class AiMessage extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String conversationId;
    private String role;
    @TableField("content_type")
    private String contentType;
    @TableField("content")
    private String content;
    @TableField("file_url")
    private String fileUrl;
    @TableField("token_count")
    private Integer tokenCount;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `conversation_id` | varchar(32) | 所属对话（外键） |
| `role` | varchar(20) | `user` / `assistant` |
| `content_type` | varchar(20) | `text` / `image` / `file` |
| `content` | text | 消息内容（AES-256-GCM加密存储） |
| `file_url` | varchar(500) | 附件文件 URL |
| `token_count` | int | 本条消息消耗的 Token 数 |

</details>

### 4.3 资料存储模块（含Office处理）

#### 功能概述

家庭资料集中存储 + Office文件处理，支持文件夹树形分类、文件上传/下载/搜索/收藏，并对存储的文件进行格式转换、AI生成等办公处理，作为家庭的"云资料库+办公工具箱"。

#### 技术实现路径（Office处理部分）

**格式转换**：后端接收文件 -> 调用 `LibreOffice` 命令（通过 `jodconverter`）进行格式转换 -> 生成结果文件 -> 返回下载链接。长任务采用异步模式（提交任务返回 taskId，前端轮询状态）。

> **LibreOffice 运维注意事项**：
> - **安装**：服务端安装 LibreOffice（推荐 Docker 运行 `libreoffice:latest` 容器），确保 `soffice` 命令可用
> - **配置**：`application.yml` 中配置 LibreOffice 路径和超时时间，`jodconverter.local.office-home=/usr/lib/libreoffice`
> - **监控**：监控 `soffice.bin` 进程状态和内存占用（建议堆内存 512MB-1GB），超过阈值自动重启
> - **故障恢复**：进程崩溃时 `soffice --headless --terminate_after_init` 重新初始化；连续3次转换失败则熔断该文件，10分钟后重试
> - **并发**：建议使用 LibreOffice 连接池（jodconverter 内置），最大并发 4-8 个实例
> - **安全**：Docker 容器化运行隔离文件系统，禁止访问宿主机文件

**AI文件生成**：用户输入自然语言要求 -> AI生成结构化文本/JSON数据 -> 后端使用 `Apache POI` / `poi-tl` 模板引擎将内容填充到预设模板中 -> 输出 `.docx` / `.xlsx` 文件。管理端需支持管理文档模板（上传和选择模板样式）。

```
AI文件生成流程:
用户输入 -> AI生成结构化内容 -> POI模板引擎填充 -> 输出文件
  例: "生成家庭月度开支表" -> AI返回JSON { headers:[...], rows:[...] } -> 填充Excel模板 -> 下载
```

#### 小程序端

**资料首页** (`storage-index.vue`)

- 显示文件夹树结构
- 每个文件夹显示：名称 + 内部文件数量 + 可见范围（仅自己/家庭）
- 根目录默认不可删除
- 长按文件夹出现操作菜单（重命名/删除/修改可见性）
- 底部浮动按钮：新建文件夹、上传文件

**文件列表页** (`storage-files.vue`)

- 展示文件列表（文件名、类型图标、大小、上传时间）
- 点击预览（图片直接查看、PDF在线浏览、其他格式显示信息）
- 长按菜单：下载、收藏、重命名、移动、删除、转发给微信好友、**Office处理**
- Office处理子菜单：
  - **格式转换**：对当前文件进行格式转换（如docx→pdf），显示转换进度，完成后可预览/下载
  - **AI文件生成**：基于当前文件内容，AI辅助生成新文件（如根据图片生成文档描述）
- 支持多选进行批量操作
- 顶部面包屑导航

**文件格式转换页** (`storage-office-convert.vue`)

- 从文件列表选中文件后进入，展示文件信息
- 选择目标格式（根据管理端配置动态展示）：
  - Word ↔ PDF
  - Excel ↔ PDF / CSV
  - PPT ↔ PDF / 图片
- 点击"开始转换"，显示转换进度条
- 转换完成后可预览、下载、转发
- 支持批量转换（同时选多个文件）

**AI文件生成页** (`storage-office-generate.vue`)

- 选择文档类型：Word / Excel / PPT
- 输入生成要求（自然语言描述）
- 可选模板样式（从管理端配置的模板中选择）
- AI生成过程中显示进度提示
- 生成完成后预览、下载、转发
- 不满意可点击"重新生成"

**Office处理历史** (`storage-office-history.vue`)

- 展示当前文件夹内所有处理过的文件记录，显示文件名、处理类型、时间、状态
- 可点击重新下载或转发

**资料搜索页** (`storage-search.vue`)

- 全局搜索（按文件名模糊匹配），结果显示文件路径
- 支持按文件类型筛选
- 点击文件进入预览

**文件预览**

- **图片/长图**：全屏预览，支持手势缩放、旋转、滑动翻页
- **视频**：微信原生 video 组件播放，支持全屏、倍速
- **PDF**：微信内置 PDF 阅读器（跳转或嵌入）
- **Word/Excel/PPT**：服务端临时转换为 PDF 后预览
- **压缩包**：列出内部文件清单，支持单独下载内部文件
- **未来扩展**：新增文件格式只需增加对应的文件类型图标和预览组件

#### 管理端

**资料存储管理页** (`StorageFileList.vue`)

- 左侧文件夹树（按用户组织），右侧文件列表
- 支持管理员上传文件到指定用户的文件夹
- 支持查看空间使用情况
- 支持对文件发起格式转换或AI生成（同小程序能力）

**文档模板管理** (`StorageOfficeTemplate.vue`)

- 管理Word/Excel模板文件，用于AI生成文档时的样式填充
- 支持上传模板文件、命名、预览、设为默认

**格式转换规则管理** (`StorageOfficeConvertRule.vue`)

- 配置各文件格式支持转换的目标格式列表
- 新增规则：选择源格式 + 勾选允许转换的目标格式
- 前端根据此配置动态展示可选的转换目标

**Office处理记录** (`StorageOfficeHistory.vue`)

- 展示所有用户的Office文件处理记录
- 筛选条件：用户、文件类型、处理类型、时间范围
- 可查看文件详情、下载原始和转换后文件、删除记录

#### 业务规则

- 文件夹可见性：`private`（仅自己）/ `family`（家庭共享），管理端不受限制
- 单文件大小限制：100MB（视频文件可单独配置，建议200MB）
- 文件夹最多支持5级嵌套
- 文件上传增加**格式白名单机制**（管理员可配置允许的文件类型）
- 默认允许类型：图片(jpg/png/gif/bmp)、文档(pdf/doc/docx/xls/xlsx/ppt/pptx)、视频(mp4/avi/mov/mkv)、压缩包(zip/rar/7z)、文本(txt/markdown)
- 禁止类型：可执行文件(exe/sh/bat/dll)、二进制库等
- **格式扩展性**：白名单由管理端动态配置，新增文件格式只需更新白名单 + 在组件中添加对应图标和预览处理器
- 文件上传到对象存储（MinIO/OSS），保留原始文件名 + UUID防重名
- Office转换任务异步处理，完成后通过状态轮询通知前端
  - **轮询策略**：前端每3秒轮询一次转换状态
  - **超时时间**：普通文件（<10MB）超时120秒，大文件（10-50MB）超时300秒
  - **失败重试**：转换失败自动重试1次，记录失败原因
  - **状态码**：`PENDING`→`PROCESSING`→`COMPLETED`/`FAILED`
- 转换后的文件保留7天后自动清理（仅保留记录）
- AI生成的文件直接保留，不设自动清理
- 支持的文件格式清单需在前后端保持一致
- **排序规则**：文件夹始终在文件之前（按名称字母序）；文件默认按上传时间倒序，支持切换按名称/大小/类型排序
- **最近标识**：24小时内上传的文件显示「NEW」角标

<details>
<summary><b>资料存储模块 — 实体设计</b></summary>

**StorageFolder（文件夹表）** — `homeai_storage_folder`

```java
@Data
@TableName("homeai_storage_folder")
@EqualsAndHashCode(callSuper = true)
public class StorageFolder extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String familyId;
    private String userId;
    private String name;
    private String parentId;
    private String visibility;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | varchar(100) | 文件夹名称 |
| `parent_id` | varchar(32) | 父文件夹 ID（根目录为空） |
| `visibility` | varchar(20) | `private` / `family` |
| `sort_order` | int | 排序序号 |

**StorageFile（文件表）** — `homeai_storage_file`

```java
@Data
@TableName("homeai_storage_file")
@EqualsAndHashCode(callSuper = true)
public class StorageFile extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String familyId;
    private String userId;
    private String folderId;
    private String fileName;
    @TableField("extension")
    private String extension;
    @TableField("file_size")
    private Long fileSize;
    @TableField("file_url")
    private String fileUrl;
    private String visibility;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `folder_id` | varchar(32) | 所属文件夹 |
| `file_name` | varchar(200) | 原始文件名 |
| `extension` | varchar(20) | 文件扩展名 |
| `file_size` | bigint | 文件大小（字节） |
| `file_url` | varchar(512) | 文件访问 URL |
| `study_count` | int | 学习次数 |

**StorageConvertTask（转换任务表）** — `homeai_storage_convert_task`

```java
@Data
@TableName("homeai_storage_convert_task")
@EqualsAndHashCode(callSuper = true)
public class StorageConvertTask extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String fileId;
    @TableField("source_format")
    private String sourceFormat;
    @TableField("target_format")
    private String targetFormat;
    private String status;
    @TableField("result_file_url")
    private String resultFileUrl;
    @TableField("error_message")
    private String errorMessage;
    @TableField("task_duration")
    private Integer taskDuration;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `file_id` | varchar(32) | 源文件 ID |
| `source_format` | varchar(20) | 源格式 |
| `target_format` | varchar(20) | 目标格式 |
| `status` | varchar(20) | `PENDING`→`PROCESSING`→`COMPLETED`/`FAILED` |
| `result_file_url` | varchar(512) | 结果文件 URL |
| `task_duration` | int | 处理耗时（秒） |

</details>

### 4.4 账单汇总与记录模块

#### 功能概述

记录日常家庭消费和收入（家庭内共享），支持多种方式记账，生成可视化消费报表，帮助家庭了解资金流向。

#### 小程序端

**账单首页** (`bill-index.vue`)

- 顶部概览卡片：家庭本月总支出、总收入、结余
- 本月支出按分类的环形图（Top 5 类别）
- 最近10条账单记录（日期、录入人、分类图标、备注、金额）
- 底部浮动按钮"记一笔"
- 账单列表底部增加导入入口提示条

**记一笔** (`bill-add.vue`)

- 日期选择（默认今天）
- 类型切换：支出 / 收入
- 金额输入（数字键盘，支持小数点后两位）
- 分类选择：网格图标+名称
- 支付方式（微信/支付宝/现金/银行卡/其他）
- 备注（可选）、凭证拍照/上传（可选）
- "保存"后返回首页并刷新
- 支持快捷重复上一笔

**账单列表** (`bill-list.vue`)

- 按月分组，每日账单按时间倒序
- 每条显示：分类图标+颜色、录入人头像、分类名、备注、金额（支出红色、收入绿色）
- 支持按分类、日期范围、导入来源筛选
- 点击可编辑

**统计报表** (`bill-statistics.vue`)

- 时段选择：本月/上月/本季度/今年/自定义
- 支出分类饼图 + 明细列表
- 月度趋势折线图（近12个月）
- 收支对比柱状图
- 导出报表（生成图片分享）

#### 管理端

**账单列表页** (`BillList.vue`)

- 展示所有家庭的所有账单（可按家庭、用户、日期、分类筛选）
- 表格列：日期、家庭、用户、分类、金额、类型、支付方式、备注
- 支持编辑、删除

**批量导入页** (`BillImport.vue`)

- **微信支付账单CSV导入**：用户上传CSV -> 后端解析字段 -> 映射分类 -> 去重 -> 预览确认 -> 写入
- **Excel表格导入**：上传 `.xlsx/.xls` 文件，按标准模板列名匹配（管理端提供模板下载）
- **银行流水导入**：支持上传银行流水文件（PDF或Excel），推荐使用AI识别
- **AI智能识别**：用户上传后可选启用AI识别（对非标准格式尤其有用），后端解析文件文本内容 -> AI提取结构化数据（日期/金额/分类/备注/支付方式）
- **二次确认机制**：AI识别结果返回前端后，用户逐条核对、编辑修改、勾选确认，确认无误后批量写入系统
- **去重规则**：相同日期 + 相同金额 + 相同备注视为重复，确认时黄色警告标注
- 手动批量录入：管理员直接填入多行数据

**消费分类管理** (`BillCategory.vue`)

- 表格展示所有消费分类（系统默认 + 用户自定义）
- 新增/编辑：分类名称、图标、颜色、排序号、是否为收入类型
- 可禁用分类（已有账单不受影响，新建不可选）

**统计报表** (`BillStatistics.vue`)

- 查看所有家庭的合并报表
- 维度切换：按家庭、按用户、按分类、按月份
- 支持导出为Excel

#### 业务规则

- 金额精确到分
- 默认分类（不可删除但可禁用）：餐饮、交通、购物、生活缴费、娱乐、医疗、教育、人情往来、工资收入、其他
- 导入的微信账单需做分类映射，未匹配的归入"其他"
- 统计数据实时计算，大数据量时可考虑缓存
- **并发编辑冲突处理**：
  - 账单记录增加 `version` 版本号字段（乐观锁）
  - 编辑保存时检查版本号，若已被他人修改则拒绝保存并提示「该账单已被其他成员修改，请刷新后重试」
  - 删除他人创建的账单时弹出确认提示「确定删除[成员名]记录的账单？」
  - 操作日志记录编辑/删除人、时间、变更内容（用于追溯）
- **缓存策略**：
  - 账单月度概览：Redis 缓存5分钟，每次新增/编辑/删除账单时清除缓存
  - 分类占比数据：缓存30分钟
  - 缓存键格式：`homeai:cache:bill:{userId}:{param}`
  - 缓存穿透保护：查询空值也缓存（较短TTL 30秒），防止恶意请求穿透到数据库

> **缓存键常量管理**：所有缓存键格式应在 `RedisKeyConstants` 常量类中集中定义，避免硬编码。
> 示例：
> ```java
> public interface RedisKeyConstants {
>     String BILL_MONTHLY = "homeai:cache:bill:monthly:%s";     // 参数：userId
>     String LEARN_STATS  = "homeai:cache:learn:stats:%s";       // 参数：userId
>     String PLAN_CALENDAR = "homeai:cache:plan:calendar:%s:%s"; // 参数：userId,date
> }
> ```

<details>
<summary><b>账单模块 — 实体设计</b></summary>

**BillEntry（账单记录表）** — `homeai_bill_entry`

```java
@Data
@TableName("homeai_bill_entry")
@EqualsAndHashCode(callSuper = true)
public class BillEntry extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String familyId;
    private String userId;
    @TableField("bill_date")
    private LocalDate billDate;
    @TableField("type")
    private String type;
    @TableField("category_id")
    private String categoryId;
    private BigDecimal amount;
    @TableField("payment_method")
    private String paymentMethod;
    private String remark;
    @TableField("voucher_url")
    private String voucherUrl;
    @TableField("source")
    private String source;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `bill_date` | date | 账单日期 |
| `type` | varchar(10) | `expense` / `income` |
| `category_id` | varchar(32) | 消费分类 ID |
| `amount` | decimal(10,2) | 金额（精确到分） |
| `payment_method` | varchar(20) | 支付方式：微信/支付宝/现金/银行卡 |
| `voucher_url` | varchar(500) | 凭证图片 URL |
| `source` | varchar(20) | `manual` / `wechat_csv` / `excel` / `bank` / `ai_import` |

**BillCategory（消费分类表）** — `homeai_bill_category`

```java
@Data
@TableName("homeai_bill_category")
@EqualsAndHashCode(callSuper = true)
public class BillCategory extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    private String icon;
    private String color;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("type")
    private String type;
    @TableField("is_default")
    private Boolean isDefault;
    @TableField("is_enabled")
    private Boolean isEnabled;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | varchar(50) | 分类名称 |
| `icon` | varchar(50) | 图标标识 |
| `color` | varchar(20) | 显示颜色（HEX） |
| `type` | varchar(10) | `income`=收入 `expense`=支出 |
| `is_system` | tinyint(1) | 系统默认（不可删除） |
| `is_enabled` | tinyint(1) | 是否启用 |

</details>


### 4.5 日常计划模块

#### 功能概述

以日历形式管理日常计划，支持提醒，帮助家庭成员合理安排时间。

#### 小程序端

**计划首页** (`plan-index.vue`)

- 月历视图，有计划日期显示小圆点标记
- 点击日期展示当日计划列表
- "今天有 X 项计划，已完成 Y 项"
- 底部浮动按钮"新增计划"

**新增计划** (`plan-add.vue`)

- 计划标题（必填，50字以内）、内容（可选）
- 日期选择（默认今天）
- 时间选择：全天 / 定时（开始+结束）
- 优先级：普通 / 重要 / 紧急（不同颜色标识）
- 计划分类：工作 / 学习 / 生活 / 运动 / 家庭 / 其他
- 提醒设置：不提醒 / 提前5/15/30/60分钟
- 重复设置：不重复 / 每天 / 每周 / 每月 / 自定义星期
- 保存后日历对应日期出现标记

**计划详情页** (`plan-detail.vue`)

- 展示完整计划信息，操作：完成/取消、编辑、删除

**今日计划（首页展示）**

- 小程序首页顶部展示今日计划概览，点击跳转到计划模块

#### 管理端

**计划管理页** (`PlanList.vue`)

- 日历视图或列表视图
- 可按用户、家庭、日期、状态筛选
- 查看单个用户一段时间内的计划完成率

#### 重复计划的实现策略

采用**预生成实例**方案（替代动态计算）：

1. 用户创建重复计划时，保存一条"主计划"（`is_repeat_master=1`）
2. 创建时预生成未来90天的计划实例记录
3. 每天凌晨 Quartz 定时任务扫描主计划，自动生成第91天的实例（滚动生成）
4. 日历视图直接查询实例表，无需动态计算，性能可控
5. 用户修改或删除某一天的计划实例，不影响主计划和后续生成

#### 微信消息提醒

**实现方式：微信小程序订阅消息**

- 创建带提醒的计划时，引导用户授权订阅
- 后端Quartz定时任务每分钟扫描：需提醒的计划实例
- 通过微信订阅消息API推送
- 推送完成后标记已提醒，避免重复

**备选方案**

- 用户打开小程序时首页弹窗展示待办计划
- 通过服务号模板消息（需申请服务号并绑定）

#### 业务规则

- 计划按日期归属，跨天计划按开始日期归属
- 计划完成状态可多次切换
- 过期未完成的计划继续展示在日历上（灰色标记）
- 计划实例表存储未来90天的实例，定期清理过期30天以上的实例

<details>
<summary><b>日常计划模块 — 实体设计</b></summary>

**PlanMaster（主计划表）** — `homeai_plan_master`

```java
@Data
@TableName("homeai_plan_master")
@EqualsAndHashCode(callSuper = true)
public class PlanMaster extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    private String title;
    private String content;
    @TableField("start_time")
    private LocalTime startTime;
    @TableField("end_time")
    private LocalTime endTime;
    @TableField("is_all_day")
    private Boolean isAllDay;
    private String priority;
    private String category;
    @TableField("remind_before")
    private Integer remindBefore;
    @TableField("is_repeat_master")
    private Boolean isRepeatMaster;
    private String repeatRule;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `title` | varchar(100) | 计划标题 |
| `content` | text | 计划内容（可选） |
| `plan_date` | date | 计划日期 |
| `start_time` / `end_time` | time | 起止时间 |
| `is_all_day` | tinyint(1) | 是否全天 |
| `priority` | varchar(20) | `normal` / `important` / `urgent` |
| `category` | varchar(20) | 分类：工作/学习/生活/运动/家庭 |
| `remind_minutes` | int | 提前提醒分钟数 |
| `repeat_rule` | varchar(200) | 重复规则 JSON `{ type, daysOfWeek[] }` |

**PlanInstance（计划实例表）** — `homeai_plan_instance`
**PlanInstance（计划实例表）** — `homeai_plan_instance`

```java
@Data
@TableName("homeai_plan_instance")
@EqualsAndHashCode(callSuper = true)
public class PlanInstance extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    @TableField("master_id")
    private String masterId;
    @TableField("plan_date")
    private LocalDate planDate;
    private String status;
    @TableField("reminded")
    private String reminded;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `master_id` | varchar(32) | 所属主计划 ID |
| `plan_date` | date | 计划日期 |
| `status` | varchar(20) | `pending` / `completed` / `cancelled` |
| `reminded` | varchar(2) | 是否已推送提醒 |

</details>

### 4.6 烹饪指南模块

#### 功能概述

家庭菜谱库，家庭成员可以记录、查看和分享菜谱，方便日常做饭参考。

#### 小程序端

**菜谱首页** (`recipe-index.vue`)

- 搜索框（按菜名搜索）
- 分类横向滚动导航
- 推荐菜谱（按浏览次数排序，最多8个）
- 最新添加（按创建时间倒序）
- 底部浮动按钮：新增菜谱

**分类浏览页** (`recipe-category.vue`)

- 左侧分类列表，右侧菜谱卡片网格
- 卡片：菜名、封面图、难度星级、烹饪时间

**菜谱详情页** (`recipe-detail.vue`)

- 封面大图、基本信息（菜名/分类/难度/时间/份数）
- **做菜视频**：有视频的菜谱显示视频播放入口，点击全屏播放（微信原生video组件）
- 食材清单：名称+用量，支持一键复制
- 烹饪步骤：左右滑动切换步骤，每步含图片+说明
- 小贴士
- 底部操作栏：收藏/取消、分享、编辑（仅创建者或管理员）

**新增/编辑菜谱** (`recipe-add.vue`)

- 基本信息：菜名、分类、封面、难度、时间、份数
- **做菜视频（可选）**：上传mp4/mov/avi格式做菜视频，建议时长30分钟内，支持从相册选择或拍摄
- 食材清单：动态添加/删除行（食材名+用量）
- 烹饪步骤：上传图片+文字说明，支持拖拽排序
- 小贴士
- 可见性：仅自己 / 家庭共享
- **可见性切换规则**：
  - **仅自己 -> 家庭共享**：切换后其他成员收到通知「[成员名]分享了一道新菜谱[菜名]」
  - **家庭共享 -> 仅自己**：已收藏的成员收藏夹自动移除，收到提示「[菜名]已被创建者设为私密」
  - **切换频率限制**：同一菜谱 24 小时内最多切换可见性 1 次
- 保存后跳转到详情页

#### 管理端

- **菜谱管理页**：按用户/家庭/分类筛选，编辑和删除任何菜谱
- **菜谱分类管理**：CRUD、排序
- 默认分类：热菜、凉菜、汤羹、主食、烘焙、饮品、小食、其他

#### 业务规则

- 用户可编辑/删除自己的菜谱，管理员可管理所有菜谱
- 家庭共享的菜谱所有家庭成员可见
- 食材清单和烹饪步骤采用JSON格式存储
- 浏览次数实时累加
- 后期扩展：评分、AI根据食材推荐菜谱
- **内容审核**：用户上传的封面图片/视频需经过内容安全审核（可接入微信内容安全API或第三方审核服务）
  - 图片：检测色情、暴力、政治敏感内容
  - 文字（菜名、步骤、小贴士）：关键词过滤 + AI内容审核
  - 视频：截图帧审核
  - 审核不通过的内容标记为「审核中」，仅创建者可见；审核通过后公开发布
- **推荐排序算法**：推荐菜谱采用混合权重 `浏览次数×0.6 + 收藏数×0.3 + 时间衰减×0.1`，确保新菜谱也有曝光机会
- **新菜尝鲜区**：最近 7 天内新增的菜谱独立展示，避免被热门菜谱淹没

<details>
<summary><b>烹饪指南模块 — 实体设计</b></summary>

**Recipe（菜谱表）** — `homeai_recipe`

```java
@Data
@TableName("homeai_recipe")
@EqualsAndHashCode(callSuper = true)
public class Recipe extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String familyId;
    private String userId;
    private String name;
    @TableField("category")
    private String category;
    @TableField("cover_image")
    private String coverImage;
    @TableField("video_url")
    private String videoUrl;
    private String difficulty;
    @TableField("cook_time")
    private Integer cookTime;
    private Integer servings;
    private String ingredients;
    private String steps;
    private String tips;
    private String visibility;
    @TableField("view_count")
    private Integer viewCount;
    @TableField("favorite_count")
    private Integer favoriteCount;
    @TableField("audit_status")
    private String auditStatus;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `category_id` | varchar(32) | 菜谱分类 |
| `cover_url` / `video_url` | varchar(500) | 封面 / 做菜视频 |
| `difficulty` | varchar(10) | `easy` / `medium` / `hard` |
| `cook_time` | int | 烹饪时间(分钟) |
| `servings` | int | 份数 |
| `ingredients` | json/text | 食材清单 JSON 数组 |
| `steps` | json/text | 烹饪步骤 JSON 数组 |
| `visibility` | varchar(20) | `private` / `family` |
| `audit_status` | varchar(20) | `approved` / `rejected` / `pending` |

**RecipeCategory（菜谱分类表）** — `homeai_recipe_category`

```java
@Data
@TableName("homeai_recipe_category")
@EqualsAndHashCode(callSuper = true)
public class RecipeCategory extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String name;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("is_default")
    private Boolean isDefault;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `name` | varchar(50) | 分类名称 |
| `sort_order` | int | 排序序号 |
| `is_system` | tinyint(1) | 系统默认（不可删除） |

</details>

### 4.7 学习模块

#### 功能概述

管理学习资料和记录学习进度，适合学生自我学习管理或家庭成员共享学习资源。

#### 小程序端

**学习首页** (`learn-index.vue`)

- 学习统计：本周学习次数、累计资料数
- 分类导航（每个分类显示资料数量）
- 最近学习记录（最近5条）
- 收藏的资料快捷入口

**资料列表页** (`learn-material.vue`)

- 展示资料（标题、类型图标、标签、上传时间）
- 支持按标签筛选、列表/网格切换
- 点击进入查看

**资料查看/学习页**

- 根据类型展示：PDF文档（微信预览）/ 视频（播放器）/ 链接（webview）/ 笔记（富文本）
- 底部操作：收藏、开始计时学习 / 手动输入学习时长

**学习记录页** (`learn-record.vue`)

- 日历视图展示学习记录
- 点击日期展示：学习了哪些资料、时长（计时或手动）、笔记
- 学习进度条（资料完成百分比）

#### 管理端

- **学习资料管理页**：新增/编辑/删除资料（标题/分类/描述/文件或链接或文本/标签/可见性）
- **学习分类管理**：CRUD、排序
- **学习记录查看**：查看用户学习记录、统计（总时长、活跃天数）
- 默认分类：语文、数学、英语、科学、编程、历史、艺术、其他

#### 业务规则

- 资料类型：**文件（PDF/Word/Excel/PPT/图片/长图）**、**视频**（支持倍速/全屏）、网页链接、纯文本笔记
- **格式扩展性**：新增资料类型只需在文件类型枚举中添加 + 对应预览组件（如有）
- **预览方式**：
  - 视频：微信原生 video 组件播放，支持倍速 0.5x-2x
  - 图片/长图：全屏预览，手势缩放/旋转
  - PDF/Word/Excel/PPT：服务端转PDF后预览
  - 网页链接：webview 内嵌打开
- 每个资料可打多个标签
- 学习进度按百分比记录（0-100%）
- 学习时长支持两种模式：
  1. 计时模式：点击"开始学习"开始计时，完成后自动记录时长
  2. 手动模式：学习完毕后手动输入时长（分钟）
- 学习笔记与记录关联
- 资料可见性：仅自己 / 家庭共享
- **默认排序**：资料列表默认按「最近学习时间」降序（未学习过的按添加时间）
- **排序选项**：支持切换为「最近学习」「最新添加」「按标题」
- **学习缓存策略**：
  - 学习统计（本周次数、累计资料数）：缓存10分钟，新增学习记录时清除
  - 缓存键格式：`homeai:cache:learn:{userId}:{param}`
<details>
<summary><b>学习模块 — 实体设计</b></summary>

**LearnMaterial（学习资料表）** — `homeai_learn_material`

```java
@Data
@TableName("homeai_learn_material")
@EqualsAndHashCode(callSuper = true)
public class LearnMaterial extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String familyId;
    private String userId;
    private String title;
    private String description;
    @TableField("category")
    private String category;
    @TableField("type")
    private String type;
    @TableField("file_url")
    private String fileUrl;
    private String tags;
    private String visibility;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `category_id` | varchar(32) | 学习分类 |
| `material_type` | varchar(20) | `file` / `video` / `link` / `note` |
| `file_url` | varchar(500) | 文件 URL（文件/视频类型） |
| `thumbnail_url` | varchar(512) | 缩略图 URL |
| `category` | varchar(50) | 分类标签 |
| `tags` | json/text | 标签 JSON 数组 |
| `study_count` | int | 学习次数 |

**LearnRecord（学习记录表）** — `homeai_learn_record`

```java
@Data
@TableName("homeai_learn_record")
@EqualsAndHashCode(callSuper = true)
public class LearnRecord extends JeecgEntity {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String userId;
    @TableField("material_id")
    private String materialId;
    @TableField("study_date")
    private LocalDate studyDate;
    @TableField("duration_minutes")
    private Integer durationMinutes;
    @TableField("mode")
    private String mode;
    private String notes;
    @Version
    private Integer version;
    @TableLogic
    private String delFlag;
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `material_id` | varchar(32) | 学习资料 ID |
| `study_date` | date | 学习日期 |
| `duration_minutes` | int | 学习时长（分钟） |
| `mode` | varchar(20) | `timing`=计时 `manual`=手动 |
| `note` | text | 学习笔记 |
| `notes` | text | 学习笔记 |

</details>

## 五、接口设计 (API)

> 完整的接口定义（含各模块全部端点、请求/响应格式、权限标记）已分离至独立文档：
>
> **👉 [`docs/api/homeai-api-v1.md`](../api/homeai-api-v1.md)**
>
> 以下仅保留接口概览供快速查阅。

### 5.1 接口规范速览

- 基础路径：`/homeai`（建议加入 `/v1` 版本前缀）
- 响应格式：JEECG 标准 `{ success, message, code, result, timestamp }`
- 认证方式：JWT（请求头 `X-Access-Token`）
- 分页参数：`pageNo`, `pageSize`；返回 `{ records[], total, pages, current }`

### 5.2 按模块API数量统计

| 模块 | 端点数量 | 小程序可用 | 管理端可用 |
| ---- | ---- | ---- | ---- |
| 微信用户 | 2 | 2 | 0 |
| 家庭管理 | 10 | 8 | 2 |
| AI对话 | 8 | 8 | 3 |
| 资料存储（含Office） | 15 | 14 | 12 |
| 账单模块 | 12 | 10 | 12 |
| 日常计划 | 7 | 7 | 6 |
| 烹饪指南 | 11 | 8 | 8 |
| 学习模块 | 10 | 9 | 7 |
| AI配置（仅管理端） | 7 | 0 | 7 |
| 文件白名单配置 | 2 | 0 | 2 |
| **合计** | **84** | **66** | **59** |

### 5.3 完整API文档

> 🔗 完整API定义请查看：[`docs/api/homeai-api-v1.md`](../api/homeai-api-v1.md)

<details>
<summary><b>Controller 实现规范与示例</b></summary>

### 5.4 Controller 实现规范

所有模块的 Controller 需遵循以下 JeecgBoot 注解约定：

```java
@Tag(name="AI对话管理", description="AI对话模块")
@RestController
@RequestMapping("/homeai/ai")
@Slf4j
public class AiConversationController {

    @AutoLog(value="ai-conversation-分页列表查询", logType=LOG_TYPE.QUERY)
    @Operation(summary="分页列表查询", description="按家庭/用户筛选对话")
    @GetMapping("/conversations")
    @RequiresPermissions("homeai:ai:conversation:list")
    public Result<IPage<AiConversation>> queryPageList(
            AiConversation conversation,
            @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
            @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<AiConversation> wrapper = QueryGenerator.initQueryWrapper(conversation, req.getParameterMap());
        Page<AiConversation> page = new Page<>(pageNo, pageSize);
        IPage<AiConversation> pageList = aiConversationService.page(page, wrapper);
        return Result.OK(pageList);
    }

    @AutoLog(value="ai-conversation-添加", logType=LOG_TYPE.OPERATION)
    @Operation(summary="添加对话")
    @PostMapping("/conversations")
    @RequiresPermissions("homeai:ai:conversation:add")
    public Result<AiConversation> add(@RequestBody AiConversation conversation) {
        aiConversationService.save(conversation);
        return Result.OK("添加成功");
    }

    @AutoLog(value="ai-conversation-编辑", logType=LOG_TYPE.OPERATION)
    @Operation(summary="编辑对话")
    @PutMapping("/conversations/{id}")
    @RequiresPermissions("homeai:ai:conversation:edit")
    public Result<AiConversation> edit(@RequestBody AiConversation conversation) {
        aiConversationService.updateById(conversation);
        return Result.OK("编辑成功");
    }

    @AutoLog(value="ai-conversation-删除", logType=LOG_TYPE.OPERATION)
    @Operation(summary="删除对话")
    @DeleteMapping("/conversations/{id}")
    @RequiresPermissions("homeai:ai:conversation:delete")
    public Result<AiConversation> delete(@RequestParam(name="id") String id) {
        aiConversationService.removeById(id);
        return Result.OK("删除成功");
    }
}
```

#### 各模块 Controller 映射路径约定

| 模块 | 基础路径 | 示例端点 |
|------|---------|---------|
| 微信用户 | `/homeai/user` | `/homeai/user/login` |
| 家庭管理 | `/homeai/family` | `/homeai/family/list` |
| AI对话 | `/homeai/ai` | `/homeai/ai/conversation/list` |
| 资料存储 | `/homeai/storage` | `/homeai/storage/file/list` |
| Office处理 | `/homeai/storage/office` | `/homeai/storage/office/convert` |
| 账单模块 | `/homeai/bill` | `/homeai/bill/entry/list` |
| 日常计划 | `/homeai/plan` | `/homeai/plan/master/list` |
| 烹饪指南 | `/homeai/recipe` | `/homeai/recipe/list` |
| 学习模块 | `/homeai/learn` | `/homeai/learn/material/list` |
| AI配置 | `/homeai/ai/config` | `/homeai/ai/config/key/list` |

#### 注解约定总结

- **Swagger/Knife4j**: `@Tag`(类)、`@Operation`(方法)、`@Schema`(字段)
- **日志**: 增删改用 `@AutoLog(logType=LOG_TYPE.OPERATION)`，查询用 `@AutoLog(logType=LOG_TYPE.QUERY)`
- **权限**: 管理端接口加 `@RequiresPermissions("homeai:module:action")`
- **分页**: 使用 `QueryGenerator.initQueryWrapper()` 自动解析查询参数
- **响应**: 统一返回 `Result<T>`，分页返回 `Result<IPage<T>>`

</details>

## 六、权限体系

### 6.1 管理端菜单与权限编码


| 功能模块     | 权限编码                                                     | 说明        |
| -------- | -------------------------------------------------------- | --------- |
| AI对话管理   | `homeai:ai:conversation:list`, `homeai:ai:conversation:add`, `homeai:ai:conversation:edit`, `homeai:ai:conversation:delete`, `homeai:ai:conversation:search`               | 对话CRUD+搜索    |
| AI密钥配置   | `homeai:config:key:list`, `homeai:config:key:add`, `homeai:config:key:edit`, `homeai:config:key:delete`               | 完整CRUD    |
| AI额度管理   | `homeai:config:quota:list`, `homeai:config:quota:edit`                        | 查看与修改     |
| 文件白名单    | `homeai:config:whitelist:list`, `homeai:config:whitelist:edit`                    | 安全配置      |
| 家庭管理     | `homeai:family:list`, `homeai:family:view`                              | 查看所有家庭    |
| 资料存储（含Office） | `homeai:storage:list`, `homeai:storage:view`, `homeai:storage:upload`, `homeai:storage:delete`, `homeai:storage:convert`, `homeai:storage:generate`, `homeai:storage:template` | 文件夹+文件管理+Office处理 |
| 账单分类     | `homeai:bill:category:list`, `homeai:bill:category:add`, `homeai:bill:category:edit`, `homeai:bill:category:delete`            | 分类CRUD    |
| 账单记录     | `homeai:bill:entry:list`, `homeai:bill:entry:add`, `homeai:bill:entry:edit`, `homeai:bill:entry:delete`, `homeai:bill:entry:import`        | 账单CRUD+导入 |
| 日常计划     | `homeai:plan:master:list`, `homeai:plan:master:add`, `homeai:plan:master:edit`, `homeai:plan:master:delete`                     | 计划CRUD    |
| 烹饪分类     | `homeai:recipe:category:list`, `homeai:recipe:category:add`, `homeai:recipe:category:edit`, `homeai:recipe:category:delete`          | 分类CRUD    |
| 菜谱管理     | `homeai:recipe:list`, `homeai:recipe:add`, `homeai:recipe:edit`, `homeai:recipe:delete`                   | 菜谱CRUD    |
| 学习分类     | `homeai:learn:category:list`, `homeai:learn:category:add`, `homeai:learn:category:edit`, `homeai:learn:category:delete`           | 分类CRUD    |
| 学习资料     | `homeai:learn:material:list`, `homeai:learn:material:add`, `homeai:learn:material:edit`, `homeai:learn:material:delete`                    | 资料CRUD    |
| 用户管理     | `homeai:user:list`, `homeai:user:view`, `homeai:user:edit`                           | 微信用户管理    |


### 6.2 小程序端权限


#### 编码规范（通用）

**Swagger/Knife4j 注解**：
- Controller 类加 `@Tag(name="XX管理", description="XX模块管理接口")`
- 方法加 `@Operation(summary="xxx", description="详细说明")`
- 实体字段加 `@Schema(description="字段说明")`

**日志注解**：
- 所有增/删/改操作加 `@AutoLog(value="xxx", logType=LOG_TYPE.OPERATION)`
- 查询操作加 `@AutoLog(value="xxx", logType=LOG_TYPE.QUERY)`（低频查询可选）
- 权限注解：所有管理端接口加 `@RequiresPermissions("homeai:module:action")`

**文件大小限制标准（统一）**：
| 模块 | 单文件上限 | 说明 |
|------|-----------|------|
| AI对话附件 | 20MB | 图片/视频/文档 |
| 资料存储 | 200MB | 大文件分片上传支持 |
| 菜谱视频 | 500MB | 视频文件 |
| 学习资料 | 200MB | PDF/视频/文档 |
| 账单导入 | 10MB | CSV/Excel/PDF |

**MyBatis-Plus 乐观锁配置**：
- 实体类 `bill_entry` 的 `version` 字段加 `@Version` 注解
- MyBatis-Plus 配置类中启用乐观锁插件：`@Bean public MybatisPlusInterceptor interceptor() { ... addInnerInterceptor(new OptimisticLockerInnerInterceptor()); }`
- 更新时自动校验 version，冲突抛出 `DataModificationException`
- 通过 `wx.login()` 获取 openid，后端签发 JWT Token
- 小程序请求头携带 `X-Access-Token`
- 有家庭的用户：账单数据按家庭隔离，烹饪可见家庭共享菜谱
- 无家庭的用户：所有数据按个人隔离
- 数据访问权限在 Service 层统一校验

<details>
<summary><b>数据权限实现方案</b></summary>

### 6.3 数据权限实现方案

#### 6.3.1 小程序端数据隔离

小程序端通过 `@PermissionData` 注解 + `familyId` 参数实现家庭级数据隔离：

```java
@RestController
@RequestMapping("/homeai/bill")
@Tag(name="账单管理")
public class BillEntryController {

    @AutoLog(value="bill-entry-分页查询", logType=LOG_TYPE.QUERY)
    @Operation(summary="分页查询")
    @GetMapping("/entry/list")
    @PermissionData(title="账单记录")
    public Result<IPage<BillEntry>> queryPageList(
            BillEntry billEntry,
            @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
            @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
            HttpServletRequest req) {
        QueryWrapper<BillEntry> wrapper = QueryGenerator.initQueryWrapper(billEntry, req.getParameterMap());
        // familyId 由小程序端传入，@PermissionData 自动追加到查询条件
        Page<BillEntry> page = new Page<>(pageNo, pageSize);
        IPage<BillEntry> pageList = billEntryService.page(page, wrapper);
        return Result.OK(pageList);
    }
}
```

**数据规则配置**（管理端 → 系统管理 → 数据规则）：

| 规则名称 | 字段 | 条件 | 规则值 |
|---------|------|------|-------|
| 家庭数据隔离 | `family_id` | `=` | `#{currentUser.familyId}` |

#### 6.3.2 管理端数据权限

管理端通过 `sys_org_code` + `@PermissionData` 实现组织级数据隔离：

```java
@AutoLog(value="账单管理-分页查询", logType=LOG_TYPE.QUERY)
@GetMapping("/entry/list")
@RequiresPermissions("homeai:bill:entry:list")
@PermissionData(title="账单记录")
public Result<IPage<BillEntry>> queryPageList(...) {
    // 管理端根据登录用户的 sys_org_code 自动追加数据权限
    // 管理员（admin）可查看所有数据
    // 普通管理员只可查看其组织及下级组织的数据
}
```

**配置要点**：
1. 实体类继承 `JeecgEntity`（自带 `sys_org_code` 字段）
2. 管理端菜单配置「数据规则」，选择对应的权限编码
3. `@PermissionData` 注解自动解析数据权限规则并追加 SQL 条件
4. 超级管理员（admin）不受数据权限限制

#### 6.3.3 家庭级数据过滤拦截器配置

```java
@Configuration
public class MybatisPlusConfig {

    /**
     * Mybatis-Plus 分页插件 + 乐观锁 + 数据权限
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        // 1. 数据权限拦截器（需放在分页插件之前）
        interceptor.addInnerInterceptor(new DataPermissionInterceptor());
        // 2. 分页插件
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        // 3. 乐观锁插件
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }
}
```

> **注意**：JeecgBoot 默认已集成上述配置，新模块只需在实体类上正确继承 `JeecgEntity` 并在 Controller 方法上添加 `@PermissionData` 注解即可。

</details>

<details>
<summary><b>通用查询规范</b></summary>

### 6.4 通用查询规范

#### 6.4.1 分页查询

使用 JeecgBoot 标准分页模式：

```java
// Controller 层
@GetMapping("/list")
public Result<IPage<Entity>> queryPageList(
        Entity entity,
        @RequestParam(name="pageNo", defaultValue="1") Integer pageNo,
        @RequestParam(name="pageSize", defaultValue="10") Integer pageSize,
        HttpServletRequest req) {
    // QueryGenerator 自动解析前端传参生成 QueryWrapper
    QueryWrapper<Entity> wrapper = QueryGenerator.initQueryWrapper(entity, req.getParameterMap());
    Page<Entity> page = new Page<>(pageNo, pageSize);
    IPage<Entity> pageList = service.page(page, wrapper);
    return Result.OK(pageList);
}

// 前端传参示例
// ?pageNo=1&pageSize=10&fieldName=value&fieldName_op=xxx
```

#### 6.4.2 时间范围查询

```java
// 实体类中声明时间范围查询参数
@Data
public class BillEntry extends JeecgEntity {
    // 单字段时间条件
    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate billDate;

    // 时间范围查询参数（不在表中映射）
    @TableField(exist = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate billDate_begin;   // 时间范围-开始

    @TableField(exist = false)
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate billDate_end;     // 时间范围-结束
}

// QueryGenerator 自动识别 _begin / _end 后缀，生成 BETWEEN 条件
// 前端传参：?billDate_begin=2026-01-01&billDate_end=2026-07-29
```

#### 6.4.3 模糊查询

```java
// 方式一：QueryGenerator 自动解析（推荐）
// 前端传参：?name=关键字&name_op=like
// QueryGenerator 自动生成 WHERE name LIKE '%关键字%'

// 方式二：LambdaQueryWrapper 手动构建
@Service
public class RecipeServiceImpl extends ServiceImpl<RecipeMapper, Recipe> implements RecipeService {

    public IPage<Recipe> searchByName(String keyword, Integer pageNo, Integer pageSize) {
        Page<Recipe> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<Recipe> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotBlank(keyword), Recipe::getName, keyword);
        wrapper.orderByDesc(Recipe::getCreateTime);
        return this.page(page, wrapper);
    }
}
```

#### 6.4.4 精确查询

```java
// 前端传参：?categoryId=xxx
// QueryGenerator 自动生成 WHERE category_id = 'xxx'

// LambdaQueryWrapper 手动方式
LambdaQueryWrapper<BillEntry> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(BillEntry::getFamilyId, familyId);
wrapper.eq(BillEntry::getBillType, "expense");
wrapper.eq(BillEntry::getCategoryId, categoryId);
```

#### 6.4.5 排序

```java
// 方式一：通过 Page 对象设置
Page<Entity> page = new Page<>(pageNo, pageSize);
page.addOrder(new OrderItem("create_time", false)); // false=降序

// 方式二：通过 QueryWrapper 设置
QueryWrapper<Entity> wrapper = new QueryWrapper<>();
wrapper.orderByDesc("create_time");

// 方式三：前端动态传参（QueryGenerator 自动解析）
// ?column=create_time&order=desc
```

#### 6.4.6 查询操作符速查表

| 前端操作符 | SQL 条件 | 示例 |
|-----------|---------|------|
| `eq` | `=` | `?name=张三`（默认） |
| `ne` | `!=` | `?name_ne=张三` |
| `like` | `LIKE %val%` | `?name_like=张` |
| `likeLeft` | `LIKE %val` | `?name_likeLeft=张` |
| `likeRight` | `LIKE val%` | `?name_likeRight=张` |
| `gt` | `>` | `?amount_gt=100` |
| `ge` | `>=` | `?amount_ge=100` |
| `lt` | `<` | `?amount_lt=1000` |
| `le` | `<=` | `?amount_le=1000` |
| `between` | `BETWEEN` | `?billDate_begin=xxx&billDate_end=xxx` |

</details>

