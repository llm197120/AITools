# 测试文档

本目录存放项目测试相关的文档与报告。

## 文档清单

| 文件名 | 内容 | 日期 | 状态 |
|--------|------|------|------|
| [test-report-homeai-20260812.xlsx](./test-report-homeai-20260812.xlsx) ～ [r12](./test-report-homeai-20260812-r12.xlsx) | 第 1～12 轮 | 2026-08-12 | 已归档 |
| [test-report-homeai-20260812-r13.xlsx](./test-report-homeai-20260812-r13.xlsx) | 第十三轮（三端运行态冒烟） | 2026-08-12 | 已归档 |
| [test-report-homeai-20260812-r14.xlsx](./test-report-homeai-20260812-r14.xlsx) | 第十四轮（消除 3 条警告） | 2026-08-12 | 已归档 |
| [test-report-homeai-20260813-r15.xlsx](./test-report-homeai-20260813-r15.xlsx) | 第十五轮（R31 后回归） | 2026-08-13 | 当前 |
| [generate_test_report.py](./generate_test_report.py) | 报告生成脚本 | 2026-08-13 | 可用 |

## 如何重新生成

```bat
cd /d "C:\Users\57089\Desktop\AI project\AITools"
set JAVA_HOME=C:\Users\57089\.jdks\ms-17.0.19
set PATH=%JAVA_HOME%\bin;%PATH%
py -3 docs\test\generate_test_report.py
```

## 当前结论（第十五轮 · 2026-08-13）

| 结果 | 数量 | 说明 |
|------|------|------|
| 通过 | 93 | 三端探活 + 联调冒烟 + 静态/接口回归 |
| 警告 | 0 | — |
| 失败 | 0 | — |

### 本轮探活

- 后端 `8080`、管理端 Vite `3100`、HBuilderX 均在线  
- 快速冒烟：`wechat-public` 匿名、dashboard、learn-stats / stats-export、小程序 goal / new / recommend 均 PASS  

### 运维提示

- 本地连 `127.0.0.1` 时，微信开发者工具需勾选「不校验合法域名」  
- 后端若改过拦截器/菜单 SQL，需重启并确认最新 homeai 模块已加载  
