// -*- coding: utf-8 -*-
import { onShow } from '@dcloudio/uni-app'
import { ensureLoginForAction } from './homeaiAuth'

/**
 * 子包列表页进入时校验登录；未登录跳转个人中心。
 * 复用 homeaiAuth.ensureLoginForAction，避免各页重复写鉴权逻辑。
 */
export function useHomeaiPageGuard() {
  onShow(() => {
    ensureLoginForAction()
  })
}
