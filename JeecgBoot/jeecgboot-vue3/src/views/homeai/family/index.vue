<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="家庭列表" />
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
        <a-upload v-if="activeTab === 'list'" name="file" :showUploadList="false" :customRequest="(file) => handleImportXls(file, familyApi.importExcel, reload)">
          <a-button preIcon="ant-design:import-outlined">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" @click="handleExportXls('家庭列表', familyApi.exportXls)">导出</a-button>
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
          <a-tag :color="record.status === 'disbanded' ? 'orange' : 'green'">
            {{ record.status === 'disbanded' ? '已解散' : '正常' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'creatorId' || column.dataIndex === 'creatorId'">
          {{ resolveUserLabel(record.creatorId) }}
        </template>
      </template>
    </BasicTable>
    <FamilyDrawer @register="registerDrawer" @success="handleSuccess" />
    <FamilyMembersDrawer @register="registerMembersDrawer" @success="handleSuccess" />
    <a-modal
      v-model:open="quotaVisible"
      title="家庭存储配额"
      :confirm-loading="quotaSaving"
      @ok="saveFamilyQuota"
    >
      <a-form layout="vertical">
        <a-form-item label="家庭">
          <a-input :value="quotaForm.familyName" disabled />
        </a-form-item>
        <a-form-item label="配额 (GB)">
          <a-input-number v-model:value="quotaForm.limitGb" :min="0.1" :max="100" :step="0.5" style="width: 100%" />
          <div style="margin-top: 8px; font-size: 12px; color: #888">
            {{ quotaForm.custom ? '当前为自定义配额' : '当前使用默认家庭配额' }}；设为 0 可在下方恢复默认
          </div>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="quotaVisible = false">取消</a-button>
        <a-button v-if="quotaForm.custom" danger :loading="quotaSaving" @click="clearFamilyQuota">恢复默认</a-button>
        <a-button type="primary" :loading="quotaSaving" @click="saveFamilyQuota">保存</a-button>
      </template>
    </a-modal>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-family" setup>
  import { PageWrapper } from '/@/components/Page';
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { familyApi, storageApi } from '/@/api/homeai';
  import type { HomeaiFamily, HomeaiPageParams } from '/@/api/homeai';
  import FamilyDrawer from './FamilyDrawer.vue';
  import FamilyMembersDrawer from './FamilyMembersDrawer.vue';
  import { useUserLabel } from '../hooks/useUserLabel';
import { useHomeaiRecycleBin } from '../hooks/useHomeaiRecycleBin';
import { useHomeaiListLoad } from '../hooks/useHomeaiListLoad';

  const { createMessage } = useMessage();
  const { listFailed, wrapListApi } = useHomeaiListLoad();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const [registerMembersDrawer, { openDrawer: openMembersDrawer }] = useDrawer();
  const { loadUserOptions, resolveUserLabel } = useUserLabel();
  loadUserOptions();
  const activeTab = ref('list');
  const quotaVisible = ref(false);
  const quotaSaving = ref(false);
  const quotaForm = ref({ familyId: '', familyName: '', limitGb: 5, custom: false });
  const GB = 1024 * 1024 * 1024;

  const columns = [
    { title: '家庭名称', dataIndex: 'name', width: 150 },
    { title: '创建者', dataIndex: 'creatorId', key: 'creatorId', width: 150 },
    { title: '成员数量', dataIndex: 'memberCount', width: 80 },
{ title: '状态', dataIndex: 'status', key: 'status', width: 80 },
    { title: '创建时间', dataIndex: 'createTime', width: 180 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '家庭列表',
    api: (params: HomeaiPageParams) =>
      wrapListApi((p) => (activeTab.value === 'list' ? familyApi.list(p) : familyApi.recycleBin(p)))(params),
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
      width: 260,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  const { rowSelection, selectedRowKeys, clearSelection, handleMoveToRecycleBin, handleBatchMoveToRecycleBin, handleRestore, handleBatchRestore, handleDeletePermanently, handleBatchDeletePermanently } = useHomeaiRecycleBin({
    api: {
      moveToRecycleBin: (ids: string[]) => familyApi.moveToRecycleBin(ids),
      restore: (ids: string[]) => familyApi.restore(ids),
      deletePermanently: (ids: string[]) => familyApi.deletePermanently(ids),
    },
    reload,
    entityName: '家庭',
    nameField: 'name',
    permanentWarn: '此操作不可恢复！',
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    clearSelection();
    reload();
  }

  function getTableAction(record: HomeaiFamily) {
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
        icon: 'ant-design:cloud-server-outlined',
        onClick: () => handleFamilyQuota(record),
        title: '存储配额',
      },
      {
        icon: 'ant-design:delete-outlined',
        onClick: () => handleMoveToRecycleBin(record),
        title: '移入回收站',
        color: 'error',
      },
    ];
  }

  async function handleFamilyQuota(record: HomeaiFamily) {
    try {
      const cfg: any = await storageApi.getFamilyStorageLimit(record.id);
      quotaForm.value = {
        familyId: record.id,
        familyName: record.name,
        limitGb: Number(((cfg?.limitBytes || 5 * GB) / GB).toFixed(2)),
        custom: !!cfg?.custom,
      };
    } catch {
      quotaForm.value = {
        familyId: record.id,
        familyName: record.name,
        limitGb: 5,
        custom: false,
      };
    }
    quotaVisible.value = true;
  }

  async function saveFamilyQuota() {
    const gb = Number(quotaForm.value.limitGb);
    if (!quotaForm.value.familyId || !(gb > 0)) {
      createMessage.warning('请输入有效配额（GB）');
      return;
    }
    quotaSaving.value = true;
    try {
      await storageApi.setFamilyStorageLimit(quotaForm.value.familyId, Math.round(gb * GB));
      createMessage.success('家庭配额已更新');
      quotaVisible.value = false;
    } finally {
      quotaSaving.value = false;
    }
  }

  async function clearFamilyQuota() {
    if (!quotaForm.value.familyId) return;
    quotaSaving.value = true;
    try {
      await storageApi.clearFamilyStorageLimit(quotaForm.value.familyId);
      createMessage.success('已恢复默认家庭配额');
      quotaVisible.value = false;
    } finally {
      quotaSaving.value = false;
    }
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: true, record: {} });
  }

  function handleDownloadTemplate() {
    handleExportXls('家庭导入模板', familyApi.exportTemplate);
  }

  function handleSuccess() {
    reload();
  }
</script>
