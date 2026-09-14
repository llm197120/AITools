<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-tabs v-model:activeKey="activeTab" @change="onMainTabChange">
      <a-tab-pane key="files" tab="文件管理" />
      <a-tab-pane key="recycle" tab="回收站" />
    </a-tabs>

    <template v-if="activeTab === 'files'">
    <div class="homeai-toolbar">
      <a-button type="primary" preIcon="ant-design:folder-add-outlined" @click="openCreateFolderModal">新增文件夹</a-button>
      <a-button preIcon="ant-design:upload-outlined" @click="handleUploadModal">上传文件</a-button>
      <a-button preIcon="ant-design:reload-outlined" @click="refreshExplorer">刷新</a-button>
      <a-button type="link" @click="statsExpanded = !statsExpanded">
        {{ statsExpanded ? '收起空间统计' : '展开空间统计' }}
      </a-button>
    </div>

    <div v-show="statsExpanded" class="stats-panel">
      <a-row :gutter="16">
        <a-col :span="6">
          <a-card :bordered="false"><a-statistic title="文件总数" :value="spaceStats.totalFiles || 0" suffix="个" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false"><a-statistic title="总占用空间" :value="formatSize(spaceStats.totalSize || 0)" /></a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false">
            <a-statistic title="用户默认配额" :value="formatSize(spaceStats.defaultUserLimitBytes || 0)" />
            <div style="margin-top: 8px; font-size: 12px; color: #888">
              家庭默认 {{ formatSize(spaceStats.defaultFamilyLimitBytes || 0) }} · 告警 {{ spaceStats.warnPercent || 80 }}%
            </div>
          </a-card>
        </a-col>
        <a-col :span="6">
          <a-card :bordered="false" title="占用 Top5（用户 / 家庭）">
            <div style="font-size: 12px; color: #888; margin-bottom: 4px">用户</div>
            <div v-for="u in topUsers" :key="'u-' + u.userId" style="padding: 2px 0">
              <div style="display: flex; justify-content: space-between; font-size: 12px">
                <span>{{ resolveUserLabel(u.userId) }}</span>
                <span :style="{ color: u.overWarn ? '#cf1322' : undefined }">
                  {{ formatSize(u.totalSize) }}
                </span>
              </div>
              <a-progress
                v-if="u.limitBytes"
                :percent="Math.min(100, Math.round(u.usedPercent || 0))"
                size="small"
                :status="u.overWarn ? 'exception' : 'normal'"
                :show-info="false"
              />
            </div>
            <div style="font-size: 12px; color: #888; margin: 8px 0 4px; display: flex; justify-content: space-between; align-items: center">
              <span>家庭</span>
              <a-button type="link" size="small" style="padding: 0; height: auto" @click="goFamilyQuota">配额看板</a-button>
            </div>
            <div v-for="f in topFamilies" :key="'f-' + f.familyId" style="padding: 2px 0; cursor: pointer" @click="openFamilyQuota(f)">
              <div style="display: flex; justify-content: space-between; font-size: 12px">
                <span>
                  {{ f.familyName || f.familyId }}
                  <a-tag v-if="f.customLimit" color="blue" style="margin-left: 4px; font-size: 10px; line-height: 16px; padding: 0 4px">自定义</a-tag>
                </span>
                <span :style="{ color: f.overWarn ? '#cf1322' : undefined }">
                  {{ formatSize(f.totalSize) }}
                </span>
              </div>
              <a-progress
                v-if="f.limitBytes"
                :percent="Math.min(100, Math.round(f.usedPercent || 0))"
                size="small"
                :status="f.overWarn ? 'exception' : 'normal'"
                :show-info="false"
              />
            </div>
            <span v-if="topUsers.length === 0 && topFamilies.length === 0" style="color: #999">暂无数据</span>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <div class="explorer">
      <div class="explorer-tree">
        <div class="explorer-tree-title">目录</div>
        <a-tree
          :tree-data="explorerTreeData"
          :field-names="{ children: 'children', title: 'name', key: 'id' }"
          :selected-keys="treeSelectedKeys"
          default-expand-all
          block-node
          @select="onFolderSelect"
        >
          <template #title="{ name }">
            <span class="tree-node-title">
              <Icon icon="ant-design:folder-outlined" :style="{ color: '#faad14' }" />
              <span>{{ name }}</span>
            </span>
          </template>
        </a-tree>
      </div>

      <div class="explorer-main">
        <div class="explorer-crumb">
          <a-button size="small" :disabled="!canGoUp" @click="goUp">上级</a-button>
          <template v-for="(crumb, idx) in breadcrumbItems" :key="crumb.id ?? 'root'">
            <span v-if="idx > 0" class="crumb-sep">/</span>
            <a
              v-if="idx < breadcrumbItems.length - 1"
              class="crumb-link"
              href="javascript:void(0)"
              @click.prevent="enterFolder(crumb.id)"
            >{{ crumb.name }}</a>
            <span v-else class="crumb-current">{{ crumb.name }}</span>
          </template>
        </div>
        <a-table
          :columns="explorerColumns"
          :data-source="explorerRows"
          :loading="listingLoading"
          :pagination="{ pageSize: 20, showSizeChanger: true, showTotal: (t: number) => `共 ${t} 条` }"
          :custom-row="explorerCustomRow"
          :row-class-name="explorerRowClass"
          :locale="{ emptyText: '当前目录为空' }"
          size="small"
          row-key="id"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'type'">
              <template v-if="record.rowType === 'folder'">
                <Icon icon="ant-design:folder-filled" :style="{ color: '#faad14', fontSize: '22px' }" />
              </template>
              <template v-else>
                <img
                  v-if="record.file?.thumbnailUrl"
                  :src="record.file.thumbnailUrl"
                  alt=""
                  class="file-thumb"
                  @click.stop="openPreview(record.file)"
                />
                <span v-else>{{ record.file?.extension || '-' }}</span>
              </template>
            </template>
            <template v-else-if="column.key === 'name'">
              <a
                v-if="record.rowType === 'folder'"
                class="name-link"
                href="javascript:void(0)"
                @click.prevent="enterFolder(record.id)"
              >{{ record.name }}</a>
              <span v-else>{{ record.name }}</span>
            </template>
            <template v-else-if="column.key === 'visibility'">
              <a-tag :color="record.visibility === 'family' ? 'green' : record.visibility === 'public' ? 'blue' : 'default'">
                {{ record.visibility === 'family' ? '家庭' : record.visibility === 'public' ? '公开' : '私有' }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'size'">
              <template v-if="record.rowType === 'folder'">
                {{ record.folder?.fileCount != null ? `${record.folder.fileCount} 个文件` : '—' }}
              </template>
              <template v-else>
                {{ formatSize(record.file?.fileSize) }}
              </template>
            </template>
            <template v-else-if="column.key === 'action'">
              <template v-if="record.rowType === 'folder'">
                <a-button type="link" size="small" @click.stop="openEditFolderModal(record.folder)">编辑</a-button>
                <a-popconfirm title="确定将该文件夹移入回收站？其内文件与子文件夹将一并移入" @confirm="handleDeleteFolder(record.id)">
                  <a-button type="link" danger size="small" @click.stop>删除</a-button>
                </a-popconfirm>
              </template>
              <template v-else-if="record.file">
                <a-button type="link" size="small" @click.stop="openPreview(record.file)">预览</a-button>
                <a-button type="link" size="small" @click.stop="openEditFileModal(record.file)">编辑</a-button>
                <a-button type="link" size="small" @click.stop="handleDownload(record.file)">下载</a-button>
                <a-button type="link" size="small" @click.stop="openConvertModal(record.file)">格式转换</a-button>
                <a-button v-if="canConvertToPdf(record.file)" type="link" size="small" @click.stop="handleConvertToPdf(record.file)">转PDF</a-button>
                <a-button type="link" danger size="small" @click.stop="handleDeleteFile(record.file)">删除</a-button>
              </template>
            </template>
          </template>
        </a-table>
      </div>
    </div>
    </template>

    <template v-else>
      <a-tabs v-model:activeKey="recycleType" size="small" @change="onRecycleTypeChange">
        <a-tab-pane key="file" tab="文件" />
        <a-tab-pane key="folder" tab="文件夹" />
      </a-tabs>
      <div class="homeai-toolbar" style="margin-bottom: 12px">
        <a-input-search
          v-model:value="recycleKeyword"
          allow-clear
          :placeholder="recycleType === 'folder' ? '按文件夹名搜索' : '按文件名搜索'"
          style="width: 260px"
          @search="loadRecycleBin"
        />
        <a-button @click="loadRecycleBin" preIcon="ant-design:reload-outlined">刷新</a-button>
        <a-button type="primary" :disabled="!recycleSelectedKeys.length" @click="handleRestoreSelected">恢复</a-button>
        <a-button danger :disabled="!recycleSelectedKeys.length" @click="handleDeletePermanentlySelected">彻底删除</a-button>
      </div>
      <a-table
        :columns="recycleType === 'folder' ? recycleFolderColumns : recycleColumns"
        :data-source="recycleFiles"
        :loading="recycleLoading"
        :row-selection="{ selectedRowKeys: recycleSelectedKeys, onChange: onRecycleSelect }"
        :pagination="recyclePagination"
        row-key="id"
        size="small"
        @change="onRecycleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'thumb'">
            <img
              v-if="record.thumbnailUrl"
              :src="record.thumbnailUrl"
              alt=""
              style="width: 40px; height: 40px; object-fit: cover; border-radius: 4px"
            />
            <span v-else>{{ record.extension || '-' }}</span>
          </template>
          <template v-else-if="column.key === 'fileSize'">
            {{ formatSize(record.fileSize) }}
          </template>
          <template v-else-if="column.key === 'visibility'">
            <a-tag :color="record.visibility === 'family' ? 'green' : record.visibility === 'public' ? 'blue' : 'default'">
              {{ record.visibility === 'family' ? '家庭' : record.visibility === 'public' ? '公开' : '私有' }}
            </a-tag>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleRestoreSelected([record.id])">恢复</a-button>
            <a-popconfirm title="彻底删除后不可恢复，确定？" @confirm="handleDeletePermanentlySelected([record.id])">
              <a-button type="link" danger size="small">彻底删除</a-button>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
    </template>

    <HomeaiFormModal @register="registerUploadModal" size="medium" title="上传文件">
      <a-form layout="vertical" class="upload-form">
        <a-divider orientation="left" plain>文件</a-divider>
        <a-form-item required>
          <a-upload-dragger
            name="file"
            :multiple="false"
            :max-count="1"
            :file-list="uploadFileList"
            :accept="uploadAccept"
            :disabled="uploading"
            :before-upload="beforeSelectUploadFile"
            @remove="clearSelectedUploadFile"
          >
            <p class="ant-upload-drag-icon">
              <Icon icon="ant-design:inbox-outlined" :size="48" />
            </p>
            <p class="ant-upload-text">点击或拖拽文件到此处</p>
            <p class="ant-upload-hint">单次一个文件；图片、文档、音视频、压缩包等，需在白名单内</p>
          </a-upload-dragger>
          <div v-if="selectedFile" class="upload-file-meta">
            已选：{{ selectedFile.name }}（{{ formatSize(selectedFile.size) }}）
          </div>
        </a-form-item>
        <a-divider orientation="left" plain>存放与可见性</a-divider>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="所属文件夹">
              <a-tree-select
                v-model:value="uploadForm.folderId"
                :tree-data="folderTreeData"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="不选则放到当前目录/根目录"
                allow-clear
                tree-default-expand-all
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="可见性">
              <a-select v-model:value="uploadForm.visibility" style="width: 100%">
                <a-select-option value="private">私有</a-select-option>
                <a-select-option value="family">家庭可见</a-select-option>
                <a-select-option value="public">公开</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col v-if="uploadForm.visibility === 'family'" :span="24">
            <a-form-item label="可见家庭" required>
              <a-select v-model:value="uploadForm.familyIds" mode="multiple" placeholder="选择家庭" style="width: 100%">
                <a-select-option v-for="f in familyOptions" :key="f.id" :value="f.id">{{ f.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-button @click="closeUploadModal()">取消</a-button>
        <a-button type="primary" :loading="uploading" @click="handleUpload">上传并关闭</a-button>
      </template>
    </HomeaiFormModal>

    <HomeaiFormModal @register="registerFolderModal" size="medium" :title="folderEditId ? '编辑文件夹' : '新增文件夹'">
      <a-form layout="vertical">
        <a-form-item label="文件夹名称" required>
          <a-input v-model:value="folderForm.name" placeholder="请输入文件夹名称" />
        </a-form-item>
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="上级文件夹">
              <a-tree-select
                v-model:value="folderForm.parentId"
                :tree-data="folderTreeData"
                :field-names="{ label: 'name', value: 'id', children: 'children' }"
                placeholder="根目录（可选）"
                allow-clear
                tree-default-expand-all
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="可见性">
              <a-select v-model:value="folderForm.visibility" style="width: 100%">
                <a-select-option value="private">私有</a-select-option>
                <a-select-option value="family">家庭可见</a-select-option>
                <a-select-option value="public">公开</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col v-if="folderForm.visibility === 'family'" :span="24">
            <a-form-item label="可见家庭" required>
              <a-select v-model:value="folderForm.familyIds" mode="multiple" placeholder="选择家庭" style="width: 100%">
                <a-select-option v-for="f in familyOptions" :key="f.id" :value="f.id">{{ f.name }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
      <template #footer>
        <a-button @click="closeFolderModal()">取消</a-button>
        <a-button type="primary" @click="handleCreateFolder">保存并关闭</a-button>
      </template>
    </HomeaiFormModal>

    <HomeaiFormModal @register="registerConvertModal" size="short" title="格式转换" @ok="handleConvert">
      <a-form-item label="源文件">
        <span>{{ convertForm.fileName }}</span>
      </a-form-item>
      <a-form-item label="目标格式" required>
        <a-select v-model:value="convertForm.targetFormat" placeholder="请选择目标格式" style="width:100%">
          <a-select-option v-for="rule in convertTargets" :key="rule.targetFormat" :value="rule.targetFormat">
            {{ rule.sourceFormat }} → {{ rule.targetFormat }}
          </a-select-option>
        </a-select>
      </a-form-item>
    </HomeaiFormModal>

    <HomeaiFormModal @register="registerFamilyQuotaModal" size="short" title="家庭存储配额" @ok="saveFamilyQuota">
      <a-form layout="vertical">
        <a-form-item label="家庭">
          <a-input :value="familyQuotaForm.familyName" disabled />
        </a-form-item>
        <a-form-item label="配额 (GB)">
          <a-input-number v-model:value="familyQuotaForm.limitGb" :min="0.1" :max="100" :step="0.5" style="width: 100%" />
          <div style="margin-top: 8px; font-size: 12px; color: #888">
            默认家庭配额 {{ formatSize(spaceStats.defaultFamilyLimitBytes || 0) }}；仅对该家庭生效
          </div>
        </a-form-item>
      </a-form>
      <template #footer>
        <a-button @click="closeFamilyQuotaModal">取消</a-button>
        <a-button v-if="familyQuotaForm.custom" danger @click="clearFamilyQuota">恢复默认</a-button>
        <a-button type="primary" @click="saveFamilyQuota">保存并关闭</a-button>
      </template>
    </HomeaiFormModal>

    <HomeaiFilePreviewModal ref="previewModalRef" />
  </PageWrapper>
</template>

<script lang="ts" name="homeai-storage-file" setup>
  import { PageWrapper } from '/@/components/Page';
import { ref, computed, onMounted } from 'vue';
  import { useModal } from '/@/components/Modal';
  import HomeaiFormModal from '../components/HomeaiFormModal.vue';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { Icon } from '/@/components/Icon';
  import { familyApi, storageApi, storageOfficeApi, storageRuleApi } from '/@/api/homeai';
  import type { HomeaiStorageFile, HomeaiStorageFolder } from '/@/api/homeai';
  import { useUserLabel } from '../hooks/useUserLabel';
  import { useGo } from '/@/hooks/web/usePage';
  import HomeaiFilePreviewModal from '../components/HomeaiFilePreviewModal.vue';
  import { toFamilyIdNameOptions } from '../utils/activeFamily';
  import { downloadHomeaiContent } from '../utils/homeaiFileContent';

  const { createMessage, createConfirm } = useMessage();
  const { resolveUserLabel, loadUserOptions } = useUserLabel();
  loadUserOptions();
  const go = useGo();

  const activeTab = ref('files');
  const recycleType = ref<'file' | 'folder'>('file');
  const recycleKeyword = ref('');
  const recycleFiles = ref<any[]>([]);
  const recycleLoading = ref(false);
  const recycleSelectedKeys = ref<string[]>([]);
  const recyclePagination = ref({
    current: 1,
    pageSize: 20,
    total: 0,
    showSizeChanger: true,
    showTotal: (t: number) => `共 ${t} 条`,
  });
  const recycleColumns = [
    { title: '预览', key: 'thumb', width: 64 },
    { title: '文件名', dataIndex: 'originalName', key: 'name' },
    { title: '扩展名', dataIndex: 'extension', width: 80 },
    { title: '上传者', dataIndex: 'userId', width: 120, customRender: ({ text }: any) => resolveUserLabel(text) },
    { title: '文件大小', dataIndex: 'fileSize', key: 'fileSize', width: 100 },
    { title: '删除时间', dataIndex: 'deletedAt', width: 160 },
    { title: '操作', key: 'action', width: 160 },
  ];
  const recycleFolderColumns = [
    { title: '文件夹', dataIndex: 'name', key: 'name' },
    { title: '可见性', dataIndex: 'visibility', key: 'visibility', width: 90 },
    { title: '创建者', dataIndex: 'userId', width: 120, customRender: ({ text }: any) => resolveUserLabel(text) },
    { title: '删除时间', dataIndex: 'deletedAt', width: 160 },
    { title: '操作', key: 'action', width: 160 },
  ];

  function onMainTabChange(key: string | number) {
    if (key === 'recycle') {
      loadRecycleBin();
    }
  }

  function onRecycleTypeChange() {
    recyclePagination.value.current = 1;
    recycleSelectedKeys.value = [];
    loadRecycleBin();
  }

  function onRecycleSelect(keys: (string | number)[]) {
    recycleSelectedKeys.value = keys.map(String);
  }

  function onRecycleTableChange(pag: any) {
    recyclePagination.value.current = pag.current;
    recyclePagination.value.pageSize = pag.pageSize;
    loadRecycleBin();
  }

  function recyclePayload(ids: string[]) {
    return recycleType.value === 'folder' ? { folderIds: ids } : { fileIds: ids };
  }

  async function loadRecycleBin() {
    recycleLoading.value = true;
    try {
      const res: any = await storageApi.recycleBin({
        pageNo: recyclePagination.value.current,
        pageSize: recyclePagination.value.pageSize,
        keyword: recycleKeyword.value || undefined,
        type: recycleType.value,
      });
      recycleFiles.value = res?.records || [];
      recyclePagination.value.total = res?.total || 0;
      recycleSelectedKeys.value = [];
    } catch {
      createMessage.error('回收站加载失败');
    } finally {
      recycleLoading.value = false;
    }
  }

  async function handleRestoreSelected(ids?: string[] | Event) {
    const targetIds = Array.isArray(ids) ? ids : recycleSelectedKeys.value;
    if (!targetIds.length) return;
    await storageApi.restore(recyclePayload(targetIds));
    createMessage.success('已恢复');
    await loadRecycleBin();
    await loadFolderTree();
    await loadCurrentDir();
    await loadSpaceStats();
  }

  async function doDeletePermanently(targetIds: string[]) {
    await storageApi.deletePermanently(recyclePayload(targetIds));
    createMessage.success('已彻底删除');
    await loadRecycleBin();
    await loadSpaceStats();
  }

  function handleDeletePermanentlySelected(ids?: string[] | Event) {
    const targetIds = Array.isArray(ids) ? ids : recycleSelectedKeys.value;
    if (!targetIds.length) return;
    // 行内已有 Popconfirm，批量操作再弹一次确认
    if (Array.isArray(ids)) {
      doDeletePermanently(targetIds);
      return;
    }
    const label = recycleType.value === 'folder' ? '文件夹' : '文件';
    createConfirm({
      iconType: 'warning',
      title: '彻底删除',
      content: `确定彻底删除选中的 ${targetIds.length} 个${label}？此操作不可恢复。`,
      onOk: () => doDeletePermanently(targetIds),
    });
  }

  const statsExpanded = ref(false);
  const spaceStats = ref<any>({ totalFiles: 0, totalSize: 0, perUser: [], perFamily: [] });
  const topUsers = computed(() =>
    [...(spaceStats.value.perUser || [])].sort((a, b) => (b.totalSize || 0) - (a.totalSize || 0)).slice(0, 5),
  );
  const topFamilies = computed(() =>
    [...(spaceStats.value.perFamily || [])].sort((a, b) => (b.totalSize || 0) - (a.totalSize || 0)).slice(0, 5),
  );

  function goFamilyQuota() {
    go('/homeai/storage/familyQuota');
  }

  async function loadSpaceStats() {
    try {
      spaceStats.value = (await storageApi.stats()) as any;
    } catch {
      // 忽略
    }
  }

  function formatSize(bytes: number): string {
    if (!bytes) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let i = 0;
    let size = bytes;
    while (size >= 1024 && i < units.length - 1) {
      size /= 1024;
      i++;
    }
    return size.toFixed(1) + ' ' + units[i];
  }
  const [registerUploadModal, { openModal: openUploadModal, closeModal: closeUploadModal }] = useModal();
  const [registerFolderModal, { openModal: openFolderModal, closeModal: closeFolderModal }] = useModal();
  const [registerConvertModal, { openModal: openConvertModalDialog, closeModal: closeConvertModal }] = useModal();
  const [registerFamilyQuotaModal, { openModal: openFamilyQuotaModal, closeModal: closeFamilyQuotaModal }] = useModal();

  const familyQuotaForm = ref({ familyId: '', familyName: '', limitGb: 5, custom: false });
  const GB = 1024 * 1024 * 1024;

  async function openFamilyQuota(f: any) {
    if (!f?.familyId) return;
    try {
      const cfg: any = await storageApi.getFamilyStorageLimit(f.familyId);
      familyQuotaForm.value = {
        familyId: f.familyId,
        familyName: f.familyName || f.familyId,
        limitGb: Number(((cfg?.limitBytes || spaceStats.value.defaultFamilyLimitBytes || 5 * GB) / GB).toFixed(2)),
        custom: !!cfg?.custom,
      };
    } catch {
      familyQuotaForm.value = {
        familyId: f.familyId,
        familyName: f.familyName || f.familyId,
        limitGb: Number(((f.limitBytes || 5 * GB) / GB).toFixed(2)),
        custom: !!f.customLimit,
      };
    }
    openFamilyQuotaModal(true);
  }

  async function saveFamilyQuota() {
    const gb = Number(familyQuotaForm.value.limitGb);
    if (!familyQuotaForm.value.familyId || !(gb > 0)) {
      createMessage.warning('请输入有效配额');
      return;
    }
    await storageApi.setFamilyStorageLimit(familyQuotaForm.value.familyId, Math.round(gb * GB));
    createMessage.success('家庭配额已更新');
    closeFamilyQuotaModal();
    await loadSpaceStats();
  }

  async function clearFamilyQuota() {
    if (!familyQuotaForm.value.familyId) return;
    await storageApi.clearFamilyStorageLimit(familyQuotaForm.value.familyId);
    createMessage.success('已恢复默认家庭配额');
    closeFamilyQuotaModal();
    await loadSpaceStats();
  }
  const selectedFile = ref<File | null>(null);
  const uploadFileList = ref<any[]>([]);
  const uploading = ref(false);
  const uploadAccept =
    '.jpg,.jpeg,.png,.gif,.bmp,.webp,.pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.mp4,.avi,.mov,.mkv,.webm,.mp3,.wav,.m4a,.aac,.zip,.rar,.7z,.txt,.csv,.md';
  const ROOT_KEY = '__root__';

  type ExplorerRow = {
    rowType: 'folder' | 'file';
    id: string;
    name: string;
    visibility?: string;
    userId?: string;
    createTime?: string;
    folder?: HomeaiStorageFolder;
    file?: HomeaiStorageFile;
  };

  type CrumbItem = { id: string | null; name: string };

  const folderTreeData = ref<HomeaiStorageFolder[]>([]);
  const selectedFolderId = ref<string | null>(null);
  const explorerRows = ref<ExplorerRow[]>([]);
  const listingLoading = ref(false);
  const selectedExplorerId = ref<string | null>(null);
  const folderEditId = ref('');

  function unwrapList(res: unknown): any[] {
    if (Array.isArray(res)) return res;
    if (res && typeof res === 'object') {
      const o = res as Record<string, unknown>;
      if (Array.isArray(o.records)) return o.records;
      if (Array.isArray(o.result)) return o.result;
    }
    return [];
  }

  function findFolder(nodes: HomeaiStorageFolder[], id: string): HomeaiStorageFolder | null {
    for (const n of nodes) {
      if (n.id === id) return n;
      if (n.children?.length) {
        const hit = findFolder(n.children, id);
        if (hit) return hit;
      }
    }
    return null;
  }

  function findFolderPath(nodes: HomeaiStorageFolder[], id: string, acc: HomeaiStorageFolder[] = []): HomeaiStorageFolder[] | null {
    for (const n of nodes) {
      const next = [...acc, n];
      if (n.id === id) return next;
      if (n.children?.length) {
        const hit = findFolderPath(n.children, id, next);
        if (hit) return hit;
      }
    }
    return null;
  }

  function pathContains(deletedId: string, currentId: string | null): boolean {
    if (!currentId) return false;
    if (currentId === deletedId) return true;
    const path = findFolderPath(folderTreeData.value, currentId) || [];
    return path.some((n) => n.id === deletedId);
  }

  const explorerTreeData = computed(() => [
    { id: ROOT_KEY, name: '全部资料', children: folderTreeData.value },
  ]);

  const treeSelectedKeys = computed(() => [selectedFolderId.value || ROOT_KEY]);

  const breadcrumbItems = computed<CrumbItem[]>(() => {
    const items: CrumbItem[] = [{ id: null, name: '全部资料' }];
    if (selectedFolderId.value) {
      const path = findFolderPath(folderTreeData.value, selectedFolderId.value) || [];
      path.forEach((n) => items.push({ id: n.id || null, name: n.name || '' }));
    }
    return items;
  });

  const canGoUp = computed(() => breadcrumbItems.value.length > 1);

  function currentChildFolders(): HomeaiStorageFolder[] {
    if (!selectedFolderId.value) return folderTreeData.value;
    return findFolder(folderTreeData.value, selectedFolderId.value)?.children || [];
  }
  const convertTargets = ref<any[]>([]);
  const convertForm = ref({
    fileId: '',
    fileName: '',
    sourceFormat: '',
    targetFormat: undefined as string | undefined,
  });

  const uploadForm = ref({
    folderId: undefined as string | undefined,
    visibility: 'private' as string,
    familyIds: [] as string[],
  });

  const folderForm = ref({
    name: '',
    parentId: undefined as string | undefined,
    visibility: 'private' as string,
    familyIds: [] as string[],
  });

  const familyOptions = ref<Array<{ id: string; name: string }>>([]);

  async function loadFamilyOptions() {
    try {
      const res = await familyApi.list({ pageNo: 1, pageSize: 500 });
      familyOptions.value = toFamilyIdNameOptions(res);
    } catch {
      createMessage.warning('家庭列表加载失败');
    }
  }

  const explorerColumns = [
    { title: '', key: 'type', width: 56 },
    { title: '名称', key: 'name' },
    { title: '可见性', dataIndex: 'visibility', key: 'visibility', width: 80 },
    { title: '大小', key: 'size', width: 110 },
    { title: '上传者 / 创建者', dataIndex: 'userId', width: 120, customRender: ({ text }: any) => resolveUserLabel(text) },
    { title: '时间', dataIndex: 'createTime', width: 160 },
    { title: '操作', key: 'action', width: 340 },
  ];

  function explorerCustomRow(record: ExplorerRow) {
    return {
      onClick: () => {
        selectedExplorerId.value = record.id;
      },
      onDblclick: () => {
        if (record.rowType === 'folder') {
          enterFolder(record.id);
        }
      },
    };
  }

  function explorerRowClass(record: ExplorerRow) {
    return selectedExplorerId.value === record.id ? 'explorer-row-selected' : '';
  }

  async function loadFolderTree() {
    try {
      const res = await storageApi.folderTree();
      folderTreeData.value = unwrapList(res) as HomeaiStorageFolder[];
    } catch {
      createMessage.error('文件夹树加载失败');
    }
  }

  async function loadCurrentDir() {
    listingLoading.value = true;
    try {
      let files: HomeaiStorageFile[] = [];
      if (selectedFolderId.value) {
        files = unwrapList(await storageApi.folderFiles(selectedFolderId.value));
      } else {
        files = unwrapList(await storageApi.rootFiles());
      }
      const folderRows: ExplorerRow[] = currentChildFolders()
        .filter((f) => f.id)
        .map((f) => ({
        rowType: 'folder',
        id: String(f.id),
        name: f.name || '',
        visibility: f.visibility,
        userId: f.userId,
        createTime: f.createTime as string | undefined,
        folder: f,
      }));
      const fileRows: ExplorerRow[] = files
        .filter((file) => file.id)
        .map((file) => ({
        rowType: 'file',
        id: String(file.id),
        name: file.originalName || '',
        visibility: file.visibility,
        userId: file.userId,
        createTime: file.createTime,
        file,
      }));
      explorerRows.value = [...folderRows, ...fileRows];
    } catch {
      explorerRows.value = [];
      createMessage.error('目录内容加载失败');
    } finally {
      listingLoading.value = false;
    }
  }

  async function refreshExplorer() {
    await loadFolderTree();
    await loadCurrentDir();
    await loadSpaceStats();
  }

  function enterFolder(id: string | null) {
    selectedFolderId.value = id;
    selectedExplorerId.value = null;
    loadCurrentDir();
  }

  function goUp() {
    const items = breadcrumbItems.value;
    if (items.length <= 1) return;
    enterFolder(items[items.length - 2].id);
  }

  function onFolderSelect(selectedKeys: (string | number)[]) {
    if (!selectedKeys.length) return;
    const key = String(selectedKeys[0]);
    enterFolder(key === ROOT_KEY ? null : key);
  }


  function getFileExtension(record: HomeaiStorageFile): string {
    if (record.extension) return String(record.extension).toLowerCase().replace(/^\./, '');
    const name = record.originalName || '';
    const idx = name.lastIndexOf('.');
    return idx >= 0 ? name.substring(idx + 1).toLowerCase() : '';
  }

  function canConvertToPdf(record: HomeaiStorageFile): boolean {
    const ext = getFileExtension(record);
    return ['doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'txt', 'csv'].includes(ext);
  }

  async function loadConvertTargets(sourceFormat: string) {
    try {
      convertTargets.value = ((await storageRuleApi.targets(sourceFormat)) as any[]) || [];
    } catch {
      convertTargets.value = [];
    }
  }

  async function openConvertModal(record: HomeaiStorageFile) {
    const sourceFormat = getFileExtension(record);
    if (!sourceFormat) {
      createMessage.warning('无法识别文件格式');
      return;
    }
    convertForm.value = {
      fileId: record.id,
      fileName: record.originalName,
      sourceFormat,
      targetFormat: undefined,
    };
    await loadConvertTargets(sourceFormat);
    if (convertTargets.value.length === 0) {
      createMessage.warning('暂无可用转换规则，请先在「转换规则」中配置');
      return;
    }
    openConvertModalDialog(true);
  }

  async function handleConvert() {
    if (!convertForm.value.targetFormat) {
      createMessage.warning('请选择目标格式');
      return;
    }
    await storageOfficeApi.convert({
      fileId: convertForm.value.fileId,
      sourceFormat: convertForm.value.sourceFormat,
      targetFormat: convertForm.value.targetFormat,
    });
    createMessage.success('转换任务已提交，请在「处理记录」中查看进度');
    closeConvertModal();
  }

  async function handleConvertToPdf(record: HomeaiStorageFile) {
    const sourceFormat = getFileExtension(record);
    await loadConvertTargets(sourceFormat);
    const pdfRule = convertTargets.value.find((r) => r.targetFormat === 'pdf');
    if (!pdfRule) {
      createMessage.warning('暂无转 PDF 规则，请先在「转换规则」中配置');
      return;
    }
    await storageOfficeApi.convert({ fileId: record.id, sourceFormat, targetFormat: 'pdf' });
    createMessage.success('PDF 转换任务已提交，请在「处理记录」中查看进度');
  }

  function openCreateFolderModal() {
    folderEditId.value = '';
    folderForm.value = {
      name: '',
      parentId: selectedFolderId.value ?? undefined,
      visibility: 'private',
      familyIds: [],
    };
    loadFamilyOptions();
    loadFolderTree();
    openFolderModal(true);
  }

  async function handleCreateFolder() {
    if (!folderForm.value.name.trim()) {
      createMessage.warning('请输入文件夹名称');
      return;
    }
    if (folderForm.value.visibility === 'family' && !folderForm.value.familyIds.length) {
      createMessage.warning('家庭可见请至少选择一个家庭');
      return;
    }
    const params: any = { name: folderForm.value.name.trim(), visibility: folderForm.value.visibility };
    if (folderForm.value.parentId) {
      params.parentId = folderForm.value.parentId;
    }
    if (folderForm.value.visibility === 'family' && folderForm.value.familyIds.length) {
      params.familyIds = folderForm.value.familyIds.join(',');
    }
    if (folderEditId.value) {
      await storageApi.updateFolder(folderEditId.value, { ...params, familyIds: folderForm.value.familyIds });
      createMessage.success('文件夹修改成功');
    } else {
      await storageApi.createFolder(params);
      createMessage.success('文件夹创建成功');
    }
    closeFolderModal();
    await loadFolderTree();
    await loadCurrentDir();
  }

  /** 树节点 familyIds 可能是数组或逗号串 */
  function normalizeFamilyIds(raw: unknown): string[] {
    if (Array.isArray(raw)) return raw.map(String).filter(Boolean);
    if (typeof raw === 'string' && raw.trim()) {
      return raw.split(',').map((s) => s.trim()).filter(Boolean);
    }
    return [];
  }

  function openEditFolderModal(record?: HomeaiStorageFolder) {
    if (!record?.id) return;
    folderEditId.value = record.id;
    folderForm.value = {
      name: record.name,
      parentId: record.parentId || undefined,
      visibility: record.visibility || 'private',
      familyIds: normalizeFamilyIds(record.familyIds),
    };
    loadFamilyOptions();
    loadFolderTree();
    openFolderModal(true);
  }

  async function handleDeleteFolder(id: string) {
    const node = findFolder(folderTreeData.value, id);
    const parentId = node?.parentId ? String(node.parentId) : null;
    try {
      await storageApi.deleteFolder(id);
      createMessage.success('文件夹已删除');
      if (pathContains(id, selectedFolderId.value)) {
        selectedFolderId.value = parentId && parentId !== '0' ? parentId : null;
      }
      await loadFolderTree();
      if (selectedFolderId.value && !findFolder(folderTreeData.value, selectedFolderId.value)) {
        selectedFolderId.value = null;
      }
      await loadCurrentDir();
      await loadSpaceStats();
    } catch (e: any) {
      createMessage.error(e?.message || '删除失败');
    }
  }

  async function handleUploadModal() {
    uploadForm.value = {
      folderId: selectedFolderId.value ?? undefined,
      visibility: 'private',
      familyIds: [],
    };
    await loadFamilyOptions();
    selectedFile.value = null;
    uploadFileList.value = [];
    uploading.value = false;
    await loadFolderTree();
    openUploadModal(true);
  }

  function beforeSelectUploadFile(file: File) {
    selectedFile.value = file;
    uploadFileList.value = [
      {
        uid: `${file.name}-${file.size}-${file.lastModified}`,
        name: file.name,
        status: 'done',
        size: file.size,
        originFileObj: file,
      },
    ];
    return false;
  }

  function clearSelectedUploadFile() {
    selectedFile.value = null;
    uploadFileList.value = [];
  }

  async function handleUpload() {
    if (!selectedFile.value) {
      createMessage.warning('请选择文件');
      return;
    }
    if (uploadForm.value.visibility === 'family' && !uploadForm.value.familyIds.length) {
      createMessage.warning('家庭可见请至少选择一个家庭');
      return;
    }
    const extra: any = { visibility: uploadForm.value.visibility };
    if (uploadForm.value.folderId) {
      extra.folderId = uploadForm.value.folderId;
    }
    if (uploadForm.value.visibility === 'family' && uploadForm.value.familyIds.length) {
      extra.familyIds = uploadForm.value.familyIds.join(',');
    }
    uploading.value = true;
    try {
      const res: any = await defHttp.uploadFile(
        { url: '/homeai/storage/files/upload' },
        { file: selectedFile.value, name: 'file', data: extra },
        { isReturnResponse: true },
      );
      if (!res || res.success !== true) {
        throw new Error(res?.message || '上传失败');
      }
      createMessage.success('上传成功');
      closeUploadModal();
      clearSelectedUploadFile();
      await loadFolderTree();
      await loadSpaceStats();
      await loadCurrentDir();
    } catch (e: any) {
      createMessage.error(e?.message || '上传失败');
    } finally {
      uploading.value = false;
    }
  }

  async function handleDownload(record: HomeaiStorageFile) {
    if (!record.id) {
      createMessage.warning('无法下载：缺少文件编号');
      return;
    }
    try {
      await downloadHomeaiContent('storage', record.id, record.originalName);
    } catch (e: any) {
      createMessage.error(e?.message || '下载失败');
    }
  }

  const previewModalRef = ref<{ open: (src: { module: 'storage'; id: string; title?: string }) => void } | null>(null);
  function openPreview(record: HomeaiStorageFile) {
    if (!record.id) return;
    previewModalRef.value?.open({ module: 'storage', id: record.id, title: record.originalName });
  }

  async function handleDeleteFile(record: HomeaiStorageFile) {
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除文件「${record.originalName}」吗？`,
      onOk: async () => {
        await storageApi.deleteFile(record.id);
        createMessage.success('删除成功');
        await loadCurrentDir();
        loadFolderTree();
        loadSpaceStats();
      },
    });
  }

  onMounted(() => {
    loadFolderTree().then(() => loadCurrentDir());
    loadSpaceStats();
    loadFamilyOptions();
  });
</script>

<style scoped lang="less">
  .stats-panel {
    margin: 0 0 12px;
  }

  .explorer {
    display: flex;
    min-height: 520px;
    border: 1px solid var(--hai-admin-border, #ece9e2);
    border-radius: 8px;
    background: #fff;
    overflow: hidden;
  }

  .explorer-tree {
    width: 26%;
    min-width: 220px;
    max-width: 320px;
    border-right: 1px solid var(--hai-admin-border, #ece9e2);
    padding: 8px 4px 12px;
    overflow: auto;
  }

  .explorer-tree-title {
    padding: 4px 12px 8px;
    font-weight: 600;
    color: var(--hai-admin-text, #2c271f);
  }

  .tree-node-title {
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }

  .explorer-main {
    flex: 1;
    min-width: 0;
    padding: 8px 12px 12px;
  }

  .explorer-crumb {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 6px;
    padding: 4px 0 12px;
  }

  .crumb-sep {
    color: #bbb;
  }

  .crumb-link {
    color: #1890ff;
  }

  .crumb-current {
    font-weight: 600;
    color: var(--hai-admin-text, #2c271f);
  }

  .name-link {
    color: #1890ff;
  }

  .file-thumb {
    width: 40px;
    height: 40px;
    object-fit: cover;
    border-radius: 4px;
    cursor: pointer;
  }

  :deep(.explorer-row-selected) td {
    background: var(--hai-admin-primary-soft, rgba(27, 79, 138, 0.08)) !important;
  }

  .upload-form {
    padding: 0;
  }

  .upload-file-meta {
    margin-top: 8px;
    color: var(--hai-admin-text-muted, #64748b);
    font-size: 13px;
  }
</style>
