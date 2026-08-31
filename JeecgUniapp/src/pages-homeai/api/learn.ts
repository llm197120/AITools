import { get, post, put, del } from './request'

export const learnApi = {
  materials: (pageNo = 1, pageSize = 50, id?: string, keyword?: string) => {
    const params: Record<string, string> = {
      pageNo: String(pageNo),
      pageSize: String(pageSize),
    }
    if (id) params.id = id
    const kw = (keyword || '').trim()
    if (kw) params.keyword = kw
    return get<any>('/learn/materials', params)
  },
  materialById: async (id: string) => {
    const page: any = await get<any>('/learn/materials', { id, pageNo: '1', pageSize: '1' })
    const list = page?.records || page || []
    return Array.isArray(list) ? list[0] : list?.records?.[0]
  },
  preview: (id: string) => get<any>(`/learn/materials/${id}/preview`),
  /** 仅管理端 Office 转 PDF；APP 打开原文件，不要再调用 */
  previewPdf: (id: string) => post(`/learn/materials/${id}/preview-pdf`),
  categories: () => get<any[]>('/learn/category/all'),
  statistics: () => get<any>('/learn/statistics'),
  calendar: (yearMonth: string) => get<string[]>('/learn/calendar', { yearMonth }),
  records: (params?: { yearMonth?: string; studyDate?: string }) => {
    const query: Record<string, string> = {}
    if (params?.yearMonth) query.yearMonth = params.yearMonth
    if (params?.studyDate) query.studyDate = params.studyDate
    return get<any[]>('/learn/records', Object.keys(query).length ? query : undefined)
  },
  create: (data: any) => post('/learn/material', data),
  update: (data: any) => put('/learn/material', data),
  remove: (id: string) => del(`/learn/material/${id}`),
  start: (materialId: string) => post(`/learn/start?materialId=${encodeURIComponent(materialId)}`),
  stop: (materialId: string) => post(`/learn/stop?materialId=${encodeURIComponent(materialId)}`),
  activeSession: () => get<any>('/learn/session/active'),
  addRecord: (materialId: string, duration: number, notes?: string) =>
    post('/learn/record', { data: { materialId, duration, recordType: 'timer', notes } }),
  goal: () => get<any>('/learn/goal'),
  setGoal: (minutes: number) => put('/learn/goal', { params: { minutes: String(minutes) } }),
}
