<route lang="json5">
{
  layout: 'default',
  style: {
    navigationStyle: 'custom',
    navigationBarTitleText: '家庭AI小工具',
    enablePullDownRefresh: true,
  },
}
</route>

<template>
  <view class="homeai-page" :style="{ paddingTop: statusBarPx + 'px' }">
    <template v-if="booting">
      <view class="boot-skeleton">
        <view class="sk-block sk-hero"></view>
        <HomeSkeleton variant="card" />
        <HomeSkeleton variant="list" :rows="2" />
      </view>
    </template>
    <template v-else>
    <!-- 问候区 -->
    <view class="header" :style="{ paddingRight: headerRightPx + 'px' }">
      <view class="header-main">
        <text class="greeting">今天想做什么？</text>
        <view class="family-row" @click="onFamilyClick">
          <wd-icon name="location" size="14px" color="#8A857C"></wd-icon>
          <text class="family-label">{{ familyLabel }}</text>
          <wd-icon name="arrow-down" size="12px" color="#8A857C"></wd-icon>
        </view>
      </view>
      <view class="plan-btn hai-press" @click="goModule('plan')">
        <wd-icon name="calendar" size="20px" color="#3A342C"></wd-icon>
        <view v-if="todayTodo > 0" class="notify-dot"></view>
      </view>
    </view>

    <!-- AI 引导 Hero -->
    <view class="hero-card" @click="goModule('ai')">
      <view class="hero-body">
        <text class="hero-title">和家庭 AI 聊聊</text>
        <text class="hero-desc">问答、整理资料、规划日常，一句话开始</text>
        <view class="hero-cta">
          <text class="hero-cta-text">开始对话</text>
          <wd-icon name="arrow-right" size="12px" color="#fff"></wd-icon>
        </view>
      </view>
      <view class="hero-decor"></view>
    </view>

    <!-- 六宫格快捷入口 -->
    <view class="quick-card">
      <view
        class="quick-item hai-press"
        v-for="item in quickEntries"
        :key="item.key"
        @click="goModule(item.key)"
      >
        <view class="quick-icon">
          <wd-icon :name="item.icon" size="22px" color="#3A342C"></wd-icon>
        </view>
        <text class="quick-title">{{ item.label }}</text>
        <text class="quick-sub">{{ item.sub }}</text>
      </view>
    </view>

    <!-- 今日计划 -->
    <view class="section">
      <view class="section-head">
        <text class="section-title">今日计划</text>
        <view class="section-link" @click="goModule('plan')">
          <text>查看日历</text>
          <wd-icon name="arrow-right" size="12px" color="#8A857C"></wd-icon>
        </view>
      </view>
      <view class="plan-card" @click="onPlanCardClick">
        <view class="plan-icon-wrap">
          <wd-icon name="clock" size="28px" color="#1B4F8A"></wd-icon>
        </view>
        <view class="plan-info">
          <text class="plan-title">今日待办安排</text>
          <text class="plan-desc" v-if="planLoadFailed">计划加载失败，点此重试</text>
          <text class="plan-desc" v-else-if="todayTodo > 0">今日有 {{ todayTodo }} 项待办，点击查看</text>
          <text class="plan-desc" v-else>暂无待办，安排一条计划开始今天</text>
        </view>
        <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
      </view>
    </view>

    <!-- 双列推荐 -->
    <view class="dual-row">
      <view class="dual-card dual-recipe" @click="goTodayCook">
        <text class="dual-tag tag-warm">今日下厨</text>
        <text class="dual-title">{{ todayCookTitle }}</text>
        <text class="dual-desc">{{ todayCookDesc }}</text>
        <view class="dual-cta cta-warm">
          <text>{{ todayCookCta }}</text>
        </view>
        <text class="dual-more">查看更多 ›</text>
      </view>
      <view class="dual-card dual-learn" @click="goModule('learn')">
        <text class="dual-tag tag-cool">持续学习</text>
        <text class="dual-title">{{ learnTitle }}</text>
        <text class="dual-desc">{{ learnDesc }}</text>
        <view class="dual-cta cta-cool">
          <text>{{ learnCta }}</text>
        </view>
        <text class="dual-more">查看更多 ›</text>
      </view>
    </view>

    <!-- 更多入口 -->
    <view class="more-entry" @click="goModule('more')">
      <view class="more-left">
        <view class="more-icon">
          <wd-icon name="app" size="18px" color="#1B4F8A"></wd-icon>
        </view>
        <view>
          <text class="more-title">全部功能</text>
          <text class="more-sub">资料存储 · 家庭管理 · 更多工具</text>
        </view>
      </view>
      <wd-icon name="arrow-right" size="14px" color="#C4BFB6"></wd-icon>
    </view>

    <view class="footer-tip">
      <text class="tip-text" @tap="goAgreement">使用即表示同意《用户服务协议》</text>
    </view>
    </template>
  </view>
</template>

<script lang="ts" setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '../../pages-homeai/stores/user'
import { useFamilyStore } from '../../pages-homeai/stores/family'
import { get as getApi } from '../../pages-homeai/api/request'
import { learnApi } from '../../pages-homeai/api/learn'
import { ensureLoginForAction, ensureProfileWhenGuest, openAuthPage } from '../../pages-homeai/utils/homeaiAuth'
import { localDateStr } from '../../pages-homeai/utils/date'
import { useHomeaiPullRefresh } from '../../pages-homeai/utils/useHomeaiPullRefresh'
import { useFamilyPoll } from '../../pages-homeai/utils/useFamilyPoll'
import HomeSkeleton from '../../components/HomeSkeleton.vue'

const userStore = useUserStore()
const familyStore = useFamilyStore()
const todayTodo = ref(0)
const planLoadFailed = ref(false)
const todayCookPlans = ref<{ recipeId?: string; recipeName?: string; title?: string }[]>([])
const learnGoal = ref<{ todayMinutes?: number; goalMinutes?: number; reached?: boolean } | null>(null)
/** 首次进入首页时展示骨架，避免家庭/计划数据未到齐时闪屏 */
const booting = ref(true)

const todayCookTitle = computed(() => {
  if (planLoadFailed.value) return '今日下厨'
  const first = todayCookPlans.value[0]
  if (first?.recipeName) return first.recipeName
  if (first?.title) return first.title
  return '烹饪指南'
})
const todayCookDesc = computed(() => {
  if (planLoadFailed.value) return '计划加载失败，点此重试'
  const n = todayCookPlans.value.length
  if (n > 1) return `今日还有 ${n - 1} 道关联菜谱`
  if (n === 1) return '来自今日计划的关联菜谱'
  return '家里想吃什么，从这里找灵感'
})
const todayCookCta = computed(() => {
  if (planLoadFailed.value) return '重试'
  return todayCookPlans.value.length ? '去烹饪' : '去看看'
})
const learnTitle = computed(() => (learnGoal.value ? '今日学习' : '学习模块'))
const learnDesc = computed(() => {
  const g = learnGoal.value
  if (!g) return '资料与打卡，攒一点小小进步'
  if (g.reached) return '今日目标已达成'
  return `已学 ${g.todayMinutes || 0} / ${g.goalMinutes || 30} 分钟`
})
const learnCta = computed(() => (learnGoal.value?.reached ? '再学一会' : '去打卡'))

const sys = uni.getSystemInfoSync()
const statusBarPx = sys.statusBarHeight || 20
const windowWidth = sys.windowWidth || 375
// 与页面左右 32rpx 对齐，额外为微信胶囊让位
const pagePadPx = (32 * windowWidth) / 750
let headerRightPx = 12
// #ifdef MP-WEIXIN
try {
  const menu = uni.getMenuButtonBoundingClientRect()
  if (menu?.left) {
    headerRightPx = Math.max(12, windowWidth - menu.left - pagePadPx + 8)
  }
} catch {
  headerRightPx = 96
}
// #endif

const quickEntries = [
  { key: 'ai', icon: 'chat', label: 'AI对话', sub: '智能问答' },
  { key: 'plan', icon: 'calendar', label: '计划', sub: '日程安排' },
  { key: 'bill', icon: 'money-circle', label: '账单', sub: '收支记账' },
  { key: 'storage', icon: 'folder', label: '资料', sub: '文件管理' },
  { key: 'recipe', icon: 'goods', label: '烹饪', sub: '菜谱灵感' },
  { key: 'learn', icon: 'books', label: '学习', sub: '资料打卡' },
]

const familyLabel = computed(() => {
  if (!userStore.isLogin) return '点击登录'
  if (familyStore.hasFamily) return familyStore.familyInfo?.name || '我的家庭'
  return '加入家庭'
})

async function reloadHome() {
  await familyStore.fetchFamilyInfo()
  await reloadPlans()
  if (userStore.isLogin) await reloadLearnGoal()
}

useHomeaiPullRefresh(async () => {
  if (!userStore.isLogin) return
  await reloadHome()
})

const { start: startFamilyPoll, stop: stopFamilyPoll } = useFamilyPoll()

onShow(async () => {
  if (!ensureProfileWhenGuest()) {
    booting.value = false
    stopFamilyPoll()
    return
  }
  try {
    await reloadHome()
  } finally {
    booting.value = false
  }
  startFamilyPoll()
})

async function reloadLearnGoal() {
  try {
    learnGoal.value = (await learnApi.goal()) || learnGoal.value
  } catch {
    // 目标失败不挡首页
  }
}

async function reloadPlans() {
  planLoadFailed.value = false
  try {
    const list = await getApi(`/plan/date/${localDateStr()}`)
    const arr = Array.isArray(list) ? list : []
    todayTodo.value = arr.filter((p: any) => p.status === 'pending').length
    todayCookPlans.value = arr.filter((p: any) => p.recipeId)
  } catch {
    planLoadFailed.value = true
  }
}

function onPlanCardClick() {
  if (planLoadFailed.value) {
    reloadPlans()
    return
  }
  goModule('plan')
}

function goAgreement() {
  uni.navigateTo({ url: '/pages/agreement/index' })
}

function goFamily() {
  if (!ensureLoginForAction()) return
  uni.switchTab({ url: '/pages/homeai/family' })
}

function onFamilyClick() {
  if (!userStore.isLogin) {
    openAuthPage()
    return
  }
  goFamily()
}

function goTodayCook() {
  if (!ensureLoginForAction()) return
  if (planLoadFailed.value) {
    reloadPlans()
    return
  }
  const first = todayCookPlans.value[0]
  if (first?.recipeId) {
    uni.navigateTo({ url: `/pages-homeai-more/recipe/detail?id=${first.recipeId}` })
    return
  }
  goModule('recipe')
}

function goModule(key: string) {
  if (!ensureLoginForAction()) return
  const pages: Record<string, string> = {
    ai: '/pages-homeai-ai/ai/conversations',
    storage: '/pages-homeai-more/storage/index',
    bill: '/pages-homeai-more/bill/index',
    plan: '/pages-homeai-more/plan/index',
    recipe: '/pages-homeai-more/recipe/index',
    learn: '/pages-homeai-more/learn/index',
    more: '/pages-homeai-more/all-functions/index',
  }
  const url = pages[key]
  if (url) {
    uni.navigateTo({ url })
  } else {
    uni.showToast({ title: '功能开发中', icon: 'none' })
  }
}
</script>

<style scoped>
.homeai-page {
  min-height: 100vh;
  box-sizing: border-box;
  padding: 0 32rpx 48rpx;
  background: var(--hai-bg);
}

.boot-skeleton {
  padding-top: 36rpx;
}

.boot-skeleton .sk-hero {
  height: 240rpx;
  border-radius: var(--hai-radius-md, 24rpx);
  margin-bottom: 24rpx;
  background: linear-gradient(90deg, #ece9e2 25%, #e8e5de 50%, #ece9e2 75%);
  background-size: 200% 100%;
  animation: home-boot-shimmer 1.5s infinite;
}

@keyframes home-boot-shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  padding: 36rpx 0 28rpx;
}

.header-main {
  flex: 1;
  min-width: 0;
}

.greeting {
  display: block;
  font-family: var(--hai-serif);
  font-size: 48rpx;
  font-weight: 700;
  color: var(--hai-text);
  line-height: 1.25;
  letter-spacing: 1rpx;
}

.family-row {
  display: inline-flex;
  align-items: center;
  gap: 6rpx;
  margin-top: 16rpx;
}

.family-label {
  font-size: 24rpx;
  color: var(--hai-text-secondary);
  max-width: 360rpx;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.plan-btn {
  position: relative;
  width: 88rpx;
  height: 88rpx;
  margin-left: 16rpx;
  border-radius: 22rpx;
  background: var(--hai-card);
  box-shadow: var(--hai-shadow);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.notify-dot {
  position: absolute;
  top: 16rpx;
  right: 16rpx;
  width: 12rpx;
  height: 12rpx;
  border-radius: 50%;
  background: var(--hai-danger);
}

.hero-card {
  position: relative;
  overflow: hidden;
  min-height: 240rpx;
  padding: 40rpx 36rpx;
  border-radius: var(--hai-radius);
  background: var(--hai-card);
  box-shadow: var(--hai-shadow);
}

.hero-body {
  position: relative;
  z-index: 1;
  max-width: 62%;
}

.hero-title {
  display: block;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 36rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.hero-desc {
  display: block;
  margin-top: 12rpx;
  font-size: 22rpx;
  line-height: 1.5;
  color: var(--hai-text-secondary);
}

.hero-cta {
  display: inline-flex;
  align-items: center;
  gap: 8rpx;
  margin-top: 28rpx;
  padding: 16rpx 28rpx;
  border-radius: 999rpx;
  background: var(--hai-primary);
}

.hero-cta-text {
  font-size: 24rpx;
  color: var(--hai-on-primary);
  font-weight: 500;
}

.hero-decor {
  position: absolute;
  right: -40rpx;
  top: -20rpx;
  width: 280rpx;
  height: 280rpx;
  border-radius: 50%;
  background: radial-gradient(circle at 35% 40%, rgba(27, 79, 138, 0.18) 0%, rgba(27, 79, 138, 0.04) 55%, transparent 70%);
}

.quick-card {
  display: flex;
  flex-wrap: wrap;
  margin-top: 24rpx;
  padding: 16rpx 8rpx 8rpx;
  border-radius: var(--hai-radius);
  background: var(--hai-card);
  box-shadow: var(--hai-shadow);
}

.quick-item {
  width: 33.333%;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20rpx 4rpx;
  position: relative;
  box-sizing: border-box;
}

.quick-item:not(:nth-child(3n))::after {
  content: '';
  position: absolute;
  right: 0;
  top: 24rpx;
  bottom: 24rpx;
  width: 1rpx;
  background: var(--hai-border);
}

.quick-icon {
  width: 80rpx;
  height: 80rpx;
  border-radius: 22rpx;
  background: var(--hai-bg);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12rpx;
}

.quick-title {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 28rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.quick-sub {
  margin-top: 4rpx;
  font-size: 18rpx;
  color: var(--hai-text-muted);
  text-align: center;
}

.section {
  margin-top: 40rpx;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20rpx;
  padding: 0 4rpx;
}

.section-title {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 34rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.section-link {
  display: flex;
  align-items: center;
  gap: 4rpx;
  font-size: 22rpx;
  color: var(--hai-text-secondary);
}

.plan-card {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 32rpx 28rpx;
  border-radius: 28rpx;
  background: var(--hai-card);
  box-shadow: var(--hai-shadow);
}

.plan-icon-wrap {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: var(--hai-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.plan-info {
  flex: 1;
  min-width: 0;
}

.plan-title {
  display: block;
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.plan-desc {
  display: block;
  margin-top: 8rpx;
  font-size: 22rpx;
  color: var(--hai-text-secondary);
  line-height: 1.4;
}

.dual-row {
  display: flex;
  gap: 20rpx;
  margin-top: 24rpx;
}

.dual-card {
  flex: 1;
  min-height: 320rpx;
  padding: 28rpx 24rpx;
  border-radius: 28rpx;
  box-shadow: var(--hai-shadow);
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
}

.dual-recipe {
  background: linear-gradient(165deg, #fff8f3 0%, #ffffff 55%);
}

.dual-learn {
  background: linear-gradient(165deg, #f2f6f4 0%, #ffffff 55%);
}

.dual-tag {
  align-self: flex-start;
  padding: 6rpx 14rpx;
  border-radius: 999rpx;
  font-size: 18rpx;
  margin-bottom: 20rpx;
}

.tag-warm {
  background: var(--hai-danger-soft);
  color: var(--hai-danger);
}

.tag-cool {
  background: rgba(62, 110, 88, 0.12);
  color: var(--hai-success);
}

.dual-title {
  font-family: 'Songti SC', 'STSong', 'Noto Serif SC', serif;
  font-size: 30rpx;
  font-weight: 700;
  color: var(--hai-text);
}

.dual-desc {
  margin-top: 10rpx;
  font-size: 20rpx;
  line-height: 1.45;
  color: var(--hai-text-secondary);
  flex: 1;
}

.dual-cta {
  align-self: flex-start;
  margin-top: 20rpx;
  padding: 10rpx 22rpx;
  border-radius: 999rpx;
  font-size: 20rpx;
  color: var(--hai-on-primary);
}

.cta-warm {
  background: var(--hai-danger);
}

.cta-cool {
  background: var(--hai-success);
}

.dual-more {
  margin-top: 18rpx;
  text-align: center;
  font-size: 20rpx;
  color: var(--hai-text-muted);
}

.more-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding: 28rpx;
  border-radius: 28rpx;
  background: var(--hai-card);
  box-shadow: var(--hai-shadow);
}

.more-left {
  display: flex;
  align-items: center;
  gap: 20rpx;
  min-width: 0;
}

.more-icon {
  width: 64rpx;
  height: 64rpx;
  border-radius: 18rpx;
  background: var(--hai-primary-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.more-title {
  display: block;
  font-size: 28rpx;
  font-weight: 600;
  color: var(--hai-text);
}

.more-sub {
  display: block;
  margin-top: 6rpx;
  font-size: 20rpx;
  color: var(--hai-text-muted);
}

.footer-tip {
  text-align: center;
  padding: 40rpx 0 16rpx;
}

.tip-text {
  font-size: 20rpx;
  color: #b5b0a6;
  text-decoration: underline;
}
</style>
