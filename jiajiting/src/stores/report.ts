import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMonthRangeBy, getRecentMonths } from '@/utils/date'
import { sumExpenses, sumByCategory } from '@/utils/budget'
import { predictMonthExpense } from '@/utils/prediction'
import { useRecordStore } from './record'
import { useBudgetStore } from './budget'

export const useReportStore = defineStore('report', () => {
  const now = new Date()
  const selectedYear = ref(now.getFullYear())
  const selectedMonth = ref(now.getMonth() + 1)

  const range = computed(() => getMonthRangeBy(selectedYear.value, selectedMonth.value))

  const monthExpenseTotal = computed(() => {
    const recordStore = useRecordStore()
    return sumExpenses(recordStore.records, range.value.start, range.value.end)
  })

  const monthIncomeTotal = computed(() => {
    const recordStore = useRecordStore()
    return recordStore.records
      .filter((r) => r.type === 'income' && r.timestamp >= range.value.start && r.timestamp <= range.value.end)
      .reduce((s, r) => s + r.amount, 0)
  })

  const categoryBreakdown = computed(() => {
    const recordStore = useRecordStore()
    const map = sumByCategory(recordStore.records, range.value.start, range.value.end)
    const total = monthExpenseTotal.value || 1
    return Object.entries(map)
      .map(([name, amount]) => ({
        name,
        amount,
        percent: Math.round((amount / total) * 100),
        color: useBudgetStore().categories.find((c) => c.name === name)?.iconColor || '#BDC3C7',
        emoji: useBudgetStore().categories.find((c) => c.name === name)?.emoji || '···',
      }))
      .sort((a, b) => b.amount - a.amount)
  })

  const trendData = computed(() => {
    const recordStore = useRecordStore()
    return getRecentMonths(6).map((m) => {
      const r = getMonthRangeBy(m.year, m.month)
      return {
        label: m.label,
        total: sumExpenses(recordStore.records, r.start, r.end),
        food: sumByCategory(recordStore.records, r.start, r.end)['餐饮'] || 0,
        shop: sumByCategory(recordStore.records, r.start, r.end)['购物'] || 0,
      }
    })
  })

  const prediction = computed(() => {
    if (selectedYear.value !== now.getFullYear() || selectedMonth.value !== now.getMonth() + 1) {
      return null
    }
    return predictMonthExpense(useRecordStore().records)
  })

  function setMonth(year: number, month: number) {
    selectedYear.value = year
    selectedMonth.value = month
  }

  function prevMonth() {
    if (selectedMonth.value === 1) {
      selectedYear.value -= 1
      selectedMonth.value = 12
    } else {
      selectedMonth.value -= 1
    }
  }

  function nextMonth() {
    const n = new Date()
    const cur = selectedYear.value * 12 + selectedMonth.value
    const nowVal = n.getFullYear() * 12 + (n.getMonth() + 1)
    if (cur >= nowVal) return
    if (selectedMonth.value === 12) {
      selectedYear.value += 1
      selectedMonth.value = 1
    } else {
      selectedMonth.value += 1
    }
  }

  return {
    selectedYear,
    selectedMonth,
    range,
    monthExpenseTotal,
    monthIncomeTotal,
    categoryBreakdown,
    trendData,
    prediction,
    setMonth,
    prevMonth,
    nextMonth,
  }
})
