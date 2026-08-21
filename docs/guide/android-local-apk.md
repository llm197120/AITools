# HomeAI 打签名 APK

**唯一发版出包命令：** `pnpm pack:apk:local`。HBuilderX 云打包与 `pnpm pack:apk`（DCloud 离线 SDK）已弃用。

发版登记、热更新 zip、启动页探测、**脚本位置与用途**见 [`app-release.md`](./app-release.md)。本文只写出包。

## 本机出包

uni-app 出 **H5**，**Capacitor 6** 官方 Android + Gradle。不依赖 DCloud SDK。详情见 [`docs/plan/android-capacitor-local-pack.md`](../plan/android-capacitor-local-pack.md)。

包名 `com.homeai.app`，证书 `C:\Users\57089\secret\homeai-release.keystore`（与历史上云打包同一套，可覆盖安装）。keystore 与 `android-pack.local.json` 不要进 git。

发 APK 前先改 `JeecgUniapp/manifest.config.ts` 的 `versionName` / `versionCode`（整数递增）。脚本会写进 `android/app/build.gradle`。

```powershell
cd JeecgUniapp
pnpm pack:apk:local
```

产物（均在 `JeecgUniapp/dist/apk/`）：

| 文件 | 说明 |
|------|------|
| `homeai-{版本}-{yyyyMMdd-HHmmss}.apk` | 带时间戳的签名包，例如 `homeai-1.0.0-20260821-135500.apk` |
| `homeai-release.apk` | 上一份的拷贝，方便 `upload-apk.ps1` |
| `homeai-h5-{版本}.zip` | 热更新包，根目录含 `index.html` |

Android 应用信息里的版本名为 `1.0.0+打包时间`。**包名不要改**，否则无法覆盖安装。

启动图标、启动图、通知小图标在打包时从 `JeecgUniapp/src/static/app/icons/1024x1024.png` 生成。覆盖安装后若桌面仍是旧图标，把应用从桌面移除再拖回来，或重启桌面（部分机型会缓存启动图标）。

第一次若还没有 `android/`：`pnpm pack:apk:local -- -InitAndroid`。本机 Java 直连 Google Maven / services.gradle.org 常 TLS 失败：Gradle 发行包用 Windows 证书从腾讯镜像拉 zip；Android 插件仓库走阿里云（`init-aliyun.gradle` + 给 Capacitor 子工程补镜像）。
