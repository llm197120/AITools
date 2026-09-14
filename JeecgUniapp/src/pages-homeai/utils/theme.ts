/**
 * 主题模式管理：跟随系统 / 白天 / 夜晚。
 * 通过根节点 .dark-mode class 覆盖 CSS 变量（见 style/homeai-theme.scss），
 * 并同步 Uni 标题栏 / TabBar / 系统状态栏（pages.json 写死的浅色不会随 class 变）。
 */
export type ThemeMode = 'system' | 'light' | 'dark'

const THEME_KEY = 'homeai_theme_mode'

const CHROME_LIGHT = {
  bg: '#F3F2EE',
  front: '#000000' as const,
  tabColor: '#8A857C',
  tabSelected: '#1B4F8A',
  border: 'white' as const,
}

const CHROME_DARK = {
  bg: '#17181C',
  front: '#ffffff' as const,
  tabColor: '#A8A49A',
  tabSelected: '#4D8FD6',
  border: 'black' as const,
}

export function getThemeMode(): ThemeMode {
  try {
    const v = uni.getStorageSync(THEME_KEY)
    if (v === 'light' || v === 'dark' || v === 'system') return v
  } catch {
    /* ignore */
  }
  return 'system'
}

export function setThemeMode(mode: ThemeMode): void {
  try {
    uni.setStorageSync(THEME_KEY, mode)
  } catch {
    /* ignore */
  }
  applyTheme()
}

export function systemIsDark(): boolean {
  try {
    if (typeof window !== 'undefined' && window.matchMedia) {
      return window.matchMedia('(prefers-color-scheme: dark)').matches
    }
  } catch {
    /* ignore */
  }
  return false
}

export function isDarkTheme(): boolean {
  const mode = getThemeMode()
  return mode === 'dark' || (mode === 'system' && systemIsDark())
}

function syncUniChrome(dark: boolean) {
  const c = dark ? CHROME_DARK : CHROME_LIGHT
  try {
    uni.setNavigationBarColor({
      frontColor: c.front,
      backgroundColor: c.bg,
    })
  } catch {
    /* H5 部分页 custom 导航栏会失败 */
  }
  try {
    uni.setTabBarStyle({
      color: c.tabColor,
      selectedColor: c.tabSelected,
      backgroundColor: c.bg,
      borderStyle: c.border,
    })
  } catch {
    /* 非 tab 页忽略 */
  }
  if (typeof document === 'undefined') return
  document.documentElement.style.colorScheme = dark ? 'dark' : 'light'
  let meta = document.querySelector('meta[name="theme-color"]')
  if (!meta) {
    meta = document.createElement('meta')
    meta.setAttribute('name', 'theme-color')
    document.head.appendChild(meta)
  }
  meta.setAttribute('content', c.bg)
}

async function syncNativeChrome(dark: boolean) {
  const bg = dark ? CHROME_DARK.bg : CHROME_LIGHT.bg
  try {
    const { registerPlugin } = await import('@capacitor/core')
    const plugin = registerPlugin<{
      setSystemBars: (o: { backgroundColor: string; lightContent: boolean }) => Promise<void>
    }>('HomeaiUpdate')
    await plugin.setSystemBars({ backgroundColor: bg, lightContent: dark })
  } catch {
    /* 旧壳无 setSystemBars，退回 StatusBar */
  }
  try {
    const { StatusBar, Style } = await import('@capacitor/status-bar')
    await StatusBar.setBackgroundColor({ color: bg })
    await StatusBar.setStyle({ style: dark ? Style.Dark : Style.Light })
  } catch {
    /* 无 StatusBar 插件 */
  }
}

export function applyTheme(): void {
  const dark = isDarkTheme()
  if (typeof document !== 'undefined') {
    document.documentElement.classList.toggle('dark-mode', dark)
  }
  syncUniChrome(dark)
  void syncNativeChrome(dark)
}

let inited = false
let routeChromeBound = false

function paintChromeAfterRoute() {
  setTimeout(() => applyTheme(), 0)
}

function bindRouteChrome() {
  if (routeChromeBound) return
  routeChromeBound = true
  ;(['navigateTo', 'redirectTo', 'reLaunch', 'switchTab', 'navigateBack'] as const).forEach((api) => {
    try {
      uni.addInterceptor(api, { success: paintChromeAfterRoute })
    } catch {
      /* ignore */
    }
  })
}

/** App 启动初始化：应用当前模式并监听系统主题变化 */
export function initTheme(): void {
  if (inited) return
  inited = true
  applyTheme()
  bindRouteChrome()
  try {
    if (typeof window !== 'undefined' && window.matchMedia) {
      const mq = window.matchMedia('(prefers-color-scheme: dark)')
      const onChange = () => {
        if (getThemeMode() === 'system') applyTheme()
      }
      if (typeof mq.addEventListener === 'function') mq.addEventListener('change', onChange)
      else if (typeof mq.addListener === 'function') (mq as any).addListener(onChange)
    }
  } catch {
    /* ignore */
  }
}