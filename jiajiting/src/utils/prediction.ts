import type { RecordItem } from '@/types/models'
import { getMonthRange } from '@/utils/date'
import { sumExpenses } from '@/utils/budget'

/** 简版月度支出预测 */
export function predictMonthExpense(records: RecordItem[], now = new Date()) {
  const { start, end, year, month } = getMonthRange(now)
  const spent = sumExpenses(records, start, now.getTime())
  const day = now.getDate()
  const daysInMonth = new Date(year, month, 0).getDate()
  const dailyAvg = day > 0 ? spent / day : 0
  const predicted = dailyAvg * daysInMonth
  const daysLeft = daysInMonth - day
  return {
    spent,
    predicted: Math.round(predicted * 100) / 100,
    dailyAvg: Math.round(dailyAvg * 100) / 100,
    daysLeft,
    progress: Math.round((day / daysInMonth) * 100),
  }
}
