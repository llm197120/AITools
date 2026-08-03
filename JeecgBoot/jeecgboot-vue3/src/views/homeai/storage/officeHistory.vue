<template>
  <div style="padding: 16px">
    <BasicTable @register="registerTable">
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
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
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage, createConfirm } = useMessage();
  const [registerTable, { reload }] = useTable({
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
    actionColumn: {
      width: 120,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
    formConfig: {
      schemas: [
        { field: 'convertType', label: '处理类型', component: 'Select', componentProps: { options: [{ label: '格式转换', value: 'format_convert' }, { label: 'AI生成', value: 'ai_generate' }] } },
        { field: 'status', label: '状态', component: 'Select', componentProps: { options: [{ label: '已完成', value: 'COMPLETED' }, { label: '处理中', value: 'PROCESSING' }, { label: '等待中', value: 'PENDING' }, { label: '失败', value: 'FAILED' }] } },
      ],
    },
  });

  function getTableAction(record: any) {
    return [
      {
        icon: 'ant-design:download-outlined',
        title: '下载结果',
        onClick: () => {
          if (record.resultFileUrl) {
            window.open(record.resultFileUrl, '_blank');
          } else {
            createMessage.warning('该记录暂无结果文件');
          }
        },
      },
      { icon: 'ant-design:delete-outlined', title: '删除', color: 'error', onClick: () => handleDelete(record) },
    ];
  }

  function handleDelete(record: any) {
    createConfirm({
      title: '确认删除',
      content: '确定删除该处理记录吗？',
      onOk: async () => {
        await defHttp.delete({ url: `/homeai/storage/office/${record.id}` });
        createMessage.success('删除成功');
        reload();
      },
    });
  }
</script>
