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

/** 文件白名单项 */
export interface HomeaiFileWhitelistItem {
  id?: string;
  extension?: string;
  mimeType?: string;
  maxSize?: number;
  enabled?: number | string | boolean;
  [key: string]: unknown;
}

/** 计划配置 */
export interface HomeaiPlanConfig {
  rollForwardDays?: number;
  [key: string]: unknown;
}

/** 通用列表/表单载荷（宽松） */
export type HomeaiPayload = Record<string, unknown>;
