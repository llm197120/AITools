/**
 * HomeAI 平台环境地址适配层
 *
 * - 微信小程序：按小程序环境版本（develop/trial/release）切换 VITE_SERVER_BASEURL__WEIXIN_*
 * - Android APP：直接返回 VITE_SERVER_BASEURL / VITE_UPLOAD_BASEURL（构建时固定，见 env/.env.production）
 *   备案前内测为局域网 IP（如 http://192.168.1.100:8080/jeecg-boot），备案完成后替换为正式 HTTPS 域名重新构建即可
 */
import { getEnvBaseUrl, getEnvBaseUploadUrl } from '@/utils/index'

/** 获取服务器 API 基础地址（转发 utils/index.ts 的 getEnvBaseUrl） */
export function getServerBaseUrl(): string {
  return getEnvBaseUrl()
}

/** 获取上传基础地址（转发 utils/index.ts 的 getEnvBaseUploadUrl） */
export function getUploadBaseUrl(): string {
  return getEnvBaseUploadUrl()
}