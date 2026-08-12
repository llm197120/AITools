<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd"> 新增规则</a-button>
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
      </template>
    </BasicTable>
    <ConvertRuleDrawer @register="registerDrawer" @success="reload" />
  </PageWrapper>
</template>

<script lang="ts" name="homeai-convert-rule" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { defHttp } from '/@/utils/http/axios';
  import ConvertRuleDrawer from './ConvertRuleDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const [registerDrawer, { openDrawer }] = useDrawer();

  const [registerTable, { reload }] = useTable({
    title: '格式转换规则',
    api: (params: any) => defHttp.get({ url: '/homeai/storage/rule/list', params }),
    columns: [
      { title: '源格式', dataIndex: 'sourceFormat', width: 150 },
      { title: '目标格式', dataIndex: 'targetFormat', width: 150 },
      { title: '状态', dataIndex: 'isEnabled', key: 'isEnabled', width: 80 },
      { title: '创建时间', dataIndex: 'createTime', width: 160 },
    ],
    useSearchForm: false,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 180,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  function getTableAction(record: any) {
    return [
      { icon: 'ant-design:eye-outlined', onClick: () => openDrawer(true, { record, isUpdate: false }), title: '查看' },
      { icon: 'ant-design:edit-outlined', onClick: () => handleEdit(record), title: '编辑' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleDelete(record), title: '删除', color: 'error' },
    ];
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: false });
  }

  function handleEdit(record: any) {
    openDrawer(true, { record, isUpdate: true });
  }

  async function handleDelete(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定要删除规则「${record.sourceFormat} → ${record.targetFormat}」吗？`,
      onOk: async () => {
        await defHttp.delete({ url: `/homeai/storage/rule/${record.id}` });
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  async function toggleStatus(record: any, checked: boolean) {
    await defHttp.put(
      {
        url: `/homeai/storage/rule/${record.id}/status`,
        params: { isEnabled: checked ? '1' : '0' },
      },
      { joinParamsToUrl: true }
    );
    reload();
  }
</script>
