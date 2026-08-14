# AGENTS.md

家庭AI小工具（Home AI Tools）多端仓库。工作语言：**中文**（注释/文档/对话默认中文，见 `.cursor/rules/chinese-default.mdc`）。Git 根 = `AITools/`，分支 `dev`。

## 仓库结构

| 目录 | 内容 | 技术栈 |
|---|---|---|
| `JeecgBoot/jeecg-boot/` | 后端单体 | Java 17 + Spring Boot 4 + MyBatis-Plus + Shiro + MySQL/Redis |
| `JeecgBoot/jeecgboot-vue3/` | 管理端 | Vue3 + Vite + Ant Design Vue4 + TS + pinia |
| `JeecgUniapp/` | 小程序 | UniApp + Vue3 + wot-design-uni + TS |
| `docs/plan/homeai-optimization-roadmap.md` | 迭代路线图（已归档至第 38 轮） | 每完成一轮在此追加摘要 |
| `tmp/`、`dish-images/` | 素材/图片 | 已 gitignore，勿提交 |

`jeecg-boot/CLAUDE.md` 与 `jeecgboot-vue3/CLAUDE.md` 为本仓库子项目指令，与本文档互补；业务模块核心为 `jeecg-boot-module/jeecg-boot-module-homeai`（`org.jeecg.modules.homeai`）与管理端 `src/views/homeai/`。

## 命令

### 后端（JeecgBoot/jeecg-boot）
- 编译：`mvn -o compile -pl jeecg-boot-module/jeecg-boot-module-homeai -am -DskipTests`
- **测试默认被跳过且 CLI 无法覆盖**：父 pom 硬编码 `<skipTests>true</skipTests>`，Maven 4 下 `-DskipTests=false` 无效；需临时改 pom 为 false（跑完必须还原），且 `surefire-junit-platform` 需联网下载（离线 `-o` 会失败）。现存唯一后端测试：`RecipeVisibilityTest`（纯逻辑单测，无 Spring 上下文）。
- 改 Java 代码必须用 `//update-begin---author:xxx---date:YYYY-MM-DD---for:【需求】说明---` / `//update-end` 注释包裹（见 jeecg-boot/CLAUDE.md）。

### 管理端（JeecgBoot/jeecgboot-vue3）
- 开发 `pnpm dev`；生产构建用 `pnpm run build:docker:prod`（与 CI 一致）
- 测试：`npx jest tests/homeai`（冒烟 3 用例）
- lint：`npx eslint <file>`；**仓库未 prettier 全量合规，勿对整文件 `prettier --write`**（会产生巨大 diff），只格式化自己改动的行。

### 小程序（JeecgUniapp）
- 开发：`pnpm dev:mp-weixin`（env 在 `env/` 目录，不在根目录）
- 无测试基建（jest/vitest）
- `vite.config.ts` 生产守卫：`mode=production` 时 URL 仍是 localhost 或微信发布环境非 https 会**中止构建**。

## 关键约定与坑（硬经验）

- **homeai 双 token 鉴权**：Shiro 链 `/homeai/**` 全 `anon`，真正门禁是 `HomeaiAuthInterceptor`——管理端路径登记在 `ADMIN_PREFIXES`（仅控制台 JWT 可过），小程序走 `X-Access-Token` 的 homeai JWT。**新增管理端接口必须登记进 `ADMIN_PREFIXES`**，并加 `@RequiresPermissions`。
- **`defHttp.uploadFile` 三参数约定**：必须传第 3 参 `{ isReturnResponse: true }` 才能拿到响应体，读 `res.result`（如 `billImport.vue` 曾因缺此参数整体不可用）；`config.data` 会被 `params.file` 覆盖，勿在 config 里塞 FormData；`callback.success` 仅成功时触发。
- **小程序 eslint resolver 怪癖**：`src/components` 下相对导入 `../../pages-homeai/**` 会误报 `import/extensions`；homeai 共享组件应放 `src/pages-homeai/components/`（如 `HomeMediaUpload.vue`）。
- **时区**：小程序日期/月份必须用 `src/pages-homeai/utils/date.ts` 的 `localDateStr`/`localMonthStr`，禁止 `new Date().toISOString().substring(...)`（UTC 偏移导致日期/「下月」按钮错一天）。
- **PowerShell 写文件会加 BOM**：`Set-Content -Encoding UTF8` 引入 BOM 触发 eslint `unicode-bom`；应改用 `[System.IO.File]::WriteAllText($p, $c, (New-Object System.Text.UTF8Encoding($false)))`。个别文件（如 `storage/index.vue`）提交时自带 BOM，属历史遗留。
- **后端接口风格**：返回 `Result.OK/Result.error`；多表写操作加 `@Transactional(rollbackFor = Exception.class)`；上传入口必须扩展名白名单 + 大小限制 + `HomeaiFileMagicUtil` 魔数校验（参考 `RecipeServiceImpl.validateUploadFile`）。
- **双端一致**：菜谱难度为 1-5 档（1入门/2简单/3中等/4较难/5困难）；family 状态字段 `status`（'disbanded'）与 `delFlag` 语义不同，勿混用。
- **迭代记录**：功能/修复完成后在路线图追加轮次；无新增 SQL 时注明；有 SQL 迁移需登记到第二节迁移清单。
