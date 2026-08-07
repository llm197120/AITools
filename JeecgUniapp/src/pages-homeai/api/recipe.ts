import { get, post, put } from './request'

export const recipeApi = {
  list: (params?: Record<string, string>) => get<any>('/recipe/list', params),
  detail: (id: string) => get<any>(`/recipe/${id}`),
  categories: () => get<any[]>('/recipe/category/all'),
  favorites: () => get<any[]>('/recipe/favorites'),
  create: (data: any) => post('/recipe', { data }),
  update: (data: any) => put('/recipe', { data }),
  toggleFavorite: (id: string) => post(`/recipe/${id}/favorite`),
}
