import {
  HOMEAI_PROFILE_TAB,
  shouldBlockHomeaiNavigate,
  shouldBlockHomeaiSwitchTab,
} from '@/pages-homeai/utils/homeaiAuth'

// #ifdef APP-PLUS
/** APP 端未登录跳转目标：手机号+密码登录页（仅 APP 编译，小程序端不包含此常量） */
const HOMEAI_LOGIN_REDIRECT = '/pages/auth/login'
// #endif

/**
 * HomeAI 登录拦截（双端并行）：
 * - APP 端：未登录跳转登录页 /pages/auth/login（reLaunch 清空页面栈，登录页为根页面）
 * - 小程序/H5 等端：保持原行为，未登录仅可访问个人中心 Tab
 */
export const homeaiRouteInterceptor = {
  install() {
    /** 未登录时按平台跳转（小程序端行为与原实现完全一致） */
    const jumpGuestRedirect = () => {
      // #ifdef APP-PLUS
      // APP 端：跳转登录页
      uni.reLaunch({ url: HOMEAI_LOGIN_REDIRECT })
      // #endif
      // #ifndef APP-PLUS
      // 小程序/H5 等端：保持原行为，跳转个人中心 Tab
      uni.switchTab({ url: HOMEAI_PROFILE_TAB })
      // #endif
    }

    uni.addInterceptor('switchTab', {
      invoke({ url }: { url: string }) {
        const path = url.split('?')[0]
        if (shouldBlockHomeaiSwitchTab(path)) {
          jumpGuestRedirect()
          return false
        }
        return true
      },
    })

    const blockNavigateIfNeeded = ({ url }: { url: string }) => {
      const path = url.split('?')[0]
      if (shouldBlockHomeaiNavigate(path)) {
        uni.showToast({ title: '请先登录', icon: 'none' })
        jumpGuestRedirect()
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
