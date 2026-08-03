<template>
  <div style="padding: 16px">
    <div class="storage-toolbar">
      <a-radio-group v-model:value="viewMode" button-style="solid" style="margin-right: 8px">
        <a-radio-button value="list">
          <Icon icon="ant-design:unordered-list-outlined" /> 文件列表
        </a-radio-button>
        <a-radio-button value="folder">
          <Icon icon="ant-design:folder-outlined" /> 文件夹视图
        </a-radio-button>
      </a-radio-group>
      <a-button type="primary" preIcon="ant-design:folder-add-outlined" @click="openCreateFolderModal">新增文件夹</a-button>
      <a-button type="primary" preIcon="ant-design:upload-outlined" @click="handleUploadModal" style="margin-left: 8px">上传文件</a-button>
    </div>

    <!-- 文件列表视图 -->
    <div v-if="viewMode === 'list'">
      <BasicTable @register="registerTable">
        <template #action="{ record }">
          <TableAction :actions="getTableAction(record)" />
        </template>
      </BasicTable>
    </div>

    <!-- 文件夹视图 -->
    <div v-if="viewMode === 'folder'" class="folder-view">
      <a-card title="文件夹" :bordered="false" class="folder-card">
        <template #extra>
          <a-button type="link" @click="loadFolderTree" preIcon="ant-design:reload-outlined">刷新</a-button>
        </template>
        <a-tree
          v-if="folderTreeData.length > 0"
          :tree-data="folderTreeData"
          :field-names="{ children: 'children', title: 'name', key: 'id' }"
          default-expand-all
          @select="onFolderSelect"
        >
          <template #title="{ name, fileCount }">
            <span style="display:flex;align-items:center;gap:8px">
              <Icon icon="ant-design:folder-outlined" :style="{color:'#faad14'}" />
              <span>{{ name }}</span>
              <a-tag v-if="fileCount !== undefined" color="blue" size="small">{{ fileCount }}个文件</a-tag>
            </span>
          </template>
        </a-tree>
        <a-empty v-else description="暂无文件夹" />
      </a-card>

      <!-- 选中文件夹的文件列表 -->
      <a-card v-if="selectedFolderId" title="文件夹内文件" :bordered="false" style="margin-top: 16px">
        <a-table
          :columns="folderFileColumns"
          :data-source="folderFiles"
          :pagination="false"
          size="small"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'fileSize'">
              {{ formatFileSize(record.fileSize) }}
            </template>
          </template>
        </a-table>
        <a-empty v-if="folderFiles.length === 0" description="该文件夹暂无文件" />
      </a-card>
    </div>

    <!-- 上传文件模态框 -->
    <BasicModal @register="registerUploadModal" title="上传文件" @ok="handleUpload" width="500px">
      <div class="upload-form">
        <a-form-item label="选择文件" required>
          <input type="file" ref="fileInputRef" @change="onFileChange" />
        </a-form-item>
        <a-form-item label="所属文件夹">
          <a-select v-model:value="uploadForm.folderId" placeholder="请选择文件夹（可选）" allowClear style="width:100%">
            <a-select-option v-for="f in folderList" :key="f.id" :value="f.id">{{ f.name }}</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="可见性">
          <a-select v-model:value="uploadForm.visibility" style="width:100%">
            <a-select-option value="private">私有</a-select-option>
            <a-select-option value="family">家庭可见</a-select-option>
            <a-select-option value="public">公开</a-select-option>
          </a-select>
        </a-form-item>
      </div>
    </BasicModal>

    <!-- 新增文件夹模态框 -->
    <BasicModal @register="registerFolderModal" title="新增文件夹" @ok="handleCreateFolder" width="400px">
      <a-form-item label="文件夹名称" required>
        <a-input v-model:value="folderForm.name" placeholder="请输入文件夹名称" />
      </a-form-item>
      <a-form-item label="上级文件夹">
        <a-select v-model:value="folderForm.parentId" placeholder="根目录（可选）" allowClear style="width:100%">
          <a-select-option v-for="f in folderList" :key="f.id" :value="f.id">{{ f.name }}</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="可见性">
        <a-select v-model:value="folderForm.visibility" style="width:100%">
          <a-select-option value="private">私有</a-select-option>
          <a-select-option value="family">家庭可见</a-select-option>
        </a-select>
      </a-form-item>
    </BasicModal>
  </div>
</template>

<script lang="ts" name="homeai-storage-file" setup>
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Icon } from '/@/components/Icon';

  const { createMessage, createConfirm } = useMessage();
  const [registerUploadModal, { openModal: openUploadModal }] = useModal();
  const [registerFolderModal, { openModal: openFolderModal }] = useModal();
  const fileInputRef = ref<HTMLInputElement | null>(null);
  const selectedFile = ref<File | null>(null);
  const folderList = ref<any[]>([]);
  const folderTreeData = ref<any[]>([]);
  const selectedFolderId = ref<string | null>(null);
  const folderFiles = ref<any[]>([]);
  const viewMode = ref<'list' | 'folder'>('list');

  const uploadForm = ref({
    folderId: undefined as string | undefined,
    visibility: 'private' as string,
  });

  const folderForm = ref({
    name: '',
    parentId: undefined as string | undefined,
    visibility: 'private' as string,
  });

  const folderFileColumns = [
    { title: '文件名', dataIndex: 'originalName', key: 'name' },
    { title: '扩展名', dataIndex: 'extension', width: 80 },
    { title: '文件大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
    { title: '上传时间', dataIndex: 'createTime', width: 160 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '资料存储管理',
    api: (params: any) => defHttp.get({ url: '/homeai/storage/file-list', params }),
    columns: [
      { title: '文件名', dataIndex: 'originalName', width: 250 },
      { title: '扩展名', dataIndex: 'extension', width: 80 },
      { title: '文件大小', dataIndex: 'fileSize', width: 100 },
      { title: '上传者', dataIndex: 'userId', width: 150 },
      { title: '可见性', dataIndex: 'visibility', width: 80 },
      { title: '下载次数', dataIndex: 'downloadCount', width: 80 },
      { title: '上传时间', dataIndex: 'createTime', width: 160 },
    ],
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: { width: 120, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    formConfig: {
      schemas: [
        { field: 'originalName', label: '文件名', component: 'Input' },
        { field: 'extension', label: '扩展名', component: 'Input' },
      ],
    },
  });

  function getTableAction(record: any) {
    return [
      { icon: 'ant-design:download-outlined', onClick: () => handleDownload(record), title: '下载' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleDeleteFile(record), title: '删除', color: 'error' },
    ];
  }

  async function loadFolderList() {
    try {
      const res = await defHttp.get({ url: '/homeai/storage/folder-list' });
      folderList.value = (res as any)?.records || (res as any[]) || [];
    } catch {
      folderList.value = [];
    }
  }

  async function loadFolderTree() {
    try {
      const res = await defHttp.get({ url: '/homeai/storage/folders' });
      folderTreeData.value = (res as any)?.result || (res as any[]) || [];
    } catch {
      folderTreeData.value = [];
    }
  }

  async function onFolderSelect(selectedKeys: string[]) {
    if (selectedKeys.length > 0) {
      selectedFolderId.value = selectedKeys[0];
      try {
        const res = await defHttp.get({ url: `/homeai/storage/folders/${selectedFolderId.value}/files` });
        folderFiles.value = (res as any)?.result || (res as any[]) || [];
      } catch {
        folderFiles.value = [];
      }
    }
  }

  function formatFileSize(bytes: number): string {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) {
      size /= 1024;
      i++;
    }
    return size.toFixed(1) + ' ' + units[i];
  }

  function openCreateFolderModal() {
    folderForm.value = { name: '', parentId: undefined, visibility: 'private' };
    loadFolderList();
    openFolderModal(true);
  }

  async function handleCreateFolder() {
    if (!folderForm.value.name.trim()) {
      createMessage.warning('请输入文件夹名称');
      return;
    }
    const params: any = { name: folderForm.value.name.trim(), visibility: folderForm.value.visibility };
    if (folderForm.value.parentId) {
      params.parentId = folderForm.value.parentId;
    }
    await defHttp.post({ url: '/homeai/storage/folders', params }, { joinParamsToUrl: true });
    createMessage.success('文件夹创建成功');
    if (viewMode.value === 'folder') {
      loadFolderTree();
    }
  }

  async function handleUploadModal() {
    uploadForm.value = { folderId: undefined, visibility: 'private' };
    selectedFile.value = null;
    if (fileInputRef.value) {
      fileInputRef.value.value = '';
    }
    loadFolderList();
    openUploadModal(true);
  }

  function onFileChange(e: Event) {
    const target = e.target as HTMLInputElement;
    if (target.files && target.files.length > 0) {
      selectedFile.value = target.files[0];
    }
  }

  async function handleUpload() {
    if (!selectedFile.value) {
      createMessage.warning('请选择文件');
      return;
    }
    // 使用 defHttp.uploadFile 上传（内部正确构造 multipart/form-data，
    // 避免 defHttp.post 的 beforeRequestHook 将 FormData 改写为 JSON 的问题）
    const extra: any = { visibility: uploadForm.value.visibility };
    if (uploadForm.value.folderId) {
      extra.folderId = uploadForm.value.folderId;
    }
    await defHttp.uploadFile(
      { url: '/homeai/storage/files/upload' },
      { file: selectedFile.value, name: 'file', data: extra },
      {
        success: () => {
          createMessage.success('上传成功');
          reload();
          if (viewMode.value === 'folder') {
            loadFolderTree();
          }
        },
      }
    );
  }

  function handleDownload(record: any) {
    // 直接使用文件的绝对访问地址下载，绕过需要鉴权的接口
    if (record.fileUrl) {
      window.open(record.fileUrl, '_blank');
    }
  }

  async function handleDeleteFile(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除文件「${record.originalName}」吗？`,
      onOk: async () => {
        await defHttp.delete({ url: `/homeai/storage/files/${record.id}` });
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  onMounted(() => {
    loadFolderTree();
  });
</script>

<style scoped lang="less">
  .storage-toolbar {
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    padding: 12px;
    background: #fafafa;
    border-radius: 4px;
  }

  .folder-view {
    .folder-card {
      margin-bottom: 0;
    }
  }

  .upload-form {
    padding: 16px 0;
  }
</style>
