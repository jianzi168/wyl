import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { Family, FamilyMembership, FamilySnapshot } from '@/types/family'
import type { RecordItem } from '@/types/models'
import { getStorage, setStorage } from '@/utils/storage'
import { createId } from '@/utils/id'
import { useUserStore } from './user'
import { useRecordStore } from './record'
import { weekCategoryBreakdown } from '@/utils/report'
import { getMonthRange } from '@/utils/date'
import { sumExpenses } from '@/utils/budget'
import { track } from '@/utils/analytics'

const MEMBERSHIP_KEY = 'family_membership'
const REGISTRY_KEY = 'family_registry'
const LEDGER_KEY = 'family_ledger'

function randomCode() {
  return Math.random().toString(36).slice(2, 8).toUpperCase()
}

function buildSnapshot(
  records: RecordItem[],
  members: Family['members']
): FamilySnapshot {
  const { start, end } = getMonthRange()
  const monthExpense = sumExpenses(records, start, end)
  const monthIncome = records
    .filter((r) => r.type === 'income' && r.timestamp >= start && r.timestamp <= end)
    .reduce((s, r) => s + r.amount, 0)
  const topCategories = weekCategoryBreakdown(records, start, end).slice(0, 5)
  const memberBreakdown = members.map((m) => ({
    userId: m.userId,
    nickname: m.nickname,
    amount: sumExpenses(
      records.filter((r) => (r.memberId || r.userId) === m.userId),
      start,
      end
    ),
  }))
  return {
    monthExpense,
    monthIncome,
    memberCount: members.length,
    topCategories,
    memberBreakdown,
    updatedAt: Date.now(),
  }
}

export const useFamilyStore = defineStore('family', () => {
  const membership = ref<FamilyMembership | null>(getStorage<FamilyMembership | null>(MEMBERSHIP_KEY, null))
  const registry = ref<Record<string, Family>>(getStorage<Record<string, Family>>(REGISTRY_KEY, {}))

  const isOwner = computed(() => membership.value?.role === 'owner')
  const isMember = computed(() => !!membership.value)
  const canCollaborate = computed(() => isMember.value)
  const family = computed(() => {
    if (!membership.value) return null
    return Object.values(registry.value).find((f) => f.id === membership.value!.familyId) || null
  })

  function persist() {
    setStorage(MEMBERSHIP_KEY, membership.value)
    setStorage(REGISTRY_KEY, registry.value)
  }

  function getLedger(): Record<string, RecordItem[]> {
    return getStorage<Record<string, RecordItem[]>>(LEDGER_KEY, {})
  }

  function saveLedger(ledger: Record<string, RecordItem[]>) {
    setStorage(LEDGER_KEY, ledger)
  }

  function getFamilyRecords(): RecordItem[] {
    const fam = family.value
    if (!fam) return []
    const ledger = getLedger()[fam.id] || []
    const userStore = useUserStore()
    const recordStore = useRecordStore()

    if (userStore.user?.id === fam.ownerId) {
      const map = new Map<string, RecordItem>()
      recordStore.records.forEach((r) => {
        const mid = r.memberId || r.userId
        if (!r.memberId || mid === fam.ownerId) {
          map.set(r.id, { ...r, memberId: fam.ownerId })
        }
      })
      ledger.forEach((r) => map.set(r.id, r))
      return Array.from(map.values())
    }
    return ledger
  }

  function appendFamilyRecord(record: RecordItem) {
    const fam = family.value
    if (!fam) return
    const ledger = getLedger()
    if (!ledger[fam.id]) ledger[fam.id] = []
    ledger[fam.id].unshift(record)
    saveLedger(ledger)
    refreshSnapshot()
  }

  function refreshSnapshot() {
    const userStore = useUserStore()
    const fam = family.value
    if (!fam || fam.ownerId !== userStore.user?.id) return
    fam.snapshot = buildSnapshot(getFamilyRecords(), fam.members)
    registry.value[fam.inviteCode] = fam
    persist()
  }

  function createFamily(name: string) {
    const userStore = useUserStore()
    const user = userStore.user
    if (!user) throw new Error('请先登录')
    if (membership.value) throw new Error('已加入家庭')

    const inviteCode = randomCode()
    const recordStore = useRecordStore()
    const members = [
      {
        id: createId('fm'),
        userId: user.id,
        nickname: userStore.displayName,
        role: 'owner' as const,
        joinedAt: Date.now(),
      },
    ]
    const fam: Family = {
      id: createId('fam'),
      name: name.trim() || '我的家庭',
      ownerId: user.id,
      ownerName: userStore.displayName,
      inviteCode,
      members,
      snapshot: buildSnapshot(recordStore.records, members),
      createdAt: Date.now(),
    }
    registry.value[inviteCode] = fam
    membership.value = { familyId: fam.id, role: 'owner', familyName: fam.name }
    persist()
    track('family_create', {})
    return fam
  }

  function joinFamily(inviteCode: string) {
    const userStore = useUserStore()
    const user = userStore.user
    if (!user) throw new Error('请先登录')
    if (membership.value) throw new Error('已加入家庭')

    const code = inviteCode.trim().toUpperCase()
    const fam = registry.value[code]
    if (!fam) throw new Error('邀请码无效，请向户主确认')

    if (!fam.members.some((m) => m.userId === user.id)) {
      fam.members.push({
        id: createId('fm'),
        userId: user.id,
        nickname: userStore.displayName,
        role: 'member',
        joinedAt: Date.now(),
      })
      fam.snapshot.memberCount = fam.members.length
      registry.value[code] = fam
    }
    membership.value = { familyId: fam.id, role: 'member', familyName: fam.name }
    persist()
    track('family_join', {})
    return fam
  }

  function leaveFamily() {
    const userStore = useUserStore()
    const fam = family.value
    if (!fam || !membership.value) return

    if (isOwner.value) {
      delete registry.value[fam.inviteCode]
      const ledger = getLedger()
      delete ledger[fam.id]
      saveLedger(ledger)
    } else {
      fam.members = fam.members.filter((m) => m.userId !== userStore.user?.id)
      fam.snapshot = buildSnapshot(getFamilyRecords(), fam.members)
      registry.value[fam.inviteCode] = fam
    }
    membership.value = null
    persist()
  }

  function copyInviteCode(): string {
    const fam = family.value
    if (!fam || !isOwner.value) return ''
    return fam.inviteCode
  }

  return {
    membership,
    family,
    isOwner,
    isMember,
    canCollaborate,
    createFamily,
    joinFamily,
    leaveFamily,
    refreshSnapshot,
    copyInviteCode,
    appendFamilyRecord,
    getFamilyRecords,
  }
})
