<template>
  <view class="page">
    <view class="section card">
      <text class="section-title">分类月预算</text>
      <view
        v-for="cat in budgetStore.categories"
        :key="cat.id"
        class="row"
        :class="{ highlight: highlightCategory === cat.name }"
      >
        <text>{{ cat.emoji }} {{ cat.name }}</text>
        <input
          class="budget-input"
          type="digit"
          :value="String(cat.budget)"
          @blur="(e: any) => updateBudget(cat.id, e.detail.value)"
        />
      </view>
    </view>
    <view class="section card">
      <text class="section-title">🎯 月度存款目标</text>
      <input
        class="budget-input full"
        type="digit"
        :value="savingsGoalStr"
        @blur="onSavingsBlur"
        placeholder="如 2000（收入-支出）"
      />
      <text class="hint">达成后可解锁「说到做到」成就</text>
    </view>
    <view class="section card">
      <text class="section-title">🟡 年度可选预算</text>
      <input
        class="budget-input full"
        type="digit"
        :value="String(budgetStore.optionalBudget.annualLimit)"
        @blur="(e: any) => budgetStore.setOptionalBudget(parseFloat(e.detail.value) || 0)"
      />
      <text class="hint">月均约 ¥{{ (budgetStore.optionalBudget.annualLimit / 12).toFixed(0) }}</text>
    </view>
    <button class="save" @tap="save">保存</button>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { useBudgetStore, useUserStore } from '@/stores'

const budgetStore = useBudgetStore()
const userStore = useUserStore()
const highlightCategory = ref('')
const savingsGoalStr = ref(String(userStore.user?.savingsGoal || ''))

onLoad((query) => {
  highlightCategory.value = (query?.category as string) || ''
})

function onSavingsBlur(e: Event) {
  const v = parseFloat((e as unknown as { detail: { value: string } }).detail.value) || 0
  savingsGoalStr.value = String(v)
  userStore.updateProfile({ savingsGoal: v || undefined })
}

function updateBudget(id: string, value: string) {
  const v = parseFloat(value) || 0
  budgetStore.updateCategory(id, { budget: v })
}

function save() {
  budgetStore.persist()
  uni.showToast({ title: '已保存', icon: 'success' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { padding: $spacing-md; }
.section { margin-bottom: $spacing-md; }
.section-title { font-weight: 600; display: block; margin-bottom: $spacing-md; }
.row { display: flex; justify-content: space-between; align-items: center; margin-bottom: $spacing-sm; font-size: $font-size-sm; padding: $spacing-xs; border-radius: $radius-sm; }
.row.highlight { background: rgba(108, 92, 231, 0.1); }
.budget-input { width: 160rpx; text-align: right; background: $color-bg-page; padding: $spacing-xs $spacing-sm; border-radius: $radius-sm; }
.budget-input.full { width: 100%; text-align: left; margin-bottom: $spacing-xs; }
.hint { font-size: $font-size-xs; color: $color-text-muted; }
.save { background: $color-primary; color: #fff; border-radius: $radius-full; border: none; }
</style>
