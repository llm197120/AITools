import { defHttp } from '/@/utils/http/axios';

const BASE = '/homeai';

/** 微信用户管理 API */
export const userApi = {
  /** 用户列表 */
  list: (params?: any) => defHttp.get({ url: `${BASE}/user/list`, params }),
  /** 用户详情 */
  getById: (id: string) => defHttp.get({ url: `${BASE}/user/${id}` }),
  /** 新增用户 */
  add: (data: any) => defHttp.post({ url: `${BASE}/user`, data }),
  /** 编辑用户 */
  edit: (id: string, data: any) => defHttp.put({ url: `${BASE}/user/${id}`, data }),
  /** 注销用户 */
  delete: (id: string) => defHttp.delete({ url: `${BASE}/user/${id}` }),
  /** 启用/禁用 */
  updateStatus: (id: string, status: string) =>
    defHttp.put({ url: `${BASE}/user/${id}/status`, params: { status } }, { joinParamsToUrl: true }),
  /** 设置用户所属家庭（familyId 为空表示解除关联） */
  setFamily: (id: string, data: { familyId?: string; role?: string }) =>
    defHttp.put({ url: `${BASE}/user/${id}/family`, data }),
  /** 导出Excel */
  exportXls: `${BASE}/user/exportXls`,
  /** 导入Excel */
  importExcel: `${BASE}/user/importExcel`,
  /** 回收站列表 */
  recycleBin: (params?: any) => defHttp.get({ url: `${BASE}/user/recycleBin`, params }),
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
  list: (params?: any) => defHttp.get({ url: `${BASE}/family/list`, params }),
  /** 家庭详情 */
  getById: (id: string) => defHttp.get({ url: `${BASE}/family/info` }),
  /** 新增家庭（管理端） */
  add: (data: any) => defHttp.post({ url: `${BASE}/family/add`, data }),
  /** 编辑家庭（管理端） */
  edit: (id: string, data: any) => defHttp.put({ url: `${BASE}/family/${id}`, data }),
  /** 创建家庭 */
  create: (data: any) => defHttp.post({ url: `${BASE}/family`, data }),
  /** 编辑家庭 */
  update: (data: any) => defHttp.put({ url: `${BASE}/family`, data }),
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
  recycleBin: (params?: any) => defHttp.get({ url: `${BASE}/family/recycleBin`, params }),
  /** 移入回收站 */
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/family/moveToRecycleBin`, data: ids }),
  /** 从回收站恢复 */
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/family/restore`, data: ids }),
  /** 彻底删除 */
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/family/deletePermanently`, data: ids }),
  /** 家庭成员列表（管理端） */
  adminMembers: (familyId: string) => defHttp.get({ url: `${BASE}/family/admin/members`, params: { familyId } }),
  /** 添加家庭成员（管理端） */
  adminAddMember: (data: any) => defHttp.post({ url: `${BASE}/family/admin/members`, data }),
  /** 移除家庭成员（管理端） */
  adminRemoveMember: (id: string) => defHttp.delete({ url: `${BASE}/family/admin/member/${id}` }),
  /** 修改成员角色（管理端） */
  adminUpdateRole: (id: string, role: string) =>
    defHttp.put({ url: `${BASE}/family/admin/member/${id}/role`, data: { role } }),
};

/** 账单管理 API */
export const billApi = {
  list: (params?: any) => defHttp.get({ url: `${BASE}/bill/list`, params }),
  add: (data: any) => defHttp.post({ url: `${BASE}/bill/add`, data }),
  edit: (id: string, data: any) => defHttp.put({ url: `${BASE}/bill/${id}`, data }),
  exportXls: `${BASE}/bill/exportXls`,
  importExcel: `${BASE}/bill/importExcel`,
  recycleBin: (params?: any) => defHttp.get({ url: `${BASE}/bill/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/bill/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/bill/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/bill/deletePermanently`, data: ids }),
};

/** 计划管理 API */
export const planApi = {
  list: (params?: any) => defHttp.get({ url: `${BASE}/plan/list`, params }),
  add: (data: any) => defHttp.post({ url: `${BASE}/plan/add`, data }),
  edit: (id: string, data: any) => defHttp.put({ url: `${BASE}/plan/${id}`, data }),
  exportXls: `${BASE}/plan/exportXls`,
  importExcel: `${BASE}/plan/importExcel`,
  recycleBin: (params?: any) => defHttp.get({ url: `${BASE}/plan/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/plan/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/plan/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/plan/deletePermanently`, data: ids }),
};

/** 菜谱管理 API */
export const recipeApi = {
  list: (params?: any) => defHttp.get({ url: `${BASE}/recipe/list`, params }),
  add: (data: any) => defHttp.post({ url: `${BASE}/recipe/add`, data }),
  edit: (id: string, data: any) => defHttp.put({ url: `${BASE}/recipe/${id}`, data }),
  exportXls: `${BASE}/recipe/exportXls`,
  importExcel: `${BASE}/recipe/importExcel`,
  recycleBin: (params?: any) => defHttp.get({ url: `${BASE}/recipe/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/recipe/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/recipe/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/recipe/deletePermanently`, data: ids }),
  /** 上传菜谱视频 */
  uploadVideo: (id: string) => `${BASE}/recipe/${id}/video`,
  /** 删除菜谱视频 */
  deleteVideo: (id: string) => defHttp.delete({ url: `${BASE}/recipe/${id}/video` }),
};

/** 学习资料 API */
export const learnApi = {
  list: (params?: any) => defHttp.get({ url: `${BASE}/learn/materials`, params }),
  add: (data: any) => defHttp.post({ url: `${BASE}/learn/addMaterial`, data }),
  edit: (id: string, data: any) => defHttp.put({ url: `${BASE}/learn/material/${id}`, data }),
  exportXls: `${BASE}/learn/exportXls`,
  importExcel: `${BASE}/learn/importExcel`,
  recycleBin: (params?: any) => defHttp.get({ url: `${BASE}/learn/recycleBin`, params }),
  moveToRecycleBin: (ids: string[]) => defHttp.put({ url: `${BASE}/learn/moveToRecycleBin`, data: ids }),
  restore: (ids: string[]) => defHttp.put({ url: `${BASE}/learn/restore`, data: ids }),
  deletePermanently: (ids: string[]) => defHttp.delete({ url: `${BASE}/learn/deletePermanently`, data: ids }),
  /** 上传学习资料文件 */
  uploadFile: (id: string) => `${BASE}/learn/materials/${id}/upload`,
};

/** 资料存储管理 API */
export const storageApi = {
  fileList: (params?: any) => defHttp.get({ url: `${BASE}/storage/file-list`, params }),
  folderList: (params?: any) => defHttp.get({ url: `${BASE}/storage/folder-list`, params }),
  folderTree: (params?: any) => defHttp.get({ url: `${BASE}/storage/folders`, params }),
  createFolder: (data: any) => defHttp.post({ url: `${BASE}/storage/folders`, params: data }, { joinParamsToUrl: true }),
  /** 修改文件夹可见性 */
  updateFolderVisibility: (id: string, visibility: string) => defHttp.patch({ url: `${BASE}/storage/folders/${id}/visibility`, params: { visibility } }, { joinParamsToUrl: true }),
  deleteFile: (id: string) => defHttp.delete({ url: `${BASE}/storage/files/${id}` }),
  deleteFolder: (id: string) => defHttp.delete({ url: `${BASE}/storage/folders/${id}` }),
};

//update-begin---author:admin ---date:2026-07-31  for：AI管理API集中定义-----------
/** AI 对话管理 API */
export const conversationApi = {
  /** 管理端对话列表（分页） */
  list: (params?: any) => defHttp.get({ url: `${BASE}/ai/conversations/list`, params }),
  /** 对话详情 */
  getById: (id: string) => defHttp.get({ url: `${BASE}/ai/conversations/${id}` }),
  /** 获取对话消息列表 */
  getMessages: (id: string) => defHttp.get({ url: `${BASE}/ai/conversations/${id}/messages` }),
  /** 重命名对话 */
  rename: (id: string, title: string) => defHttp.put({ url: `${BASE}/ai/conversations/${id}/rename`, params: { title } }, { joinParamsToUrl: true }),
  /** 删除对话 */
  delete: (id: string) => defHttp.delete({ url: `${BASE}/ai/conversations/${id}` }),
};

/** AI 对话（SSE流式）API */
export const chatApi = {
  /** Token配额检查 */
  checkQuota: (params?: any) => defHttp.get({ url: `${BASE}/ai/chat/quota`, params }),
  /** 停止生成 */
  stop: () => defHttp.post({ url: `${BASE}/ai/chat/stop` }),
  /** 上传对话附件文件 */
  uploadFile: `${BASE}/ai/chat/upload`,
};

/** AI 密钥配置 API */
export const keyConfigApi = {
  list: (params?: any) => defHttp.get({ url: `${BASE}/ai/key-config/list`, params }),
  add: (data: any) => defHttp.post({ url: `${BASE}/ai/key-config`, data }),
  edit: (data: any) => defHttp.put({ url: `${BASE}/ai/key-config`, data }),
  delete: (id: string) => defHttp.delete({ url: `${BASE}/ai/key-config/${id}` }),
  toggleStatus: (id: string, isEnabled?: string) =>
    defHttp.put({ url: `${BASE}/ai/key-config/${id}/status`, params: isEnabled ? { isEnabled } : {} }, { joinParamsToUrl: true }),
  setDefault: (id: string) => defHttp.put({ url: `${BASE}/ai/key-config/${id}/default` }),
};

/** AI Token额度 API */
export const quotaApi = {
  /** 获取默认配额配置 */
  getDefaultQuota: () => defHttp.get({ url: `${BASE}/ai/key-config/quota/default` }),
  /** 获取用户Token消耗统计（管理端分页） */
  logList: (params?: any) => defHttp.get({ url: `${BASE}/ai/key-config/quota/list`, params }),
};
//update-end---author:admin ---date:2026-07-31  for：AI管理API集中定义-----------
