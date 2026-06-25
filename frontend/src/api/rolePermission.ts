import { http, type ApiResponse } from './http'

// 角色权限管理 API。
// 管理端通过这里读取角色、菜单树并保存角色菜单关系，最终影响不同账号登录后的菜单范围。
export interface RolePermissionRole {
  roleId: number
  code: string
  name: string
}

export interface RolePermissionMenu {
  menuId: number
  code: string
  title: string
  path: string
  icon: string
  parentCode?: string
  sortOrder: number
}

export interface PermissionCapability {
  code: string
  name: string
  description?: string
}

export interface PermissionMatrixRoleRow {
  roleId: number
  roleCode: string
  roleName: string
  menuCodes: string[]
  capabilityCodes: string[]
}

export interface PermissionMatrixResponse {
  roles: RolePermissionRole[]
  menus: RolePermissionMenu[]
  capabilities: PermissionCapability[]
  roleRows: PermissionMatrixRoleRow[]
}

export function rolePermissionRolesApi() {
  return http.get<never, ApiResponse<RolePermissionRole[]>>('/admin/role-permissions/roles')
}

export function rolePermissionMenusApi() {
  return http.get<never, ApiResponse<RolePermissionMenu[]>>('/admin/role-permissions/menus')
}

export function rolePermissionMenuCodesApi(roleId: number) {
  return http.get<never, ApiResponse<string[]>>(`/admin/role-permissions/roles/${roleId}/menus`)
}

export function updateRolePermissionMenusApi(roleId: number, menuCodes: string[]) {
  return http.put<never, ApiResponse<string[]>>(`/admin/role-permissions/roles/${roleId}/menus`, {
    menuCodes,
  })
}

export function permissionMatrixApi() {
  return http.get<never, ApiResponse<PermissionMatrixResponse>>('/admin/role-permissions/matrix')
}
