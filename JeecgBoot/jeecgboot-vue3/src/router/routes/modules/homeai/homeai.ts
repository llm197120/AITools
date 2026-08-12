/**
 * HomeAI 管理端路由定义（开发参考 / 文档用途）
 *
 * 【权限模式说明】
 * - 当前 projectSetting.permissionMode 为 BACK：动态路由与侧边菜单以后台 DB 菜单为准
 *   （getBackMenuAndPerms → permission store 构建），本文件不会在 BACK 模式下直接注册进路由表
 * - 本文件仅作开发对照与文档：组件路径须与 DB 菜单 component 字段保持一致
 *   （例如 views/homeai/user/index → 菜单 component 填 homeai/user/index）
 * - 请勿删除本路由定义：若切换为 ROUTE_MAPPING / ROLE 等前端路由映射模式，仍会用到 asyncRoutes
 */
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
    {
      path: 'user',
      name: 'HomeaiUser',
      component: () => import('/@/views/homeai/user/index.vue'),
      meta: { title: '用户管理', icon: 'ant-design:user-outlined' },
    },
    {
      path: 'family',
      name: 'HomeaiFamily',
      component: () => import('/@/views/homeai/family/index.vue'),
      meta: { title: '家庭管理', icon: 'ant-design:team-outlined' },
    },
    {
      path: 'dashboard/crossStats',
      name: 'HomeaiDashboardCrossStats',
      component: () => import('/@/views/homeai/dashboard/crossStats.vue'),
      meta: { title: '综合统计', icon: 'ant-design:dashboard-outlined' },
    },
    // AI 管理（与设计文档 / 菜单 SQL 路径一致：/homeai/ai/*）
    {
      path: 'ai',
      name: 'HomeaiAi',
      component: LAYOUT,
      redirect: '/homeai/ai/conversationList',
      meta: { title: 'AI管理', icon: 'ant-design:robot-outlined' },
      children: [
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
      ],
    },
    {
      path: 'storage',
      name: 'HomeaiStorage',
      component: LAYOUT,
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
        {
          path: 'fileWhitelist',
          name: 'HomeaiStorageFileWhitelist',
          component: () => import('/@/views/homeai/storage/fileWhitelist.vue'),
          meta: { title: '文件白名单', icon: 'ant-design:safety-outlined' },
        },
      ],
    },
    {
      path: 'bill',
      name: 'HomeaiBill',
      component: LAYOUT,
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
    {
      path: 'plan',
      name: 'HomeaiPlan',
      component: LAYOUT,
      redirect: '/homeai/plan/planList',
      meta: { title: '计划管理', icon: 'ant-design:calendar-outlined' },
      children: [
        {
          path: 'planList',
          name: 'HomeaiPlanList',
          component: () => import('/@/views/homeai/plan/planList.vue'),
          meta: { title: '计划列表' },
        },
        {
          path: 'planCategory',
          name: 'HomeaiPlanCategory',
          component: () => import('/@/views/homeai/plan/planCategory.vue'),
          meta: { title: '计划分类' },
        },
        {
          path: 'planConfig',
          name: 'HomeaiPlanConfig',
          component: () => import('/@/views/homeai/plan/planConfig.vue'),
          meta: { title: '计划配置' },
        },
        {
          path: 'auditLog',
          name: 'HomeaiPlanAuditLog',
          component: () => import('/@/views/homeai/plan/auditLog.vue'),
          meta: { title: '操作审计' },
        },
      ],
    },
    {
      path: 'recipe',
      name: 'HomeaiRecipe',
      component: LAYOUT,
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
    {
      path: 'learn',
      name: 'HomeaiLearn',
      component: LAYOUT,
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
          path: 'learnCategory',
          name: 'HomeaiLearnCategory',
          component: () => import('/@/views/homeai/learn/learnCategory.vue'),
          meta: { title: '学习分类' },
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
