<script lang="ts">
import { onLaunch, onShow, onHide, onLoad, onReady } from '@dcloudio/uni-app'
import 'abortcontroller-polyfill/dist/abortcontroller-polyfill-only'
import { initLocalNotify } from '@/pages-homeai/utils/push'
import { initStandaloneShell } from '@/pages-homeai/platform/runtime'
import { initConnectionMonitor, pokeConnection } from '@/pages-homeai/offline/conn'
import { initSyncLoop, setSyncConfig } from '@/pages-homeai/offline/syncQueue'
import { registerAllSenders } from '@/pages-homeai/offline/senders'
import { initPendingUploadFlush } from '@/pages-homeai/offline/pendingUpload'
import { getServerBaseUrl } from '@/pages-homeai/api/request'

/** 启动拉取后端同步配置（batchSize/intervalMs/maxRetries/imageCacheLimitMB） */
function loadSyncConfig() {
  uni.request({
    url: `${getServerBaseUrl()}/homeai/config/sync`,
    method: 'GET',
    timeout: 8000,
    success: (res: any) => {
      const d = res.data
      if (d?.success && d.result) {
        const cfg = d.result
        setSyncConfig({
          batchSize: Number(cfg.batchSize) || 1,
          intervalMs: Number(cfg.intervalMs) || 5000,
          maxRetriesPerItemPerDay: Number(cfg.maxRetriesPerDay) || 20,
          imageCacheLimitMB: Number(cfg.imageCacheLimitMb) || 4096,
        })
        try {
          uni.setStorageSync('homeai_sync_config', {
            imageCacheLimitMB: Number(cfg.imageCacheLimitMb) || 4096,
          })
        } catch {
          /* ignore */
        }
      }
    },
  })
}
export default {
  onLaunch: function (options) {
    console.log('App Launch')
    console.log('应用启动路径：', options.path)
    // 未登录分流与隐私同意改由 pages/launch 处理，避免首页先闪再跳转
    try {
      initLocalNotify()
    } catch (e) {
      console.warn('本地通知初始化失败（云打包缺 Push 或 Capacitor 插件未同步）', e)
    }
    initStandaloneShell()
    // 离线能力：连接监控 + 缓慢同步引擎 + 各模块同步执行器 + 离线文件补传 + 同步配置
    initConnectionMonitor()
    registerAllSenders()
    initSyncLoop()
    initPendingUploadFlush()
    loadSyncConfig()
  },
  onShow: function (options) {
    console.log('App Show')
    console.log('应用启动路径：', options.path)
    // 回到前台立即重探连接（离线恢复后触发同步）
    pokeConnection()
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
