<template>
  <view class="page">
    <view class="tabs card">
      <text class="tab" :class="{ active: loanType === 'mortgage' }" @tap="loanType = 'mortgage'">房贷</text>
      <text class="tab" :class="{ active: loanType === 'car' }" @tap="loanType = 'car'">车贷</text>
    </view>

    <view class="form card">
      <view class="row">
        <text class="label">{{ loanType === 'mortgage' ? '贷款总额（万）' : '贷款金额（万）' }}</text>
        <input class="input" type="digit" v-model="principalWan" placeholder="如 100" />
      </view>
      <view class="row">
        <text class="label">年利率（%）</text>
        <input class="input" type="digit" v-model="annualRate" placeholder="如 3.85" />
      </view>
      <view class="row">
        <text class="label">{{ loanType === 'mortgage' ? '贷款年限' : '贷款期数（月）' }}</text>
        <input class="input" type="number" v-model="termInput" :placeholder="loanType === 'mortgage' ? '如 30' : '如 36'" />
      </view>
      <view class="row">
        <text class="label">还款方式</text>
        <view class="method-row">
          <text class="chip" :class="{ active: method === 'equal_payment' }" @tap="method = 'equal_payment'">等额本息</text>
          <text class="chip" :class="{ active: method === 'equal_principal' }" @tap="method = 'equal_principal'">等额本金</text>
        </view>
      </view>
      <button class="calc-btn" @tap="calculate">计算</button>
    </view>

    <view v-if="result" class="result card">
      <text class="result-title">计算结果</text>
      <view class="result-row">
        <text>首期/月均还款</text>
        <text class="highlight">¥{{ formatMoney(result.monthlyPayment) }}</text>
      </view>
      <view class="result-row">
        <text>还款总额</text>
        <text>¥{{ formatMoney(result.totalPayment) }}</text>
      </view>
      <view class="result-row">
        <text>利息总额</text>
        <text>¥{{ formatMoney(result.totalInterest) }}</text>
      </view>
      <button class="sync-btn" @tap="syncRecurring">同步为固定支出</button>
      <text class="hint">将月供添加至「固定收支」，每月自动入账</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { calculateLoan, type LoanResult, type RepayMethod } from '@/utils/loan-calculator'
import { useRecurringStore } from '@/stores/recurring'
import { formatMoney } from '@/utils/date'
import { ensureLoggedIn } from '@/utils/auth-guard'
import { track } from '@/utils/analytics'

const loanType = ref<'mortgage' | 'car'>('mortgage')
const principalWan = ref('100')
const annualRate = ref('3.85')
const termInput = ref('30')
const method = ref<RepayMethod>('equal_payment')
const result = ref<LoanResult | null>(null)

function calculate() {
  try {
    const months =
      loanType.value === 'mortgage' ? Number(termInput.value) * 12 : Number(termInput.value)
    result.value = calculateLoan({
      principal: parseFloat(principalWan.value) * 10000,
      annualRate: parseFloat(annualRate.value),
      months,
      method: method.value,
    })
    track('loan_calc', { type: loanType.value })
  } catch (err) {
    uni.showToast({ title: err instanceof Error ? err.message : '计算失败', icon: 'none' })
  }
}

function syncRecurring() {
  if (!result.value) return
  const recurringStore = useRecurringStore()
  const name = loanType.value === 'mortgage' ? '房贷月供' : '车贷月供'
  const day = new Date().getDate()
  recurringStore.addItem({
    name,
    amount: result.value.monthlyPayment,
    type: 'expense',
    category: loanType.value === 'mortgage' ? '账单' : '出行',
    repeatRule: 'monthly',
    dayOfMonth: Math.min(day, 28),
    isActive: true,
    nextTrigger: Date.now(),
  })
  track('loan_sync_recurring', { type: loanType.value })
  uni.showToast({ title: '已添加固定支出', icon: 'success' })
}

onShow(() => ensureLoggedIn())
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.tabs { display: flex; margin-bottom: $spacing-md; padding: $spacing-xs; }
.tab { flex: 1; text-align: center; padding: $spacing-sm; border-radius: $radius-full; font-size: $font-size-sm; color: $color-text-secondary; }
.tab.active { background: $color-primary; color: #fff; font-weight: 600; }
.form { margin-bottom: $spacing-md; }
.row { margin-bottom: $spacing-md; }
.label { font-size: $font-size-sm; color: $color-text-secondary; display: block; margin-bottom: $spacing-xs; }
.input { background: $color-bg-page; padding: $spacing-sm; border-radius: $radius-sm; font-size: $font-size-sm; }
.method-row { display: flex; gap: $spacing-sm; }
.chip { padding: $spacing-xs $spacing-md; border-radius: $radius-full; font-size: $font-size-xs; background: $color-bg-page; border: 1rpx solid $color-border; }
.chip.active { background: rgba(108,92,231,0.12); border-color: $color-primary; color: $color-primary; }
.calc-btn { background: $color-primary; color: #fff; border: none; border-radius: $radius-full; margin-top: $spacing-sm; }
.result-title { font-weight: 600; display: block; margin-bottom: $spacing-md; }
.result-row { display: flex; justify-content: space-between; margin-bottom: $spacing-sm; font-size: $font-size-sm; }
.highlight { color: $color-expense; font-weight: 700; font-size: $font-size-lg; }
.sync-btn { margin-top: $spacing-md; background: $color-bg-card; border: 1rpx solid $color-primary; color: $color-primary; border-radius: $radius-full; font-size: $font-size-sm; }
.hint { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-top: $spacing-xs; text-align: center; }
</style>
