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
│   └── architecture-design.md # 架构设计方案（核心设计文档）
│
├── requirements/              # 需求文档
│   └── README.md              # 需求文档索引
│
├── api/                       # 接口文档
│   └── README.md              # 接口文档索引
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
├── deploy/                    # 部署文档
│   └── README.md              # 部署文档索引
│
└── guide/                     # 使用指南
    └── README.md              # 使用指南索引
```

## 核心设计文档

| 文档 | 路径 | 说明 |
|------|------|------|
| architecture-design.md | `docs/design/architecture-design.md` | 系统架构、功能细化、API 定义、开发路线图 |
| ui-miniapp-v1.md | `docs/design/ui-miniapp-v1.md` | 小程序全部模块线框图（含目录） |

## 文档维护规范

1. **命名规则**：`{类型}-{模块}-{版本/日期}.md`，使用小写字母与连字符
2. **版本管理**：文档与代码同步版本，重要变更需更新版本号
3. **引用方式**：文档间引用使用相对路径，如 `../design/architecture-design.md`
4. **更新记录**：每份文档顶部应包含更新记录表格

## 快速导航

- [架构设计方案](./design/architecture-design.md) - 阅读完整的系统设计
