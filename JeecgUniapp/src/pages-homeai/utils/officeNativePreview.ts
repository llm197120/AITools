/**
 * APP / H5 页内 Office 预览（打进包，不走 CDN）
 * Word：docx-preview（vue-office 同款引擎）
 * Excel：SheetJS
 * PPTX：JSZip 抽文本与图片
 */
import {
  escapeHtml,
  extractEmbedIds,
  extractSlideTexts,
  guessMediaMime,
  parseRelationshipTargets,
  resolveZipPath,
  slideFileIndex,
  stripScripts,
} from './officePreviewParse'

export type OfficeKind = 'word' | 'excel' | 'ppt'

export type OfficePreviewHandle = {
  kind: OfficeKind
  sheets: string[]
  pptTotal: number
  render: (host: HTMLElement, opts?: { sheetIndex?: number; pptPage?: number }) => Promise<void>
}

function normalizeExt(ext: string): string {
  return String(ext || '').replace(/^\./, '').toLowerCase()
}

export function officePreviewKind(ext: string): OfficeKind | '' {
  const e = normalizeExt(ext)
  if (e === 'docx' || e === 'doc') return 'word'
  if (e === 'xlsx' || e === 'xls') return 'excel'
  if (e === 'pptx' || e === 'ppt') return 'ppt'
  return ''
}

export function officeNeedsExternalApp(ext: string): boolean {
  const e = normalizeExt(ext)
  return e === 'doc' || e === 'ppt'
}

function setHostHtml(host: HTMLElement, html: string) {
  host.innerHTML = stripScripts(html)
}

async function buildPptSlides(data: ArrayBuffer): Promise<string[]> {
  const JSZip = (await import('jszip')).default
  const zip = await JSZip.loadAsync(data)
  const names = Object.keys(zip.files)
    .filter((n) => /^ppt\/slides\/slide\d+\.xml$/i.test(n))
    .sort((a, b) => slideFileIndex(a) - slideFileIndex(b))
  if (!names.length) throw new Error('未找到幻灯片内容')
  const slides: string[] = []
  for (const name of names) {
    const xml: string = await zip.file(name)!.async('string')
    const relPath = name.replace(/^(ppt\/slides\/)(slide\d+\.xml)$/i, 'ppt/slides/_rels/$2.rels')
    const relFile = zip.file(relPath)
    const rels = parseRelationshipTargets(relFile ? await relFile.async('string') : '')
    const texts = extractSlideTexts(xml)
    const body = texts.map((t) => `<p>${escapeHtml(t)}</p>`).join('')
    const imgs: string[] = []
    for (const id of extractEmbedIds(xml)) {
      const target = rels[id]
      if (!target) continue
      const mediaPath = resolveZipPath('ppt/slides', target)
      const mime = guessMediaMime(mediaPath)
      if (!mime) continue
      const file = zip.file(mediaPath)
      if (!file) continue
      const b64: string = await file.async('base64')
      imgs.push(`<img alt="" src="data:${mime};base64,${b64}" />`)
    }
    const inner = body + imgs.join('')
    slides.push(`<div class="ppt-slide">${inner || '<p>（此页无可展示文本）</p>'}</div>`)
  }
  return slides
}

export async function prepareOfficePreview(ext: string, data: ArrayBuffer): Promise<OfficePreviewHandle> {
  const e = normalizeExt(ext)
  if (e === 'doc' || e === 'ppt') {
    throw new Error('旧版 .doc / .ppt 无法在 App 内预览，请用其他应用打开')
  }
  if (typeof document === 'undefined') {
    throw new Error('当前平台不支持页内文档预览')
  }
  if (e === 'docx') {
    const { renderAsync } = await import('docx-preview')
    return {
      kind: 'word',
      sheets: [],
      pptTotal: 1,
      render: async (host) => {
        host.innerHTML = ''
        await renderAsync(data, host, undefined, {
          className: 'homeai-docx',
          inWrapper: true,
          ignoreWidth: true,
          breakPages: true,
          useBase64URL: true,
        })
      },
    }
  }
  if (e === 'xlsx' || e === 'xls') {
    const XLSX = await import('xlsx')
    const wb = XLSX.read(new Uint8Array(data), { type: 'array' })
    const sheets: string[] = wb.SheetNames || []
    if (!sheets.length) throw new Error('工作簿是空的')
    return {
      kind: 'excel',
      sheets,
      pptTotal: 1,
      render: async (host, opts) => {
        const idx = Math.min(Math.max(0, opts?.sheetIndex || 0), sheets.length - 1)
        const ws = wb.Sheets[sheets[idx]]
        const table = XLSX.utils.sheet_to_html(ws, { header: '', footer: '' })
        setHostHtml(host, `<div class="excel-sheet">${table || '<p>（此表无数据）</p>'}</div>`)
      },
    }
  }
  if (e === 'pptx') {
    const slides = await buildPptSlides(data)
    return {
      kind: 'ppt',
      sheets: [],
      pptTotal: slides.length,
      render: async (host, opts) => {
        const page = Math.min(Math.max(1, opts?.pptPage || 1), slides.length)
        setHostHtml(host, slides[page - 1] || '')
      },
    }
  }
  throw new Error('不支持的文档格式')
}
