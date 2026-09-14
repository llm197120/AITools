import { describe, expect, it } from 'vitest'
import {
  decodeXmlEntities,
  escapeHtml,
  extractEmbedIds,
  extractSlideTexts,
  parseRelationshipTargets,
  resolveZipPath,
  slideFileIndex,
  stripScripts,
} from './officePreviewParse'

describe('officePreviewParse', () => {
  it('解析 zip 相对路径', () => {
    expect(resolveZipPath('ppt/slides', '../media/image1.png')).toBe('ppt/media/image1.png')
    expect(resolveZipPath('ppt/slides', '../../ppt/media/a.jpg')).toBe('ppt/media/a.jpg')
  })

  it('解析关系 Id 与 Target', () => {
    const xml = `<Relationships>
      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image1.png"/>
      <Relationship Target="../media/pic.jpg" Id="rId3" Type="http://x/image"/>
    </Relationships>`
    expect(parseRelationshipTargets(xml)).toEqual({
      rId2: '../media/image1.png',
      rId3: '../media/pic.jpg',
    })
  })

  it('抽取幻灯片文本与图片 embed', () => {
    const xml = `<p><a:t>标题</a:t><a:t xml:space="preserve"> 内容</a:t><a:blip r:embed="rId2"/></p>`
    expect(extractSlideTexts(xml)).toEqual(['标题', '内容'])
    expect(extractEmbedIds(xml)).toEqual(['rId2'])
  })

  it('转义与实体', () => {
    expect(escapeHtml('<a&b>')).toBe('&lt;a&amp;b&gt;')
    expect(decodeXmlEntities('A&amp;B')).toBe('A&B')
    expect(slideFileIndex('ppt/slides/slide12.xml')).toBe(12)
    expect(stripScripts('<p>a</p><script>x</script>')).toBe('<p>a</p>')
  })
})
