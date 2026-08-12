# 部署文档

本目录存放环境搭建与部署运维相关文档。实际操作入口以 `JeecgBoot/deploy/` 为准。

## 文档清单

| 文件名 | 内容 | 状态 |
|--------|------|------|
| [github-actions-acr-cicd-design.md](./github-actions-acr-cicd-design.md) | GitHub Actions + 阿里云 ACR CI/CD 方案 | **已实施**（后端镜像推送；管理端 ECS 本机构建） |

## 当前部署要点（与代码一致）

| 端 | 方式 | 说明 |
|----|------|------|
| 后端 | GitHub Actions → ACR → ECS Docker Compose | 见 `JeecgBoot/deploy/README.md` |
| 管理端 | ECS 本机 `pnpm run build:docker:prod` + Nginx | **非** Artifacts 下载；见 `JeecgBoot/deploy/frontend-nginx/` |
| 小程序 | 本地/CI 构建上传微信 | 先填实 `JeecgUniapp/env/.env.production` 的 `YOUR_DOMAIN` |

## 管理端构建对齐

- CI 与部署均使用：`pnpm run build:docker:prod`（读 `.env.docker.prod`，`VITE_GLOB_DOMAIN_URL=/jeecgboot`）
- 部署后核对 `dist/_app.config.js`

## 小程序上架前 Checklist

1. 填写 `env/.env.production` 真实 API（含微信 develop/trial/release 分环境变量）
2. 微信后台配置 request / uploadFile 合法域名
3. `manifest.config.ts`：正式版将 `urlCheck` 改为 `true`，开启 `__usePrivacyCheck__`
4. Android `targetSdkVersion` 建议 ≥ 34（源配置已调至 34）

## 文档分类

- **环境搭建**：开发/测试/生产环境搭建指南
- **部署手册**：应用部署步骤与注意事项
- **运维指南**：日常运维与故障处理
- **配置说明**：各环境配置参数说明

## 命名规范

`{type}-{env}-v{version}.md`

示例：`setup-production-env-v1.0.md`
