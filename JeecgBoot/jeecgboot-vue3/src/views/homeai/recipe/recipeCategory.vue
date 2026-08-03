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
        <template v-if="column.key === 'isDefault'">
          <a-tag v-if="record.isDefault === 1" color="blue">系统默认</a-tag>
        </template>
      </template>
    </BasicTable>
    <BasicModal @register="registerModal" :title="isUpdate ? '编辑分类' : '新增分类'" width="420px">
      <BasicForm @register="registerForm" @submit="handleSubmit" />
      <template #footer>
        <a-button @click="closeModal()">取消</a-button>
        <a-button type="primary" @click="submit">保存</a-button>
      </template>
    </BasicModal>
  </div>
</template>

<script lang="ts" name="homeai-recipe-category" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage, createConfirm } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '菜谱分类管理',
    api: (params: any) => defHttp.get({ url: '/homeai/recipe/category/list', params }),
    columns: [
      { title: '分类名称', dataIndex: 'name', width: 200 },
      { title: '排序', dataIndex: 'sortOrder', width: 100 },
      { title: '类型', dataIndex: 'isDefault', key: 'isDefault', width: 120 },
      { title: '创建时间', dataIndex: 'createTime', width: 180 },
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
      { field: 'sortOrder', label: '排序', component: 'InputNumber', defaultValue: 0 },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  function handleAdd() {
    isUpdate.value = false;
    recordId.value = '';
    resetFields();
    setFieldsValue({ sortOrder: 0 });
    openModal(true);
  }

  function handleEdit(record: any) {
    isUpdate.value = true;
    recordId.value = record.id;
    setFieldsValue({ name: record.name, sortOrder: record.sortOrder });
    openModal(true);
  }

  function getTableAction(record: any) {
    return [
      { icon: 'ant-design:edit-outlined', onClick: () => handleEdit(record), title: '编辑' },
      { icon: 'ant-design:delete-outlined', onClick: () => handleDelete(record), title: '删除', color: 'error' },
    ];
  }

  function handleDelete(record: any) {
    createConfirm({
      title: '确认删除',
      content: `确定删除分类「${record.name}」吗？`,
      onOk: async () => {
        await defHttp.delete({ url: `/homeai/recipe/category/${record.id}` });
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  async function handleSubmit(values: any) {
    try {
      if (isUpdate.value) {
        await defHttp.put({ url: '/homeai/recipe/category', data: { id: recordId.value, ...values } });
        createMessage.success('编辑成功');
      } else {
        await defHttp.post({ url: '/homeai/recipe/category', data: values });
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
