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
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === 'expense' ? 'red' : 'green'">{{ record.type === 'expense' ? '支出' : '收入' }}</a-tag>
        </template>
        <template v-if="column.key === 'color'">
          <span class="color-dot" :style="{ background: record.color }"></span>
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

<script lang="ts" name="homeai-bill-category" setup>
  import { PageWrapper } from '/@/components/Page';
  import { BasicTable, TableAction } from '/@/components/Table';
  import { BasicModal } from '/@/components/Modal';
  import { BasicForm } from '/@/components/Form';
  import { billApi } from '/@/api/homeai';
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
    title: '消费分类管理',
    columns: [
      { title: '分类名称', dataIndex: 'name', width: 150 },
      { title: '图标', dataIndex: 'icon', width: 60 },
      { title: '颜色', dataIndex: 'color', key: 'color', width: 60 },
      { title: '类型', dataIndex: 'type', key: 'type', width: 60 },
      { title: '系统默认', dataIndex: 'isDefault', width: 70 },
      { title: '排序', dataIndex: 'sortOrder', width: 60 },
    ],
    formSchemas: [
      { field: 'name', label: '分类名称', component: 'Input', required: true },
      {
        field: 'type',
        label: '类型',
        component: 'RadioGroup',
        required: true,
        componentProps: {
          options: [
            { label: '支出', value: 'expense' },
            { label: '收入', value: 'income' },
          ],
        },
      },
      { field: 'icon', label: '图标(emoji)', component: 'Input', componentProps: { placeholder: '如: 🍚' } },
      { field: 'color', label: '颜色', component: 'Input', componentProps: { placeholder: '如: #faad14' } },
      { field: 'sortOrder', label: '排序', component: 'InputNumber', defaultValue: 0 },
    ],
    api: {
      list: (params) => billApi.categoryList(params),
      add: (data) => billApi.addCategory(data),
      edit: (data) => billApi.editCategory(data),
      delete: (id) => billApi.deleteCategory(id),
    },
    defaultFormValues: { type: 'expense', sortOrder: 0 },
    deleteConfirmContent: '确定删除分类「{name}」吗？',
  });
</script>

<style scoped>
  .color-dot {
    display: inline-block;
    width: 16px;
    height: 16px;
    border-radius: 50%;
  }
</style>
