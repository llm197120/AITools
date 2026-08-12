import { createRouter } from '@/plugin/uni-mini-router'
import pagesJson from '../pages.json'
import pagesJsonToRoutes from 'uni-parse-pages'
import { useUserStore } from '@/pages-homeai/stores/user'

// 生成路由表
const routes = pagesJsonToRoutes(pagesJson)
setRouteName(routes)
const router = createRouter({
  routes: [...routes],
})

/**
 * 未登录可访问白名单（与 homeaiRoute 一致：仅个人中心作登录入口）
 */
export const whiteList = ['/pages/homeai/profile']
/** 未登录默认跳转页 */
export const loginPage = '/pages/homeai/profile'

/**
 * 全局前置守卫：HomeAI 鉴权
 * - 已登录：放行
 * - 未登录：白名单放行，其它跳转个人中心
 */
export const beforEach = (to, from, next) => {
  const userStore = useUserStore()
  if (userStore.isLogin) {
    next()
    return
  }
  if (whiteList.includes(to.path)) {
    next()
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
