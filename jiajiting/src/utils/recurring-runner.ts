import type { RecurringItem, RecordItem } from '@/types/models'
import { createId } from '@/utils/id'

export function shouldTriggerToday(item: RecurringItem, today = new Date()): boolean {
  if (!item.isActive) return false
  const day = today.getDate()
  if (item.repeatRule === 'monthly' && item.dayOfMonth === day) {
    if (!item.lastTriggered) return true
    const last = new Date(item.lastTriggered)
    return last.getMonth() !== today.getMonth() || last.getFullYear() !== today.getFullYear()
  }
  return false
}

export function createRecordFromRecurring(item: RecurringItem, userId: string): RecordItem {
  const now = Date.now()
  return {
    id: createId('rec'),
    userId,
    amount: item.amount,
    type: item.type,
    category: item.category,
    nature: item.type === 'income' ? '刚需' : '可选',
    remark: item.name,
    source: 'recurring',
    timestamp: now,
    isRecurring: true,
    recurringId: item.id,
    createdAt: now,
    updatedAt: now,
  }
}
