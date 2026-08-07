import { get, post, put } from './request'

export const planApi = {
  calendar: (yearMonth: string) => get<any>(`/plan/calendar`, { yearMonth }),
  byDate: (date: string) => get<any[]>(`/plan/date/${date}`),
  categories: () => get<any[]>('/plan/categories'),
  create: (data: any) => post('/plan', data),
  toggle: (instanceId: string) => put(`/plan/instance/${instanceId}/toggle`),
}
