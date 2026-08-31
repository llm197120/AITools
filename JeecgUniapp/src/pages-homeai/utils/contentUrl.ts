/**
 * 资料原文件鉴权地址与下载文案（APP / 预览页共用，避免各处手写 /content）
 */
import { getServerBaseUrl } from '../api/request'
import { isImageExt, isVideoExt } from './filePreview'

export function buildContentUrl(kind: 'storage' | 'learn', id: string): string {
  const base = getServerBaseUrl().replace(/\/$/, '')
  return kind === 'storage'
    ? `${base}/homeai/storage/files/${id}/content`
    : `${base}/homeai/learn/materials/${id}/content`
}

export function resolveContentUrl(input: {
  id?: string
  materialId?: string
  fileUrl?: string
}): string {
  if (input.id) return buildContentUrl('storage', input.id)
  if (input.materialId) return buildContentUrl('learn', input.materialId)
  return input.fileUrl || ''
}

/** 图片/视频是「保存到相册」，文档实际是下载后系统打开 */
export function fileSaveActionName(ext?: string): string {
  const e = (ext || '').toLowerCase()
  if (isImageExt(e) || isVideoExt(e)) return '保存到相册'
  return '打开文件'
}

export function downloadFailTitle(err: unknown): string {
  const raw = String((err as { message?: string })?.message || err || '')
  if (/401|未登录|过期/.test(raw)) return '登录已过期，请重新登录'
  if (/403|无权/.test(raw)) return '无权访问该文件'
  if (/404|不存在/.test(raw)) return '文件不存在或已删除'
  if (/打开失败|没有可打开/.test(raw)) return '手机上没有可打开该文件的应用'
  if (raw && raw !== '下载失败' && raw.length <= 28 && !/^HTTP \d+$/i.test(raw)) return raw
  return '下载失败，请稍后重试'
}
