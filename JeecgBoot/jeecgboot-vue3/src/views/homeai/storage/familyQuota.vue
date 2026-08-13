<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-row :gutter="16" style="margin-bottom: 16px">
      <a-col :span="6">
        <a-card :bordered="false"><a-statistic title="家庭数" :value="summary.familyCount || 0" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false">
          <a-statistic title="超告警" :value="summary.warnCount || 0" :value-style="{ color: (summary.warnCount || 0) > 0 ? '#cf1322' : undefined }" />
        </a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false"><a-statistic title="自定义配额" :value="summary.customCount || 0" /></a-card>
      </a-col>
      <a-col :span="6">
        <a-card :bordered="false">
          <a-statistic title="合计已用" :value="formatSize(summary.totalUsed || 0)" />
          <div style="margin-top: 8px; font-size: 12px; color: #888">
            默认家庭配额 {{ formatSize(defaultFamilyLimitBytes) }} · 告警 {{ warnPercent }}%
          </div>
        </a-card>
      </a-col>
    </a-row>

    <a-card :bordered="false">
      <a-space style="margin-bottom: 12px" wrap>
        <a-input v-model:value="keyword" placeholder="家庭名称 / ID" allow-clear style="width: 220px" @pressEnter="load" />
        <a-checkbox v-model:checked="onlyWarn" @change="load">仅告警</a-checkbox>
        <a-checkbox v-model:checked="onlyCustom" @change="load">仅自定义</a-checkbox>
        <a-button type="primary" @click="load">查询</a-button>
        <a-button :disabled="selectedRowKeys.length === 0" @click="openBatchSet">批量设配额 ({{ selectedRowKeys.length }})</a-button>
        <a-button :disabled="selectedRowKeys.length === 0" danger @click="batchReset">批量恢复默认</a-button>
      </a-space>
      <a-table
        row-key="familyId"
        size="small"
        :columns="columns"
        :data-source="items"
        :loading="loading"
        :row-selection="rowSelection"
        :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'used'">
            <div>{{ formatSize(record.totalSize) }} / {{ formatSize(record.limitBytes) }}</div>
            <a-progress
              :percent="Math.min(100, Math.round(record.usedPercent || 0))"
              size="small"
              :status="record.overWarn ? 'exception' : 'normal'"
            />
          </template>
          <template v-else-if="column.key === 'customLimit'">
            <a-tag :color="record.customLimit ? 'blue' : 'default'">{{ record.customLimit ? '自定义' : '默认' }}</a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="openSingle(record)">调整</a-button>
            <a-button v-if="record.customLimit" type="link" size="small" danger @click="resetOne(record)">恢复默认</a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal v-model:open="batchVisible" :title="batchTitle" :confirm-loading="saving" @ok="saveBatch">
      <a-form layout="vertical">
        <a-form-item label="配额 (GB)">
          <a-input-number v-model:value="batchGb" :min="0.1" :max="100" :step="0.5" style="width: 100%" />
        </a-form-item>
      </a-form>
    </a-modal>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-storage-family-quota" setup>
  import { PageWrapper } from '/@/components/Page';
  import { computed, onMounted, reactive, ref } from 'vue';
  import { storageApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage, createConfirm } = useMessage();
  const GB = 1024 * 1024 * 1024;
  const loading = ref(false);
  const saving = ref(false);
  const keyword = ref('');
  const onlyWarn = ref(false);
  const onlyCustom = ref(false);
  const items = ref<any[]>([]);
  const summary = reactive({ familyCount: 0, warnCount: 0, customCount: 0, totalUsed: 0 });
  const defaultFamilyLimitBytes = ref(5 * GB);
  const warnPercent = ref(80);
  const selectedRowKeys = ref<string[]>([]);
  const batchVisible = ref(false);
  const batchGb = ref(5);
  const batchFamilyId = ref('');

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const batchTitle = computed(() => (batchFamilyId.value ? '调整家庭配额' : `批量设配额（${selectedRowKeys.value.length} 个家庭）`));

  const columns = [
    { title: '家庭', dataIndex: 'familyName', width: 180 },
    { title: '成员', dataIndex: 'memberCount', width: 80 },
    { title: '文件数', dataIndex: 'fileCount', width: 80 },
    { title: '用量 / 配额', key: 'used', width: 280 },
    { title: '配额类型', key: 'customLimit', width: 100 },
    { title: '操作', key: 'action', width: 160 },
  ];

  function formatSize(bytes: number): string {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) {
      size /= 1024;
      i++;
    }
    return `${size.toFixed(i === 0 ? 0 : 1)} ${units[i]}`;
  }

  async function load() {
    loading.value = true;
    try {
      const data: any = await storageApi.familyQuotaBoard({
        keyword: keyword.value || undefined,
        onlyWarn: onlyWarn.value || undefined,
        onlyCustom: onlyCustom.value || undefined,
      });
      items.value = data?.items || [];
      Object.assign(summary, data?.summary || {});
      defaultFamilyLimitBytes.value = data?.defaultFamilyLimitBytes || 5 * GB;
      warnPercent.value = data?.warnPercent || 80;
      selectedRowKeys.value = selectedRowKeys.value.filter((id) => items.value.some((r) => r.familyId === id));
    } catch {
      items.value = [];
    } finally {
      loading.value = false;
    }
  }

  function openSingle(record: any) {
    batchFamilyId.value = record.familyId;
    batchGb.value = Number(((record.limitBytes || defaultFamilyLimitBytes.value) / GB).toFixed(2));
    batchVisible.value = true;
  }

  function openBatchSet() {
    batchFamilyId.value = '';
    batchGb.value = Number((defaultFamilyLimitBytes.value / GB).toFixed(2));
    batchVisible.value = true;
  }

  async function saveBatch() {
    const ids = batchFamilyId.value ? [batchFamilyId.value] : selectedRowKeys.value;
    if (!ids.length) {
      createMessage.warning('请选择家庭');
      return;
    }
    saving.value = true;
    try {
      await storageApi.batchFamilyStorageLimit({
        items: ids.map((familyId) => ({ familyId, limitBytes: Math.round(batchGb.value * GB) })),
      });
      createMessage.success('已保存');
      batchVisible.value = false;
      await load();
    } finally {
      saving.value = false;
    }
  }

  async function resetOne(record: any) {
    await storageApi.clearFamilyStorageLimit(record.familyId);
    createMessage.success('已恢复默认');
    await load();
  }

  function batchReset() {
    createConfirm({
      iconType: 'warning',
      title: '恢复默认配额',
      content: `将 ${selectedRowKeys.value.length} 个家庭恢复为默认家庭配额？`,
      onOk: async () => {
        await storageApi.batchFamilyStorageLimit({ resetIds: selectedRowKeys.value });
        createMessage.success('已恢复默认');
        selectedRowKeys.value = [];
        await load();
      },
    });
  }

  onMounted(load);
</script>
