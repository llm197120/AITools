<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="用户列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <a-alert
      v-if="listFailed"
      type="error"
      show-icon
      message="列表加载失败，当前可能不是最新数据"
      style="margin-bottom: 12px"
    >
      <template #action>
        <a-button size="small" @click="reload">重试</a-button>
      </template>
    </a-alert>
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
  import { onMounted, ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { userApi, familyApi } from '/@/api/homeai';
import { toFamilySelectOptions } from '../utils/activeFamily';
  import type { HomeaiPageParams, HomeaiUser } from '/@/api/homeai';
  import { useHomeaiRecycleBin } from '../hooks/useHomeaiRecycleBin';
  import { useHomeaiListLoad } from '../hooks/useHomeaiListLoad';
  import UserDrawer from './UserDrawer.vue';

  const { createMessage, createConfirm } = useMessage();
  const { listFailed, wrapListApi } = useHomeaiListLoad();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const activeTab = ref('list');
  const familyOptions = ref<any[]>([]);

  // 加载家庭下拉（用于按家庭筛选）
  async function loadFamilyOptions() {
    try {
      const res: any = await familyApi.list({ pageNo: 1, pageSize: 1000 });
      familyOptions.value = toFamilySelectOptions(res);
    } catch {
      createMessage.warning('家庭列表加载失败');
    }
  }

  onMounted(() => {
    loadFamilyOptions();
  });

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
    api: (params: HomeaiPageParams) =>
      wrapListApi((p) => (activeTab.value === 'list' ? userApi.list(p) : userApi.recycleBin(p)))(params),
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
      width: 240,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  const { rowSelection, selectedRowKeys, clearSelection, handleMoveToRecycleBin, handleBatchMoveToRecycleBin, handleRestore, handleBatchRestore, handleDeletePermanently, handleBatchDeletePermanently } = useHomeaiRecycleBin({
    api: {
      moveToRecycleBin: (ids: string[]) => userApi.moveToRecycleBin(ids),
      restore: (ids: string[]) => userApi.restore(ids),
      deletePermanently: (ids: string[]) => userApi.deletePermanently(ids),
    },
    reload,
    entityName: '用户',
    nameField: 'nickname',
    permanentWarn: '此操作不可恢复！',
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    clearSelection();
    reload();
  }

  function getTableAction(record: HomeaiUser) {
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
        icon: 'ant-design:key-outlined',
        onClick: () => handleResetPassword(record),
        title: '重置密码',
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
    handleExportXls('用户导入模板', userApi.exportTemplate);
  }

  async function handleToggleStatus(record: HomeaiUser) {
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

  async function handleResetPassword(record: HomeaiUser) {
    createConfirm({
      iconType: 'warning',
      title: '重置密码',
      content: `确定将用户「${record.nickname}」的密码重置为默认密码 123456 吗？重置后需重新登录。`,
      onOk: async () => {
        await userApi.resetPassword(record.id);
        createMessage.success('密码已重置为 123456');
      },
    });
  }

  function handleSuccess() {
    reload();
  }
</script>
