/** 日期工具 */

export function getMonthRange(date = new Date()) {
  const year = date.getFullYear()
  const month = date.getMonth()
  const start = new Date(year, month, 1).getTime()
  const end = new Date(year, month + 1, 0, 23, 59, 59, 999).getTime()
  return { start, end, year, month: month + 1 }
}

export function getMonthRangeBy(year: number, month: number) {
  const start = new Date(year, month - 1, 1).getTime()
  const end = new Date(year, month, 0, 23, 59, 59, 999).getTime()
  return { start, end, year, month }
}

/** 上月同期：本月1日～今天对应的上月1日～上月同日 */
export function getLastMonthSamePeriodRange(now = new Date()) {
  const day = now.getDate()
  const thisMonth = getMonthRange(now)
  const lastMonthDate = new Date(now.getFullYear(), now.getMonth() - 1, 1)
  const lastStart = lastMonthDate.getTime()
  const lastEndDay = Math.min(day, new Date(now.getFullYear(), now.getMonth(), 0).getDate())
  const lastEnd = new Date(
    now.getFullYear(),
    now.getMonth() - 1,
    lastEndDay,
    23,
    59,
    59,
    999
  ).getTime()
  const thisEnd = now.getTime()
  return { thisStart: thisMonth.start, thisEnd, lastStart, lastEnd }
}

export function getRecentMonths(count: number, from = new Date()): Array<{ year: number; month: number; label: string }> {
  const result = []
  for (let i = count - 1; i >= 0; i--) {
    const d = new Date(from.getFullYear(), from.getMonth() - i, 1)
    result.push({
      year: d.getFullYear(),
      month: d.getMonth() + 1,
      label: `${d.getMonth() + 1}月`,
    })
  }
  return result
}

export function formatDate(timestamp: number): string {
  const d = new Date(timestamp)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}-${day}`
}

export function formatDateTime(timestamp: number): string {
  const d = new Date(timestamp)
  return `${formatDate(timestamp)} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

export function formatMoney(amount: number): string {
  return amount.toFixed(2)
}

export function monthLabel(year: number, month: number): string {
  return `${year}年${month}月`
}
