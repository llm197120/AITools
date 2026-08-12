<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
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
  </PageWrapper>
  <BasicModal @register="registerModal" :title="isUpdate ? '编辑分类' : '新增分类'" width="420px">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <template #footer>
      <a-button @click="closeModal()">取消</a-button>
      <a-button type="primary" @click="submit">保存</a-button>
    </template>
  </BasicModal>
</template>

<script lang="ts" name="homeai-recipe-category" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { BasicModal } from '/@/components/Modal';
  import { BasicForm } from '/@/components/Form';
  import { recipeApi } from '/@/api/homeai';
  import { useHomeaiCrud } from '../hooks/useHomeaiCrud';

  const {
    registerTable,
    registerModal,
    registerForm,
    isUpdate,
    handleAdd,
    handleSubmit,
    getTableAction,
    closeModal,
    submit,
  } = useHomeaiCrud({
    title: '菜谱分类管理',
    columns: [
      { title: '分类名称', dataIndex: 'name', width: 200 },
      { title: '排序', dataIndex: 'sortOrder', width: 100 },
      { title: '类型', dataIndex: 'isDefault', key: 'isDefault', width: 120 },
      { title: '创建时间', dataIndex: 'createTime', width: 180 },
    ],
    formSchemas: [
      { field: 'name', label: '分类名称', component: 'Input', required: true },
      { field: 'sortOrder', label: '排序', component: 'InputNumber', defaultValue: 0 },
    ],
    api: {
      list: (params) => recipeApi.categoryList(params),
      add: (data) => recipeApi.addCategory(data),
      edit: (data) => recipeApi.editCategory(data),
      delete: (id) => recipeApi.deleteCategory(id),
    },
    defaultFormValues: { sortOrder: 0 },
    deleteConfirmContent: '确定删除分类「{name}」吗？',
  });
</script>
