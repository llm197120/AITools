<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑学习资料' : '新增学习资料'" width="40%">
    <BasicForm @register="registerForm" @submit="handleSubmit">
      <template #fileUrlSlot="{ model, field }">
        <template v-if="model.type === 'link'">
          <a-input v-model:value="model[field]" placeholder="填写链接地址，例如 https://..." />
        </template>
        <HomeaiMediaUpload
          v-else
          v-model:value="model[field]"
          :mode="learnUploadMode(model.type)"
          :upload-url="isUpdate && recordId ? learnApi.uploadFile(recordId) : learnApi.uploadTemp()"
          :extra-data="() => ({ type: model.type })"
          :accept="getLearnAccept(model.type)"
          :max-size="learnMaxSize(model.type)"
          :content-module="isUpdate && recordId ? 'learn' : undefined"
          :content-id="isUpdate && recordId ? recordId : undefined"
          tip="点击或拖拽文件到此处上传，格式需与资料类型匹配"
        />
        <a-button
          v-if="isUpdate && recordId && model.type !== 'link'"
          type="link"
          style="padding-left: 0"
          @click="openPreview"
        >预览资料</a-button>
      </template>
    </BasicForm>
    <template #footer>
      <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
      <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
    </template>
  </BasicDrawer>
  <HomeaiFilePreviewModal ref="previewModalRef" />
</template>

<script lang="ts" name="homeai-learn-drawer" setup>
  import { ref, onMounted } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { learnApi } from '/@/api/homeai';
  import type { HomeaiCategory, HomeaiPayload } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';
  import HomeaiMediaUpload from '/@/views/homeai/components/HomeaiMediaUpload.vue';
  import HomeaiFilePreviewModal from '/@/views/homeai/components/HomeaiFilePreviewModal.vue';
  import { useUserLabel } from '../hooks/useUserLabel';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');
  const originalUserId = ref('');
  const saving = ref(false);
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const { userOptions, loadUserOptions } = useUserLabel();
  const formActions = { setFieldsValue: (_values: Record<string, any>) => {} };

  /** 资料类型 → 允许上传的文件类型 */
  function getLearnAccept(type?: string): string {
    const map: Record<string, string> = {
      video: 'video/*,.mp4,.mov,.webm,.mkv,.avi',
      audio: 'audio/*,.mp3,.wav,.m4a,.aac',
      image: 'image/*,.jpg,.jpeg,.png,.gif,.webp,.bmp',
      pdf: '.pdf',
      doc: '.doc,.docx',
      xls: '.xls,.xlsx',
      ppt: '.ppt,.pptx',
      note: '.txt,.md',
    };
    return (type && map[type]) || '';
  }

  function learnUploadMode(type?: string): 'image' | 'video' | 'audio' | 'file' {
    if (type === 'image') return 'image';
    if (type === 'video') return 'video';
    if (type === 'audio') return 'audio';
    return 'file';
  }

  function learnMaxSize(type?: string): number {
    if (type === 'video') return 200;
    if (type === 'audio' || type === 'pdf' || type === 'doc' || type === 'xls' || type === 'ppt') return 50;
    if (type === 'image') return 20;
    return 10;
  }

  const previewModalRef = ref<{ open: (src: { module: 'learn'; id: string }) => void } | null>(null);
  function openPreview() {
    if (!recordId.value) return;
    previewModalRef.value?.open({ module: 'learn', id: recordId.value });
  }

  async function loadCategoryOptions() {
    try {
      const list: HomeaiCategory[] = (await learnApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
    } catch {
      categoryOptions.value = [];
      createMessage.warning('分类加载失败，请关闭后重试');
    }
  }

  onMounted(async () => {
    await Promise.all([loadCategoryOptions(), loadUserOptions()]);
  });

  const [registerDrawer, { closeDrawer }] = useDrawerInner((data) => {
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    originalUserId.value = data.record?.userId || '';
    if (isUpdate.value && data.record) {
      setFieldsValue({
        title: data.record.title,
        type: data.record.type,
        categoryId: data.record.categoryId,
        tags: data.record.tags,
        fileUrl: data.record.fileUrl,
        userId: data.record.userId,
      });
    } else {
      resetFields();
    }
  });

  const [registerForm, { setFieldsValue, resetFields, submit }] = useForm({
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
            { label: '音频', value: 'audio' },
            { label: '图片', value: 'image' },
            { label: 'PDF', value: 'pdf' },
            { label: '文档', value: 'doc' },
            { label: '表格', value: 'xls' },
            { label: 'PPT', value: 'ppt' },
            { label: '链接', value: 'link' },
            { label: '笔记', value: 'note' },
          ],
          onChange: () => formActions.setFieldsValue({ fileUrl: '' }),
        },
        defaultValue: 'note',
      },
      {
        field: 'userId',
        label: '所属用户',
        component: 'Select',
        required: true,
        dynamicDisabled: () => isUpdate.value && !!originalUserId.value,
        componentProps: {
          options: userOptions,
          showSearch: true,
          placeholder: '请选择归属用户',
        },
      },
      {
        field: 'categoryId',
        label: '分类',
        component: 'Select',
        componentProps: { options: categoryOptions, allowClear: true, placeholder: '请选择分类' },
      },
      { field: 'tags', label: '标签', component: 'Input', help: '多个标签用逗号分隔' },
      { field: 'fileUrl', label: '资料文件', component: 'Input', slot: 'fileUrlSlot' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });
  formActions.setFieldsValue = setFieldsValue;

  async function handleSubmit(values: HomeaiPayload) {
    const type = String(values.type || '');
    const fileUrl = String(values.fileUrl || '').trim();
    values.title = String(values.title || '').trim();
    if (!values.title) {
      createMessage.warning('请输入标题');
      return false;
    }
    if (!values.userId) {
      createMessage.warning('请选择归属用户');
      return false;
    }
    if (type === 'link') {
      if (!fileUrl) {
        createMessage.warning('请填写链接地址');
        return false;
      }
      if (!/^https?:\/\//i.test(fileUrl)) {
        createMessage.warning('链接请以 http:// 或 https:// 开头');
        return false;
      }
      values.fileUrl = fileUrl;
    }
    if (type && !['link', 'note'].includes(type) && !fileUrl) {
      createMessage.warning('请上传与类型匹配的资料文件');
      return false;
    }
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
