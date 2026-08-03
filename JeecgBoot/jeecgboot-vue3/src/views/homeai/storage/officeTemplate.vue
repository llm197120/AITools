<template>
  <div style="padding: 16px">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd"> 新增模板</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'isDefault'">
          <a-tag v-if="record.isDefault === '1'" color="blue">默认</a-tag>
          <a-button v-else size="small" type="link" @click="setDefault(record)">设为默认</a-button>
        </template>
      </template>
    </BasicTable>
  </div>
  <BasicModal @register="registerModal" :title="isUpdate ? '编辑模板' : '新增模板'" width="480px">
    <BasicForm @register="registerForm" @submit="handleSubmit">
      <template #fileUrlSlot="{ model, field }">
        <div style="display: flex; gap: 8px">
          <a-input v-model:value="model[field]" placeholder="模板文件地址" style="flex: 1" />
          <input ref="fileInputRef" type="file" style="display: none" @change="onFileChange" />
          <a-button :disabled="!recordId" @click="triggerUpload">上传</a-button>
        </div>
      </template>
    </BasicForm>
    <template #footer>
      <a-button @click="closeModal()">取消</a-button>
      <a-button type="primary" @click="submit">保存</a-button>
    </template>
  </BasicModal>
</template>

<script lang="ts" name="homeai-storage-template" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage, createConfirm } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '文档模板管理',
    api: (params: any) => defHttp.get({ url: '/homeai/storage/template/list', params }),
    columns: [
      { title: '模板名称', dataIndex: 'name', width: 200 },
      { title: '类型', dataIndex: 'type', width: 80 },
      { title: '默认', dataIndex: 'isDefault', key: 'isDefault', width: 80 },
      { title: '备注', dataIndex: 'remark', width: 200 },
      { title: '创建时间', dataIndex: 'createTime', width: 160 },
    ],
    useSearchForm: false,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  const isUpdate = ref(false);
  const recordId = ref('');
  const fileInputRef = ref<HTMLInputElement | null>(null);
  const selectedFile = ref<File | null>(null);
  const [registerModal, { openModal, closeModal }] = useModal();
  const [registerForm, { setFieldsValue, resetFields, validate, submit }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'name', label: '模板名称', component: 'Input', required: true },
      { field: 'type', label: '类型', component: 'Input', componentProps: { placeholder: '如: docx, xlsx' } },
      { field: 'fileUrl', label: '文件地址', component: 'Input', slot: 'fileUrlSlot' },
      { field: 'remark', label: '备注', component: 'InputTextArea' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  function handleAdd() {
    isUpdate.value = false;
    recordId.value = '';
    resetFields();
    openModal(true);
  }

  function getTableAction(record: any) {
    return [
      { icon: 'ant-design:edit-outlined', onClick: () => handleEdit(record), title: '编辑' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleDelete(record), title: '删除', color: 'error' },
    ];
  }

  function handleEdit(record: any) {
    isUpdate.value = true;
    recordId.value = record.id;
    setFieldsValue({ ...record });
    openModal(true);
  }

  function triggerUpload() {
    if (!recordId.value) {
      createMessage.warning('请先保存模板，再上传文件');
      return;
    }
    fileInputRef.value?.click();
  }

  function onFileChange(e: Event) {
    const target = e.target as HTMLInputElement;
    if (target.files && target.files[0]) {
      selectedFile.value = target.files[0];
      uploadFile();
    }
    target.value = '';
  }

  async function uploadFile() {
    if (!selectedFile.value || !recordId.value) return;
    try {
      const formData = new FormData();
      formData.append('file', selectedFile.value);
      const url: any = await defHttp.uploadFile(
        { url: `/homeai/storage/template/${recordId.value}/upload`, data: formData },
      );
      setFieldsValue({ fileUrl: url });
      createMessage.success('文件上传成功');
    } catch (e: any) {
      createMessage.error(e?.message || '上传失败');
    }
  }

  async function handleDelete(record: any) {
    createConfirm({
      title: '确认删除',
      content: `确定删除模板「${record.name}」吗？`,
      onOk: async () => {
        await defHttp.delete({ url: `/homeai/storage/template/${record.id}` });
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  async function handleSubmit(values: any) {
    try {
      if (isUpdate.value) {
        await defHttp.put({ url: '/homeai/storage/template', data: { id: recordId.value, ...values } });
        createMessage.success('编辑成功');
      } else {
        await defHttp.post({ url: '/homeai/storage/template', data: values });
        createMessage.success('新增成功');
      }
      closeModal();
      reload();
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '操作失败');
      return false;
    }
  }

  async function setDefault(record: any) {
    await defHttp.put({ url: `/homeai/storage/template/${record.id}/default` });
    reload();
  }
</script>
