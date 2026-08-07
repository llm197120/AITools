/**
 * HomeAI 小程序 API 集中定义
 */
export { get, post, put, del, getServerBaseUrl, getBaseUrl } from './request'
export { billApi } from './bill'
export { recipeApi } from './recipe'
export { storageApi } from './storage'
export { aiApi } from './ai'
export { planApi } from './plan'
export { learnApi } from './learn'

import { get } from './request'

export const configApi = {
  fileWhitelist: () => get<any>('/config/file-whitelist'),
  wechatPublic: () => get<{ planRemindTemplateId?: string }>('/config/wechat-public'),
}
