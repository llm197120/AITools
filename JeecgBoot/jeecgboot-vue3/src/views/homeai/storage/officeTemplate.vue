<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
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
    <BasicModal @register="registerModal" :title="isUpdate ? '编辑模板' : '新增模板'" width="480px">
      <BasicForm @register="registerForm" @submit="handleSubmit">
        <template #fileUrlSlot>
          <div style="display: flex; gap: 8px; align-items: center">
            <input ref="fileInputRef" type="file" accept=".doc,.docx,.xls,.xlsx,.ppt,.pptx" style="display: none" @change="onFileChange" />
            <a-button type="primary" @click="fileInputRef?.click()">选择模板文件</a-button>
            <span v-if="selectedFileName" style="color: #666; flex: 1; overflow: hidden; text-overflow: ellipsis">{{ selectedFileName }}</span>
            <span v-else style="color: #999">请上传模板文件</span>
          </div>
          <div v-if="uploadedFileUrl" style="margin-top: 8px; font-size: 12px; color: #52c41a">已上传：{{ uploadedFileUrl }}</div>
        </template>
      </BasicForm>
      <template #footer>
        <a-button @click="closeModal()">取消</a-button>
        <a-button type="primary" :loading="saving" @click="submit">保存</a-button>
      </template>
    </BasicModal>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-storage-template" setup>
  import { PageWrapper } from '/@/components/Page';
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
  const saving = ref(false);
  const fileInputRef = ref<HTMLInputElement | null>(null);
  const selectedFile = ref<File | null>(null);
  const selectedFileName = ref('');
  const uploadedFileUrl = ref('');
  const [registerModal, { openModal, closeModal }] = useModal();
  const [registerForm, { setFieldsValue, resetFields, submit }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'name', label: '模板名称', component: 'Input', required: true },
      { field: 'type', label: '类型', component: 'Input', componentProps: { placeholder: '如: docx, xlsx' } },
      { field: 'fileUrl', label: '模板文件', component: 'Input', slot: 'fileUrlSlot' },
      { field: 'remark', label: '备注', component: 'InputTextArea' },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  function resetFileState() {
    selectedFile.value = null;
    selectedFileName.value = '';
    uploadedFileUrl.value = '';
    if (fileInputRef.value) fileInputRef.value.value = '';
  }

  function handleAdd() {
    isUpdate.value = false;
    recordId.value = '';
    resetFileState();
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
    resetFileState();
    uploadedFileUrl.value = record.fileUrl || '';
    selectedFileName.value = record.fileUrl ? '已有模板文件（重新选择可替换）' : '';
    setFieldsValue({ ...record });
    openModal(true);
  }

  function onFileChange(e: Event) {
    const target = e.target as HTMLInputElement;
    if (target.files && target.files[0]) {
      selectedFile.value = target.files[0];
      selectedFileName.value = target.files[0].name;
      uploadedFileUrl.value = '';
    }
  }

  async function uploadSelectedFile(id: string) {
    if (!selectedFile.value) return uploadedFileUrl.value;
    const url: any = await defHttp.uploadFile(
      { url: `/homeai/storage/template/${id}/upload` },
      { file: selectedFile.value, name: 'file' },
    );
    uploadedFileUrl.value = url;
    setFieldsValue({ fileUrl: url });
    return url;
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
    saving.value = true;
    try {
      if (!isUpdate.value && !selectedFile.value && !values.fileUrl) {
        createMessage.warning('请上传模板文件');
        return false;
      }
      if (isUpdate.value) {
        await defHttp.put({ url: '/homeai/storage/template', data: { id: recordId.value, ...values } });
        if (selectedFile.value) {
          await uploadSelectedFile(recordId.value);
        }
        createMessage.success('编辑成功');
      } else if (selectedFile.value) {
        await defHttp.uploadFile(
          { url: '/homeai/storage/template/create-with-file' },
          { file: selectedFile.value, name: 'file', data: { name: values.name, type: values.type || '', remark: values.remark || '' } },
        );
        createMessage.success('新增成功');
      } else {
        const res: any = await defHttp.post({ url: '/homeai/storage/template', data: values });
        const newId = res?.id;
        if (newId && selectedFile.value) {
          await uploadSelectedFile(newId);
        }
        createMessage.success('新增成功');
      }
      closeModal();
      reload();
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '操作失败');
      return false;
    } finally {
      saving.value = false;
    }
  }

  async function setDefault(record: any) {
    await defHttp.put({ url: `/homeai/storage/template/${record.id}/default` });
    reload();
  }
</script>
