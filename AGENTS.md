# AGENTS.md

家庭AI小工具（Home AI Tools）多端仓库。工作语言：**中文**（注释/文档/对话默认中文，见 `.cursor/rules/chinese-default.mdc`）。Git 根 = `AITools/`，分支 `dev`。

## 仓库结构

| 目录 | 内容 | 技术栈 |
|---|---|---|
| `JeecgBoot/jeecg-boot/` | 后端单体 | Java 17 + Spring Boot 4 + MyBatis-Plus + Shiro + MySQL/Redis |
| `JeecgBoot/jeecgboot-vue3/` | 管理端 | Vue3 + Vite + Ant Design Vue4 + TS + pinia |
| `JeecgUniapp/` | **Android App**（主客户端） | UniApp H5 + Capacitor 6 + Vue3 + wot-design-uni + TS；微信小程序通道保留但不上架 |
| `ComfyUI/` | **本机 ComfyUI 修图/生图**（后续可单独成仓） | 文档 + 工作流 JSON + 启停脚本；运行时在 `C:\Users\57089\ComfyUI-portable\` |
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

### Android 端（JeecgUniapp，H5 + Capacitor）
- 开发：`pnpm dev:h5`（与发版同一套壳）；`pnpm dev:app-android` 仅作历史对照，**不要**再走 HBuilderX 云打包发版
- **发版**：`pnpm pack:apk:local`（H5 + Capacitor，包名 `com.homeai.app`）。流程见 `docs/guide/app-release.md`，出包见 `docs/guide/android-local-apk.md`
- **已弃用**：HBuilderX 云打包；`pnpm pack:apk`（DCloud 离线 SDK，第 54 轮归档）
- 登录：手机号 + 密码（`/homeai/auth/login/password`）；平台能力走 `src/pages-homeai/platform/`（auth / filePicker / download / env）
- 推送：本地通知兜底（Capacitor `@capacitor/local-notifications`；代码里仍保留 `plus.push` 给已装的旧壳），进程被杀无法送达
- 无测试基建（jest/vitest）
- `vite.config.ts` 生产守卫：**仅 mp-weixin** 在 localhost / 非 https 时中止构建；`app-android` 内测允许局域网/服务器 IP 侧载
- 微信小程序：`pnpm dev:mp-weixin` 通道仍在，产品已决定暂不上架；勿按「主客户端」对待

## 关键约定与坑（硬经验）

- **homeai 双 token 鉴权**：Shiro 链 `/homeai/**` 全 `anon`，真正门禁是 `HomeaiAuthInterceptor`——管理端路径登记在 `ADMIN_PREFIXES`（仅控制台 JWT 可过），Android / 移动端走 `X-Access-Token` 的 homeai JWT（新 token 以 `userId` claim 为准，兼容旧 openid）。**新增管理端接口必须登记进 `ADMIN_PREFIXES`**，并加 `@RequiresPermissions`。
- **`defHttp.uploadFile` 三参数约定**：必须传第 3 参 `{ isReturnResponse: true }` 才能拿到响应体，读 `res.result`（如 `billImport.vue` 曾因缺此参数整体不可用）；`config.data` 会被 `params.file` 覆盖，勿在 config 里塞 FormData；`callback.success` 仅成功时触发。
- **UniApp eslint resolver 怪癖**：`src/components` 下相对导入 `../../pages-homeai/**` 会误报 `import/extensions`；homeai 共享组件应放 `src/pages-homeai/components/`（如 `HomeMediaUpload.vue`）。
- **时区**：移动端日期/月份必须用 `src/pages-homeai/utils/date.ts` 的 `localDateStr`/`localMonthStr`，禁止 `new Date().toISOString().substring(...)`（UTC 偏移导致日期/「下月」按钮错一天）。
- **Android 适配**：业务页只调 `pages-homeai/platform/*`，避免 `#ifdef` 散落；文件选择用 `filePicker`（勿直接 `chooseMessageFile`）；备案完成前侧载 APK 内测，见 `docs/plan/android-migration-design.md`。
- **PowerShell 写文件会加 BOM**：`Set-Content -Encoding UTF8` 引入 BOM 触发 eslint `unicode-bom`；应改用 `[System.IO.File]::WriteAllText($p, $c, (New-Object System.Text.UTF8Encoding($false)))`。个别文件（如 `storage/index.vue`）提交时自带 BOM，属历史遗留。
- **后端接口风格**：返回 `Result.OK/Result.error`；多表写操作加 `@Transactional(rollbackFor = Exception.class)`；上传入口必须扩展名白名单 + 大小限制 + `HomeaiFileMagicUtil` 魔数校验（参考 `RecipeServiceImpl.validateUploadFile`）。
- **双端一致**：菜谱难度为 1-5 档（1入门/2简单/3中等/4较难/5困难）；family 状态字段 `status`（'disbanded'）与 `delFlag` 语义不同，勿混用。
- **迭代记录**：功能/修复完成后在路线图追加轮次；无新增 SQL 时注明；有 SQL 迁移需登记到第二节迁移清单。
