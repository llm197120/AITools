# HomeAI Android 本机打包（H5 + Capacitor）

## 现状

**发版只走** `pnpm pack:apk:local`。包名 `com.homeai.app`，证书 `C:\Users\57089\secret\homeai-release.keystore`。

已弃用：

- HBuilderX 云打包（第 56 轮曾因缺 Push 红屏；现不再发版）
- DCloud 离线 SDK `pnpm pack:apk`（第 54 轮脚本仅归档）

发布登记与热更新见 [`docs/guide/app-release.md`](../guide/app-release.md)。

## 管道

```
pnpm build:h5          → dist/build/h5（uni-app 的 H5，业务页继续用 uni.*）
npx cap sync android   → Capacitor 官方 Android 工程
gradlew assembleRelease → 本机签名 APK（同一套 keystore，可覆盖历史上的云打包安装）
```

不依赖 `plus.*` 运行时，不依赖 DCloud aar / AppKey。微信小程序通道仍用 `pnpm build:mp-weixin`，互不影响。

```powershell
cd JeecgUniapp
pnpm pack:apk:local
```

产物：`dist/apk/homeai-1.0.0-yyyyMMdd-HHmmss.apk`，以及热更新用的 `dist/apk/homeai-h5-{版本}.zip`（根目录含 `index.html`）。系统应用信息里的 versionName 为 `1.0.0+yyyyMMdd-HHmmss`。Android 包名始终是 `com.homeai.app`。

## 为什么选 Capacitor 而不是 uni-app x / 纯原生

| 方案 | 与现状 | 本机出包 | 结论 |
|------|--------|----------|------|
| uni-app H5 + Capacitor | 页面/Pinia/wot 几乎原样；只改 `platform/*` | Gradle 官方流程 | **采用** |
| uni-app x | uvue / 新 API，等于换客户端 | 可以 | 不做 |
| Kotlin 重写 | 复用率低 | 可以 | 不做 |

`platform/*` 本来就是为「业务不写 `#ifdef`」准备的。Capacitor 壳走 **H5 条件编译** + `isCapacitorNative()`。代码里仍保留 `plus.*` / APP-PLUS 分支，只为已装的旧壳，不要再出那种包。

## 已完成能力

### 管道

- `capacitor.config.ts`（`appId=com.homeai.app`，`webDir=dist/build/h5`，允许明文 HTTP）
- Capacitor **6.x**（对齐本机 JDK 17 + SDK 34；不要升 8，除非先装 JDK 21）
- `pnpm pack:apk:local`：品牌资源 → build:h5 → cap sync → assembleRelease
- `JeecgUniapp/android/` 已 `cap add`，本机已打出签名 APK
- 打包时从 `src/static/app/icons/1024x1024.png` 生成自适应图标 / 启动图 / 通知小标（不要沿用 Capacitor 默认 Logo）
- `platform/runtime.ts`：独立 App / 退出 / 打开外链
- 计划本地通知走 `@capacitor/local-notifications`

### 原生能力

| 能力 | 实现 |
|------|------|
| 保存图片/视频到相册 | `@capacitor-community/media` |
| 打开/另存文档 | Filesystem 写入缓存 + `@capacitor/share` |
| 外链 | `@capacitor/browser` |
| 返回键 / 状态栏 | `@capacitor/app`：有页面栈则返回，栈底（Tab/登录/启动页）直接退出；StatusBar 不遮挡 WebView |
| 应用内更新 | `HomeaiUpdate` 插件：下载 / SHA-256 / 解压（防 zip-slip）/ 安装 APK；启动页探测 |

## 本机一次性

1. 已有：JDK 17、`ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk`、keystore。
2. 在 `JeecgUniapp/` 已装 Capacitor 6：`@capacitor/core` / `cli` / `android` / `app`
3. `npx cap add android`（只做一次，生成 `android/`，可提交）
4. `pnpm pack:apk:local`

`android/app/keystore.properties`、`android/local.properties` 已 gitignore。
