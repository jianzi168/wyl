import { getStorage, setStorage } from '@/utils/storage'

const KEY = 'premium_active'
const EXPIRE_KEY = 'premium_expire_at'

export function isPremium(): boolean {
  if (!getStorage(KEY, false)) return false
  const expire = getStorage<number>(EXPIRE_KEY, 0)
  if (expire && Date.now() > expire) {
    setPremium(false)
    return false
  }
  return true
}

export function setPremium(active: boolean, days = 365) {
  setStorage(KEY, active)
  setStorage(EXPIRE_KEY, active ? Date.now() + days * 86400000 : 0)
}

export function premiumExpireLabel(): string {
  const expire = getStorage<number>(EXPIRE_KEY, 0)
  if (!expire) return ''
  const d = new Date(expire)
  return `${d.getFullYear()}-${d.getMonth() + 1}-${d.getDate()}`
}
