<template>
  <div style="padding: 16px">
    <!-- 筛选与汇总 -->
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space>
        <a-select v-model:value="dimension" style="width: 140px" @change="load">
          <a-select-option value="category">按分类</a-select-option>
          <a-select-option value="user">按用户</a-select-option>
          <a-select-option value="month">按月份</a-select-option>
        </a-select>
        <a-date-picker v-model:value="yearMonth" picker="month" :allow-clear="false" @change="load" />
        <a-button type="primary" @click="load">查询</a-button>
      </a-space>
    </a-card>

    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card :bordered="false"><a-statistic title="总支出" :value="data.totalExpense || 0" :precision="2" prefix="¥" value-style="color:#e74c3c" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false"><a-statistic title="总收入" :value="data.totalIncome || 0" :precision="2" prefix="¥" value-style="color:#27ae60" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false"><a-statistic title="结余" :value="data.balance || 0" :precision="2" prefix="¥" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false"><a-statistic title="账单笔数" :value="data.count || 0" suffix="笔" /></a-card>
      </a-col>
    </a-row>

    <BasicTable :dataSource="data.rows || []" :columns="columns" :pagination="false" row-key="name" />
  </div>
</template>

<script lang="ts" name="homeai-bill-statistics" setup>
  import { ref, reactive } from 'vue';
  import dayjs from 'dayjs';
  import { BasicTable } from '/@/components/Table';
  import { defHttp } from '/@/utils/http/axios';

  const dimension = ref('category');
  const yearMonth = ref(dayjs().format('YYYY-MM'));
  const data = reactive<any>({ totalExpense: 0, totalIncome: 0, balance: 0, count: 0, rows: [] });

  const columns = [
    { title: '维度', dataIndex: 'name', width: 200 },
    { title: '支出', dataIndex: 'expense', key: 'expense', width: 200, customRender: ({ text }: any) => `¥${Number(text || 0).toFixed(2)}` },
    { title: '收入', dataIndex: 'income', key: 'income', width: 200, customRender: ({ text }: any) => `¥${Number(text || 0).toFixed(2)}` },
  ];

  async function load() {
    try {
      const res: any = await defHttp.get({
        url: '/homeai/bill/admin/stats',
        params: { yearMonth: yearMonth.value, dimension: dimension.value },
      });
      data.totalExpense = res.totalExpense;
      data.totalIncome = res.totalIncome;
      data.balance = res.balance;
      data.count = res.count;
      data.rows = res.rows || [];
    } catch {
      data.rows = [];
    }
  }

  load();
</script>
