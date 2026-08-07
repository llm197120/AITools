import {
  HOMEAI_PROFILE_TAB,
  shouldBlockHomeaiNavigate,
  shouldBlockHomeaiSwitchTab,
} from '@/pages-homeai/utils/homeaiAuth'

/**
 * HomeAI 小程序登录拦截：未登录仅可访问个人中心 Tab
 */
export const homeaiRouteInterceptor = {
  install() {
    uni.addInterceptor('switchTab', {
      invoke({ url }: { url: string }) {
        const path = url.split('?')[0]
        if (shouldBlockHomeaiSwitchTab(path)) {
          uni.switchTab({ url: HOMEAI_PROFILE_TAB })
          return false
        }
        return true
      },
    })

    uni.addInterceptor('navigateTo', {
      invoke({ url }: { url: string }) {
        const path = url.split('?')[0]
        if (shouldBlockHomeaiNavigate(path)) {
          uni.showToast({ title: '请先登录', icon: 'none' })
          uni.switchTab({ url: HOMEAI_PROFILE_TAB })
          return false
        }
        return true
      },
    })
  },
}
