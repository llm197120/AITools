<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="isUpdate ? '编辑学习资料' : '新增学习资料'" width="40%">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <!-- 底部按钮 -->
    <template #footer>
      <a-button type="primary" @click="submit">保存</a-button>
      <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-learn-drawer" setup>
  import { ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicForm, useForm } from '/@/components/Form';
  import { learnApi } from '/@/api/homeai';
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
      { field: 'category', label: '分类', component: 'Input' },
      { field: 'tags', label: '标签', component: 'Input', help: '多个标签用逗号分隔' },
      { field: 'fileUrl', label: '文件URL', component: 'Input' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: any) {
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
    }
  }
</script>
