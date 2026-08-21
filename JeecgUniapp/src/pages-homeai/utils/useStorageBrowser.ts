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

export type StorageSheetAction = { name: string; key: string; color?: string }

export function useStorageBrowser(getFolderId: () => string | null, getUserId: () => string | undefined) {
  const folderTree = ref<StorageFolderNode[]>([])
  const files = ref<any[]>([])
  const loading = ref(false)
  const currentFolder = ref<StorageFolderNode | null>(null)
  const PAGE_SIZE = 20
  const pageNo = ref(1)
  const hasMore = ref(true)
  const loadingMore = ref(false)

  const subFolders = computed(() => getChildFolders(folderTree.value, getFolderId()))
  const breadcrumbs = computed(() => buildBreadcrumb(folderTree.value, getFolderId()))
  const canCreateFolder = computed(() => canCreateSubFolder(currentFolder.value))

  const folderSheetVisible = ref(false)
  const folderSheetActions = ref<StorageSheetAction[]>([])
  const folderSheetTarget = ref<StorageFolderNode | null>(null)

  const fileSheetVisible = ref(false)
  const fileSheetActions = ref<StorageSheetAction[]>([])
  const fileSheetTarget = ref<any>(null)

  const namePopupVisible = ref(false)
  const namePopupTitle = ref('')
  const namePopupPlaceholder = ref('')
  const namePopupValue = ref('')
  const namePopupKind = ref<'createFolder' | 'renameFolder' | 'renameFile'>('createFolder')
  const namePopupParentId = ref<string | null>(null)
  const namePopupFolder = ref<StorageFolderNode | null>(null)
  const namePopupFile = ref<any>(null)

  const confirmVisible = ref(false)
  const confirmTitle = ref('')
  const confirmMessage = ref('')
  const confirmKind = ref<'deleteFolder' | 'deleteFile'>('deleteFolder')
  const confirmFolder = ref<StorageFolderNode | null>(null)
  const confirmFile = ref<any>(null)

  function unwrapFilePage(page: any): { records: any[]; total?: number } {
    if (!page) return { records: [] }
    if (Array.isArray(page)) return { records: page }
    const records = page.records || page.list || []
    return { records: Array.isArray(records) ? records : [], total: page.total }
  }

  async function fetchFiles(reset = false) {
    if (loadingMore.value) return
    if (!reset && !hasMore.value) return
    loadingMore.value = true
    try {
      const nextPage = reset ? 1 : pageNo.value + 1
      const fid = getFolderId()
      const page = fid
        ? await storageApi.folderFiles(fid, nextPage, PAGE_SIZE)
        : await storageApi.rootFiles(nextPage, PAGE_SIZE)
      const { records, total } = unwrapFilePage(page)
      const normalized = normalizeStorageFiles(records)
      files.value = reset ? normalized : files.value.concat(normalized)
      pageNo.value = nextPage
      if (typeof total === 'number') {
        hasMore.value = files.value.length < total
      } else {
        hasMore.value = normalized.length >= PAGE_SIZE
      }
    } finally {
      loadingMore.value = false
    }
  }

  async function refresh() {
    loading.value = true
    try {
      folderTree.value = (await storageApi.folders()) || []
      const fid = getFolderId()
      currentFolder.value = fid ? findFolderNode(folderTree.value, fid) : null
      pageNo.value = 1
      hasMore.value = true
      await fetchFiles(true)
    } finally {
      loading.value = false
    }
  }

  async function loadMore() {
    await fetchFiles(false)
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

  function openCreateFolder(parentId: string | null) {
    if (!canCreateFolder.value) {
      uni.showToast({ title: '已达最大文件夹层级(5级)', icon: 'none' })
      return
    }
    namePopupKind.value = 'createFolder'
    namePopupParentId.value = parentId
    namePopupTitle.value = '新建文件夹'
    namePopupPlaceholder.value = '输入文件夹名称'
    namePopupValue.value = ''
    namePopupVisible.value = true
  }

  function openRenameFolder(folder: StorageFolderNode) {
    if (!canEditFolder(folder)) {
      uni.showToast({ title: '仅创建者可重命名', icon: 'none' })
      return
    }
    namePopupKind.value = 'renameFolder'
    namePopupFolder.value = folder
    namePopupTitle.value = '重命名文件夹'
    namePopupPlaceholder.value = '输入新名称'
    namePopupValue.value = folder.name
    namePopupVisible.value = true
  }

  function openRenameFile(file: any) {
    if (!canDeleteFile(file)) {
      uni.showToast({ title: '仅上传者可重命名', icon: 'none' })
      return
    }
    namePopupKind.value = 'renameFile'
    namePopupFile.value = file
    namePopupTitle.value = '重命名文件'
    namePopupPlaceholder.value = '输入新名称'
    namePopupValue.value = getStorageDisplayName(file)
    namePopupVisible.value = true
  }

  async function submitNamePopup() {
    const name = namePopupValue.value.trim()
    if (!name) {
      uni.showToast({ title: '请输入名称', icon: 'none' })
      return
    }
    namePopupVisible.value = false
    try {
      if (namePopupKind.value === 'createFolder') {
        const parentId = namePopupParentId.value
        let visibility: StorageVisibility =
          (currentFolder.value?.visibility as StorageVisibility) || 'private'
        let familyIds: string[] | undefined = currentFolder.value?.familyIds
        if (!parentId) {
          const picked = await pickStorageVisibility('private')
          if (!picked) return
          visibility = picked.visibility
          familyIds = picked.familyIds
        }
        await storageApi.createFolder(name, parentId || undefined, visibility, familyIds)
        uni.showToast({ title: '创建成功', icon: 'success' })
      } else if (namePopupKind.value === 'renameFolder' && namePopupFolder.value) {
        await storageApi.renameFolder(namePopupFolder.value.id, name)
        uni.showToast({ title: '重命名成功', icon: 'success' })
      } else if (namePopupKind.value === 'renameFile' && namePopupFile.value) {
        await storageApi.renameFile(namePopupFile.value.id, name)
        uni.showToast({ title: '重命名成功', icon: 'success' })
      }
      await refresh()
    } catch (e: any) {
      uni.showToast({ title: e.message || '操作失败', icon: 'none' })
    }
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

  function openDeleteFolder(folder: StorageFolderNode) {
    if (!canDeleteFolder(folder)) {
      uni.showToast({ title: '仅创建者可删除', icon: 'none' })
      return
    }
    confirmKind.value = 'deleteFolder'
    confirmFolder.value = folder
    confirmTitle.value = '删除文件夹'
    confirmMessage.value = `确定删除「${folder.name}」？文件夹内所有文件将一并删除。`
    confirmVisible.value = true
  }

  function openDeleteFile(file: any) {
    if (!canDeleteFile(file)) {
      uni.showToast({ title: '仅上传者可删除', icon: 'none' })
      return
    }
    confirmKind.value = 'deleteFile'
    confirmFile.value = file
    confirmTitle.value = '删除文件'
    confirmMessage.value = `确定删除「${getStorageDisplayName(file)}」？`
    confirmVisible.value = true
  }

  async function submitConfirm() {
    confirmVisible.value = false
    try {
      if (confirmKind.value === 'deleteFolder' && confirmFolder.value) {
        await storageApi.deleteFolder(confirmFolder.value.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      } else if (confirmKind.value === 'deleteFile' && confirmFile.value) {
        await storageApi.deleteFile(confirmFile.value.id)
        uni.showToast({ title: '已删除', icon: 'success' })
      }
      await refresh()
    } catch (e: any) {
      uni.showToast({ title: e.message || '删除失败', icon: 'none' })
    }
  }

  let pendingUploads = 0

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
      pendingUploads++
      uni.showLoading({ title: '上传中...' })
      try {
        await uploadStorageFile(filePath, {
          folderId: fid || undefined,
          visibility,
          familyIds,
          fileName,
        })
        uni.showToast({ title: `上传成功：${fileName || '文件'}`, icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e.message || '上传失败', icon: 'none' })
      } finally {
        pendingUploads--
        if (pendingUploads === 0) {
          uni.hideLoading()
          await refresh()
        }
      }
    })
  }

  function showFolderActions(folder: StorageFolderNode) {
    const actions: StorageSheetAction[] = [{ name: '进入文件夹', key: 'enter' }]
    if (canEditFolder(folder)) {
      actions.push({ name: '重命名', key: 'rename' })
      actions.push({ name: '修改可见性', key: 'visibility' })
    }
    if (canDeleteFolder(folder)) {
      actions.push({ name: '删除文件夹', key: 'delete', color: '#e74c3c' })
    }
    folderSheetTarget.value = folder
    folderSheetActions.value = actions
    folderSheetVisible.value = true
  }

  function onFolderSheetSelect({ index }: { index: number }) {
    const folder = folderSheetTarget.value
    const action = folderSheetActions.value[index]
    if (!folder || !action) return
    if (action.key === 'enter') enterFolder(folder)
    else if (action.key === 'rename') openRenameFolder(folder)
    else if (action.key === 'visibility') changeFolderVisibility(folder)
    else if (action.key === 'delete') openDeleteFolder(folder)
  }

  function showFileActions(file: any) {
    const actions: StorageSheetAction[] = [
      { name: '预览', key: 'preview' },
      { name: '下载', key: 'download' },
      { name: '收藏/取消', key: 'favorite' },
    ]
    if (canDeleteFile(file)) {
      actions.push({ name: '重命名', key: 'rename' })
      actions.push({ name: '删除', key: 'delete', color: '#e74c3c' })
    }
    fileSheetTarget.value = file
    fileSheetActions.value = actions
    fileSheetVisible.value = true
  }

  async function onFileSheetSelect({ index }: { index: number }) {
    const file = fileSheetTarget.value
    const action = fileSheetActions.value[index]
    if (!file || !action) return
    if (action.key === 'preview') openFile(file)
    else if (action.key === 'download') downloadFile(file)
    else if (action.key === 'favorite') {
      await storageApi.toggleFavorite(file.id)
      await refresh()
    } else if (action.key === 'rename') openRenameFile(file)
    else if (action.key === 'delete') openDeleteFile(file)
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
    loadMore,
    hasMore,
    loadingMore,
    enterFolder,
    onBreadcrumb,
    openFile,
    createFolder: openCreateFolder,
    renameFolder: openRenameFolder,
    changeFolderVisibility,
    deleteFolder: openDeleteFolder,
    renameFile: openRenameFile,
    deleteFile: openDeleteFile,
    uploadFiles,
    showFolderActions,
    showFileActions,
    canDeleteFolder,
    canDeleteFile,
    canEditFolder,
    folderSheetVisible,
    folderSheetActions,
    onFolderSheetSelect,
    fileSheetVisible,
    fileSheetActions,
    onFileSheetSelect,
    namePopupVisible,
    namePopupTitle,
    namePopupPlaceholder,
    namePopupValue,
    submitNamePopup,
    confirmVisible,
    confirmTitle,
    confirmMessage,
    submitConfirm,
  }
}
