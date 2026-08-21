# 家庭AI小工具 - 项目文档总览

> 项目名称：Home AI Tools | 家庭用AI小工具
> 技术栈：JeecgBoot (Spring Boot) + JeecgUniapp (uni-app) + LangChain4j

## 目录结构

```
docs/                          # 文档根目录
├── README.md                  # 本文档 - 项目文档索引
│
├── design/                    # 设计文档
│   ├── README.md              # 设计文档索引
│   ├── architecture-overview.md # 架构总览与路线图
│   ├── module-details.md        # 模块功能详细设计
│   ├── ai-security.md           # AI能力与安全设计
│   ├── database-flows.md        # 流程图与数据库设计
│   └── ui-miniapp-v1.md         # 小程序线框图
│
├── requirements/              # 需求文档
│   └── README.md              # 需求文档索引
│
├── api/                       # 接口文档
│   ├── README.md              # 接口文档索引
│   └── homeai-api-v1.md       # 家庭AI小工具完整API接口定义
│
├── plan/                       # 实现计划与进度
│   ├── implementation-plan.md   # 8阶段实现计划
│   ├── phase-progress.md        # 各阶段完成情况记录
│   ├── homeai-optimization-roadmap.md  # 迭代优化路线图（第 1～40 轮）
│   ├── comfyui-roadmap.md             # 跳转 stub → ComfyUI/docs/
│   └── android-migration-design.md     # 小程序→Android 迁移方案（暂不执行）
│
├── review/                    # 审查记录
│   └── README.md              # 审查记录索引
│
├── test/                      # 测试文档
│   └── README.md              # 测试文档索引
│
├── meeting/                   # 会议纪要
│   └── README.md              # 会议纪要索引
│
├── deploy/                    # 部署文档 + 一键发布/启停
│   ├── README.md              # 部署文档索引（含 publish-all / start-all / stop-all）
│   ├── publish-all.ps1        # 一键发布：后端 + 管理端 + APP
│   ├── start-all.ps1 / stop-all.ps1
│   └── github-actions-acr-cicd-design.md  # GitHub Actions + ACR CI/CD（待实施）
│
└── guide/                     # 使用指南
    ├── README.md              # 使用指南索引
    ├── app-release.md         # APP 发布（APK 覆盖 / 热更新）
    ├── android-local-apk.md   # 本机打签名 APK
    └── comfyui-retouch-manual.md  # 跳转 stub → ComfyUI/docs/

ComfyUI/                       # 与 docs/ 同级；本机 ComfyUI，后续可单独成仓
├── README.md
├── docs/                      # 安装 / 手册 / 路线图
├── scripts/                   # 启停与验证脚本备份
└── workflows/                 # 工作流 JSON
```

## 根目录文档（AI 能力调研，2026-08）

| 文档 | 路径 | 说明 |
|------|------|------|
| LLM API 价格对比 | `docs/llm-pricing-comparison.md` | 国际/国内主流模型能力×价格对比（含接入注意事项） |
| AI 图片精修开源工具 | `docs/ai-image-editing-tools.md` | GitHub 精修工具调研（GFPGAN/CodeFormer/Upscayl 等） |
| 本机本地 AI 方案 | `docs/local-photo-video-ai.md` | RTX 4060 8G + 16G 照片精修/视频剪辑/AI 视频生成方案 |
| 百炼图片编辑 API | `docs/qwen-image-edit-api.md` | Qwen-Image-Edit 接入指南（含 JeecgBoot/Java 示例） |
| **ComfyUI（独立目录）** | [`ComfyUI/README.md`](../ComfyUI/README.md) | 本机修图/生图：安装、手册、路线、工作流 JSON |

## 核心设计文档

| 文档 | 路径 | 说明 |
|------|------|------|
| architecture-overview.md | `docs/design/architecture-overview.md` | 架构总览、家庭模型、模块结构、开发路线图 |
| module-details.md | `docs/design/module-details.md` | 模块功能详细说明、接口概览、权限体系 |
| ai-security.md | `docs/design/ai-security.md` | AI能力集成、安全设计、微信消息提醒 |
| database-flows.md | `docs/design/database-flows.md` | 系统流程图、数据库DDL与ER图 |
| ui-miniapp-v1.md | `docs/design/ui-miniapp-v1.md` | 小程序全部模块线框图（含目录） |
| homeai-api-v1.md | `docs/api/homeai-api-v1.md` | 完整API接口定义（从架构文档分离） |

## 文档维护规范

1. **命名规则**：`{类型}-{模块}-{版本/日期}.md`，使用小写字母与连字符
2. **版本管理**：文档与代码同步版本，重要变更需更新版本号
3. **引用方式**：文档间引用使用相对路径，如 `../design/architecture-design.md`
4. **更新记录**：每份文档顶部应包含更新记录表格

## 快速导航

- [一键发布 / 启停](./deploy/README.md) - APP + 管理端 + 后端
- [APP 发布流程](./guide/app-release.md) - versionCode、覆盖安装与热更新
- [架构总览与路线图](./design/architecture-overview.md) - 阅读架构总览
- [模块功能详细设计](./design/module-details.md) - 查看模块功能说明
- [AI能力与安全设计](./design/ai-security.md) - 查看AI与安全设计
- [流程图与数据库设计](./design/database-flows.md) - 查看流程图与数据库设计
- [API 接口文档](./api/homeai-api-v1.md) - 查看所有接口定义
