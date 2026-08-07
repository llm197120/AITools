import { get, post, put, del, getServerBaseUrl } from './request'
import { getToken } from '../utils/auth'

export const billApi = {
  categories: (type?: string) =>
    get<any[]>('/bill/categories', type ? { type } : undefined),
  entries: (yearMonth: string) => get<any[]>('/bill/entries', { yearMonth }),
  summary: (yearMonth: string) => get<any>('/bill/summary', { yearMonth }),
  categoryStats: (yearMonth: string) => get<any[]>('/bill/stats', { yearMonth }),
  create: (data: any) => post('/bill/entry', { data }),
  update: (data: any) => put('/bill/entry', { data }),
  remove: (id: string) => del(`/bill/entry/${id}`),
  /** 小程序端导入预览 */
  importPreview: (filePath: string, type: 'wechat_csv' | 'excel') =>
    uploadImport('/bill/app/import/preview', filePath, { type }),
  /** 小程序端导入确认 */
  importConfirm: (entries: any[]) => post('/bill/app/import/confirm', { data: { entries } }),
}

function uploadImport(url: string, filePath: string, formData: Record<string, string>) {
  return new Promise<any[]>((resolve, reject) => {
    uni.uploadFile({
      url: getServerBaseUrl() + '/homeai' + url,
      filePath,
      name: 'file',
      formData,
      header: { 'X-Access-Token': getToken() || '' },
      success: (res) => {
        try {
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
}
