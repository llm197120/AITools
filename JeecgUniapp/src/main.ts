import '@/style/index.scss'
import '@/style/custom/main.css'
import '@/style/custom/icon.css'
import '@/style/custom/animation.css'
import 'virtual:uno.css'
import { createSSRApp } from 'vue'

import App from './App.vue'
import { prototypeInterceptor, requestInterceptor, routeInterceptor, homeaiRouteInterceptor } from './interceptors'
import { registerGlobComp } from '@/components/registerGlobComp'
import store from './store'
import router from './router'

export function createApp() {
  const app = createSSRApp(App)
  app.use(store)
  app.use(router)
  app.use(routeInterceptor)
  app.use(homeaiRouteInterceptor)
  app.use(requestInterceptor)
  app.use(prototypeInterceptor)
  //update-begin---author:copilot ---date:2026-08-11 for：【P2】移除未使用的 VueQuery（OpenAPI 示例已删除）-----------
  //update-end---author:copilot ---date:2026-08-11 for：【P2】移除未使用的 VueQuery（OpenAPI 示例已删除）-----------
  //#ifndef MP-WEIXIN
  // 注册全局组件
  registerGlobComp(app);
  // #endif
  return {
    app,
  }
}
