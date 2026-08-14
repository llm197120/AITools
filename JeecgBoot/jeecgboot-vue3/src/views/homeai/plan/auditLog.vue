<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button preIcon="ant-design:reload-outlined" type="primary" @click="reload">刷新</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'result'">
          <a-tag :color="record.result === 'success' ? 'green' : 'red'">{{ record.result || '-' }}</a-tag>
        </template>
      </template>
    </BasicTable>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-audit-log" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, useTable } from '/@/components/Table';
  import { auditApi } from '/@/api/homeai';
  import type { HomeaiPageParams } from '/@/api/homeai';

  const moduleOptions = [
    { label: '计划', value: 'plan' },
    { label: '存储', value: 'storage' },
    { label: '菜谱', value: 'recipe' },
    { label: '学习', value: 'learn' },
    { label: 'AI', value: 'ai' },
  ];

  const actionOptions = [
    { label: '补跑重复实例', value: 'plan_repeat_roll_forward' },
    { label: '资料移入回收站', value: 'storage_move_recycle' },
    { label: '资料恢复', value: 'storage_restore' },
    { label: '资料彻底删除', value: 'storage_delete_permanently' },
    { label: '菜谱彻底删除', value: 'recipe_delete_permanently' },
    { label: '学习资料彻底删除', value: 'learn_delete_permanently' },
    { label: '生成文档', value: 'ai_generate' },
  ];

  const [registerTable, { reload }] = useTable({
    title: '操作审计日志',
    api: (params: HomeaiPageParams) => auditApi.logs(params),
    columns: [
      { title: '时间', dataIndex: 'createTime', width: 170 },
      { title: '模块', dataIndex: 'module', width: 90 },
      { title: '操作', dataIndex: 'actionType', width: 120 },
      { title: '操作人', dataIndex: 'userId', width: 140 },
      { title: '摘要', dataIndex: 'targetSummary', width: 260 },
      { title: '结果', dataIndex: 'result', key: 'result', width: 90 },
      { title: 'IP', dataIndex: 'ipAddress', width: 130 },
    ],
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    formConfig: {
      labelWidth: 80,
      schemas: [
        { field: 'module', label: '模块', component: 'Select', componentProps: { options: moduleOptions, allowClear: true } },
        { field: 'actionType', label: '操作类型', component: 'Select', componentProps: { options: actionOptions, allowClear: true } },
      ],
    },
  });
</script>
