/**
 * useHomeaiRecycleBin — HomeAI 主列表页「回收站」操作通用封装
 *
 * 收敛 6 个主列表页重复的：移入回收站 / 恢复 / 彻底删除（单条 + 批量）+ rowSelection。
 *
 * ## 用法示例
 *
 * ```ts
 * const { rowSelection, selectedRowKeys, clearSelection, handleMoveToRecycleBin, ... } =
 *   useHomeaiRecycleBin({
 *     api: {
 *       moveToRecycleBin: (ids) => recipeApi.moveToRecycleBin(ids),
 *       restore: (ids) => recipeApi.restore(ids),
 *       deletePermanently: (ids) => recipeApi.deletePermanently(ids),
 *     },
 *     reload,
 *     entityName: '菜谱',
 *     nameField: 'name',
 *   });
 * ```
 */
import { ref, computed } from 'vue';
import { useMessage } from '/@/hooks/web/useMessage';

export interface HomeaiRecycleBinApi {
  moveToRecycleBin: (ids: string[]) => Promise<unknown>;
  restore: (ids: string[]) => Promise<unknown>;
  deletePermanently: (ids: string[]) => Promise<unknown>;
}

export interface UseHomeaiRecycleBinOptions {
  /** 回收站三个操作接口 */
  api: HomeaiRecycleBinApi;
  /** 刷新列表（操作成功后调用） */
  reload: () => void;
  /** 实体名，用于确认文案（用户/菜谱/家庭...），默认「记录」 */
  entityName?: string;
  /** 确认文案中展示的名称字段，默认 name（用户页为 nickname，计划/学习为 title） */
  nameField?: string;
  /** 彻底删除确认追加的警示文案（如「此操作不可恢复！」） */
  permanentWarn?: string;
  /** 单条确认文案是否包含「名称」（账单等无名称字段的实体设为 false），默认 true */
  confirmWithName?: boolean;
}

export function useHomeaiRecycleBin(options: UseHomeaiRecycleBinOptions) {
  const { api, reload, entityName = '记录', nameField = 'name', permanentWarn = '', confirmWithName = true } = options;
  const { createMessage, createConfirm } = useMessage();

  const selectedRowKeys = ref<string[]>([]);

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  function clearSelection() {
    selectedRowKeys.value = [];
  }

  function recordLabel(record: any): string {
    return String(record?.[nameField] ?? record?.name ?? record?.title ?? '');
  }

  function handleMoveToRecycleBin(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: confirmWithName
        ? `确定将该${entityName}「${recordLabel(record)}」移入回收站吗？`
        : `确定将该${entityName}移入回收站吗？`,
      onOk: async () => {
        await api.moveToRecycleBin([String(record.id)]);
        createMessage.success('已移入回收站');
        clearSelection();
        reload();
      },
    });
  }

  function handleBatchMoveToRecycleBin() {
    createConfirm({
      iconType: 'warning',
      title: '确认移入回收站',
      content: `确定将选中的 ${selectedRowKeys.value.length} 个${entityName}移入回收站吗？`,
      onOk: async () => {
        await api.moveToRecycleBin(selectedRowKeys.value);
        createMessage.success('已移入回收站');
        clearSelection();
        reload();
      },
    });
  }

  async function handleRestore(record: any) {
    await api.restore([String(record.id)]);
    createMessage.success('恢复成功');
    reload();
  }

  async function handleBatchRestore() {
    await api.restore(selectedRowKeys.value);
    createMessage.success('恢复成功');
    clearSelection();
    reload();
  }

  function handleDeletePermanently(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: confirmWithName
        ? `确定彻底删除该${entityName}「${recordLabel(record)}」吗？${permanentWarn}`
        : `确定彻底删除该${entityName}吗？${permanentWarn}`,
      onOk: async () => {
        await api.deletePermanently([String(record.id)]);
        createMessage.success('已彻底删除');
        clearSelection();
        reload();
      },
    });
  }

  function handleBatchDeletePermanently() {
    createConfirm({
      iconType: 'warning',
      title: '确认彻底删除',
      content: `确定彻底删除选中的 ${selectedRowKeys.value.length} 个${entityName}吗？${permanentWarn}`,
      onOk: async () => {
        await api.deletePermanently(selectedRowKeys.value);
        createMessage.success('已彻底删除');
        clearSelection();
        reload();
      },
    });
  }

  return {
    rowSelection,
    selectedRowKeys,
    clearSelection,
    handleMoveToRecycleBin,
    handleBatchMoveToRecycleBin,
    handleRestore,
    handleBatchRestore,
    handleDeletePermanently,
    handleBatchDeletePermanently,
  };
}
