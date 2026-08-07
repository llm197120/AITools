<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑计划' : '新增计划'" width="40%">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <template #footer>
      <a-button type="primary" @click="submit">保存</a-button>
      <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-plan-drawer" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { planApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');
  const categoryOptions = ref<{ label: string; value: string }[]>([]);

  async function loadCategories() {
    try {
      const list: any[] = (await planApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.name }));
    } catch {
      categoryOptions.value = [];
    }
  }

  const [registerDrawer, { closeDrawer }] = useDrawerInner(async (data) => {
    await loadCategories();
    updateSchema({
      field: 'category',
      componentProps: {
        options: categoryOptions.value,
        placeholder: '请选择分类',
        allowClear: true,
      },
    });
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    if (isUpdate.value && data.record) {
      setFieldsValue({ ...data.record });
    } else {
      resetFields();
    }
  });

  const [registerForm, { setFieldsValue, resetFields, submit, updateSchema }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'title', label: '标题', component: 'Input', required: true },
      { field: 'planDate', label: '日期', component: 'DatePicker', required: true, defaultValue: null },
      {
        field: 'category',
        label: '分类',
        component: 'Select',
        componentProps: {
          options: categoryOptions,
          placeholder: '请选择分类',
          allowClear: true,
        },
      },
      {
        field: 'priority',
        label: '优先级',
        component: 'Select',
        componentProps: {
          options: [
            { label: '紧急', value: 'urgent' },
            { label: '重要', value: 'important' },
            { label: '普通', value: 'normal' },
          ],
        },
        defaultValue: 'normal',
      },
      { field: 'content', label: '内容', component: 'InputTextArea' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: any) {
    try {
      if (isUpdate.value) {
        await planApi.edit(recordId.value, values);
        createMessage.success('编辑成功');
      } else {
        await planApi.add(values);
        createMessage.success('新增成功');
      }
      closeDrawer();
      emit('success');
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '操作失败');
      return false;
    }
  }
</script>
