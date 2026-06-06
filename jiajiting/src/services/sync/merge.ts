import type { RecordItem } from '@/types/models'

/**
 * 按 id 合并本地与云端记录，updatedAt 较新者胜出。
 * 相等时保留本地（优先未同步的编辑）。
 */
export function mergeRecords(local: RecordItem[], remote: RecordItem[]): RecordItem[] {
  const map = new Map<string, RecordItem>()

  for (const item of remote) {
    map.set(item.id, item)
  }

  for (const item of local) {
    const existing = map.get(item.id)
    if (!existing) {
      map.set(item.id, item)
      continue
    }
    if (item.updatedAt >= existing.updatedAt) {
      map.set(item.id, item)
    }
  }

  return Array.from(map.values()).sort((a, b) => b.timestamp - a.timestamp)
}
