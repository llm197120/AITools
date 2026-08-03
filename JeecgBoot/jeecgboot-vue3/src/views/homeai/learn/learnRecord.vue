<template>
  <div style="padding: 16px">
    <!-- 统计概览 -->
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
  </div>
</template>

<script lang="ts" name="homeai-learn-record" setup>
  import { ref, onMounted } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { defHttp } from '/@/utils/http/axios';

  const stats = ref({ totalRecords: 0, totalDurationMinutes: 0, activeUserCount: 0, activeDayCount: 0 });

  const [registerTable, { reload }] = useTable({
    title: '学习记录',
    api: (params: any) => defHttp.get({ url: '/homeai/learn/admin/records', params }),
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
      schemas: [
        { field: 'userId', label: '用户ID', component: 'Input' },
      ],
    },
  });

  async function loadStats() {
    try {
      stats.value = (await defHttp.get({ url: '/homeai/learn/admin/stats' })) as any;
    } catch {
      // 使用默认值
    }
  }

  onMounted(() => {
    loadStats();
  });
</script>
