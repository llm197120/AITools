<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space direction="vertical" style="width: 100%">
        <a-form layout="inline">
          <a-form-item label="归属用户" required>
            <a-select
              v-model:value="importUserId"
              :options="userOptions"
              allow-clear
              show-search
              option-filter-prop="label"
              placeholder="请选择账单归属用户"
              style="min-width: 220px"
            />
          </a-form-item>
        </a-form>
        <a-space>
        <a-radio-group v-model:value="fileType">
          <a-radio-button value="wechat_csv">微信支付CSV</a-radio-button>
          <a-radio-button value="excel">Excel账单</a-radio-button>
        </a-radio-group>
        <a-upload :show-upload-list="false" :before-upload="handleFile" accept=".csv,.xlsx,.xls">
          <a-button type="primary">选择文件并解析</a-button>
        </a-upload>
        <a-button v-if="rows.length > 0" type="primary" danger :loading="importing" @click="confirmImport"
          >确认导入选中({{ checkedRows.length }})</a-button
        >
        </a-space>
      </a-space>
    </a-card>

    <a-alert v-if="message" :message="message" type="info" show-icon closable style="margin-bottom: 16px" />

    <a-empty v-if="rows.length === 0" description="请选择文件并解析账单" style="margin-top: 24px" />

    <BasicTable
      v-if="rows.length > 0"
      :dataSource="rows"
      :columns="columns"
      :row-selection="{ selectedRowKeys: checkedKeys, onChange: onCheckedChange }"
      :pagination="false"
      row-key="index"
      :scroll="{ x: 1000 }"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.duplicate ? 'orange' : 'green'">{{ record.duplicate ? '重复' : '新增' }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'type'">
          <span :class="record.type === 'income' ? 'hai-text-success' : 'hai-text-danger'">{{ record.type === 'income' ? '收入' : '支出' }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'amount'">
          <span :class="record.type === 'income' ? 'hai-amount-income' : 'hai-amount-expense'">{{ record.amount }}</span>
        </template>
      </template>
    </BasicTable>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-bill-import" setup>
  import { PageWrapper } from '/@/components/Page';
  import { ref, computed, onMounted } from 'vue';
  import { BasicTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { defHttp } from '/@/utils/http/axios';
  import { useUserLabel } from '../hooks/useUserLabel';

  const { createMessage, createConfirm } = useMessage();
  const { userOptions, loadUserOptions } = useUserLabel();
  const fileType = ref('wechat_csv');
  const importUserId = ref<string>();

  onMounted(() => {
    loadUserOptions();
  });

  /** 导入预览行（后端解析结果） */
  interface BillImportRow {
    index?: number;
    billDate?: string;
    type?: string;
    categoryId?: string;
    categoryName?: string;
    amount?: number | string;
    paymentMethod?: string;
    remark?: string;
    /** 是否重复（默认不勾选） */
    duplicate?: boolean;
    status?: string;
    [key: string]: unknown;
  }

  const rows = ref<BillImportRow[]>([]);
  const checkedKeys = ref<string[]>([]);
  const importing = ref(false);
  const message = ref('');

  const checkedRows = computed(() => rows.value.filter((r) => checkedKeys.value.includes(String(r.index))));

  const columns = [
    { title: '日期', dataIndex: 'billDate', width: 110 },
    { title: '类型', dataIndex: 'type', width: 70 },
    { title: '分类', dataIndex: 'categoryName', width: 100 },
    { title: '金额', dataIndex: 'amount', width: 100 },
    { title: '支付方式', dataIndex: 'paymentMethod', width: 100 },
    { title: '备注', dataIndex: 'remark', width: 200 },
    {
      title: '状态',
      key: 'status',
      dataIndex: 'status',
      width: 100,
    },
  ];

  function onCheckedChange(keys: string[]) {
    checkedKeys.value = keys;
  }

  async function handleFile(file: File) {
    try {
      const res: any = await defHttp.uploadFile(
        { url: '/homeai/bill/import/preview' },
        { file, name: 'file', data: { type: fileType.value } },
        { isReturnResponse: true }
      );
      if (!res || res.success !== true || res.code !== 200) {
        throw new Error(res?.message || '解析失败');
      }
      rows.value = (res?.result as BillImportRow[] | undefined) || [];
      checkedKeys.value = rows.value.filter((r) => !r.duplicate).map((r) => String(r.index));
      message.value = `解析到 ${rows.value.length} 条记录，其中重复 ${rows.value.filter((r) => r.duplicate).length} 条（默认不勾选）。`;
    } catch (e: any) {
      createMessage.error(e?.message || '解析失败');
    }
    return false;
  }

  async function confirmImport() {
    const entries = checkedRows.value.map((r) => ({
      billDate: r.billDate,
      type: r.type,
      categoryId: r.categoryId,
      amount: r.amount,
      remark: r.remark,
      paymentMethod: r.paymentMethod,
    }));
    if (entries.length === 0) {
      createMessage.warning('请先勾选要导入的记录');
      return;
    }
    if (!importUserId.value) {
      createMessage.warning('请选择账单归属用户');
      return;
    }
    createConfirm({
      title: '确认导入',
      content: `确认将 ${entries.length} 条账单导入到所选用户？`,
      onOk: async () => {
        importing.value = true;
        try {
          const res: any = await defHttp.post({
            url: '/homeai/bill/import/confirm',
            data: { entries, userId: importUserId.value },
          });
          createMessage.success(res?.message || '导入完成');
          rows.value = [];
          checkedKeys.value = [];
        } finally {
          importing.value = false;
        }
      },
    });
  }
</script>
