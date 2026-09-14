<template>
  <HomeaiFormModal v-bind="$attrs" size="medium" @register="registerDrawer" :title="title">
    <template v-if="isViewMode">
      <Description :column="2" :data="record" :schema="viewSchema" />
    </template>
    <template v-else>
      <BasicForm @register="registerForm" @submit="handleSubmit">
        <template #providerSlot="{ model, field }">
          <a-select v-model:value="model[field]" placeholder="请选择提供商">
            <a-select-option value="DeepSeek">DeepSeek</a-select-option>
            <a-select-option value="Qwen">通义千问(Qwen)</a-select-option>
            <a-select-option value="OpenAI">OpenAI</a-select-option>
            <a-select-option value="Anthropic">Anthropic</a-select-option>
            <a-select-option value="Ollama">Ollama(本地)</a-select-option>
            <a-select-option value="custom">自定义</a-select-option>
          </a-select>
        </template>
      </BasicForm>
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

<script lang="ts" name="homeai-key-drawer" setup>
  import { ref, computed } from 'vue';
  import { useModalInner } from '/@/components/Modal';
  import { Description } from '/@/components/Description';
  import { BasicForm, useForm } from '/@/components/Form';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { keyConfigApi } from '/@/api/homeai';
  import HomeaiFormModal from '../components/HomeaiFormModal.vue';
  import { COL_FULL } from '../utils/formLayout';

  const emit = defineEmits(['success']);
  const { createMessage } = useMessage();

  const isUpdate = ref(false);
  const record = ref<Recordable>({});
  const isViewMode = ref(false);

  const title = computed(() => {
    if (isViewMode.value) return '密钥详情';
    return isUpdate.value ? '编辑密钥' : '新增密钥';
  });

  const viewSchema: any[] = [
    { label: '提供商', field: 'provider' },
    { label: '模型名', field: 'modelName' },
    { label: 'API Key', field: 'apiKeyEncrypted' },
    { label: 'API地址', field: 'apiBaseUrl' },
    { label: '备注', field: 'remark' },
    { label: '排序号', field: 'sortOrder' },
    {
      label: '状态',
      field: 'isEnabled',
      render: (val: string) => (val === '1' ? '启用' : '停用'),
    },
    {
      label: '默认模型',
      field: 'isDefault',
      render: (val: string) => (val === '1' ? '是' : '否'),
    },
  ];

  const [registerDrawer, { closeModal: closeDrawer }] = useModalInner((data) => {
    isUpdate.value = data?.isUpdate;
    record.value = data?.record || {};
    // 查看模式：isUpdate=false 但有 record 数据
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
        field: 'provider',
        label: '提供商',
        component: 'Select',
        required: true,
        slot: 'providerSlot',
      },
      { field: 'modelName', label: '模型名', component: 'Input', required: true },
      {
        field: 'apiKeyRaw',
        label: 'API Key',
        component: 'InputPassword',
        required: false,
        helpMessage: '编辑时留空表示不修改密钥',
      },
      { field: 'apiBaseUrl', label: 'API地址', component: 'Input', colProps: COL_FULL },
      { field: 'remark', label: '备注', component: 'InputTextArea', colProps: COL_FULL },
      { field: 'sortOrder', label: '排序号', component: 'InputNumber', defaultValue: 0 },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: any) {
    try {
      if (isUpdate.value) {
        await keyConfigApi.edit({ id: record.value.id, ...values });
        createMessage.success('编辑成功');
      } else {
        await keyConfigApi.add(values);
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
