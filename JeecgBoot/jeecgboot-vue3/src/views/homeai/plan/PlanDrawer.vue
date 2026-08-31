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
  import { ref, onMounted } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { planApi, recipeApi } from '/@/api/homeai';
  import type { HomeaiCategory, HomeaiPayload, HomeaiRecipe } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useUserLabel } from '../hooks/useUserLabel';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');
  const originalUserId = ref('');
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const recipeOptions = ref<{ label: string; value: string }[]>([]);
  const { userOptions, loadUserOptions } = useUserLabel();

  onMounted(() => {
    loadUserOptions();
  });

  const repeatRuleOptions = [
    { label: '不重复', value: 'none' },
    { label: '每天', value: 'daily' },
    { label: '每周', value: 'weekly' },
    { label: '每月', value: 'monthly' },
  ];

  async function loadCategories() {
    try {
      const list: HomeaiCategory[] = (await planApi.categories()) || [];
      categoryOptions.value = list.map((c) => ({ label: c.name, value: c.name }));
    } catch {
      categoryOptions.value = [];
      createMessage.warning('分类加载失败，请关闭后重试');
    }
  }

  async function loadRecipes() {
    try {
      const res = await recipeApi.list({ pageNo: 1, pageSize: 200 });
      const records = Array.isArray(res) ? res : res?.records || [];
      recipeOptions.value = records.map((r: HomeaiRecipe) => ({
        label: r.name,
        value: r.id,
      }));
    } catch {
      recipeOptions.value = [];
      createMessage.warning('菜谱列表加载失败，请关闭后重试');
    }
  }

  const [registerDrawer, { closeDrawer }] = useDrawerInner(async (data) => {
    await Promise.all([loadCategories(), loadRecipes()]);
    updateSchema([
      {
        field: 'category',
        componentProps: {
          options: categoryOptions.value,
          placeholder: '请选择分类',
          allowClear: true,
        },
      },
      {
        field: 'recipeId',
        componentProps: {
          options: recipeOptions.value,
          placeholder: '可选，关联菜谱',
          allowClear: true,
          showSearch: true,
          optionFilterProp: 'label',
        },
      },
    ]);
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    originalUserId.value = data.record?.userId || '';
    if (isUpdate.value && data.record) {
      setFieldsValue({ ...data.record });
    } else {
      resetFields();
    }
  });

  const [registerForm, { setFieldsValue, resetFields, submit, updateSchema }] = useForm({
    labelWidth: 100,
    schemas: [
      {
        field: 'userId',
        label: '所属用户',
        component: 'Select',
        required: true,
        dynamicDisabled: () => isUpdate.value && !!originalUserId.value,
        componentProps: {
          options: userOptions,
          showSearch: true,
          optionFilterProp: 'label',
          placeholder: '请选择归属用户',
        },
      },
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
        field: 'recipeId',
        label: '关联菜谱',
        component: 'Select',
        componentProps: {
          options: recipeOptions,
          placeholder: '可选，关联菜谱',
          allowClear: true,
          showSearch: true,
          optionFilterProp: 'label',
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
      {
        field: 'repeatRule',
        label: '重复规则',
        component: 'Select',
        componentProps: {
          options: repeatRuleOptions,
          placeholder: '请选择重复规则',
        },
        defaultValue: 'none',
      },
      {
        field: 'isAllDay',
        label: '全天',
        component: 'Select',
        componentProps: {
          options: [
            { label: '否', value: 0 },
            { label: '是', value: 1 },
          ],
        },
        defaultValue: 0,
      },
      {
        field: 'startTime',
        label: '开始时间',
        component: 'TimePicker',
        componentProps: {
          format: 'HH:mm',
          valueFormat: 'HH:mm:ss',
          placeholder: '请选择开始时间',
        },
        ifShow: ({ values }) => values.isAllDay !== 1,
      },
      {
        field: 'endTime',
        label: '结束时间',
        component: 'TimePicker',
        componentProps: {
          format: 'HH:mm',
          valueFormat: 'HH:mm:ss',
          placeholder: '请选择结束时间',
        },
        ifShow: ({ values }) => values.isAllDay !== 1,
      },
      {
        field: 'remindMinutes',
        label: '提前提醒(分)',
        component: 'InputNumber',
        componentProps: {
          min: 0,
          placeholder: '提前多少分钟提醒',
          style: { width: '100%' },
        },
      },
      { field: 'content', label: '内容', component: 'InputTextArea' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: HomeaiPayload) {
    values.title = String(values.title || '').trim();
    if (!values.title) {
      createMessage.warning('请输入标题');
      return false;
    }
    if (!values.userId) {
      createMessage.warning('请选择归属用户');
      return false;
    }
    if (values.content != null) values.content = String(values.content).trim();
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
