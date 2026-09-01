import { get, post, put, del, getServerBaseUrl } from './request'
import { getToken } from '../utils/auth'
import { consumeHomeaiUnauthorized } from '../utils/homeaiAuth'
import { toUploadParams } from '../platform/uploadPath'

export const billApi = {
  categories: (type?: string) =>
    get<any[]>('/bill/categories', type ? { type } : undefined),
  entries: (yearMonth: string, pageNo?: number, pageSize?: number, keyword?: string) => {
    const params: Record<string, string> = { yearMonth }
    if (pageNo != null) {
      params.pageNo = String(pageNo)
      params.pageSize = String(pageSize ?? 20)
    }
    const kw = (keyword || '').trim()
    if (kw) params.keyword = kw
    return get<any>('/bill/entries', params)
  },
  entryById: (id: string) => get<any>(`/bill/entry/${id}`),
  summary: (yearMonth: string) => get<any>('/bill/summary', { yearMonth }),
  categoryStats: (yearMonth: string) => get<any[]>('/bill/stats', { yearMonth }),
  create: (data: any) => post('/bill/entry', { data }),
  update: (data: any) => put('/bill/entry', { data }),
  remove: (id: string) => del(`/bill/entry/${id}`),
  /** 小程序端导入预览 */
  importPreview: (filePath: string, type: 'wechat_csv' | 'excel', fileName?: string) =>
    uploadImport('/bill/app/import/preview', filePath, { type }, fileName),
  /** 小程序端导入确认 */
  importConfirm: (entries: any[]) => post('/bill/app/import/confirm', { data: { entries } }),
}

function uploadImport(
  url: string,
  filePath: string,
  formData: Record<string, string>,
  fileName?: string,
) {
  return new Promise<any[]>((resolve, reject) => {
    toUploadParams(filePath, fileName)
      .then((upload) => {
        uni.uploadFile({
          url: getServerBaseUrl() + '/homeai' + url,
          ...upload,
          name: 'file',
          formData,
          header: { 'X-Access-Token': getToken() || '' },
          timeout: 120000,
          success: (res) => {
            try {
              if (consumeHomeaiUnauthorized(res.statusCode, res.data)) {
                reject(new Error('登录已过期'))
                return
              }
              const data = JSON.parse(res.data)
              if (data.success) resolve(data.result)
              else reject(new Error(data.message || '解析失败'))
            } catch (e) {
              reject(e)
            }
          },
          fail: reject,
        })
      })
      .catch(reject)
  })
}
