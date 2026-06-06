<template>
  <view class="page" v-if="record">
    <view class="success">✅ 已记录</view>
    <view class="summary card">
      <text class="main">{{ emoji }} {{ record.category }} · {{ record.nature }}  ¥{{ formatMoney(record.amount) }}</text>
      <view class="budget-block">
        <text>{{ record.category }}预算剩余 ¥{{ formatMoney(catStatus.remaining) }} · 已用 {{ catStatus.percent }}%</text>
        <view class="bar"><view class="fill" :style="{ width: Math.min(catStatus.percent, 100) + '%' }" /></view>
      </view>
      <view class="budget-block">
        <text>🟡 可选预算剩余 ¥{{ formatMoney(budgetStore.optionalRemaining) }}</text>
        <view class="bar optional"><view class="fill" :style="{ width: Math.min(budgetStore.optionalUsagePercent, 100) + '%' }" /></view>
      </view>
      <view v-if="insight" class="insight">💡 {{ insight.content }}</view>
    </view>

    <view class="nature-edit card">
      <text class="label">修改性质</text>
      <view class="nature-row">
        <view v-for="n in natures" :key="n" class="chip" :class="{ active: record.nature === n }" @tap="changeNature(n)">{{ n }}</view>
      </view>
    </view>

    <view class="actions">
      <button class="btn ghost" @tap="finish">完成</button>
      <button class="btn primary" @tap="again">再记一笔</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useRecordStore, useBudgetStore } from '@/stores'
import { formatMoney } from '@/utils/date'
import { useFeedbackInsight } from '@/composables/useInsights'
import { track } from '@/utils/analytics'
import { trySubscribeAfterRecord } from '@/utils/subscribe-message'
import type { RecordNature } from '@/types/models'

const recordStore = useRecordStore()
const budgetStore = useBudgetStore()
const recordId = ref('')
const record = computed(() => recordStore.getById(recordId.value))
const natures: RecordNature[] = ['刚需', '可选', '奢侈']

const catStatus = computed(() =>
  record.value ? budgetStore.getCategoryStatus(record.value.category) : { remaining: 0, percent: 0 }
)

const emoji = computed(() =>
  budgetStore.categories.find((c) => c.name === record.value?.category)?.emoji || '···'
)

const insight = computed(() =>
  record.value
    ? useFeedbackInsight(record.value.category, record.value.amount)
    : null
)

onLoad((query) => {
  recordId.value = (query?.id as string) || ''
  if (!record.value) {
    uni.switchTab({ url: '/pages/record/index' })
    return
  }
  track('record_feedback_view', { category: record.value.category })
  trySubscribeAfterRecord()
})

function changeNature(n: RecordNature) {
  if (record.value) recordStore.updateNature(record.value.id, n)
}

function finish() {
  uni.switchTab({ url: '/pages/record/index' })
}

function again() {
  uni.redirectTo({ url: '/pages/record/quick' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-lg $spacing-md; }
.success { font-size: $font-size-xl; font-weight: 700; text-align: center; margin-bottom: $spacing-lg; }
.summary { margin-bottom: $spacing-md; }
.main { font-size: $font-size-lg; font-weight: 600; display: block; margin-bottom: $spacing-md; }
.budget-block { margin-bottom: $spacing-sm; font-size: $font-size-sm; color: $color-text-secondary; }
.bar { height: 12rpx; background: $color-border; border-radius: $radius-full; margin-top: $spacing-xs; overflow: hidden; }
.fill { height: 100%; background: $color-primary; border-radius: $radius-full; }
.bar.optional .fill { background: $color-warning; }
.insight { margin-top: $spacing-md; padding: $spacing-sm; background: rgba(108,92,231,0.08); border-radius: $radius-sm; font-size: $font-size-sm; }
.nature-edit { margin-bottom: $spacing-xl; }
.label { font-size: $font-size-sm; color: $color-text-muted; display: block; margin-bottom: $spacing-sm; }
.nature-row { display: flex; gap: $spacing-sm; }
.chip { padding: $spacing-xs $spacing-md; border-radius: $radius-full; background: $color-bg-page; font-size: $font-size-sm; }
.chip.active { background: $color-primary; color: #fff; }
.actions { display: flex; gap: $spacing-md; }
.btn { flex: 1; border-radius: $radius-full; font-size: $font-size-md; }
.btn.primary { background: $color-primary; color: #fff; border: none; }
.btn.ghost { background: $color-bg-card; color: $color-text-secondary; border: 1rpx solid $color-border; }
</style>
