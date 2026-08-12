/**
 * useHomeaiCrud — HomeAI 分类/简单 CRUD 列表页通用封装
 *
 * ## 用法示例（计划分类）
 *
 * ```ts
 * const {
 *   registerTable,
 *   registerModal,
 *   registerForm,
 *   isUpdate,
 *   handleAdd,
 *   handleEdit,
 *   handleDelete,
 *   handleSubmit,
 *   getTableAction,
 *   closeModal,
 *   submit,
 * } = useHomeaiCrud({
 *   title: '计划分类管理',
 *   columns: [...],
 *   formSchemas: [...],
 *   api: {
 *     list: (params) => planApi.categoryList(params),
 *     add: (data) => planApi.addCategory(data),
 *     edit: (data) => planApi.editCategory(data),
 *     delete: (id) => planApi.deleteCategory(id),
 *   },
 *   defaultFormValues: { sortOrder: 0, isEnabled: 1 },
 * });
 * ```
 *
 * 页面模板仍负责 `#bodyCell` 自定义渲染与 Modal 结构，hook 只收敛表格/表单/增删改逻辑。
 */
import { ref } from 'vue';
import { BasicColumn, FormSchema, useTable } from '/@/components/Table';
import { useModal } from '/@/components/Modal';
import { useForm } from '/@/components/Form';
import { useMessage } from '/@/hooks/web/useMessage';
import type { HomeaiPageParams, HomeaiPayload } from '/@/api/homeai/types';

/** 行记录（宽松） */
export type HomeaiCrudRecord = Record<string, unknown> & { id?: string; name?: string };

/** CRUD API 约定 */
export interface HomeaiCrudApi {
  /** 分页/列表 */
  list: (params?: HomeaiPageParams) => Promise<unknown>;
  /** 新增 */
  add?: (data: HomeaiPayload) => Promise<unknown>;
  /** 编辑（通常带 id） */
  edit?: (data: HomeaiPayload) => Promise<unknown>;
  /** 删除 */
  delete?: (id: string) => Promise<unknown>;
}

export interface UseHomeaiCrudOptions {
  /** 表格标题 */
  title: string;
  /** 表格列 */
  columns: BasicColumn[];
  /** 表单 schemas */
  formSchemas: FormSchema[];
  /** 列表/增删改 API */
  api: HomeaiCrudApi;
  /** 是否使用搜索表单，默认 false（分类页常见） */
  useSearchForm?: boolean;
  /** 操作列宽度，默认 120 */
  actionColumnWidth?: number;
  /** 表单 label 宽度，默认 100 */
  labelWidth?: number;
  /** 新增时的默认表单值 */
  defaultFormValues?: HomeaiPayload;
  /** 删除确认文案用的名称字段，默认 name */
  nameField?: string;
  /** 删除确认标题，默认「确认删除」 */
  deleteConfirmTitle?: string;
  /** 删除确认内容模板，可用 {name} 占位 */
  deleteConfirmContent?: string;
}

/**
 * 封装 HomeAI 常见「表格 + 弹窗表单」CRUD 模式
 */
export function useHomeaiCrud(options: UseHomeaiCrudOptions) {
  const {
    title,
    columns,
    formSchemas,
    api,
    useSearchForm = false,
    actionColumnWidth = 120,
    labelWidth = 100,
    defaultFormValues = {},
    nameField = 'name',
    deleteConfirmTitle = '确认删除',
    deleteConfirmContent = '确定删除「{name}」吗？',
  } = options;

  const { createMessage, createConfirm } = useMessage();
  const isUpdate = ref(false);
  const recordId = ref('');

  const [registerTable, { reload }] = useTable({
    title,
    api: (params: HomeaiPageParams) => api.list(params),
    columns,
    useSearchForm,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: actionColumnWidth,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
  });

  const [registerModal, { openModal, closeModal }] = useModal();
  const [registerForm, { setFieldsValue, resetFields, submit }] = useForm({
    labelWidth,
    schemas: formSchemas,
    showSubmitButton: false,
    showResetButton: false,
  });

  /** 打开新增弹窗 */
  function handleAdd() {
    isUpdate.value = false;
    recordId.value = '';
    resetFields();
    setFieldsValue({ ...defaultFormValues });
    openModal(true);
  }

  /** 打开编辑弹窗 */
  function handleEdit(record: HomeaiCrudRecord) {
    isUpdate.value = true;
    recordId.value = String(record.id ?? '');
    setFieldsValue({ ...record });
    openModal(true);
  }

  /** 删除（带确认） */
  function handleDelete(record: HomeaiCrudRecord) {
    if (!api.delete) {
      createMessage.warning('未配置删除接口');
      return;
    }
    const name = String(record[nameField] ?? record.name ?? '');
    createConfirm({
      title: deleteConfirmTitle,
      content: deleteConfirmContent.replace('{name}', name),
      onOk: async () => {
        await api.delete!(String(record.id));
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  /** 表格行操作按钮 */
  function getTableAction(record: HomeaiCrudRecord) {
    return [
      { icon: 'ant-design:edit-outlined', onClick: () => handleEdit(record), title: '编辑' },
      {
        icon: 'ant-design:delete-outlined',
        onClick: () => handleDelete(record),
        title: '删除',
        color: 'error' as const,
      },
    ];
  }

  /** 表单提交（新增 / 编辑） */
  async function handleSubmit(values: HomeaiPayload) {
    try {
      if (isUpdate.value) {
        if (!api.edit) {
          createMessage.warning('未配置编辑接口');
          return false;
        }
        await api.edit({ id: recordId.value, ...values });
        createMessage.success('编辑成功');
      } else {
        if (!api.add) {
          createMessage.warning('未配置新增接口');
          return false;
        }
        await api.add(values);
        createMessage.success('新增成功');
      }
      closeModal();
      reload();
      return true;
    } catch (e: unknown) {
      const err = e as { message?: string };
      createMessage.error(err?.message || '操作失败');
      return false;
    }
  }

  return {
    registerTable,
    registerModal,
    registerForm,
    isUpdate,
    recordId,
    reload,
    handleAdd,
    handleEdit,
    handleDelete,
    handleSubmit,
    getTableAction,
    openModal,
    closeModal,
    submit,
    setFieldsValue,
    resetFields,
  };
}
