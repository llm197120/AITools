import {
  jumpToGuestAuth,
  shouldBlockHomeaiNavigate,
  shouldBlockHomeaiSwitchTab,
} from '@/pages-homeai/utils/homeaiAuth'

/**
 * HomeAI 登录拦截（双端并行）：
 * - 手机号登录端（H5 / Capacitor / 旧 APP-PLUS）：未登录跳转 /pages/auth/login
 * - 微信小程序：未登录仅可访问个人中心 Tab
 */
export const homeaiRouteInterceptor = {
  install() {
    uni.addInterceptor('switchTab', {
      invoke({ url }: { url: string }) {
        const path = url.split('?')[0]
        if (shouldBlockHomeaiSwitchTab(path)) {
          jumpToGuestAuth(undefined, path)
          return false
        }
        return true
      },
    })

    const blockNavigateIfNeeded = ({ url }: { url: string }) => {
      const path = url.split('?')[0]
      if (shouldBlockHomeaiNavigate(path)) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        jumpToGuestAuth(undefined, url)
        return false
      }
      return true
    }

    uni.addInterceptor('navigateTo', {
      invoke: blockNavigateIfNeeded,
    })

    uni.addInterceptor('redirectTo', {
      invoke: blockNavigateIfNeeded,
    })

    uni.addInterceptor('reLaunch', {
      invoke: blockNavigateIfNeeded,
    })
  },
}
