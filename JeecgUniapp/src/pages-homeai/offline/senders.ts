/**
 * 同步执行器注册：syncQueue 恢复在线后按 module+action 分发到真实后端接口。
 * payload 结构由各页面 mutate 时约定（与注册处保持一致）。
 */
import { planApi } from '../api/plan'
import { billApi } from '../api/bill'
import { learnApi } from '../api/learn'
import { recipeApi } from '../api/recipe'
import { storageApi } from '../api/storage'
import { post } from '../api/request'
import { registerSender } from './syncQueue'
import { listPendingUploads, deletePendingUpload } from './pendingUpload'
import { getToken } from '../utils/auth'

/** 把 pending://key 图片上传为真实 URL（离线保存的菜谱图片，恢复同步时补传） */
async function uploadPendingImage(key: string): Promise<string | null> {
  const items = await listPendingUploads()
  const it = items.find((i) => i.key === key)
  if (!it) return null
  try {
    const url = await new Promise<string>((resolve, reject) => {
      uni.uploadFile({
        url: it.url,
        file: it.blob,
        name: it.name || 'file',
        fileName: it.fileName,
        formData: it.formData || {},
        header: { 'X-Access-Token': getToken() || '' },
        timeout: 120000,
        success: (res) => {
          try {
            const d = JSON.parse(res.data)
            if (d.success) resolve(d.result)
            else reject(new Error(d.message || '上传失败'))
          } catch (e) {
            reject(e)
          }
        },
        fail: reject,
      })
    })
    await deletePendingUpload(key)
    return url as string
  } catch {
    return null
  }
}

/** 回填 payload 中 pending:// 图片字段 */
async function resolvePendingImages(data: any): Promise<any> {
  const out = { ...data }
  for (const f of ['coverUrl', 'cover', 'imageUrl', 'videoUrl', 'audioUrl']) {
    const v = out[f]
    if (typeof v === 'string' && v.startsWith('pending://')) {
      const real = await uploadPendingImage(v.slice('pending://'.length))
      if (real) out[f] = real
      else out[f] = ''
    }
  }
  return out
}

export function registerAllSenders(): void {
  // ---- 计划 ----
  registerSender('plan', 'create', (p: any) => planApi.create(p.data))
  registerSender('plan', 'update', (p: any) => planApi.update(p.instanceId, p.data))
  registerSender('plan', 'toggle', (p: any) => planApi.toggle(p.instanceId))
  registerSender('plan', 'remove', (p: any) => planApi.remove(p.instanceId))

  // ---- 账单 ----
  registerSender('bill', 'create', (p: any) => billApi.create(p.data))
  registerSender('bill', 'update', (p: any) => billApi.update(p.data))
  registerSender('bill', 'remove', (p: any) => billApi.remove(p.id))

  // ---- 学习 ----
  registerSender('learn', 'addRecord', (p: any) =>
    post('/learn/record', {
      data: {
        materialId: p.materialId || '',
        duration: p.duration,
        recordType: p.recordType || 'timer',
        notes: p.notes || '',
      },
    }),
  )
  registerSender('learn', 'stop', (p: any) => learnApi.stop(p.materialId))
  registerSender('learn', 'setGoal', (p: any) => learnApi.setGoal(p.minutes))
  registerSender('learn', 'create', (p: any) => learnApi.create(p.data))
  registerSender('learn', 'update', (p: any) => learnApi.update(p.data))
  registerSender('learn', 'remove', (p: any) => learnApi.remove(p.id))

  // ---- 菜谱 ----
  registerSender('recipe', 'create', async (p: any) =>
    recipeApi.create({ data: await resolvePendingImages(p.data) }),
  )
  registerSender('recipe', 'update', async (p: any) =>
    recipeApi.update({ data: await resolvePendingImages(p.data) }),
  )
  registerSender('recipe', 'remove', (p: any) => recipeApi.remove(p.id))
  registerSender('recipe', 'toggleFavorite', (p: any) => recipeApi.toggleFavorite(p.id))

  // ---- 资料（存储） ----
  registerSender('storage', 'createFolder', (p: any) =>
    storageApi.createFolder(p.name, p.parentId, p.visibility, p.familyIds),
  )
  registerSender('storage', 'renameFolder', (p: any) => storageApi.renameFolder(p.id, p.name))
  registerSender('storage', 'deleteFolder', (p: any) => storageApi.deleteFolder(p.id))
  registerSender('storage', 'renameFile', (p: any) => storageApi.renameFile(p.id, p.name))
  registerSender('storage', 'deleteFile', (p: any) => storageApi.deleteFile(p.id))
  registerSender('storage', 'updateFileVisibility', (p: any) =>
    storageApi.updateFileVisibility(p.id, p.visibility, p.familyIds),
  )
  registerSender('storage', 'updateFolderVisibility', (p: any) =>
    storageApi.updateFolderVisibility(p.id, p.visibility, p.familyIds),
  )
  registerSender('storage', 'toggleFavorite', (p: any) => storageApi.toggleFavorite(p.id))
}
