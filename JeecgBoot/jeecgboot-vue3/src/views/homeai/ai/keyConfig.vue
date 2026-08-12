<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd"> 新增</a-button>
        <a-button preIcon="ant-design:export-outlined" @click="onExportXls"> 导出</a-button>
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
  </PageWrapper>
</template>

<script lang="ts" name="homeai-ai-key" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { keyConfigApi } from '/@/api/homeai';
  import KeyConfigDrawer from './KeyConfigDrawer.vue';

  const { createConfirm, createMessage } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  const [registerTable, { reload }] = useTable({
    title: 'AI密钥配置',
    api: (params) => keyConfigApi.list(params),
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

  function getTableAction(record: Recordable) {
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

  function handleDelete(record: Recordable) {
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除密钥配置「${record.provider || record.modelName || record.id}」吗？`,
      onOk: async () => {
        await keyConfigApi.delete(String(record.id));
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  async function toggleStatus(record: Recordable, checked: boolean) {
    await keyConfigApi.toggleStatus(String(record.id), checked ? '1' : '0');
    reload();
  }

  async function setDefault(record: Recordable) {
    await keyConfigApi.setDefault(String(record.id));
    reload();
  }

  function onExportXls() {
    // TODO
  }
</script>
