<template>
  <view class="page">
    <view class="nav card">
      <text @tap="shiftWeek(-1)">‹ 上周</text>
      <text class="title">{{ weekLabel }}</text>
      <text @tap="shiftWeek(1)" :class="{ disabled: isCurrentWeek }">下周 ›</text>
    </view>

    <view class="summary card">
      <view class="col">
        <text class="label">本周支出</text>
        <text class="value">¥{{ formatMoney(weekTotal) }}</text>
      </view>
      <view class="col">
        <text class="label">上周支出</text>
        <text class="value muted">¥{{ formatMoney(lastWeekTotal) }}</text>
      </view>
      <view class="col">
        <text class="label">对比</text>
        <text class="value" :class="diff >= 0 ? 'good' : 'bad'">
          {{ diff >= 0 ? '↓' : '↑' }}{{ Math.abs(diffPercent) }}%
        </text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">每日支出</text>
      <view v-for="day in daily" :key="day.date" class="day-row">
        <text class="day-label">{{ day.label }}</text>
        <view class="bar-track">
          <view class="bar" :style="{ width: barWidth(day.amount) + '%' }" />
        </view>
        <text class="amt">¥{{ formatMoney(day.amount) }}</text>
      </view>
    </view>

    <view class="actions card">
      <button class="action-btn" @tap="onAiWeekly">🤖 AI 周报解读</button>
      <button class="action-btn ghost" @tap="onShare">📤 分享周报</button>
    </view>

    <view class="card">
      <text class="section-title">分类占比</text>
      <view v-for="c in categories" :key="c.name" class="cat-row">
        <text>{{ c.name }}</text>
        <text>{{ c.percent }}% · ¥{{ formatMoney(c.amount) }}</text>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useRecordStore } from '@/stores'
import { formatMoney } from '@/utils/date'
import {
  getWeekRange,
  getWeekDailyBreakdown,
  weekCategoryBreakdown,
} from '@/utils/report'
import { sumExpenses } from '@/utils/budget'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { useCoachStore } from '@/stores/coach'
import { useAchievementStore } from '@/stores/achievement'
import { incrementShareCount } from '@/utils/share'

const recordStore = useRecordStore()
const coachStore = useCoachStore()
const achievementStore = useAchievementStore()
const weekOffset = ref(0)

const weekRange = computed(() => {
  const d = new Date()
  d.setDate(d.getDate() + weekOffset.value * 7)
  return getWeekRange(d)
})

const lastWeekRange = computed(() => {
  const d = new Date(weekRange.value.monday)
  d.setDate(d.getDate() - 7)
  return getWeekRange(d)
})

const isCurrentWeek = computed(() => weekOffset.value >= 0)

const weekLabel = computed(() => {
  const { monday, sunday } = weekRange.value
  return `${monday.getMonth() + 1}.${monday.getDate()} - ${sunday.getMonth() + 1}.${sunday.getDate()}`
})

const weekTotal = computed(() =>
  sumExpenses(recordStore.records, weekRange.value.start, weekRange.value.end)
)
const lastWeekTotal = computed(() =>
  sumExpenses(recordStore.records, lastWeekRange.value.start, lastWeekRange.value.end)
)
const diff = computed(() => lastWeekTotal.value - weekTotal.value)
const diffPercent = computed(() =>
  lastWeekTotal.value ? Math.round((diff.value / lastWeekTotal.value) * 100) : 0
)

const daily = computed(() =>
  getWeekDailyBreakdown(recordStore.records, weekRange.value.start, weekRange.value.end)
)
const categories = computed(() =>
  weekCategoryBreakdown(recordStore.records, weekRange.value.start, weekRange.value.end)
)

const maxDaily = computed(() => Math.max(...daily.value.map((d) => d.amount), 1))

function barWidth(amount: number) {
  return Math.round((amount / maxDaily.value) * 100)
}

function shiftWeek(delta: number) {
  if (delta > 0 && isCurrentWeek.value) return
  weekOffset.value += delta
}

async function onAiWeekly() {
  try {
    await coachStore.interpretWeekly()
    uni.navigateTo({ url: '/pages/profile/coach' })
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '生成失败', icon: 'none' })
  }
}

function onShare() {
  incrementShareCount()
  achievementStore.checkShare()
  achievementStore.checkShareMaster()
  uni.showToast({ title: '已记录分享', icon: 'success' })
}

onShow(() => ensureLoggedIn())
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.nav { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-md; }
.title { font-weight: 600; }
.disabled { opacity: 0.3; }
.summary { display: flex; margin-bottom: $spacing-md; }
.col { flex: 1; text-align: center; }
.label { font-size: $font-size-xs; color: $color-text-muted; display: block; }
.value { font-size: $font-size-md; font-weight: 600; display: block; margin-top: 4rpx; }
.muted { color: $color-text-secondary; }
.good { color: $color-income; }
.bad { color: $color-expense; }
.card { margin-bottom: $spacing-md; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-md; }
.day-row { display: flex; align-items: center; gap: $spacing-sm; margin-bottom: $spacing-sm; font-size: $font-size-sm; }
.day-label { width: 80rpx; }
.bar-track { flex: 1; height: 12rpx; background: $color-border; border-radius: $radius-full; overflow: hidden; }
.bar { height: 100%; background: $color-primary; border-radius: $radius-full; }
.amt { width: 100rpx; text-align: right; }
.cat-row { display: flex; justify-content: space-between; font-size: $font-size-sm; margin-bottom: $spacing-xs; }
.actions { display: flex; gap: $spacing-sm; margin-bottom: $spacing-md; }
.action-btn { flex: 1; background: $color-primary; color: #fff; border: none; border-radius: $radius-full; font-size: $font-size-xs; }
.action-btn.ghost { background: $color-bg-card; color: $color-primary; border: 1rpx solid $color-border; }
</style>
