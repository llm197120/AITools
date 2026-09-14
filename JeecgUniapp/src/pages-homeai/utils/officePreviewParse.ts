/** PPTX 关系与路径解析（纯逻辑，便于单测） */

export function escapeHtml(text: string): string {
  return String(text)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

export function decodeXmlEntities(text: string): string {
  return String(text)
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, n) => String.fromCharCode(Number(n)))
}

export function resolveZipPath(fromDir: string, target: string): string {
  const raw = String(target || '').replace(/\\/g, '/')
  if (!raw) return ''
  if (raw.startsWith('/')) return raw.replace(/^\/+/, '')
  const base = String(fromDir || '').replace(/\\/g, '/').replace(/\/+$/, '')
  const parts = `${base}/${raw}`.split('/')
  const out: string[] = []
  for (const p of parts) {
    if (!p || p === '.') continue
    if (p === '..') out.pop()
    else out.push(p)
  }
  return out.join('/')
}

export function parseRelationshipTargets(relsXml: string): Record<string, string> {
  const map: Record<string, string> = {}
  const re = /<Relationship\b([^>]*)\/?>/gi
  let m: RegExpExecArray | null
  while ((m = re.exec(relsXml || ''))) {
    const attrs = m[1] || ''
    const id = /\bId="([^"]+)"/i.exec(attrs)?.[1]
    const target = /\bTarget="([^"]+)"/i.exec(attrs)?.[1]
    if (id && target) map[id] = target
  }
  return map
}

export function extractSlideTexts(slideXml: string): string[] {
  const texts: string[] = []
  const re = /<a:t(?:\s[^>]*)?>([^<]*)<\/a:t>/g
  let m: RegExpExecArray | null
  while ((m = re.exec(slideXml || ''))) {
    const t = decodeXmlEntities(m[1] || '').trim()
    if (t) texts.push(t)
  }
  return texts
}

export function extractEmbedIds(slideXml: string): string[] {
  const ids: string[] = []
  const re = /r:embed="([^"]+)"/g
  let m: RegExpExecArray | null
  while ((m = re.exec(slideXml || ''))) {
    if (m[1] && !ids.includes(m[1])) ids.push(m[1])
  }
  return ids
}

export function slideFileIndex(path: string): number {
  const m = String(path).match(/slide(\d+)\.xml$/i)
  return m ? Number(m[1]) : 0
}

export function guessMediaMime(path: string): string {
  const ext = path.split('?')[0].split('.').pop()?.toLowerCase() || ''
  if (ext === 'png') return 'image/png'
  if (ext === 'jpg' || ext === 'jpeg') return 'image/jpeg'
  if (ext === 'gif') return 'image/gif'
  if (ext === 'webp') return 'image/webp'
  if (ext === 'emf' || ext === 'wmf') return ''
  return 'image/png'
}

export function stripScripts(html: string): string {
  return String(html || '').replace(/<script\b[^<]*(?:(?!<\/script>)<[^<]*)*<\/script>/gi, '')
}
