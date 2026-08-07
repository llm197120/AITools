<template>
  <div style="padding: 16px">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="家庭列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, familyApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined" type="primary">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" type="primary" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" type="primary" @click="handleExportXls('家庭列表', familyApi.exportXls)">导出</a-button>
        <a-button v-if="activeTab === 'list' && selectedRowKeys.length > 0" preIcon="ant-design:delete-outlined" type="primary" danger @click="handleBatchMoveToRecycleBin">
          移入回收站({{ selectedRowKeys.length }})
        </a-button>
        <a-button v-if="activeTab === 'recycle' && selectedRowKeys.length > 0" preIcon="ant-design:undo-outlined" type="primary" @click="handleBatchRestore">
          恢复({{ selectedRowKeys.length }})
        </a-button>
        <a-button v-if="activeTab === 'recycle' && selectedRowKeys.length > 0" preIcon="ant-design:delete-outlined" type="primary" danger @click="handleBatchDeletePermanently">
          彻底删除({{ selectedRowKeys.length }})
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'disbanded' ? 'orange' : 'green'">
            {{ record.status === 'disbanded' ? '已解散' : '正常' }}
          </a-tag>
        </template>
      </template>
    </BasicTable>
    <FamilyDrawer @register="registerDrawer" @success="handleSuccess" />
    <FamilyMembersDrawer @register="registerMembersDrawer" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" name="homeai-family" setup>
  import { computed, ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { useUserStore } from '/@/store/modules/user';
  import { familyApi } from '/@/api/homeai';
  import FamilyDrawer from './FamilyDrawer.vue';
  import FamilyMembersDrawer from './FamilyMembersDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const [registerMembersDrawer, { openDrawer: openMembersDrawer }] = useDrawer();
  const selectedRowKeys = ref<string[]>([]);
  const activeTab = ref('list');

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const columns = [
    { title: '家庭名称', dataIndex: 'name', width: 150 },
    { title: '创建者ID', dataIndex: 'creatorId', width: 150 },
    { title: '成员数量', dataIndex: 'memberCount', width: 80 },
{ title: '状态', dataIndex: 'status', key: 'status', width: 80 },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '家庭列表',
    api: (params: any) => activeTab.value === 'list' ? familyApi.list(params) : familyApi.recycleBin(params),
    columns: columns,
    useSearchForm: true,
    formConfig: {
      schemas: [
        { field: 'name', label: '家庭名称', component: 'Input' },
        {
          field: 'status',
          label: '状态',
          component: 'Select',
          componentProps: {
            options: [
              { label: '正常', value: 'normal' },
              { label: '已解散', value: 'disbanded' },
            ],
            allowClear: true,
          },
        },
      ],
    },
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 200,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    selectedRowKeys.value = [];
    reload();
  }

  function getTableAction(record: any) {
    if (activeTab.value === 'recycle') {
      return [
        {
          icon: 'ant-design:undo-outlined',
          onClick: () => handleRestore(record),
          title: '恢复',
        },
        {
          icon: 'ant-design:delete-outlined',
          onClick: () => handleDeletePermanently(record),
          title: '彻底删除',
          color: 'error',
        },
      ];
    }
    return [
      {
        icon: 'ant-design:eye-outlined',
        onClick: () => openDrawer(true, { record }),
        title: '查看',
      },
      {
        icon: 'ant-design:team-outlined',
        onClick: () => openMembersDrawer(true, { familyId: record.id, familyName: record.name }),
        title: '成员',
      },
      {
        icon: 'ant-design:delete-outlined',
        onClick: () => handleMoveToRecycleBin(record),
        title: '移入回收站',
        color: 'error',
      },
    ];
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: true, record: {} });
  }

  function handleDownloadTemplate() {
    const token = useUserStore().getToken;
    window.open(`/jeecg-boot/homeai/family/exportTemplate?token=${encodeURIComponent(token)}`, '_blank');
  }

  function handleSuccess() {
    reload();
  }

  async function handleMoveToRecycleBin(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将家庭「${record.name}」移入回收站吗？`,
      onOk: async () => {
        await familyApi.moveToRecycleBin([record.id]);
        createMessage.success('已移入回收站');
        reload();
      },
    });
  }

  async function handleBatchMoveToRecycleBin() {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将选中的 ${selectedRowKeys.value.length} 个家庭移入回收站吗？`,
      onOk: async () => {
        await familyApi.moveToRecycleBin(selectedRowKeys.value);
        createMessage.success('已移入回收站');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  async function handleRestore(record: any) {
    await familyApi.restore([record.id]);
    createMessage.success('恢复成功');
    reload();
  }

  async function handleBatchRestore() {
    await familyApi.restore(selectedRowKeys.value);
    createMessage.success('恢复成功');
    selectedRowKeys.value = [];
    reload();
  }

  async function handleDeletePermanently(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除家庭「${record.name}」吗？此操作不可恢复！`,
      onOk: async () => {
        await familyApi.deletePermanently([record.id]);
        createMessage.success('已彻底删除');
        reload();
      },
    });
  }

  async function handleBatchDeletePermanently() {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除选中的 ${selectedRowKeys.value.length} 个家庭吗？此操作不可恢复！`,
      onOk: async () => {
        await familyApi.deletePermanently(selectedRowKeys.value);
        createMessage.success('已彻底删除');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }
</script>
