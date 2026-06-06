export type CouponStatus = 'available' | 'used' | 'expired'

export interface Coupon {
  id: string
  title: string
  description: string
  category: string
  emoji: string
  amount: number
  minSpend: number
  status: CouponStatus
  expireAt: number
  link: string
  linkType: 'mini_program' | 'webview'
  appId?: string
}
