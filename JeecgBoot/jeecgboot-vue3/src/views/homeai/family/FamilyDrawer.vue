<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="drawerTitle" width="40%">
    <!-- 查看模式 -->
    <template v-if="!isUpdate">
      <Description :column="1" :data="record" :schema="viewSchema" />
    </template>
    <!-- 编辑/新增模式 -->
    <BasicForm v-else @register="registerForm" @submit="handleSubmit" />
    <!-- 底部按钮 -->
    <template #footer>
      <template v-if="isUpdate">
        <a-button type="primary" @click="submit">保存</a-button>
        <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
      </template>
      <a-button v-else @click="closeDrawer()">关闭</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-family-drawer" setup>
  import { computed, ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { Description } from '/@/components/Description';
  import { BasicForm, useForm } from '/@/components/Form';
  import { familyApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const record = ref<Recordable>({});
  const isUpdate = ref(false);

  const drawerTitle = computed(() => {
    if (!isUpdate.value) return '家庭详情';
    return record.value.id ? '编辑家庭' : '新增家庭';
  });

  const [registerDrawer, { closeDrawer }] = useDrawerInner((data) => {
    isUpdate.value = data.isUpdate || false;
    record.value = data.record || {};
    if (isUpdate.value && data.record?.id) {
      setFieldsValue({ ...data.record });
    } else if (isUpdate.value) {
      resetFields();
    }
  });

  const viewSchema: any[] = [
    { label: '家庭名称', field: 'name' },
    { label: '创建者ID', field: 'creatorId' },
    { label: '成员数量', field: 'memberCount' },
    {
      label: '状态',
      field: 'delFlag',
      render: (val: number) => (val === 1 ? '已解散' : '正常'),
    },
    { label: '创建时间', field: 'createTime' },
    { label: '更新时间', field: 'updateTime' },
  ];

  const [registerForm, { setFieldsValue, resetFields, submit }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'name', label: '家庭名称', component: 'Input', required: true },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: any) {
    try {
      if (record.value.id) {
        await familyApi.edit(record.value.id, values);
        createMessage.success('编辑成功');
      } else {
        await familyApi.add(values);
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
