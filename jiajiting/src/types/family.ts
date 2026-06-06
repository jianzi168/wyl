export type FamilyRole = 'owner' | 'member'

export interface MemberBreakdown {
  userId: string
  nickname: string
  amount: number
}

export interface FamilySnapshot {
  monthExpense: number
  monthIncome: number
  memberCount: number
  topCategories: { name: string; amount: number; percent: number }[]
  memberBreakdown: MemberBreakdown[]
  updatedAt: number
}

export interface FamilyMember {
  id: string
  userId: string
  nickname: string
  role: FamilyRole
  joinedAt: number
}

export interface Family {
  id: string
  name: string
  ownerId: string
  ownerName: string
  inviteCode: string
  members: FamilyMember[]
  snapshot: FamilySnapshot
  createdAt: number
}

export interface FamilyMembership {
  familyId: string
  role: FamilyRole
  familyName: string
}
