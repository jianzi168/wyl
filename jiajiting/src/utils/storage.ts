const PREFIX = 'jiajiting_'

export function getStorage<T>(key: string, fallback: T): T {
  try {
    const raw = uni.getStorageSync(PREFIX + key)
    if (raw === '' || raw === undefined || raw === null) return fallback
    return typeof raw === 'string' ? (JSON.parse(raw) as T) : (raw as T)
  } catch {
    return fallback
  }
}

export function setStorage<T>(key: string, value: T): void {
  uni.setStorageSync(PREFIX + key, JSON.stringify(value))
}

export function removeStorage(key: string): void {
  uni.removeStorageSync(PREFIX + key)
}
