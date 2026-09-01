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
  try {
    const { WebView } = await import('@capacitor/core')
    const { path } = await WebView.getServerBasePath()
    // 非 APK 内嵌资源（热更新目录）才需要重置
    if (path && !path.includes('/android_asset/public')) {
      await WebView.resetServerBasePath()
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
}

/** Capacitor 壳：状态栏 + 返回键（有栈则返回，无可返回则退出） */
export async function initStandaloneShell(): Promise<void> {
  if (!isCapacitorNative()) return
  await resetHotUpdateIfShellChanged()
  try {
    const { StatusBar, Style } = await import('@capacitor/status-bar')
    await StatusBar.setOverlaysWebView({ overlay: false })
    await StatusBar.setBackgroundColor({ color: '#F3F2EE' })
    await StatusBar.setStyle({ style: Style.Light })
  } catch {
    // 无 StatusBar 插件时忽略
  }
  if (backButtonBound) return
  try {
    const { App } = await import('@capacitor/app')
    await App.addListener('backButton', () => {
      if (canPopUniPage()) {
        uni.navigateBack({})
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
