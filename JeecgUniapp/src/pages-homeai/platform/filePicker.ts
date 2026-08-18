/**
 * HomeAI 平台文件选择适配器：小程序走 chooseMessageFile，APP 走 chooseFile
 * 与 useHomeaiFilePick 的 pickFiles 结果形状保持一致（path/name/size）
 */

export interface PickedDocument {
  path: string
  name?: string
  size?: number
}

export interface PickDocumentOptions {
  /** 可选文件数量（APP 端 chooseFile 仅支持单文件，恒为 1） */
  count?: number
  /** 扩展名白名单，如 ['pdf', 'doc', 'docx', 'txt'] */
  extension?: string[]
  /** 文件类型：all / file / video / image（仅小程序 chooseMessageFile 生效） */
  type?: 'all' | 'file' | 'video' | 'image'
}

/**
 * 选择文档/通用文件
 * 成功返回文件数组，用户取消或平台不支持时 resolve([])
 */
export function pickDocument(opts?: PickDocumentOptions): Promise<PickedDocument[]> {
  const count = opts?.count ?? 1
  return new Promise((resolve) => {
    // #ifdef MP-WEIXIN
    // 微信小程序：chooseMessageFile 从聊天记录/本地选择文件
    uni.chooseMessageFile({
      count,
      type: opts?.type || 'all',
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
    // APP 端：chooseFile 支持选择文档；部分平台/低版本可能不可用，兜底返回空
    try {
      uni.chooseFile({
        count: 1,
        extension: opts?.extension,
        success: (r) => {
          const file: any = r.tempFiles?.[0]
          resolve(
            file
              ? [
                  {
                    path: file.path,
                    name: file.name,
                    size: file.size,
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
  })
}
