import { get, post, put } from './request'

export const recipeApi = {
  list: (params?: Record<string, string>) => get<any>('/recipe/list', params),
  detail: (id: string) => get<any>(`/recipe/${id}`),
  categories: () => get<any[]>('/recipe/category/all'),
  favorites: () => get<any[]>('/recipe/favorites'),
  hot: (limit = 20) => get<any[]>('/recipe/hot', { limit: String(limit) }),
  recommend: (limit = 8, season = 'auto') =>
    get<any[]>('/recipe/recommend', { limit: String(limit), season }),
  newest: (limit = 8, days = 30) =>
    get<any[]>('/recipe/new', { limit: String(limit), days: String(days) }),
  create: (data: any) => post('/recipe', { data }),
  update: (data: any) => put('/recipe', { data }),
  toggleFavorite: (id: string) => post(`/recipe/${id}/favorite`),
}
