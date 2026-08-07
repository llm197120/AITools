/** 文件夹树工具（对应后端 MAX_FOLDER_DEPTH=5，level 0~4） */
export const MAX_FOLDER_DEPTH = 5

export interface StorageFolderNode {
  id: string
  name: string
  parentId?: string | null
  userId?: string
  visibility?: string
  familyIds?: string[]
  level?: number
  fileCount?: number
  children?: StorageFolderNode[]
}

export function findFolderNode(tree: StorageFolderNode[], id: string | null): StorageFolderNode | null {
  if (!id) return null
  for (const node of tree) {
    if (node.id === id) return node
    if (node.children?.length) {
      const found = findFolderNode(node.children, id)
      if (found) return found
    }
  }
  return null
}

/** 获取某层级的子文件夹（parentId 为空则返回根节点） */
export function getChildFolders(tree: StorageFolderNode[], parentId: string | null): StorageFolderNode[] {
  if (!parentId) return tree
  const parent = findFolderNode(tree, parentId)
  return parent?.children || []
}

/** 构建面包屑路径 */
export function buildBreadcrumb(
  tree: StorageFolderNode[],
  folderId: string | null,
): { id: string | null; name: string }[] {
  const crumbs: { id: string | null; name: string }[] = [{ id: null, name: '我的资料' }]
  if (!folderId) return crumbs

  const path: StorageFolderNode[] = []
  function dfs(nodes: StorageFolderNode[], target: string): boolean {
    for (const n of nodes) {
      path.push(n)
      if (n.id === target) return true
      if (n.children?.length && dfs(n.children, target)) return true
      path.pop()
    }
    return false
  }
  if (dfs(tree, folderId)) {
    path.forEach((n) => crumbs.push({ id: n.id, name: n.name }))
  }
  return crumbs
}

/** 是否可在该文件夹下新建子文件夹（后端 level 0~3 可建，level 4 不可） */
export function canCreateSubFolder(folder: StorageFolderNode | null): boolean {
  if (!folder) return true
  const level = folder.level ?? 0
  return level < MAX_FOLDER_DEPTH - 1
}

export function isFolderOwner(folder: StorageFolderNode, userId?: string): boolean {
  return !!userId && folder.userId === userId
}

export function isFileOwner(file: { userId?: string }, userId?: string): boolean {
  return !!userId && file.userId === userId
}
