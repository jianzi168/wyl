import { computed } from 'vue'
import { pickInsight, type InsightContext } from '@/constants/insight-rules'
import { useRecordStore } from '@/stores/record'
import { useBudgetStore } from '@/stores/budget'

export function useInsightContext(): InsightContext {
  const recordStore = useRecordStore()
  const budgetStore = useBudgetStore()
  const categorySpent: Record<string, number> = {}
  const categoryBudget: Record<string, number> = {}
  budgetStore.categories.forEach((c) => {
    categorySpent[c.name] = budgetStore.getCategorySpent(c.name)
    categoryBudget[c.name] = c.budget
  })
  return {
    monthExpenseTotal: recordStore.monthExpenseTotal,
    lastMonthSamePeriodTotal: recordStore.lastMonthSamePeriodTotal,
    categorySpent,
    categoryBudget,
    optionalUsagePercent: budgetStore.optionalUsagePercent,
    optionalRemaining: budgetStore.optionalRemaining,
  }
}

export function useHomeInsight() {
  const ctx = computed(() => useInsightContext())
  return computed(() => pickInsight(ctx.value, 'card'))
}

export function useFeedbackInsight(lastCategory: string, lastAmount: number) {
  const base = useInsightContext()
  return pickInsight(
    { ...base, lastRecordCategory: lastCategory, lastRecordAmount: lastAmount },
    'inline'
  )
}
