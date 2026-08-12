<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space>
        <a-date-picker v-model:value="yearMonth" picker="month" value-format="YYYY-MM" :allow-clear="false" @change="load" />
        <a-input v-model:value="userId" placeholder="用户ID（可选）" style="width: 220px" allow-clear @pressEnter="load" />
        <a-select v-model:value="days" style="width: 140px" @change="load">
          <a-select-option :value="7">近 7 日趋势</a-select-option>
          <a-select-option :value="30">近 30 日趋势</a-select-option>
          <a-select-option :value="90">近 90 日趋势</a-select-option>
        </a-select>
        <a-button type="primary" @click="load">查询</a-button>
      </a-space>
    </a-card>

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card :bordered="false">
          <a-statistic title="计划完成率" :value="plan.overallRate || 0" suffix="%" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false">
          <a-statistic title="计划完成/总数" :value="plan.completed || 0" :suffix="`/ ${plan.total || 0}`" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false">
          <a-statistic title="学习总时长" :value="learn.totalDurationMinutes || 0" suffix="分钟" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false">
          <a-statistic title="学习记录 / 活跃用户" :value="learn.totalRecords || 0" :suffix="`/ ${learn.activeUserCount || 0}人`" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="14">
        <a-card title="学习趋势" :bordered="false">
          <div ref="chartRef" style="height: 320px"></div>
        </a-card>
      </a-col>
      <a-col :span="10">
        <a-card title="计划完成率（按用户）" :bordered="false">
          <BasicTable :dataSource="plan.byUser || []" :columns="planColumns" :pagination="false" row-key="userId" size="small" />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="学习按分类" :bordered="false">
      <BasicTable :dataSource="learn.byCategory || []" :columns="categoryColumns" :pagination="false" row-key="categoryId" />
    </a-card>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-dashboard-cross-stats" setup>
  import { PageWrapper } from '/@/components/Page';
  import { ref, onMounted, Ref } from 'vue';
  import dayjs from 'dayjs';
  import { BasicTable } from '/@/components/Table';
  import { dashboardApi } from '/@/api/homeai';
  import { useECharts } from '/@/hooks/web/useECharts';

  const yearMonth = ref(dayjs().format('YYYY-MM'));
  const userId = ref('');
  const days = ref(30);
  const plan = ref<any>({ overallRate: 0, total: 0, completed: 0, byUser: [] });
  const learn = ref<any>({
    totalRecords: 0,
    totalDurationMinutes: 0,
    activeUserCount: 0,
    trend: [],
    byCategory: [],
  });

  const chartRef = ref<HTMLDivElement | null>(null);
  const { setOptions } = useECharts(chartRef as Ref<HTMLDivElement>);

  const planColumns = [
    { title: '用户ID', dataIndex: 'userId', width: 180 },
    { title: '总数', dataIndex: 'total', width: 80 },
    { title: '已完成', dataIndex: 'completed', width: 80 },
    { title: '完成率%', dataIndex: 'rate', width: 90 },
  ];

  const categoryColumns = [
    { title: '分类', dataIndex: 'categoryName', width: 160 },
    { title: '资料数', dataIndex: 'materialCount', width: 90 },
    { title: '记录数', dataIndex: 'recordCount', width: 90 },
    { title: '时长(分钟)', dataIndex: 'totalDuration', width: 110 },
  ];

  async function load() {
    try {
      const res: any = await dashboardApi.planLearn({
        yearMonth: yearMonth.value,
        days: days.value,
        userId: userId.value || undefined,
      });
      plan.value = res?.plan || { overallRate: 0, total: 0, completed: 0, byUser: [] };
      learn.value = res?.learn || {};
      const trend = learn.value.trend || [];
      setOptions({
        tooltip: { trigger: 'axis' },
        legend: { data: ['记录数', '时长(分钟)'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: { type: 'category', data: trend.map((t: any) => t.date) },
        yAxis: [
          { type: 'value', name: '记录数' },
          { type: 'value', name: '分钟' },
        ],
        series: [
          { name: '记录数', type: 'bar', data: trend.map((t: any) => t.recordCount || 0) },
          {
            name: '时长(分钟)',
            type: 'line',
            yAxisIndex: 1,
            smooth: true,
            data: trend.map((t: any) => t.durationMinutes || 0),
          },
        ],
      });
    } catch {
      plan.value = { overallRate: 0, total: 0, completed: 0, byUser: [] };
      learn.value = { totalRecords: 0, totalDurationMinutes: 0, activeUserCount: 0, trend: [], byCategory: [] };
      setOptions({ title: { text: '暂无数据', left: 'center', top: 'center' } });
    }
  }

  onMounted(load);
</script>
