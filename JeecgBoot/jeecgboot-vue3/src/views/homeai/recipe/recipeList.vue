<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onTabChange">
      <a-tab-pane key="list" tab="菜谱列表" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>
    <a-alert
      v-if="activeTab === 'list'"
      type="info"
      show-icon
      style="margin-bottom: 12px"
      message="菜谱封面需单独导入（Excel 不含图片文件）。请用工具栏「导入封面」，按图片文件名或所在文件夹名匹配菜谱名称。"
    />
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-space wrap :size="[8, 8]">
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:plus-outlined" type="primary" @click="handleAdd">新增</a-button>
        <a-button v-if="activeTab === 'list'" type="primary" preIcon="ant-design:file-image-outlined" @click="openCoverImport">导入封面</a-button>
        <a-upload
          v-if="activeTab === 'list'"
          name="file"
          :showUploadList="false"
          :customRequest="(file) => handleImportXls(file, recipeApi.importExcel, reload)"
        >
          <a-button preIcon="ant-design:import-outlined">导入 Excel</a-button>
        </a-upload>
        <a-button v-if="activeTab === 'list'" preIcon="ant-design:download-outlined" @click="handleDownloadTemplate">下载模板</a-button>
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
        </a-space>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'coverUrl'">
          <a-image v-if="record.coverUrl" :src="record.coverUrl" :width="48" :height="48" style="border-radius: 4px; object-fit: cover" />
          <span v-else style="color: #bbb">无</span>
        </template>
        <template v-else-if="column.key === 'categoryId' || column.dataIndex === 'categoryId' || column.dataIndex === 'category'">
          {{ categoryLabel(record) }}
        </template>
        <template v-else-if="column.key === 'difficulty'">
          <a-tag :color="difficultyColor(record.difficulty)">{{ difficultyLabel(record.difficulty) }}</a-tag>
        </template>
        <template v-else-if="column.key === 'userId' || column.dataIndex === 'userId'">
          {{ resolveUserLabel(record.userId) }}
        </template>
      </template>
    </BasicTable>
    <RecipeDrawer @register="registerDrawer" @success="handleSuccess" />

    <BasicModal
      v-model:open="coverImportOpen"
      title="批量导入菜谱封面"
      width="70%"
      :minHeight="520"
      :maskClosable="false"
      :canFullscreen="true"
      :keyboard="!importing"
      :closeFunc="beforeCoverImportClose"
      wrapClassName="recipe-cover-import-modal"
      :footer="null"
    >
      <div class="cover-import-body">
        <a-alert type="info" show-icon class="cover-import-alert">
          <template #message>
            按图片文件名或所在文件夹名匹配菜谱名称。例如「红烧肉.jpg」或「红烧肉/cover.jpg」都会更新名为「红烧肉」的封面；同名菜谱全部更新。可选手动选择单张/多张，或直接导入整个菜谱图片文件夹。可拖动右下角调整窗口大小，也可点右上角全屏。
          </template>
        </a-alert>
        <a-space wrap class="cover-import-actions">
          <input
            ref="coverFileInput"
            type="file"
            class="cover-import-file-input"
            multiple
            accept="image/*,.zip,application/zip"
            :disabled="importing"
            @change="onCoverInputChange"
          />
          <input
            ref="coverFolderInput"
            type="file"
            class="cover-import-file-input"
            multiple
            webkitdirectory
            :disabled="importing"
            @change="onCoverInputChange"
          />
          <a-button preIcon="ant-design:file-image-outlined" :disabled="importing" @click="pickCoverFiles">选择图片（单张/多选）</a-button>
          <a-button preIcon="ant-design:folder-open-outlined" :disabled="importing" @click="pickCoverFolder">选择菜谱文件夹</a-button>
          <a-button v-if="coverFiles.length" :disabled="importing" @click="resetCoverImport">清空</a-button>
        </a-space>
        <a-typography-text v-if="collectingCovers" type="secondary">正在读取所选文件（已收到 {{ collectingCount }} 个）…</a-typography-text>
        <a-typography-text v-else-if="coverFiles.length">
          已选 {{ coverFiles.length }} 个文件，将导入 {{ coverPlan.length }} 张封面（同菜去重，非图片自动跳过）
        </a-typography-text>
        <div v-if="importing || coverResult" class="cover-import-progress">
          <a-progress
            :percent="importPercent"
            :status="importProgressStatus"
            :format="() => `${importDone}/${Math.max(coverPlan.length, importDone)}`"
          />
          <div class="cover-import-progress-text">{{ importProgressText }}</div>
        </div>
        <div v-if="coverPlan.length" class="cover-import-list">
          <div v-for="(item, i) in coverPlan" :key="item.displayName + i" class="cover-import-item" :class="{ 'is-current': importing && importingIndex === i }">
            <span class="cover-import-thumb">{{ isZipFile(item.displayName) ? 'ZIP' : '图' }}</span>
            <span class="cover-import-name" :title="item.displayName">{{ item.displayName }}</span>
            <a-tag>{{ item.key }}</a-tag>
            <a-tag :color="coverItemTagColor(coverItemStates[i]?.status)">{{ coverItemTagText(coverItemStates[i]?.status) }}</a-tag>
          </div>
        </div>
        <div v-if="coverResult" class="cover-import-result">
          <a-typography-text type="success">导入完成：成功 {{ coverResult.matched?.length || 0 }} 张</a-typography-text>
          <a-table
            v-if="coverResult.matched?.length"
            size="small"
            :data-source="coverResult.matched"
            :columns="coverResultColumns"
            :pagination="false"
            :scroll="{ y: 180 }"
            row-key="fileName"
            style="margin-top: 8px"
          />
          <a-typography-text v-if="coverResult.unmatched?.length" type="warning" style="display: block; margin-top: 8px">
            未匹配（{{ coverResult.unmatched.length }}）：{{ coverResult.unmatched.join('、') }}
          </a-typography-text>
        </div>
        <a-button
          type="primary"
          block
          :loading="importing || collectingCovers"
          :disabled="!coverPlan.length || collectingCovers"
          class="cover-import-submit"
          @click="startImportCovers"
        >
          {{ importing ? `导入中 ${importDone}/${coverPlan.length}` : `开始导入（${coverPlan.length} 张）` }}
        </a-button>
        <div class="cover-import-resize-handle" title="拖动调整窗口大小" @mousedown.prevent="onCoverModalResizeStart"></div>
      </div>
    </BasicModal>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-recipe-list" setup>
  import { PageWrapper } from '/@/components/Page';
  import { ref, onMounted, onBeforeUnmount, computed, watch, nextTick } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { useDrawer } from '/@/components/Drawer';
  import { BasicModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useMethods } from '/@/hooks/system/useMethods';
  import { defHttp } from '/@/utils/http/axios';
  import { recipeApi } from '/@/api/homeai';
  import type { HomeaiCategory, HomeaiPageParams, HomeaiRecipe } from '/@/api/homeai';
  import RecipeDrawer from './RecipeDrawer.vue';
  import { pickCoverUploads, toUploadFile, isZipFile, type CoverPickFile } from './recipeCoverMatch';
  import { useUserLabel } from '../hooks/useUserLabel';
  import { recipeDifficultyColor } from '../hooks/homeaiStatusColors';
  import { useHomeaiRecycleBin } from '../hooks/useHomeaiRecycleBin';
  import { useUserStore } from '/@/store/modules/user';

  const { createMessage } = useMessage();
  const { handleExportXls, handleImportXls } = useMethods();
  const [registerDrawer, { openDrawer }] = useDrawer();
  const { userOptions, loadUserOptions, resolveUserLabel } = useUserLabel();
  const activeTab = ref('list');
  type CoverItemStatus = 'wait' | 'uploading' | 'ok' | 'fail';
  type CoverItemState = { status: CoverItemStatus; hint?: string };

  const coverImportOpen = ref(false);
  const coverFileInput = ref<HTMLInputElement | null>(null);
  const coverFolderInput = ref<HTMLInputElement | null>(null);
  const coverFiles = ref<CoverPickFile[]>([]);
  const importing = ref(false);
  const importDone = ref(0);
  const importingIndex = ref(-1);
  const importCurrentName = ref('');
  const currentUploadPercent = ref(0);
  const collectingCovers = ref(false);
  const collectingCount = ref(0);
  const coverItemStates = ref<CoverItemState[]>([]);
  const coverResult = ref<{
    matched: { fileName?: string; recipeName?: string; count?: number; coverUrl?: string }[];
    unmatched: string[];
  } | null>(null);
  const coverResultColumns = [
    { title: '文件名', dataIndex: 'fileName', key: 'fileName' },
    { title: '菜谱', dataIndex: 'recipeName', key: 'recipeName', width: 120 },
    { title: '更新数', dataIndex: 'count', key: 'count', width: 80 },
  ];
  const coverPlan = computed(() => pickCoverUploads(coverFiles.value));
  const importPercent = computed(() => {
    const total = coverPlan.value.length;
    if (!total) return 0;
    if (!importing.value && coverResult.value) return 100;
    return Math.min(100, Math.round(((importDone.value + currentUploadPercent.value / 100) / total) * 100));
  });
  const importProgressStatus = computed(() => {
    if (importing.value) return 'active';
    if (coverResult.value?.unmatched?.length && !coverResult.value?.matched?.length) return 'exception';
    return 'success';
  });
  const importProgressText = computed(() => {
    if (importing.value) {
      const name = importCurrentName.value ? `：${importCurrentName.value}` : '';
      const upload = currentUploadPercent.value ? `（上传 ${currentUploadPercent.value}%）` : '';
      return `正在导入 ${Math.min(importDone.value + 1, coverPlan.value.length)}/${coverPlan.value.length}${name}${upload}`;
    }
    if (!coverResult.value) return '';
    const ok = coverResult.value.matched?.length || 0;
    const miss = coverResult.value.unmatched?.length || 0;
    return `导入结束：成功 ${ok} 张${miss ? `，未匹配 ${miss} 个` : ''}`;
  });

  let coverCollectBuffer: CoverPickFile[] = [];
  let coverCollectTimer: ReturnType<typeof setTimeout> | null = null;

  function resetCoverImport() {
    if (coverCollectTimer) {
      clearTimeout(coverCollectTimer);
      coverCollectTimer = null;
    }
    coverCollectBuffer = [];
    collectingCovers.value = false;
    collectingCount.value = 0;
    coverFiles.value = [];
    coverResult.value = null;
    coverItemStates.value = [];
    importDone.value = 0;
    importingIndex.value = -1;
    importCurrentName.value = '';
    currentUploadPercent.value = 0;
  }

  function openCoverImport() {
    resetCoverImport();
    coverImportOpen.value = true;
  }

  async function beforeCoverImportClose() {
    if (importing.value) {
      createMessage.warning('正在导入封面，请等待完成后再关闭');
      return false;
    }
    resetCoverImport();
    return true;
  }

  function onCollectCovers(file: CoverPickFile) {
    coverCollectBuffer.push(file);
    collectingCount.value = coverCollectBuffer.length;
    collectingCovers.value = true;
    coverResult.value = null;
    if (coverCollectTimer) clearTimeout(coverCollectTimer);
    coverCollectTimer = setTimeout(() => {
      coverFiles.value = [...coverFiles.value, ...coverCollectBuffer];
      coverCollectBuffer = [];
      collectingCount.value = 0;
      collectingCovers.value = false;
      coverCollectTimer = null;
    }, 150);
    return false;
  }

  function onCoverInputChange(e: Event) {
    const input = e.target as HTMLInputElement;
    const files = input.files;
    if (files?.length) {
      Array.from(files).forEach((file) => onCollectCovers(file as CoverPickFile));
    }
    input.value = '';
  }

  function pickCoverFiles() {
    if (importing.value) return;
    coverFileInput.value?.click();
  }

  function pickCoverFolder() {
    if (importing.value) return;
    coverFolderInput.value?.click();
  }

  function coverItemTagColor(status?: CoverItemStatus) {
    if (status === 'uploading') return 'processing';
    if (status === 'ok') return 'success';
    if (status === 'fail') return 'error';
    return 'default';
  }

  function coverItemTagText(status?: CoverItemStatus) {
    if (status === 'uploading') return '上传中';
    if (status === 'ok') return '成功';
    if (status === 'fail') return '失败';
    return '待导入';
  }

  function setCoverItemState(index: number, state: CoverItemState) {
    const next = coverItemStates.value.slice();
    next[index] = state;
    coverItemStates.value = next;
  }

  watch(importingIndex, async (i) => {
    if (i < 0) return;
    await nextTick();
    document.querySelector('.cover-import-item.is-current')?.scrollIntoView({ block: 'nearest' });
  });

  function onCoverModalResizeStart(e: MouseEvent) {
    const modal = (e.currentTarget as HTMLElement | null)?.closest('.ant-modal') as HTMLElement | null;
    if (!modal) return;
    const startX = e.clientX;
    const startY = e.clientY;
    const startW = modal.offsetWidth;
    const startH = modal.offsetHeight;
    const onMove = (ev: MouseEvent) => {
      const w = Math.min(Math.max(760, startW + ev.clientX - startX), window.innerWidth * 0.96);
      const h = Math.min(Math.max(520, startH + ev.clientY - startY), window.innerHeight * 0.92);
      modal.style.width = `${Math.round(w)}px`;
      modal.style.height = `${Math.round(h)}px`;
    };
    const onUp = () => {
      document.removeEventListener('mousemove', onMove);
      document.removeEventListener('mouseup', onUp);
    };
    document.addEventListener('mousemove', onMove);
    document.addEventListener('mouseup', onUp);
  }

  async function uploadCoverOnce(item: { file: CoverPickFile; displayName: string }, onPercent: (pct: number) => void) {
    const post = () =>
      defHttp.uploadFile(
        {
          url: recipeApi.importCovers,
          timeout: 120 * 1000,
          onUploadProgress: (e: ProgressEvent) => {
            if (e.total) onPercent(Math.min(100, Math.round((e.loaded / e.total) * 100)));
          },
        },
        { file: toUploadFile(item.file), name: 'file', filename: item.displayName },
        { isReturnResponse: true },
      );
    try {
      return await post();
    } catch (e: any) {
      const msg = String(e?.message || '');
      if (!/timeout|network|重置|OSS|存储/i.test(msg)) {
        throw e;
      }
      onPercent(0);
      return await post();
    }
  }

  async function startImportCovers() {
    const plan = coverPlan.value;
    if (!plan.length || importing.value) return;
    importing.value = true;
    importDone.value = 0;
    importingIndex.value = -1;
    importCurrentName.value = '';
    currentUploadPercent.value = 0;
    coverResult.value = null;
    coverItemStates.value = plan.map(() => ({ status: 'wait' as CoverItemStatus }));
    const matched: { fileName?: string; recipeName?: string; count?: number; coverUrl?: string }[] = [];
    const unmatched: string[] = [];
    try {
      for (let i = 0; i < plan.length; i++) {
        const item = plan[i];
        importingIndex.value = i;
        importCurrentName.value = item.displayName;
        currentUploadPercent.value = 0;
        setCoverItemState(i, { status: 'uploading' });
        try {
          const res: any = await uploadCoverOnce(item, (pct) => {
            currentUploadPercent.value = pct;
          });
          importDone.value += 1;
          currentUploadPercent.value = 100;
          if (!res || res.success !== true || res.code !== 200) {
            const hint = res?.message || '导入失败';
            unmatched.push(`${item.displayName}（${hint}）`);
            setCoverItemState(i, { status: 'fail', hint });
            continue;
          }
          const r = res.result || {};
          const itemMatched = r.matched || [];
          const itemUnmatched = r.unmatched || [];
          matched.push(...itemMatched);
          unmatched.push(...itemUnmatched);
          if (itemMatched.length) {
            setCoverItemState(i, { status: 'ok' });
          } else {
            setCoverItemState(i, { status: 'fail', hint: itemUnmatched[0] || '未匹配' });
          }
        } catch (itemErr: any) {
          importDone.value += 1;
          const hint = itemErr?.message || '导入失败';
          unmatched.push(`${item.displayName}（${hint}）`);
          setCoverItemState(i, { status: 'fail', hint });
        }
      }
      coverResult.value = { matched, unmatched: Array.from(new Set(unmatched)) };
      if (matched.length) {
        createMessage.success(`已导入 ${matched.length} 张封面`);
        reload();
      } else if (unmatched.length) {
        createMessage.warning('没有匹配到菜谱，请确认文件名或文件夹名与菜谱名称一致');
      }
    } catch (e: any) {
      createMessage.error(e?.message || '导入失败');
    } finally {
      importing.value = false;
      importingIndex.value = -1;
      importCurrentName.value = '';
      currentUploadPercent.value = 0;
    }
  }

  const RECIPE_CATEGORY_LABELS: Record<string, string> = {
    rc_hot: '热菜',
    rc_cold: '凉菜',
    rc_soup: '汤羹',
    rc_staple: '主食',
    rc_bake: '烘焙',
    rc_drink: '饮品',
    rc_snack: '小食',
    rc_other: '其他',
  };

  function categoryLabel(record: any) {
    const id = record?.categoryId || record?.category;
    return record?.categoryName || categoryNameMap.value[id] || RECIPE_CATEGORY_LABELS[id] || id || '-';
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
  const categoryOptions = ref<{ label: string; value: string }[]>(
    Object.entries(RECIPE_CATEGORY_LABELS).map(([value, label]) => ({ label, value }))
  );
  const categoryNameMap = ref<Record<string, string>>({ ...RECIPE_CATEGORY_LABELS });
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
      const raw: any = await recipeApi.categories();
      const list: HomeaiCategory[] = Array.isArray(raw) ? raw : raw?.records || raw?.result || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
      categoryNameMap.value = {
        ...RECIPE_CATEGORY_LABELS,
        ...Object.fromEntries(list.filter((c) => c?.id).map((c) => [c.id, c.name])),
      };
    } catch {
      categoryOptions.value = Object.entries(RECIPE_CATEGORY_LABELS).map(([value, label]) => ({ label, value }));
      categoryNameMap.value = { ...RECIPE_CATEGORY_LABELS };
    }
  }

  const columns = [
    {
      title: '封面',
      dataIndex: 'coverUrl',
      key: 'coverUrl',
      width: 72,
    },
    { title: '菜名', dataIndex: 'name', width: 160 },
    {
      title: '分类',
      dataIndex: 'categoryId',
      key: 'categoryId',
      width: 100,
      customRender: ({ text, record }: any) => categoryLabel(record || { categoryId: text }),
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

  onBeforeUnmount(() => {
    if (coverCollectTimer) {
      clearTimeout(coverCollectTimer);
      coverCollectTimer = null;
    }
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

<style lang="less" scoped>
  :deep(.jeecg-basic-table-header__table-title-box) {
    align-items: flex-start !important;
    margin-bottom: 16px;
  }

  :deep(.jeecg-basic-table-header__table-title-box + div) {
    margin: 12px 0 8px !important;
    padding-top: 4px !important;
  }

  .cover-import-body {
    position: relative;
    display: flex;
    flex-direction: column;
    gap: 12px;
    height: 100%;
    min-height: 0;
    padding-bottom: 8px;
  }

  .cover-import-file-input {
    position: absolute;
    width: 0;
    height: 0;
    opacity: 0;
    pointer-events: none;
  }

  .cover-import-actions {
    position: relative;
    z-index: 3;
  }

  .cover-import-resize-handle {
    position: absolute;
    right: 0;
    bottom: 0;
    width: 16px;
    height: 16px;
    cursor: nwse-resize;
    background: linear-gradient(135deg, transparent 50%, #1890ff 50%);
    z-index: 2;
  }

  .cover-import-alert,
  .cover-import-actions,
  .cover-import-progress,
  .cover-import-submit {
    flex-shrink: 0;
  }

  .cover-import-progress-text {
    margin-top: 4px;
    color: rgba(0, 0, 0, 0.45);
  }

  .cover-import-list {
    flex: 1;
    min-height: 160px;
    overflow: auto;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    padding: 0 8px;
  }

  .cover-import-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 0;
    border-bottom: 1px solid #f0f0f0;

    &.is-current {
      background: #e6f7ff;
      margin: 0 -8px;
      padding-left: 8px;
      padding-right: 8px;
    }
  }

  .cover-import-thumb {
    width: 40px;
    height: 40px;
    border-radius: 4px;
    background: #f5f5f5;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 12px;
    color: #999;
    flex-shrink: 0;
  }

  .cover-import-name {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .cover-import-result {
    flex-shrink: 0;
    max-height: 220px;
    overflow: auto;
  }
</style>

<style lang="less">
  /* 弹窗挂到 body，需非 scoped；右下角拖动可改宽高 */
  .recipe-cover-import-modal {
    .ant-modal {
      padding-bottom: 0;
      min-width: 760px;
      max-width: 96vw;
      height: 72vh;
      min-height: 520px;
      max-height: 92vh;
      resize: both;
      overflow: hidden;
    }

    .ant-modal-content {
      display: flex;
      flex-direction: column;
      height: 100%;
      overflow: hidden;
    }

    .ant-modal::-webkit-resizer {
      background: linear-gradient(-45deg, #1890ff 0 6px, transparent 6px 8px, #1890ff 8px 12px, transparent 12px);
    }

    .ant-modal-body {
      flex: 1;
      overflow: hidden;
      display: flex;
      flex-direction: column;
      min-height: 0;
    }

    .jeecg-modal-wrapper,
    .jeecg-modal-content,
    .ant-modal-body .scroll-container {
      height: 100%;
      max-height: 100%;
    }

    .ant-modal-body .scroll-container > .scrollbar__wrap,
    .ant-modal-body .scroll-container .scrollbar__view {
      height: 100%;
      max-height: 100%;
      min-height: 0;
    }

    /* 勿把滚动条轨道拉成整层，否则会挡住选文件按钮 */
    .scrollbar__bar.is-horizontal {
      height: 6px !important;
      min-height: 0 !important;
    }

    .scrollbar__bar.is-vertical {
      width: 6px !important;
      min-width: 0 !important;
    }
  }
</style>
