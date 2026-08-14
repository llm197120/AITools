<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="菜谱列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-upload
          v-if="activeTab === 'list'"
          name="file"
          :showUploadList="false"
          :customRequest="(file) => handleImportXls(file, recipeApi.importExcel, reload)"
        >
          <a-button preIcon="ant-design:import-outlined">导入</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" @click="handleDownloadTemplate">下载模板</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:picture-outlined" @click="openCoverImport">批量导入封面</a-button>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:export-outlined" @click="handleExportXls('菜谱列表', recipeApi.exportXls)"
          >导出</a-button
        >
        <a-button
          v-if="activeTab === 'list' && selectedRowKeys.length > 0"
          preIcon="ant-design:delete-outlined"
          type="primary"
          danger
          @click="handleBatchMoveToRecycleBin"
        >
          移入回收站({{ selectedRowKeys.length }})
        </a-button>
        <a-button v-if="activeTab === 'recycle' && selectedRowKeys.length > 0" preIcon="ant-design:undo-outlined" @click="handleBatchRestore">
          恢复({{ selectedRowKeys.length }})
        </a-button>
        <a-button
          v-if="activeTab === 'recycle' && selectedRowKeys.length > 0"
          preIcon="ant-design:delete-outlined"
          type="primary"
          danger
          @click="handleBatchDeletePermanently"
        >
          彻底删除({{ selectedRowKeys.length }})
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'difficulty'">
          <a-tag :color="difficultyColor(record.difficulty)">{{ difficultyLabel(record.difficulty) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'userId' || column.dataIndex === 'userId'">
          {{ resolveUserLabel(record.userId) }}
        </template>
      </template>
    </BasicTable>
    <RecipeDrawer @register="registerDrawer" @success="handleSuccess" />

    <BasicModal v-model:open="coverImportOpen" title="批量导入菜谱封面" width="640px" :footer="null">
      <a-alert
        message="按图片文件名（去掉扩展名）匹配菜谱名称，例如「红烧肉.jpg」会更新名为「红烧肉」的菜谱封面；同名的所有菜谱都会更新。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-upload multiple :before-upload="onCollectCovers" :show-upload-list="false" :accept="'image/*'">
        <a-button preIcon="ant-design:plus-outlined">选择封面图片（可多选）</a-button>
      </a-upload>
      <div v-if="coverFiles.length" style="margin-top: 12px">
        <div
          v-for="(f, i) in coverFiles"
          :key="f.uid"
          style="display: flex; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px solid #f0f0f0"
        >
          <a-image :src="f.thumbUrl" width="40" height="40" style="border-radius: 4px" />
          <span style="flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap">{{ f.name }}</span>
          <a-button type="text" danger size="small" @click="coverFiles.splice(i, 1)">移除</a-button>
        </div>
        <a-button type="primary" block :loading="importing" style="margin-top: 12px" @click="startImportCovers">
          开始导入（{{ coverFiles.length }} 张）
        </a-button>
      </div>
      <template v-if="coverResult">
        <a-divider />
        <a-typography-text type="success">导入完成：成功 {{ coverResult.matched?.length || 0 }} 张</a-typography-text>
        <a-table
          v-if="coverResult.matched?.length"
          size="small"
          :data-source="coverResult.matched"
          :columns="coverResultColumns"
          :pagination="false"
          row-key="fileName"
          style="margin-top: 8px"
        />
        <a-typography-text v-if="coverResult.unmatched?.length" type="warning" style="display: block; margin-top: 8px">
          未匹配（{{ coverResult.unmatched.length }}）：{{ coverResult.unmatched.join('、') }}
        </a-typography-text>
      </template>
    </BasicModal>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-recipe-list" setup>
  import { PageWrapper } from '/@/components/Page';
  import { ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { BasicModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { defHttp } from '/@/utils/http/axios';
  import { recipeApi } from '/@/api/homeai';
  import type { HomeaiCategory, HomeaiPageParams, HomeaiRecipe } from '/@/api/homeai';
  import RecipeDrawer from './RecipeDrawer.vue';
  import { useUserLabel } from '../hooks/useUserLabel';
  import { recipeDifficultyColor } from '../hooks/homeaiStatusColors';
  import { useHomeaiRecycleBin } from '../hooks/useHomeaiRecycleBin';
  import { useUserStore } from '/@/store/modules/user';

  const { createMessage } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const { userOptions, loadUserOptions, resolveUserLabel } = useUserLabel();
  const activeTab = ref('list');
  const coverImportOpen = ref(false);
  const coverFiles = ref<{ uid: string; name: string; file: File; thumbUrl: string }[]>([]);
  const importing = ref(false);
  const coverResult = ref<{
    matched: { fileName?: string; recipeName?: string; count?: number; coverUrl?: string }[];
    unmatched: string[];
  } | null>(null);
  const coverResultColumns = [
    { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
    { title: '菜谱', dataIndex: 'recipeName', key: 'recipeName', width: 120 },
    { title: '更新数', dataIndex: 'count', key: 'count', width: 80 },
  ];

  function openCoverImport() {
    coverImportOpen.value = true;
  }

  function onCollectCovers(file: File) {
    coverFiles.value.push({
      uid: `${Date.now()}-${Math.random().toString(36).slice(2)}`,
      name: file.name,
      file,
      thumbUrl: URL.createObjectURL(file),
    });
    return false;
  }

  async function startImportCovers() {
    if (!coverFiles.value.length) return;
    importing.value = true;
    coverResult.value = null;
    const matched: { fileName?: string; recipeName?: string; count?: number; coverUrl?: string }[] = [];
    const unmatched: string[] = [];
    try {
      for (const f of coverFiles.value) {
        const res: any = await defHttp.uploadFile(
          { url: '/homeai/recipe/import-covers' },
          { file: f.file, name: 'file' },
          { isReturnResponse: true }
        );
        if (!res || res.success !== true || res.code !== 200) {
          unmatched.push(`${f.name}（${res?.message || '导入失败'}）`);
          continue;
        }
        const r = res.result || {};
        matched.push(...(r.matched || []));
        unmatched.push(...(r.unmatched || []));
      }
      coverResult.value = { matched, unmatched: Array.from(new Set(unmatched)) };
      if (matched.length) {
        createMessage.success(`已导入 ${matched.length} 张封面`);
        reload();
      }
    } catch (e: any) {
      createMessage.error(e?.message || '导入失败');
    } finally {
      importing.value = false;
    }
  }

  function difficultyLabel(v: number | string | null | undefined) {
    const n = Number(v);
    if (!Number.isFinite(n) || n < 1) return '-';
    const labels = ['', '入门', '简单', '中等', '较难', '困难'];
    return labels[Math.min(5, Math.max(1, Math.round(n)))] || String(v);
  }

  function difficultyColor(v: number | string | null | undefined) {
    return recipeDifficultyColor(v);
  }
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const categoryNameMap = ref<Record<string, string>>({});
  const difficultyOptions = [
    { label: '入门', value: 1 },
    { label: '简单', value: 2 },
    { label: '中等', value: 3 },
    { label: '较难', value: 4 },
    { label: '困难', value: 5 },
  ];
  const visibilityOptions = [
    { label: '公开', value: 'public' },
    { label: '家庭', value: 'family' },
    { label: '仅自己', value: 'private' },
  ];

  async function loadCategoryOptions() {
    try {
      const list: HomeaiCategory[] = (await recipeApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
      categoryNameMap.value = Object.fromEntries(list.map((c) => [c.id, c.name]));
    } catch {
      categoryOptions.value = [];
      categoryNameMap.value = {};
    }
  }

  const columns = [
    { title: '菜名', dataIndex: 'name', width: 160 },
    {
      title: '分类',
      dataIndex: 'categoryId',
      key: 'categoryId',
      width: 100,
      customRender: ({ text }: any) => categoryNameMap.value[text] || text || '-',
    },
    { title: '难度', dataIndex: 'difficulty', key: 'difficulty', width: 70 },
    { title: '烹饪时间(分)', dataIndex: 'cookTime', width: 90 },
    {
      title: '可见性',
      dataIndex: 'visibility',
      width: 90,
      customRender: ({ text }: any) => (text === 'public' ? '公开' : text === 'family' ? '家庭' : text === 'private' ? '仅自己' : text || '-'),
    },
    { title: '浏览数', dataIndex: 'viewCount', width: 70 },
    { title: '用户', dataIndex: 'userId', key: 'userId', width: 160 },
  ];

  const [registerTable, { reload }] = useTable({
    title: '菜谱管理',
    api: (params: HomeaiPageParams) => (activeTab.value === 'list' ? recipeApi.list(params) : recipeApi.recycleBin(params)),
    columns: columns,
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: { width: 120, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
    formConfig: {
      labelWidth: 80,
      schemas: [
        { field: 'name', label: '菜名', component: 'Input', colProps: { span: 8 } },
        {
          field: 'categoryId',
          label: '分类',
          component: 'Select',
          colProps: { span: 8 },
          componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' },
        },
        {
          field: 'difficulty',
          label: '难度',
          component: 'Select',
          colProps: { span: 8 },
          componentProps: { options: difficultyOptions, allowClear: true, placeholder: '请选择难度' },
        },
        {
          field: 'cookTime_begin',
          label: '烹饪时间≥',
          component: 'InputNumber',
          colProps: { span: 8 },
          componentProps: { min: 0, placeholder: '最短(分)' },
        },
        {
          field: 'cookTime_end',
          label: '烹饪时间≤',
          component: 'InputNumber',
          colProps: { span: 8 },
          componentProps: { min: 0, placeholder: '最长(分)' },
        },
        {
          field: 'visibility',
          label: '可见性',
          component: 'Select',
          colProps: { span: 8 },
          componentProps: { options: visibilityOptions, allowClear: true, placeholder: '请选择可见性' },
        },
        {
          field: 'userId',
          label: '用户',
          component: 'Select',
          colProps: { span: 8 },
          componentProps: { options: userOptions, allowClear: true, showSearch: true, optionFilterProp: 'label', placeholder: '请选择用户' },
        },
      ],
    },
  });

  const { rowSelection, selectedRowKeys, clearSelection, handleMoveToRecycleBin, handleBatchMoveToRecycleBin, handleRestore, handleBatchRestore, handleDeletePermanently, handleBatchDeletePermanently } = useHomeaiRecycleBin({
    api: {
      moveToRecycleBin: (ids: string[]) => recipeApi.moveToRecycleBin(ids),
      restore: (ids: string[]) => recipeApi.restore(ids),
      deletePermanently: (ids: string[]) => recipeApi.deletePermanently(ids),
    },
    reload,
    entityName: '菜谱',
    nameField: 'name',
    permanentWarn: '此操作不可恢复！',
  });

  onMounted(async () => {
    await Promise.all([loadCategoryOptions(), loadUserOptions()]);
  });

  function onTabChange(key: string) {
    activeTab.value = key;
    clearSelection();
    reload();
  }

  function getTableAction(record: HomeaiRecipe) {
    if (activeTab.value === 'recycle') {
      return [
        { icon: 'ant-design:undo-outlined', onClick: () => handleRestore(record), title: '恢复' },
        { icon: 'ant-design:delete-outlined', onClick: () => handleDeletePermanently(record), title: '彻底删除', color: 'error' },
      ];
    }
    return [
      { icon: 'ant-design:edit-outlined', onClick: () => openDrawer(true, { record, isUpdate: true }), title: '编辑' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleMoveToRecycleBin(record), title: '移入回收站', color: 'error' },
    ];
  }

  function handleAdd() {
    openDrawer(true, { isUpdate: false, record: {} });
  }

  function handleDownloadTemplate() {
    const token = useUserStore().getToken;
    window.open(`/jeecg-boot/homeai/recipe/exportTemplate?token=${encodeURIComponent(token)}`, '_blank');
  }

  function handleSuccess() {
    reload();
  }
</script>
