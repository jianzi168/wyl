<template>
  <view class="page">
    <view class="month-nav card">
      <text @tap="reportStore.prevMonth">‹</text>
      <text class="month">{{ monthLabel }}</text>
      <text @tap="reportStore.nextMonth">›</text>
    </view>

    <view class="report-links card">
      <text class="link" @tap="goWeekly">📊 周报</text>
      <text class="link" @tap="goMonthly">📅 月报热力图</text>
    </view>

    <view class="summary card">
      <view class="item">
        <text class="label">收入</text>
        <text class="value income">¥{{ formatMoney(reportStore.monthIncomeTotal) }}</text>
      </view>
      <view class="item">
        <text class="label">支出</text>
        <text class="value expense">¥{{ formatMoney(reportStore.monthExpenseTotal) }}</text>
      </view>
      <view class="item">
        <text class="label">结余</text>
        <text class="value">¥{{ formatMoney(reportStore.monthIncomeTotal - reportStore.monthExpenseTotal) }}</text>
      </view>
    </view>

    <view class="chart-card card">
      <text class="section-title">支出占比</text>
      <RingChart
        v-if="ringSegments.length"
        :segments="ringSegments"
        :total="reportStore.monthExpenseTotal"
        :size="200"
      />
      <view
        v-for="seg in reportStore.categoryBreakdown"
        :key="seg.name"
        class="legend"
        @tap="goDetail(seg.name)"
        @longpress="setBudget(seg.name)"
      >
        <text>{{ seg.emoji }} {{ seg.name }}</text>
        <view class="bar-track"><view class="bar" :style="{ width: seg.percent + '%', background: seg.color }" /></view>
        <text class="amt">{{ seg.percent }}% ¥{{ formatMoney(seg.amount) }}</text>
      </view>
    </view>

    <view class="chart-card card">
      <text class="section-title">近 6 月趋势</text>
      <LineChart :labels="trendLabels" :series="trendSeries" :width="320" :height="160" />
    </view>

    <view v-if="reportStore.prediction" class="prediction card">
      <text class="section-title">🔮 本月预测</text>
      <text>已花 ¥{{ formatMoney(reportStore.prediction.spent) }}，预计全月 ¥{{ formatMoney(reportStore.prediction.predicted) }}</text>
      <text class="hint">按当前日均 ¥{{ formatMoney(reportStore.prediction.dailyAvg) }} 推算，还剩 {{ reportStore.prediction.daysLeft }} 天</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import { useRecordStore } from '@/stores/record'
import RingChart from '@/components/RingChart.vue'
import LineChart from '@/components/LineChart.vue'
import { useReportStore } from '@/stores'
import { formatMoney, monthLabel as fmtMonth } from '@/utils/date'
import { ensureLoggedIn } from '@/utils/auth-guard'

const reportStore = useReportStore()

const monthLabel = computed(() =>
  fmtMonth(reportStore.selectedYear, reportStore.selectedMonth)
)

const ringSegments = computed(() =>
  reportStore.categoryBreakdown.map((s) => ({ color: s.color, percent: s.percent }))
)

const trendLabels = computed(() => reportStore.trendData.map((d) => d.label))
const trendSeries = computed(() => [
  { name: '整体', color: '#6c5ce7', data: reportStore.trendData.map((d) => d.total) },
  { name: '餐饮', color: '#ff6b6b', data: reportStore.trendData.map((d) => d.food) },
  { name: '购物', color: '#4ecdc4', data: reportStore.trendData.map((d) => d.shop) },
])

function goDetail(category: string) {
  uni.navigateTo({
    url: `/pages/report/detail?category=${encodeURIComponent(category)}&year=${reportStore.selectedYear}&month=${reportStore.selectedMonth}`,
  })
}

function goWeekly() {
  uni.navigateTo({ url: '/pages/report/weekly' })
}

function goMonthly() {
  uni.navigateTo({ url: '/pages/report/monthly' })
}

function setBudget(category: string) {
  uni.showModal({
    title: '设置预算',
    content: `为「${category}」调整月预算？`,
    success(res) {
      if (res.confirm) {
        uni.navigateTo({
          url: `/pages/settings/budget?category=${encodeURIComponent(category)}`,
        })
      }
    },
  })
}

onShow(() => ensureLoggedIn())

onPullDownRefresh(() => {
  useRecordStore()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.report-links { display: flex; gap: $spacing-lg; margin-bottom: $spacing-md; }
.link { font-size: $font-size-sm; color: $color-primary; font-weight: 500; }
.month-nav { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-md; font-size: $font-size-lg; }
.month { font-weight: 600; }
.summary { display: flex; justify-content: space-between; margin-bottom: $spacing-md; }
.item { text-align: center; flex: 1; }
.label { font-size: $font-size-xs; color: $color-text-muted; display: block; }
.value { font-size: $font-size-md; font-weight: 600; display: block; margin-top: 4rpx; }
.income { color: $color-income; }
.expense { color: $color-expense; }
.chart-card { margin-bottom: $spacing-md; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-md; }
.legend { display: flex; align-items: center; gap: $spacing-sm; margin-bottom: $spacing-sm; font-size: $font-size-sm; }
.bar-track { flex: 1; height: 10rpx; background: $color-border; border-radius: $radius-full; overflow: hidden; }
.bar { height: 100%; }
.amt { width: 140rpx; text-align: right; color: $color-text-secondary; font-size: $font-size-xs; }
.prediction { font-size: $font-size-sm; color: $color-text-secondary; }
.hint { display: block; margin-top: $spacing-xs; font-size: $font-size-xs; color: $color-text-muted; }
</style>
