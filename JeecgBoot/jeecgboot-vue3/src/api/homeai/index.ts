import { defHttp } from '/@/utils/http/axios';
import type {
  HomeaiBill,
  HomeaiCategory,
  HomeaiFamily,
  HomeaiFamilyMember,
  HomeaiFileWhitelistItem,
  HomeaiLearnMaterial,
  HomeaiPageParams,
  HomeaiPayload,
  HomeaiPlan,
  HomeaiPlanConfig,
  HomeaiRecipe,
  HomeaiStorageFolder,
  HomeaiUser,
} from './types';

// 类型再导出，保持 `import { xxx } from '/@/api/homeai'` 兼容
export type * from './types';

const BASE = '/homeai';

/** 微信用户管理 API */
export const userApi = {
  /** 用户列表 */
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/user/list`, params }),
  /** 用户详情 */
  getById: (id: string) => defHttp.get({ url: `${BASE}/user/${id}` }),
  /** 新增用户 */
  add: (data: Partial<HomeaiUser> | HomeaiPayload) => defHttp.post({ url: `${BASE}/user`, data }),
  /** 编辑用户 */
  edit: (id: string, data: Partial<HomeaiUser> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/user/${id}`, data }),
  /** 注销用户 */
  delete: (id: string) => defHttp.delete({ url: `${BASE}/user/${id}` }),
  /** 启用/禁用 */
  updateStatus: (id: string, status: string) =>
    defHttp.put({ url: `${BASE}/user/${id}/status`, params: { status } }, { joinParamsToUrl: true }),
  /** 设置用户所属家庭（familyId 为空表示解除关联） */
  setFamily: (id: string, data: { familyId?: string; role?: string }) =>
    defHttp.put({ url: `${BASE}/user/${id}/family`, data }),
  /** 用户下拉（日历筛选等） */
  options: () => defHttp.get({ url: `${BASE}/user/options` }),
  /** 导出Excel */
  exportXls: `${BASE}/user/exportXls`,
  /** 导入Excel */
  importExcel: `${BASE}/user/importExcel`,
  /** 回收站列表 */
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/user/recycleBin`, params }),
  /** 移入回收站 */
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/user/moveToRecycleBin`, data: ids }),
  /** 从回收站恢复 */
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/user/restore`, data: ids }),
  /** 彻底删除 */
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/user/deletePermanently`, data: ids }),
};

/** 家庭管理 API */
export const familyApi = {
  /** 家庭列表 */
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/family/list`, params }),
  /** 家庭详情（当前用户所属家庭；id 预留与列表行联动） */
  getById: (_id?: string) => defHttp.get({ url: `${BASE}/family/info` }),
  /** 新增家庭（管理端） */
  add: (data: Partial<HomeaiFamily> | HomeaiPayload) => defHttp.post({ url: `${BASE}/family/add`, data }),
  /** 编辑家庭（管理端） */
  edit: (id: string, data: Partial<HomeaiFamily> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/family/${id}`, data }),
  /** 创建家庭 */
  create: (data: Partial<HomeaiFamily> | HomeaiPayload) => defHttp.post({ url: `${BASE}/family`, data }),
  /** 编辑家庭 */
  update: (data: Partial<HomeaiFamily> | HomeaiPayload) => defHttp.put({ url: `${BASE}/family`, data }),
  /** 解散家庭 */
  disband: () => defHttp.delete({ url: `${BASE}/family/disband` }),
  /** 转让管理员 */
  transfer: (targetUserId: string) =>
    defHttp.post({ url: `${BASE}/family/transfer`, params: { targetUserId } }),
  /** 导出Excel */
  exportXls: `${BASE}/family/exportXls`,
  /** 导入Excel */
  importExcel: `${BASE}/family/importExcel`,
  /** 回收站列表 */
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/family/recycleBin`, params }),
  /** 移入回收站 */
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/family/moveToRecycleBin`, data: ids }),
  /** 从回收站恢复 */
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/family/restore`, data: ids }),
  /** 彻底删除 */
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/family/deletePermanently`, data: ids }),
  /** 家庭成员列表（管理端） */
  adminMembers: (familyId: string) => defHttp.get({ url: `${BASE}/family/admin/members`, params: { familyId } }),
  /** 添加家庭成员（管理端） */
  adminAddMember: (data: Partial<HomeaiFamilyMember> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/family/admin/members`, data }),
  /** 移除家庭成员（管理端） */
  adminRemoveMember: (id: string) => defHttp.delete({ url: `${BASE}/family/admin/member/${id}` }),
  /** 修改成员角色（管理端） */
  adminUpdateRole: (id: string, role: string) =>
    defHttp.put({ url: `${BASE}/family/admin/member/${id}/role`, data: { role } }),
};

/** 账单管理 API */
export const billApi = {
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/bill/list`, params }),
  add: (data: Partial<HomeaiBill> | HomeaiPayload) => defHttp.post({ url: `${BASE}/bill/add`, data }),
  edit: (id: string, data: Partial<HomeaiBill> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/bill/${id}`, data }),
  exportXls: `${BASE}/bill/exportXls`,
  importExcel: `${BASE}/bill/importExcel`,
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/bill/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/bill/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/bill/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/bill/deletePermanently`, data: ids }),
  categoryList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/bill/category-list`, params }),
  addCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/bill/category`, data }),
  editCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/bill/category`, data }),
  deleteCategory: (id: string) => defHttp.delete({ url: `${BASE}/bill/category/${id}` }),
};

/** 计划管理 API */
export const planApi = {
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/plan/list`, params }),
  add: (data: Partial<HomeaiPlan> | HomeaiPayload) => defHttp.post({ url: `${BASE}/plan/add`, data }),
  edit: (id: string, data: Partial<HomeaiPlan> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/plan/${id}`, data }),
  exportXls: `${BASE}/plan/exportXls`,
  importExcel: `${BASE}/plan/importExcel`,
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/plan/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/plan/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/plan/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/plan/deletePermanently`, data: ids }),
  /** 启用的计划分类（下拉） */
  categories: () => defHttp.get({ url: `${BASE}/plan/categories` }),
  categoryList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/plan/category-list`, params }),
  addCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/plan/category`, data }),
  editCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/plan/category`, data }),
  deleteCategory: (id: string) => defHttp.delete({ url: `${BASE}/plan/category/${id}` }),
  /** 管理端日历 */
  adminCalendar: (params: { yearMonth: string; userId?: string }) =>
    defHttp.get({ url: `${BASE}/plan/admin/calendar`, params }),
  adminPlansByDate: (date: string, userId?: string) =>
    defHttp.get({ url: `${BASE}/plan/admin/date/${date}`, params: userId ? { userId } : {} }),
  rollForwardRepeat: (masterId?: string) =>
    defHttp.post(
      { url: `${BASE}/plan/admin/repeat/roll-forward`, params: masterId ? { masterId } : {} },
      { joinParamsToUrl: true },
    ),
  rollForwardLogs: (params?: { pageNo?: number; pageSize?: number }) =>
    defHttp.get({ url: `${BASE}/plan/admin/repeat/roll-forward/logs`, params }),
  completion: (params?: { userId?: string; yearMonth?: string }) =>
    defHttp.get({ url: `${BASE}/plan/admin/completion`, params }),
};

/** 审计日志 API */
export const auditApi = {
  logs: (params?: { module?: string; actionType?: string; pageNo?: number; pageSize?: number }) =>
    defHttp.get({ url: `${BASE}/audit/logs`, params }),
};

/** 菜谱管理 API */
export const recipeApi = {
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/recipe/list`, params }),
  /** 菜谱详情（含食材/步骤） */
  getById: (id: string) => defHttp.get({ url: `${BASE}/recipe/${id}` }),
  add: (data: Partial<HomeaiRecipe> | HomeaiPayload) => defHttp.post({ url: `${BASE}/recipe/add`, data }),
  edit: (id: string, data: Partial<HomeaiRecipe> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/recipe/${id}`, data }),
  exportXls: `${BASE}/recipe/exportXls`,
  importExcel: `${BASE}/recipe/importExcel`,
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/recipe/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/recipe/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/recipe/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/recipe/deletePermanently`, data: ids }),
  /** 上传菜谱视频 */
  uploadVideo: (id: string) => `${BASE}/recipe/${id}/video`,
  /** 删除菜谱视频 */
  deleteVideo: (id: string) => defHttp.delete({ url: `${BASE}/recipe/${id}/video` }),
  /** 启用的菜谱分类（下拉） */
  categories: () => defHttp.get({ url: `${BASE}/recipe/category/all` }),
  categoryList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/recipe/category/list`, params }),
  addCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/recipe/category`, data }),
  editCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/recipe/category`, data }),
  deleteCategory: (id: string) => defHttp.delete({ url: `${BASE}/recipe/category/${id}` }),
  toggleFavorite: (id: string) => defHttp.post({ url: `${BASE}/recipe/${id}/favorite` }),
  favorites: () => defHttp.get({ url: `${BASE}/recipe/favorites` }),
  /** 热门排行 */
  hot: (limit = 20) => defHttp.get({ url: `${BASE}/recipe/hot`, params: { limit } }),
  /** 为你推荐 */
  recommend: (limit = 8, season = 'auto') =>
    defHttp.get({ url: `${BASE}/recipe/recommend`, params: { limit, season } }),
  newest: (limit = 8, days = 30) =>
    defHttp.get({ url: `${BASE}/recipe/new`, params: { limit, days } }),
};

/** 综合统计 API */
export const dashboardApi = {
  planLearn: (params?: { yearMonth?: string; days?: number; userId?: string }) =>
    defHttp.get({ url: `${BASE}/dashboard/plan-learn`, params }),
};

/** 学习资料 API */
export const learnApi = {
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/learn/materials`, params }),
  add: (data: Partial<HomeaiLearnMaterial> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/learn/addMaterial`, data }),
  edit: (id: string, data: Partial<HomeaiLearnMaterial> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/learn/material/${id}`, data }),
  exportXls: `${BASE}/learn/exportXls`,
  importExcel: `${BASE}/learn/importExcel`,
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/learn/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/learn/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/learn/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/learn/deletePermanently`, data: ids }),
  /** 上传学习资料文件 */
  uploadFile: (id: string) => `${BASE}/learn/materials/${id}/upload`,
  /** 新增前预上传文件 */
  uploadTemp: () => `${BASE}/learn/upload`,
  /** 启用的学习分类（下拉） */
  categories: () => defHttp.get({ url: `${BASE}/learn/category/all` }),
  categoryList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/learn/category/list`, params }),
  addCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/learn/category`, data }),
  editCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/learn/category`, data }),
  deleteCategory: (id: string) => defHttp.delete({ url: `${BASE}/learn/category/${id}` }),
  adminStatsTrend: (days = 30) => defHttp.get({ url: `${BASE}/learn/admin/stats/trend`, params: { days } }),
  /** 管理端汇总（支持 days / userId） */
  adminStats: (params?: { days?: number; userId?: string }) =>
    defHttp.get({ url: `${BASE}/learn/admin/stats`, params }),
  /** 管理端：按分类学习统计（时长单位：分钟） */
  adminStatsByCategory: (params?: { days?: number; userId?: string }) =>
    defHttp.get({ url: `${BASE}/learn/admin/stats/category`, params }),
  /** 管理端：按用户排行 */
  adminStatsByUser: (days = 30) => defHttp.get({ url: `${BASE}/learn/admin/stats/user`, params: { days } }),
  /** 管理端：多维统计 Excel 导出 */
  adminStatsExport: `${BASE}/learn/admin/stats/export`,
};

/** 系统配置 API */
export const configApi = {
  getFileWhitelist: () => defHttp.get({ url: `${BASE}/config/file-whitelist` }),
  updateFileWhitelist: (items: HomeaiFileWhitelistItem[]) =>
    defHttp.put({ url: `${BASE}/config/file-whitelist`, data: items }),
  getPlanConfig: () => defHttp.get({ url: `${BASE}/config/plan` }),
  updatePlanConfig: (data: Partial<HomeaiPlanConfig> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/config/plan`, data }),
  getWechatPublic: () => defHttp.get({ url: `${BASE}/config/wechat-public` }),
};

/** 资料存储管理 API */
export const storageApi = {
  fileList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/storage/file-list`, params }),
  folderList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/storage/folder-list`, params }),
  folderTree: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/storage/folders`, params }),
  createFolder: (data: Partial<HomeaiStorageFolder> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/storage/folders`, params: data }, { joinParamsToUrl: true }),
  /** 修改文件夹可见性（后端为 PATCH；经 request 发起，避免 VAxios 无 patch 快捷方法） */
  updateFolderVisibility: (id: string, visibility: string) =>
    defHttp.request(
      { url: `${BASE}/storage/folders/${id}/visibility`, method: 'PATCH', params: { visibility } },
      { joinParamsToUrl: true },
    ),
  deleteFile: (id: string) => defHttp.delete({ url: `${BASE}/storage/files/${id}` }),
  deleteFolder: (id: string) => defHttp.delete({ url: `${BASE}/storage/folders/${id}` }),
  recycleBin: (params?: HomeaiPageParams & { keyword?: string; type?: 'file' | 'folder' }) =>
    defHttp.get({ url: `${BASE}/storage/recycleBin`, params }),
  /** 恢复：传 fileIds / folderIds（兼容旧版纯数组） */
  restore: (payload: string[] | { fileIds?: string[]; folderIds?: string[] }) =>
    defHttp.put({
      url: `${BASE}/storage/restore`,
      data: Array.isArray(payload) ? { fileIds: payload } : payload,
    }),
  deletePermanently: (payload: string[] | { fileIds?: string[]; folderIds?: string[] }) =>
    defHttp.delete({
      url: `${BASE}/storage/deletePermanently`,
      data: Array.isArray(payload) ? { fileIds: payload } : payload,
    }),
  storageConfig: () => defHttp.get({ url: `${BASE}/config/storage` }),
  saveStorageConfig: (data: {
    defaultUserLimitBytes?: number;
    defaultFamilyLimitBytes?: number;
    warnPercent?: number;
  }) =>
    defHttp.put({ url: `${BASE}/config/storage`, data }),
  getFamilyStorageLimit: (familyId: string) =>
    defHttp.get({ url: `${BASE}/config/storage/family/${familyId}` }),
  setFamilyStorageLimit: (familyId: string, limitBytes: number | null) =>
    defHttp.put({ url: `${BASE}/config/storage/family/${familyId}`, data: { limitBytes } }),
  clearFamilyStorageLimit: (familyId: string) =>
    defHttp.delete({ url: `${BASE}/config/storage/family/${familyId}` }),
  checkGenerateQuota: (instruction?: string) =>
    defHttp.get({
      url: `${BASE}/storage/office/generate/quota-check`,
      params: instruction ? { instruction } : {},
    }),
};

//update-begin---author:admin ---date:2026-07-31  for：AI管理API集中定义-----------
/** AI 对话管理 API */
export const conversationApi = {
  /** 管理端对话列表（分页） */
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/ai/conversations/list`, params }),
  /** 对话详情 */
  getById: (id: string) => defHttp.get({ url: `${BASE}/ai/conversations/${id}` }),
  /** 获取对话消息列表 */
  getMessages: (id: string) => defHttp.get({ url: `${BASE}/ai/conversations/${id}/messages` }),
  /** 重命名对话 */
  rename: (id: string, title: string) =>
    defHttp.put({ url: `${BASE}/ai/conversations/${id}/rename`, params: { title } }, { joinParamsToUrl: true }),
  /** 删除对话 */
  delete: (id: string) => defHttp.delete({ url: `${BASE}/ai/conversations/${id}` }),
};

/** AI 对话（SSE流式）API */
export const chatApi = {
  /** Token配额检查 */
  checkQuota: (params?: HomeaiPageParams | HomeaiPayload) =>
    defHttp.get({ url: `${BASE}/ai/chat/quota`, params }),
  /** 停止生成 */
  stop: () => defHttp.post({ url: `${BASE}/ai/chat/stop` }),
  /** 上传对话附件文件 */
  uploadFile: `${BASE}/ai/chat/upload`,
};

/** AI 密钥配置 API */
export const keyConfigApi = {
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/ai/key-config/list`, params }),
  add: (data: HomeaiPayload) => defHttp.post({ url: `${BASE}/ai/key-config`, data }),
  edit: (data: HomeaiPayload) => defHttp.put({ url: `${BASE}/ai/key-config`, data }),
  delete: (id: string) => defHttp.delete({ url: `${BASE}/ai/key-config/${id}` }),
  toggleStatus: (id: string, isEnabled?: string) =>
    defHttp.put(
      { url: `${BASE}/ai/key-config/${id}/status`, params: isEnabled ? { isEnabled } : {} },
      { joinParamsToUrl: true },
    ),
  setDefault: (id: string) => defHttp.put({ url: `${BASE}/ai/key-config/${id}/default` }),
};

/** AI Token额度 API */
export const quotaApi = {
  /** 获取默认配额配置 */
  getDefaultQuota: () => defHttp.get({ url: `${BASE}/ai/key-config/quota/default` }),
  /** 获取用户Token消耗统计（管理端分页） */
  logList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/ai/key-config/quota/list`, params }),
  /** 用户额度配置列表（含每日/每月限额） */
  getUserQuotaPage: (params?: HomeaiPageParams) =>
    defHttp.get({ url: `${BASE}/ai/key-config/quota/user/list`, params }),
  /** 更新用户额度配置 */
  updateUserQuota: (data: HomeaiPayload) => defHttp.put({ url: `${BASE}/ai/key-config/quota/user`, data }),
  /** 额度使用概览 */
  getOverview: () => defHttp.get({ url: `${BASE}/ai/key-config/quota/overview` }),
};
//update-end---author:admin ---date:2026-07-31  for：AI管理API集中定义-----------
