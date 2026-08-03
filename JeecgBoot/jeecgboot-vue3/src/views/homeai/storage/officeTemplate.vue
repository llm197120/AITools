<template>
  <div style="padding: 16px">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd"> 新增模板</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'isDefault'">
          <a-tag v-if="record.isDefault === '1'" color="blue">默认</a-tag>
          <a-button v-else size="small" type="link" @click="setDefault(record)">设为默认</a-button>
        </template>
      </template>
    </BasicTable>
  </div>
</template>

<script lang="ts" name="homeai-storage-template" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { defHttp } from '/@/utils/http/axios';

  const [registerTable, { reload }] = useTable({
    title: '文档模板管理',
    api: (params: any) => defHttp.get({ url: '/homeai/storage/template/list', params }),
    columns: [
      { title: '模板名称', dataIndex: 'name', width: 200 },
      { title: '类型', dataIndex: 'type', width: 80 },
      { title: '默认', dataIndex: 'isDefault', key: 'isDefault', width: 80 },
      { title: '备注', dataIndex: 'remark', width: 200 },
      { title: '创建时间', dataIndex: 'createTime', width: 160 },
    ],
    useSearchForm: false,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  function handleAdd() { /* 暂未实现抽屉 */ }

  function getTableAction(record: any) {
    return [
      { icon: 'ant-design:edit-outlined', onClick: () => handleEdit(record), title: '编辑' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleDelete(record), title: '删除', color: 'error' },
    ];
  }

  function handleEdit(record: any) { /* TODO */ }

  async function handleDelete(record: any) {
    await defHttp.delete({ url: `/homeai/storage/template/${record.id}` });
    reload();
  }

  async function setDefault(record: any) {
    await defHttp.put({ url: `/homeai/storage/template/${record.id}/default` });
    reload();
  }
</script>
