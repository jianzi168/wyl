import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { SyncStatus } from '@/types/auth'
import { syncRecords, pendingCount as getPendingCount, enqueue } from '@/services/sync'
import { getStorage, setStorage } from '@/utils/storage'
import { useUserStore } from './user'

const LAST_SYNC_KEY = 'last_synced_at'

export const useSyncStore = defineStore('sync', () => {
  const status = ref<SyncStatus>('idle')
  const lastSyncedAt = ref<number | null>(getStorage<number | null>(LAST_SYNC_KEY, null))
  const errorMessage = ref<string | null>(null)

  const pending = computed(() => getPendingCount())

  function setStatus(next: SyncStatus, error?: string) {
    status.value = next
    errorMessage.value = error || null
  }

  async function syncNow(): Promise<boolean> {
    const userStore = useUserStore()
    const { useRecordStore } = await import('./record')
    const recordStore = useRecordStore()

    if (!userStore.isLoggedIn || !userStore.token) {
      setStatus('idle')
      return false
    }

    setStatus('syncing')

    try {
      const network = await getNetworkType()
      if (network === 'none') {
        setStatus('offline')
        return false
      }

      const userId = userStore.user!.id
      const merged = await syncRecords(userStore.token, userId, recordStore.records)
      recordStore.setRecords(merged)

      lastSyncedAt.value = Date.now()
      setStorage(LAST_SYNC_KEY, lastSyncedAt.value)
      setStatus('idle')
      return true
    } catch (err) {
      const message = err instanceof Error ? err.message : '同步失败'
      setStatus('error', message)
      return false
    }
  }

  function queueRecordUpsert(record: Parameters<typeof enqueue>[0]['payload']) {
    enqueue({ entity: 'record', type: 'upsert', payload: record })
  }

  function queueRecordDelete(recordId: string) {
    enqueue({ entity: 'record', type: 'delete', payload: { id: recordId } })
  }

  return {
    status,
    lastSyncedAt,
    errorMessage,
    pending,
    syncNow,
    queueRecordUpsert,
    queueRecordDelete,
  }
})

function getNetworkType(): Promise<string> {
  return new Promise((resolve) => {
    uni.getNetworkType({
      success: (res) => resolve(res.networkType),
      fail: () => resolve('unknown'),
    })
  })
}
