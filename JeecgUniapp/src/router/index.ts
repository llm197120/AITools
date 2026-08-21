import { createRouter } from '@/plugin/uni-mini-router'
import pagesJson from '../pages.json'
import pagesJsonToRoutes from 'uni-parse-pages'
import { useUserStore } from '@/pages-homeai/stores/user'
import {
  HOMEAI_LOGIN_PAGE,
  HOMEAI_PROFILE_TAB,
  usesPhoneLogin,
} from '@/pages-homeai/utils/homeaiAuth'

// 生成路由表
const routes = pagesJsonToRoutes(pagesJson)
setRouteName(routes)
const router = createRouter({
  routes: [...routes],
})

function normalizePath(path?: string): string {
  if (!path) return ''
  return path.startsWith('/') ? path : `/${path}`
}

const PHONE_LOGIN_WHITE_LIST = [
  '/pages/launch/index',
  '/pages/auth/login',
  '/pages/agreement/index',
  '/pages/privacy/index',
  '/pages/homeai/profile',
]
const MP_WHITE_LIST = ['/pages/launch/index', '/pages/homeai/profile']

function currentWhiteList(): string[] {
  return usesPhoneLogin() ? PHONE_LOGIN_WHITE_LIST : MP_WHITE_LIST
}

function currentLoginPage(): string {
  return usesPhoneLogin() ? HOMEAI_LOGIN_PAGE : HOMEAI_PROFILE_TAB
}

/**
 * 全局前置守卫：HomeAI 鉴权
 * - 已登录：放行
 * - 未登录：白名单放行，其它跳转登录入口
 */
export const beforEach = (to, from, next) => {
  const userStore = useUserStore()
  if (userStore.isLogin) {
    next()
    return
  }
  const toPath = normalizePath(to?.path)
  if (currentWhiteList().includes(toPath)) {
    next()
    return
  }
  const loginPage = currentLoginPage()
  if (usesPhoneLogin()) {
    next({ path: loginPage, navType: 'replaceAll' })
    return
  }
  next({ path: loginPage })
}

router.beforeEach(beforEach)

/** 路由的最后一级为路由名字，不可重复 */
function setRouteName(routes) {
  routes.forEach((item) => {
    if (item.path) {
      const name = item.path.split('/').pop()
      item.name = name
    }
  })
}

export default router
