import { isCloudMode } from '@/services/config'
import { getStorage, setStorage } from '@/utils/storage'
import { MOCK_COUPONS } from '@/constants/coupons-mock'
import { request } from '@/services/http'
import type { Coupon, CouponStatus } from '@/types/coupon'
import type { AllianceDeal } from '@/constants/alliance-deals'
import { ALLIANCE_DEALS } from '@/constants/alliance-deals'

const COUPON_KEY = 'coupons_local'

function initLocalCoupons(): Coupon[] {
  const saved = getStorage<Coupon[] | null>(COUPON_KEY, null)
  if (saved) return saved
  setStorage(COUPON_KEY, MOCK_COUPONS)
  return MOCK_COUPONS
}

export async function fetchCoupons(): Promise<Coupon[]> {
  if (isCloudMode() && import.meta.env.VITE_ALLIANCE_MODE === 'cloud') {
    const token = getStorage<string>('token', '')
    return request<Coupon[]>('/alliance/coupons', { token })
  }
  return initLocalCoupons()
}

export async function markCouponUsed(id: string): Promise<void> {
  const list = initLocalCoupons()
  const item = list.find((c) => c.id === id)
  if (item) {
    item.status = 'used'
    setStorage(COUPON_KEY, list)
  }
}

export function getDeals(): AllianceDeal[] {
  return ALLIANCE_DEALS
}

export function openAllianceLink(deal: { link: string; linkType: string; appId?: string }) {
  if (deal.linkType === 'mini_program' && deal.appId) {
    // #ifdef MP-WEIXIN
    uni.navigateToMiniProgram({
      appId: deal.appId,
      path: deal.link,
      fail() {
        uni.showToast({ title: '跳转失败', icon: 'none' })
      },
    })
    return
    // #endif
  }
  // #ifdef H5
  window.open(deal.link, '_blank')
  // #endif
  // #ifndef H5
  uni.setClipboardData({
    data: deal.link,
    success() {
      uni.showToast({ title: '链接已复制，请在浏览器打开', icon: 'none' })
    },
  })
  // #endif
}

export function filterCouponsByStatus(coupons: Coupon[], status: CouponStatus) {
  const now = Date.now()
  return coupons.filter((c) => {
    if (status === 'expired') return c.status === 'expired' || (c.status === 'available' && c.expireAt < now)
    if (status === 'available') return c.status === 'available' && c.expireAt >= now
    return c.status === status
  })
}
