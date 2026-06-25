import { http, type ApiResponse, type PageResponse } from './http'

export interface SensitiveWordRow {
  id: number
  word: string
  category: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  enabled: boolean
  createdAt: string
  updatedAt: string
}

export interface ModerationLogRow {
  id: number
  scene: string
  contentHash: string
  matchedWords?: string
  riskLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  action: string
  operator?: string
  traceId?: string
  createdAt: string
}

export interface DataDictionaryTable {
  tableName: string
  displayName: string
  module: string
  description?: string
  sensitiveLevel: 'LOW' | 'MEDIUM' | 'HIGH'
  exportAllowed: boolean
}

export interface DataDictionaryField {
  tableName: string
  fieldName: string
  displayName: string
  description?: string
  sensitive: boolean
  maskingRule?: string
  exportAllowed: boolean
}

export function sensitiveWordsApi(params?: { page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<SensitiveWordRow>>>('/admin/sensitive-words', { params })
}

export function createSensitiveWordApi(payload: { word: string; category: string; riskLevel: string; enabled: boolean }) {
  return http.post<never, ApiResponse<SensitiveWordRow>>('/admin/sensitive-words', payload)
}

export function moderationLogsApi(params?: { page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<ModerationLogRow>>>('/admin/content-moderation/logs', { params })
}

export function dataDictionaryTablesApi(params?: { module?: string }) {
  return http.get<never, ApiResponse<DataDictionaryTable[]>>('/admin/data-dictionary/tables', { params })
}

export function dataDictionaryFieldsApi(tableName: string) {
  return http.get<never, ApiResponse<DataDictionaryField[]>>(`/admin/data-dictionary/tables/${encodeURIComponent(tableName)}/fields`)
}
