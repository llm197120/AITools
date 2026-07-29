---
name: 架构总览与路线图
overview: 基于 JeecgBoot + JeecgUniapp 技术栈，设计一套完整的家庭AI小工具系统方案，包含微信小程序用户端和PC浏览器管理端，覆盖AI对话、资料存储（含Office处理）、账单管理、日常计划、烹饪指南、学习模块六大功能模块。
todos:
  - id: phase-1-foundation
    content: 第一阶段：基础设施 + 微信登录 + 家庭模型
    status: in_progress
  - id: phase-2a-ai-chat
    content: 第二阶段A：AI基础对话 + 密钥配置 + SSE流式
    status: pending
  - id: phase-2b-ai-multimodal
    content: 第二阶段B：多模态上传 + Token配额管理 + 对话历史管理
    status: pending
  - id: phase-3-bill
    content: 第三阶段：账单模块
    status: pending
  - id: phase-4-plan
    content: 第四阶段：日常计划 + 微信消息提醒
    status: pending
  - id: phase-5-storage-office
    content: 第五阶段：资料存储 + Office处理
    status: pending
  - id: phase-6-recipe-learn
    content: 第六阶段：烹饪指南 + 学习模块
    status: pending
  - id: phase-7-polish
    content: 第七阶段：权限配置 + 安全加固 + 性能优化
    status: pending
isProject: false
---

> 本文档摘自 [architecture-design.md](architecture-design.md) 拆分，内容同步 v8 版本

# 家庭AI小工具 - 架构总览与路线图 v8

## 目录

- [一、项目概述](#一项目概述)
  - [1.1 项目定位](#1.1-项目定位)
  - [1.2 技术架构总览](#1.2-技术架构总览)
  - [1.3 核心依赖](#1.3-核心依赖)
  - [1.4 关键外部依赖清单](#1.4-关键外部依赖清单)
- [二、家庭（Family）数据模型](#二家庭family数据模型)
  - [2.1 设计动机](#2.1-设计动机)
  - [2.2 数据共享策略](#2.2-数据共享策略)
  - [2.3 家庭相关 API](#2.3-家庭相关-api)
  - [2.4 用户关联家庭后的行为变化](#2.4-用户关联家庭后的行为变化)
  - [2.5 家庭生命周期规则](#2.5-家庭生命周期规则)
- [三、项目模块结构](#三项目模块结构)
  - [3.1 目录结构规划](#3.1-目录结构规划)
    - [后端模块](#后端模块)
  - [3.2 模块注册方式](#3.2-模块注册方式)
  - [3.3 小程序分包策略](#3.3-小程序分包策略)
- [十、文件存储方案](#十文件存储方案)
- [十一、开发路线图](#十一开发路线图)
  - [第一阶段：基础设施 + 微信登录 + 家庭模型（预计2-3周）](#第一阶段基础设施-+-微信登录-+-家庭模型预计2-3周)
  - [第二阶段A：AI基础对话（预计1周）](#第二阶段aai基础对话预计1周)
  - [第二阶段B：多模态 + Token配额（预计1周）](#第二阶段b多模态-+-token配额预计1周)
  - [第三阶段：账单模块（预计1-2周）](#第三阶段账单模块预计1-2周)
  - [第四阶段：日常计划 + 消息提醒（预计1-2周）](#第四阶段日常计划-+-消息提醒预计1-2周)
  - [第五阶段：资料存储 + Office处理（预计2-3周）](#第五阶段资料存储-+-office处理预计2-3周)
  - [第六阶段：烹饪指南 + 学习模块（预计2-3周）](#第六阶段烹饪指南-+-学习模块预计2-3周)
  - [第七阶段：收尾优化（预计1周）](#第七阶段收尾优化预计1周)
- [十二、扩展性设计](#十二扩展性设计)
  - [12.1 模块化设计](#12.1-模块化设计)
  - [12.2 通用组件规划](#12.2-通用组件规划)
  - [12.3 可扩展模块示例](#12.3-可扩展模块示例)
- [十三、关键实现注意事项](#十三关键实现注意事项)

---

## 一、项目概述

### 1.1 项目定位

家庭日常使用的AI小工具合集，通过微信小程序提供便捷的移动端服务，通过PC浏览器提供完善的管理后台，以AI能力为核心驱动。

### 1.2 技术架构总览

```
微信小程序 (JeecgUniapp)
  AI对话 | 资料存储（含Office处理）| 账单记录 | 日常计划 | 烹饪指南 | 学习模块
         |
    HTTP/WebSocket
         |
后端服务 (JEECGBOOT Spring Boot)
  用户认证(Shiro+JWT) | 家庭管理 | AI服务(LangChain4j+LiteFlow) | 文件处理(OSS) | 业务模块服务
         |
   MySQL + Redis + MinIO
         |
PC管理端 (JEECGBOOT Vue3 + Ant Design)
  用户管理 | 家庭管理 | AI配置 | 文件+Office管理 | 账单管理 | 计划管理 | 烹饪管理 | 学习管理
```

### 1.3 核心依赖


| 层级    | 技术栈                                                 |
| ----- | --------------------------------------------------- |
| 后端框架  | Spring Boot 4.1.0 (via JeecgBoot 3.9.3), Java 17    |
| AI框架  | LangChain4j 1.17.2 + LiteFlow 2.15.0 (jeecg-aiflow) |
| 数据库   | MySQL + Redis                                       |
| 对象存储  | MinIO / 阿里云OSS                                      |
| 小程序前端 | uni-app 3.0 + Vue 3 + wot-design-uni                |
| 管理端前端 | Vue 3 + Ant Design Vue 4 + Vite 8                   |
| 权限框架  | Apache Shiro 3.0 + JWT                              |


### 1.4 关键外部依赖清单


| 功能              | 依赖                               | 说明                |
| --------------- | -------------------------------- | ----------------- |
| 微信账单CSV解析       | `apache-commons-csv` 或 `opencsv` | 导入微信支付账单          |
| Office格式转换      | `jodconverter` + LibreOffice     | .docx -> .pdf 等   |
| Word模板生成        | `poi-tl`                         | AI生成内容填充模板输出.docx |
| Excel生成         | `Apache POI` (已有)                | AI生成结构化数据输出.xlsx  |
| Markdown渲染(小程序) | `towxml` 或 `mp-html`             | 对话消息渲染            |
| 服务端图表生成         | `JFreeChart` 或后端渲染ECharts        | 生成分享图片用           |


## 二、家庭（Family）数据模型

### 2.1 设计动机

方案中包含"家庭成员"概念（账单共享、菜谱共享、资料共享），但之前各模块的数据隔离策略不一致。引入统一的"家庭"模型解决问题。

### 2.2 数据共享策略

**关键前提：家庭是可选的。** 用户可以不加入任何家庭，此时所有模块按个人模式运行。加入家庭后，部分模块自动切换到家庭共享模式。

```
┌─────────────────────────────────────────┐
│              Family (家庭)                │
│  creator: 创建者（一个微信用户）           │
├─────────────────────────────────────────┤
│  Member 1 (爸爸)   Member 2 (妈妈)        │
│  Member 3 (孩子)   Member 4 (其他)        │
└─────────────────────────────────────────┘
```


| 模块       | 无家庭时  | 有家庭时          |
| -------- | ----- | ------------- |
| AI对话     | 按用户隔离 | 按用户隔离（不变）     |
| 资料存储（含Office） | 仅自己   | 新增"家庭共享"可见性选项 |
| 账单       | 个人记账  | 家庭内共享，统一统计    |
| 日常计划     | 按用户隔离 | 按用户隔离（不变）     |
| 烹饪指南     | 仅自己   | 新增"家庭共享"可见性选项 |
| 学习模块     | 仅自己   | 新增"家庭共享"可见性选项 |


### 2.3 家庭相关 API


| 方法     | 路径                           | 说明               |
| ------ | ---------------------------- | ---------------- |
| GET    | `/homeai/family/info`        | 获取当前家庭信息               |
| POST   | `/homeai/family`             | 创建家庭（创建者自动成为管理员） |
| POST   | `/homeai/family/invite-code`  | 生成6位字母数字邀请码      |
| POST   | `/homeai/family/members`     | 通过邀请码加入家庭        |
| GET    | `/homeai/family/members`     | 获取家庭成员列表         |
| DELETE | `/homeai/family/member/{id}` | 移除成员（仅管理员可操作）      |
| DELETE | `/homeai/family/leave`       | 主动退出家庭           |
| DELETE | `/homeai/family/disband`     | 解散家庭（仅创建者）       |
| PUT    | `/homeai/family`                | 修改家庭信息（名称等）      |
| PUT    | `/homeai/family/member/{id}/role` | 修改成员角色(管理员/普通/受限)  |
| POST   | `/homeai/family/transfer` | 转让管理员身份给其他成员      |


### 2.4 用户关联家庭后的行为变化

- **小程序首页**：每个模块的可见性不变，但账单、烹饪等模块内部数据范围自动变为家庭共享
- **管理端**：用户列表增加"家庭"列，可按家庭筛选；家庭管理页可查看所有家庭及其成员

### 2.5 家庭生命周期规则

**创建家庭**

- 任何微信用户可创建家庭，一个用户只能属于一个家庭
- 创建者自动成为家庭管理员（管理员身份可转让给其他成员）
- **管理员转让逻辑**：新管理员需同意（通过小程序消息确认），确认后原管理员降级为普通成员；转让后原创建者身份标记保留（用于解散等高级操作）

**成员角色与权限等级**

家庭内成员分为三个角色等级：

| 角色     | 权限说明                                                       |
| -------- | -------------------------------------------------------------- |
| 管理员   | 可修改家庭名称、移除成员、解散家庭、管理所有共享数据、转让管理员给其他成员 |
| 普通成员 | 可查看共享数据、创建/编辑自己的内容、上传文件                      |
| 受限成员 | 仅可查看共享数据（不可创建/编辑/删除任何内容），适用于儿童账户          |

- 管理员可为每个成员设置角色（默认「普通成员」）
- 「受限成员」在全系统各个模块的权限统一如下：
  - **文件**：只能查看和下载家庭共享文件，不能上传、编辑、重命名、删除文件或文件夹
  - **账单**：只能查看家庭账单，不能新增、编辑、删除账单（包括自己创建的）
  - **菜谱**：只能查看家庭共享菜谱，不能新增、编辑、删除菜谱
  - **计划**：只能查看自己和其他人分配给自己的计划，不能新建
  - **学习**：可查看学习资料和创建自己的学习笔记，不能上传资料
  - **AI对话**：可使用AI对话（消耗家庭Token），但不可查看其他成员的对话记录
  - **家庭管理**：不可查看家庭管理页，不可邀请/移除成员
- 角色由管理员在家庭管理页中调整

**加入家庭**

- 创建者在小程序"我的家庭"页面点击"生成邀请码"，生成6位字母数字邀请码（有效期24小时）
- 邀请码通过微信转发给家人或口头告知
- 其他用户在小程序输入邀请码加入家庭

**退出家庭**

- **管理员转让前置**：如果管理员是唯一的管理员，退出前必须先转让管理员身份给其他普通成员（不能直接退出导致家庭无人管理）
- **唯一成员情况**：如果家庭只剩一人且该成员退出，则自动触发解散流程（数据进入30天保留期）
- **主动退出**：用户可随时退出，退出后：
  - 该用户创建的账单保留在家庭中（家庭共享数据归属于家庭）
  - 该用户创建的菜谱如果为"家庭共享"，则保留为家庭共享菜谱
  - 该用户创建的"家庭共享"文件夹中的文件保留在原位置
  - 该用户的 AI 对话、计划、学习记录、个人文件（仅自己）跟随用户离开
- **成员被移除**：同上，由管理员操作（被移除的成员将收到小程序消息通知）
- **用户注销账号**：用户可在个人中心自行注销账号，注销后：
  - 如果是家庭成员：先自动退出家庭（按退出规则处理）
  - 该用户的个人数据（对话、计划、学习记录）标记匿名化（user_id置空，保留内容）
  - 注销操作记录审计日志
  - 账号注销后不可恢复，需重新注册

**解散家庭**

- 仅管理员可操作，解散前弹出郑重二次确认弹窗
  - 确认文案：「解散家庭将删除所有家庭共享数据，此操作不可撤销。所有家庭成员将恢复为无家庭状态。」
  - 需输入"确认解散"文字方可执行（防止误触）
- 解散后共享数据变化：
  - 家庭共享数据（账单、共享菜谱、共享文件夹中的文件）进入 30 天保留期，到期前 7 天推送消息通知所有成员备份
  - 所有成员的个人数据（AI对话、个人计划、未共享的菜谱/文件）不受影响，跟随用户离开
  - 保留期结束后物理删除，写入审计日志
- 所有成员恢复为无家庭状态，可自由加入/创建新家庭

## 三、项目模块结构

### 3.1 目录结构规划

#### 后端模块

在 `JeecgBoot/jeecg-boot/jeecg-boot-module/` 下新增 `jeecg-boot-module-homeai` 模块。

```
jeecg-boot-module-homeai/
├── src/main/java/org/jeecg/modules/homeai/
│   ├── family/          -- 家庭管理模块（新增）
│   ├── ai/              -- AI对话模块
│   ├── storage/         -- 资料存储模块（含Office处理、格式转换、AI文件生成）
│   ├── bill/            -- 账单模块
│   ├── plan/            -- 日常计划模块
│   ├── recipe/          -- 烹饪指南模块
│   ├── learn/           -- 学习模块
│   ├── user/            -- 微信用户管理
│   └── config/          -- AI密钥与额度配置
```

### 3.2 模块注册方式

- **后端**：在 `jeecg-boot/pom.xml` 中添加 `<module>jeecg-boot-module-homeai</module>`，在 `jeecg-system-start/pom.xml` 中添加依赖引用
- **管理端**：在 `src/router/routes/modules/` 下新增 route 文件（自动被 glob import 发现），菜单权限通过后端 `sys_permission` 表配置
- **小程序**：在 `vite.config.ts` 的 `subPackages` 数组中添加分包，使用 `<route>` 块声明页面路由

### 3.3 小程序分包策略

微信小程序单个分包大小限制为 2MB，为避免超出限制，将 6 个模块拆分为两个分包：

```
分包一: pages-homeai-core/       -- 热启动优先加载
  ├── index.vue                  -- 首页（九宫格入口）
  └── ai/                        -- AI对话（用户第一触达的功能）

分包二: pages-homeai-more/       -- 按需加载
  ├── storage/                   -- 资料存储（含Office处理）
  ├── bill/                      -- 账单记录
  ├── plan/                      -- 日常计划
  ├── recipe/                    -- 烹饪指南
  └── learn/                     -- 学习模块
```

- 分包一控制在 1.5MB 以内，为首页图片和 AI 对话 Markdown 渲染库预留空间
- 分包二控制在 1.8MB 以内
- 通用组件（HomeUpload、HomeCalendar、HomeChart）放于分包一中，分包二复用

<details>
<summary><b>📦 JeecgBoot 标准 Maven 模块配置（点击展开）</b></summary>

> **Maven 模块注册**：在 `jeecg-boot/jeecg-boot/pom.xml` 的 `<modules>` 中添加新模块：
>
> ```xml
> <!-- 在 jeecg-boot/pom.xml 的 <modules> 中添加 -->
> <module>jeecg-boot-module-homeai</module>
> ```
>
> **父 POM 配置模板**：新建 `jeecg-boot-module-homeai/pom.xml`，继承 JeecgBoot 父 POM：
>
> ```xml
> <?xml version="1.0" encoding="UTF-8"?>
> <project xmlns="http://maven.apache.org/POM/4.0.0"
>          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
>          xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
>          http://maven.apache.org/xsd/maven-4.0.0.xsd">
>     <modelVersion>4.0.0</modelVersion>
>     <artifactId>jeecg-boot-module-homeai</artifactId>
>     <version>3.9.3</version>
>     <packaging>jar</packaging>
>     <parent>
>         <groupId>org.jeecgframework.boot</groupId>
>         <artifactId>jeecg-boot</artifactId>
>         <version>3.9.3</version>
>         <relativePath>../pom.xml</relativePath>
>     </parent>
>
>     <dependencies>
>         <!-- 依赖系统核心模块 -->
>         <dependency>
>             <groupId>org.jeecgframework.boot</groupId>
>             <artifactId>jeecg-boot-module-system</artifactId>
>         </dependency>
>         <!-- AI 编排能力（LangChain4j + LiteFlow） -->
>         <dependency>
>             <groupId>org.jeecgframework.boot</groupId>
>             <artifactId>jeecg-boot-starter-ai</artifactId>
>         </dependency>
>         <!-- AI RAG 知识库 -->
>         <dependency>
>             <groupId>org.jeecgframework.boot</groupId>
>             <artifactId>jeecg-boot-module-airag</artifactId>
>         </dependency>
>     </dependencies>
> </project>
> ```
>
> **自动注册机制**：JeecgBoot 启动类通过 `@SpringBootApplication` 组合注解自动扫描 `org.jeecg` 包下所有子包，新模块遵循包路径 `org.jeecg.modules.homeai` 即可自动注册，无需额外配置：
>
> ```java
> // jeecg-system-start 启动类（已有代码，无需修改）
> @SpringBootApplication(scanBasePackages = {"org.jeecg"})
> @MapperScan({"org.jeecg.**.mapper"})
> public class JeecgSystemApplication {
>     public static void main(String[] args) {
>         SpringApplication.run(JeecgSystemApplication.class, args);
>     }
> }
> ```
>
> **依赖引入**：还需在 `jeecg-system-start/pom.xml` 中添加对 `jeecg-boot-module-homeai` 的直接依赖，确保启动时加载：
>
> ```xml
> <dependency>
>     <groupId>org.jeecgframework.boot</groupId>
>     <artifactId>jeecg-boot-module-homeai</artifactId>
> </dependency>
> ```

</details>

## 十、文件存储方案

采用JEECG内置的OSS配置：


| 存储类型   | 适用场景   |
| ------ | ------ |
| 本地存储   | 开发环境   |
| MinIO  | 自建生产环境 |
| 阿里云OSS | 云上生产环境 |


文件路径规则：`homeai/{module}/{userId}/{yyyyMM}/{uuid}.{ext}`

## 十一、开发路线图

### 第一阶段：基础设施 + 微信登录 + 家庭模型（预计2-3周）

- 后端模块创建：新建 `jeecg-boot-module-homeai`，在 pom.xml 中添加对 `jeecg-boot-module-airag` 和 `jeecg-boot-starter-ai` 的 Maven 依赖
- 管理端路由配置：新增homeai菜单与路由
- 小程序首页：创建 `pages-homeai-core` 和 `pages-homeai-more` 两个分包，九宫格入口页面
- 微信登录集成：`wx.login` -> JWT -> 用户自动注册/登录
- 家庭模型：家庭创建、邀请、加入、成员管理

### 第二阶段A：AI基础对话（预计1周）

- AI密钥配置页（管理端CRUD，AES加密存储）
- SSE流式对话后端接口（发送消息、停止生成、消息历史）
- 小程序聊天页面（消息气泡、输入区域、Markdown渲染、停止按钮）

### 第二阶段B：多模态 + Token配额（预计1周）

- 小程序图片/文件上传到对话
- Token配额管理（日/月Token限额、每次对话记录消耗）
- 管理端配额配置页 + 统计概览
- 对话历史管理（重命名、删除、搜索）

### 第三阶段：账单模块（预计1-2周）

- 消费分类管理
- 记一笔 + 按家庭共享的账单列表 + 编辑/删除
- 批量导入（微信账单CSV解析映射）
- 统计报表（饼图、趋势图、家庭合并报表）

### 第四阶段：日常计划 + 消息提醒（预计1-2周）

- 日历视图 + 计划CRUD
- 重复计划预生成实例（90天滚动）
- 管理端计划查看 + 完成率统计
- 微信订阅消息提醒 + Quartz定时任务

### 第五阶段：资料存储 + Office处理（预计2-3周）

- 文件夹树形管理 + 共享可见性
- 文件上传（白名单校验）+ 下载 + 预览
- 搜索 + 收藏 + 批量操作
- **Office处理集成**：格式转换（jodconverter）+ 模板管理 + AI文件生成（POI模板填充）
- 文件列表长按菜单增加"Office处理"子菜单
- Office处理历史记录

### 第六阶段：烹饪指南 + 学习模块（预计2-3周）
- 烹饪：菜谱CRUD + 分类 + 步骤图文展示 + **做菜视频** + 可见性控制
- 学习：资料管理 + 计时/手动双模式学习 + 学习记录 + 进度跟踪

### 第七阶段：收尾优化（预计1周）

- 权限配置完善 + 安全加固（XSS过滤、敏感信息脱敏）
- 文件白名单管理页
- 错误处理 + 性能优化 + 文档完善

## 十二、扩展性设计

### 12.1 模块化设计

- 后端每个功能为独立子包，新增直接加子包
- 管理端每个功能为独立路由文件，自动发现
- 小程序每个功能为独立页面，分包加载

### 12.2 通用组件规划

- 小程序：`HomeUpload`（文件上传，内置白名单校验）、`HomeCalendar`（日历）、`HomeChart`（统计图表）、`HomeOfficeConvert`（Office格式转换）、`HomeFileIcon`（文件类型图标，统一管理各格式图标）
- 管理端：利用JEECG现有 `BasicTable`、`BasicForm`、`BasicDrawer` 满足CRUD

### 12.3 可扩展模块示例


| 待定模块 | 简要说明            |
| ---- | --------------- |
| 家庭健康 | 家庭成员健康数据记录、体检提醒 |
| 家庭记事 | 共享留言板、家庭公告      |
| 资产管理 | 家庭固定资产登记        |
| 节日提醒 | 家庭成员生日/纪念日提醒    |


## 附录：JeecgBoot 代码模板与框架配置

<details>
<summary><b>📄 基础代码模板 —— JeecgBoot 三层架构（点击展开）</b></summary>

> **实体类（Entity）**：继承 `JeecgEntity` 基类，自动获得 `createBy`、`createTime`、`updateBy`、`updateTime`、`delFlag` 审计字段。
>
> ```java
> package org.jeecg.modules.homeai.bill.entity;
>
> import com.baomidou.mybatisplus.annotation.TableName;
> import lombok.Data;
> import lombok.EqualsAndHashCode;
> import org.jeecgframework.poi.excel.annotation.Excel;
> import org.jeecg.system.base.entity.JeecgEntity;
>
> /**
>  * 账单条目
>  */
> @Data
> @TableName("homeai_bill_entry")
> @EqualsAndHashCode(callSuper = true)
> public class BillEntry extends JeecgEntity {
>
>     private static final long serialVersionUID = 1L;
>
>     /** 家庭ID */
>     private String familyId;
>
>     /** 消费金额（分） */
>     private Long amount;
>
>     /** 消费分类 */
>     @Excel(name = "消费分类", width = 15)
>     private String category;
>
>     /** 备注 */
>     @Excel(name = "备注", width = 20)
>     private String remark;
>
>     /** 消费时间 */
>     private java.util.Date billDate;
>
>     /** 数据来源（manual=手动录入 / wechat_csv=微信导入） */
>     private String sourceType;
> }
> ```
>
> **Controller**：继承 `JeecgController` 基类，自动获得标准 CRUD 接口（分页列表、增删改查、导入导出）。通过 `@RequiresPermissions` 实现按钮级权限控制。
>
> ```java
> package org.jeecg.modules.homeai.bill.controller;
>
> import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
> import jakarta.servlet.http.HttpServletRequest;
> import org.jeecg.common.api.vo.Result;
> import org.jeecg.common.system.query.QueryGenerator;
> import org.jeecg.modules.homeai.bill.entity.BillEntry;
> import org.jeecg.modules.homeai.bill.service.IBillEntryService;
> import org.jeecg.system.base.controller.JeecgController;
> import org.springframework.web.bind.annotation.*;
> import io.swagger.v3.oas.annotations.Operation;
> import io.swagger.v3.oas.annotations.tags.Tag;
> import org.apache.shiro.authz.annotation.RequiresPermissions;
>
> /**
>  * 账单管理 控制器
>  */
> @RestController
> @RequestMapping("/homeai/bill")
> @RequiresPermissions("homeai:bill:entry")
> @Tag(name = "账单管理", description = "账单模块管理接口")
> public class BillEntryController extends JeecgController<BillEntry, IBillEntryService> {
>
>     @GetMapping("/list")
>     @Operation(summary = "账单列表")
>     @RequiresPermissions("homeai:bill:entry:list")
>     public Result<?> queryPageList(BillEntry billEntry,
>                                    @RequestParam(name = "pageNo", defaultValue = "1") Integer pageNo,
>                                    @RequestParam(name = "pageSize", defaultValue = "10") Integer pageSize,
>                                    HttpServletRequest req) {
>         QueryWrapper<BillEntry> queryWrapper = QueryGenerator.initQueryWrapper(billEntry, req.getParameterMap());
>         return Result.OK(service.page(new Page<>(pageNo, pageSize), queryWrapper));
>     }
>
>     @PostMapping("/add")
>     @Operation(summary = "新增账单")
>     @RequiresPermissions("homeai:bill:entry:add")
>     public Result<?> add(@RequestBody BillEntry billEntry) {
>         service.save(billEntry);
>         return Result.OK("新增成功");
>     }
>
>     @PutMapping("/edit")
>     @Operation(summary = "编辑账单")
>     @RequiresPermissions("homeai:bill:entry:edit")
>     public Result<?> edit(@RequestBody BillEntry billEntry) {
>         service.updateById(billEntry);
>         return Result.OK("编辑成功");
>     }
>
>     @DeleteMapping("/delete")
>     @Operation(summary = "删除账单")
>     @RequiresPermissions("homeai:bill:entry:delete")
>     public Result<?> delete(@RequestParam(name = "id") String id) {
>         service.removeById(id);
>         return Result.OK("删除成功");
>     }
> }
> ```
>
> **Service 接口与实现**：
>
> ```java
> package org.jeecg.modules.homeai.bill.service;
>
> import com.baomidou.mybatisplus.extension.service.IService;
> import org.jeecg.modules.homeai.bill.entity.BillEntry;
>
> public interface IBillEntryService extends IService<BillEntry> {
> }
> ```
>
> ```java
> package org.jeecg.modules.homeai.bill.service.impl;
>
> import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
> import org.jeecg.modules.homeai.bill.entity.BillEntry;
> import org.jeecg.modules.homeai.bill.mapper.BillEntryMapper;
> import org.jeecg.modules.homeai.bill.service.IBillEntryService;
> import org.springframework.stereotype.Service;
>
> @Service
> public class BillEntryServiceImpl extends ServiceImpl<BillEntryMapper, BillEntry> implements IBillEntryService {
> }
> ```

</details>

<details>
<summary><b>⚙️ MyBatis-Plus 插件配置（点击展开）</b></summary>

> JeecgBoot 在 `MybatisPlusConfig` 中已默认配置分页插件。HomeAI 模块如有自定义配置需求，可参考以下模板在模块内补充：
>
> ```java
> package org.jeecg.modules.homeai.config;
>
> import com.baomidou.mybatisplus.annotation.DbType;
> import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
> import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
> import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
> import org.springframework.context.annotation.Bean;
> import org.springframework.context.annotation.Configuration;
>
> /**
>  * HomeAI 模块 MyBatis-Plus 插件配置
>  *
>  * 分页插件     — 自动拼接 LIMIT 语句，支持多数据库方言
>  * 乐观锁插件    — 通过 @Version 注解实现并发安全更新
>  * 逻辑删除     — 全局注入 del_flag=0 条件（JeecgBoot 默认开启，无需重复配置）
>  */
> @Configuration
> public class HomeaiMybatisPlusConfig {
>
>     @Bean
>     public MybatisPlusInterceptor mybatisPlusInterceptor() {
>         MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
>         // 分页插件（MySQL 方言）
>         interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
>         // 乐观锁插件（实体类字段加 @Version 注解生效）
>         interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
>         return interceptor;
>     }
> }
> ```
>
> **说明**：
> - **分页**：JeecgBoot 基类 `JeecgController` 的 `queryPageList` 方法已内置分页，开发 Controller 时直接复用即可
> - **乐观锁**：如需使用，在实体类金额等关键字段加 `@Version`，更新时 MP 自动校验版本号
> - **逻辑删除**：JeecgBoot 全局配置 `mybatis-plus.global-config.db-config.logic-delete-field=del_flag`，所有继承 `JeecgEntity` 的实体自动生效，查询时自动追加 `del_flag=0`

</details>

<details>
<summary><b>🔐 Shiro + JWT 权限体系说明（点击展开）</b></summary>

> JeecgBoot 采用 **Shiro + JWT** 的无状态认证方案，HomeAI 模块直接复用，无需额外配置。
>
> #### 认证流程
>
> ```
> 客户端请求
>     ↓
> 携带 X-Access-Token 请求头
>     ↓
> Shiro JwtFilter 拦截 → 解析 Token → 验证签名 → 设置 SecurityContext
>     ↓
> 进入 Controller → @RequiresPermissions 鉴权
> ```
>
> #### 关键注解
>
> | 注解 | 作用 | 使用位置 |
> |------|------|----------|
> | `@RequiresAuthentication` | 要求用户已登录（JWT Token 有效） | Controller 类 / 方法 |
> | `@RequiresPermissions("homeai:bill:entry:list")` | 要求当前用户拥有指定权限 | Controller 方法 |
> | `@RequiresRoles("admin")` | 要求当前用户拥有指定角色 | Controller 方法 |
> | `@PermissionData` | 数据权限过滤，自动拼接数据范围 SQL | Service 方法 |
>
> #### 权限配置流程
>
> 1. **后端**：在 Controller 方法的 `@RequiresPermissions` 中声明权限标识（如 `homeai:bill:entry:add`）
> 2. **管理端**：JeecgBoot 自动扫描注册到 `sys_permission` 表（通过 `META-INF/JeecgBootPermission.txt` 或在线菜单配置）
> 3. **分配**：在管理端的"角色管理"中为角色勾选对应菜单/按钮权限
> 4. **生效**：用户下次请求时，Shiro 自动校验权限标识
>
> #### 数据权限注解 `@PermissionData`
>
> ```java
> @RequestMapping("/homeai/bill")
> @RestController
> public class BillEntryController extends JeecgController<BillEntry, IBillEntryService> {
>
>     @GetMapping("/list")
>     @RequiresPermissions("homeai:bill:entry:list")
>     @PermissionData(title = "家庭账单列表")  // 自动根据用户角色过滤数据
>     public Result<?> queryPageList(...) { ... }
> }
> ```
>
> #### JeecgEntity 基类字段
>
> 所有业务实体继承 `JeecgEntity` 后自动包含以下字段，由 MyBatis-Plus 自动填充：
>
> | 字段 | 类型 | 说明 | 填充时机 |
> |------|------|------|----------|
> | `createBy` | `String` | 创建人登录名 | insert 时自动填充 |
> | `createTime` | `Date` | 创建时间 | insert 时自动填充 |
> | `updateBy` | `String` | 更新人登录名 | update 时自动填充 |
> | `updateTime` | `Date` | 更新时间 | update 时自动填充 |
> | `delFlag` | `String` | 逻辑删除标记（0=正常，1=删除） | 全局过滤 |

</details>

<details>
<summary><b>🛡️ 数据权限与家庭隔离设计（点击展开）</b></summary>

> JeecgBoot 的 `@PermissionData` 数据权限机制可与家庭隔离策略无缝结合，实现"用户只能看到自己家庭的数据"。
>
> #### 方案一：利用 `@PermissionData` 实现家庭级过滤（推荐）
>
> JeecgBoot 的数据权限本质是：在 SQL 执行前自动追加数据范围条件。通过自定义数据权限规则，实现家庭维度隔离：
>
> ```java
> @Service
> public class BillEntryServiceImpl extends ServiceImpl<BillEntryMapper, BillEntry> implements IBillEntryService {
>
>     /**
>      * 查询当前用户家庭的账单
>      * 通过 JeecgBoot 的 QueryGenerator 拼装 family_id 条件
>      */
>     public Page<BillEntry> queryFamilyBills(Page<BillEntry> page, BillEntry param, HttpServletRequest req) {
>         // 1. 获取当前登录用户的家庭 ID（从 Shiro 上下文 / Token 中）
>         LoginUser user = (LoginUser) SecurityUtils.getSubject().getPrincipal();
>         String familyId = user.getIdentityClaim("familyId");
>
>         // 2. 构建查询条件
>         QueryWrapper<BillEntry> wrapper = QueryGenerator.initQueryWrapper(param, req.getParameterMap());
>         if (StringUtils.isNotBlank(familyId)) {
>             wrapper.eq("family_id", familyId);
>         }
>
>         // 3. 分页查询
>         return page(page, wrapper);
>     }
> }
> ```
>
> #### 方案二：`@PermissionData` 注解 + 角色规则配置（管理端）
>
> 通过 JeecgBoot 管理端的"数据规则配置"页面，为 `homeai:bill:entry:list` 权限配置数据规则：
>
> | 规则字段 | 条件 | 规则值 | 说明 |
> |----------|------|--------|------|
> | `family_id` | `=` | `#{sys.user.identityClaim('familyId')}` | 自动带入当前用户的家庭 ID |
>
> 此方案无需修改代码，纯配置化实现数据隔离，适合管理端的通用查询。
>
> #### 方案三：`sys_org_code` 机构隔离（可选）
>
> 如果未来需要扩展"多家族"维度，可利用 JeecgBoot 现有的 `sys_org_code` 字段实现：
>
> - 每条数据的 `sys_org_code` 在创建时自动赋值为用户的机构编码
> - 数据权限规则设为：`sys_org_code LIKE #{sys.orgCode}||'%'`
> - 支持树形机构层级，用户能看到所属机构及其下级机构的数据
>
> #### 家庭隔离策略对照表
>
> | 模块 | 无家庭 | 有家庭（小程序） | 管理端查询 |
> |------|--------|-----------------|-----------|
> | 账单 | `user_id = 当前用户` | `family_id = 当前家庭` | 支持按家庭筛选 |
> | 资料存储 | `user_id = 当前用户` | `user_id = 当前用户 OR (family_id = 当前家庭 AND visibility='family')` | 支持按家庭筛选 |
> | 烹饪指南 | `user_id = 当前用户` | `user_id = 当前用户 OR (family_id = 当前家庭 AND visibility='family')` | 支持按家庭筛选 |
> | AI对话 | `user_id = 当前用户` | `user_id = 当前用户`（不变） | 仅个人可见 |
> | 日常计划 | `user_id = 当前用户` | `user_id = 当前用户`（不变） | 仅个人可见 |
> | 学习模块 | `user_id = 当前用户` | `user_id = 当前用户 OR (family_id = 当前家庭 AND visibility='family')` | 支持按家庭筛选 |

</details>


## 十三、关键实现注意事项

1. **AI对话流式响应**：SSE实现，小程序使用 `uni.request` 的 `enableChunked` 模式，支持中途停止
2. **Token配额**：每次对话完成后记录 Token 消耗，从日/月额度扣除
3. **文件转换（Office）**：资料存储中内置，`jodconverter` + LibreOffice，异步+轮询
4. **AI文件生成（Office）**：资料存储中内置，AI生成结构化内容 -> `Apache POI` / `poi-tl` 填充模板 -> 输出文件
5. **账单导入**：`apache-commons-csv` 解析微信支付CSV，映射分类，去重
6. **重复计划**：预生成90天实例，每天 Quartz 滚动生成，不动态计算
7. **数据隔离**：有家庭按家庭隔离（账单/菜谱/资料），无家庭按用户隔离
8. **账单AI识别**：上传非标准格式文件（银行PDF/Excel）-> 后端解析文本 -> AI提取结构化数据 -> 前端二次确认 -> 写入系统
9. **烹饪视频**：视频文件单独存储，关联菜谱ID；列表页卡片左上角显示🎬标记；详情页提供播放入口
10. **资料存储格式扩展**：文件白名单管理端动态配置，前端 `HomeFileIcon` 组件统一管理文件类型图标，预览组件按扩展名路由
11. **加密存储**：AI密钥 AES-256-GCM 加密存储（认证加密模式，自带完整性校验），前端脱敏
12. **文件安全**：白名单校验 + 文件魔数校验，防止恶意上传
13. **XSS防护**：所有用户富文本内容入库前清洗
14. **对话内容加密**：对话消息 AES-256-GCM 加密存储（认证加密模式，自带完整性校验），管理端解密查看，加密密钥与 AI 密钥隔离
15. **小程序分包**：分为 `core`（AI对话）和 `more`（资料存储含Office+其余模块），每个分包不超过2M
16. **邀请码保护**：校验接口限流 5次/分/IP，连续10次失败封禁30分钟
17. **Office规则扩展**：格式转换规则由管理端配置，在资料存储模块中动态展示可选转换目标

> **数据库设计规范（遵循 JeecgBoot 标准）**：
> - **主键**：统一命名为 `id`，类型 `varchar(32)`，单列主键，唯一索引
> - **审计字段**：所有业务表必须包含四件套 — `create_by`(创建人)、`create_time`(创建时间)、`update_by`(更新人)、`update_time`(更新时间)
> - **逻辑删除**：统一使用 `del_flag` 字段，`0`=正常 `1`=已删除
> - **字段注释**：每个字段必须有 `COMMENT`，枚举/状态字段需标注取值规则
> - **命名规范**：表名使用 `homeai_` 前缀，字段使用英文 snake_case，禁止拼音
> - **状态类型**：优先使用 `varchar(1)` / `varchar(2)`，少用 `int`/`tinyint`
> - **字符集**：统一使用 `utf8mb4` + `utf8mb4_unicode_ci`
> - **引擎**：统一使用 `ENGINE=InnoDB` + `ROW_FORMAT=DYNAMIC`
> - **索引命名**：普通索引 `idx_` 前缀，唯一约束 `uniq_` 前缀
> - **索引**：高频查询字段加索引，普通索引 `idx_` 前缀
> - **外键**：业务表不使用外键约束，关联关系由程序维护



## 相关文档

以下文档由同一份架构设计文档拆分而来：

- [模块功能详细设计](./module-details.md) — 模块功能详细说明、接口设计、权限体系
- [AI能力与安全设计](./ai-security.md) — AI能力集成、安全设计、微信消息提醒
- [流程图与数据库设计](./database-flows.md) — 系统流程图、数据库设计
