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
        <template v-if="column.key === 'color'">
          <span class="color-dot" :style="{ background: record.color }"></span>
        </template>
        <template v-if="column.key === 'isEnabled'">
          <a-tag :color="record.isEnabled === 1 ? 'green' : 'default'">{{ record.isEnabled === 1 ? '启用' : '停用' }}</a-tag>
        </template>
      </template>
    </BasicTable>
  </PageWrapper>
  <BasicModal @register="registerModal" :title="isUpdate ? '编辑分类' : '新增分类'" width="480px">
    <BasicForm @register="registerForm" @submit="handleSubmit" />
    <template #footer>
      <a-button @click="closeModal()">取消</a-button>
      <a-button type="primary" @click="submit">保存</a-button>
    </template>
  </BasicModal>
</template>

<script lang="ts" name="homeai-plan-category" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { BasicModal } from '/@/components/Modal';
  import { BasicForm } from '/@/components/Form';
  import { planApi } from '/@/api/homeai';
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
    title: '计划分类管理',
    columns: [
      { title: '分类名称', dataIndex: 'name', width: 150 },
      { title: '图标', dataIndex: 'icon', width: 60 },
      { title: '颜色', dataIndex: 'color', key: 'color', width: 60 },
      { title: '排序', dataIndex: 'sortOrder', width: 60 },
      { title: '状态', dataIndex: 'isEnabled', key: 'isEnabled', width: 80 },
    ],
    formSchemas: [
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
    api: {
      list: (params) => planApi.categoryList(params),
      add: (data) => planApi.addCategory(data),
      edit: (data) => planApi.editCategory(data),
      delete: (id) => planApi.deleteCategory(id),
    },
    defaultFormValues: { sortOrder: 0, isEnabled: 1 },
    deleteConfirmContent: '确定删除分类「{name}」吗？',
  });
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
