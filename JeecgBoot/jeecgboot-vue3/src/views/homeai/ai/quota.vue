<template>
  <div style="padding: 16px">
    <!-- 默认配额配置卡片 -->
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card title="今日总消耗" :bordered="false">
          <a-statistic :value="overview.todayTotal || 0" suffix="Token" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="本月总消耗" :bordered="false">
          <a-statistic :value="overview.monthTotal || 0" suffix="Token" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="活跃用户数" :bordered="false">
          <a-statistic :value="overview.activeUserCount || 0" suffix="人" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card title="默认每日限额" :bordered="false">
          <a-statistic :value="defaultQuota.dailyLimit || 10000" suffix="Token" />
        </a-card>
      </a-col>
    </a-row>

    <!-- 用户配额消耗列表 -->
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:reload-outlined" type="primary" @click="handleRefresh">刷新</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="[{ icon: 'ant-design:edit-outlined', title: '配置额度', onClick: () => openEdit(record) }]" />
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
  <BasicModal @register="registerModal" title="配置用户额度" width="480px">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <template #footer>
      <a-button @click="closeModal()">取消</a-button>
      <a-button type="primary" @click="submit">保存</a-button>
    </template>
  </BasicModal>
</template>

<script lang="ts" name="homeai-ai-quota" setup>
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { quotaApi } from '/@/api/homeai';

  const { createMessage } = useMessage();
  const defaultQuota = ref({
    dailyLimit: 10000,
    monthlyLimit: 200000,
    overdraftLimit: 1000,
  });
  const overview = ref({ todayTotal: 0, monthTotal: 0, activeUserCount: 0 });

  const [registerTable, { reload }] = useTable({
    title: '用户Token消耗统计',
    api: quotaApi.getUserQuotaPage,
    columns: [
      { title: '用户', dataIndex: 'nickname', width: 140 },
      { title: '用户ID', dataIndex: 'userId', width: 180 },
      { title: '每日限额', dataIndex: 'dailyLimit', width: 100 },
      { title: '每月限额', dataIndex: 'monthlyLimit', width: 110 },
      { title: '今日消耗', dataIndex: 'dailyUsage', key: 'dailyUsage', width: 250 },
      { title: '本月消耗', dataIndex: 'monthlyUsage', key: 'monthlyUsage', width: 250 },
      { title: '最后活跃', dataIndex: 'lastActiveTime', width: 180 },
      { title: '有效期至', dataIndex: 'effectiveEnd', width: 120 },
    ],
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
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

  async function loadOverview() {
    try {
      const res = await quotaApi.getOverview();
      overview.value = res as any;
    } catch {
      // 忽略
    }
  }

  const editUserId = ref('');
  const [registerModal, { openModal, closeModal }] = useModal();
  const [registerForm, { setFieldsValue, resetFields, submit }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'nickname', label: '用户', component: 'Input', componentProps: { disabled: true } },
      { field: 'dailyLimit', label: '每日限额', component: 'InputNumber', required: true, defaultValue: 10000 },
      { field: 'monthlyLimit', label: '每月限额', component: 'InputNumber', required: true, defaultValue: 200000 },
      { field: 'effectiveEnd', label: '有效期至', component: 'DatePicker', componentProps: { valueFormat: 'YYYY-MM-DD HH:mm:ss' } },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  function openEdit(record: any) {
    editUserId.value = record.userId;
    setFieldsValue({
      nickname: record.nickname,
      dailyLimit: record.dailyLimit,
      monthlyLimit: record.monthlyLimit,
      effectiveEnd: record.effectiveEnd,
    });
    openModal(true);
  }

  async function handleSubmit(values: any) {
    try {
      await quotaApi.updateUserQuota({
        userId: editUserId.value,
        dailyLimit: values.dailyLimit,
        monthlyLimit: values.monthlyLimit,
        effectiveEnd: values.effectiveEnd || undefined,
      });
      createMessage.success('配置已更新');
      closeModal();
      reload();
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '更新失败');
      return false;
    }
  }

  function handleRefresh() {
    loadDefaultQuota();
    loadOverview();
    reload();
  }

  onMounted(() => {
    loadDefaultQuota();
    loadOverview();
  });
</script>
