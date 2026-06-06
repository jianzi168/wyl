<template>
  <view class="page">
    <view class="nav card">
      <text @tap="reportStore.prevMonth">‹</text>
      <text class="title">{{ monthLabel }}</text>
      <text @tap="reportStore.nextMonth">›</text>
    </view>

    <view class="summary card">
      <text>收入 ¥{{ formatMoney(reportStore.monthIncomeTotal) }}</text>
      <text>支出 ¥{{ formatMoney(reportStore.monthExpenseTotal) }}</text>
      <text>结余 ¥{{ formatMoney(reportStore.monthIncomeTotal - reportStore.monthExpenseTotal) }}</text>
    </view>

    <view class="card">
      <text class="section-title">📅 支出日历</text>
      <CalendarHeatmap :rows="heatmap" />
    </view>

    <view class="card">
      <text class="section-title">分类 TOP</text>
      <view v-for="seg in reportStore.categoryBreakdown.slice(0, 5)" :key="seg.name" class="row">
        <text>{{ seg.emoji }} {{ seg.name }}</text>
        <text>¥{{ formatMoney(seg.amount) }} ({{ seg.percent }}%)</text>
      </view>
    </view>

    <view class="actions card">
      <button class="action-btn" @tap="onAiMonthly">🤖 AI 月报解读</button>
      <button class="action-btn ghost" @tap="onShare">📤 分享月报</button>
    </view>

    <view class="card tips">
      <text class="section-title">💡 月报摘要</text>
      <text>{{ summaryText }}</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import CalendarHeatmap from '@/components/CalendarHeatmap.vue'
import { useReportStore } from '@/stores'
import { formatMoney, monthLabel as fmtMonth } from '@/utils/date'
import { buildMonthHeatmap } from '@/utils/report'
import { useRecordStore } from '@/stores'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { useCoachStore } from '@/stores/coach'
import { useAchievementStore } from '@/stores/achievement'
import { incrementShareCount } from '@/utils/share'

const reportStore = useReportStore()
const recordStore = useRecordStore()
const coachStore = useCoachStore()
const achievementStore = useAchievementStore()

const monthLabel = computed(() =>
  fmtMonth(reportStore.selectedYear, reportStore.selectedMonth)
)

const heatmap = computed(() =>
  buildMonthHeatmap(
    recordStore.records,
    reportStore.selectedYear,
    reportStore.selectedMonth
  )
)

const summaryText = computed(() => {
  const top = reportStore.categoryBreakdown[0]
  if (!top) return '本月暂无支出记录'
  return `本月支出最高分类为${top.name}（¥${top.amount.toFixed(0)}，占${top.percent}%）。关注可选消费可进一步优化预算。`
})

async function onAiMonthly() {
  try {
    await coachStore.interpretMonthly()
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
.summary { display: flex; justify-content: space-between; margin-bottom: $spacing-md; font-size: $font-size-sm; }
.card { margin-bottom: $spacing-md; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-md; }
.row { display: flex; justify-content: space-between; font-size: $font-size-sm; margin-bottom: $spacing-xs; }
.tips { font-size: $font-size-sm; color: $color-text-secondary; line-height: 1.6; }
.actions { display: flex; gap: $spacing-sm; margin-bottom: $spacing-md; }
.action-btn { flex: 1; background: $color-primary; color: #fff; border: none; border-radius: $radius-full; font-size: $font-size-xs; }
.action-btn.ghost { background: $color-bg-card; color: $color-primary; border: 1rpx solid $color-border; }
</style>
