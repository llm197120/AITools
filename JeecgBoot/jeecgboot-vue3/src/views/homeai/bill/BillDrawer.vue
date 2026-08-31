<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑账单' : '新增账单'" width="40%">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <template #footer>
      <a-button type="primary" @click="submit">保存</a-button>
      <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-bill-drawer" setup>
  import { ref, onMounted } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { billApi } from '/@/api/homeai';
import type { HomeaiCategory, HomeaiPayload } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useUserLabel } from '../hooks/useUserLabel';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');
  const originalUserId = ref('');
  const categoryOptions = ref<{ label: string; value: string }[]>([]);
  const { userOptions, loadUserOptions } = useUserLabel();

  onMounted(() => {
    loadUserOptions();
  });

  async function loadCategories(type?: string) {
    try {
      const res = await billApi.categoryList({ pageNo: 1, pageSize: 200, type: type || undefined });
      const records = Array.isArray(res) ? res : res?.records || [];
      categoryOptions.value = records.map((c: HomeaiCategory) => ({
        label: c.name || c.id || '',
        value: c.id || '',
      }));
    } catch {
      categoryOptions.value = [];
      createMessage.warning('分类加载失败，请关闭后重试');
    }
  }

  async function applyCategorySchema() {
    updateSchema([
      {
        field: 'categoryId',
        componentProps: {
          options: categoryOptions.value,
          allowClear: true,
          showSearch: true,
          optionFilterProp: 'label',
          placeholder: '请选择分类',
        },
      },
    ]);
  }

  async function handleTypeChange(type: string) {
    await loadCategories(type);
    await applyCategorySchema();
    setFieldsValue({ categoryId: undefined });
  }

  const [registerDrawer, { closeDrawer }] = useDrawerInner(async (data) => {
    isUpdate.value = data.isUpdate || false;
    recordId.value = data.record?.id || '';
    originalUserId.value = data.record?.userId || '';
    const type = data.record?.type || 'expense';
    await loadCategories(type);
    await applyCategorySchema();
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
      { field: 'billDate', label: '日期', component: 'DatePicker', required: true, defaultValue: null },
      {
        field: 'type',
        label: '类型',
        component: 'Select',
        required: true,
        componentProps: {
          options: [
            { label: '收入', value: 'income' },
            { label: '支出', value: 'expense' },
          ],
          onChange: (val: string) => handleTypeChange(val),
        },
        defaultValue: 'expense',
      },
      { field: 'amount', label: '金额', component: 'InputNumber', required: true },
      {
        field: 'categoryId',
        label: '分类',
        component: 'Select',
        required: true,
        componentProps: {
          options: categoryOptions,
          allowClear: true,
          showSearch: true,
          optionFilterProp: 'label',
          placeholder: '请选择分类',
        },
      },
      {
        field: 'paymentMethod',
        label: '支付方式',
        component: 'Select',
        componentProps: {
          options: [
            { label: '微信', value: '微信' },
            { label: '支付宝', value: '支付宝' },
            { label: '现金', value: '现金' },
            { label: '银行卡', value: '银行卡' },
            { label: '其他', value: '其他' },
          ],
        },
        defaultValue: '微信',
      },
      { field: 'remark', label: '备注', component: 'InputTextArea' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: HomeaiPayload) {
    if (!values.userId) {
      createMessage.warning('请选择归属用户');
      return false;
    }
    const amount = Number(values.amount);
    if (!Number.isFinite(amount) || amount <= 0) {
      createMessage.warning('请填写有效金额');
      return false;
    }
    values.amount = Math.round(amount * 100) / 100;
    if (values.remark != null) values.remark = String(values.remark).trim();
    try {
      if (isUpdate.value) {
        await billApi.edit(recordId.value, values);
        createMessage.success('编辑成功');
      } else {
        await billApi.add(values);
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
