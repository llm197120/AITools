<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="drawerTitle" width="55%">
    <template #default>
      <a-space style="margin-bottom: 12px">
        <a-button preIcon="ant-design:user-add-outlined" type="primary" @click="openAddModal">
          添加成员
        </a-button>
      </a-space>
      <BasicTable @register="registerTable" :rowSelection="rowSelection">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'role'">
            <a-select
              :value="record.role"
              style="width: 110px"
              @change="(val) => handleChangeRole(record, val)"
            >
              <a-select-option value="admin">管理员</a-select-option>
              <a-select-option value="member">成员</a-select-option>
              <a-select-option value="restricted">受限</a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" danger @click="handleRemove(record)">移除</a-button>
          </template>
        </template>
      </BasicTable>
    </template>
    <template #footer>
      <a-button type="primary" @click="closeDrawer()">关闭</a-button>
    </template>
  </BasicDrawer>

  <!-- 添加成员弹窗 -->
  <a-modal
    v-model:open="addVisible"
    title="添加成员"
    :width="420"
    :confirm-loading="adding"
    @ok="handleAddMember"
    @cancel="addVisible = false"
  >
    <a-form layout="vertical">
      <a-form-item label="用户" required>
        <a-select
          v-model:value="addForm.userId"
          show-search
          option-filter-prop="label"
          placeholder="请选择用户"
          :options="userOptions"
        />
      </a-form-item>
      <a-form-item label="角色类型">
        <a-select v-model:value="addForm.role" placeholder="请选择角色">
          <a-select-option value="member">成员</a-select-option>
          <a-select-option value="restricted">受限</a-select-option>
          <a-select-option value="admin">管理员</a-select-option>
        </a-select>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script lang="ts" name="homeai-family-members" setup>
  import { computed, reactive, ref } from 'vue';
  import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
  import { BasicTable, useTable } from '/@/components/Table';
  import { familyApi, userApi } from '/@/api/homeai';
  import { useMessage } from '/@/hooks/web/useMessage';

  const emit = defineEmits(['success']);
  const { createMessage, createConfirm } = useMessage();

  const familyId = ref('');
  const familyName = ref('');
  const memberList = ref<any[]>([]);
  const selectedRowKeys = ref<string[]>([]);

  const drawerTitle = computed(() => `成员管理 - ${familyName.value || ''}`);

  const rowSelection = computed(() => ({
    selectedRowKeys: selectedRowKeys.value,
    onChange: (keys: string[]) => {
      selectedRowKeys.value = keys;
    },
  }));

  const [registerDrawer, { closeDrawer }] = useDrawerInner((data) => {
    familyId.value = data?.familyId || '';
    familyName.value = data?.familyName || '';
    selectedRowKeys.value = [];
    loadMembers();
  });

  const columns = [
    { title: '微信昵称', dataIndex: 'nickname', width: 150 },
    { title: '手机号', dataIndex: 'phone', width: 130 },
    { title: '角色', dataIndex: 'role', key: 'role', width: 140 },
    { title: '加入时间', dataIndex: 'joinedAt', width: 180 },
    { title: '操作', dataIndex: 'action', key: 'action', width: 80, fixed: 'right' },
  ];

  const [registerTable, { reload }] = useTable({
    title: '家庭成员列表',
    api: (params: any) => {
      if (!familyId.value) {
        return Promise.resolve({ records: [], total: 0 });
      }
      return familyApi.adminMembers(familyId.value).then((res: any) => {
        const list = Array.isArray(res) ? res : [];
        // 简单分页
        const pageNo = params?.pageNo || 1;
        const pageSize = params?.pageSize || 10;
        const start = (pageNo - 1) * pageSize;
        return { records: list.slice(start, start + pageSize), total: list.length };
      });
    },
    columns: columns,
    useSearchForm: false,
    showIndexColumn: true,
    pagination: { pageSize: 10 },
    actionColumn: { width: 80, title: '操作', dataIndex: 'action' },
  });

  async function loadMembers() {
    if (!familyId.value) return;
    try {
      const res: any = await familyApi.adminMembers(familyId.value);
      memberList.value = Array.isArray(res) ? res : [];
      reload();
    } catch (e: any) {
      createMessage.error(e?.message || '加载成员失败');
    }
  }

  // ---------- 添加成员 ----------
  const addVisible = ref(false);
  const adding = ref(false);
  const addForm = reactive<{ userId?: string; role: string }>({ userId: undefined, role: 'member' });
  const userOptions = ref<any[]>([]);

  async function openAddModal() {
    addForm.userId = undefined;
    addForm.role = 'member';
    // 加载未加入家庭的用户
    try {
      const res: any = await userApi.list({ pageNo: 1, pageSize: 1000, delFlag: 0 });
      const list = res?.records || res || [];
      userOptions.value = list
        .filter((item: any) => !memberList.value.some((m: any) => m.userId === item.id))
        .map((item: any) => ({ label: `${item.nickname || '未知'}` + (item.phone ? `(${item.phone})` : ''), value: item.id }));
      addVisible.value = true;
    } catch {
      userOptions.value = [];
      createMessage.warning('用户列表加载失败');
      addVisible.value = true;
    }
  }

  async function handleAddMember() {
    if (!addForm.userId) {
      createMessage.warning('请选择用户');
      return;
    }
    adding.value = true;
    try {
      await familyApi.adminAddMember({ familyId: familyId.value, userId: addForm.userId, role: addForm.role });
      createMessage.success('添加成功');
      addVisible.value = false;
      await loadMembers();
      emit('success');
    } catch (e: any) {
      createMessage.error(e?.message || '添加失败');
    } finally {
      adding.value = false;
    }
  }

  // ---------- 修改角色 ----------
  async function applyRoleChange(record: any, role: string) {
    try {
      await familyApi.adminUpdateRole(record.memberId, role);
      createMessage.success('角色修改成功');
      await loadMembers();
      emit('success');
    } catch (e: any) {
      createMessage.error(e?.message || '修改角色失败');
      await loadMembers();
    }
  }

  async function handleChangeRole(record: any, role: string) {
    if (role === record.role) return;
    if (role === 'admin') {
      createConfirm({
        iconType: 'warning',
        title: '确认升为管理员',
        content: `确定将「${record.nickname || '该成员'}」设为管理员吗？`,
        onOk: async () => {
          await applyRoleChange(record, role);
        },
      });
      await loadMembers();
      return;
    }
    await applyRoleChange(record, role);
  }

  // ---------- 移除成员 ----------
  function handleRemove(record: any) {
    createConfirm({
      iconType: 'warning',
      title: '确认移除',
      content: `确定将成员「${record.nickname || '未知'}」从该家庭移除吗？`,
      onOk: async () => {
        try {
          await familyApi.adminRemoveMember(record.memberId);
          createMessage.success('移除成功');
          await loadMembers();
          emit('success');
        } catch (e: any) {
          createMessage.error(e?.message || '移除失败');
        }
      },
    });
  }
</script>
