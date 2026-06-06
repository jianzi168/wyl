import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User } from '@/types/models'
import { getStorage, setStorage, removeStorage } from '@/utils/storage'
import * as authService from '@/services/auth'

const STORAGE_KEY = 'user'
const TOKEN_KEY = 'token'

export const useUserStore = defineStore('user', () => {
  const user = ref<User | null>(getStorage<User | null>(STORAGE_KEY, null))
  const token = ref<string>(getStorage<string>(TOKEN_KEY, ''))
  const isNewUser = ref(false)
  const authLoading = ref(false)

  const isLoggedIn = computed(() => !!user.value?.id && !!token.value)
  const displayName = computed(() => user.value?.nickname || '游客')
  const hasPhone = computed(() => !!user.value?.phone)
  const needsPhoneBind = computed(() => isLoggedIn.value && !hasPhone.value)

  function persist() {
    setStorage(STORAGE_KEY, user.value)
    setStorage(TOKEN_KEY, token.value)
  }

  function applySession(session: { user: User; token: string }, newUser = false) {
    user.value = session.user
    token.value = session.token
    isNewUser.value = newUser
    persist()
  }

  async function restoreSession(): Promise<boolean> {
    if (!token.value) return false
    authLoading.value = true
    try {
      const session = await authService.restoreSession(token.value)
      if (!session) {
        logout()
        return false
      }
      applySession(session)
      return true
    } finally {
      authLoading.value = false
    }
  }

  async function loginWithWechat(): Promise<void> {
    authLoading.value = true
    try {
      const result = await authService.loginWithWechat()
      applySession(result.session, result.isNewUser)

      try {
        const profile = await authService.getWxUserProfile()
        if (user.value) {
          user.value = {
            ...user.value,
            nickname: profile.userInfo.nickName,
            avatar: profile.userInfo.avatarUrl,
          }
          persist()
        }
      } catch {
        // 用户拒绝授权昵称时仍可登录
      }
    } finally {
      authLoading.value = false
    }
  }

  async function bindPhone(payload: {
    phone?: string
    code?: string
    encryptedData?: string
    iv?: string
  }): Promise<void> {
    if (!user.value || !token.value) {
      throw new Error('请先登录')
    }

    const updated = await authService.bindPhone(
      token.value,
      user.value.wxOpenId,
      payload
    )
    user.value = updated
    persist()
  }

  function completeOnboarding(monthlyIncome?: number, savingsGoal?: number) {
    if (!user.value) return
    user.value = {
      ...user.value,
      onboardingCompleted: true,
      monthlyIncome,
      savingsGoal,
    }
    persist()
  }

  function updateProfile(patch: Partial<User>) {
    if (!user.value) return
    user.value = { ...user.value, ...patch }
    persist()
  }

  function logout() {
    user.value = null
    token.value = ''
    isNewUser.value = false
    removeStorage(STORAGE_KEY)
    removeStorage(TOKEN_KEY)
  }

  return {
    user,
    token,
    isNewUser,
    authLoading,
    isLoggedIn,
    displayName,
    hasPhone,
    needsPhoneBind,
    restoreSession,
    loginWithWechat,
    bindPhone,
    completeOnboarding,
    updateProfile,
    logout,
  }
})
