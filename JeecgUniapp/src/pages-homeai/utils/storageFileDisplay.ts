/**
 * 资料文件展示名（始终使用 originalName，不使用 storedName / fileUrl）
 */
export type StorageFileLike = {
  originalName?: string
  original_name?: string
  storedName?: string
  stored_name?: string
}

export function getStorageDisplayName(file?: StorageFileLike | null): string {
  if (!file) return '未命名文件'
  const name = file.originalName || file.original_name
  if (name && String(name).trim()) return String(name).trim()
  return '未命名文件'
}

/** 规范化列表项字段（兼容 snake_case） */
export function normalizeStorageFile<T extends StorageFileLike>(file: T): T {
  if (!file) return file
  if (!file.originalName && file.original_name) {
    file.originalName = file.original_name
  }
  if (!file.storedName && file.stored_name) {
    file.storedName = file.stored_name
  }
  return file
}

export function normalizeStorageFiles<T extends StorageFileLike>(files?: T[] | null): T[] {
  if (!files) return []
  return files.map((f) => normalizeStorageFile(f))
}
