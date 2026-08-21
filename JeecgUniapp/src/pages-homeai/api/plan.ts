import { get, post, put, del } from './request'

export const planApi = {
  calendar: (yearMonth: string) => get<any>(`/plan/calendar`, { yearMonth }),
  byDate: (date: string) => get<any[]>(`/plan/date/${date}`),
  categories: () => get<any[]>('/plan/categories'),
  create: (data: any) => post('/plan', data),
  toggle: (instanceId: string) => put(`/plan/instance/${instanceId}/toggle`),
  instance: (instanceId: string) => get<any>(`/plan/instance/${instanceId}`),
  update: (instanceId: string, data: any) => put(`/plan/instance/${instanceId}`, data),
  remove: (instanceId: string) => del(`/plan/instance/${instanceId}`),
}
