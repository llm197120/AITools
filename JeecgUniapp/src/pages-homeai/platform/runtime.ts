/**
 * 运行时判断：现行发版是 uni-app H5 + Capacitor；已装的旧云打包壳仍是 APP-PLUS。
 * 业务页不要直接碰 plus / Capacitor，只通过 platform/* 调用。
 */
export function isCapacitorNative(): boolean {
  try {
    const cap = (typeof window !== 'undefined' && (window as any).Capacitor) || undefined
    return !!cap?.isNativePlatform?.()
  } catch {
    return false
  }
}

/** 独立安装的 Android App（DCloud 壳或 Capacitor 壳），不是微信小程序、也不是普通浏览器 */
export function isStandaloneApp(): boolean {
  // #ifdef APP-PLUS
  return true
  // #endif
  return isCapacitorNative()
}

export function exitStandaloneApp(): void {
  // #ifdef APP-PLUS
  plus.runtime.quit()
  return
  // #endif
  if (!isCapacitorNative()) return
  import('@capacitor/app')
    .then((m) => m.App.exitApp())
    .catch(() => undefined)
}

export function openExternalUrl(url: string): void {
  // #ifdef APP-PLUS
  plus.runtime.openURL(url)
  return
  // #endif
  if (isCapacitorNative()) {
    import('@capacitor/browser')
      .then((m) => m.Browser.open({ url }))
      .catch(() => {
        if (typeof window !== 'undefined') window.open(url, '_blank', 'noopener,noreferrer')
      })
    return
  }
  if (typeof window !== 'undefined') {
    window.open(url, '_blank', 'noopener,noreferrer')
  }
}

/** 栈底页：系统返回应直接退出，不要回到启动页或停在「再按一次」 */
const APP_ROOT_ROUTES = new Set([
  'pages/launch/index',
  'pages/auth/login',
  'pages/homeai/index',
  'pages/homeai/family',
  'pages/homeai/profile',
])

function pageRoute(page: any): string {
  const raw = page?.route || page?.$page?.route || ''
  return String(raw).replace(/^\//, '')
}

function canPopUniPage(): boolean {
  const pages = getCurrentPages()
  if (!pages.length) return false
  const current = pageRoute(pages[pages.length - 1])
  if (APP_ROOT_ROUTES.has(current)) return false
  return pages.length > 1
}

let backButtonBound = false
const hardwareBackHandlers: Array<() => boolean> = []
/** Capacitor 硬件返回已处理完时，挡住 uni 同一次按键再 pop 一次 */
let suppressUniBackUntil = 0

function markHardwareBackHandled() {
  suppressUniBackUntil = Date.now() + 400
}

/** 为 true 时 onBackPress 应拦截（硬件返回已 pop / 已关蒙层） */
export function shouldSuppressUniBackPress(): boolean {
  return Date.now() < suppressUniBackUntil
}

/** 返回 true 表示已消费返回键（如关掉图片蒙层），不再 navigateBack */
export function registerHardwareBackHandler(handler: () => boolean): () => void {
  hardwareBackHandlers.push(handler)
  return () => {
    const i = hardwareBackHandlers.lastIndexOf(handler)
    if (i >= 0) hardwareBackHandlers.splice(i, 1)
  }
}

function consumeHardwareBack(): boolean {
  if (dismissPreviewImageOverlay()) return true
  for (let i = hardwareBackHandlers.length - 1; i >= 0; i--) {
    try {
      if (hardwareBackHandlers[i]()) return true
    } catch {
      // 单个处理器异常不阻断后续返回
    }
  }
  return false
}

/** uni.previewImage 在 H5 壳里是页面蒙层；系统返回必须先关掉它 */
export function dismissPreviewImageOverlay(): boolean {
  if (typeof document === 'undefined') return false
  const root = document.getElementById('u-a-p')
  const opened = !!(root && root.childElementCount > 0)
  if (!opened) return false
  try {
    const closer = (uni as any).closePreviewImage
    if (typeof closer === 'function') {
      closer()
      return true
    }
  } catch {
    // ignore
  }
  try {
    root.remove()
    return true
  } catch {
    return false
  }
}

/** 记录上次运行的壳版本（用于检测 APK 升级/重装） */
const SHELL_CODE_KEY = 'homeai_last_shell_code'
/** 热更新版本残留 key（与 updater.ts WEB_VERSION_KEY 一致，避免循环 import） */
const WEB_VERSION_KEY = 'homeai_web_version'

/**
 * 壳（APK）升级/重装后清理热更新残留：
 * Capacitor 的 persistServerBasePath 会把 WebView base path 持久化到旧热更新目录，
 * APK 升级后仍加载旧目录资源，与当前 bundle 不一致会导致 chunk 加载失败
 * （Failed to resolve module specifier）与版本误判（装新包仍弹旧更新）。
 * 检测到壳版本变化时：重置 base path 到 APK 内嵌资源（assets/public）并清除热更新版本。
 */
export async function resetHotUpdateIfShellChanged(): Promise<void> {
  if (!isCapacitorNative()) return
  let shellCode = 0
  try {
    const { App } = await import('@capacitor/app')
    const info = await App.getInfo()
    shellCode = Number(info.build)
    if (!Number.isFinite(shellCode) || shellCode <= 0) return
  } catch {
    return
  }
  let lastShell = 0
  try {
    lastShell = Number(uni.getStorageSync(SHELL_CODE_KEY))
  } catch {
    /* ignore */
  }
  if (lastShell === shellCode) return // 壳未变
  let resetApplied = false
  try {
    const { WebView } = await import('@capacitor/core')
    const { path } = await WebView.getServerBasePath()
    // 非 APK 内嵌资源（热更新目录）才需要重置
    if (path && !path.includes('/android_asset/public')) {
      await WebView.resetServerBasePath()
      resetApplied = true
    }
  } catch {
    // 重置失败不阻断启动
  }
  try {
    uni.removeStorageSync(WEB_VERSION_KEY)
  } catch {
    /* ignore */
  }
  try {
    uni.setStorageSync(SHELL_CODE_KEY, String(shellCode))
  } catch {
    /* ignore */
  }
  // 壳升级且曾指向热更新目录：重置后必须 reload，当前会话才能加载 APK 内嵌新资源
  // （否则旧页面继续运行，动态 import 的 chunk 名与旧目录不匹配 → Failed to resolve module specifier）
  if (resetApplied && typeof window !== 'undefined') {
    setTimeout(() => {
      window.location.reload()
    }, 100)
  }
}

/** Capacitor 壳：状态栏 + 返回键（有栈则返回，无可返回则退出） */
export async function initStandaloneShell(): Promise<void> {
  if (!isCapacitorNative()) return
  await resetHotUpdateIfShellChanged()
  try {
    const { StatusBar } = await import('@capacitor/status-bar')
    await StatusBar.setOverlaysWebView({ overlay: false })
  } catch {
    // 无 StatusBar 插件时忽略；颜色由 theme.applyTheme 按昼夜同步
  }
  if (backButtonBound) return
  try {
    const { App } = await import('@capacitor/app')
    await App.addListener('backButton', () => {
      if (consumeHardwareBack()) {
        markHardwareBackHandled()
        return
      }
      if (canPopUniPage()) {
        try {
          uni.hideLoading()
        } catch {
          // ignore
        }
        uni.navigateBack({})
        markHardwareBackHandled()
        return
      }
      // 栈底仍调 exitApp：Capacitor 一旦挂了 backButton 监听就不会走系统默认 finish
      void App.exitApp()
    })
    backButtonBound = true
  } catch {
    // ignore
  }
}
