<template>
  <view class="page">
    <view v-if="showOnboardingBanner" class="banner" @tap="goOnboarding">
      <text>⚙️ 完成预算设置，记账反馈更精准 ›</text>
    </view>

    <view class="header gradient-header">
      <view class="header-top">
        <text class="greeting">👋 {{ userStore.displayName }}</text>
        <text class="date">{{ todayLabel }}</text>
      </view>
      <view class="budget-summary">
        <text class="label">本月可支配</text>
        <text class="amount">¥{{ formatMoney(disposable) }}</text>
        <view class="progress-track">
          <view class="progress-bar" :style="{ width: budgetPercent + '%' }" />
        </view>
        <text class="hint">已花 ¥{{ formatMoney(recordStore.monthExpenseTotal) }} · 可选剩余 ¥{{ formatMoney(budgetStore.optionalRemaining) }}</text>
        <text v-if="recordStore.vsLastMonthDiff > 0" class="compare good">
          📉 比上月同期少花 ¥{{ formatMoney(recordStore.vsLastMonthDiff) }}
        </text>
        <text v-else-if="recordStore.vsLastMonthDiff < 0" class="compare warn">
          📈 比上月同期多花 ¥{{ formatMoney(Math.abs(recordStore.vsLastMonthDiff)) }}
        </text>
      </view>
    </view>

    <view v-if="homeInsight" class="insight card" @tap="goInsights">
      <text class="insight-text">💡 {{ homeInsight.content }}</text>
      <text class="insight-more">查看洞察中心 ›</text>
    </view>

    <view class="section">
      <text class="section-title">快捷记账</text>
      <view class="category-grid">
        <view
          v-for="cat in quickCategories"
          :key="cat.id"
          class="category-item card"
          @tap="goRecord(cat.name)"
        >
          <text class="emoji">{{ cat.emoji }}</text>
          <text class="name">{{ cat.name }}</text>
          <text class="spent">¥{{ formatMoney(budgetStore.getCategorySpent(cat.name)) }}</text>
        </view>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">最近记录</text>
        <text v-if="!recordStore.recentRecords.length" class="text-muted">暂无记录</text>
      </view>
      <view
        v-for="item in recordStore.recentRecords"
        :key="item.id"
        class="record-item card"
        @tap="editRecord(item.id)"
        @longpress="deleteRecord(item.id)"
      >
        <text>{{ categoryEmoji(item.category) }} {{ item.category }} · {{ item.nature }}</text>
        <text class="record-amount">¥{{ formatMoney(item.amount) }}</text>
      </view>
    </view>

    <view v-if="!fabCollapsed" class="fab" @tap="goRecord()">
      <text class="fab-text">＋ 记一笔</text>
    </view>
    <view v-else class="fab-mini" @tap="fabCollapsed = false">＋</view>
    <text class="fab-toggle" @tap="toggleFab">{{ fabCollapsed ? '展开' : '收起' }}</text>
  </view>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { useUserStore, useRecordStore, useBudgetStore, useSyncStore } from '@/stores'
import { useInsightStore } from '@/stores/insight'
import { formatMoney } from '@/utils/date'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { useHomeInsight } from '@/composables/useInsights'
import { getStorage, setStorage } from '@/utils/storage'

const userStore = useUserStore()
const recordStore = useRecordStore()
const budgetStore = useBudgetStore()
const syncStore = useSyncStore()
const insightStore = useInsightStore()
const fabCollapsed = ref(getStorage('fab_collapsed', false))
const insightDismissed = ref(getStorage('insight_dismissed_date', ''))

const todayLabel = computed(() => {
  const d = new Date()
  return `${d.getMonth() + 1}月${d.getDate()}日`
})

const showOnboardingBanner = computed(
  () => userStore.isLoggedIn && !userStore.user?.onboardingCompleted
)

const disposable = computed(() =>
  Math.max(0, budgetStore.totalMonthlyBudget - recordStore.monthExpenseTotal)
)

const quickCategories = computed(() =>
  budgetStore.categories.filter((c) => c.isVisible).slice(0, 6)
)

const budgetPercent = computed(() => {
  const total = budgetStore.totalMonthlyBudget
  if (!total) return 0
  return Math.min(100, Math.round((recordStore.monthExpenseTotal / total) * 100))
})

const homeInsight = computed(() => {
  const today = new Date().toDateString()
  if (insightDismissed.value === today) return null
  return useHomeInsight().value
})

function categoryEmoji(name: string) {
  return budgetStore.categories.find((c) => c.name === name)?.emoji || '···'
}

function goInsights() {
  uni.navigateTo({ url: '/pages/profile/insights' })
}

function goRecord(category?: string) {
  const query = category ? `?category=${encodeURIComponent(category)}` : ''
  uni.navigateTo({ url: `/pages/record/quick${query}` })
}

function editRecord(id: string) {
  uni.navigateTo({ url: `/pages/record/quick?id=${id}` })
}

function deleteRecord(id: string) {
  uni.showModal({
    title: '删除记录',
    content: '确定删除这条记录？',
    confirmColor: '#d63031',
    success(res) {
      if (res.confirm) recordStore.removeRecord(id)
    },
  })
}

function goOnboarding() {
  uni.navigateTo({ url: '/pages/onboarding/index' })
}

function toggleFab() {
  fabCollapsed.value = !fabCollapsed.value
  setStorage('fab_collapsed', fabCollapsed.value)
}

onMounted(() => {
  if (!ensureLoggedIn()) return
  if (userStore.isNewUser && !userStore.user?.onboardingCompleted) {
    uni.navigateTo({ url: '/pages/onboarding/index' })
  }
  syncStore.syncNow()
})

onShow(() => {
  if (!userStore.isLoggedIn) return
  syncStore.syncNow()
  insightStore.generateFromRules()
})

onPullDownRefresh(async () => {
  await syncStore.syncNow()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { min-height: 100vh; padding: $spacing-md; padding-bottom: 160rpx; padding-top: calc($spacing-md + env(safe-area-inset-top)); }
.banner { background: rgba(108,92,231,0.12); color: $color-primary; padding: $spacing-sm $spacing-md; border-radius: $radius-md; margin-bottom: $spacing-sm; font-size: $font-size-sm; }
.header { padding: $spacing-lg $spacing-md; margin-bottom: $spacing-md; }
.header-top { display: flex; justify-content: space-between; margin-bottom: $spacing-md; }
.greeting { font-size: $font-size-lg; font-weight: 600; }
.date { font-size: $font-size-sm; opacity: 0.9; }
.budget-summary .label { font-size: $font-size-sm; opacity: 0.9; }
.budget-summary .amount { display: block; font-size: $font-size-xxl; font-weight: 700; margin: $spacing-xs 0; }
.progress-track { height: 12rpx; background: rgba(255,255,255,0.3); border-radius: $radius-full; overflow: hidden; }
.progress-bar { height: 100%; background: #fff; border-radius: $radius-full; }
.hint { display: block; margin-top: $spacing-sm; font-size: $font-size-sm; opacity: 0.95; }
.compare { display: block; margin-top: $spacing-xs; font-size: $font-size-sm; }
.compare.good { opacity: 0.95; }
.compare.warn { opacity: 0.85; }
.insight { margin-bottom: $spacing-md; }
.insight-text { font-size: $font-size-sm; color: $color-text-secondary; display: block; }
.insight-more { font-size: $font-size-xs; color: $color-primary; margin-top: $spacing-xs; display: block; }
.section { margin-bottom: $spacing-lg; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.section-title { font-size: $font-size-lg; font-weight: 600; margin-bottom: $spacing-sm; display: block; }
.category-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: $spacing-sm; }
.category-item { display: flex; flex-direction: column; align-items: center; padding: $spacing-md $spacing-sm; }
.emoji { font-size: 40rpx; }
.name { font-size: $font-size-sm; margin-top: $spacing-xs; }
.spent { font-size: $font-size-xs; color: $color-text-muted; margin-top: 4rpx; }
.record-item { display: flex; justify-content: space-between; margin-bottom: $spacing-sm; }
.record-amount { font-weight: 600; color: $color-expense; }
.fab { position: fixed; right: $spacing-lg; bottom: calc(120rpx + env(safe-area-inset-bottom)); background: $color-primary-gradient; color: #fff; padding: $spacing-sm $spacing-lg; border-radius: $radius-full; box-shadow: 0 8rpx 24rpx rgba(108,92,231,0.4); }
.fab-mini { position: fixed; right: $spacing-lg; bottom: calc(120rpx + env(safe-area-inset-bottom)); width: 88rpx; height: 88rpx; border-radius: 50%; background: $color-primary; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 40rpx; }
.fab-text { font-size: $font-size-md; font-weight: 600; }
.fab-toggle { position: fixed; right: $spacing-lg; bottom: calc(200rpx + env(safe-area-inset-bottom)); font-size: $font-size-xs; color: $color-text-muted; }
</style>
