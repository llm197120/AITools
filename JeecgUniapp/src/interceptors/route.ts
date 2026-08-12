/**
 * 原 Jeecg 黑名单登录拦截（指向已删除的 /pages/login/*）
 *
 * HomeAI 鉴权已由 homeaiRouteInterceptor 接管：
 * - 未登录仅可访问个人中心 Tab（/pages/homeai/profile）
 * - Tab / 功能页拦截见 src/interceptors/homeaiRoute.ts
 *
 * 此处保留空 install，避免 main.ts 注册报错，且不与 homeaiRoute 双重拦截冲突。
 */
export const routeInterceptor = {
  install() {
    // 鉴权由 homeaiRouteInterceptor 统一处理，此处不再注册 uni.addInterceptor
  },
}
