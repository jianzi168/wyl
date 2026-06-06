import type { SyncQueueItem } from '@/types/auth'
import { getStorage, setStorage } from '@/utils/storage'
import { createId } from '@/utils/id'

const QUEUE_KEY = 'sync_queue'

export function loadQueue(): SyncQueueItem[] {
  return getStorage<SyncQueueItem[]>(QUEUE_KEY, [])
}

export function saveQueue(queue: SyncQueueItem[]): void {
  setStorage(QUEUE_KEY, queue)
}

export function enqueue(item: Omit<SyncQueueItem, 'id' | 'createdAt' | 'retryCount'>): void {
  const queue = loadQueue()
  const duplicateIndex = queue.findIndex(
    (q) =>
      q.entity === item.entity &&
      q.type === item.type &&
      JSON.stringify(q.payload) === JSON.stringify(item.payload)
  )
  if (duplicateIndex >= 0) {
    queue.splice(duplicateIndex, 1)
  }
  queue.push({
    ...item,
    id: createId('sync'),
    createdAt: Date.now(),
    retryCount: 0,
  })
  saveQueue(queue)
}

export function clearQueue(): void {
  saveQueue([])
}

export function pendingCount(): number {
  return loadQueue().length
}
