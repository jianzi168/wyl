import { request } from '@/services/http'
import type { AuthSession, BindPhonePayload, WxLoginResult } from '@/types/auth'
import type { User } from '@/types/models'

interface CloudLoginResponse {
  user: User
  token: string
  expiresAt?: number
  isNewUser: boolean
}

interface CloudBindPhoneResponse {
  user: User
}

export async function cloudWxLogin(): Promise<WxLoginResult> {
  const loginRes = await uni.login({ provider: 'weixin' })
  if (!loginRes.code) {
    throw new Error('微信登录失败，请重试')
  }

  const data = await request<CloudLoginResponse>('/auth/wx-login', {
    method: 'POST',
    data: { code: loginRes.code },
  })

  return {
    session: {
      user: data.user,
      token: data.token,
      expiresAt: data.expiresAt,
    },
    isNewUser: data.isNewUser,
  }
}

export async function cloudBindPhone(
  token: string,
  payload: BindPhonePayload
): Promise<User> {
  const data = await request<CloudBindPhoneResponse>('/auth/bind-phone', {
    method: 'POST',
    token,
    data: payload as Record<string, unknown>,
  })
  return data.user
}

export async function cloudRestoreSession(token: string): Promise<AuthSession | null> {
  try {
    const data = await request<{ user: User; expiresAt?: number }>('/auth/me', {
      token,
    })
    return { user: data.user, token, expiresAt: data.expiresAt }
  } catch {
    return null
  }
}
