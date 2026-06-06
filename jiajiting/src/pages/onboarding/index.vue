<template>
  <view class="page">
    <view v-if="step === 0" class="step">
      <text class="emoji">👋</text>
      <text class="title">记完就知道还能花多少</text>
      <text class="desc">每笔消费后立刻看到预算影响，让省钱看得见</text>
      <button class="btn primary" @tap="step = 1">开始设置</button>
      <button class="btn ghost" @tap="skip">先逛逛</button>
    </view>

    <view v-else-if="step === 1" class="step">
      <text class="title">月收入与模板</text>
      <input v-model="incomeStr" class="input" type="digit" placeholder="月收入，如 8000" />
      <view class="templates">
        <view
          v-for="key in templateKeys"
          :key="key"
          class="tpl"
          :class="{ active: template === key }"
          @tap="template = key"
        >
          {{ templates[key].label }}
        </view>
      </view>
      <button class="btn primary" @tap="step = 2">下一步</button>
    </view>

    <view v-else class="step">
      <text class="title">可选预算</text>
      <text class="desc">年度可选支出上限（默认月收入 × {{ ratioLabel }}）</text>
      <input v-model="optionalStr" class="input" type="digit" :placeholder="String(suggestedOptional)" />
      <text class="hint">预警：消耗 80% 时提醒</text>
      <input v-model="savingsGoalStr" class="input" type="digit" placeholder="月度存款目标（可选）" />
      <button class="btn primary" @tap="finish">完成，记第一笔</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { BUDGET_TEMPLATES, type BudgetTemplateKey } from '@/constants/budget-templates'
import { useUserStore, useBudgetStore, useAchievementStore } from '@/stores'
import { track } from '@/utils/analytics'

const userStore = useUserStore()
const budgetStore = useBudgetStore()
const achievementStore = useAchievementStore()

const step = ref(0)
const incomeStr = ref('8000')
const optionalStr = ref('')
const savingsGoalStr = ref('')
const template = ref<BudgetTemplateKey>('worker')
const templates = BUDGET_TEMPLATES
const templateKeys = Object.keys(BUDGET_TEMPLATES) as BudgetTemplateKey[]

const income = computed(() => parseFloat(incomeStr.value) || 8000)
const ratioLabel = computed(() => `${Math.round(BUDGET_TEMPLATES[template.value].optionalRatio * 100)}%`)
const suggestedOptional = computed(() =>
  Math.round(income.value * BUDGET_TEMPLATES[template.value].optionalRatio * 12)
)

function skip() {
  uni.switchTab({ url: '/pages/record/index' })
}

function finish() {
  const monthlyIncome = income.value
  budgetStore.applyTemplate(monthlyIncome, template.value)
  const annual = parseFloat(optionalStr.value) || suggestedOptional.value
  budgetStore.setOptionalBudget(annual)
  const savingsGoal = parseFloat(savingsGoalStr.value) || undefined
  userStore.completeOnboarding(monthlyIncome, savingsGoal)
  achievementStore.checkOnboarding()
  track('budget_onboarding_complete', { template: template.value })
  uni.redirectTo({ url: '/pages/record/quick' })
}
</script>

<style lang="scss" scoped>
@import '@/styles/variables.scss';
.page { min-height: 100vh; padding: $spacing-xl $spacing-lg; display: flex; align-items: center; }
.step { width: 100%; text-align: center; }
.emoji { font-size: 80rpx; display: block; margin-bottom: $spacing-md; }
.title { font-size: $font-size-xl; font-weight: 700; display: block; margin-bottom: $spacing-sm; }
.desc { font-size: $font-size-sm; color: $color-text-secondary; display: block; margin-bottom: $spacing-xl; line-height: 1.6; }
.input { background: $color-bg-card; border-radius: $radius-md; padding: $spacing-md; margin-bottom: $spacing-md; text-align: left; }
.templates { display: flex; justify-content: center; gap: $spacing-sm; margin-bottom: $spacing-xl; }
.tpl { padding: $spacing-sm $spacing-lg; border-radius: $radius-full; background: $color-bg-card; font-size: $font-size-sm; }
.tpl.active { background: $color-primary; color: #fff; }
.hint { font-size: $font-size-xs; color: $color-text-muted; display: block; margin-bottom: $spacing-lg; }
.btn { margin-bottom: $spacing-sm; border-radius: $radius-full; }
.btn.primary { background: $color-primary; color: #fff; border: none; }
.btn.ghost { background: transparent; color: $color-text-muted; border: none; }
</style>
