/**
 * 资料存储 — 文件夹树与文件夹内文件列表逻辑
 * 从 fileList.vue 抽出，降低主页面体积
 */
import { ref } from 'vue';
import { defHttp } from '/@/utils/http/axios';
import { useMessage } from '/@/hooks/web/useMessage';
import type { HomeaiStorageFile, HomeaiStorageFolder } from '/@/api/homeai/types';

export function useFileFolderTree() {
  const { createMessage } = useMessage();

  const folderTreeData = ref<HomeaiStorageFolder[]>([]);
  const selectedFolderId = ref<string | null>(null);
  const folderFiles = ref<HomeaiStorageFile[]>([]);

  /** 加载文件夹树 */
  async function loadFolderTree() {
    try {
      const res = await defHttp.get({ url: '/homeai/storage/folders' });
      folderTreeData.value = ((res as { result?: HomeaiStorageFolder[] })?.result ||
        (res as HomeaiStorageFolder[]) ||
        []) as HomeaiStorageFolder[];
    } catch {
      folderTreeData.value = [];
    }
  }

  /** 选中文件夹并加载其下文件 */
  async function onFolderSelect(selectedKeys: string[]) {
    if (selectedKeys.length > 0) {
      selectedFolderId.value = selectedKeys[0];
      try {
        const res = await defHttp.get({
          url: `/homeai/storage/folders/${selectedFolderId.value}/files`,
        });
        folderFiles.value = ((res as { result?: HomeaiStorageFile[] })?.result ||
          (res as HomeaiStorageFile[]) ||
          []) as HomeaiStorageFile[];
      } catch {
        folderFiles.value = [];
      }
    }
  }

  /** 刷新当前选中文件夹内的文件（若有选中） */
  async function reloadSelectedFolderFiles() {
    if (selectedFolderId.value) {
      await onFolderSelect([selectedFolderId.value]);
    }
  }

  /** 删除文件夹后清理选中态并刷新树 */
  async function handleDeleteFolder(id: string) {
    try {
      await defHttp.delete({ url: `/homeai/storage/folders/${id}` });
      createMessage.success('文件夹已删除');
      if (selectedFolderId.value === id) {
        selectedFolderId.value = null;
        folderFiles.value = [];
      }
      await loadFolderTree();
    } catch (e: unknown) {
      const err = e as { message?: string };
      createMessage.error(err?.message || '删除失败');
    }
  }

  /** 清空选中态 */
  function clearFolderSelection() {
    selectedFolderId.value = null;
    folderFiles.value = [];
  }

  return {
    folderTreeData,
    selectedFolderId,
    folderFiles,
    loadFolderTree,
    onFolderSelect,
    reloadSelectedFolderFiles,
    handleDeleteFolder,
    clearFolderSelection,
  };
}
