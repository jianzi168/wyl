import type { RecordItem } from '@/types/models'

export function sumExpenses(records: RecordItem[], start: number, end: number): number {
  return records
    .filter((r) => r.type === 'expense' && r.timestamp >= start && r.timestamp <= end)
    .reduce((s, r) => s + r.amount, 0)
}

export function sumByCategory(
  records: RecordItem[],
  start: number,
  end: number
): Record<string, number> {
  const map: Record<string, number> = {}
  records
    .filter((r) => r.type === 'expense' && r.timestamp >= start && r.timestamp <= end)
    .forEach((r) => {
      map[r.category] = (map[r.category] || 0) + r.amount
    })
  return map
}

export function budgetStatus(spent: number, budget: number) {
  const percent = budget > 0 ? Math.round((spent / budget) * 100) : 0
  const remaining = budget - spent
  const isWarning = percent >= 80 && percent < 100
  const isOver = percent >= 100
  return { spent, budget, percent, remaining, isWarning, isOver }
}
