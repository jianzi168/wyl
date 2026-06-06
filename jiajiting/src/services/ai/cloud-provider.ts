import { post } from '@/services/http'
import { getStorage } from '@/utils/storage'
import type { CoachRequest, CoachResponse } from './types'

export async function interpretCloud(req: CoachRequest): Promise<CoachResponse> {
  const token = getStorage<string>('token', '')
  return post<CoachResponse>('/ai/coach', req, token)
}
