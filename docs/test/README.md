# 测试文档

本目录存放项目测试相关的文档与报告。

## 文档清单

| 文件名 | 内容 | 日期 | 状态 |
|--------|------|------|------|
| [test-report-homeai-20260812.xlsx](./test-report-homeai-20260812.xlsx) ～ [r12](./test-report-homeai-20260812-r12.xlsx) | 第 1～12 轮 | 2026-08-12 | 已归档 |
| [test-report-homeai-20260812-r13.xlsx](./test-report-homeai-20260812-r13.xlsx) | 第十三轮（三端运行态冒烟） | 2026-08-12 | 已归档 |
| [test-report-homeai-20260812-r14.xlsx](./test-report-homeai-20260812-r14.xlsx) | 第十四轮（消除 3 条警告） | 2026-08-12 | 已归档 |
| [test-report-homeai-20260813-r15.xlsx](./test-report-homeai-20260813-r15.xlsx) | 第十五轮（R31 后回归） | 2026-08-13 | 已归档 |
| [test-report-homeai-20260902-r16.xlsx](./test-report-homeai-20260902-r16.xlsx) | 第十六轮初稿（含过期静态断言） | 2026-09-02 | 已归档 |
| [test-report-homeai-20260903-r16.xlsx](./test-report-homeai-20260903-r16.xlsx) | 第十六轮（断言对齐后综合回归） | 2026-09-03 | 已归档 |
| [test-report-homeai-20260904-r16.xlsx](./test-report-homeai-20260904-r16.xlsx) | 第十六轮（2026-09-04 全系统回归） | 2026-09-04 | 当前 |
| [generate_test_report.py](./generate_test_report.py) | 报告生成脚本 | 2026-08-13 | 可用 |

## 如何重新生成

```bat
cd /d "C:\Users\57089\Desktop\AI project\AITools"
set JAVA_HOME=C:\Users\57089\.jdks\ms-17.0.19
set PATH=%JAVA_HOME%\bin;%PATH%
py -3 docs\test\generate_test_report.py
```

## 当前结论（第十六轮 · 2026-09-04）

| 结果 | 数量 | 说明 |
|------|------|------|
| 通过 | 96 | Excel 脚本项（Maven 在脚本中标记跳过，已在会话内单独跑通） |
| 警告 | 1 | 微信 `urlCheck=false`（暂不上架） |
| 跳过 | 2 | 报告脚本 `HOMEAI_SKIP_MVN=1`（避免重复编译） |
| 失败 | 0 | — |

### 本轮已执行

- 管理端 Jest：9/9 通过
- UniApp Vitest：46/46 通过（8 个文件）
- 后端 homeai 单测：166/166 通过（模块 pom 已 `skipTests=false`，未改父 pom）
- 管理端 Vite `http://localhost:3100/` 已启动；经代理 `POST /jeecgboot/sys/mLogin` 成功
- UniApp H5 `http://localhost:9000/` 已启动；登录/首页/协议路由 HTTP 200
- 管理端菜单 `getUserPermissionByToken` 返回约 28 条 homeai 页面（含 `docConfig`、`syncConfig`）
- 管理端页面只读 API：用户/家庭/看板/AI/存储/账单/计划/菜谱/学习/配置等 **全部 success**
- 移动端页面只读 API：家庭/计划/菜谱/学习/账单/存储/AI/文档/版本 **全部 success**
- 写操作冒烟（临时手机号用户，测完回收站彻底删除）：注册/密码登录/H5 代理登录、学习目标、家庭创建改名解散、计划 CRUD、账单 CRUD、菜谱创建收藏删除、文件夹创建删除、AI 会话创建删除、改密与退出；**39/39 通过**
- 上传与安全：拒绝 `.exe`、拒绝伪造 PNG 魔数、学习类型/扩展名不一致失败；PNG/TXT/PDF 存储上传、详情/预览/收藏/搜索、学习预上传、对话附件上传均通过
- Office 转换：规则库当前仅 `csv → xlsx`；提交后轮询 **COMPLETED**（约 5s）
- AI：配额预检 `allowed=true`，最小 SSE 发送收到 `MESSAGE` 事件后停止；测试用户已彻底删除
- 同账号再次登录会作废 Redis 中上一把 token（单会话），属现有行为

### 未覆盖

- 当前环境无浏览器自动化，未能代替人手在 Vite/H5 里点选（服务仍开着：管理端 3100、H5 9000）
- 无 Android 真机 / 微信开发者工具 E2E
- Office 仅覆盖已启用的 `csv→xlsx` 规则（无 txt/docx 目标规则）

### 运维提示

- 本地连 `127.0.0.1` 时，微信开发者工具需勾选「不校验合法域名」  
- 后端若改过拦截器/菜单 SQL，需重启并确认最新 homeai 模块已加载  
