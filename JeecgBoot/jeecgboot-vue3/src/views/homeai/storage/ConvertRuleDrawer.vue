<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="title" width="40%">
    <!-- 查看模式 -->
    <template v-if="isViewMode">
      <Description :column="1" :data="record" :schema="viewSchema" />
    </template>
    <!-- 编辑/新增模式 -->
    <template v-else>
      <BasicForm @register="registerForm" @submit="handleSubmit" />
    </template>
    <!-- 底部按钮 -->
    <template #footer>
      <template v-if="!isViewMode">
        <a-button type="primary" @click="submit">保存</a-button>
        <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
      </template>
      <a-button v-else @click="closeDrawer()">关闭</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="convert-rule-drawer" setup>
  import { ref, computed } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { Description } from '/@/components/Description';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { storageRuleApi } from '/@/api/homeai';
  import type { HomeaiPayload } from '/@/api/homeai';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();
  const isUpdate = ref(false);
  const isViewMode = ref(false);
  const record = ref<Recordable>({});

  const title = computed(() => {
    if (isViewMode.value) return '规则详情';
    return isUpdate.value ? '编辑规则' : '新增规则';
  });

  const viewSchema: any[] = [
    { label: '源格式', field: 'sourceFormat' },
    { label: '目标格式', field: 'targetFormat' },
    {
      label: '状态',
      field: 'isEnabled',
      render: (val: string) => (val === '1' ? '启用' : '停用'),
    },
    { label: '创建时间', field: 'createTime' },
    { label: '更新时间', field: 'updateTime' },
  ];

  const [registerDrawer, { closeDrawer }] = useDrawerInner((data) => {
    isUpdate.value = data?.isUpdate;
    record.value = data?.record || {};
    isViewMode.value = !data?.isUpdate && data?.record?.id;
    if (!isViewMode.value) {
      setFieldsValue(data?.record || {});
    }
  });

  const [registerForm, { setFieldsValue, submit }] = useForm({
    labelWidth: 100,
    schemas: [
      {
        field: 'sourceFormat',
        label: '源格式',
        component: 'Input',
        required: true,
        componentProps: { placeholder: '如: docx, xlsx, pdf' },
      },
      {
        field: 'targetFormat',
        label: '目标格式',
        component: 'Input',
        required: true,
        componentProps: { placeholder: '如: pdf, csv, docx' },
      },
      {
        field: 'isEnabled',
        label: '状态',
        component: 'RadioGroup',
        componentProps: {
          options: [
            { label: '启用', value: '1' },
            { label: '停用', value: '0' },
          ],
        },
        defaultValue: '1',
      },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: HomeaiPayload) {
    try {
      if (isUpdate.value) {
        await storageRuleApi.update({ id: record.value.id, ...values });
        createMessage.success('编辑成功');
      } else {
        await storageRuleApi.create(values);
        createMessage.success('新增成功');
      }
      closeDrawer();
      emit('success');
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '保存失败');
      return false;
    }
  }
</script>
