<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <a-alert
      type="info"
      show-icon
      style="margin-bottom: 16px"
      message="配置允许上传的文件扩展名。危险类型（html/js/exe 等）始终禁止，无法加入白名单。"
    />
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd">新增扩展名</a-button>
        <a-button type="primary" preIcon="ant-design:save-outlined" :loading="saving" @click="handleSave">保存配置</a-button>
      </template>
      <template #action="{ record }">
        <a-button type="link" danger @click="handleRemove(record)">删除</a-button>
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'extension'">
          <a-input v-model:value="record.extension" placeholder="如 pdf" />
        </template>
        <template v-if="column.key === 'category'">
          <a-select v-model:value="record.category" style="width: 120px" :options="categoryOptions" />
        </template>
        <template v-if="column.key === 'isEnabled'">
          <a-switch :checked="record.isEnabled === 1" @change="(v: boolean) => (record.isEnabled = v ? 1 : 0)" />
        </template>
      </template>
    </BasicTable>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-file-whitelist" setup>
  import { PageWrapper } from '/@/components/Page';
  import { onMounted, ref } from 'vue';
  import { BasicTable, useTable } from '/@/components/Table';
  import { configApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage } = useMessage();
  const items = ref<any[]>([]);
  const saving = ref(false);

  const categoryOptions = [
    { label: '图片', value: 'image' },
    { label: '文档', value: 'doc' },
    { label: '视频', value: 'video' },
    { label: '压缩包', value: 'archive' },
    { label: '文本', value: 'text' },
    { label: '其他', value: 'other' },
  ];

  const [registerTable, { setTableData }] = useTable({
    title: '文件白名单配置',
    dataSource: items,
    columns: [
      { title: '扩展名', dataIndex: 'extension', key: 'extension', width: 150 },
      { title: '分类', dataIndex: 'category', key: 'category', width: 140 },
      { title: '排序', dataIndex: 'sortOrder', width: 80 },
      { title: '启用', dataIndex: 'isEnabled', key: 'isEnabled', width: 80 },
    ],
    pagination: false,
    useSearchForm: false,
    showIndexColumn: true,
    actionColumn: { width: 80, title: '操作', dataIndex: 'action', slots: { customRender: 'action' } },
  });

  async function loadData() {
    const res: any = await configApi.getFileWhitelist();
    items.value = (res?.items || []).map((r: any, i: number) => ({
      ...r,
      sortOrder: r.sortOrder ?? i + 1,
      isEnabled: r.isEnabled ?? 1,
      category: r.category || 'other',
    }));
    setTableData(items.value);
  }

  function handleAdd() {
    items.value.push({
      extension: '',
      category: 'other',
      sortOrder: items.value.length + 1,
      isEnabled: 1,
    });
    setTableData([...items.value]);
  }

  function handleRemove(record: any) {
    const index = items.value.indexOf(record);
    if (index >= 0) items.value.splice(index, 1);
    setTableData([...items.value]);
  }

  async function handleSave() {
    const payload = items.value.map((r, i) => ({
      extension: (r.extension || '').trim().replace(/^\./, '').toLowerCase(),
      category: r.category || 'other',
      sortOrder: r.sortOrder ?? i + 1,
      isEnabled: r.isEnabled ?? 1,
    }));
    if (payload.some((r) => !r.extension)) {
      createMessage.warning('请填写所有扩展名');
      return;
    }
    saving.value = true;
    try {
      await configApi.updateFileWhitelist(payload);
      createMessage.success('保存成功');
      await loadData();
    } catch (e: any) {
      createMessage.error(e?.message || '保存失败');
    } finally {
      saving.value = false;
    }
  }

  onMounted(loadData);
</script>
