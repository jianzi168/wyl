import { useUserStore } from '@/stores/user'

const AUTH_PAGES = ['pages/auth/login', 'pages/auth/bind-phone']

export function ensureLoggedIn(): boolean {
  const userStore = useUserStore()
  if (userStore.isLoggedIn) return true

  uni.navigateTo({ url: '/pages/auth/login' })
  return false
}

export function isAuthPage(route?: string): boolean {
  if (!route) return false
  return AUTH_PAGES.some((p) => route.includes(p))
}
