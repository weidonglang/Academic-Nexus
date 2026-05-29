import { http, type ApiResponse } from './http'
import type { ApplicationStatus } from './student'

export type RegistrationApplicationType =
  | 'MINOR_MAJOR_REGISTRATION'
  | 'RETAKE_REGISTRATION'
  | 'INTERNAL_CREDIT_SUBSTITUTION'
  | 'EXTERNAL_CREDIT_SUBSTITUTION'
  | 'SCORE_BONUS'
  | 'STREAM_MAJOR_CONFIRMATION'
  | 'MAJOR_DIRECTION_CONFIRMATION'

export interface RegistrationApplication {
  id: number
  type: RegistrationApplicationType
  targetName: string
  courseName?: string
  reason: string
  status: ApplicationStatus
  submittedAt: string
  reviewedAt?: string
  reviewComment?: string
}

export interface AdminRegistrationApplication extends RegistrationApplication {
  studentId: number
  studentNo: string
  studentName: string
  college: string
  major: string
  className: string
}

export interface SubmitRegistrationApplicationPayload {
  type: RegistrationApplicationType
  targetName: string
  courseName?: string
  reason: string
}

export interface ReviewRegistrationApplicationPayload {
  decision: 'APPROVE' | 'REJECT'
  comment: string
}

export const registrationTypeText: Record<RegistrationApplicationType, string> = {
  MINOR_MAJOR_REGISTRATION: '微专业报名',
  RETAKE_REGISTRATION: '重修报名',
  INTERNAL_CREDIT_SUBSTITUTION: '校内学分节点替代申请',
  EXTERNAL_CREDIT_SUBSTITUTION: '校外课程学分节点替代申请',
  SCORE_BONUS: '成绩加分申请',
  STREAM_MAJOR_CONFIRMATION: '分流专业确认',
  MAJOR_DIRECTION_CONFIRMATION: '专业方向确认',
}

export function registrationApplicationsApi(params?: { type?: RegistrationApplicationType }) {
  return http.get<never, ApiResponse<RegistrationApplication[]>>('/students/me/registration-applications', {
    params,
  })
}

export function submitRegistrationApplicationApi(payload: SubmitRegistrationApplicationPayload) {
  return http.post<never, ApiResponse<RegistrationApplication>>('/students/me/registration-applications', payload)
}

export function adminRegistrationApplicationsApi(params?: {
  status?: ApplicationStatus | ''
  type?: RegistrationApplicationType | ''
  keyword?: string
}) {
  return http.get<never, ApiResponse<AdminRegistrationApplication[]>>('/admin/registration-applications', {
    params,
  })
}

export function reviewRegistrationApplicationApi(id: number, payload: ReviewRegistrationApplicationPayload) {
  return http.post<never, ApiResponse<AdminRegistrationApplication>>(
    `/admin/registration-applications/${id}/review`,
    payload,
  )
}
