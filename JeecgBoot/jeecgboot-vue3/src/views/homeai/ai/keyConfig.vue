<template>
  <div style="padding: 16px">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd"> 新增</a-button>
        <a-button type="primary" preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'isEnabled'">
          <a-switch checkedChildren="启用" unCheckedChildren="停用"
            :checked="record.isEnabled === '1'"
            @change="(checked: boolean) => toggleStatus(record, checked)" />
        </template>
        <template v-if="column.key === 'isDefault'">
          <a-tag v-if="record.isDefault === '1'" color="blue">默认</a-tag>
          <a-button v-else size="small" type="link" @click="setDefault(record)">设为默认</a-button>
        </template>
        <template v-if="column.key === 'apiKeyEncrypted'">
          <span>{{ record.apiKeyEncrypted || '---' }}</span>
        </template>
      </template>
    </BasicTable>
    <KeyConfigDrawer @register="registerDrawer" @success="reload" />
  </div>
</template>

<script lang="ts" name="homeai-ai-key" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { defHttp } from '/@/utils/http/axios';
  import KeyConfigDrawer from './KeyConfigDrawer.vue';

  const [registerDrawer, { openDrawer }] = useDrawer();

  const [registerTable, { reload }] = useTable({
    title: 'AI密钥配置',
    api: (params: any) => defHttp.get({ url: '/homeai/ai/key-config/list', params }),
    columns: [
      { title: '提供商', dataIndex: 'provider', width: 120 },
      { title: '模型名', dataIndex: 'modelName', width: 150 },
      { title: 'API Key', dataIndex: 'apiKeyEncrypted', key: 'apiKeyEncrypted', width: 200 },
      { title: 'API地址', dataIndex: 'apiBaseUrl', width: 200 },
      { title: '默认', dataIndex: 'isDefault', key: 'isDefault', width: 80 },
      { title: '状态', dataIndex: 'isEnabled', key: 'isEnabled', width: 80 },
      { title: '排序', dataIndex: 'sortOrder', width: 60 },
      { title: '备注', dataIndex: 'remark', width: 150 },
    ],
    useSearchForm: false,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 120,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  function getTableAction(record: any) {
    return [
      {
        icon: 'ant-design:eye-outlined',
        onClick: () => openDrawer(true, { record, isUpdate: false }),
        title: '查看',
      },
      { icon: 'ant-design:edit-outlined', onClick: () => openDrawer(true, { record, isUpdate: true }), title: '编辑' },
      {
        icon: 'ant-design:delete-outlined',
        onClick: () => handleDelete(record),
        title: '删除',
        color: 'error',
      },
    ];
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: false });
  }

  async function handleDelete(record: any) {
    await defHttp.delete({ url: `/homeai/ai/key-config/${record.id}` });
    reload();
  }

  async function toggleStatus(record: any, checked: boolean) {
    await defHttp.put({ url: `/homeai/ai/key-config/${record.id}/status`, params: { isEnabled: checked ? '1' : '0' } }, { joinParamsToUrl: true });
    reload();
  }

  async function setDefault(record: any) {
    await defHttp.put({ url: `/homeai/ai/key-config/${record.id}/default` });
    reload();
  }

  function onExportXls() {
    // TODO
  }
</script>
