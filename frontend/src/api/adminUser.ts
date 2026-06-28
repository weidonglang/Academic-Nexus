import { http, type ApiResponse } from './http'

export type UserStatus = 'ACTIVE' | 'DISABLED' | 'LOCKED'

export interface AdminUser {
  userId: number
  username: string
  displayName: string
  status: UserStatus
  lastLoginAt?: string
  roleCodes: string[]
}

export interface AdminRole {
  roleId: number
  code: string
  name: string
}

export interface PageResponse<T> {
  records: T[]
  page: number
  size: number
  total: number
}

export interface CreateUserPayload {
  username: string
  displayName: string
  password: string
  roleCodes: string[]
}

export interface UpdateUserPayload {
  displayName: string
  status: UserStatus
}

export interface ImportIssue {
  rowNumber: number
  column: string
  reason: string
  suggestion: string
}

export interface ImportPreview {
  totalRows: number
  validRows: number
  errorRows: number
  duplicateAccounts: number
  missingFields: number
  formatErrors: number
  errors: ImportIssue[]
}

export interface ImportCommitResult {
  taskId: number
  successCount: number
  failureCount: number
  items: ImportIssue[]
}

// 功能：分页查询用户账号。
// 说明：管理端用户页面按关键字、页码和每页条数查询，避免一次性加载全部账号。
export function adminUsersApi(params?: { keyword?: string; page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<AdminUser>>>('/admin/users', {
    params,
  })
}

// 功能：查询可分配角色。
// 说明：新增用户、分配角色弹窗使用该接口加载 ADMIN、TEACHER、STUDENT 等角色。
export function adminUserRolesApi() {
  return http.get<never, ApiResponse<AdminRole[]>>('/admin/users/roles')
}

// 功能：新增系统用户。
// 说明：管理员提交账号、显示名、密码和角色，后端完成密码加密和角色绑定。
export function createAdminUserApi(payload: CreateUserPayload) {
  return http.post<never, ApiResponse<AdminUser>>('/admin/users', payload)
}

// 功能：修改用户基础信息。
// 说明：更新显示名和账号状态，适用于禁用、锁定或恢复账号。
export function updateAdminUserApi(userId: number, payload: UpdateUserPayload) {
  return http.put<never, ApiResponse<AdminUser>>(`/admin/users/${userId}`, payload)
}

// 功能：更新用户角色。
// 说明：角色变化会影响用户菜单范围和后端接口访问权限。
export function updateAdminUserRolesApi(userId: number, roleCodes: string[]) {
  return http.put<never, ApiResponse<AdminUser>>(`/admin/users/${userId}/roles`, { roleCodes })
}

// 功能：重置用户密码。
// 说明：管理员为用户设置新密码，后端加密保存，用户下次登录使用新密码。
export function resetAdminUserPasswordApi(userId: number, password: string) {
  return http.put<never, ApiResponse<void>>(`/admin/users/${userId}/password`, { password })
}

// 功能：删除用户账号。
// 说明：后端会校验业务数据关联，避免删除仍有学生档案或历史数据的账号。
export function deleteAdminUserApi(userId: number) {
  return http.delete<never, ApiResponse<void>>(`/admin/users/${userId}`)
}

export function previewAdminUsersImportApi(content: string) {
  return http.post<never, ApiResponse<ImportPreview>>('/admin/users/import-preview', { content })
}

export function commitAdminUsersImportApi(content: string) {
  return http.post<never, ApiResponse<ImportCommitResult>>('/admin/users/import-commit', { content })
}
