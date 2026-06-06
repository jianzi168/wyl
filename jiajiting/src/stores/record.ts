import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { RecordItem, RecordNature } from '@/types/models'
import { DEFAULT_NATURE } from '@/constants/categories'
import { getStorage, setStorage } from '@/utils/storage'
import { createId } from '@/utils/id'
import { getMonthRange, getLastMonthSamePeriodRange } from '@/utils/date'
import { sumExpenses } from '@/utils/budget'
import { useUserStore } from './user'
import { track } from '@/utils/analytics'

const STORAGE_KEY = 'records'

export const useRecordStore = defineStore('record', () => {
  const records = ref<RecordItem[]>(getStorage<RecordItem[]>(STORAGE_KEY, []))
  const lastCategory = ref<string>(getStorage<string>('last_category', '购物'))
  const lastNature = ref<RecordNature>(getStorage<RecordNature>('last_nature', DEFAULT_NATURE))

  const { start, end } = getMonthRange()

  const monthExpenses = computed(() =>
    records.value.filter(
      (r) => r.type === 'expense' && r.timestamp >= start && r.timestamp <= end
    )
  )

  const monthExpenseTotal = computed(() =>
    monthExpenses.value.reduce((sum, r) => sum + r.amount, 0)
  )

  const monthIncomeTotal = computed(() =>
    records.value
      .filter((r) => r.type === 'income' && r.timestamp >= start && r.timestamp <= end)
      .reduce((sum, r) => sum + r.amount, 0)
  )

  const recentRecords = computed(() =>
    [...records.value].sort((a, b) => b.timestamp - a.timestamp).slice(0, 5)
  )

  const lastMonthSamePeriodTotal = computed(() => {
    const { lastStart, lastEnd } = getLastMonthSamePeriodRange()
    return sumExpenses(records.value, lastStart, lastEnd)
  })

  const vsLastMonthDiff = computed(() => lastMonthSamePeriodTotal.value - monthExpenseTotal.value)

  function persist() {
    setStorage(STORAGE_KEY, records.value)
    setStorage('last_category', lastCategory.value)
    setStorage('last_nature', lastNature.value)
  }

  function setRecords(next: RecordItem[]) {
    records.value = next
    persist()
  }

  function addRecord(payload: {
    amount: number
    category: string
    nature?: RecordNature
    remark?: string
    type?: 'expense' | 'income'
    timestamp?: number
    source?: RecordItem['source']
    importBatchId?: string
    recurringId?: string
    skipSync?: boolean
  }) {
    const userStore = useUserStore()
    const now = Date.now()
    const nature = payload.nature ?? DEFAULT_NATURE

    const userId = userStore.user?.id || 'guest'
    const item: RecordItem = {
      id: createId('rec'),
      userId,
      amount: payload.amount,
      type: payload.type ?? 'expense',
      category: payload.category,
      nature,
      remark: payload.remark,
      source: payload.source ?? 'manual',
      importBatchId: payload.importBatchId,
      timestamp: payload.timestamp ?? now,
      isRecurring: !!payload.recurringId,
      recurringId: payload.recurringId,
      memberId: userId,
      createdAt: now,
      updatedAt: now,
    }

    records.value.unshift(item)
    lastCategory.value = payload.category
    lastNature.value = nature
    persist()

    track('record_create', { category: payload.category, source: item.source })

    if (!payload.skipSync) {
      import('./sync').then(({ useSyncStore }) => {
        const syncStore = useSyncStore()
        syncStore.queueRecordUpsert(item)
        syncStore.syncNow()
      })
      import('./achievement').then(({ useAchievementStore }) => {
        useAchievementStore().checkAfterRecord()
      })
      import('./family').then(({ useFamilyStore }) => {
        const familyStore = useFamilyStore()
        if (familyStore.canCollaborate) {
          familyStore.appendFamilyRecord(item)
        }
      })
    }

    return item
  }

  function addRecordsBatch(items: RecordItem[]) {
    records.value = [...items, ...records.value]
    persist()
    import('./sync').then(({ useSyncStore }) => {
      items.forEach((item) => useSyncStore().queueRecordUpsert(item))
      useSyncStore().syncNow()
    })
  }

  function updateRecord(id: string, patch: Partial<RecordItem>) {
    const idx = records.value.findIndex((r) => r.id === id)
    if (idx < 0) return
    records.value[idx] = { ...records.value[idx], ...patch, updatedAt: Date.now() }
    persist()
    import('./sync').then(({ useSyncStore }) => {
      useSyncStore().queueRecordUpsert(records.value[idx])
      useSyncStore().syncNow()
    })
  }

  function updateNature(id: string, nature: RecordNature) {
    updateRecord(id, { nature })
  }

  function removeRecord(id: string) {
    records.value = records.value.filter((r) => r.id !== id)
    persist()
    import('./sync').then(({ useSyncStore }) => {
      useSyncStore().queueRecordDelete(id)
      useSyncStore().syncNow()
    })
  }

  function getById(id: string) {
    return records.value.find((r) => r.id === id)
  }

  function getExpensesInRange(rangeStart: number, rangeEnd: number) {
    return records.value.filter(
      (r) => r.type === 'expense' && r.timestamp >= rangeStart && r.timestamp <= rangeEnd
    )
  }

  function clearAll() {
    records.value = []
    persist()
  }

  return {
    records,
    lastCategory,
    lastNature,
    monthExpenses,
    monthExpenseTotal,
    monthIncomeTotal,
    recentRecords,
    lastMonthSamePeriodTotal,
    vsLastMonthDiff,
    setRecords,
    addRecord,
    addRecordsBatch,
    updateRecord,
    updateNature,
    removeRecord,
    getById,
    getExpensesInRange,
    clearAll,
  }
})
