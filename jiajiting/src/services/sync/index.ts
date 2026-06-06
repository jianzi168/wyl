import { isCloudMode } from '@/services/config'
import { request } from '@/services/http'
import { getStorage, setStorage } from '@/utils/storage'
import type { RecordItem } from '@/types/models'
import type { SyncQueueItem } from '@/types/auth'
import { mergeRecords } from './merge'
import { loadQueue, clearQueue, enqueue, pendingCount } from './queue'

interface PullResponse {
  records: RecordItem[]
  serverTime: number
}

interface PushResponse {
  success: boolean
  failedIds?: string[]
}

const cloudCacheKey = (userId: string) => `cloud_cache_records_${userId}`

/** 云端拉取记账记录 */
export async function pullRecords(token: string, userId: string): Promise<RecordItem[]> {
  if (!isCloudMode()) {
    return getStorage<RecordItem[]>(cloudCacheKey(userId), [])
  }

  const data = await request<PullResponse>(`/sync/records?userId=${userId}`, { token })
  return data.records
}

/** 云端推送队列中的变更 */
export async function pushQueue(token: string, queue: SyncQueueItem[]): Promise<void> {
  if (!queue.length || !isCloudMode()) return

  await request<PushResponse>('/sync/records/batch', {
    method: 'POST',
    token,
    data: { operations: queue },
  })
}

/** 本地模式：将合并结果写入模拟云端缓存 */
export function saveLocalCloudCache(userId: string, records: RecordItem[]): void {
  setStorage(cloudCacheKey(userId), records)
}

export async function syncRecords(
  token: string,
  userId: string,
  localRecords: RecordItem[]
): Promise<RecordItem[]> {
  const queue = loadQueue()
  const remote = await pullRecords(token, userId)
  const merged = mergeRecords(localRecords, remote)

  if (isCloudMode()) {
    await pushQueue(token, queue)
  } else {
    saveLocalCloudCache(userId, merged)
  }

  clearQueue()
  return merged
}

export { mergeRecords, enqueue, loadQueue, clearQueue, pendingCount }
