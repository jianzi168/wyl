import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Category, OptionalBudget } from '@/types/models'
import { DEFAULT_CATEGORIES } from '@/constants/categories'
import { BUDGET_TEMPLATES, type BudgetTemplateKey } from '@/constants/budget-templates'
import { getStorage, setStorage } from '@/utils/storage'
import { createId } from '@/utils/id'
import { getMonthRangeBy } from '@/utils/date'
import { budgetStatus } from '@/utils/budget'
import { useUserStore } from './user'

const CATEGORIES_KEY = 'categories'
const OPTIONAL_KEY = 'optional_budget'
const LUXURY_KEY = 'luxury_monthly_limit'

export const useBudgetStore = defineStore('budget', () => {
  const categories = ref<Category[]>(
    getStorage<Category[]>(CATEGORIES_KEY, initDefaultCategories())
  )
  const optionalBudget = ref<OptionalBudget>(
    getStorage<OptionalBudget>(OPTIONAL_KEY, initDefaultOptionalBudget())
  )
  const luxuryMonthlyLimit = ref<number>(getStorage<number>(LUXURY_KEY, 200))

  const totalMonthlyBudget = computed(() =>
    categories.value.filter((c) => c.isVisible).reduce((sum, c) => sum + c.budget, 0)
  )

  function getRecords() {
    return getStorage<import('@/types/models').RecordItem[]>('records', [])
  }

  const optionalUsage = computed(() => {
    const { start, end } = getMonthRangeBy(new Date().getFullYear(), new Date().getMonth() + 1)
    return getRecords()
      .filter((r) => r.type === 'expense' && r.nature === '可选' && r.timestamp >= start && r.timestamp <= end)
      .reduce((sum, r) => sum + r.amount, 0)
  })

  const optionalRemaining = computed(() =>
    Math.max(0, optionalBudget.value.annualLimit / 12 - optionalUsage.value)
  )

  const optionalUsagePercent = computed(() => {
    const limit = optionalBudget.value.annualLimit / 12
    return limit > 0 ? (optionalUsage.value / limit) * 100 : 0
  })

  function initDefaultCategories(): Category[] {
    const userId = useUserStore().user?.id || 'guest'
    return DEFAULT_CATEGORIES.map((c) => ({
      ...c,
      id: createId('cat'),
      userId,
    }))
  }

  function initDefaultOptionalBudget(): OptionalBudget {
    return {
      userId: useUserStore().user?.id || 'guest',
      year: new Date().getFullYear(),
      annualLimit: 6000,
      warnPercent: 80,
      currentUsage: 0,
    }
  }

  function persist() {
    setStorage(CATEGORIES_KEY, categories.value)
    setStorage(OPTIONAL_KEY, optionalBudget.value)
    setStorage(LUXURY_KEY, luxuryMonthlyLimit.value)
  }

  function getCategorySpent(categoryName: string, year?: number, month?: number): number {
    const records = getRecords()
    let start: number, end: number
    if (year && month) {
      const range = getMonthRangeBy(year, month)
      start = range.start
      end = range.end
    } else {
      const range = getMonthRangeBy(new Date().getFullYear(), new Date().getMonth() + 1)
      start = range.start
      end = range.end
    }
    return records
      .filter(
        (r) =>
          r.type === 'expense' &&
          r.category === categoryName &&
          r.timestamp >= start &&
          r.timestamp <= end
      )
      .reduce((sum, r) => sum + r.amount, 0)
  }

  function getCategoryStatus(categoryName: string) {
    const cat = categories.value.find((c) => c.name === categoryName)
    const spent = getCategorySpent(categoryName)
    return budgetStatus(spent, cat?.budget || 0)
  }

  function wouldExceedCategory(categoryName: string, amount: number) {
    const status = getCategoryStatus(categoryName)
    return status.budget > 0 && status.spent + amount > status.budget
  }

  function applyTemplate(monthlyIncome: number, template: BudgetTemplateKey) {
    const tpl = BUDGET_TEMPLATES[template]
    categories.value = categories.value.map((c) => ({
      ...c,
      budget: Math.round(monthlyIncome * (tpl.ratios[c.name as keyof typeof tpl.ratios] || 0.05)),
    }))
    optionalBudget.value = {
      ...optionalBudget.value,
      annualLimit: Math.round(monthlyIncome * tpl.optionalRatio * 12),
    }
    persist()
  }

  function updateCategory(id: string, patch: Partial<Category>) {
    const idx = categories.value.findIndex((c) => c.id === id)
    if (idx >= 0) {
      categories.value[idx] = { ...categories.value[idx], ...patch }
      persist()
    }
  }

  function toggleCategoryVisible(id: string) {
    const cat = categories.value.find((c) => c.id === id)
    if (cat) updateCategory(id, { isVisible: !cat.isVisible })
  }

  function setOptionalBudget(annualLimit: number, warnPercent = 80) {
    optionalBudget.value = { ...optionalBudget.value, annualLimit, warnPercent }
    persist()
  }

  return {
    categories,
    optionalBudget,
    luxuryMonthlyLimit,
    totalMonthlyBudget,
    optionalUsage,
    optionalRemaining,
    optionalUsagePercent,
    getCategorySpent,
    getCategoryStatus,
    wouldExceedCategory,
    applyTemplate,
    updateCategory,
    toggleCategoryVisible,
    setOptionalBudget,
    persist,
  }
})
