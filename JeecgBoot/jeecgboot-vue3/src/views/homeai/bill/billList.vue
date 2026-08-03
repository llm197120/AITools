<template>
  <div style="padding: 16px">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="账单列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, billApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined" type="primary">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" type="primary" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" type="primary" @click="handleExportXls('账单列表', billApi.exportXls)">导出</a-button>
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
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === 'income' ? 'green' : 'red'">{{ record.type === 'income' ? '收入' : '支出' }}</a-tag>
        </template>
        <template v-if="column.key === 'amount'">
          <span :style="{color: record.type==='income'?'#27ae60':'#e74c3c'}">¥{{ record.amount }}</span>
        </template>
      </template>
    </BasicTable>
    <BillDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="homeai-bill-list" setup>
  import { computed, ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { billApi } from '/@/api/homeai';
  import BillDrawer from './BillDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const selectedRowKeys = ref<string[]>([]);
  const activeTab = ref('list');

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const columns = [
    { title: '日期', dataIndex: 'billDate', width: 120 },
    { title: '用户', dataIndex: 'userId', width: 160 },
    { title: '类型', dataIndex: 'type', key: 'type', width: 70 },
    { title: '分类', dataIndex: 'categoryId', width: 100 },
    { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
    { title: '支付方式', dataIndex: 'paymentMethod', width: 90 },
    { title: '备注', dataIndex: 'remark', width: 200 },
    { title: '来源', dataIndex: 'source', width: 90 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '账单管理',
    api: (params: any) => activeTab.value === 'list' ? billApi.list(params) : billApi.recycleBin(params),
    columns: columns,
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: { width: 120, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    formConfig: {
      labelWidth: 80,
      schemas: [
        { field: 'billDate', label: '日期', component: 'DatePicker', colProps: { span: 8 } },
        { field: 'type', label: '类型', component: 'Select', colProps: { span: 8 }, componentProps: { options: [{label:'收入',value:'income'},{label:'支出',value:'expense'}] } },
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
    const headers = ['日期', '用户', '类型(income/expense)', '分类ID', '金额', '支付方式', '备注', '来源'];
    const blob = new Blob(['\uFEFF' + headers.join(',') + '\n'], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = '账单导入模板.csv';
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
      content: '确定将该账单移入回收站吗？',
      onOk: async () => {
        await billApi.moveToRecycleBin([record.id]);
        createMessage.success('已移入回收站');
        reload();
      },
    });
  }

  async function handleBatchMoveToRecycleBin() {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将选中的 ${selectedRowKeys.value.length} 条账单移入回收站吗？`,
      onOk: async () => {
        await billApi.moveToRecycleBin(selectedRowKeys.value);
        createMessage.success('已移入回收站');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  async function handleRestore(record: any) {
    await billApi.restore([record.id]);
    createMessage.success('恢复成功');
    reload();
  }

  async function handleBatchRestore() {
    await billApi.restore(selectedRowKeys.value);
    createMessage.success('恢复成功');
    selectedRowKeys.value = [];
    reload();
  }

  async function handleDeletePermanently(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: '确定彻底删除该账单吗？此操作不可恢复！',
      onOk: async () => {
        await billApi.deletePermanently([record.id]);
        createMessage.success('已彻底删除');
        reload();
      },
    });
  }

  async function handleBatchDeletePermanently() {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除选中的 ${selectedRowKeys.value.length} 条账单吗？此操作不可恢复！`,
      onOk: async () => {
        await billApi.deletePermanently(selectedRowKeys.value);
        createMessage.success('已彻底删除');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }
</script>
