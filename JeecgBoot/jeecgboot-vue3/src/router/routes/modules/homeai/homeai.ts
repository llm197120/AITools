import type { AppRouteModule } from '/@/router/types';
import { LAYOUT } from '/@/router/constant';

const homeai: AppRouteModule = {
  path: '/homeai',
  name: 'HomeAI',
  component: LAYOUT,
  redirect: '/homeai/user',
  meta: {
    orderNo: 30,
    icon: 'ant-design:home-outlined',
    title: '家庭AI小工具',
  },
  children: [
    // 用户管理
    {
      path: 'user',
      name: 'HomeaiUser',
      component: () => import('/@/views/homeai/user/index.vue'),
      meta: { title: '用户管理', icon: 'ant-design:user-outlined' },
    },
    // 家庭管理
    {
      path: 'family',
      name: 'HomeaiFamily',
      component: () => import('/@/views/homeai/family/index.vue'),
      meta: { title: '家庭管理', icon: 'ant-design:team-outlined' },
    },
        {
          path: 'conversationList',
          name: 'HomeaiAiConversationList',
          component: () => import('/@/views/homeai/ai/conversationList.vue'),
          meta: { title: 'AI对话管理', icon: 'ant-design:message-outlined' },
        },
        {
          path: 'keyConfig',
          name: 'HomeaiAiKeyConfig',
          component: () => import('/@/views/homeai/ai/keyConfig.vue'),
          meta: { title: 'AI密钥配置', icon: 'ant-design:key-outlined' },
        },
        {
          path: 'quota',
          name: 'HomeaiAiQuota',
          component: () => import('/@/views/homeai/ai/quota.vue'),
          meta: { title: 'Token额度配置', icon: 'ant-design:dashboard-outlined' },
        },
    // 资料存储管理
    {
      path: 'storage',
      name: 'HomeaiStorage',
      redirect: '/homeai/storage/fileList',
      meta: { title: '资料存储管理', icon: 'ant-design:folder-outlined' },
      children: [
        {
          path: 'fileList',
          name: 'HomeaiStorageFileList',
          component: () => import('/@/views/homeai/storage/fileList.vue'),
          meta: { title: '文件管理', icon: 'ant-design:file-outlined' },
        },
        {
          path: 'officeTemplate',
          name: 'HomeaiStorageOfficeTemplate',
          component: () => import('/@/views/homeai/storage/officeTemplate.vue'),
          meta: { title: '文档模板', icon: 'ant-design:file-word-outlined' },
        },
        {
          path: 'convertRule',
          name: 'HomeaiStorageConvertRule',
          component: () => import('/@/views/homeai/storage/convertRule.vue'),
          meta: { title: '转换规则', icon: 'ant-design:swap-outlined' },
        },
        {
          path: 'officeHistory',
          name: 'HomeaiStorageOfficeHistory',
          component: () => import('/@/views/homeai/storage/officeHistory.vue'),
          meta: { title: '处理记录', icon: 'ant-design:history-outlined' },
        },
      ],
    },
    // 账单管理
    {
      path: 'bill',
      name: 'HomeaiBill',
      redirect: '/homeai/bill/billList',
      meta: { title: '账单管理', icon: 'ant-design:account-book-outlined' },
      children: [
        {
          path: 'billList',
          name: 'HomeaiBillList',
          component: () => import('/@/views/homeai/bill/billList.vue'),
          meta: { title: '账单列表' },
        },
        {
          path: 'billCategory',
          name: 'HomeaiBillCategory',
          component: () => import('/@/views/homeai/bill/billCategory.vue'),
          meta: { title: '消费分类' },
        },
        {
          path: 'billStatistics',
          name: 'HomeaiBillStatistics',
          component: () => import('/@/views/homeai/bill/billStatistics.vue'),
          meta: { title: '统计报表' },
        },
        {
          path: 'billImport',
          name: 'HomeaiBillImport',
          component: () => import('/@/views/homeai/bill/billImport.vue'),
          meta: { title: '账单导入' },
        },
      ],
    },
    // 计划管理
    {
      path: 'plan',
      name: 'HomeaiPlan',
      redirect: '/homeai/plan/planList',
      meta: { title: '计划管理', icon: 'ant-design:calendar-outlined' },
      children: [
        {
          path: 'planList',
          name: 'HomeaiPlanList',
          component: () => import('/@/views/homeai/plan/planList.vue'),
          meta: { title: '计划列表' },
        },
      ],
    },
    // 菜谱管理
    {
      path: 'recipe',
      name: 'HomeaiRecipe',
      redirect: '/homeai/recipe/recipeList',
      meta: { title: '菜谱管理', icon: 'ant-design:fire-outlined' },
      children: [
        {
          path: 'recipeList',
          name: 'HomeaiRecipeList',
          component: () => import('/@/views/homeai/recipe/recipeList.vue'),
          meta: { title: '菜谱列表' },
        },
        {
          path: 'recipeCategory',
          name: 'HomeaiRecipeCategory',
          component: () => import('/@/views/homeai/recipe/recipeCategory.vue'),
          meta: { title: '菜谱分类' },
        },
      ],
    },
    // 学习管理
    {
      path: 'learn',
      name: 'HomeaiLearn',
      redirect: '/homeai/learn/learnList',
      meta: { title: '学习管理', icon: 'ant-design:book-outlined' },
      children: [
        {
          path: 'learnList',
          name: 'HomeaiLearnList',
          component: () => import('/@/views/homeai/learn/learnList.vue'),
          meta: { title: '学习资料' },
        },
        {
          path: 'learnRecord',
          name: 'HomeaiLearnRecord',
          component: () => import('/@/views/homeai/learn/learnRecord.vue'),
          meta: { title: '学习记录' },
        },
      ],
    },
  ],
};

export default homeai;
