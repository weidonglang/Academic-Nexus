import { http, type ApiResponse } from './http'

export interface EvaluationTask {
  offeringId: number
  selectionId: number
  courseCode: string
  courseName: string
  teacherName: string
  term: string
  scheduleText: string
  classroom: string
  evaluated: boolean
  teachingScore?: number
  contentScore?: number
  interactionScore?: number
  overallScore?: number
  comment?: string
  submittedAt?: string
}

export interface SubmitEvaluationPayload {
  teachingScore: number
  contentScore: number
  interactionScore: number
  overallScore: number
  comment?: string
}

export interface EvaluationSummary {
  offeringId: number
  courseCode: string
  courseName: string
  teacherName: string
  term: string
  selectedCount: number
  submittedCount: number
  averageTeachingScore?: number
  averageContentScore?: number
  averageInteractionScore?: number
  averageOverallScore?: number
}

export interface EvaluationRecord {
  evaluationId: number
  studentNo: string
  studentName: string
  courseCode: string
  courseName: string
  teacherName: string
  term: string
  teachingScore: number
  contentScore: number
  interactionScore: number
  overallScore: number
  comment?: string
  submittedAt: string
}

export function evaluationTasksApi() {
  return http.get<never, ApiResponse<EvaluationTask[]>>('/evaluations/tasks')
}

export function submitEvaluationApi(offeringId: number, payload: SubmitEvaluationPayload) {
  return http.post<never, ApiResponse<void>>(`/evaluations/tasks/${offeringId}`, payload)
}

export function adminEvaluationSummariesApi(term?: string) {
  return http.get<never, ApiResponse<EvaluationSummary[]>>('/admin/evaluations/summaries', {
    params: term ? { term } : undefined,
  })
}

export function adminEvaluationRecordsApi(params?: { term?: string; offeringId?: number }) {
  return http.get<never, ApiResponse<EvaluationRecord[]>>('/admin/evaluations/records', {
    params,
  })
}
