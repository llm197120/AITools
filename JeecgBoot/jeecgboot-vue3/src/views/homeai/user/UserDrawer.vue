<template>
  <BasicDrawer v-bind="$attrs" @register="registerDrawer" :title="drawerTitle" width="40%">
    <!-- 查看模式 -->
    <template v-if="!isUpdate">
      <Description :column="1" :data="record" :schema="viewSchema" />
    </template>
    <!-- 编辑模式 -->
    <BasicForm v-else @register="registerForm" @submit="handleSubmit" />
    <!-- 底部按钮 -->
    <template #footer>
      <template v-if="isUpdate">
        <a-button type="primary" @click="submit">保存</a-button>
        <a-button style="margin-left: 8px" @click="closeDrawer()">取消</a-button>
      </template>
      <a-button v-else @click="closeDrawer()">关闭</a-button>
    </template>
  </BasicDrawer>
</template>

<script lang="ts" name="homeai-user-drawer" setup>
import { computed, ref } from 'vue';
import { BasicDrawer, useDrawerInner } from '/@/components/Drawer';
import { Description } from '/@/components/Description';
import { BasicForm, useForm } from '/@/components/Form';
import { userApi, familyApi } from '/@/api/homeai';
import { useMessage } from '/@/hooks/web/useMessage';

const emit = defineEmits(['success']);
const { createMessage } = useMessage();
const record = ref<Recordable>({});
const isUpdate = ref(false);
const familyOptions = ref<any[]>([]);

const drawerTitle = computed(() => {
    if (!isUpdate.value) return '用户详情';
    return record.value.id ? '编辑用户' : '新增用户';
  });

// 加载家庭下拉选项
async function loadFamilyOptions() {
  try {
    const res: any = await familyApi.list({ pageNo: 1, pageSize: 1000 });
    const list = res?.records || res || [];
    familyOptions.value = list
      .filter((item: any) => item.delFlag === 0)
      .map((item: any) => ({ label: item.name, value: item.id }));
  } catch {
    familyOptions.value = [];
  }
}

const [registerDrawer, { closeDrawer }] = useDrawerInner((data) => {
    isUpdate.value = data.isUpdate || false;
    record.value = data.record || {};
    loadFamilyOptions();
    if (isUpdate.value && data.record?.id) {
      setFieldsValue({ ...data.record });
    } else if (isUpdate.value) {
      resetFields();
    }
  });

  const viewSchema: any[] = [
    { label: '微信昵称', field: 'nickname' },
    { label: 'openid', field: 'openid' },
    { label: '手机号', field: 'phone' },
    { label: '所属家庭', field: 'familyName', render: (val: string) => val || '无' },
    { label: '家庭角色', field: 'familyRole' },
    { label: '角色类型', field: 'familyRoleType' },
    { label: '状态', field: 'status', render: (val: string) => (val === '1' ? '正常' : '禁用') },
    { label: '注册时间', field: 'createTime' },
    { label: '最后登录', field: 'lastLoginTime' },
  ];

  const [registerForm, { setFieldsValue, resetFields, submit }] = useForm({
    labelWidth: 100,
    schemas: [
      { field: 'nickname', label: '微信昵称', component: 'Input', required: true },
      { field: 'phone', label: '手机号', component: 'Input', required: true },
      {
        field: 'familyId',
        label: '所属家庭',
        component: 'Select',
        componentProps: {
          options: familyOptions,
          allowClear: true,
          placeholder: '请选择家庭（不选则为无家庭）',
        },
      },
      {
        field: 'familyRole',
        label: '家庭角色',
        component: 'Select',
        componentProps: {
          options: [
            { label: '爸爸', value: '爸爸' },
            { label: '妈妈', value: '妈妈' },
            { label: '孩子', value: '孩子' },
            { label: '其他', value: '其他' },
          ],
        },
      },
      {
        field: 'familyRoleType',
        label: '角色类型',
        component: 'Select',
        componentProps: {
          options: [
            { label: '管理员', value: 'admin' },
            { label: '成员', value: 'member' },
            { label: '受限', value: 'restricted' },
          ],
        },
      },
      {
        field: 'status',
        label: '状态',
        component: 'RadioGroup',
        componentProps: {
          options: [
            { label: '正常', value: '1' },
            { label: '禁用', value: '0' },
          ],
        },
        defaultValue: '1',
      },
    ],
    showSubmitButton: false,
    showResetButton: false,
  });

  async function handleSubmit(values: any) {
    try {
      const { familyId, ...rest } = values;
      if (record.value.id) {
        // 编辑：基本信息走 edit（忽略家庭字段），家庭关联单独维护
        await userApi.edit(record.value.id, rest);
        await userApi.setFamily(record.value.id, { familyId: familyId || undefined });
        createMessage.success('编辑成功');
      } else {
        // 新增：后端 add 会根据 familyId 自动同步家庭关联
        await userApi.add(values);
        createMessage.success('新增成功');
      }
      closeDrawer();
      emit('success');
      return true;
    } catch (e: any) {
      createMessage.error(e?.message || '操作失败');
      return false;
    }
  }
</script>
