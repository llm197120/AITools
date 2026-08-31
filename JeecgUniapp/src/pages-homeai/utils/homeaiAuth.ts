import { isMpWeixin } from '@/utils/platform'
import { useUserStore } from '../stores/user'

/** 手机号登录：Android 主客户端（H5 / Capacitor）与旧 APP-PLUS；仅微信小程序走微信登录 */
export function usesPhoneLogin(): boolean {
  return !isMpWeixin
}

/** 未登录用户唯一可访问的 Tab 页（小程序登录入口） */
export const HOMEAI_PROFILE_TAB = '/pages/homeai/profile'

/** APP 端手机号登录/注册页 */
export const HOMEAI_LOGIN_PAGE = '/pages/auth/login'

/** 启动中转页 */
export const HOMEAI_LAUNCH_PAGE = '/pages/launch/index'

export const HOMEAI_ONBOARD_FAMILY_KEY = 'homeai_onboard_family'

const HOMEAI_TAB_NEED_LOGIN = ['/pages/homeai/index', '/pages/homeai/family']

const HOMEAI_TABS = ['/pages/homeai/index', '/pages/homeai/family', '/pages/homeai/profile']

/** 需登录才能进入的功能页（子包等） */
const HOMEAI_FEATURE_PREFIXES = [
  '/pages-homeai-ai/',
  '/pages-homeai-more/',
]

export function isHomeaiFeaturePath(path: string): boolean {
  const p = path.split('?')[0]
  if (p === '/pages/auth/change-password' || p === '/pages/auth/profile-edit') {
    return true
  }
  return HOMEAI_FEATURE_PREFIXES.some((prefix) => p.startsWith(prefix))
}

export function isHomeaiTabPath(path: string): boolean {
  return HOMEAI_TABS.includes(path.split('?')[0])
}

/** 当前页完整路径（含 query），用于 401 回跳 */
export function getCurrentFullPath(): string {
  const pages = getCurrentPages()
  const current: any = pages[pages.length - 1]
  if (!current?.route) return ''
  const path = `/${current.route}`
  const opts = current.options || {}
  const keys = Object.keys(opts)
  if (!keys.length) return path
  const query = keys.map((k) => `${encodeURIComponent(k)}=${encodeURIComponent(String(opts[k]))}`).join('&')
  return `${path}?${query}`
}

function buildLoginUrl(mode?: 'register', redirect?: string): string {
  const params: string[] = []
  if (mode === 'register') params.push('mode=register')
  if (redirect && redirect !== HOMEAI_LOGIN_PAGE && redirect !== HOMEAI_LAUNCH_PAGE) {
    params.push(`redirect=${encodeURIComponent(redirect)}`)
  }
  return params.length ? `${HOMEAI_LOGIN_PAGE}?${params.join('&')}` : HOMEAI_LOGIN_PAGE
}

/**
 * 未登录时跳转登录入口：APP 进手机号登录页，小程序进个人中心。
 * @param mode 仅 APP 有效；register 时打开注册模式
 * @param redirect 登录成功后回跳路径
 */
export function jumpToGuestAuth(mode?: 'register', redirect?: string) {
  if (usesPhoneLogin()) {
    uni.reLaunch({ url: buildLoginUrl(mode, redirect) })
    return
  }
  uni.switchTab({ url: HOMEAI_PROFILE_TAB })
}

/**
 * 从当前页进入登录/注册页（保留页面栈，便于从个人中心返回）
 * @param mode register 时打开注册模式
 */
export function openAuthPage(mode?: 'register') {
  if (usesPhoneLogin()) {
    const redirect = getCurrentFullPath()
    uni.navigateTo({ url: buildLoginUrl(mode, redirect) })
    return
  }
  uni.switchTab({ url: HOMEAI_PROFILE_TAB })
}

/** 登录成功后回到原页，或进入首页 */
export function afterLoginNavigate(redirect?: string) {
  const raw = redirect ? decodeURIComponent(redirect) : ''
  const path = raw.split('?')[0]
  if (path && isHomeaiTabPath(path)) {
    uni.switchTab({ url: path })
    return
  }
  if (path && (isHomeaiFeaturePath(path) || path.startsWith('/pages-homeai'))) {
    uni.switchTab({ url: '/pages/homeai/index' })
    setTimeout(() => {
      uni.navigateTo({ url: raw.startsWith('/') ? raw : `/${raw}` })
    }, 400)
    return
  }
  uni.switchTab({ url: '/pages/homeai/index' })
}

/** Tab 页：未登录时引导到登录入口，不发起微信登录、不提示「登录失败」 */
export function ensureProfileWhenGuest(): boolean {
  const userStore = useUserStore()
  if (userStore.isLogin) {
    return true
  }
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  const route = current?.route ? `/${current.route}` : ''
  if (usesPhoneLogin()) {
    if (route !== HOMEAI_LOGIN_PAGE && route !== HOMEAI_LAUNCH_PAGE) {
      jumpToGuestAuth(undefined, getCurrentFullPath())
      return false
    }
    return true
  }
  if (route !== HOMEAI_PROFILE_TAB && route !== HOMEAI_LAUNCH_PAGE) {
    uni.switchTab({ url: HOMEAI_PROFILE_TAB })
    return false
  }
  return true
}

/** 操作前检查登录，未登录引导到登录入口 */
export function ensureLoginForAction(showToast = true): boolean {
  const userStore = useUserStore()
  if (userStore.isLogin) {
    return true
  }
  if (showToast) {
    uni.showToast({ title: '请先登录', icon: 'none' })
  }
  jumpToGuestAuth(undefined, getCurrentFullPath())
  return false
}

/** 微信一键登录（仅小程序端） */
export async function wechatLogin() {
  const userStore = useUserStore()
  const loginRes = await uni.login({ provider: 'weixin' })
  if (!loginRes.code) {
    throw new Error('获取微信登录凭证失败')
  }
  await userStore.login(loginRes.code)
  await userStore.refreshUserInfo()
}

export function shouldBlockHomeaiSwitchTab(path: string): boolean {
  const userStore = useUserStore()
  return !userStore.isLogin && HOMEAI_TAB_NEED_LOGIN.includes(path)
}

export function shouldBlockHomeaiNavigate(path: string): boolean {
  const userStore = useUserStore()
  return !userStore.isLogin && isHomeaiFeaturePath(path)
}

export function isHomeaiUnauthorizedPayload(statusCode?: number, data?: any): boolean {
  if (statusCode === 401) return true
  const code = data?.code
  return code === 401 || code === '401'
}

/** 401：清本地会话并回登录。上传 / 对话等旁路请求与 request.ts 共用。 */
export function handleHomeaiUnauthorized() {
  useUserStore().clearLocalSession()
  jumpToGuestAuth(undefined, getCurrentFullPath())
}

export function consumeHomeaiUnauthorized(statusCode?: number, raw?: any): boolean {
  let data = raw
  if (typeof raw === 'string') {
    try {
      data = JSON.parse(raw)
    } catch {
      data = undefined
    }
  }
  if (!isHomeaiUnauthorizedPayload(statusCode, data)) return false
  handleHomeaiUnauthorized()
  return true
}
