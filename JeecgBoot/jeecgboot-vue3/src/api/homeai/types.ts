/**
 * HomeAI 管理端常用接口类型
 * 字段以可选 + 索引签名扩展，避免与后端返回过度严格对齐
 */

/** 通用分页查询参数 */
export interface HomeaiPageParams {
  /** 页码 */
  pageNo?: number;
  /** 每页条数 */
  pageSize?: number;
  /** 关键词 / 名称等模糊筛选 */
  keyword?: string;
  /** 用户 ID */
  userId?: string;
  /** 家庭 ID */
  familyId?: string;
  /** 状态 */
  status?: string | number;
  /** 分类 */
  category?: string;
  /** 分类 ID */
  categoryId?: string;
  /** 标题 */
  title?: string;
  /** 名称 */
  name?: string;
  /** 日期 / 计划日期 */
  planDate?: string;
  /** 可见性 */
  visibility?: string;
  /** 文件夹 ID */
  folderId?: string;
  /** 其它筛选字段 */
  [key: string]: unknown;
}

/** 通用分页返回（MyBatis-Plus IPage 结构；部分接口直接返回数组） */
export interface HomeaiPageResult<T> {
  records?: T[];
  total?: number;
  current?: number;
  size?: number;
}

/** 微信用户 */
export interface HomeaiUser {
  id?: string;
  openid?: string;
  nickname?: string;
  avatarUrl?: string;
  phone?: string;
  familyId?: string;
  familyName?: string;
  familyRole?: string;
  familyRoleType?: string;
  status?: string | number;
  lastLoginTime?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** 家庭 */
export interface HomeaiFamily {
  id?: string;
  name?: string;
  creatorId?: string;
  memberCount?: number;
  status?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** 家庭成员（管理端） */
export interface HomeaiFamilyMember {
  id?: string;
  familyId?: string;
  userId?: string;
  role?: string;
  [key: string]: unknown;
}

/** 账单 */
export interface HomeaiBill {
  id?: string;
  familyId?: string;
  userId?: string;
  billDate?: string;
  type?: string;
  categoryId?: string;
  categoryName?: string;
  amount?: number | string;
  paymentMethod?: string;
  remark?: string;
  voucherUrl?: string;
  source?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** 计划（主计划 / 实例展示） */
export interface HomeaiPlan {
  id?: string;
  userId?: string;
  title?: string;
  content?: string;
  planDate?: string;
  startTime?: string;
  endTime?: string;
  isAllDay?: number;
  priority?: string;
  category?: string;
  recipeId?: string;
  recipeName?: string;
  remindMinutes?: number;
  isRepeatMaster?: number;
  repeatRule?: string;
  status?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** 计划 / 学习 / 菜谱等分类 */
export interface HomeaiCategory {
  id?: string;
  name?: string;
  icon?: string;
  color?: string;
  sortOrder?: number;
  isEnabled?: number;
  [key: string]: unknown;
}

/** 菜谱食材 */
export interface HomeaiRecipeIngredient {
  name?: string;
  /** 展示用量（前端编辑用） */
  amount?: string;
  quantity?: number | string;
  unit?: string;
  [key: string]: unknown;
}

/** 菜谱 */
export interface HomeaiRecipe {
  id?: string;
  familyId?: string;
  userId?: string;
  name?: string;
  categoryId?: string;
  coverUrl?: string;
  videoUrl?: string;
  difficulty?: number;
  cookTime?: number;
  servings?: number;
  ingredients?: string | HomeaiRecipeIngredient[];
  steps?: string;
  tips?: string;
  visibility?: string;
  viewCount?: number;
  favoriteCount?: number;
  auditStatus?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** 菜谱步骤 */
export interface HomeaiRecipeStep {
  id?: string;
  recipeId?: string;
  description?: string;
  imageUrl?: string;
  stepNum?: number;
  sortOrder?: number;
  [key: string]: unknown;
}

/** 菜谱详情（含子表；对应 GET /recipe/{id} 返回） */
export interface HomeaiRecipeDetail {
  recipe?: HomeaiRecipe;
  ingredients?: HomeaiRecipeIngredient[];
  steps?: HomeaiRecipeStep[];
  [key: string]: unknown;
}

/** 学习资料 */
export interface HomeaiLearnMaterial {
  id?: string;
  userId?: string;
  title?: string;
  type?: string;
  fileUrl?: string;
  coverUrl?: string;
  category?: string;
  categoryId?: string;
  tags?: string;
  description?: string;
  totalDuration?: number;
  createTime?: string;
  [key: string]: unknown;
}

/** 存储文件 */
export interface HomeaiStorageFile {
  id?: string;
  familyId?: string;
  userId?: string;
  folderId?: string;
  originalName?: string;
  storedName?: string;
  extension?: string;
  mimeType?: string;
  fileSize?: number;
  fileUrl?: string;
  thumbnailUrl?: string;
  visibility?: string;
  isFavorite?: string;
  downloadCount?: number;
  createTime?: string;
  [key: string]: unknown;
}

/** 存储文件夹树节点 */
export interface HomeaiStorageFolder {
  id?: string;
  name?: string;
  parentId?: string;
  userId?: string;
  visibility?: string;
  fileCount?: number;
  level?: number;
  deletedAt?: string;
  children?: HomeaiStorageFolder[];
  [key: string]: unknown;
}

/** 家庭配额看板行（/config/storage/families） */
export interface HomeaiFamilyQuotaItem {
  familyId?: string;
  familyName?: string;
  memberCount?: number;
  fileCount?: number;
  usedBytes?: number;
  limitBytes?: number;
  /** 是否家庭级覆盖配额 */
  custom?: boolean;
  warn?: boolean;
  [key: string]: unknown;
}

/** 文件白名单项（对齐后端 homeai_file_whitelist） */
export interface HomeaiFileWhitelistItem {
  id?: string;
  /** 扩展名，如 pdf/docx */
  extension?: string;
  /** 分类（doc/image/video/other） */
  category?: string;
  /** 排序 */
  sortOrder?: number;
  /** 是否启用（0/1，后端为 Integer） */
  isEnabled?: number;
  [key: string]: unknown;
}

/** 计划配置（Redis 配置 DTO） */
export interface HomeaiPlanConfig {
  repeatHorizonDays?: number;
  instanceCleanupDays?: number;
  remindEnabled?: boolean;
  aiDocPolishEnabled?: boolean;
  [key: string]: unknown;
}

/** 存储转换规则（homeai_convert_rule） */
export interface HomeaiConvertRule {
  id?: string;
  sourceFormat?: string;
  targetFormat?: string;
  /** 0/1 字符串 */
  isEnabled?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** Office 文档模板（homeai_office_template） */
export interface HomeaiOfficeTemplate {
  id?: string;
  name?: string;
  type?: string;
  fileUrl?: string;
  previewUrl?: string;
  /** 是否默认（0/1 字符串） */
  isDefault?: string;
  remark?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** 存储转换任务（homeai_storage_convert_task） */
export interface HomeaiConvertTask {
  id?: string;
  fileId?: string;
  userId?: string;
  convertType?: string;
  sourceFormat?: string;
  targetFormat?: string;
  instruction?: string;
  /** PENDING/PROCESSING/COMPLETED/FAILED */
  status?: string;
  resultFileUrl?: string;
  resultFileSize?: number;
  errorMessage?: string;
  taskDuration?: number;
  createTime?: string;
  [key: string]: unknown;
}

/** 操作审计日志（homeai_audit_log） */
export interface HomeaiAuditLog {
  id?: string;
  userId?: string;
  actionType?: string;
  module?: string;
  targetId?: string;
  targetSummary?: string;
  detail?: string;
  result?: string;
  ipAddress?: string;
  createBy?: string;
  createTime?: string;
  [key: string]: unknown;
}

/** AI 对话（管理端列表） */
export interface HomeaiConversation {
  id?: string;
  userId?: string;
  title?: string;
  messageCount?: number;
  createTime?: string;
  updateTime?: string;
  [key: string]: unknown;
}

/** AI 用户额度记录（管理端配额页） */
export interface HomeaiQuotaRecord {
  id?: string;
  userId?: string;
  nickname?: string;
  dailyLimit?: number;
  monthlyLimit?: number;
  dailyUsage?: number;
  monthlyUsage?: number;
  lastActiveTime?: string;
  effectiveEnd?: string;
  [key: string]: unknown;
}

/** 通用列表/表单载荷（宽松） */
export type HomeaiPayload = Record<string, unknown>;
