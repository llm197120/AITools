/**
 * HomeAI 平台文件选择适配器：
 * Capacitor 原生走 ACTION_OPEN_DOCUMENT（系统文件管理），避免 WebView 只出相册视频；
 * 小程序走 chooseMessageFile；APP-PLUS 走 chooseFile；浏览器 H5 走 `<input type=file>`。
 * 与 useHomeaiFilePick 的 pickFiles 结果形状保持一致（path/name/size）
 */
import { pickCapacitorDocuments } from './capFilePick'
import { buildAccept, resolveAppChooseType } from './fileAccept'
import { isCapacitorNative } from './runtime'

export interface PickedDocument {
  path: string
  name?: string
  size?: number
  mimeType?: string
}

export interface PickDocumentOptions {
  /** 可选文件数量（APP 端 chooseFile 仅支持单文件，恒为 1） */
  count?: number
  /** 扩展名白名单，如 ['pdf', 'doc', 'docx', 'txt'] */
  extension?: string[]
  /** 文件类型：all / file / video / image */
  type?: 'all' | 'file' | 'video' | 'image'
}

/**
 * 选择文档/通用文件
 * 成功返回文件数组，用户取消或平台不支持时 resolve([])
 */
export function pickDocument(opts?: PickDocumentOptions): Promise<PickedDocument[]> {
  if (isCapacitorNative()) {
    return pickCapacitorDocuments(opts).catch((err) => {
      const msg = String((err && (err as any).message) || err || '')
      if (/not implemented|UNIMPLEMENTED|NOT_CAPACITOR/i.test(msg)) {
        return pickDocumentWebOrUni(opts)
      }
      uni.showToast({ title: msg.replace(/^选择失败:\s*/, '') || '选择失败', icon: 'none' })
      return []
    })
  }
  return pickDocumentWebOrUni(opts)
}

function pickDocumentWebOrUni(opts?: PickDocumentOptions): Promise<PickedDocument[]> {
  const count = opts?.count ?? 1
  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    // 微信：extension 仅 type=file 时生效；音频/指定扩展名时强制 file
    const mpType = opts?.extension?.length ? 'file' : opts?.type || 'all'
    uni.chooseMessageFile({
      count,
      type: mpType,
      extension: opts?.extension,
      success: (r) => {
        const files: PickedDocument[] = (r.tempFiles || []).map((f: any) => ({
          path: f.path,
          name: f.name,
          size: f.size,
        }))
        resolve(files)
      },
      fail: () => resolve([]),
    })
    // #endif

    // #ifdef APP-PLUS
    // Android type=file 通常只出文档，音频需 type=all；extension 带点更稳
    try {
      const appType = resolveAppChooseType(opts?.type, opts?.extension)
      const extension = opts?.extension?.map((e) => (e.startsWith('.') ? e : `.${e.replace(/^\./, '')}`))
      uni.chooseFile({
        count: 1,
        type: appType,
        extension,
        success: (r) => {
          const file: any = r.tempFiles?.[0]
          resolve(
            file
              ? [
                  {
                    path: file.path,
                    name: file.name,
                    size: file.size,
                    mimeType: file.type,
                  },
                ]
              : [],
          )
        },
        fail: () => resolve([]),
      })
    } catch (e) {
      // chooseFile 不可用（如 HBuilderX 基础库版本过低），按取消处理
      resolve([])
    }
    // #endif

    // #ifdef H5
    const input = document.createElement('input')
    input.type = 'file'
    input.multiple = count > 1
    // Capacitor 兜底：video/* 会被 WebView 收成相册；用 */* 尽量出系统文件选择
    input.accept = isCapacitorNative() ? '*/*' : buildAccept({ extension: opts?.extension, type: opts?.type })
    input.style.position = 'fixed'
    input.style.left = '-9999px'
    const done = (files: PickedDocument[]) => {
      input.remove()
      resolve(files)
    }
    input.addEventListener('cancel', () => done([]))
    input.onchange = () => {
      const list = Array.from(input.files || []).slice(0, count)
      done(
        list.map((f) => ({
          path: URL.createObjectURL(f),
          name: f.name,
          size: f.size,
          mimeType: f.type,
        })),
      )
    }
    document.body.appendChild(input)
    input.click()
    // #endif
  })
}
