import { ref, computed } from 'vue'

import { storageApi } from '../api/storage'

import {

  buildBreadcrumb,

  canCreateSubFolder,

  findFolderNode,

  getChildFolders,

  isFileOwner,

  isFolderOwner,

  type StorageFolderNode,

} from './folderTree'

import { uploadStorageFile, showStorageUploadMenu } from './useStorageUpload'
import { getStorageDisplayName, normalizeStorageFiles } from './storageFileDisplay'
import {
  pickStorageVisibility,
  pickStorageVisibilityChange,
  type StorageVisibility,
} from './storageVisibility'

import { previewFile } from './filePreview'
import { downloadStorageFile } from './fileDownload'



export function useStorageBrowser(getFolderId: () => string | null, getUserId: () => string | undefined) {

  const folderTree = ref<StorageFolderNode[]>([])

  const files = ref<any[]>([])

  const loading = ref(false)

  const currentFolder = ref<StorageFolderNode | null>(null)



  const subFolders = computed(() => getChildFolders(folderTree.value, getFolderId()))

  const breadcrumbs = computed(() => buildBreadcrumb(folderTree.value, getFolderId()))

  const canCreateFolder = computed(() => canCreateSubFolder(currentFolder.value))



  async function refresh() {

    loading.value = true

    try {

      folderTree.value = (await storageApi.folders()) || []

      const fid = getFolderId()

      currentFolder.value = fid ? findFolderNode(folderTree.value, fid) : null

      if (fid) {

        files.value = normalizeStorageFiles((await storageApi.folderFiles(fid)) || [])

      } else {

        files.value = normalizeStorageFiles((await storageApi.rootFiles()) || [])

      }

    } finally {

      loading.value = false

    }

  }



  function onBreadcrumb(id: string | null) {

    if (id === getFolderId()) return

    if (!id) {

      uni.redirectTo({ url: '/pages-homeai-more/storage/index' })

      return

    }

    const node = findFolderNode(folderTree.value, id)

    uni.redirectTo({

      url: `/pages-homeai-more/storage/files?folderId=${id}&name=${encodeURIComponent(node?.name || '')}`,

    })

  }



  function enterFolder(folder: StorageFolderNode) {

    uni.navigateTo({

      url: `/pages-homeai-more/storage/files?folderId=${folder.id}&name=${encodeURIComponent(folder.name)}`,

    })

  }



  function openFile(file: any) {

    const ext = file.extension || ''

    previewFile({

      id: file.id,

      fileUrl: file.fileUrl,

      originalName: getStorageDisplayName(file),

      extension: ext,

    })

  }



  async function downloadFile(file: any) {

    await downloadStorageFile({

      id: file.id,

      fileUrl: file.fileUrl,

      originalName: file.originalName,

      extension: file.extension,

    })

  }



  function canDeleteFolder(folder: StorageFolderNode) {

    return isFolderOwner(folder, getUserId())

  }



  function canDeleteFile(file: any) {

    return isFileOwner(file, getUserId())

  }



  function canEditFolder(folder: StorageFolderNode) {

    return isFolderOwner(folder, getUserId())

  }



  async function createFolder(parentId: string | null) {

    if (!canCreateFolder.value) {

      uni.showToast({ title: '已达最大文件夹层级(5级)', icon: 'none' })

      return

    }

    uni.showModal({

      title: '新建文件夹',

      editable: true,

      placeholderText: '输入文件夹名称',

      success: async (res) => {

        if (res.confirm && res.content?.trim()) {

          try {

            let visibility: StorageVisibility =
              (currentFolder.value?.visibility as StorageVisibility) || 'private'
            let familyIds: string[] | undefined = currentFolder.value?.familyIds

            if (!parentId) {
              const picked = await pickStorageVisibility('private')
              if (!picked) return
              visibility = picked.visibility
              familyIds = picked.familyIds
            }

            await storageApi.createFolder(res.content.trim(), parentId || undefined, visibility, familyIds)

            uni.showToast({ title: '创建成功', icon: 'success' })

            await refresh()

          } catch (e: any) {

            uni.showToast({ title: e.message || '创建失败', icon: 'none' })

          }

        }

      },

    })

  }



  async function renameFolder(folder: StorageFolderNode) {

    if (!canEditFolder(folder)) {

      uni.showToast({ title: '仅创建者可重命名', icon: 'none' })

      return

    }

    uni.showModal({

      title: '重命名文件夹',

      editable: true,

      placeholderText: '输入新名称',

      content: folder.name,

      success: async (res) => {

        if (res.confirm && res.content?.trim()) {

          try {

            await storageApi.renameFolder(folder.id, res.content.trim())

            uni.showToast({ title: '重命名成功', icon: 'success' })

            await refresh()

          } catch (e: any) {

            uni.showToast({ title: e.message || '重命名失败', icon: 'none' })

          }

        }

      },

    })

  }



  async function changeFolderVisibility(folder: StorageFolderNode) {

    if (!canEditFolder(folder)) {

      uni.showToast({ title: '仅创建者可修改可见性', icon: 'none' })

      return

    }

    const picked = await pickStorageVisibilityChange(folder.visibility as StorageVisibility)
    if (!picked) return
    if (picked.visibility === folder.visibility && !picked.familyIds?.length) return

    try {
      await storageApi.updateFolderVisibility(folder.id, picked.visibility, picked.familyIds)
      uni.showToast({ title: '可见性已更新', icon: 'success' })
      await refresh()
    } catch (e: any) {
      uni.showToast({ title: e.message || '修改失败', icon: 'none' })
    }
  }



  async function deleteFolder(folder: StorageFolderNode) {

    if (!canDeleteFolder(folder)) {

      uni.showToast({ title: '仅创建者可删除', icon: 'none' })

      return

    }

    uni.showModal({

      title: '删除文件夹',

      content: `确定删除「${folder.name}」？文件夹内所有文件将一并删除。`,

      success: async (res) => {

        if (res.confirm) {

          try {

            await storageApi.deleteFolder(folder.id)

            uni.showToast({ title: '已删除', icon: 'success' })

            await refresh()

          } catch (e: any) {

            uni.showToast({ title: e.message || '删除失败', icon: 'none' })

          }

        }

      },

    })

  }



  async function renameFile(file: any) {

    if (!canDeleteFile(file)) {

      uni.showToast({ title: '仅上传者可重命名', icon: 'none' })

      return

    }

    uni.showModal({

      title: '重命名文件',

      editable: true,

      placeholderText: '输入新名称',

      content: getStorageDisplayName(file),

      success: async (res) => {

        if (res.confirm && res.content?.trim()) {

          try {

            await storageApi.renameFile(file.id, res.content.trim())

            uni.showToast({ title: '重命名成功', icon: 'success' })

            await refresh()

          } catch (e: any) {

            uni.showToast({ title: e.message || '重命名失败', icon: 'none' })

          }

        }

      },

    })

  }



  async function deleteFile(file: any) {

    if (!canDeleteFile(file)) {

      uni.showToast({ title: '仅上传者可删除', icon: 'none' })

      return

    }

    uni.showModal({

      title: '删除文件',

      content: `确定删除「${getStorageDisplayName(file)}」？`,

      success: async (res) => {

        if (res.confirm) {

          try {

            await storageApi.deleteFile(file.id)

            uni.showToast({ title: '已删除', icon: 'success' })

            await refresh()

          } catch (e: any) {

            uni.showToast({ title: e.message || '删除失败', icon: 'none' })

          }

        }

      },

    })

  }



  async function uploadFiles() {

    const fid = getFolderId()
    let visibility: StorageVisibility = (currentFolder.value?.visibility as StorageVisibility) || 'private'
    let familyIds: string[] | undefined = currentFolder.value?.familyIds

    if (!fid) {
      const picked = await pickStorageVisibility('private')
      if (!picked) return
      visibility = picked.visibility
      familyIds = picked.familyIds
    }

    showStorageUploadMenu(async (filePath, fileName) => {
      uni.showLoading({ title: '上传中...' })
      try {
        await uploadStorageFile(filePath, {
          folderId: fid || undefined,
          visibility,
          familyIds,
          fileName,
        })

        uni.hideLoading()

        uni.showToast({ title: `上传成功：${fileName || '文件'}`, icon: 'success' })

        await refresh()

      } catch (e: any) {

        uni.hideLoading()

        uni.showToast({ title: e.message || '上传失败', icon: 'none' })

      }

    })

  }



  function showFolderActions(folder: StorageFolderNode) {

    const actions: { name: string; key: string; color?: string }[] = [

      { name: '进入文件夹', key: 'enter' },

    ]

    if (canEditFolder(folder)) {

      actions.push({ name: '重命名', key: 'rename' })

      actions.push({ name: '修改可见性', key: 'visibility' })

    }

    if (canDeleteFolder(folder)) {

      actions.push({ name: '删除文件夹', key: 'delete', color: '#e74c3c' })

    }

    uni.showActionSheet({

      itemList: actions.map((a) => a.name),

      success: (res) => {

        const action = actions[res.tapIndex]

        if (action.key === 'enter') enterFolder(folder)

        else if (action.key === 'rename') renameFolder(folder)

        else if (action.key === 'visibility') changeFolderVisibility(folder)

        else if (action.key === 'delete') deleteFolder(folder)

      },

    })

  }



  function showFileActions(file: any) {

    const actions: { name: string; key: string; color?: string }[] = [

      { name: '预览', key: 'preview' },

      { name: '下载', key: 'download' },

      { name: '收藏/取消', key: 'favorite' },

    ]

    if (canDeleteFile(file)) {

      actions.push({ name: '重命名', key: 'rename' })

      actions.push({ name: '删除', key: 'delete', color: '#e74c3c' })

    }

    uni.showActionSheet({

      itemList: actions.map((a) => a.name),

      success: async (res) => {

        const action = actions[res.tapIndex]

        if (action.key === 'preview') openFile(file)

        else if (action.key === 'download') downloadFile(file)

        else if (action.key === 'favorite') {

          await storageApi.toggleFavorite(file.id)

          await refresh()

        } else if (action.key === 'rename') renameFile(file)

        else if (action.key === 'delete') deleteFile(file)

      },

    })

  }



  return {

    folderTree,

    files,

    loading,

    currentFolder,

    subFolders,

    breadcrumbs,

    canCreateFolder,

    refresh,

    enterFolder,

    onBreadcrumb,

    openFile,

    createFolder,

    renameFolder,

    changeFolderVisibility,

    deleteFolder,

    renameFile,

    deleteFile,

    uploadFiles,

    showFolderActions,

    showFileActions,

    canDeleteFolder,

    canDeleteFile,

    canEditFolder,

  }

}

