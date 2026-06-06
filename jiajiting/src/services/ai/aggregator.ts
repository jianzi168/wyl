import type { RecordItem } from '@/types/models'
import type { WeeklyAggregate, MonthlyAggregate, QaAggregate } from './types'
import { getWeekRange, getLastWeekRange, weekCategoryBreakdown } from '@/utils/report'
import { sumExpenses } from '@/utils/budget'
import { getMonthRange, monthLabel } from '@/utils/date'

function topCategories(records: RecordItem[], start: number, end: number, limit = 3) {
  const items = weekCategoryBreakdown(records, start, end)
  return items.slice(0, limit).map((c) => ({
    name: c.name,
    amount: c.amount,
    percent: c.percent,
  }))
}

export function buildWeeklyAggregate(records: RecordItem[], date = new Date()): WeeklyAggregate {
  const range = getWeekRange(date)
  const lastRange = getLastWeekRange(date)
  const weekTotal = sumExpenses(records, range.start, range.end)
  const lastWeekTotal = sumExpenses(records, lastRange.start, lastRange.end)
  const diffPercent = lastWeekTotal
    ? Math.round(((weekTotal - lastWeekTotal) / lastWeekTotal) * 100)
    : 0
  const { monday, sunday } = range
  const weekLabel = `${monday.getMonth() + 1}.${monday.getDate()} - ${sunday.getMonth() + 1}.${sunday.getDate()}`
  return {
    weekLabel,
    weekTotal,
    lastWeekTotal,
    diffPercent,
    topCategories: topCategories(records, range.start, range.end),
  }
}

export function buildMonthlyAggregate(
  records: RecordItem[],
  year: number,
  month: number,
  totalBudget: number,
  optionalUsagePercent: number
): MonthlyAggregate {
  const start = new Date(year, month - 1, 1).getTime()
  const end = new Date(year, month, 0, 23, 59, 59, 999).getTime()
  const income = records
    .filter((r) => r.type === 'income' && r.timestamp >= start && r.timestamp <= end)
    .reduce((s, r) => s + r.amount, 0)
  const expense = sumExpenses(records, start, end)
  const budgetAchievementRate =
    totalBudget > 0 ? Math.min(100, Math.round((1 - expense / totalBudget) * 100)) : 0
  return {
    monthLabel: monthLabel(year, month),
    income,
    expense,
    balance: income - expense,
    budgetAchievementRate,
    optionalUsagePercent,
    topCategories: topCategories(records, start, end, 5),
  }
}

export function buildQaAggregate(records: RecordItem[]): QaAggregate {
  const { start, end } = getMonthRange()
  const monthExpense = sumExpenses(records, start, end)
  const lastStart = new Date(start)
  lastStart.setMonth(lastStart.getMonth() - 1)
  const lastEnd = new Date(end)
  lastEnd.setMonth(lastEnd.getMonth() - 1)
  const lastMonthExpense = sumExpenses(records, lastStart.getTime(), lastEnd.getTime())

  const cats = weekCategoryBreakdown(records, start, end)
  const top = cats[0] || { name: '无', amount: 0, percent: 0 }

  const recent3MonthTrend: { label: string; total: number }[] = []
  const now = new Date()
  for (let i = 2; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const y = d.getFullYear()
    const m = d.getMonth() + 1
    const s = new Date(y, m - 1, 1).getTime()
    const e = new Date(y, m, 0, 23, 59, 59, 999).getTime()
    recent3MonthTrend.push({
      label: `${m}月`,
      total: sumExpenses(records, s, e),
    })
  }

  return {
    monthExpense,
    lastMonthExpense,
    optionalUsagePercent: 0,
    topCategory: top.name,
    topCategoryAmount: top.amount,
    recent3MonthTrend,
  }
}
