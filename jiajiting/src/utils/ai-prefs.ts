import { getStorage, setStorage } from '@/utils/storage'

const KEY = 'ai_enabled'

export function isAiEnabled(): boolean {
  return getStorage(KEY, true)
}

export function setAiEnabled(enabled: boolean) {
  setStorage(KEY, enabled)
}
