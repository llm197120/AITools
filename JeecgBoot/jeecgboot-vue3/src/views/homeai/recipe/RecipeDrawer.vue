<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑菜谱' : '新增菜谱'" width="40%">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <!-- 底部按钮 -->
    <template #footer>
      <a-button type="primary" @click="submit">保存</a-button>
      <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-recipe-drawer" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { recipeApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');

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
      { field: 'name', label: '菜名', component: 'Input', required: true },
      { field: 'categoryId', label: '分类', component: 'Input', required: true },
      {
        field: 'difficulty',
        label: '难度',
        component: 'Select',
        componentProps: {
          options: [
            { label: '简单', value: 1 },
            { label: '中等', value: 2 },
            { label: '困难', value: 3 },
          ],
        },
        defaultValue: 1,
      },
      { field: 'cookTime', label: '烹饪时间(分)', component: 'InputNumber' },
      { field: 'servings', label: '份数', component: 'InputNumber', defaultValue: 1 },
      { field: 'tips', label: '小贴士', component: 'InputTextArea' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: any) {
    try {
      if (isUpdate.value) {
        await recipeApi.edit(recordId.value, values);
        createMessage.success('编辑成功');
      } else {
        await recipeApi.add(values);
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
