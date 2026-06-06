import { isCloudMode } from '@/services/config'
import type { AuthSession, BindPhonePayload, WxLoginResult } from '@/types/auth'
import * as local from './local-provider'
import * as cloud from './cloud-provider'

export async function loginWithWechat(): Promise<WxLoginResult> {
  if (isCloudMode()) {
    return cloud.cloudWxLogin()
  }
  return local.localWxLogin()
}

export async function bindPhone(
  token: string,
  openId: string,
  payload: BindPhonePayload
) {
  if (isCloudMode()) {
    return cloud.cloudBindPhone(token, payload)
  }
  return local.localBindPhone(openId, payload)
}

export async function restoreSession(token: string): Promise<AuthSession | null> {
  if (!token) return null
  if (isCloudMode()) {
    return cloud.cloudRestoreSession(token)
  }
  return local.localRestoreSession(token)
}

export function getWxUserProfile(): Promise<UniApp.GetUserProfileRes> {
  return new Promise((resolve, reject) => {
    uni.getUserProfile({
      desc: '用于展示昵称和头像',
      success: resolve,
      fail: reject,
    })
  })
}
