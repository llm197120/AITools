<template>
  <div style="padding: 16px">
    <BasicTable @register="registerTable">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag v-if="record.status === 'COMPLETED'" color="success">已完成</a-tag>
          <a-tag v-else-if="record.status === 'PROCESSING'" color="processing">处理中</a-tag>
          <a-tag v-else-if="record.status === 'PENDING'" color="warning">等待中</a-tag>
          <a-tag v-else color="error">失败</a-tag>
        </template>
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" name="homeai-office-history" setup>
  import { BasicTable, useTable } from '/@/components/Table';
  import { defHttp } from '/@/utils/http/axios';

  const [registerTable] = useTable({
    title: 'Office处理记录',
    api: (params: any) => defHttp.get({ url: '/homeai/storage/office/list', params }),
    columns: [
      { title: '文件ID', dataIndex: 'fileId', width: 250 },
      { title: '处理类型', dataIndex: 'convertType', width: 120 },
      { title: '源格式', dataIndex: 'sourceFormat', width: 100 },
      { title: '目标格式', dataIndex: 'targetFormat', width: 100 },
      { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
      { title: '耗时(s)', dataIndex: 'taskDuration', width: 80 },
      { title: '失败原因', dataIndex: 'errorMessage', width: 200 },
      { title: '创建时间', dataIndex: 'createTime', width: 160 },
    ],
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    formConfig: {
      schemas: [
        { field: 'convertType', label: '处理类型', component: 'Select', componentProps: { options: [{ label: '格式转换', value: 'format_convert' }, { label: 'AI生成', value: 'ai_generate' }] } },
        { field: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '已完成', value: 'COMPLETED' }, { label: '处理中', value: 'PROCESSING' }, { label: '等待中', value: 'PENDING' }, { label: '失败', value: 'FAILED' }] } },
      ],
    },
  });
</script>
