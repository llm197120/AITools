<template>
  <HomeaiFormModal v-bind="$attrs" size="short" @register="registerDrawer" :title="title">
    <template v-if="isViewMode">
      <Description :column="1" :data="record" :schema="viewSchema" />
    </template>
    <template v-else>
      <BasicForm @register="registerForm" @submit="handleSubmit" />
    </template>
    <template #footer>
      <template v-if="!isViewMode">
        <a-button @click="closeDrawer()">取消</a-button>
        <a-button type="primary" @click="submit">保存并关闭</a-button>
      </template>
      <a-button v-else @click="closeDrawer()">关闭</a-button>
    </template>
  </HomeaiFormModal>
</template>

<script lang="ts" name="convert-rule-drawer" setup>
  import { ref, computed } from 'vue';
  import { useModalInner } from '/@/components/Modal';
  import { Description } from '/@/components/Description';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { storageRuleApi } from '/@/api/homeai';
  import type { HomeaiPayload } from '/@/api/homeai';
  import HomeaiFormModal from '../components/HomeaiFormModal.vue';

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

  const [registerDrawer, { closeModal: closeDrawer }] = useModalInner((data) => {
    isUpdate.value = data?.isUpdate;
    record.value = data?.record || {};
    isViewMode.value = !data?.isUpdate && data?.record?.id;
    if (!isViewMode.value) {
      setFieldsValue(data?.record || {});
    }
  });

  const [registerForm, { setFieldsValue, submit }] = useForm({
    labelWidth: 100,
    baseColProps: { span: 12 },
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
        colProps: { span: 24 },
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
