<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space>
        <a-select v-model:value="days" style="width: 140px" @change="reloadAll">
          <a-select-option :value="7">近 7 日</a-select-option>
          <a-select-option :value="30">近 30 日</a-select-option>
          <a-select-option :value="90">近 90 日</a-select-option>
          <a-select-option :value="0">全部</a-select-option>
        </a-select>
        <a-input v-model:value="filterUserId" placeholder="用户ID（可选）" style="width: 220px" allow-clear @pressEnter="reloadAll" />
        <a-button type="primary" @click="reloadAll">查询</a-button>
        <a-button preIcon="ant-design:download-outlined" @click="exportStats">导出统计</a-button>
      </a-space>
    </a-card>

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card title="学习记录总数" :bordered="false">
          <a-statistic :value="stats.totalRecords || 0" suffix="条" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="总学习时长" :bordered="false">
          <a-statistic :value="stats.totalDurationMinutes || 0" suffix="分钟" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="活跃用户数" :bordered="false">
          <a-statistic :value="stats.activeUserCount || 0" suffix="人" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="活跃天数" :bordered="false">
          <a-statistic :value="stats.activeDayCount || 0" suffix="天" />
        </a-card>
      </a-col>
    </a-row>

    <a-card :title="trendTitle" :bordered="false" style="margin-bottom: 16px">
      <div ref="chartRef" style="height: 320px"></div>
    </a-card>

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="12">
        <a-card title="按分类" :bordered="false">
          <BasicTable :dataSource="byCategory" :columns="categoryColumns" :pagination="false" row-key="categoryId" size="small" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="按用户排行" :bordered="false">
          <BasicTable :dataSource="byUser" :columns="userColumns" :pagination="false" row-key="userId" size="small" />
        </a-card>
      </a-col>
    </a-row>

    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:reload-outlined" type="primary" @click="reload">刷新</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'duration'">
          <a-tag color="blue">{{ record.duration || 0 }} 秒</a-tag>
        </template>
        <template v-if="column.key === 'mode'">
          <a-tag>{{ record.mode === 'timer' ? '计时' : '手动' }}</a-tag>
        </template>
      </template>
    </BasicTable>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-learn-record" setup>
  import { PageWrapper } from '/@/components/Page';
  import { computed, ref, onMounted, Ref } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { defHttp } from '/@/utils/http/axios';
  import { learnApi } from '/@/api/homeai';
  import type { HomeaiPageParams } from '/@/api/homeai';
  import { useECharts } from '/@/hooks/web/useECharts';
  import { useMethods } from '/@/hooks/system/useMethods';

  const { handleExportXlsx } = useMethods();
  const days = ref(30);
  const filterUserId = ref('');
  const stats = ref({ totalRecords: 0, totalDurationMinutes: 0, activeUserCount: 0, activeDayCount: 0 });
    /** 分类统计行 */
  interface LearnCategoryStat { categoryName?: string; materialCount?: number; recordCount?: number; totalDuration?: number }
  /** 用户统计行 */
  interface LearnUserStat { nickname?: string; userId?: string; recordCount?: number; durationMinutes?: number; activeDays?: number }

  const byCategory = ref<LearnCategoryStat[]>([]);
  const byUser = ref<LearnUserStat[]>([]);
  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  const trendTitle = computed(() => (days.value > 0 ? `近${days.value}日学习趋势` : '学习趋势（需选择近 N 日）'));

  const categoryColumns = [
    { title: '分类', dataIndex: 'categoryName', width: 140 },
    { title: '资料数', dataIndex: 'materialCount', width: 80 },
    { title: '记录数', dataIndex: 'recordCount', width: 80 },
    { title: '时长(分钟)', dataIndex: 'totalDuration', width: 100 },
  ];
  const userColumns = [
    { title: '昵称', dataIndex: 'nickname', width: 120 },
    { title: '用户ID', dataIndex: 'userId', width: 160 },
    { title: '记录数', dataIndex: 'recordCount', width: 80 },
    { title: '时长(分钟)', dataIndex: 'durationMinutes', width: 100 },
    { title: '活跃天', dataIndex: 'activeDays', width: 80 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '学习记录',
    api: (params: HomeaiPageParams) =>
      defHttp.get({
        url: '/homeai/learn/admin/records',
        params: { ...params, userId: filterUserId.value || undefined },
      }),
    columns: [
      { title: '用户', dataIndex: 'nickname', width: 140 },
      { title: '用户ID', dataIndex: 'userId', width: 170 },
      { title: '资料', dataIndex: 'materialTitle', width: 200 },
      { title: '时长', dataIndex: 'duration', key: 'duration', width: 100 },
      { title: '模式', dataIndex: 'mode', key: 'mode', width: 80 },
      { title: '时间', dataIndex: 'createTime', width: 180 },
    ],
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    formConfig: {
      schemas: [{ field: 'userId', label: '用户ID', component: 'Input' }],
    },
  });

  async function loadStats() {
    try {
      stats.value = (await learnApi.adminStats({
        days: days.value,
        userId: filterUserId.value || undefined,
      })) as any;
    } catch {
      stats.value = { totalRecords: 0, totalDurationMinutes: 0, activeUserCount: 0, activeDayCount: 0 };
    }
  }

  async function loadTrend() {
    if (days.value <= 0) {
      setOptions({ title: { text: '请选择近 7/30/90 日查看趋势', left: 'center', top: 'center' } });
      return;
    }
    try {
      const trend: { date?: string; count?: number; durationMinutes?: number }[] = (await learnApi.adminStatsTrend(days.value)) || [];
      setOptions({
        tooltip: { trigger: 'axis' },
        legend: { data: ['记录数', '时长(分钟)'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: trend.map((t) => t.date) },
        yAxis: [
          { type: 'value', name: '记录数' },
          { type: 'value', name: '分钟' },
        ],
        series: [
          { name: '记录数', type: 'bar', data: trend.map((t) => t.recordCount || 0) },
          {
            name: '时长(分钟)',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            data: trend.map((t) => t.durationMinutes || 0),
          },
        ],
      });
    } catch {
      setOptions({ title: { text: '暂无趋势数据', left: 'center', top: 'center' } });
    }
  }

  async function loadDims() {
    try {
      byCategory.value =
        (await learnApi.adminStatsByCategory({
          days: days.value,
          userId: filterUserId.value || undefined,
        })) || [];
    } catch {
      byCategory.value = [];
    }
    try {
      byUser.value = (await learnApi.adminStatsByUser(days.value > 0 ? days.value : 30)) || [];
      if (filterUserId.value) {
        byUser.value = byUser.value.filter((u) => u.userId === filterUserId.value);
      }
    } catch {
      byUser.value = [];
    }
  }

  async function reloadAll() {
    await Promise.all([loadStats(), loadTrend(), loadDims(), reload()]);
  }

  function exportStats() {
    handleExportXlsx('学习统计导出', learnApi.adminStatsExport, {
      days: days.value,
      userId: filterUserId.value || undefined,
    });
  }

  onMounted(() => {
    reloadAll();
  });
</script>
