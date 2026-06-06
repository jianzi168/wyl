import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { Coupon, CouponStatus } from '@/types/coupon'
import { fetchCoupons, markCouponUsed, filterCouponsByStatus } from '@/services/alliance'
import { track } from '@/utils/analytics'

export const useCouponStore = defineStore('coupon', () => {
  const coupons = ref<Coupon[]>([])
  const loading = ref(false)

  async function load() {
    loading.value = true
    try {
      coupons.value = await fetchCoupons()
    } finally {
      loading.value = false
    }
  }

  function byStatus(status: CouponStatus) {
    return filterCouponsByStatus(coupons.value, status)
  }

  async function useCoupon(id: string) {
    await markCouponUsed(id)
    await load()
    track('coupon_use', { id })
  }

  return { coupons, loading, load, byStatus, useCoupon }
})
