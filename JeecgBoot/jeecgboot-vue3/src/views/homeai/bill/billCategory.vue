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
        <template v-if="column.key === 'type'">
          <a-tag :color="record.type === 'expense' ? 'red' : 'green'">{{ record.type === 'expense' ? '支出' : '收入' }}</a-tag>
        </template>
        <template v-if="column.key === 'color'">
          <span class="color-dot" :style="{background: record.color}"></span>
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
<script lang="ts" name="homeai-bill-category" setup>
  import { ref } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { BasicForm, useForm } from '/@/components/Form';
  import { defHttp } from '/@/utils/http/axios';
  import { useMessage } from '/@/hooks/web/useMessage';

  const { createMessage, createConfirm } = useMessage();
  const [registerTable, { reload }] = useTable({
    title: '消费分类管理',
    api: (params: any) => defHttp.get({ url: '/homeai/bill/category-list', params }),
    columns: [
      { title: '分类名称', dataIndex: 'name', width: 150 },
      { title: '图标', dataIndex: 'icon', width: 60 },
      { title: '颜色', dataIndex: 'color', key: 'color', width: 60 },
      { title: '类型', dataIndex: 'type', key: 'type', width: 60 },
      { title: '系统默认', dataIndex: 'isDefault', width: 70 },
      { title: '排序', dataIndex: 'sortOrder', width: 60 },
    ],
    useSearchForm: false, showTableSetting: true, showIndexColumn: true,
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

  const [registerForm, { setFieldsValue, resetFields, validate, submit }] = useForm({
    labelWidth: 100,
    schemas: [
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
    showSubmitButton: false,
    showResetButton: false,
  });

  function handleAdd() {
    isUpdate.value = false;
    recordId.value = '';
    resetFields();
    setFieldsValue({ type: 'expense', sortOrder: 0 });
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
        await defHttp.delete({ url: `/homeai/bill/category/${record.id}` });
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  async function handleSubmit(values: any) {
    try {
      if (isUpdate.value) {
        await defHttp.put({ url: '/homeai/bill/category', data: { id: recordId.value, ...values } });
        createMessage.success('编辑成功');
      } else {
        await defHttp.post({ url: '/homeai/bill/category', data: values });
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
<style>.color-dot{display:inline-block;width:16px;height:16px;border-radius:50%}</style>
