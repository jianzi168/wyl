import { getStorage, setStorage } from '@/utils/storage'

const KEY = 'share_report_count'

export function getShareCount(): number {
  return getStorage(KEY, 0)
}

export function incrementShareCount(): number {
  const next = getShareCount() + 1
  setStorage(KEY, next)
  return next
}
