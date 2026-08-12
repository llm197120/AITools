<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="用户列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, userApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" @click="handleExportXls('微信用户列表', userApi.exportXls)">导出</a-button>
        <a-button v-if="activeTab === 'list' && selectedRowKeys.length > 0" preIcon="ant-design:delete-outlined" type="primary" danger @click="handleBatchMoveToRecycleBin">
          移入回收站({{ selectedRowKeys.length }})
        </a-button>
        <a-button v-if="activeTab === 'recycle' && selectedRowKeys.length > 0" preIcon="ant-design:undo-outlined" @click="handleBatchRestore">
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
          <a-tag :color="record.status === '1' ? 'green' : 'red'">
            {{ record.status === '1' ? '正常' : '禁用' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'familyName'">
          <span>{{ record.familyName || '无' }}</span>
        </template>
      </template>
    </BasicTable>
    <UserDrawer @register="registerDrawer" @success="handleSuccess" />
  </PageWrapper>
</template>

<script lang="ts" name="homeai-user" setup>
  import { PageWrapper } from '/@/components/Page';
  import { computed, onMounted, ref, watch } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { useUserStore } from '/@/store/modules/user';
  import { userApi, familyApi } from '/@/api/homeai';
  import UserDrawer from './UserDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const selectedRowKeys = ref<string[]>([]);
  const activeTab = ref('list');
  const familyOptions = ref<any[]>([]);

  // 加载家庭下拉（用于按家庭筛选）
  async function loadFamilyOptions() {
    try {
      const res: any = await familyApi.list({ pageNo: 1, pageSize: 1000 });
      familyOptions.value = (res?.records || res || []).map((f: any) => ({ label: f.name, value: f.id }));
    } catch {
      familyOptions.value = [];
    }
  }

  onMounted(() => {
    loadFamilyOptions();
  });

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const columns = [
    { title: '微信昵称', dataIndex: 'nickname', width: 150 },
      { title: 'openid', dataIndex: 'openid', width: 200, customRender: ({ text }: any) => (text ? text.substring(0, 3) + '****' + text.substring(text.length - 4) : '-') },
    { title: '手机号', dataIndex: 'phone', width: 120 },
    { title: '所属家庭', dataIndex: 'familyName', key: 'familyName', width: 150 },
    { title: '家庭角色', dataIndex: 'familyRole', width: 100 },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80 },
    { title: '注册时间', dataIndex: 'createTime', width: 180 },
    { title: '最后登录', dataIndex: 'lastLoginTime', width: 180 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '微信用户列表',
    api: (params: any) => activeTab.value === 'list' ? userApi.list(params) : userApi.recycleBin(params),
    columns: columns,
    useSearchForm: true,
    formConfig: {
      schemas: [
        { field: 'nickname', label: '昵称', component: 'Input' },
        { field: 'phone', label: '手机号', component: 'Input' },
        {
          field: 'familyId',
          label: '所属家庭',
          component: 'Select',
          componentProps: { options: familyOptions, allowClear: true },
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
        onClick: () => openDrawer(true, { record, isUpdate: false }),
        title: '查看',
      },
      {
        icon: 'ant-design:edit-outlined',
        onClick: () => openDrawer(true, { record, isUpdate: true }),
        title: '编辑',
      },
      {
        icon: record.status === '1' ? 'ant-design:stop-outlined' : 'ant-design:check-circle-outlined',
        onClick: () => handleToggleStatus(record),
        title: record.status === '1' ? '禁用' : '启用',
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
    window.open(`/jeecg-boot/homeai/user/exportTemplate?token=${encodeURIComponent(token)}`, '_blank');
  }

  async function handleToggleStatus(record: any) {
    const newStatus = record.status === '1' ? '0' : '1';
    const label = newStatus === '1' ? '启用' : '禁用';
    createConfirm({
      iconType: 'warning',
      title: '确认操作',
      content: `确定要${label}用户「${record.nickname}」吗？`,
      onOk: async () => {
        await userApi.updateStatus(record.id, newStatus);
        createMessage.success(`${label}成功`);
        reload();
      },
    });
  }

  async function handleMoveToRecycleBin(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将用户「${record.nickname}」移入回收站吗？`,
      onOk: async () => {
        await userApi.moveToRecycleBin([record.id]);
        createMessage.success('已移入回收站');
        reload();
      },
    });
  }

  async function handleBatchMoveToRecycleBin() {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将选中的 ${selectedRowKeys.value.length} 个用户移入回收站吗？`,
      onOk: async () => {
        await userApi.moveToRecycleBin(selectedRowKeys.value);
        createMessage.success('已移入回收站');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  async function handleRestore(record: any) {
    await userApi.restore([record.id]);
    createMessage.success('恢复成功');
    reload();
  }

  async function handleBatchRestore() {
    await userApi.restore(selectedRowKeys.value);
    createMessage.success('恢复成功');
    selectedRowKeys.value = [];
    reload();
  }

  async function handleDeletePermanently(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除用户「${record.nickname}」吗？此操作不可恢复！`,
      onOk: async () => {
        await userApi.deletePermanently([record.id]);
        createMessage.success('已彻底删除');
        reload();
      },
    });
  }

  async function handleBatchDeletePermanently() {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除选中的 ${selectedRowKeys.value.length} 个用户吗？此操作不可恢复！`,
      onOk: async () => {
        await userApi.deletePermanently(selectedRowKeys.value);
        createMessage.success('已彻底删除');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }

  function handleSuccess() {
    reload();
  }
</script>
