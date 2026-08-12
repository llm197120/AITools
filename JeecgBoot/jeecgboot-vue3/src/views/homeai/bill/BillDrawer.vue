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
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { billApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');
  const categoryOptions = ref<{ label: string; value: string }[]>([]);

  async function loadCategories() {
    try {
      const res: any = await billApi.categoryList({ pageNo: 1, pageSize: 200 });
      const records = res?.records || res || [];
      categoryOptions.value = (records as any[]).map((c) => ({
        label: c.name || c.categoryName || c.id,
        value: c.id,
      }));
    } catch {
      categoryOptions.value = [];
    }
  }

  const [registerDrawer, { closeDrawer }] = useDrawerInner(async (data) => {
    await loadCategories();
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

  async function handleSubmit(values: any) {
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
