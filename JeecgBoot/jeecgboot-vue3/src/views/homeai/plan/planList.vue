<template>
  <div style="padding: 16px">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="计划列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <a-row v-if="activeTab === 'list'" :gutter="16" style="margin-bottom: 16px">
      <a-col :span="12">
        <a-card :bordered="false" title="本月完成率统计">
          <a-table
            :data-source="completion"
            :columns="completionColumns"
            :pagination="false"
            size="small"
            row-key="userId"
          />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card :bordered="false" title="完成率分布">
          <a-progress
            type="circle"
            :percent="completionRate"
            :format="(p: number) => p + '%'"
          />
          <span style="margin-left: 12px; color: #666">整体完成率（按已产生实例计算）</span>
        </a-card>
      </a-col>
    </a-row>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, planApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined" type="primary">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" type="primary" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" type="primary" @click="handleExportXls('计划列表', planApi.exportXls)">导出</a-button>
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
        <template v-if="column.key === 'priority'">
          <a-tag :color="{urgent:'#e74c3c',important:'#f39c12',normal:'#999'}[record.priority]">{{ {urgent:'紧急',important:'重要',normal:'普通'}[record.priority] }}</a-tag>
        </template>
      </template>
    </BasicTable>
    <PlanDrawer @register="registerDrawer" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="homeai-plan-list" setup>
import { computed, ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
import { planApi } from '/@/api/homeai';
import { defHttp } from '/@/utils/http/axios';
  import PlanDrawer from './PlanDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const selectedRowKeys = ref<string[]>([]);
const activeTab = ref('list');
const completion = ref<any[]>([]);
const completionColumns = [
  { title: '用户ID', dataIndex: 'userId', width: 180 },
  { title: '计划数', dataIndex: 'total', width: 80 },
  { title: '已完成', dataIndex: 'completed', width: 80 },
  { title: '完成率', dataIndex: 'rate', key: 'rate', width: 120, customRender: ({ text }: any) => text + '%' },
];
const completionRate = computed(() => {
  const total = completion.value.reduce((s, r) => s + (r.total || 0), 0);
  const done = completion.value.reduce((s, r) => s + (r.completed || 0), 0);
  return total === 0 ? 0 : Math.round((done / total) * 100);
});

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const columns = [
    { title: '标题', dataIndex: 'title', width: 200 },
    { title: '日期', dataIndex: 'planDate', width: 110 },
    { title: '分类', dataIndex: 'category', width: 80 },
    { title: '优先级', dataIndex: 'priority', key: 'priority', width: 70 },
    { title: '用户', dataIndex: 'userId', width: 160 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '计划管理',
    api: (params: any) => activeTab.value === 'list' ? planApi.list(params) : planApi.recycleBin(params),
    columns: columns,
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: { width: 120, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    formConfig: {
      labelWidth: 80,
      schemas: [
        { field: 'title', label: '标题', component: 'Input', colProps: { span: 8 } },
        { field: 'category', label: '分类', component: 'Input', colProps: { span: 8 } },
        { field: 'userId', label: '用户ID', component: 'Input', colProps: { span: 8 } },
        { field: 'planDate', label: '计划日期', component: 'DatePicker', colProps: { span: 8 } },
      ],
    },
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    selectedRowKeys.value = [];
    reload();
    if (key === 'list') loadCompletion();
  }

  async function loadCompletion() {
    try {
      completion.value = (await defHttp.get({ url: '/homeai/plan/admin/completion' })) as any[] || [];
    } catch {
      completion.value = [];
    }
  }

  onMounted(() => {
    loadCompletion();
  });

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

  async function handleMoveToRecycleBin(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将该计划移入回收站吗？`,
      onOk: async () => {
        await planApi.moveToRecycleBin([record.id]);
        createMessage.success('已移入回收站');
        reload();
      },
    });
  }

  async function handleBatchMoveToRecycleBin() {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将选中的 ${selectedRowKeys.value.length} 条计划移入回收站吗？`,
      onOk: async () => {
        await planApi.moveToRecycleBin(selectedRowKeys.value);
        createMessage.success('已移入回收站');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  async function handleRestore(record: any) {
    await planApi.restore([record.id]);
    createMessage.success('恢复成功');
    reload();
  }

  async function handleBatchRestore() {
    await planApi.restore(selectedRowKeys.value);
    createMessage.success('恢复成功');
    selectedRowKeys.value = [];
    reload();
  }

  async function handleDeletePermanently(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: '确定彻底删除该计划吗？此操作不可恢复！',
      onOk: async () => {
        await planApi.deletePermanently([record.id]);
        createMessage.success('已彻底删除');
        reload();
      },
    });
  }

  async function handleBatchDeletePermanently() {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除选中的 ${selectedRowKeys.value.length} 条计划吗？此操作不可恢复！`,
      onOk: async () => {
        await planApi.deletePermanently(selectedRowKeys.value);
        createMessage.success('已彻底删除');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  function handleDownloadTemplate() {
    const headers = ['计划标题', '计划内容', '分类', '优先级(normal/important/urgent)', '是否全天(0/1)', '开始时间(HH:MM)', '结束时间(HH:MM)', '提前提醒分钟数'];
    const blob = new Blob(['\uFEFF' + headers.join(',') + '\n'], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = '计划导入模板.csv';
    link.click();
    URL.revokeObjectURL(link.href);
  }

  function handleSuccess() {
    reload();
  }
</script>
