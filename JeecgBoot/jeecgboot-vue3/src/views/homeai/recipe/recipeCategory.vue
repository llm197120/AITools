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
  <HomeaiFormModal @register="registerModal" size="short" :title="isUpdate ? '编辑分类' : '新增分类'">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <template #footer>
      <a-button @click="closeModal()">取消</a-button>
      <a-button type="primary" @click="submit">保存并关闭</a-button>
    </template>
  </HomeaiFormModal>
</template>

<script lang="ts" name="homeai-recipe-category" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { BasicForm } from '/@/components/Form';
  import HomeaiFormModal from '../components/HomeaiFormModal.vue';
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
      { field: 'name', label: '分类名称', component: 'Input', required: true, colProps: { span: 24 } },
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
