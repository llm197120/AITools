/**
 * HomeAI 小程序 put 请求导出修复
 */
import { put as _put } from '../api/request'
export { get, post, del } from '../api/request'
export const put = _put
