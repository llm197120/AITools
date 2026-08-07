<template>
  <div style="padding: 16px">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="计划列表" />
      <a-tab-pane key="calendar" tab="日历视图" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <a-row v-if="activeTab === 'calendar'" :gutter="16" style="margin-bottom: 16px">
      <a-col :span="24" style="margin-bottom: 12px">
        <a-space>
          <span>筛选用户：</span>
          <a-select
            v-model:value="calendarUserId"
            allow-clear
            placeholder="全部用户"
            style="width: 220px"
            :options="userOptions"
            @change="onCalendarUserChange"
          />
        </a-space>
      </a-col>
      <a-col :span="14">
        <a-card :bordered="false" title="计划日历">
          <template #extra>
            <a-button size="small" type="link" @click="handleRollForwardAll">全量补跑重复实例</a-button>
          </template>
          <a-calendar v-model:value="calendarValue" @panelChange="onCalendarPanelChange" @select="onCalendarSelect">
            <template #dateCellRender="{ current }">
              <a-badge v-if="calendarCellStatus(current) === 'pending'" status="processing" />
              <a-badge v-else-if="calendarCellStatus(current) === 'expired'" status="default" />
              <a-badge v-else-if="calendarCellStatus(current) === 'mixed'" status="warning" />
            </template>
          </a-calendar>
        </a-card>
      </a-col>
      <a-col :span="10">
        <a-card :bordered="false" :title="selectedDateLabel + ' 的计划'">
          <a-list v-if="dayPlans.length" :data-source="dayPlans" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta
                  :title="item.title"
                  :description="`${item.category || '-'} · ${item.userId || '-'} · ${statusLabel(item.status)}${item.repeatRule && item.repeatRule !== 'none' ? ' · ' + repeatLabel(item.repeatRule) : ''}`"
                />
              </a-list-item>
            </template>
          </a-list>
          <a-empty v-else description="当日暂无计划" />
        </a-card>
      </a-col>
    </a-row>
    <a-card v-if="activeTab === 'calendar'" title="补跑操作日志" :bordered="false" style="margin-bottom: 16px">
      <a-table
        :data-source="rollLogs"
        :columns="rollLogColumns"
        :pagination="rollLogPagination"
        size="small"
        row-key="id"
        @change="onRollLogTableChange"
      />
    </a-card>
    <a-row v-if="activeTab === 'list'" :gutter="16" style="margin-bottom: 16px">
      <a-col :span="24" style="margin-bottom: 12px">
        <a-space>
          <span>完成率筛选：</span>
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
    <BasicTable @register="registerTable" :rowSelection="rowSelection" v-show="activeTab !== 'calendar'">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, planApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined" type="primary">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" type="primary" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" type="primary" @click="handleExportXls('计划列表', planApi.exportXls)">导出</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:sync-outlined" @click="handleRollForwardAll">补跑重复实例</a-button>
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
import dayjs, { Dayjs } from 'dayjs';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
import { planApi, userApi } from '/@/api/homeai';
import { defHttp } from '/@/utils/http/axios';
  import PlanDrawer from './PlanDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const selectedRowKeys = ref<string[]>([]);
const activeTab = ref('list');
const completion = ref<any[]>([]);
const categoryOptions = ref<{ label: string; value: string }[]>([]);
const calendarUserId = ref<string | undefined>(undefined);
const completionUserId = ref<string | undefined>(undefined);
const userOptions = ref<{ label: string; value: string }[]>([]);
const rollLogs = ref<any[]>([]);
const rollLogPagination = ref({ current: 1, pageSize: 5, total: 0 });
const rollLogColumns = [
  { title: '操作时间', dataIndex: 'createTime', width: 170 },
  { title: '操作人', dataIndex: 'userId', width: 140 },
  { title: '摘要', dataIndex: 'targetSummary' },
  { title: '结果', dataIndex: 'result', width: 80 },
];
const calendarValue = ref<Dayjs>(dayjs());
const calendarDates = ref<string[]>([]);
const expiredDates = ref<string[]>([]);
const pendingDates = ref<string[]>([]);
const dayPlans = ref<any[]>([]);
const selectedDateLabel = computed(() => calendarValue.value.format('YYYY-MM-DD'));

function calendarCellStatus(current: Dayjs): string | null {
  const d = current.format('YYYY-MM-DD');
  const hasPending = pendingDates.value.includes(d);
  const hasExpired = expiredDates.value.includes(d);
  if (hasPending && hasExpired) return 'mixed';
  if (hasPending) return 'pending';
  if (hasExpired) return 'expired';
  if (calendarDates.value.includes(d)) return 'pending';
  return null;
}

function statusLabel(status: string) {
  return { pending: '待完成', completed: '已完成', expired: '已过期', cancelled: '已取消' }[status] || status || '-';
}

async function loadCalendarDates(yearMonth?: string) {
  const ym = yearMonth || calendarValue.value.format('YYYY-MM');
  try {
    const res: any = await planApi.adminCalendar({
      yearMonth: ym,
      userId: calendarUserId.value,
    });
    if (Array.isArray(res)) {
      calendarDates.value = res.map((d) => (typeof d === 'string' ? d : dayjs(d).format('YYYY-MM-DD')));
      expiredDates.value = [];
      pendingDates.value = [];
      return;
    }
    calendarDates.value = (res?.dates || []).map((d: string) => dayjs(d).format('YYYY-MM-DD'));
    expiredDates.value = (res?.expiredDates || []).map((d: string) => dayjs(d).format('YYYY-MM-DD'));
    pendingDates.value = (res?.pendingDates || []).map((d: string) => dayjs(d).format('YYYY-MM-DD'));
  } catch {
    calendarDates.value = [];
    expiredDates.value = [];
    pendingDates.value = [];
  }
}

async function loadDayPlans(date?: string) {
  const d = date || calendarValue.value.format('YYYY-MM-DD');
  try {
    dayPlans.value =
      (await planApi.adminPlansByDate(d, calendarUserId.value)) || [];
  } catch {
    dayPlans.value = [];
  }
}

function onCalendarUserChange() {
  loadCalendarDates();
  loadDayPlans();
}

async function loadUserOptions() {
  try {
    userOptions.value = ((await userApi.options()) as any[]) || [];
  } catch {
    userOptions.value = [];
  }
}

async function loadRollLogs(pageNo = 1) {
  try {
    const res: any = await planApi.rollForwardLogs({ pageNo, pageSize: rollLogPagination.value.pageSize });
    rollLogs.value = res?.records || [];
    rollLogPagination.value = {
      ...rollLogPagination.value,
      current: res?.current || pageNo,
      total: res?.total || 0,
    };
  } catch {
    rollLogs.value = [];
  }
}

function onRollLogTableChange(pagination: any) {
  loadRollLogs(pagination.current);
}

function onCalendarPanelChange(date: Dayjs) {
  calendarValue.value = date;
  loadCalendarDates(date.format('YYYY-MM'));
}

function onCalendarSelect(date: Dayjs) {
  calendarValue.value = date;
  loadDayPlans(date.format('YYYY-MM-DD'));
}

async function loadCategoryOptions() {
  try {
    const list: any[] = (await planApi.categories()) || [];
    categoryOptions.value = list.map((c) => ({ label: c.name, value: c.name }));
  } catch {
    categoryOptions.value = [];
  }
}
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

function repeatLabel(rule: string) {
  return { none: '不重复', daily: '每天', weekly: '每周', monthly: '每月' }[rule] || rule || '-';
}

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
    { title: '重复', dataIndex: 'repeatRule', width: 80, customRender: ({ text }: any) => repeatLabel(text) },
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
        { field: 'category', label: '分类', component: 'Select', colProps: { span: 8 }, componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' } },
        { field: 'userId', label: '用户ID', component: 'Input', colProps: { span: 8 } },
        { field: 'planDate', label: '计划日期', component: 'DatePicker', colProps: { span: 8 } },
      ],
    },
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    selectedRowKeys.value = [];
    if (key !== 'calendar') {
      reload();
    }
    if (key === 'list') {
      loadUserOptions();
      loadCompletion();
    }
    if (key === 'calendar') {
      loadUserOptions();
      loadCalendarDates();
      loadDayPlans();
      loadRollLogs();
    }
  }

  async function loadCompletion() {
    try {
      const params: any = {};
      if (completionUserId.value) params.userId = completionUserId.value;
      completion.value = (await defHttp.get({ url: '/homeai/plan/admin/completion', params })) as any[] || [];
    } catch {
      completion.value = [];
    }
  }

  onMounted(() => {
    loadUserOptions();
    loadCompletion();
    loadCategoryOptions();
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
      ...(record.isRepeatMaster === 1
        ? [{ icon: 'ant-design:sync-outlined', onClick: () => handleRollForwardOne(record), title: '补跑实例' }]
        : []),
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

  function handleRollForwardAll() {
    createConfirm({
      iconType: 'info',
      title: '补跑重复计划实例',
      content: '将为所有重复计划补齐至配置窗口天数，是否继续？',
      onOk: async () => {
        const res: any = await planApi.rollForwardRepeat();
        createMessage.success(`已新建 ${res?.created ?? 0} 条实例`);
        loadRollLogs();
        if (activeTab.value === 'calendar') {
          loadCalendarDates();
          loadDayPlans();
        }
      },
    });
  }

  function handleRollForwardOne(record: any) {
    createConfirm({
      iconType: 'info',
      title: '补跑重复实例',
      content: `为计划「${record.title}」补齐未来实例？`,
      onOk: async () => {
        const res: any = await planApi.rollForwardRepeat(record.id);
        createMessage.success(`已新建 ${res?.created ?? 0} 条实例`);
        loadRollLogs();
      },
    });
  }
</script>
