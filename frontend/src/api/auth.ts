import { http, type ApiResponse } from './http'

export interface LoginRequest {
  username: string
  password: string
}

export interface UserSession {
  id: number
  username: string
  displayName: string
  roles: string[]
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  expiresAt: string
  user: UserSession
}

// 功能：封装登录接口。
// 说明：登录页提交账号密码后调用，后端返回 token、用户信息和角色信息。
export function loginApi(payload: LoginRequest) {
  return http.post<never, ApiResponse<LoginResponse>>('/auth/login', payload)
}

// 功能：封装退出登录接口。
// 说明：配合 auth store 清空本地登录状态，完成前后端退出流程。
export function logoutApi() {
  return http.post<never, ApiResponse<void>>('/auth/logout')
}

export function currentUserApi() {
  return http.get<never, ApiResponse<UserSession>>('/me')
}
