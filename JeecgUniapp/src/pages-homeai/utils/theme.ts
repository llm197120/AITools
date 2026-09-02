/**
 * 主题模式管理：跟随系统 / 白天 / 夜晚。
 * 通过根节点 .dark-mode class 覆盖 CSS 变量（见 style/homeai-theme.scss）。
 */
export type ThemeMode = 'system' | 'light' | 'dark'

const THEME_KEY = 'homeai_theme_mode'

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

export function applyTheme(): void {
  const mode = getThemeMode()
  const dark = mode === 'dark' || (mode === 'system' && systemIsDark())
  if (typeof document !== 'undefined') {
    document.documentElement.classList.toggle('dark-mode', dark)
  }
}

let inited = false

/** App 启动初始化：应用当前模式并监听系统主题变化 */
export function initTheme(): void {
  if (inited) return
  inited = true
  applyTheme()
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