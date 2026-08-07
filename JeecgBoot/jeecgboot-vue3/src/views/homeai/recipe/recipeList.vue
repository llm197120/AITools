<template>
  <div style="padding: 16px">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="菜谱列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, recipeApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined" type="primary">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" type="primary" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" type="primary" @click="handleExportXls('菜谱列表', recipeApi.exportXls)">导出</a-button>
        <a-button v-if="activeTab === 'list' && selectedRowKeys.length > 0" preIcon="ant-design:delete-outlined" type="primary" danger @click="handleBatchMoveToRecycleBin">
          移入回收站({{ selectedRowKeys.length }})
        </a-button>
        <a-button v-if="activeTab === 'recycle' && selectedRowKeys.length > 0" preIcon="ant-design:undo-outlined" type="primary" @click="handleBatchRestore">
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
        <template v-if="column.key === 'difficulty'">
          <a-tag :color="difficultyColor(record.difficulty)">{{ difficultyLabel(record.difficulty) }}</a-tag>
        </template>
      </template>
    </BasicTable>
    <RecipeDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="homeai-recipe-list" setup>
  import { computed, ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { recipeApi } from '/@/api/homeai';
  import RecipeDrawer from './RecipeDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const selectedRowKeys = ref<string[]>([]);
  const activeTab = ref('list');

  function difficultyLabel(v: number | string | null | undefined) {
    const n = Number(v);
    if (!Number.isFinite(n) || n < 1) return '-';
    const labels = ['', '入门', '简单', '中等', '较难', '困难'];
    return labels[Math.min(5, Math.max(1, Math.round(n)))] || String(v);
  }

  function difficultyColor(v: number | string | null | undefined) {
    const n = Number(v);
    if (n <= 2) return '#27ae60';
    if (n === 3) return '#f39c12';
    return '#e74c3c';
  }
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const categoryNameMap = ref<Record<string, string>>({});

  async function loadCategoryOptions() {
    try {
      const list: any[] = (await recipeApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
      categoryNameMap.value = Object.fromEntries(list.map((c) => [c.id, c.name]));
    } catch {
      categoryOptions.value = [];
      categoryNameMap.value = {};
    }
  }

  onMounted(() => {
    loadCategoryOptions();
  });

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const columns = [
    { title: '菜名', dataIndex: 'name', width: 160 },
    {
      title: '分类',
      dataIndex: 'categoryId',
      key: 'categoryId',
      width: 100,
      customRender: ({ text }: any) => categoryNameMap.value[text] || text || '-',
    },
    { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 70 },
    { title: '烹饪时间(分)', dataIndex: 'cookTime', width: 90 },
    { title: '浏览数', dataIndex: 'viewCount', width: 70 },
    { title: '用户', dataIndex: 'userId', width: 160 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '菜谱管理',
    api: (params: any) => activeTab.value === 'list' ? recipeApi.list(params) : recipeApi.recycleBin(params),
    columns: columns,
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: { width: 120, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    formConfig: {
      labelWidth: 80,
      schemas: [
        { field: 'name', label: '菜名', component: 'Input', colProps: { span: 8 } },
        { field: 'categoryId', label: '分类', component: 'Select', colProps: { span: 8 }, componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' } },
        { field: 'userId', label: '用户ID', component: 'Input', colProps: { span: 8 } },
      ],
    },
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    selectedRowKeys.value = [];
    reload();
  }

  function getTableAction(record: any) {
    if (activeTab.value === 'recycle') {
      return [
        { icon: 'ant-design:undo-outlined', onClick: () => handleRestore(record), title: '恢复' },
        { icon: 'ant-design:delete-outlined', onClick: () => handleDeletePermanently(record), title: '彻底删除', color: 'error' },
      ];
    }
    return [
      { icon: 'ant-design:edit-outlined', onClick: () => openDrawer(true, { record, isUpdate: true }), title: '编辑' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleMoveToRecycleBin(record), title: '移入回收站', color: 'error' },
    ];
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: false, record: {} });
  }

  function handleDownloadTemplate() {
    const headers = ['菜名', '分类', '难度(1-5)', '烹饪时间(分)', '份数', '小贴士'];
    const blob = new Blob(['\uFEFF' + headers.join(',') + '\n'], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = '菜谱导入模板.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }

  function handleSuccess() {
    reload();
  }

  async function handleMoveToRecycleBin(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将该菜谱「${record.name}」移入回收站吗？`,
      onOk: async () => {
        await recipeApi.moveToRecycleBin([record.id]);
        createMessage.success('已移入回收站');
        reload();
      },
    });
  }

  async function handleBatchMoveToRecycleBin() {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将选中的 ${selectedRowKeys.value.length} 个菜谱移入回收站吗？`,
      onOk: async () => {
        await recipeApi.moveToRecycleBin(selectedRowKeys.value);
        createMessage.success('已移入回收站');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  async function handleRestore(record: any) {
    await recipeApi.restore([record.id]);
    createMessage.success('恢复成功');
    reload();
  }

  async function handleBatchRestore() {
    await recipeApi.restore(selectedRowKeys.value);
    createMessage.success('恢复成功');
    selectedRowKeys.value = [];
    reload();
  }

  async function handleDeletePermanently(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除菜谱「${record.name}」吗？此操作不可恢复！`,
      onOk: async () => {
        await recipeApi.deletePermanently([record.id]);
        createMessage.success('已彻底删除');
        reload();
      },
    });
  }

  async function handleBatchDeletePermanently() {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除选中的 ${selectedRowKeys.value.length} 个菜谱吗？此操作不可恢复！`,
      onOk: async () => {
        await recipeApi.deletePermanently(selectedRowKeys.value);
        createMessage.success('已彻底删除');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }
</script>
