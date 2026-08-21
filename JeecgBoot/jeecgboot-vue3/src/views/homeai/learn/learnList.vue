<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="学习资料列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, learnApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" @click="handleExportXls('学习资料列表', learnApi.exportXls)">导出</a-button>
        <a-button v-if="activeTab === 'list' && selectedRowKeys.length > 0" preIcon="ant-design:delete-outlined" type="primary" danger @click="handleBatchMoveToRecycleBin">
          移入回收站({{ selectedRowKeys.length }})
        </a-button>
        <a-button v-if="activeTab === 'recycle' && selectedRowKeys.length > 0" preIcon="ant-design:undo-outlined" @click="handleBatchRestore">
          恢复({{ selectedRowKeys.length }})
        </a-button>
        <a-button v-if="activeTab === 'recycle' && selectedRowKeys.length > 0" preIcon="ant-design:delete-outlined" type="primary" danger @click="handleBatchDeletePermanently">
          彻底删除({{ selectedRowKeys.length }})
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'userId' || column.dataIndex === 'userId'">
          {{ resolveUserLabel(record.userId) }}
        </template>
      </template>
    </BasicTable>
    <LearnDrawer @register="registerDrawer" @success="handleSuccess" />
    <HomeaiFilePreviewModal ref="previewModalRef" />
  </PageWrapper>
</template>

<script lang="ts" name="homeai-learn-list" setup>
  import { PageWrapper } from '/@/components/Page';
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { learnApi } from '/@/api/homeai';
  import type { HomeaiCategory, HomeaiLearnMaterial, HomeaiPageParams } from '/@/api/homeai';
  import LearnDrawer from './LearnDrawer.vue';
  import HomeaiFilePreviewModal from '../components/HomeaiFilePreviewModal.vue';
  import { useUserLabel } from '../hooks/useUserLabel';
  import { useHomeaiRecycleBin } from '../hooks/useHomeaiRecycleBin';
  import { downloadCsvTemplate } from '../utils/csvTemplate';

  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const { userOptions, loadUserOptions, resolveUserLabel } = useUserLabel();
  const activeTab = ref('list');
  const categoryOptions = ref<{ label: string; value: string }[]>([]);

  async function loadCategoryOptions() {
    try {
      const list: HomeaiCategory[] = (await learnApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
    } catch {
      categoryOptions.value = [];
    }
  }

  onMounted(async () => {
    await Promise.all([loadCategoryOptions(), loadUserOptions()]);
  });

  const columns = [
    { title: '标题', dataIndex: 'title', width: 200 },
    { title: '分类', dataIndex: 'category', width: 80 },
    { title: '类型', dataIndex: 'type', width: 70 },
    { title: '标签', dataIndex: 'tags', width: 150 },
    { title: '用户', dataIndex: 'userId', key: 'userId', width: 160 },
    { title: '创建时间', dataIndex: 'createTime', width: 160 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '学习资料管理',
    api: (params: HomeaiPageParams) => activeTab.value === 'list' ? learnApi.list(params) : learnApi.recycleBin(params),
    columns: columns,
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: { width: 160, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    formConfig: {
      labelWidth: 80,
      schemas: [
        { field: 'title', label: '标题', component: 'Input', colProps: { span: 8 } },
        { field: 'categoryId', label: '分类', component: 'Select', colProps: { span: 8 }, componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' } },
        { field: 'userId', label: '用户', component: 'Select', colProps: { span: 8 }, componentProps: { options: userOptions, allowClear: true, showSearch: true, optionFilterProp: 'label', placeholder: '请选择用户' } },
      ],
    },
  });

  const { rowSelection, selectedRowKeys, clearSelection, handleMoveToRecycleBin, handleBatchMoveToRecycleBin, handleRestore, handleBatchRestore, handleDeletePermanently, handleBatchDeletePermanently } = useHomeaiRecycleBin({
    api: {
      moveToRecycleBin: (ids: string[]) => learnApi.moveToRecycleBin(ids),
      restore: (ids: string[]) => learnApi.restore(ids),
      deletePermanently: (ids: string[]) => learnApi.deletePermanently(ids),
    },
    reload,
    entityName: '资料',
    nameField: 'title',
    permanentWarn: '此操作不可恢复！',
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    clearSelection();
    reload();
  }

  function getTableAction(record: HomeaiLearnMaterial) {
    if (activeTab.value === 'recycle') {
      return [
        { icon: 'ant-design:undo-outlined', onClick: () => handleRestore(record), title: '恢复' },
        { icon: 'ant-design:delete-outlined', onClick: () => handleDeletePermanently(record), title: '彻底删除', color: 'error' },
      ];
    }
    return [
      { icon: 'ant-design:eye-outlined', onClick: () => openPreview(record), title: '预览' },
      { icon: 'ant-design:edit-outlined', onClick: () => openDrawer(true, { record, isUpdate: true }), title: '编辑' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleMoveToRecycleBin(record), title: '移入回收站', color: 'error' },
    ];
  }

  const previewModalRef = ref<{ open: (src: { module: 'learn'; id: string; title?: string }) => void } | null>(null);
  function openPreview(record: HomeaiLearnMaterial) {
    if (!record.id) return;
    previewModalRef.value?.open({ module: 'learn', id: record.id, title: record.title });
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: false, record: {} });
  }

  function handleDownloadTemplate() {
    downloadCsvTemplate(['标题', '类型(video/audio/image/pdf/doc/xls/ppt/link/note)', '分类', '标签', '文件URL'], '学习资料导入模板.csv');
  }

  function handleSuccess() {
    reload();
  }
</script>
