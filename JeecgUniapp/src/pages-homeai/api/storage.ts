import { get, post, put, patch, del } from './request'

export const storageApi = {
  folders: () => get<any[]>('/storage/folders'),
  assignableFamilies: () => get<Array<{ id: string; name: string }>>('/storage/assignable-families'),
  folderFiles: (folderId: string, pageNo?: number, pageSize?: number) =>
    get<any>(
      `/storage/folders/${folderId}/files`,
      pageNo != null ? { pageNo: String(pageNo), pageSize: String(pageSize ?? 20) } : undefined,
    ),
  rootFiles: (pageNo?: number, pageSize?: number) =>
    get<any>(
      '/storage/files/root',
      pageNo != null ? { pageNo: String(pageNo), pageSize: String(pageSize ?? 20) } : undefined,
    ),
  fileDetail: (id: string) => get<any>(`/storage/files/${id}`),
  fileAccessUrl: (id: string) => get<string>(`/storage/files/${id}/access-url`),
  createFolder: (
    name: string,
    parentId?: string,
    visibility = 'private',
    familyIds?: string[],
  ) => {
    const params: Record<string, string> = { name, visibility }
    if (parentId) params.parentId = parentId
    if (familyIds?.length) params.familyIds = familyIds.join(',')
    return post('/storage/folders', { params })
  },
  deleteFolder: (id: string) => del(`/storage/folders/${id}`),
  renameFolder: (id: string, name: string) => put(`/storage/folders/${id}/rename`, { params: { name } }),
  updateFolderVisibility: (id: string, visibility: string, familyIds?: string[]) => {
    const params: Record<string, string> = { visibility }
    if (familyIds?.length) params.familyIds = familyIds.join(',')
    return patch(`/storage/folders/${id}/visibility`, params)
  },
  updateFileVisibility: (id: string, visibility: string, familyIds?: string[]) => {
    const params: Record<string, string> = { visibility }
    if (familyIds?.length) params.familyIds = familyIds.join(',')
    return patch(`/storage/files/${id}/visibility`, params)
  },
  deleteFile: (id: string) => del(`/storage/files/${id}`),
  renameFile: (id: string, name: string) => put(`/storage/files/${id}/rename`, { params: { name } }),
  toggleFavorite: (id: string) => put(`/storage/files/${id}/favorite`),
  search: (keyword: string) => get<any[]>('/storage/files/search', { keyword }),
  myRecycleBin: (params?: { type?: 'file' | 'folder'; keyword?: string; pageNo?: number; pageSize?: number }) => {
    const q: Record<string, string> = {
      type: params?.type || 'file',
      pageNo: String(params?.pageNo ?? 1),
      pageSize: String(params?.pageSize ?? 20),
    }
    if (params?.keyword) q.keyword = params.keyword
    return get<any>('/storage/my/recycleBin', q)
  },
  myRestore: (payload: { fileIds?: string[]; folderIds?: string[] }) =>
    put('/storage/my/restore', { data: payload }),
  myDeletePermanently: (payload: { fileIds?: string[]; folderIds?: string[] }) =>
    del('/storage/my/deletePermanently', { data: payload }),
  generateQuotaCheck: (instruction?: string) =>
    get<any>('/storage/office/generate/quota-check', instruction ? { instruction } : undefined),
  convert: (fileId: string, targetFormat: string) =>
    post('/storage/office/convert', { params: { fileId, targetFormat } }),
  preview: (id: string) => get<any>(`/storage/files/${id}/preview`),
  previewPdf: (id: string) => post(`/storage/files/${id}/preview-pdf`),
  generate: (fileId: string, instruction: string, docType?: string) =>
    post('/storage/office/generate', { params: { fileId, instruction, docType: docType || 'word' } }),
  convertHistory: () => get<any[]>('/storage/office/history'),
}
