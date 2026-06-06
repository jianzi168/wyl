<template>
  <view class="page" v-if="snapshot">
    <view class="header card">
      <text class="title">{{ familyStore.family?.name }} · 家庭汇总</text>
      <text class="meta">只读 · {{ familyStore.family?.members.length }} 位成员</text>
      <text class="updated">更新于 {{ formatDate(snapshot.updatedAt) }}</text>
    </view>

    <view class="summary card">
      <view class="item">
        <text class="label">本月收入</text>
        <text class="value income">¥{{ formatMoney(snapshot.monthIncome) }}</text>
      </view>
      <view class="item">
        <text class="label">本月支出</text>
        <text class="value expense">¥{{ formatMoney(snapshot.monthExpense) }}</text>
      </view>
      <view class="item">
        <text class="label">结余</text>
        <text class="value">¥{{ formatMoney(snapshot.monthIncome - snapshot.monthExpense) }}</text>
      </view>
    </view>

    <view v-if="snapshot.memberBreakdown?.length" class="card">
      <text class="section-title">成员支出（本月）</text>
      <view v-for="m in snapshot.memberBreakdown" :key="m.userId" class="row">
        <text>{{ m.nickname }}</text>
        <text>¥{{ formatMoney(m.amount) }}</text>
      </view>
    </view>

    <view class="card">
      <text class="section-title">分类 TOP</text>
      <view v-for="c in snapshot.topCategories" :key="c.name" class="row">
        <text>{{ c.name }}</text>
        <text>¥{{ formatMoney(c.amount) }} ({{ c.percent }}%)</text>
      </view>
      <text v-if="!snapshot.topCategories.length" class="empty">暂无支出</text>
    </view>

    <view class="readonly-tip card">
      <text>🔒 协作模式：仅展示家庭聚合汇总，不展示单笔明细。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useFamilyStore } from '@/stores/family'
import { formatMoney, formatDate } from '@/utils/date'
import { ensureLoggedIn } from '@/utils/auth-guard'

const familyStore = useFamilyStore()

const snapshot = computed(() => {
  if (familyStore.isOwner) return familyStore.family?.snapshot
  const fam = familyStore.family
  if (!fam) return null
  const records = familyStore.getFamilyRecords()
  const { start, end } = (() => {
    const now = new Date()
    const s = new Date(now.getFullYear(), now.getMonth(), 1).getTime()
    const e = new Date(now.getFullYear(), now.getMonth() + 1, 0, 23, 59, 59, 999).getTime()
    return { start: s, end: e }
  })()
  return {
    ...fam.snapshot,
    memberBreakdown: fam.members.map((m) => ({
      userId: m.userId,
      nickname: m.nickname,
      amount: records
        .filter((r) => (r.memberId || r.userId) === m.userId && r.type === 'expense')
        .filter((r) => r.timestamp >= start && r.timestamp <= end)
        .reduce((s, r) => s + r.amount, 0),
    })),
  }
})

onShow(() => {
  if (!ensureLoggedIn()) return
  if (!familyStore.isMember) {
    uni.redirectTo({ url: '/pages/settings/family' })
    return
  }
  if (familyStore.isOwner) familyStore.refreshSnapshot()
})
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.title { font-size: $font-size-lg; font-weight: 700; display: block; }
.meta { font-size: $font-size-sm; color: $color-text-secondary; display: block; margin-top: 4rpx; }
.updated { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: $spacing-xs; }
.summary { display: flex; margin: $spacing-md 0; }
.item { flex: 1; text-align: center; }
.label { font-size: $font-size-xs; color: $color-text-muted; display: block; }
.value { font-size: $font-size-md; font-weight: 600; display: block; margin-top: 4rpx; }
.income { color: $color-income; }
.expense { color: $color-expense; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-md; }
.row { display: flex; justify-content: space-between; font-size: $font-size-sm; margin-bottom: $spacing-xs; }
.empty { font-size: $font-size-sm; color: $color-text-muted; }
.readonly-tip { font-size: $font-size-sm; color: $color-text-secondary; line-height: 1.6; }
</style>
