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
          mode="file"
          :upload-url="isUpdate && recordId ? learnApi.uploadFile(recordId) : learnApi.uploadTemp()"
          :extra-data="() => ({ type: model.type })"
          :accept="getLearnAccept(model.type)"
          tip="点击或拖拽文件到此处上传，格式需与资料类型匹配"
        />
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
  import type { HomeaiCategory, HomeaiPayload } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';
  import HomeaiMediaUpload from '/@/views/homeai/components/HomeaiMediaUpload.vue';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');
  const saving = ref(false);
  const categoryOptions = ref<{ label: string; value: string }[]>([]);

  /** 资料类型 → 允许上传的文件类型 */
  function getLearnAccept(type?: string): string {
    const map: Record<string, string> = {
      video: 'video/*',
      image: 'image/*',
      pdf: '.pdf',
      doc: '.doc,.docx',
      xls: '.xls,.xlsx',
      ppt: '.ppt,.pptx',
      note: '.txt,.md',
    };
    return (type && map[type]) || '';
  }

  async function loadCategoryOptions() {
    try {
      const list: HomeaiCategory[] = (await learnApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.id }));
    } catch {
      categoryOptions.value = [];
    }
  }

  onMounted(loadCategoryOptions);

  const [registerDrawer, { closeDrawer }] = useDrawerInner((data) => {
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    if (isUpdate.value && data.record) {
      setFieldsValue({ ...data.record });
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

  async function handleSubmit(values: HomeaiPayload) {
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
