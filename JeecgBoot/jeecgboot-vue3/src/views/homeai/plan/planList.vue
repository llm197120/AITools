<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="计划列表" />
      <a-tab-pane key="calendar" tab="日历视图" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <PlanCalendarTab v-if="activeTab === 'calendar'" ref="calendarTabRef" />
    <a-row v-if="activeTab === 'list'" :gutter="16" style="margin-bottom: 16px">
      <a-col :span="24" style="margin-bottom: 12px">
        <a-space>
          <span>完成率筛选：</span>
          <a-date-picker
            v-model:value="completionMonth"
            picker="month"
            format="YYYY-MM"
            value-format="YYYY-MM"
            placeholder="选择月份"
            style="width: 140px"
            @change="loadCompletion"
          />
          <a-select
            v-model:value="completionUserId"
            allow-clear
            placeholder="全部用户"
            style="width: 220px"
            :options="userOptions"
            @change="loadCompletion"
          />
        </a-space>
      </a-col>
      <a-col :span="12">
        <a-card :bordered="false" :title="`${completionMonth || '本月'}完成率统计`">
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
    <a-alert
      v-if="listFailed && activeTab !== 'calendar'"
      type="error"
      show-icon
      message="列表加载失败，当前可能不是最新数据"
      style="margin-bottom: 12px"
    >
      <template #action>
        <a-button size="small" @click="reload">重试</a-button>
      </template>
    </a-alert>
    <BasicTable @register="registerTable" :rowSelection="rowSelection" v-show="activeTab !== 'calendar'">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, planApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" @click="handleExportXls('计划列表', planApi.exportXls)">导出</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:sync-outlined" @click="handleRollForwardAll">补跑重复实例</a-button>
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
        <template v-if="column.key === 'priority'">
          <a-tag :color="planPriorityColor(record.priority)">{{ {urgent:'紧急',important:'重要',normal:'普通'}[record.priority] }}</a-tag>
        </template>
        <template v-else-if="column.key === 'userId' || column.dataIndex === 'userId'">
          {{ resolveUserLabel(record.userId) }}
        </template>
      </template>
    </BasicTable>
    <PlanDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" name="homeai-plan-list" setup>
  import { PageWrapper } from '/@/components/Page';
  import { computed, ref, onMounted } from 'vue';
  import dayjs from 'dayjs';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { planApi } from '/@/api/homeai';
  import { useUserLabel } from '../hooks/useUserLabel';
  import { planPriorityColor } from '../hooks/homeaiStatusColors';
  import { useHomeaiRecycleBin } from '../hooks/useHomeaiRecycleBin';
  import { useHomeaiListLoad } from '../hooks/useHomeaiListLoad';
  import { downloadCsvTemplate } from '../utils/csvTemplate';
  import PlanDrawer from './PlanDrawer.vue';
  import PlanCalendarTab from './PlanCalendarTab.vue';

  const { createMessage } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const activeTab = ref('list');
  const completion = ref<Array<{ userId?: string; total?: number; completed?: number; rate?: number }>>([]);
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const completionUserId = ref<string | undefined>(undefined);
  const completionMonth = ref<string>(dayjs().format('YYYY-MM'));
  const { userOptions, loadUserOptions, resolveUserLabel } = useUserLabel();
  const calendarTabRef = ref<{ refreshCalendarTab?: () => void; handleRollForwardAll?: () => void } | null>(null);

  function repeatLabel(rule: string) {
    return { none: '不重复', daily: '每天', weekly: '每周', monthly: '每月' }[rule] || rule || '-';
  }

  async function loadCategoryOptions() {
    try {
      const list = ((await planApi.categories()) as Array<{ name: string }>) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.name }));
    } catch {
      categoryOptions.value = [];
    }
  }

  const completionColumns = computed(() => [
    { title: '用户', dataIndex: 'userId', width: 180, customRender: ({ text }: { text: string }) => resolveUserLabel(text) },
    { title: '计划数', dataIndex: 'total', width: 80 },
    { title: '已完成', dataIndex: 'completed', width: 80 },
    {
      title: '完成率',
      dataIndex: 'rate',
      key: 'rate',
      width: 120,
      customRender: ({ text }: { text: number }) => text + '%',
    },
  ]);
  const completionRate = computed(() => {
    const total = completion.value.reduce((s, r) => s + (r.total || 0), 0);
    const done = completion.value.reduce((s, r) => s + (r.completed || 0), 0);
    return total === 0 ? 0 : Math.round((done / total) * 100);
  });

  const columns = [
    { title: '标题', dataIndex: 'title', width: 200 },
    { title: '日期', dataIndex: 'planDate', width: 110 },
    { title: '分类', dataIndex: 'category', width: 80 },
    { title: '关联菜谱', dataIndex: 'recipeName', width: 140 },
    { title: '重复', dataIndex: 'repeatRule', width: 80, customRender: ({ text }: { text: string }) => repeatLabel(text) },
    { title: '优先级', dataIndex: 'priority', key: 'priority', width: 70 },
    { title: '用户', dataIndex: 'userId', key: 'userId', width: 160 },
  ];

  const { listFailed, wrapListApi } = useHomeaiListLoad();

  const [registerTable, { reload }] = useTable({
    title: '计划管理',
    api: wrapListApi((params) => (activeTab.value === 'list' ? planApi.list(params) : planApi.recycleBin(params))),
    columns: columns,
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: { width: 120, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    formConfig: {
      labelWidth: 80,
      schemas: [
        { field: 'title', label: '标题', component: 'Input', colProps: { span: 8 } },
        {
          field: 'category',
          label: '分类',
          component: 'Select',
          colProps: { span: 8 },
          componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' },
        },
        { field: 'userId', label: '用户ID', component: 'Input', colProps: { span: 8 } },
        { field: 'planDate', label: '计划日期', component: 'DatePicker', colProps: { span: 8 } },
      ],
    },
  });

  const { rowSelection, selectedRowKeys, clearSelection, handleMoveToRecycleBin, handleBatchMoveToRecycleBin, handleRestore, handleBatchRestore, handleDeletePermanently, handleBatchDeletePermanently } = useHomeaiRecycleBin({
    api: {
      moveToRecycleBin: (ids: string[]) => planApi.moveToRecycleBin(ids),
      restore: (ids: string[]) => planApi.restore(ids),
      deletePermanently: (ids: string[]) => planApi.deletePermanently(ids),
    },
    reload,
    entityName: '计划',
    permanentWarn: '此操作不可恢复！',
    confirmWithName: false,
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    clearSelection();
    if (key !== 'calendar') {
      reload();
    }
    if (key === 'list') {
      loadUserOptions();
      loadCompletion();
    }
  }

  async function loadCompletion() {
    try {
      const params: { userId?: string; yearMonth?: string } = {};
      if (completionUserId.value) params.userId = completionUserId.value;
      if (completionMonth.value) params.yearMonth = completionMonth.value;
      completion.value = ((await planApi.completion(params)) as typeof completion.value) || [];
    } catch {
      completion.value = [];
    }
  }

  onMounted(() => {
    loadUserOptions();
    loadCompletion();
    loadCategoryOptions();
  });

  function getTableAction(record: Record<string, unknown> & { id?: string; title?: string; isRepeatMaster?: number }) {
    if (activeTab.value === 'recycle') {
      return [
        { icon: 'ant-design:undo-outlined', onClick: () => handleRestore(record), title: '恢复' },
        {
          icon: 'ant-design:delete-outlined',
          onClick: () => handleDeletePermanently(record),
          title: '彻底删除',
          color: 'error' as const,
        },
      ];
    }
    return [
      { icon: 'ant-design:edit-outlined', onClick: () => openDrawer(true, { record, isUpdate: true }), title: '编辑' },
      ...(record.isRepeatMaster === 1
        ? [{ icon: 'ant-design:sync-outlined', onClick: () => handleRollForwardOne(record), title: '补跑实例' }]
        : []),
      {
        icon: 'ant-design:delete-outlined',
        onClick: () => handleMoveToRecycleBin(record),
        title: '移入回收站',
        color: 'error' as const,
      },
    ];
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: false, record: {} });
  }

  function handleDownloadTemplate() {
    downloadCsvTemplate(
      ['计划标题', '计划内容', '分类', '优先级(normal/important/urgent)', '是否全天(0/1)', '开始时间(HH:MM)', '结束时间(HH:MM)', '提前提醒分钟数'],
      '计划导入模板.csv',
    );
  }

  function handleSuccess() {
    reload();
  }

  function handleRollForwardAll() {
    createConfirm({
      iconType: 'info',
      title: '补跑重复计划实例',
      content: '将为所有重复计划补齐至配置窗口天数，是否继续？',
      onOk: async () => {
        const res = (await planApi.rollForwardRepeat()) as { created?: number };
        createMessage.success(`已新建 ${res?.created ?? 0} 条实例`);
        calendarTabRef.value?.refreshCalendarTab?.();
      },
    });
  }

  function handleRollForwardOne(record: { id?: string; title?: string }) {
    createConfirm({
      iconType: 'info',
      title: '补跑重复实例',
      content: `为计划「${record.title}」补齐未来实例？`,
      onOk: async () => {
        const res = (await planApi.rollForwardRepeat(String(record.id))) as { created?: number };
        createMessage.success(`已新建 ${res?.created ?? 0} 条实例`);
      },
    });
  }
</script>
