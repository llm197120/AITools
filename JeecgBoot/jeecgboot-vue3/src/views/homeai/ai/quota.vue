<template>
  <div style="padding: 16px">
    <!-- 默认配额配置卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="8">
        <a-card title="每日限额" :bordered="false">
          <a-statistic :value="defaultQuota.dailyLimit || 10000" suffix="Token" />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="每月限额" :bordered="false">
          <a-statistic :value="defaultQuota.monthlyLimit || 200000" suffix="Token" />
        </a-card>
      </a-col>
      <a-col :span="8">
        <a-card title="允许透支" :bordered="false">
          <a-statistic :value="defaultQuota.overdraftLimit || 1000" suffix="Token" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 用户配额消耗列表 -->
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:reload-outlined" type="primary" @click="handleRefresh">刷新</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'dailyUsage'">
          <a-progress
            :percent="getPercent(record.dailyUsage, defaultQuota.dailyLimit)"
            :stroke-color="getColor(record.dailyUsage, defaultQuota.dailyLimit)"
            size="small"
            :format="() => `${record.dailyUsage || 0} / ${defaultQuota.dailyLimit || 10000}`"
          />
        </template>
        <template v-if="column.key === 'monthlyUsage'">
          <a-progress
            :percent="getPercent(record.monthlyUsage, defaultQuota.monthlyLimit)"
            :stroke-color="getColor(record.monthlyUsage, defaultQuota.monthlyLimit)"
            size="small"
            :format="() => `${record.monthlyUsage || 0} / ${defaultQuota.monthlyLimit || 200000}`"
          />
        </template>
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" name="homeai-ai-quota" setup>
  import { ref, onMounted } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { quotaApi } from '/@/api/homeai';

  const defaultQuota = ref({
    dailyLimit: 10000,
    monthlyLimit: 200000,
    overdraftLimit: 1000,
  });

  const [registerTable, { reload }] = useTable({
    title: '用户Token消耗统计',
    api: quotaApi.logList,
    columns: [
      { title: '用户', dataIndex: 'nickname', width: 140 },
      { title: '用户ID', dataIndex: 'userId', width: 180 },
      { title: '今日消耗', dataIndex: 'dailyUsage', key: 'dailyUsage', width: 250 },
      { title: '本月消耗', dataIndex: 'monthlyUsage', key: 'monthlyUsage', width: 250 },
      { title: '最后活跃', dataIndex: 'lastActiveTime', width: 180 },
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

  function getPercent(used: number, total: number): number {
    if (!total || total === 0) return 0;
    return Math.min(Math.round((used / total) * 100), 100);
  }

  function getColor(used: number, total: number): { '0%': string; '100%': string } {
    const pct = getPercent(used, total);
    if (pct >= 90) return { '0%': '#ff4d4f', '100%': '#ff7875' };
    if (pct >= 70) return { '0%': '#faad14', '100%': '#ffc53d' };
    return { '0%': '#52c41a', '100%': '#73d13d' };
  }

  async function loadDefaultQuota() {
    try {
      const res = await quotaApi.getDefaultQuota();
      if (res) {
        defaultQuota.value = res as any;
      }
    } catch {
      // 使用默认值
    }
  }

  function handleRefresh() {
    loadDefaultQuota();
    reload();
  }

  onMounted(() => {
    loadDefaultQuota();
  });
</script>
