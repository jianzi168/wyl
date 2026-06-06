import { appConfig } from './config'

export class ApiError extends Error {
  constructor(
    message: string,
    public statusCode?: number
  ) {
    super(message)
    this.name = 'ApiError'
  }
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown>
  token?: string
}

export async function post<T>(path: string, data: unknown, token?: string): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    data: data as Record<string, unknown>,
    token,
  })
}

export async function request<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const { method = 'GET', data, token } = options
  const url = `${appConfig.apiBase.replace(/\/$/, '')}${path}`

  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method,
      data,
      timeout: appConfig.requestTimeout,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
      success(res) {
        if (res.statusCode && res.statusCode >= 200 && res.statusCode < 300) {
          resolve(res.data as T)
          return
        }
        reject(new ApiError(`请求失败 (${res.statusCode})`, res.statusCode))
      },
      fail(err) {
        reject(new ApiError(err.errMsg || '网络请求失败'))
      },
    })
  })
}
