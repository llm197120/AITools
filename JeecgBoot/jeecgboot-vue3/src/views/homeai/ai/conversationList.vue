<template>
  <PageWrapper contentFullHeight dense contentClass="!p-4 homeai-page-body">
    <BasicTable @register="registerTable" :rowSelection="rowSelection">
      <template #tableTitle>
        <a-button v-if="selectedRowKeys.length > 0" preIcon="ant-design:delete-outlined" type="primary" danger @click="handleBatchDelete">
          批量删除({{ selectedRowKeys.length }})
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction :actions="getTableAction(record)" />
      </template>
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'modelName'">
          <a-tag color="blue">{{ record.modelName || '默认' }}</a-tag>
        </template>
        <template v-if="column.key === 'messageCount'">
          <a-badge :count="record.messageCount" :number-style="{ backgroundColor: '#52c41a' }" />
        </template>
        <template v-else-if="column.key === 'userId' || column.dataIndex === 'userId'">
          {{ resolveUserLabel(record.userId) }}
        </template>
      </template>
    </BasicTable>

    <!-- 消息详情弹窗 -->
    <BasicModal @register="registerMsgModal" title="对话消息" width="700px" :footer="null">
      <a-spin :spinning="msgLoading">
        <div class="message-list" v-if="messages.length > 0">
          <div v-for="msg in messages" :key="msg.id" :class="['msg-item', msg.role === 'user' ? 'msg-user' : 'msg-ai']">
            <div class="msg-role">{{ msg.role === 'user' ? '用户' : 'AI' }}</div>
            <div class="msg-content">{{ msg.content }}</div>
            <div class="msg-meta">{{ msg.createTime }}</div>
          </div>
        </div>
        <a-empty v-else description="暂无消息" />
      </a-spin>
    </BasicModal>
  </PageWrapper>
</template>

<script lang="ts" name="homeai-ai-conversations" setup>
  import { PageWrapper } from '/@/components/Page';
  import { computed, ref, onMounted } from 'vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { BasicModal, useModal } from '/@/components/Modal';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { conversationApi } from '/@/api/homeai';
  import { useUserLabel } from '../hooks/useUserLabel';

  const { createMessage, createConfirm } = useMessage();
  const { loadUserOptions, resolveUserLabel } = useUserLabel();
  const [registerMsgModal, { openModal: openMsgModal }] = useModal();
  const selectedRowKeys = ref<string[]>([]);
  const messages = ref<any[]>([]);
  const msgLoading = ref(false);

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const [registerTable, { reload }] = useTable({
    title: 'AI对话管理',
    api: (params: any) => conversationApi.list(params),
    columns: [
      { title: '对话标题', dataIndex: 'title', width: 250 },
      { title: '用户', dataIndex: 'userId', key: 'userId', width: 160 },
      { title: '模型', dataIndex: 'modelName', key: 'modelName', width: 120 },
      { title: '消息数', dataIndex: 'messageCount', key: 'messageCount', width: 80 },
      { title: '创建时间', dataIndex: 'createTime', width: 180 },
      { title: '更新时间', dataIndex: 'updateTime', width: 180 },
    ],
    useSearchForm: true,
    showTableSetting: true,
    showIndexColumn: true,
    actionColumn: {
      width: 200,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
    },
    formConfig: {
      schemas: [
        { field: 'title', label: '标题', component: 'Input' },
        { field: 'userId', label: '用户ID', component: 'Input' },
      ],
    },
  });

  onMounted(() => {
    loadUserOptions();
  });

  function getTableAction(record: any) {
    return [
      {
        icon: 'ant-design:message-outlined',
        onClick: () => handleViewMessages(record),
        title: '查看消息',
      },
      {
        icon: 'ant-design:delete-outlined',
        onClick: () => handleDelete(record),
        title: '删除',
        color: 'error',
      },
    ];
  }

  async function handleViewMessages(record: any) {
    msgLoading.value = true;
    messages.value = [];
    openMsgModal(true);
    try {
      messages.value = (await conversationApi.getMessages(record.id)) as any[];
    } catch {
      createMessage.error('加载消息失败');
    } finally {
      msgLoading.value = false;
    }
  }

  async function handleDelete(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除对话「${record.title}」吗？`,
      onOk: async () => {
        await conversationApi.delete(record.id);
        createMessage.success('删除成功');
        reload();
      },
    });
  }

  async function handleBatchDelete() {
    createConfirm({
      iconType: 'warning',
      title: '确认删除',
      content: `确定删除选中的 ${selectedRowKeys.value.length} 个对话吗？`,
      onOk: async () => {
        for (const id of selectedRowKeys.value) {
          await conversationApi.delete(id);
        }
        createMessage.success('删除成功');
        selectedRowKeys.value = [];
        reload();
      },
    });
  }
</script>

<style scoped lang="less">
  .message-list {
    max-height: 500px;
    overflow-y: auto;
    padding: 8px;
  }
  .msg-item {
    padding: 12px 16px;
    border-radius: 8px;
    margin-bottom: 12px;
    &.msg-user {
      background: #e6f7ff;
      border-left: 3px solid #1890ff;
    }
    &.msg-ai {
      background: #f6ffed;
      border-left: 3px solid #52c41a;
    }
    .msg-role {
      font-size: 12px;
      color: #999;
      margin-bottom: 4px;
    }
    .msg-content {
      font-size: 14px;
      color: #333;
      white-space: pre-wrap;
      word-break: break-all;
    }
    .msg-meta {
      font-size: 11px;
      color: #bbb;
      margin-top: 6px;
    }
  }
</style>
