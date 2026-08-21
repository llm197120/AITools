# HomeAI APP 发布流程

版本号存在库表 `homeai_app_version`（单行 `id=current`），由管理端 **家庭AI小工具 → APP版本** 控制。APP 启动（隐私同意之后）拉公开接口 `GET /homeai/app/version`：仅当本地整数 **小于** 服务端 `versionCode` 才提示更新。不要用 `!=`，也不要用 Jeecg 自带 `/sys/version`。

种子记录是 `1.0.0` / `100`，**默认关闭**。未打开「对 APP 生效」时，启动页不会弹更新。

## 1. 先选方式


| 方式             | 改什么               | 适用                                                 |
| -------------- | ----------------- | -------------------------------------------------- |
| **覆盖安装 APK**   | 原生壳 + 页面一起换       | 加了原生权限/插件、换了 Capacitor、或壳太旧                        |
| **热更新 H5 zip** | 只换 Web 资源，不重装 APK | 只改业务页面/样式/接口调用；壳 `versionCode` ≥ 「最低壳 versionCode」 |


壳低于「最低壳 versionCode」时，即使选了热更新，APP 也会改走 APK。

发版只走本机 Capacitor：`pnpm pack:apk:local`。HBuilderX 云打包已弃用（旧云打包壳没有热更新，只能覆盖安装 APK）。

## 2. 改版本号

源在 `JeecgUniapp/manifest.config.ts`（会生成 `src/manifest.json`）：

```ts
versionName: '1.0.1',
versionCode: '101',
```

`versionCode` 必须是递增正整数，且 **大于** 用户手机上已有的值。热更新也要加大这个整数（写入库），但 **不必** 重打 APK；原生 `versionCode` 可以仍是旧壳。

包名始终 `com.homeai.app`，证书不要换，否则无法覆盖安装。

## 3. 出包

```powershell
cd JeecgUniapp
pnpm pack:apk:local
```

产物：


| 文件                              | 用途                                       |
| ------------------------------- | ---------------------------------------- |
| `dist/apk/homeai-{版本}-{时间}.apk` | 覆盖安装；同时覆盖一份 `homeai-release.apk` 方便上传    |
| `dist/apk/homeai-h5-{版本}.zip`   | 热更新。zip **根目录必须有** `index.html`（脚本已按此打包） |


不要用 HBuilderX 云打包或 `pnpm pack:apk`。更细的出包说明见 `[android-local-apk.md](./android-local-apk.md)`。脚本位置与用途见下文第 8 节。

## 4. 管理端登记（必做，否则 APP 不知道有新版）

1. 重启过后端，执行过 `alter_homeai_app_version.sql` 与 `alter_homeai_menus_iteration69.sql`。
2. 打开 **APP版本**，刷新菜单缓存后才能看到该页。
3. 填写版本号、`versionCode`（与本次发布一致或更大）。
4. 选更新方式；需要时打开「强制更新」（用户不能点「稍后」）。
5. 「最低壳 versionCode」：热更新时原生壳低于此值会改走 APK。一般填当前 Capacitor 壳的 `versionCode`。
6. 上传 APK 和/或 zip，点 **保存**，再打开 **对 APP 生效**。

上传限制：APK ≤ 200MB，zip ≤ 80MB；会做魔数校验并计算 SHA-256。

## 5. 下载页（给还没装更新器的人）

覆盖安装时仍建议把 APK 放到服务器下载页，方便首次安装或更新器尚未装上的旧包：

```powershell
.\JeecgBoot\deploy\frp\upload-apk.ps1 -ApkPath 'JeecgUniapp\dist\apk\homeai-release.apk' -Version '1.0.1'
```

详见 `[frp-home-deployment.md](../deploy/frp-home-deployment.md)` 第 6 节。

## 6. 用户侧会发生什么

1. 打开 APP → 隐私已同意 → 启动页探测版本。
2. 有新版本：弹窗展示更新说明。非强制可「稍后」；强制只能「立即更新」。
3. APK：下载后调起系统安装。首次需允许「安装未知应用」。
4. 热更新：下载 zip、校验、解压到应用私有目录，切换 Web 根路径后重载。下次覆盖安装新 APK 时，Capacitor 会丢掉这份热更新目录，改用 APK 内资源。

探测失败（后端没开、没跑 SQL）会静默进入登录/首页，不挡启动。

## 7. 检查清单

- [ ] `versionCode` 比用户手机上的大
- [ ] 管理端已保存且「对 APP 生效」已打开
- [ ] APK 覆盖：已上传 APK；热更新：zip 根目录有 `index.html`
- [ ] 加过原生能力时选 APK，不要只发 zip
- [ ] 用户已装带启动页更新检测的包（第 69 轮之后的 Capacitor APK）；更早的包仍只能去下载页重装
- [ ] API 地址变了必须重打 APK（热更新 zip 里也会带编译进包的地址，但旧壳不会自己改 `VITE_SERVER_BASEURL_APP`）



## 8. 脚本位置与用途

日常发 APP：**改版本号 →** `pnpm pack:apk:local` **→ 管理端登记 → 可选上传下载页**。若同时要发管理端和后端，用仓库总入口 [`docs/deploy/publish-all.ps1`](../deploy/publish-all.ps1)；只出 APP：`.\publish-all.ps1 -App`。下面按「要跑的 / 被它调用的 / 环境一次性 / 已弃用」列出，勿把内部脚本当入口。

工作目录：APP 出包在 `JeecgUniapp/`；下载页与穿透在 `JeecgBoot/deploy/frp/`；一键发布/启停在 `docs/deploy/`。

### 8.1 日常发版（要跑）


| 入口                    | 位置                                                 | 用处                                                                                                                                                          |
| --------------------- | -------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `publish-all.ps1`     | `docs/deploy/publish-all.ps1`                      | **按目标发布**：`-Backend` / `-Frontend` / `-App`（可组合）。只出 APP：`.\publish-all.ps1 -App`。可选 `-UploadApk`。启停见同目录 `start-all.ps1` / `stop-all.ps1` |
| `pnpm pack:apk:local` | `JeecgUniapp/scripts/capacitor/pack-apk-local.ps1` | **唯一出包命令**。品牌资源 → `pnpm build:h5` → `cap sync` → 签名 `assembleRelease`。产物在 `JeecgUniapp/dist/apk/`：带时间戳 APK、`homeai-release.apk`、热更新 `homeai-h5-{版本}.zip`    |
| 可选 `-SkipBuild`       | 同上                                                 | H5 已构建、只重打原生壳时用                                                                                                                                             |
| 可选 `-InitAndroid`     | 同上                                                 | 本机还没有 `android/` 时：`pnpm pack:apk:local -- -InitAndroid`                                                                                                    |
| 可选 `-Upload`          | 同上                                                 | 打完后立刻调 `upload-apk.ps1`（一般更习惯手动上传）                                                                                                                          |
| `upload-apk.ps1`      | `JeecgBoot/deploy/frp/upload-apk.ps1`              | 把签名 APK 传到服务器下载页 `/var/www/homeai-apk/homeai-latest.apk`。示例：`.\upload-apk.ps1 -ApkPath '..\..\..\JeecgUniapp\dist\apk\homeai-release.apk' -Version '1.0.1'` |


签名配置：`JeecgUniapp/android-pack.local.json`（从 `android-pack.local.json.example` 复制，**勿提交**）。证书 `C:\Users\57089\secret\homeai-release.keystore`。

### 8.2 出包管道内部（不要单独当发版入口）

`pack-apk-local.ps1` 会按顺序调用这些文件：


| 文件                            | 位置                                              | 用处                                                                                                       |
| ----------------------------- | ----------------------------------------------- | -------------------------------------------------------------------------------------------------------- |
| `sync-android-branding.ps1`   | `JeecgUniapp/scripts/capacitor/`                | 从 `src/static/app/icons/1024x1024.png` 生成自适应图标、启动图、通知小标、`favicon.ico`                                    |
| `HomeaiBrandAssets.cs`        | 同上                                              | 给上面脚本用的 C# 切图逻辑                                                                                          |
| `patch-android.ps1`           | 同上                                              | `cap sync` 之后补权限、明文 HTTP、minSdk 26、阿里云 Maven、把 `versionName`/`versionCode` 写入 `android/app/build.gradle` |
| `init-aliyun.gradle`          | 同上                                              | Gradle `--init-script`：给 Capacitor 子工程 classpath 也走阿里云，避免本机 JDK 直连 `dl.google.com` TLS 失败 |
| `common.ps1`                  | `JeecgUniapp/scripts/android-offline/`          | 共用函数：读 manifest 版本、写无 BOM UTF-8、拷贝带时间戳 APK、写 `keystore.properties`                                       |
| `homeai-signing.gradle`       | `JeecgUniapp/scripts/android-offline/overlays/` | 签名 gradle 片段，打进 `android/app/`                                                                           |
| `network_security_config.xml` | 同上 overlays                                     | 允许局域网/HTTP 访问后端，拷进 Android `res/xml/`                                                                    |




### 8.3 本机环境（只跑一次或排障）


| 入口                   | 位置                                                          | 用处                                                                               |
| -------------------- | ----------------------------------------------------------- | -------------------------------------------------------------------------------- |
| `pnpm pack:apk:sdk`  | `JeecgUniapp/scripts/android-offline/setup-android-sdk.ps1` | 未装 Android Studio 时安装 cmdline-tools / SDK（设置 `ANDROID_HOME`）。Capacitor 出包仍需要 SDK |
| `pnpm pack:apk:cert` | `JeecgUniapp/scripts/android-offline/show-cert.ps1`         | 打印 keystore 的 SHA1/SHA256，核对覆盖安装是否同一套证书                                          |




### 8.4 下载页与家宽穿透（发 APK 给家人装）

目录：`JeecgBoot/deploy/frp/`。手册：[frp-home-deployment.md](../deploy/frp-home-deployment.md)、目录内 [README.md](../../JeecgBoot/deploy/frp/README.md)。


| 脚本                                   | 用处                                                                                             |
| ------------------------------------ | ---------------------------------------------------------------------------------------------- |
| `remote-install.ps1`                 | 本机 SSH 到云服务器，装 frps + Nginx + `/app/` 下载页（一次性）                                                 |
| `install-server.sh`                  | 同上，但不能从本机 SSH 时在服务器上直接跑                                                                        |
| `setup-local.ps1`                    | 本机装 frpc + Windows Nginx；可选 `-BuildAdmin`、`-PatchAppEnv`（把 APP 生产 API 改成公网）、`-RegisterStartup` |
| `start-local.ps1` / `stop-local.ps1` | 日常启停本机 nginx/frpc（**不**动 Java 后端）。含 Java 的一键启停见 `docs/deploy/start-all.ps1` / `stop-all.ps1` |
| `restart-nginx.ps1`                  | 只重载本机 Nginx                                                                                    |
| `check-server.ps1`                   | 看本机端口/进程和远端 frps、nginx 是否活着                                                                    |
| `upload-apk.ps1`                     | 见 8.1，发版时真正会再次用到的                                                                              |
| `common.ps1`                         | FRP 脚本共用（SSH、读 `config.env` / `secrets.env`）                                                   |


`secrets.env`、`android-pack.local.json` 含密码，不要进 git。

### 8.5 已弃用（调用会报错或不要再跑）

DCloud 离线 SDK / HBuilderX 云打包已停。这些 npm 脚本还留着，只为避免习惯性输入打到半截旧流程：


| 入口                            | 位置                                                 | 说明                               |
| ----------------------------- | -------------------------------------------------- | -------------------------------- |
| `pnpm pack:apk`               | `scripts/android-offline/pack-apk.ps1`             | **一运行就抛错**，提示改用 `pack:apk:local` |
| `pnpm pack:apk:init`          | `scripts/android-offline/init-offline-project.ps1` | 旧：把 overlay 写入 DCloud 离线工程       |
| `pnpm pack:apk:extract-sdk`   | `scripts/android-offline/extract-offline-sdk.ps1`  | 旧：解压 DCloud Android 离线 SDK zip   |
| `overlays/dcloud_control.xml` | `scripts/android-offline/overlays/`                | 仅旧离线壳使用                          |


