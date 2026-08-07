<template>
  <div style="padding: 16px">
    <BasicTable @register="registerTable">
      <template #tableTitle>
        <a-button type="primary" preIcon="ant-design:plus-outlined" @click="handleAdd"> 新增分类</a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'color'">
          <span class="color-dot" :style="{ background: record.color }"></span>
        </template>
        <template v-if="column.key === 'isEnabled'">
          <a-tag :color="record.isEnabled === 1 ? 'green' : 'default'">{{ record.isEnabled === 1 ? '启用' : '停用' }}</a-tag>
        </template>
      </template>
    </BasicTable>
  </div>
  <BasicModal @register="registerModal" :title="isUpdate ? '编辑分类' : '新增分类'" width="480px">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <template #footer>
      <a-button @click="closeModal()">取消</a-button>
      <a-button type="primary" @click="submit">保存</a-button>
    </template>
  </BasicModal>
</template>

<script lang="ts" name="homeai-plan-category" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { planApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage, createConfirm } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '计划分类管理',
    api: (params: any) => planApi.categoryList(params),
    columns: [
      { title: '分类名称', dataIndex: 'name', width: 150 },
      { title: '图标', dataIndex: 'icon', width: 60 },
      { title: '颜色', dataIndex: 'color', key: 'color', width: 60 },
      { title: '排序', dataIndex: 'sortOrder', width: 60 },
      { title: '状态', dataIndex: 'isEnabled', key: 'isEnabled', width: 80 },
    ],
    useSearchForm: false,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 120,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  const isUpdate = ref(false);
  const recordId = ref('');
  const [registerModal, { openModal, closeModal }] = useModal();
  const [registerForm, { setFieldsValue, resetFields, submit }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'name', label: '分类名称', component: 'Input', required: true },
      { field: 'icon', label: '图标(emoji)', component: 'Input', componentProps: { placeholder: '如: 📋' } },
      { field: 'color', label: '颜色', component: 'Input', componentProps: { placeholder: '如: #1890ff' } },
      { field: 'sortOrder', label: '排序', component: 'InputNumber', defaultValue: 0 },
      {
        field: 'isEnabled',
        label: '状态',
        component: 'RadioGroup',
        defaultValue: 1,
        componentProps: {
          options: [
            { label: '启用', value: 1 },
            { label: '停用', value: 0 },
          ],
        },
      },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  function handleAdd() {
    isUpdate.value = false;
    recordId.value = '';
    resetFields();
    setFieldsValue({ sortOrder: 0, isEnabled: 1 });
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

  async function handleDelete(record: any) {
    createConfirm({
      title: '确认删除',
      content: `确定删除分类「${record.name}」吗？`,
      onOk: async () => {
        await planApi.deleteCategory(record.id);
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  async function handleSubmit(values: any) {
    try {
      if (isUpdate.value) {
        await planApi.editCategory({ id: recordId.value, ...values });
        createMessage.success('编辑成功');
      } else {
        await planApi.addCategory(values);
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
</script>

<style scoped>
  .color-dot {
    display: inline-block;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    vertical-align: middle;
  }
</style>
