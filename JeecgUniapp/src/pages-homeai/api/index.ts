/**
 * HomeAI 小程序 API 集中定义
 */
import { get } from './request'

export { get, post, put, del, getServerBaseUrl, getBaseUrl } from './request'
export { billApi } from './bill'
export { recipeApi } from './recipe'
export { storageApi } from './storage'
export { planApi } from './plan'
export { learnApi } from './learn'

export const configApi = {
  fileWhitelist: () => get<any>('/config/file-whitelist'),
  wechatPublic: () =>
    get<{ planRemindTemplateId?: string; learnRemindTemplateId?: string }>('/config/wechat-public'),
}
