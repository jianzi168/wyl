import type { AuthSession, BindPhonePayload, WxLoginResult } from '@/types/auth'
import type { User } from '@/types/models'
import { getStorage, setStorage } from '@/utils/storage'
import { createId } from '@/utils/id'

const OPENID_KEY = 'wx_openid_dev'

function getOrCreateOpenId(loginCode: string): string {
  const cached = getStorage<string>(OPENID_KEY, '')
  if (cached) return cached
  const openId = `local_${loginCode.slice(0, 8)}_${Date.now().toString(36)}`
  setStorage(OPENID_KEY, openId)
  return openId
}

function buildUser(openId: string, patch?: Partial<User>): User {
  const now = Date.now()
  return {
    id: getStorage<string>(`user_id_${openId}`, '') || createId('user'),
    wxOpenId: openId,
    nickname: patch?.nickname || '微信用户',
    avatar: patch?.avatar || '',
    phone: patch?.phone,
    onboardingCompleted: patch?.onboardingCompleted ?? false,
    monthlyIncome: patch?.monthlyIncome,
    savingsGoal: patch?.savingsGoal,
    createdAt: patch?.createdAt || now,
  }
}

export async function localWxLogin(): Promise<WxLoginResult> {
  const loginRes = await uni.login({ provider: 'weixin' })
  if (!loginRes.code) {
    throw new Error('微信登录失败，请重试')
  }

  const openId = getOrCreateOpenId(loginRes.code)
  const existingUser = getStorage<User | null>(`user_profile_${openId}`, null)
  const isNewUser = !existingUser

  const user = existingUser || buildUser(openId)
  setStorage(`user_id_${openId}`, user.id)
  setStorage(`user_profile_${openId}`, user)

  const session: AuthSession = {
    user,
    token: `local_token_${openId}`,
  }

  return { session, isNewUser }
}

export async function localBindPhone(
  openId: string,
  payload: BindPhonePayload
): Promise<User> {
  const user = getStorage<User | null>(`user_profile_${openId}`, null)
  if (!user) throw new Error('用户不存在，请重新登录')

  const phone = payload.phone?.trim()
  if (!phone || !/^1\d{10}$/.test(phone)) {
    throw new Error('请输入正确的手机号')
  }

  const updated: User = { ...user, phone }
  setStorage(`user_profile_${openId}`, updated)
  return updated
}

export function localRestoreSession(token: string): AuthSession | null {
  if (!token.startsWith('local_token_')) return null
  const openId = token.replace('local_token_', '')
  const user = getStorage<User | null>(`user_profile_${openId}`, null)
  if (!user) return null
  return { user, token }
}
