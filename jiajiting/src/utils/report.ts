import type { RecordItem } from '@/types/models'
import { sumExpenses, sumByCategory } from '@/utils/budget'

export function getWeekRange(date = new Date()) {
  const d = new Date(date)
  const day = d.getDay() || 7
  const monday = new Date(d)
  monday.setDate(d.getDate() - day + 1)
  monday.setHours(0, 0, 0, 0)
  const sunday = new Date(monday)
  sunday.setDate(monday.getDate() + 6)
  sunday.setHours(23, 59, 59, 999)
  return { start: monday.getTime(), end: sunday.getTime(), monday, sunday }
}

export function getLastWeekRange(date = new Date()) {
  const d = new Date(date)
  d.setDate(d.getDate() - 7)
  return getWeekRange(d)
}

export interface DayExpense {
  date: number
  day: number
  amount: number
  label: string
}

export function getWeekDailyBreakdown(records: RecordItem[], start: number, end: number): DayExpense[] {
  const result: DayExpense[] = []
  const cursor = new Date(start)
  while (cursor.getTime() <= end) {
    const dayStart = new Date(cursor)
    dayStart.setHours(0, 0, 0, 0)
    const dayEnd = new Date(cursor)
    dayEnd.setHours(23, 59, 59, 999)
    const amount = sumExpenses(records, dayStart.getTime(), dayEnd.getTime())
    result.push({
      date: dayStart.getTime(),
      day: cursor.getDate(),
      amount,
      label: `${cursor.getMonth() + 1}/${cursor.getDate()}`,
    })
    cursor.setDate(cursor.getDate() + 1)
  }
  return result
}

export interface HeatmapCell {
  day: number
  amount: number
  level: 0 | 1 | 2 | 3
  empty: boolean
}

export function buildMonthHeatmap(
  records: RecordItem[],
  year: number,
  month: number
): HeatmapCell[][] {
  const firstDay = new Date(year, month - 1, 1)
  const daysInMonth = new Date(year, month, 0).getDate()
  const startWeekday = firstDay.getDay()
  const amounts: number[] = []

  for (let d = 1; d <= daysInMonth; d++) {
    const s = new Date(year, month - 1, d, 0, 0, 0, 0).getTime()
    const e = new Date(year, month - 1, d, 23, 59, 59, 999).getTime()
    amounts.push(sumExpenses(records, s, e))
  }

  const max = Math.max(...amounts, 1)
  const rows: HeatmapCell[][] = []
  let week: HeatmapCell[] = []

  for (let i = 0; i < startWeekday; i++) {
    week.push({ day: 0, amount: 0, level: 0, empty: true })
  }

  for (let d = 1; d <= daysInMonth; d++) {
    const amount = amounts[d - 1]
    let level: 0 | 1 | 2 | 3 = 0
    if (amount > 0) {
      const ratio = amount / max
      level = ratio > 0.66 ? 3 : ratio > 0.33 ? 2 : 1
    }
    week.push({ day: d, amount, level, empty: false })
    if (week.length === 7) {
      rows.push(week)
      week = []
    }
  }
  if (week.length) {
    while (week.length < 7) week.push({ day: 0, amount: 0, level: 0, empty: true })
    rows.push(week)
  }
  return rows
}

export function weekCategoryBreakdown(records: RecordItem[], start: number, end: number) {
  const map = sumByCategory(records, start, end)
  const total = Object.values(map).reduce((s, v) => s + v, 0) || 1
  return Object.entries(map)
    .map(([name, amount]) => ({ name, amount, percent: Math.round((amount / total) * 100) }))
    .sort((a, b) => b.amount - a.amount)
}
