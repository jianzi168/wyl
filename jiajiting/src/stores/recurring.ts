import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RecurringItem } from '@/types/models'
import { getStorage, setStorage } from '@/utils/storage'
import { createId } from '@/utils/id'
import { shouldTriggerToday, createRecordFromRecurring } from '@/utils/recurring-runner'
import { useUserStore } from './user'
import { useRecordStore } from './record'

const KEY = 'recurring_items'

export const useRecurringStore = defineStore('recurring', () => {
  const items = ref<RecurringItem[]>(getStorage<RecurringItem[]>(KEY, []))

  const activeItems = computed(() => items.value.filter((i) => i.isActive))

  const thisMonthCalendar = computed(() => {
    const now = new Date()
    const y = now.getFullYear()
    const m = now.getMonth()
    return items.value
      .filter((i) => i.isActive && i.repeatRule === 'monthly')
      .map((i) => ({
        ...i,
        dateLabel: `${m + 1}月${i.dayOfMonth}日`,
        signedAmount: i.type === 'income' ? i.amount : -i.amount,
      }))
      .sort((a, b) => a.dayOfMonth - b.dayOfMonth)
  })

  function persist() {
    setStorage(KEY, items.value)
  }

  function addItem(payload: Omit<RecurringItem, 'id' | 'lastTriggered' | 'userId'>) {
    const item: RecurringItem = {
      ...payload,
      userId: useUserStore().user?.id || 'guest',
      id: createId('recur'),
      lastTriggered: undefined,
    }
    items.value.push(item)
    persist()
    return item
  }

  function updateItem(id: string, patch: Partial<RecurringItem>) {
    const idx = items.value.findIndex((i) => i.id === id)
    if (idx >= 0) {
      items.value[idx] = { ...items.value[idx], ...patch }
      persist()
    }
  }

  function removeItem(id: string) {
    items.value = items.value.filter((i) => i.id !== id)
    persist()
  }

  function runDueItems() {
    const userStore = useUserStore()
    const recordStore = useRecordStore()
    const userId = userStore.user?.id
    if (!userId) return

    const today = new Date()
    for (const item of items.value) {
      if (!shouldTriggerToday(item, today)) continue
      const exists = recordStore.records.some(
        (r) =>
          r.recurringId === item.id &&
          new Date(r.timestamp).getMonth() === today.getMonth() &&
          new Date(r.timestamp).getFullYear() === today.getFullYear()
      )
      if (exists) continue

      const rec = createRecordFromRecurring(item, userId)
      recordStore.addRecord({
        amount: rec.amount,
        category: rec.category,
        type: rec.type,
        remark: rec.remark,
        nature: rec.nature,
        timestamp: rec.timestamp,
        source: 'recurring',
        recurringId: item.id,
      })
      updateItem(item.id, { lastTriggered: Date.now(), nextTrigger: Date.now() })
    }
  }

  return {
    items,
    activeItems,
    thisMonthCalendar,
    addItem,
    updateItem,
    removeItem,
    runDueItems,
  }
})
