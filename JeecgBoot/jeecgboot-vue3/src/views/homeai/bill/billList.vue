<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="账单列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <a-alert
      v-if="listFailed"
      type="error"
      show-icon
      message="列表加载失败，当前可能不是最新数据"
      style="margin-bottom: 12px"
    >
      <template #action>
        <a-button size="small" @click="reload">重试</a-button>
      </template>
    </a-alert>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, billApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" @click="handleExportXls('账单列表', billApi.exportXls)">导出</a-button>
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
        <template v-if="column.key === 'type'">
          <span :class="record.type === 'income' ? 'hai-text-success' : 'hai-text-danger'">{{
            record.type === 'income' ? '收入' : '支出'
          }}</span>
        </template>
        <template v-if="column.key === 'amount'">
          <span :class="record.type === 'income' ? 'hai-amount-income' : 'hai-amount-expense'">¥{{ record.amount }}</span>
        </template>
        <template v-else-if="column.key === 'userId' || column.dataIndex === 'userId'">
          {{ resolveUserLabel(record.userId) }}
        </template>
      </template>
    </BasicTable>
    <BillDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" name="homeai-bill-list" setup>
  import { PageWrapper } from '/@/components/Page';
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { billApi } from '/@/api/homeai';
  import type { HomeaiBill, HomeaiCategory, HomeaiPageParams } from '/@/api/homeai';
  import { useUserLabel } from '../hooks/useUserLabel';
  import { useHomeaiRecycleBin } from '../hooks/useHomeaiRecycleBin';
  import { useHomeaiListLoad } from '../hooks/useHomeaiListLoad';
  import { downloadCsvTemplate } from '../utils/csvTemplate';
  import BillDrawer from './BillDrawer.vue';

  const { handleExportXls, handleImportXls } = useMethods();
  const { createMessage } = useMessage();
  const { userOptions, loadUserOptions, resolveUserLabel } = useUserLabel();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const activeTab = ref('list');
  const categoryOptions = ref<{ label: string; value: string }[]>([]);

  async function loadCategoryOptions() {
    try {
      const res = await billApi.categoryList({ pageNo: 1, pageSize: 500 });
      const records = Array.isArray(res) ? res : res?.records || [];
      categoryOptions.value = records.map((c: HomeaiCategory) => ({
        label: c.name || c.id || '',
        value: c.id || '',
      }));
    } catch {
      createMessage.warning('分类加载失败');
    }
  }

  const columns = [
    { title: '日期', dataIndex: 'billDate', width: 120 },
    { title: '用户', dataIndex: 'userId', key: 'userId', width: 160 },
    { title: '类型', dataIndex: 'type', key: 'type', width: 70 },
      { title: '分类', dataIndex: 'categoryName', width: 100, customRender: ({ text, record }: any) => text || record?.categoryId || '-' },
    { title: '金额', dataIndex: 'amount', key: 'amount', width: 100 },
    { title: '支付方式', dataIndex: 'paymentMethod', width: 90 },
    { title: '备注', dataIndex: 'remark', width: 200 },
    { title: '来源', dataIndex: 'source', width: 90 },
  ];

  const { listFailed, wrapListApi } = useHomeaiListLoad();

  const [registerTable, { reload }] = useTable({
    title: '账单管理',
    api: wrapListApi((params: HomeaiPageParams) => activeTab.value === 'list' ? billApi.list(params) : billApi.recycleBin(params)),
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
        { field: 'categoryId', label: '分类', component: 'Select', colProps: { span: 8 }, componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' } },
        { field: 'userId', label: '用户', component: 'Select', colProps: { span: 8 }, componentProps: { options: userOptions, allowClear: true, showSearch: true, optionFilterProp: 'label', placeholder: '请选择用户' } },
      ],
    },
  });

  const { rowSelection, selectedRowKeys, clearSelection, handleMoveToRecycleBin, handleBatchMoveToRecycleBin, handleRestore, handleBatchRestore, handleDeletePermanently, handleBatchDeletePermanently } = useHomeaiRecycleBin({
    api: {
      moveToRecycleBin: (ids: string[]) => billApi.moveToRecycleBin(ids),
      restore: (ids: string[]) => billApi.restore(ids),
      deletePermanently: (ids: string[]) => billApi.deletePermanently(ids),
    },
    reload,
    entityName: '账单',
    permanentWarn: '此操作不可恢复！',
    confirmWithName: false,
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    clearSelection();
    reload();
  }

  onMounted(() => {
    loadUserOptions();
    loadCategoryOptions();
  });

  function getTableAction(record: HomeaiBill) {
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
    downloadCsvTemplate(['日期', '用户', '类型(income/expense)', '分类ID', '金额', '支付方式', '备注', '来源'], '账单导入模板.csv');
  }

  function handleSuccess() {
    reload();
  }
</script>
