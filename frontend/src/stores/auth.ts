import { defineStore } from 'pinia'
import { loginApi, logoutApi, type UserSession } from '@/api/auth'

interface AuthState {
  accessToken: string
  refreshToken: string
  user: UserSession | null
}

export const ACCESS_TOKEN_KEY = 'tianshi.accessToken'
export const REFRESH_TOKEN_KEY = 'tianshi.refreshToken'
export const USER_KEY = 'tianshi.user'

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    accessToken: localStorage.getItem(ACCESS_TOKEN_KEY) ?? '',
    refreshToken: localStorage.getItem(REFRESH_TOKEN_KEY) ?? '',
    user: readStoredUser(),
  }),
  getters: {
    isAuthenticated: (state) => Boolean(state.accessToken),
  },
  actions: {
    // 功能：保存登录成功后的用户信息、角色信息和 token。
    // 说明：登录页调用本方法后，前端把认证结果写入 Pinia 和 localStorage，
    // 后续路由守卫、菜单加载和 Axios 请求头都会依赖这里保存的登录状态。
    async login(username: string, password: string) {
      const response = await loginApi({ username, password })
      this.accessToken = response.data.accessToken
      this.refreshToken = response.data.refreshToken
      this.user = response.data.user
      localStorage.setItem(ACCESS_TOKEN_KEY, this.accessToken)
      localStorage.setItem(REFRESH_TOKEN_KEY, this.refreshToken)
      localStorage.setItem(USER_KEY, JSON.stringify(this.user))
    },
    // 功能：退出登录。
    // 说明：先尽量通知后端退出，再清空本地 token、用户信息和菜单相关状态，
    // 防止切换账号时继续沿用上一个用户的登录数据。
    async logout() {
      if (this.accessToken) {
        try {
          await logoutApi()
        } catch {
          // The local session should still be cleared if the server token is already invalid.
        }
      }
      this.clearSession()
    },
    // 功能：清空本地会话。
    // 说明：401/403、退出登录或 token 失效时都会调用，统一移除 localStorage 中的认证信息。
    clearSession() {
      this.accessToken = ''
      this.refreshToken = ''
      this.user = null
      localStorage.removeItem(ACCESS_TOKEN_KEY)
      localStorage.removeItem(REFRESH_TOKEN_KEY)
      localStorage.removeItem(USER_KEY)
    },
  },
})

function readStoredUser(): UserSession | null {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as UserSession
  } catch {
    localStorage.removeItem(USER_KEY)
    return null
  }
}
