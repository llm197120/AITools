---
name: Home AI Tools API 接口文档
version: v1
description: 家庭AI小工具系统完整API接口定义，基于 JeecgBoot RESTful 规范
---

# 家庭AI小工具 - API 接口文档 v1

> 本文档从 [`architecture-design.md`](architecture-design.md) 中分离，保持内容同步。
>
> 接口基础路径：`/homeai`（建议加入版本前缀 `/v1/homeai`）
>
> 更新日期：2026-07-29

---

## 五、接口设计 (API)

### 5.1 总体接口规范

遵循JEECG标准响应格式（建议所有API加入版本前缀 `/v1`，如 `/v1/homeai/user/login`，以便后续版本迭代兼容）：

> **版本前缀说明**：建议初期即加入 `/v1` 前缀，后续API变更时增加 `/v2` 新端点，旧版本保持兼容至宣布废弃后6个月。
> **RESTful 设计原则**：遵循 HTTP 方法语义（GET=查询 POST=创建 PUT=修改 PATCH=部分更新 DELETE=删除）。
> 部分操作端点（如 `/download`、`/rename`）为框架常用惯例，在 Controller 层通过自定义方法实现。
> 正式编码时建议对以下路径做 RESTful 优化：
> - `/entries/import` → 按资源建模，用 `POST /entries` 带 `source=import` 参数
> - `/files/export/bill` → `GET /bills/export`
> - `/files/{id}/rename` → `PATCH /files/{id}` 带 `name` 字段


```json
{
    "success": true,
    "message": "操作成功!",
    "code": 200,
    "result": {},
    "timestamp": 1234567890
}
```

分页请求参数：`pageNo`, `pageSize`
分页响应格式：`{ records: [], total, pages, current }`

### 5.2 核心API列表

#### 微信用户


| 方法   | 路径                          | 说明             | 小程序 | 管理端 |
| ---- | --------------------------- | -------------- | --- | --- |
| POST | `/homeai/user/login`        | 微信登录(code换JWT) | Y   |     |
| POST | `/homeai/user/refresh-token` | 刷新Token        | Y   |     |


#### 家庭管理


| 方法     | 路径                           | 说明       | 小程序 | 管理端 |
| ------ | ---------------------------- | -------- | --- | --- |
| GET    | `/homeai/family/info`        | 获取当前家庭信息 | Y   |     |
| POST   | `/homeai/family`      | 创建家庭     | Y   |     |
| POST   | `/homeai/family/invite-code`      | 生成6位邀请码  | Y   |     |
| POST   | `/homeai/family/members`        | 通过邀请码加入  | Y   |     |
| GET    | `/homeai/family/members`     | 家庭成员列表   | Y   | Y   |
| DELETE | `/homeai/family/member/{id}` | 移除成员     | Y   |     |
| DELETE | `/homeai/family/leave`       | 主动退出家庭   | Y   |     |
| DELETE | `/homeai/family/disband`     | 解散家庭     | Y   |     |
| PUT    | `/homeai/family/member/{id}/role` | 修改成员角色  | Y   |     |
| POST   | `/homeai/family/transfer`         | 转让管理员身份   | Y   |     |
| GET    | `/homeai/user/list`          | 用户列表     |     | Y   |
| GET    | `/homeai/user/{id}`          | 用户详情     |     | Y   |
| PUT    | `/homeai/user/{id}`          | 编辑用户信息   |     | Y   |
| DELETE | `/homeai/user/{id}`          | 注销用户账号   |     | Y   |
| PUT    | `/homeai/user/{id}/status`   | 启用/禁用用户  |     | Y   |


#### AI对话模块


| 方法     | 路径                                       | 说明            | 小程序 | 管理端 |
| ------ | ---------------------------------------- | ------------- | --- | --- |
| GET    | `/homeai/ai/conversations`               | 对话列表（分页）      | Y   | Y   |
| POST   | `/homeai/ai/conversations`               | 新建对话          | Y   |     |
| POST   | `/homeai/ai/conversations/{id}/messages` | 发送消息(SSE流式响应) | Y   |     |
| GET    | `/homeai/ai/conversations/{id}/messages` | 获取消息历史        | Y   | Y   |
| POST   | `/homeai/ai/conversations/{id}/stop`     | 停止生成          | Y   |     |
| PUT    | `/homeai/ai/conversations/{id}`          | 重命名对话         | Y   |     |
| DELETE | `/homeai/ai/conversations/{id}`          | 删除对话          | Y   | Y   |
| GET    | `/homeai/ai/conversations/search`        | 搜索对话（按标题）   | Y   |     |
| POST   | `/homeai/ai/conversations/{id}/upload`   | 上传文件到对话       | Y   |     |




#### 资料存储模块


| 方法     | 路径                                    | 说明         | 小程序 | 管理端 |
| ------ | ------------------------------------- | ---------- | --- | --- |
| GET    | `/homeai/storage/folders/tree`        | 文件夹树       | Y   | Y   |
| POST   | `/homeai/storage/folders`             | 新建文件夹      | Y   | Y   |
| PUT    | `/homeai/storage/folders/{id}`        | 编辑文件夹      | Y   | Y   |
| DELETE | `/homeai/storage/folders/{id}`        | 删除文件夹      | Y   | Y   |
| POST   | `/homeai/storage/files`               | 上传文件       | Y   | Y   |
| GET    | `/homeai/storage/files/{id}/download`  | 下载文件       | Y   | Y   |
| PATCH  | `/homeai/storage/files/{id}`    | 重命名/移动/收藏 | Y   | Y   |
| GET    | `/homeai/storage/files`               | 文件列表(按文件夹) | Y   | Y   |
| DELETE | `/homeai/storage/files/{id}`          | 删除文件       | Y   | Y   |
| GET    | `/homeai/storage/files/search`        | 搜索         | Y   | Y   |
| PATCH  | `/homeai/storage/folders/{id}/visibility` | 修改文件夹可见性(仅自己/家庭) | Y   |     |


| POST   | `/homeai/storage/files/{id}/convert` | 发起格式转换（Office） | Y   | Y   |
| GET    | `/homeai/storage/convert/tasks/{taskId}`  | 查询转换状态       | Y   | Y   |
| POST   | `/homeai/storage/files/{id}/generate`| AI生成文件（Office） | Y   |     |
| GET    | `/homeai/storage/office-templates` | Office模板列表      |     | Y   |
| POST   | `/homeai/storage/office-templates` | 上传Office模板      |     | Y   |
| PUT    | `/homeai/storage/office-templates/{id}` | 更新Office模板   |     | Y   |
| DELETE | `/homeai/storage/office-templates/{id}` | 删除Office模板   |     | Y   |
| GET    | `/homeai/storage/convert-rules`    | 格式转换规则       |     | Y   |
| PUT    | `/homeai/storage/convert-rules`    | 更新转换规则配置     |     | Y   |
| GET    | `/homeai/storage/office-history`   | Office处理记录     | Y   | Y   |


#### 账单模块


| 方法     | 路径                             | 说明        | 小程序 | 管理端 |
| ------ | ------------------------------ | --------- | --- | --- |
| GET    | `/homeai/bill/categories`      | 消费分类列表    | Y   | Y   |
| POST   | `/homeai/bill/categories`      | 新增分类      |     | Y   |
| PUT    | `/homeai/bill/categories/{id}` | 编辑分类      |     | Y   |
| DELETE | `/homeai/bill/categories/{id}` | 删除分类      |     | Y   |
| POST   | `/homeai/bill/entries`         | 新增/导入账单(CSV/Excel)  | Y   | Y   |
| POST   | `/homeai/bill/entries/batch`   | 批量添加      |     | Y   |
| POST   | `/homeai/bill/entries/import/ai-parse` | AI识别解析文件  | Y   | Y   |
| POST   | `/homeai/bill/entries/import/confirm` | AI识别结果确认入库 | Y   | Y   |
| PUT    | `/homeai/bill/entries/import/item/{id}` | 编辑未确认导入的单条记录 | Y   | Y   |
| GET    | `/homeai/bill/entries`         | 账单列表      | Y   | Y   |
| PUT    | `/homeai/bill/entries/{id}`    | 编辑账单      | Y   | Y   |
| DELETE | `/homeai/bill/entries/{id}`    | 删除账单      | Y   | Y   |
| GET    | `/homeai/bill/import/template` | 下载Excel导入模板     |     | Y   |
| GET    | `/homeai/bill/statistics`      | 统计报表      | Y   | Y   |


#### 日常计划模块


| 方法     | 路径                          | 说明        | 小程序 | 管理端 |
| ------ | --------------------------- | --------- | --- | --- |
| POST   | `/homeai/plan/masters`             | 新增计划      | Y   | Y   |
| PUT    | `/homeai/plan/masters/{id}`        | 编辑计划      | Y   | Y   |
| DELETE | `/homeai/plan/masters/{id}`        | 删除计划      | Y   | Y   |
| GET    | `/homeai/plan/masters`             | 计划列表(按日期) | Y   | Y   |
| GET    | `/homeai/plan/instances/calendar`    | 日历视图数据    | Y   | Y   |
| PATCH  | `/homeai/plan/instances/{id}/status` | 更新状态      | Y   | Y   |
| GET    | `/homeai/plan/instances/today`       | 今日计划(首页用) | Y   |     |


#### 烹饪指南模块


| 方法     | 路径                               | 说明        | 小程序 | 管理端 |
| ------ | -------------------------------- | --------- | --- | --- |
| GET    | `/homeai/recipe/categories`      | 分类列表      | Y   | Y   |
| POST   | `/homeai/recipe/categories`      | 新增分类      |     | Y   |
| PUT    | `/homeai/recipe/categories/{id}` | 编辑分类      |     | Y   |
| DELETE | `/homeai/recipe/categories/{id}` | 删除分类      |     | Y   |
| GET    | `/homeai/recipe`                | 菜谱列表      | Y   | Y   |
| POST   | `/homeai/recipe`                | 新增菜谱      | Y   | Y   |
| PUT    | `/homeai/recipe/{id}`           | 编辑菜谱      | Y   | Y   |
| DELETE | `/homeai/recipe/{id}`           | 删除菜谱      | Y   | Y   |
| GET    | `/homeai/recipe/{id}`           | 菜谱详情      | Y   | Y   |
| POST   | `/homeai/recipe/{id}/favorite`  | 收藏/取消收藏菜谱 | Y   |     |
| POST   | `/homeai/recipe/{id}/video`    | 上传做菜视频   | Y   |     |
| DELETE | `/homeai/recipe/{id}/video`    | 删除做菜视频   | Y   |     |


#### 学习模块


| 方法     | 路径                                      | 说明        | 小程序 | 管理端 |
| ------ | --------------------------------------- | --------- | --- | --- |
| GET    | `/homeai/learn/categories`              | 分类列表      | Y   | Y   |
| POST   | `/homeai/learn/categories`              | 新增分类      |     | Y   |
| PUT    | `/homeai/learn/categories/{id}`         | 编辑分类      |     | Y   |
| DELETE | `/homeai/learn/categories/{id}`         | 删除分类      |     | Y   |
| GET    | `/homeai/learn/materials`               | 资料列表      | Y   | Y   |
| POST   | `/homeai/learn/materials`               | 新增资料      | Y   | Y   |
| PUT    | `/homeai/learn/materials/{id}`          | 编辑资料      | Y   | Y   |
| DELETE | `/homeai/learn/materials/{id}`          | 删除资料      | Y   | Y   |
| POST   | `/homeai/learn/materials/{id}/favorite` | 收藏/取消收藏资料 | Y   |     |
| POST   | `/homeai/learn/materials/{id}/upload`   | 上传资料文件(PDF/Word/Excel/PPT/图片/视频) | Y   | Y   |
| POST   | `/homeai/learn/records`                 | 保存学习记录    | Y   |     |
| GET    | `/homeai/learn/records`                 | 学习记录列表    | Y   |     |
| GET    | `/homeai/learn/records/calendar`        | 学习日历视图    | Y   |     |


#### AI配置模块（仅管理端）


| 方法     | 路径                            | 说明   |
| ------ | ----------------------------- | ---- |
| GET    | `/homeai/config/keys`         | 密钥列表 |
| POST   | `/homeai/config/keys`         | 新增密钥 |
| PUT    | `/homeai/config/keys/{id}`    | 编辑密钥 |
| DELETE | `/homeai/config/keys/{id}`    | 删除密钥 |
| GET    | `/homeai/config/quotas`       | 额度列表 |
| PUT    | `/homeai/config/quotas/{id}`  | 修改额度 |
| GET    | `/homeai/config/quotas/stats` | 使用统计 |


#### 文件白名单配置（仅管理端）


| 方法  | 路径                              | 说明      |
| --- | ------------------------------- | ------- |
| GET | `/homeai/config/file-whitelist` | 白名单列表   |
| PUT | `/homeai/config/file-whitelist` | 更新白名单配置 |

---

## 附录：权限编码对照

| 功能模块     | 权限编码                                                     | 说明        |
| -------- | -------------------------------------------------------- | --------- |
| AI对话管理     | `homeai:ai:conversation:list`, `homeai:ai:conversation:add`, `homeai:ai:conversation:edit`, `homeai:ai:conversation:delete`, `homeai:ai:conversation:search`               | 对话CRUD+搜索    |
| AI密钥配置   | `homeai:config:key:list`, `homeai:config:key:add`, `homeai:config:key:edit`, `homeai:config:key:delete`               | 完整CRUD    |
| AI额度管理   | `homeai:config:quota:list`, `homeai:config:quota:edit`                        | 查看与修改     |
| 文件白名单    | `homeai:config:whitelist:list`, `homeai:config:whitelist:edit`                    | 安全配置      |
| 家庭管理     | `homeai:family:list`, `homeai:family:view`, `homeai:family:transfer`                              | 查看所有家庭    |
| 资料存储（含Office） | `homeai:storage:list`, `homeai:storage:view`, `homeai:storage:upload`, `homeai:storage:delete`, `homeai:storage:convert`, `homeai:storage:generate`, `homeai:storage:template` | 文件夹+文件管理+Office处理 |
| 账单分类     | `homeai:bill:category:list`, `homeai:bill:category:add`, `homeai:bill:category:edit`, `homeai:bill:category:delete`            | 分类CRUD    |
| 账单记录     | `homeai:bill:entry:list`, `homeai:bill:entry:add`, `homeai:bill:entry:edit`, `homeai:bill:entry:delete`, `homeai:bill:entry:import`        | 账单CRUD+导入 |
| 日常计划     | `homeai:plan:master:list`, `homeai:plan:master:add`, `homeai:plan:master:edit`, `homeai:plan:master:delete`                     | 计划CRUD    |
| 烹饪分类     | `homeai:recipe:category:list`, `homeai:recipe:category:add`, `homeai:recipe:category:edit`, `homeai:recipe:category:delete`          | 分类CRUD    |
| 菜谱管理     | `homeai:recipe:list`, `homeai:recipe:add`, `homeai:recipe:edit`, `homeai:recipe:delete`                   | 菜谱CRUD    |
| 学习分类     | `homeai:learn:category:list`, `homeai:learn:category:add`, `homeai:learn:category:edit`, `homeai:learn:category:delete`           | 分类CRUD    |
| 学习资料     | `homeai:learn:material:list`, `homeai:learn:material:add`, `homeai:learn:material:edit`, `homeai:learn:material:delete`                    | 资料CRUD    |
| 用户管理     | `homeai:user:list`, `homeai:user:view`, `homeai:user:edit`                           | 微信用户管理    |

