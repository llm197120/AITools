<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑学习资料' : '新增学习资料'" width="40%">
    <BasicForm @register="registerForm" @submit="handleSubmit">
      <template #fileUrlSlot="{ model, field }">
        <div style="display: flex; gap: 8px; align-items: center">
          <input ref="fileInputRef" type="file" style="display: none" @change="onFileChange" />
          <a-button type="primary" :loading="uploading" @click="fileInputRef?.click()">选择文件</a-button>
          <span v-if="selectedFileName" style="color: #666; flex: 1; overflow: hidden; text-overflow: ellipsis">{{ selectedFileName }}</span>
          <span v-else-if="model[field]" style="color: #52c41a; flex: 1">已上传文件</span>
          <span v-else style="color: #999">请上传资料文件</span>
        </div>
      </template>
    </BasicForm>
    <template #footer>
      <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
      <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-learn-drawer" setup>
  import { ref, onMounted } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { learnApi } from '/@/api/homeai';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');
  const saving = ref(false);
  const uploading = ref(false);
  const fileInputRef = ref<HTMLInputElement | null>(null);
  const selectedFileName = ref('');
  const categoryOptions = ref<{ label: string; value: string }[]>([]);

  async function loadCategoryOptions() {
    try {
      const list: any[] = (await learnApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
    } catch {
      categoryOptions.value = [];
    }
  }

  onMounted(loadCategoryOptions);

  const [registerDrawer, { closeDrawer }] = useDrawerInner((data) => {
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    selectedFileName.value = '';
    if (fileInputRef.value) fileInputRef.value.value = '';
    if (isUpdate.value && data.record) {
      setFieldsValue({ ...data.record });
      if (data.record.fileUrl) {
        selectedFileName.value = '已有文件（重新选择可替换）';
      }
    } else {
      resetFields();
    }
  });

  const [registerForm, { setFieldsValue, resetFields, submit, getFieldsValue }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'title', label: '标题', component: 'Input', required: true },
      {
        field: 'type',
        label: '类型',
        component: 'Select',
        required: true,
        componentProps: {
          options: [
            { label: '视频', value: 'video' },
            { label: '图片', value: 'image' },
            { label: 'PDF', value: 'pdf' },
            { label: '文档', value: 'doc' },
            { label: '表格', value: 'xls' },
            { label: 'PPT', value: 'ppt' },
            { label: '链接', value: 'link' },
            { label: '笔记', value: 'note' },
          ],
        },
        defaultValue: 'video',
      },
      { field: 'categoryId', label: '分类', component: 'Select', componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' } },
      { field: 'tags', label: '标签', component: 'Input', help: '多个标签用逗号分隔' },
      { field: 'fileUrl', label: '资料文件', component: 'Input', slot: 'fileUrlSlot' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function onFileChange(e: Event) {
    const target = e.target as HTMLInputElement;
    if (!target.files || !target.files[0]) return;
    const file = target.files[0];
    selectedFileName.value = file.name;
    uploading.value = true;
    try {
      if (isUpdate.value && recordId.value) {
        const url: any = await defHttp.uploadFile(
          { url: learnApi.uploadFile(recordId.value) },
          { file, name: 'file' },
        );
        setFieldsValue({ fileUrl: url });
      } else {
        const { type } = await getFieldsValue();
        if (!type) {
          createMessage.warning('请先选择资料类型');
          selectedFileName.value = '';
          return;
        }
        const url: any = await defHttp.uploadFile(
          { url: learnApi.uploadTemp() },
          { file, name: 'file', data: { type } },
        );
        setFieldsValue({ fileUrl: url });
      }
      createMessage.success('文件上传成功');
    } catch (err: any) {
      createMessage.error(err?.message || '上传失败');
      selectedFileName.value = '';
    } finally {
      uploading.value = false;
      target.value = '';
    }
  }

  async function handleSubmit(values: any) {
    saving.value = true;
    try {
      if (isUpdate.value) {
        await learnApi.edit(recordId.value, values);
        createMessage.success('编辑成功');
      } else {
        await learnApi.add(values);
        createMessage.success('新增成功');
      }
      closeDrawer();
      emit('success');
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '操作失败');
      return false;
    } finally {
      saving.value = false;
    }
  }
</script>
