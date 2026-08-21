/** 菜谱封面导入：按文件名/父目录归并，与后端 RecipeCoverMatch 对齐 */

const IMAGE_EXTS = new Set(['jpg', 'jpeg', 'png', 'gif', 'webp', 'bmp']);
const GENERIC_PARENTS = new Set([
  'images',
  'img',
  'imgs',
  'image',
  'pictures',
  'pics',
  'pic',
  'photo',
  'photos',
  'covers',
  'cover',
  'assets',
  'static',
  'upload',
  'uploads',
  'dishes',
  'dish-images',
  '图片',
  '封面',
  '菜谱',
  '菜谱图',
  '菜谱图片',
  '图库',
]);
const GENERIC_BASENAMES = new Set([
  'cover',
  '封面',
  '主图',
  'index',
  'poster',
  'thumb',
  'thumbnail',
  'default',
  'image',
  'img',
  'photo',
  'pic',
  '0',
  '1',
  '01',
]);

export type CoverPickFile = File & { webkitRelativePath?: string };

export function fileDisplayPath(file: CoverPickFile): string {
  const rel = (file.webkitRelativePath || '').replace(/\\/g, '/').replace(/^\.?\//, '');
  return rel || file.name;
}

export function toUploadFile(file: CoverPickFile): File {
  const name = fileDisplayPath(file);
  if (name === file.name) return file;
  return new File([file], name, { type: file.type, lastModified: file.lastModified });
}

function normalize(name: string): string {
  return (name || '').replace(/\u3000/g, ' ').trim().replace(/\s+/g, ' ').toLowerCase();
}

function originalPath(name: string): string {
  let path = (name || '').replace(/\\/g, '/').trim();
  while (path.startsWith('./')) path = path.slice(2);
  while (path.startsWith('/')) path = path.slice(1);
  while (path.endsWith('/')) path = path.slice(0, -1);
  return path;
}

function isIgnoredPath(name: string): boolean {
  const path = originalPath(name);
  if (!path) return true;
  return path.split('/').some((p) => p.startsWith('.') || p.toLowerCase() === '__macosx') || path.includes('..');
}

function extension(name: string): string {
  const path = originalPath(name);
  const file = path.includes('/') ? path.slice(path.lastIndexOf('/') + 1) : path;
  const dot = file.lastIndexOf('.');
  if (dot < 0 || dot === file.length - 1) return '';
  return file
    .slice(dot + 1)
    .replace(/[^a-zA-Z0-9]/g, '')
    .toLowerCase();
}

export function isImageFile(name: string): boolean {
  return IMAGE_EXTS.has(extension(name));
}

export function isZipFile(name: string): boolean {
  return extension(name) === 'zip';
}

function basenameNoExt(name: string): string {
  const path = originalPath(name);
  const file = path.includes('/') ? path.slice(path.lastIndexOf('/') + 1) : path;
  const dot = file.lastIndexOf('.');
  return dot < 0 ? file : file.slice(0, dot);
}

function parentName(name: string): string {
  const path = originalPath(name);
  const slash = path.lastIndexOf('/');
  if (slash <= 0) return '';
  const parentPath = path.slice(0, slash);
  const prev = parentPath.lastIndexOf('/');
  return prev < 0 ? parentPath : parentPath.slice(prev + 1);
}

function candidateKey(name: string): string {
  const base = basenameNoExt(name);
  const parent = parentName(name);
  if (!GENERIC_BASENAMES.has(normalize(base)) && base) return base;
  if (parent && !GENERIC_PARENTS.has(normalize(parent))) return parent;
  return base || parent;
}

function coverScore(name: string, recipeName: string): number {
  const path = originalPath(name);
  const depth = (path.match(/\//g) || []).length;
  let score = 10 - depth;
  const base = normalize(basenameNoExt(name));
  if (base === normalize(recipeName)) score += 100;
  else if (GENERIC_BASENAMES.has(base)) score += 40;
  return score;
}

export interface CoverImportItem {
  key: string;
  file: CoverPickFile;
  displayName: string;
}

/** 同一菜谱只保留最像封面的一张；zip 原样保留（后端展开） */
export function pickCoverUploads(files: CoverPickFile[]): CoverImportItem[] {
  const zips: CoverImportItem[] = [];
  const best = new Map<string, { item: CoverImportItem; score: number }>();
  for (const file of files) {
    const path = fileDisplayPath(file);
    if (isZipFile(path) || isZipFile(file.name)) {
      zips.push({ key: path, file, displayName: path });
      continue;
    }
    if (isIgnoredPath(path) || !isImageFile(path)) continue;
    const key = candidateKey(path);
    if (!key) continue;
    const score = coverScore(path, key);
    const prev = best.get(normalize(key));
    if (!prev || score > prev.score) {
      best.set(normalize(key), { item: { key, file, displayName: path }, score });
    }
  }
  return [...zips, ...[...best.values()].map((v) => v.item)];
}
