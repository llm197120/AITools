import type { App } from 'vue'

/**
 * OA 页面已移除，原 echarts / 拖拽相关全局组件不再注册。
 * 保留空实现以兼容 #ifndef MP-WEIXIN 分支的调用。
 */
export function registerGlobComp(app: App) {
  // 留空
}
