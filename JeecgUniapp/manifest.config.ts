// manifest.config.ts
import { defineManifestConfig } from '@uni-helper/vite-plugin-uni-manifest'
import path from 'node:path'
import { loadEnv } from 'vite'

// 获取环境变量的范例
const env = loadEnv(process.env.NODE_ENV!, path.resolve(process.cwd(), 'env'))
const {
  VITE_APP_TITLE,
  VITE_UNI_APPID,
  VITE_WX_APPID,
  VITE_APP_PUBLIC_BASE,
  VITE_FALLBACK_LOCALE,
} = env

export default defineManifestConfig({
  name: VITE_APP_TITLE,
  appid: VITE_UNI_APPID,
  description: '面向家庭场景的记账、菜谱、学习与 AI 助手',
  versionName: '1.0.1',
  versionCode: '101',
  transformPx: false,
  locale: VITE_FALLBACK_LOCALE, // 'zh-Hans'
  /* 5+App特有相关 */
  'app-plus': {
    usingComponents: true,
    webView: {
      render: 'always',
      userAgent: '',
    },
    nvueStyleCompiler: 'uni-app',
    compilerVersion: 3,
    compatible: {
      ignoreVersion: true,
    },
    splashscreen: {
      alwaysShowBeforeRender: true,
      waiting: true,
      autoclose: true,
      delay: 0,
    },
    /* 模块配置：仅保留本 App 实际用到的能力 */
    modules: {
      Camera: {},
      Gallery: {},
      // 本地通知（plus.push.createMessage），不接入 UniPush / 厂商通道
      Push: {},
    },
    /* 应用发布信息 */
    distribute: {
      /* android打包配置 */
      android: {
        packagename: 'com.homeai.app',
        minSdkVersion: 26,
        // 上架前建议 targetSdk ≥ 34（符合应用商店政策）
        targetSdkVersion: 34,
        abiFilters: ['armeabi-v7a', 'arm64-v8a'],
        // 备案前内测走局域网 HTTP；上架 HTTPS 后可改为 false
        usesCleartextTraffic: true,
        permissions: [
          // # Android 权限精简（隐私合规）：仅保留功能必需项，见 docs/plan/android-migration-design.md
          // 已移除（非必需/敏感）：MOUNT_UNMOUNT_FILESYSTEMS、READ_LOGS、GET_ACCOUNTS、READ_PHONE_STATE、
          // WRITE_SETTINGS、CHANGE_NETWORK_STATE、CHANGE_WIFI_STATE、FLASHLIGHT
          // （FLASHLIGHT：Android 6+ 闪光灯由 Camera API 控制，该权限已废弃为 no-op，移除不影响相机使用）
          // 以下为功能必需权限：
          // INTERNET：访问后端 API（自定义 permissions 会覆盖默认列表，必须显式声明）
          '<uses-permission android:name="android.permission.INTERNET"/>',
          // VIBRATE：通知/消息提醒振动
          '<uses-permission android:name="android.permission.VIBRATE"/>',
          // POST_NOTIFICATIONS：Android 13+ 本地通知（计划提醒 / 学习目标），运行时在 push.ts 申请
          '<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>',
          // ACCESS_WIFI_STATE：获取 Wi-Fi 状态（网络连接判断）
          '<uses-permission android:name="android.permission.ACCESS_WIFI_STATE"/>',
          // ACCESS_NETWORK_STATE：获取网络状态（网络连接判断）
          '<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>',
          // CAMERA：拍摄/上传图片、扫码
          '<uses-permission android:name="android.permission.CAMERA"/>',
          // WAKE_LOCK：保持唤醒（后台保活/长任务）
          '<uses-permission android:name="android.permission.WAKE_LOCK"/>',
          // 相册/存储权限（保存图片/视频到本地，见 platform/download.ts 运行时申请）：
          // WRITE_EXTERNAL_STORAGE：Android 13 以下保存到相册需要写外部存储
          '<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>',
          // READ_MEDIA_IMAGES / VIDEO / AUDIO：Android 13+ 媒体权限（相册、选音频）
          // 隐私合规：仅在用户主动保存或选择媒体时申请，拒绝后引导去系统设置开启
          '<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"/>',
          '<uses-permission android:name="android.permission.READ_MEDIA_VIDEO"/>',
          '<uses-permission android:name="android.permission.READ_MEDIA_AUDIO"/>',
          // 覆盖安装新 APK（启动页更新）
          '<uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES"/>',
          '<uses-feature android:name="android.hardware.camera.autofocus"/>',
          '<uses-feature android:name="android.hardware.camera"/>',
        ],
      },
      /* ios打包配置 */
      ios: {},
      /**
       * 已弃用云打包。下列 sdkConfigs 仅避免有人误用 HBuilderX 时再次缺 Push 红屏。
       * 云打包依据 sdkConfigs 决定是否打入原生 SDK。
       * 仅声明 modules.Push、此处为空时，云打包经常不带 aps，启动一调 plus.push 就报「缺少push模块」。
       * 只要 push 节点、不要 unipush / 厂商通道（本地通知兜底）。
       */
      sdkConfigs: {
        push: {},
      },
      /* 图标配置 */
      icons: {
        android: {
          hdpi: 'src/static/app/icons/72x72.png',
          xhdpi: 'src/static/app/icons/96x96.png',
          xxhdpi: 'src/static/app/icons/144x144.png',
          xxxhdpi: 'src/static/app/icons/192x192.png',
        },
        ios: {
          appstore: 'src/static/app/icons/1024x1024.png',
          ipad: {
            app: 'src/static/app/icons/76x76.png',
            'app@2x': 'src/static/app/icons/152x152.png',
            notification: 'src/static/app/icons/20x20.png',
            'notification@2x': 'src/static/app/icons/40x40.png',
            'proapp@2x': 'src/static/app/icons/167x167.png',
            settings: 'src/static/app/icons/29x29.png',
            'settings@2x': 'src/static/app/icons/58x58.png',
            spotlight: 'src/static/app/icons/40x40.png',
            'spotlight@2x': 'src/static/app/icons/80x80.png',
          },
          iphone: {
            'app@2x': 'src/static/app/icons/120x120.png',
            'app@3x': 'src/static/app/icons/180x180.png',
            'notification@2x': 'src/static/app/icons/40x40.png',
            'notification@3x': 'src/static/app/icons/60x60.png',
            'settings@2x': 'src/static/app/icons/58x58.png',
            'settings@3x': 'src/static/app/icons/87x87.png',
            'spotlight@2x': 'src/static/app/icons/80x80.png',
            'spotlight@3x': 'src/static/app/icons/120x120.png',
          },
        },
      },
    },
  },
  /* 快应用特有相关 */
  quickapp: {},
  /* 小程序特有相关 */
  'mp-weixin': {
    appid: VITE_WX_APPID,
    setting: {
      /**
       * 校验合法域名。本地连 127.0.0.1 时请在微信开发者工具勾选「不校验合法域名」。
       * 正式上架前需在公众平台配置 request / uploadFile 合法域名。
       */
      urlCheck: false,
      minified: true,
      es6: true,
    },
    usingComponents: true,
    // lazyCodeLoading 在 uni-app 中会导致小程序空白，暂时禁用
    // lazyCodeLoading: 'requiredComponents',
    /**
     * 隐私合规：已开启 __usePrivacyCheck__（微信小程序隐私保护指引）。
     * 上架前须保持开启，并完善隐私协议弹窗 / 权限说明（与微信小程序隐私指引一致）。
     */
    __usePrivacyCheck__: true,
  },
  'mp-alipay': {
    usingComponents: true,
    styleIsolation: 'shared',
  },
  'mp-baidu': {
    usingComponents: true,
  },
  'mp-toutiao': {
    usingComponents: true,
  },
  h5: {
    router: {
      base: VITE_APP_PUBLIC_BASE,
    },
  },
  'app-harmony': {
    distribute: {
      bundleName: 'com.homeai.app',
    },
  },
  uniStatistics: {
    enable: false,
  },
  vueVersion: '3',
})
