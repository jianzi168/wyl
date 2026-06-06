/** 运行时配置 — 通过 .env.local 覆盖 */

export type AuthMode = 'local' | 'cloud'

export const appConfig = {
  authMode: (import.meta.env.VITE_AUTH_MODE || 'local') as AuthMode,
  apiBase: import.meta.env.VITE_API_BASE || '',
  requestTimeout: 15000,
}

export function isCloudMode(): boolean {
  return appConfig.authMode === 'cloud' && !!appConfig.apiBase
}
