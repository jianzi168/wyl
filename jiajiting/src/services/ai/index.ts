import { isCloudMode } from '@/services/config'
import { isAiEnabled } from '@/utils/ai-prefs'
import type { CoachRequest, CoachResponse } from './types'
import { interpretLocal } from './local-provider'
import { interpretCloud } from './cloud-provider'

export type { CoachRequest, CoachResponse, CoachScene } from './types'
export { buildWeeklyAggregate, buildMonthlyAggregate, buildQaAggregate } from './aggregator'

export async function interpretCoach(req: CoachRequest): Promise<CoachResponse> {
  if (!isAiEnabled()) {
    throw new Error('AI 功能已关闭，可在隐私设置中开启')
  }
  if (isCloudMode() && import.meta.env.VITE_AI_MODE === 'cloud') {
    return interpretCloud(req)
  }
  return interpretLocal(req)
}
