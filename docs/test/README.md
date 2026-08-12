# 测试文档

本目录存放项目测试相关的文档与报告。

## 文档清单

| 文件名 | 内容 | 日期 | 状态 |
|--------|------|------|------|
| [test-report-homeai-20260812.xlsx](./test-report-homeai-20260812.xlsx) ～ [r12](./test-report-homeai-20260812-r12.xlsx) | 第 1～12 轮 | 2026-08-12 | 已归档 |
| [test-report-homeai-20260812-r13.xlsx](./test-report-homeai-20260812-r13.xlsx) | 第十三轮（三端运行态冒烟） | 2026-08-12 | 已归档 |
| [test-report-homeai-20260812-r14.xlsx](./test-report-homeai-20260812-r14.xlsx) | 第十四轮（消除 3 条警告） | 2026-08-12 | 当前 |
| [generate_test_report.py](./generate_test_report.py) | 报告生成脚本 | 2026-08-12 | 可用 |

## 如何重新生成

```bat
cd /d "C:\Users\57089\Desktop\AI project\AITools"
set JAVA_HOME=C:\Users\57089\.jdks\ms-17.0.19
set PATH=%JAVA_HOME%\bin;%PATH%
py -3 docs\test\generate_test_report.py
```

## 当前结论（第十四轮 · 2026-08-12）

| 结果 | 数量 | 说明 |
|------|------|------|
| 通过 | 93 | 含联调冒烟；原 3 条警告已清零 |
| 警告 | 0 | — |
| 失败 | 0 | — |

### 警告处理记录

1. **dashboard 缺权** — 已执行 `alter_homeai_menus_dashboard_grant.sql`（菜单 + admin/vue3 授权）；并补 `homeai_plan_master.recipe_id`（`alter_homeai_plan_recipe_iteration23.sql`）。`/homeai/dashboard/plan-learn` 现返回 success。  
2. **`wechat-public` 匿名 401** — 已加入 `HomeaiAuthInterceptor.PUBLIC_PATHS`；后端已重启加载。匿名访问现 success。  
3. **`urlCheck=false`** — `manifest.config.ts` / `src/manifest.json` 已改为 `true`。本地连 `127.0.0.1` 时请在微信开发者工具勾选「不校验合法域名」，并重新编译小程序。  

### 运维提示

- 当前后端由本轮 `mvn spring-boot:run` 拉起（原 IDEA 进程已停止）。若要用 IDEA 调试，请改回 IDEA 启动并确认加载了最新 homeai 模块。  
- 小程序需重新编译/预览后 `urlCheck` 才会进运行包。
