import { http, type ApiResponse } from './http'

export interface PageResponse<T> {
  records: T[]
  page: number
  size: number
  total: number
}

export interface Notice {
  id: number
  title: string
  content: string
  category: string
  pinned: boolean
  publishedAt: string
  publisher: string
}

export interface Notification {
  id: number
  title: string
  content: string
  category: string
  readFlag: boolean
  createdAt: string
  readAt?: string
  relatedType?: string
  relatedId?: number
}

export interface PublishNoticePayload {
  title: string
  content: string
  category: string
  pinned: boolean
  roleCode?: string
}

export interface NoticeStat {
  id: number
  title: string
  category: string
  publishedAt: string
  publisher: string
  targetTotal: number
  readCount: number
  unreadCount: number
}

// 功能：查询首页公告。
// 说明：学生、教师或管理员进入首页时调用，展示已发布公告标题、类别和发布时间。
export function homeNoticesApi(params?: { category?: string; page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<Notice>>>('/notices/home', { params })
}

// 功能：查询当前用户通知。
// 说明：用于首页未读通知和通知列表，支持按已读状态分页查询。
export function myNotificationsApi(params?: { read?: boolean; page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<Notification>>>('/notifications/me', { params })
}

// 功能：标记通知已读。
// 说明：用户查看通知后调用，后端记录 readFlag 和 readAt，管理端可统计已读/未读。
export function markNotificationReadApi(id: number) {
  return http.put<never, ApiResponse<void>>(`/notifications/me/${id}/read`)
}

// 功能：发布通知公告。
// 说明：管理端公告页面调用，后端保存公告并为目标角色用户生成通知记录。
export function publishNoticeApi(payload: PublishNoticePayload) {
  return http.post<never, ApiResponse<Notice>>('/admin/notices', payload)
}

// 功能：查询公告发布统计。
// 说明：管理端用于展示公告目标人数、已读数和未读数，体现通知管理闭环。
export function adminNoticeStatsApi() {
  return http.get<never, ApiResponse<NoticeStat[]>>('/admin/notices/stats')
}
