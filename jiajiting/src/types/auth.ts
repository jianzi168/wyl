import type { User } from './models'

export interface AuthSession {
  user: User
  token: string
  expiresAt?: number
}

export interface WxLoginResult {
  session: AuthSession
  isNewUser: boolean
}

export interface BindPhonePayload {
  phone?: string
  code?: string
  encryptedData?: string
  iv?: string
}

export type SyncEntity = 'record'
export type SyncOpType = 'upsert' | 'delete'

export interface SyncQueueItem {
  id: string
  entity: SyncEntity
  type: SyncOpType
  payload: unknown
  createdAt: number
  retryCount: number
}

export type SyncStatus = 'idle' | 'syncing' | 'offline' | 'error'
export type InsightPriority = 'low' | 'medium' | 'high' | 'critical'
export type InsightChannel = 'inline' | 'card' | 'inbox' | 'push'
