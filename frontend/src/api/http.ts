import axios from 'axios'
import { useAuthStore } from '@/stores/auth'

export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  traceId?: string
  timestamp: string
}

export interface PageResponse<T> {
  records: T[]
  page: number
  size: number
  total: number
}

export const http = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

let refreshPromise: Promise<string> | null = null

// 功能：请求发送前统一附加登录 token。
// 说明：业务页面不需要重复处理 Authorization，请求拦截器会从 auth store 中读取 accessToken，
// 后端 BearerTokenAuthenticationFilter 根据该 token 识别当前登录用户。
http.interceptors.request.use((config) => {
  const auth = useAuthStore()
  if (auth.accessToken) {
    config.headers.Authorization = `Bearer ${auth.accessToken}`
  }
  return config
})

// 功能：统一处理后端响应、登录失效和无权限状态。
// 说明：401 清空会话并回登录页；403 保留登录态并进入无权限页，方便用户理解权限边界。
http.interceptors.response.use(
  (response) => response.data,
  async (error) => {
    const status = error.response?.status
    const originalRequest = error.config
    const url = String(originalRequest?.url || '')
    if (status === 401 && !url.includes('/auth/login') && !url.includes('/auth/refresh') && !originalRequest?._retry) {
      const auth = useAuthStore()
      if (auth.refreshToken) {
        originalRequest._retry = true
        try {
          refreshPromise = refreshPromise || auth.refreshSession()
          const token = await refreshPromise
          originalRequest.headers = originalRequest.headers || {}
          originalRequest.headers.Authorization = `Bearer ${token}`
          return http.request(originalRequest)
        } catch {
          auth.clearSession()
          if (window.location.pathname !== '/login') {
            window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
          }
        } finally {
          refreshPromise = null
        }
      } else if (window.location.pathname !== '/login') {
        auth.clearSession()
        window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
      }
    } else if (status === 401 && window.location.pathname !== '/login') {
      const auth = useAuthStore()
      auth.clearSession()
      window.location.assign(`/login?redirect=${encodeURIComponent(window.location.pathname)}`)
    }
    if (status === 403 && window.location.pathname !== '/403') {
      window.location.assign('/403')
    }
    return Promise.reject(error)
  },
)
