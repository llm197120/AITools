---
name: 家庭AI小工具 - 开发环境启动指南
version: v1.0
date: 2026-07-29
---

# 家庭AI小工具 — 开发环境启动指南

> **适用版本**：JeecgBoot 3.9.3 + Spring Boot 4.1.0 + Vue3  
> **JDK 要求**：JDK 17+（支持 17/21/24/25）  
> **Node 要求**：Node 18+，推荐 pnpm

---

## 一、目录结构概览

```
AITools/
├── docs/                              # 设计文档
│   ├── plan/                          #   实施计划 + 阶段进度
│   └── design/                        #   数据流 + 模块详情
├── JeecgBoot/                         # 主项目
│   ├── jeecg-boot/
│   │   ├── pom.xml                    #   根 POM（Spring Boot 4.1.0）
│   │   ├── jeecg-module-system/
│   │   │   └── jeecg-system-start/    #   ★ 启动模块
│   │   │       ├── pom.xml            #       依赖 jeecg-boot-module-homeai
│   │   │       └── src/main/resources/
│   │   │           ├── application.yml          # 激活 dev profile
│   │   │           └── application-dev.yml      # ★ 开发环境配置
│   │   └── jeecg-boot-module/
│   │       └── jeecg-boot-module-homeai/        # ★ HomeAI 业务模块
│   │           ├── pom.xml            #       依赖 airag + cloud + ai starter
│   │           ├── sql/               #       ★ 建表 + 菜单 SQL
│   │           ├── doc/               #       代码修改日志
│   │           └── src/main/java/org/jeecg/modules/homeai/
│   │               ├── config/        #       JWT + MyBatis-Plus 配置
│   │               ├── user/          #       微信用户
│   │               ├── family/        #       家庭管理
│   │               ├── ai/            #       AI 对话 + 密钥 + 配额
│   │               ├── storage/       #       资料存储 + Office
│   │               ├── bill/          #       账单模块
│   │               ├── plan/          #       日常计划
│   │               ├── recipe/        #       烹饪指南 + 学习
│   │               └── learn/         #       学习控制器（引用 recipe 实体）
│   ├── jeecgboot-vue3/                # ★ 管理端前端
│   │   ├── package.json               #       pnpm dev
│   │   ├── .env.development           #       代理到 localhost:8080/jeecg-boot
│   │   └── src/views/homeai/          #       HomeAI 页面
│   ├── docker-compose.yml             # Docker 开发环境
│   └── JeecgUniapp/                   # ★ 小程序端（微信开发者工具打开）
│       ├── src/pages.json             #       分包 + 路由注册
│       ├── src/pages-homeai/          #       主功能页（首页/个人中心/家庭）
│       ├── src/pages-homeai-ai/       #       AI 对话分包
│       └── src/pages-homeai-more/     #       其他模块分包
```

---

## 二、环境依赖一览

| 组件 | 版本 | 用途 | 端口 |
|------|------|------|:---:|
| JDK | 17+ | Java 编译运行 | — |
| Maven | 3.8+ | 后端构建 | — |
| MySQL | 8.0+ | 主数据库 | 3306 |
| Redis | 5.0+ | 缓存/Token 配额 | 6379 |
| Node.js | 18+ | 前端构建 | — |
| pnpm | 8+ | 前端包管理 | — |
| HBuilder X | 最新版 | 小程序开发 | — |
| 微信开发者工具 | 最新版 | 小程序调试 | — |
| PostgreSQL | 14+ | AI 向量库（可选） | 5432 |

> **说明**：PostgreSQL 仅 `jeecg-boot-module-airag` 向量存储需要。如果暂不使用 RAG 功能，启动时会自动降级，不影响其他功能。

---

## 三、快速启动步骤

### 第 1 步：准备数据库

**1.1 确保 MySQL 服务已启动**（端口 3306）

**1.2 创建数据库**（如果尚未创建）：

```sql
CREATE DATABASE IF NOT EXISTS `jeecg-boot` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

**1.3 导入 JeecgBoot 基础表**（首次启动会自动执行，也可手动导入）：
- 安装包路径：`jeecg-boot/db/jeecgboot-mysql-*.sql`

**1.4 导入 HomeAI 模块表**：

```sql
USE `jeecg-boot`;
SOURCE JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql/init_homeai_tables.sql;
```

> `init_homeai_tables.sql` 包含 24 张 HomeAI 业务表（全部使用 `CREATE TABLE IF NOT EXISTS`，可重复执行）。

**1.5 导入菜单权限数据**：

```sql
SOURCE JeecgBoot/jeecg-boot/jeecg-boot-module/jeecg-boot-module-homeai/sql/init_homeai_menus.sql;
```

> 菜单 SQL 会注册：账单管理、计划管理、菜谱管理、学习管理 4 个一级菜单及其子页面。

---

### 第 2 步：修改后端配置

编辑 `JeecgBoot/jeecg-boot/jeecg-module-system/jeecg-system-start/src/main/resources/application-dev.yml`：

**2.1 修改数据库连接**（约第 55 行）：

```yaml
datasource:
  master:
    url: jdbc:mysql://localhost:3306/jeecg-boot?characterEncoding=UTF-8&useUnicode=true&useSSL=false&tinyInt1isBit=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai
    username: root
    password: 你的密码
```

**2.2 修改 Redis 连接**（约第 60 行）：

```yaml
redis:
  database: 0
  host: 127.0.0.1
  port: 6379
  password:              # 无密码则留空
```

**2.3 AI 密钥配置**（约第 188 行，可选—可在管理端配置）：

```yaml
jeecg:
  ai-chat:
    enabled: true
    model: deepseek-v4-pro
    apiKey: 你的API密钥
    apiHost: https://api.deepseek.com/v1
    timeout: 60
```

> **注意**：AI 密钥也可以通过管理端「AI 密钥配置」页面动态管理，无需改配置文件。配置文件中设置的是系统默认值。

**2.4 文件上传路径**（约第 280 行）：

```yaml
jeecg:
  path:
    upload: D:/jeecg/upFiles          # Windows 使用绝对路径
    webapp: D:/jeecg/webapp
```

---

### 第 3 步：启动后端

```bash
# 进入后端目录
cd JeecgBoot/jeecg-boot

# 编译安装（首次）
mvn clean install -DskipTests

# 启动（指定 dev profile）
cd jeecg-module-system/jeecg-system-start
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

> **启动类**：`org.jeecg.JeecgSystemApplication`  
> **后端地址**：`http://localhost:8080/jeecg-boot`  
> **Swagger 文档**：`http://localhost:8080/jeecg-boot/doc.html`  
> **Druid 监控**：`http://localhost:8080/jeecg-boot/druid`（admin / 123456）

启动成功后日志会打印：

```
Application Jeecg-Boot is running! Access URLs:
Local:      http://localhost:8080/jeecg-boot
Swagger文档: http://localhost:8080/jeecg-boot/doc.html
```

---

### 第 4 步：启动管理端前端

```bash
# 进入前端目录
cd JeecgBoot/jeecgboot-vue3

# 安装依赖（首次）
pnpm install

# 启动开发服务器
pnpm dev
```

> **管理端地址**：`http://localhost:3100`  
> **默认管理员**：admin / 123456

前端会自动代理 `/jeecgboot` 请求到 `http://localhost:8080/jeecg-boot`（配置在 `.env.development`）。

---

### 第 5 步：启动小程序端

**5.1 用 HBuilder X 打开 `JeecgUniapp` 目录**

**5.2 配置后端地址**（如果未配置）：

检查 `src/pages-homeai/api/request.ts` 中的 `BASE_URL` 设置。

**5.3 运行到微信开发者工具**：
- HBuilder X → 运行 → 运行到小程序模拟器 → 微信开发者工具

---

## 四、验证清单

启动完成后，按以下顺序逐项验证：

### 4.1 后端服务验证

| 验证项 | 地址/方法 | 预期结果 |
|--------|-----------|----------|
| Swagger 文档 | `http://localhost:8080/jeecg-boot/doc.html` | 显示所有 API 分组 |
| Druid 监控 | `http://localhost:8080/jeecg-boot/druid` | 输入 admin/123456 登录 |
| 用户登录 | `POST /jeecg-boot/sys/login` | `{"username":"admin","password":"123456"}` → 返回 token |

### 4.2 管理端页面验证

登录 `http://localhost:3100` 后，左侧菜单应出现以下 HomeAI 模块：

| 一级菜单 | 子页面 | 路由 |
|--------|--------|------|
| AI 对话管理 | 密钥配置 / Token 额度 | `/homeai/ai/keyConfig` `/homeai/ai/quota` |
| 资料存储管理 | 文件管理 / 模板 / 规则 / 记录 | `/homeai/storage/...` |
| 账单管理 | 账单列表 / 消费分类 | `/homeai/bill/billList` `/homeai/bill/billCategory` |
| 计划管理 | 计划列表 | `/homeai/plan/planList` |
| 菜谱管理 | 菜谱列表 | `/homeai/recipe/recipeList` |
| 学习管理 | 学习资料 | `/homeai/learn/learnList` |

### 4.3 HomeAI 专属 API 验证

| 接口 | 说明 |
|------|------|
| `POST /homeai/user/login` | 微信登录（code → JWT） |
| `GET /homeai/user/info` | 获取当前用户信息 |
| `POST /homeai/family` | 创建家庭 |
| `POST /homeai/ai/chat/send` | SSE 流式对话 |
| `GET /homeai/bill/statistics` | 账单月度统计 |
| `GET /homeai/plan/calendar` | 计划日历概览 |
| `GET /homeai/recipe/list` | 菜谱列表 |
| `GET /homeai/learn/materials` | 学习资料列表 |

---

## 五、HomeAI 模块专属配置

### 5.1 JWT 签名密钥

`HomeaiJwtUtil` 使用独立 JWT 体系（区别于管理端 Shiro），签名密钥硬编码在代码中：

```java
// HomeaiJwtUtil.java 第 22 行
private static final String SECRET = "homeai-wxapp-jwt-secret-key-2026";
```

> **生产环境请务必修改此密钥**。

### 5.2 API Key 加密

AI 密钥通过 `HomeaiJwtUtil` 之外的独立 AES-256-GCM 加密存储，密钥从配置读取：

```yaml
# 在 application-dev.yml 中添加
homeai:
  ai:
    key-encryption-key: 你的32字节密钥
```

### 5.3 Token 配额

- 日配额：10,000 tokens
- 月配额：200,000 tokens
- 最多透支：1,000 tokens
- 配额通过 Redis 原子计数，DB 持久化审计

### 5.4 文件上传

上传路径在 `jeecg.path.upload` 配置，默认为 `/opt/upFiles`（Linux）。Windows 开发请改为 `D:/jeecg/upFiles` 或类似路径。

---

## 六、常见问题排查

| 问题 | 原因 | 解决 |
|------|------|------|
| 启动报 `no database selected` | 未创建数据库 | 执行 `CREATE DATABASE jeecg-boot` |
| 启动报 `table doesn't exist` | 未导入 HomeAI 表 | 执行 `init_homeai_tables.sql` |
| 菜单不显示 | 未导入菜单 SQL | 执行 `init_homeai_menus.sql` |
| 前端 404 | 路由未注册 | 检查 `src/router/routes/modules/homeai/*.ts` 是否被引入 |
| Redis 连接失败 | Redis 未启动或密码不对 | 确保 Redis 在 6379 端口运行 |
| AI 对话无响应 | AI API Key 未配置 | 在管理端「AI 密钥配置」添加 API Key |
| 小程序登录失败 | 微信 appid/secret 未配置 | 开发阶段自动使用 mock openid |
| PostgreSQL 连接失败 | 未安装 PostgreSQL | 不影响核心功能，AI RAG 向量存储自动降级 |

---

## 七、开发工作流建议

```
1. 修改代码 → 2. 后端 mvn compile（热部署） → 3. 前端 pnpm dev（HMR）
                                        ↓
4. 管理端验证 → 5. Swagger 测试 API → 6. 小程序联调
```

---

**文档版本**：v1.0 | **最后更新**：2026-07-29 | **作者**：HomeAI Team
