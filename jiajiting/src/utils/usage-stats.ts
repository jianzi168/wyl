import { getStorage, setStorage } from '@/utils/storage'

export function incrementStat(key: string): number {
  const next = getStorage(key, 0) + 1
  setStorage(key, next)
  return next
}

export function getStat(key: string): number {
  return getStorage(key, 0)
}
