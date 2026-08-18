<script lang="ts">
import { onLaunch, onShow, onHide, onLoad, onReady } from '@dcloudio/uni-app'
import { useUserStore } from '@/pages-homeai/stores/user'
import { HOMEAI_PROFILE_TAB } from '@/pages-homeai/utils/homeaiAuth'
import 'abortcontroller-polyfill/dist/abortcontroller-polyfill-only'
 // #ifdef APP-PLUS
import appUpdate from "@/common/appUpdate";
import { initLocalNotify } from '@/pages-homeai/utils/push'
// #endif
export default {
  onLaunch: function (options) {
    console.log('App Launch')
    console.log('应用启动路径：', options.path)
    const userStore = useUserStore()
    if (!userStore.isLogin) {
      uni.switchTab({ url: HOMEAI_PROFILE_TAB })
    }
    // #ifdef APP-PLUS
    // 检测升级
    appUpdate()
    // 隐私合规：首启隐私弹窗（仅 App 端；小程序端走微信平台隐私能力）
    const privacyAgreed = uni.getStorageSync('homeai_privacy_agreed')
    if (!privacyAgreed) {
      uni.showModal({
        title: '隐私保护指引',
        content: '首次使用前请阅读《用户协议》与《隐私政策》，同意后方可使用本应用。',
        confirmText: '同意并继续',
        cancelText: '暂不同意',
        success: (res) => {
          if (res.confirm) {
            uni.setStorageSync('homeai_privacy_agreed', true)
            // 完整内容可在「我的-隐私协议」中查看
            uni.showToast({ title: '可在「我的-隐私协议」查看完整内容', icon: 'none' })
          }
          // 取消/关闭不阻塞业务：仅记录未同意，下次启动再次提示
        },
      })
    }
    // 注册本地通知点击监听（计划提醒兜底方案，替代 EMAS 推送）
    initLocalNotify()
    // #endif
    // 家庭AI小工具：启动后直接进入家庭AI首页（见 pages/launch/launch.vue），
    // 使用微信登录（uni.login -> code2Session），无需 JEECG 账号密码登录。
    // 如需恢复 JEECG 登录门禁，取消下面代码注释：
    // const userStore = useUserStore()
    // if (!userStore.isLogined) {
    //   uni.reLaunch({ url: '/pages/login/login' })
    // }
  },
  onShow: function (options) {
    console.log('App Show')
    console.log('应用启动路径：', options.path)
    // 家庭AI小工具：不再强制 JEECG 登录
  },
  onHide: function () {
    console.log('App Hide')
  },
  // 全局变量
  globalData: {
    isLocalConfig: false,
    systemInfo: uni.getSystemInfoSync(),
    // 导航的高度
    navHeight: 44,
  },
}
</script>

<style lang="scss">
:root,
page {
  font-size: 14px;
  color: #333333;
  font-family:
    Helvetica Neue,
    Helvetica,
    sans-serif;
}
uni-page-body {
  height: 100%;
  & > uni-view {
    height: 100%;
  }
}
.shadow-warp {
  position: relative;
  box-shadow: 0 0 5px rgba(168, 92, 92, 0.1);
}

/* stylelint-disable selector-type-no-unknown */
button::after {
  border: none;
}

swiper,
scroll-view {
  flex: 1;
  height: 100%;
  overflow: hidden;
}

image {
  width: 100%;
  height: 100%;
  vertical-align: middle;
}

// 单行省略，优先使用 unocss: text-ellipsis
.ellipsis {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

// 两行省略
.ellipsis-2 {
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  word-break: break-all;
}

// 三行省略
.ellipsis-3 {
  display: -webkit-box;
  overflow: hidden;
  text-overflow: ellipsis;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
</style>
