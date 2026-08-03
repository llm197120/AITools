<template>
  <div style="padding: 16px">
    <a-card :bordered="false" style="margin-bottom: 16px">
      <a-space>
        <a-radio-group v-model:value="fileType">
          <a-radio-button value="wechat_csv">微信支付CSV</a-radio-button>
          <a-radio-button value="excel">Excel账单</a-radio-button>
        </a-radio-group>
        <a-upload
          :show-upload-list="false"
          :before-upload="handleFile"
          accept=".csv,.xlsx,.xls"
        >
          <a-button type="primary">选择文件并解析</a-button>
        </a-upload>
        <a-button v-if="rows.length > 0" type="primary" danger :loading="importing" @click="confirmImport">确认导入选中({{ checkedRows.length }})</a-button>
      </a-space>
    </a-card>

    <a-alert
      v-if="message"
      :message="message"
      type="info"
      show-icon
      closable
      style="margin-bottom: 16px"
    />

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
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" name="homeai-bill-import" setup>
  import { ref, computed } from 'vue';
  import { BasicTable } from '/@/components/Table';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { defHttp } from '/@/utils/http/axios';

  const { createMessage, createConfirm } = useMessage();
  const fileType = ref('wechat_csv');
  const rows = ref<any[]>([]);
  const checkedKeys = ref<string[]>([]);
  const importing = ref(false);
  const message = ref('');

  const checkedRows = computed(() => rows.value.filter((r) => checkedKeys.value.includes(String(r.index))));

  const columns = [
    { title: '日期', dataIndex: 'billDate', width: 110 },
    { title: '类型', dataIndex: 'type', width: 70, customRender: ({ text }: any) => (text === 'income' ? '收入' : '支出') },
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

  function onCheckedChange(keys: any[]) {
    checkedKeys.value = keys;
  }

  async function handleFile(file: File) {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('type', fileType.value);
    try {
      const res: any = await defHttp.uploadFile(
        { url: '/homeai/bill/import/preview', data: formData },
        { isTransformResponse: true },
      );
      rows.value = (res as any[]) || [];
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
    createConfirm({
      title: '确认导入',
      content: `确认导入 ${entries.length} 条账单记录？`,
      onOk: async () => {
        importing.value = true;
        try {
          const res: any = await defHttp.post({ url: '/homeai/bill/import/confirm', data: { entries } });
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
