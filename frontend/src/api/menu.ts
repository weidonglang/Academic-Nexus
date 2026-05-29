import { http, type ApiResponse } from './http'

export interface MenuItem {
  code: string
  title: string
  path: string
  icon: string
  children: MenuItem[]
}

// 功能：查询当前用户菜单权限。
// 说明：登录后由 menu store 调用，后端根据角色返回菜单树，前端据此生成侧边栏。
export function menusApi() {
  return http.get<never, ApiResponse<MenuItem[]>>('/menus')
}
