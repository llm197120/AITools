import { defHttp } from '/@/utils/http/axios';
import type {
  HomeaiAppVersion,
  HomeaiAuditLog,
  HomeaiBill,
  HomeaiCategory,
  HomeaiConvertRule,
  HomeaiConvertTask,
  HomeaiFamily,
  HomeaiFamilyMember,
  HomeaiFamilyQuotaItem,
  HomeaiFileWhitelistItem,
  HomeaiFilePreview,
  HomeaiLearnMaterial,
  HomeaiOfficeTemplate,
  HomeaiPageParams,
  HomeaiPageResult,
  HomeaiPayload,
  HomeaiPlan,
  HomeaiPlanConfig,
  HomeaiRecipe,
  HomeaiRecipeDetail,
  HomeaiStorageFile,
  HomeaiStorageFolder,
  HomeaiUser,
} from './types';

// 类型再导出，保持 `import { xxx } from '/@/api/homeai'` 兼容
export type * from './types';

const BASE = '/homeai';

/** 微信用户管理 API */
export const userApi = {
  /** 用户列表 */
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiUser> | HomeaiUser[]> =>
    defHttp.get({ url: `${BASE}/user/list`, params }),
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
  /** 重置 App 登录密码为默认 123456 */
  resetPassword: (id: string) => defHttp.put({ url: `${BASE}/user/${id}/resetPassword` }),
  /** 设置用户所属家庭（familyId 为空表示解除关联） */
  setFamily: (id: string, data: { familyId?: string; role?: string }) =>
    defHttp.put({ url: `${BASE}/user/${id}/family`, data }),
  /** 用户下拉（日历筛选等） */
  options: () => defHttp.get({ url: `${BASE}/user/options` }),
  /** 导出Excel */
  exportXls: `${BASE}/user/exportXls`,
  /** 导入模板 */
  exportTemplate: `${BASE}/user/exportTemplate`,
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
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiFamily> | HomeaiFamily[]> => defHttp.get({ url: `${BASE}/family/list`, params }),
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
  /** 导入模板 */
  exportTemplate: `${BASE}/family/exportTemplate`,
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
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiBill> | HomeaiBill[]> => defHttp.get({ url: `${BASE}/bill/list`, params }),
  add: (data: Partial<HomeaiBill> | HomeaiPayload) => defHttp.post({ url: `${BASE}/bill/add`, data }),
  edit: (id: string, data: Partial<HomeaiBill> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/bill/${id}`, data }),
  exportXls: `${BASE}/bill/exportXls`,
  importExcel: `${BASE}/bill/importExcel`,
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/bill/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/bill/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/bill/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/bill/deletePermanently`, data: ids }),
  categoryList: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiCategory> | HomeaiCategory[]> => defHttp.get({ url: `${BASE}/bill/category-list`, params }),
  addCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/bill/category`, data }),
  editCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/bill/category`, data }),
  deleteCategory: (id: string) => defHttp.delete({ url: `${BASE}/bill/category/${id}` }),
  /** 管理端统计（按分类/用户/月份） */
  adminStats: (params?: { yearMonth?: string; dimension?: string }): Promise<{
    totalExpense?: number;
    totalIncome?: number;
    balance?: number;
    count?: number;
    rows?: { name?: string; expense?: number; income?: number }[];
  }> =>
    defHttp.get({ url: `${BASE}/bill/admin/stats`, params }),
};

/** 计划管理 API */
export const planApi = {
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiPlan> | HomeaiPlan[]> => defHttp.get({ url: `${BASE}/plan/list`, params }),
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
  logs: (params?: { module?: string; actionType?: string; pageNo?: number; pageSize?: number }): Promise<HomeaiPageResult<HomeaiAuditLog> | HomeaiAuditLog[]> =>
    defHttp.get({ url: `${BASE}/audit/logs`, params }),
};

/** 菜谱管理 API */
export const recipeApi = {
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiRecipe> | HomeaiRecipe[]> => defHttp.get({ url: `${BASE}/recipe/list`, params }),
  /** 菜谱详情（含食材/步骤） */
  getById: (id: string): Promise<HomeaiRecipeDetail> => defHttp.get({ url: `${BASE}/recipe/${id}` }),
  add: (data: Partial<HomeaiRecipe> | HomeaiPayload) => defHttp.post({ url: `${BASE}/recipe/add`, data }),
  edit: (id: string, data: Partial<HomeaiRecipe> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/recipe/${id}`, data }),
  exportXls: `${BASE}/recipe/exportXls`,
  exportTemplate: `${BASE}/recipe/exportTemplate`,
  importExcel: `${BASE}/recipe/importExcel`,
  importCovers: `${BASE}/recipe/import-covers`,
  recycleBin: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/recipe/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/recipe/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/recipe/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/recipe/deletePermanently`, data: ids }),
  /** 启用的菜谱分类（下拉） */
  categories: () => defHttp.get({ url: `${BASE}/recipe/category/all` }),
  categoryList: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/recipe/category/list`, params }),
  addCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.post({ url: `${BASE}/recipe/category`, data }),
  editCategory: (data: Partial<HomeaiCategory> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/recipe/category`, data }),
  deleteCategory: (id: string) => defHttp.delete({ url: `${BASE}/recipe/category/${id}` }),
};

/** 综合统计 API */
export const dashboardApi = {
  planLearn: (params?: { yearMonth?: string; days?: number; userId?: string }) =>
    defHttp.get({ url: `${BASE}/dashboard/plan-learn`, params }),
};

/** 学习资料 API */
export const learnApi = {
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiLearnMaterial> | HomeaiLearnMaterial[]> => defHttp.get({ url: `${BASE}/learn/materials`, params }),
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
  preview: (id: string): Promise<HomeaiFilePreview> => defHttp.get({ url: `${BASE}/learn/materials/${id}/preview` }),
  previewPdf: (id: string): Promise<HomeaiFilePreview> =>
    defHttp.post({ url: `${BASE}/learn/materials/${id}/preview-pdf` }),
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
  getFileWhitelist: (): Promise<{ items?: HomeaiFileWhitelistItem[] }> => defHttp.get({ url: `${BASE}/config/file-whitelist` }),
  updateFileWhitelist: (items: HomeaiFileWhitelistItem[]) =>
    defHttp.put({ url: `${BASE}/config/file-whitelist`, data: items }),
  getPlanConfig: (): Promise<HomeaiPlanConfig> => defHttp.get({ url: `${BASE}/config/plan` }),
  updatePlanConfig: (data: Partial<HomeaiPlanConfig> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/config/plan`, data }),
  getAppVersion: (): Promise<HomeaiAppVersion> => defHttp.get({ url: `${BASE}/app/version/admin` }),
  updateAppVersion: (data: Partial<HomeaiAppVersion> | HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/app/version/admin`, data }),
  uploadAppPackage: `${BASE}/app/version/upload`,
};

/** 资料存储管理 API */
export const storageApi = {
  fileList: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiStorageFile> | HomeaiStorageFile[]> => defHttp.get({ url: `${BASE}/storage/file-list`, params }),
  folderList: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiStorageFolder> | HomeaiStorageFolder[]> => defHttp.get({ url: `${BASE}/storage/folder-list`, params }),
  /** 文件夹树（管理端） */
  folderTree: (): Promise<HomeaiStorageFolder[]> => defHttp.get({ url: `${BASE}/storage/folders` }),
  /** 文件夹内文件列表 */
  folderFiles: (folderId: string, params?: HomeaiPageParams) =>
    defHttp.get({ url: `${BASE}/storage/folders/${folderId}/files`, params }),
  /** 新建文件夹 */
  createFolder: (params: Recordable) =>
    defHttp.post({ url: `${BASE}/storage/folders`, params }, { joinParamsToUrl: true }),
  /** 编辑文件夹 */
  updateFolder: (id: string, data: HomeaiPayload) =>
    defHttp.put({ url: `${BASE}/storage/folders/${id}`, data }),
  /** 删除文件夹 */
  deleteFolder: (id: string) => defHttp.delete({ url: `${BASE}/storage/folders/${id}` }),
  /** 空间用量统计 */
  stats: () => defHttp.get({ url: `${BASE}/storage/stats` }),
  /** 修改文件夹可见性（后端为 PATCH；经 request 发起，避免 VAxios 无 patch 快捷方法） */
  updateFolderVisibility: (id: string, visibility: string) =>
    defHttp.request(
      { url: `${BASE}/storage/folders/${id}/visibility`, method: 'PATCH', params: { visibility } },
      { joinParamsToUrl: true },
    ),
  deleteFile: (id: string) => defHttp.delete({ url: `${BASE}/storage/files/${id}` }),
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
  familyQuotaBoard: (params?: { keyword?: string; onlyWarn?: boolean; onlyCustom?: boolean }): Promise<{
    items?: HomeaiFamilyQuotaItem[];
    summary?: { families?: number; warnCount?: number; customCount?: number };
    defaultFamilyLimitBytes?: number;
    warnPercent?: number;
  }> =>
    defHttp.get({ url: `${BASE}/config/storage/families`, params }),
  batchFamilyStorageLimit: (data: {
    items?: { familyId: string; limitBytes: number | null }[];
    resetIds?: string[];
  }) => defHttp.put({ url: `${BASE}/config/storage/families/batch`, data }),
  preview: (id: string): Promise<HomeaiFilePreview> => defHttp.get({ url: `${BASE}/storage/files/${id}/preview` }),
  previewPdf: (id: string): Promise<HomeaiFilePreview> =>
    defHttp.post({ url: `${BASE}/storage/files/${id}/preview-pdf` }),
};

/** 资料存储 Office 处理 API（格式转换/处理记录） */
export const storageOfficeApi = {
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiConvertTask> | HomeaiConvertTask[]> => defHttp.get({ url: `${BASE}/storage/office/list`, params }),
  deleteTask: (id: string) => defHttp.delete({ url: `${BASE}/storage/office/${id}` }),
  /** 提交格式转换任务（后端为 @RequestParam，POST 参数走 URL） */
  convert: (params: Recordable) =>
    defHttp.post({ url: `${BASE}/storage/office/convert`, params }, { joinParamsToUrl: true }),
};

/** 资料存储 Office 模板 API */
export const storageTemplateApi = {
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiOfficeTemplate> | HomeaiOfficeTemplate[]> => defHttp.get({ url: `${BASE}/storage/template/list`, params }),
  create: (data: HomeaiPayload) => defHttp.post({ url: `${BASE}/storage/template`, data }),
  update: (data: HomeaiPayload) => defHttp.put({ url: `${BASE}/storage/template`, data }),
  delete: (id: string) => defHttp.delete({ url: `${BASE}/storage/template/${id}` }),
  setDefault: (id: string) => defHttp.put({ url: `${BASE}/storage/template/${id}/default` }),
};

/** 资料存储转换规则 API */
export const storageRuleApi = {
  list: (params?: HomeaiPageParams): Promise<HomeaiPageResult<HomeaiConvertRule> | HomeaiConvertRule[]> => defHttp.get({ url: `${BASE}/storage/rule/list`, params }),
  targets: (sourceFormat?: string) =>
    defHttp.get({ url: `${BASE}/storage/rule/targets`, params: sourceFormat ? { sourceFormat } : {} }),
  create: (data: HomeaiPayload) => defHttp.post({ url: `${BASE}/storage/rule`, data }),
  update: (data: HomeaiPayload) => defHttp.put({ url: `${BASE}/storage/rule`, data }),
  delete: (id: string) => defHttp.delete({ url: `${BASE}/storage/rule/${id}` }),
  toggleStatus: (id: string, isEnabled: string) =>
    defHttp.put(
      { url: `${BASE}/storage/rule/${id}/status`, params: { isEnabled } },
      { joinParamsToUrl: true },
    ),
};

//update-begin---author:admin ---date:2026-07-31  for：AI管理API集中定义-----------
/** AI 对话管理 API */
export const conversationApi = {
  /** 管理端对话列表（分页） */
  list: (params?: HomeaiPageParams) => defHttp.get({ url: `${BASE}/ai/conversations/list`, params }),
  /** 获取对话消息列表 */
  getMessages: (id: string) => defHttp.get({ url: `${BASE}/ai/conversations/${id}/messages` }),
  /** 删除对话 */
  delete: (id: string) => defHttp.delete({ url: `${BASE}/ai/conversations/${id}` }),
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
  /** 用户额度配置列表（含每日/每月限额） */
  getUserQuotaPage: (params?: HomeaiPageParams) =>
    defHttp.get({ url: `${BASE}/ai/key-config/quota/user/list`, params }),
  /** 更新用户额度配置 */
  updateUserQuota: (data: HomeaiPayload) => defHttp.put({ url: `${BASE}/ai/key-config/quota/user`, data }),
  /** 额度使用概览 */
  getOverview: () => defHttp.get({ url: `${BASE}/ai/key-config/quota/overview` }),
};
//update-end---author:admin ---date:2026-07-31  for：AI管理API集中定义-----------
