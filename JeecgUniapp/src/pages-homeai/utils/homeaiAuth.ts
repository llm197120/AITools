import { useUserStore } from '../stores/user'

/** 未登录用户唯一可访问的 Tab 页 */
export const HOMEAI_PROFILE_TAB = '/pages/homeai/profile'

const HOMEAI_TAB_NEED_LOGIN = ['/pages/homeai/index', '/pages/homeai/family']

/** 需登录才能进入的功能页（子包等） */
const HOMEAI_FEATURE_PREFIXES = [
  '/pages-homeai-ai/',
  '/pages-homeai-more/',
]

export function isHomeaiFeaturePath(path: string): boolean {
  return HOMEAI_FEATURE_PREFIXES.some((prefix) => path.startsWith(prefix))
}

/** Tab 页：未登录时强制回到个人中心 */
export function ensureProfileWhenGuest(): boolean {
  const userStore = useUserStore()
  if (userStore.isLogin) {
    return true
  }
  const pages = getCurrentPages()
  const current = pages[pages.length - 1]
  const route = current?.route ? `/${current.route}` : ''
  if (route !== HOMEAI_PROFILE_TAB) {
    uni.switchTab({ url: HOMEAI_PROFILE_TAB })
    return false
  }
  return true
}

/** 操作前检查登录，未登录跳转个人中心 */
export function ensureLoginForAction(showToast = true): boolean {
  const userStore = useUserStore()
  if (userStore.isLogin) {
    return true
  }
  if (showToast) {
    uni.showToast({ title: '请先登录', icon: 'none' })
  }
  uni.switchTab({ url: HOMEAI_PROFILE_TAB })
  return false
}

/** 微信一键登录 */
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
