import { http, type ApiResponse, type PageResponse } from './http'

export interface AdminGrade {
  gradeId: number
  studentNo: string
  studentName: string
  courseId: number
  courseCode: string
  courseName: string
  term: string
  score: number
  gradePoint: number
  examType: string
  gradeStatus: string
  locked: boolean
}

export interface AdminGradePayload {
  studentNo: string
  courseId: number
  term: string
  score: number
  gradePoint: number
  examType: string
  gradeStatus: string
  locked: boolean
}

export interface AdminExam {
  examId: number
  offeringId: number
  courseCode: string
  courseName: string
  teacherName: string
  term: string
  examTime: string
  room: string
  seatNo: string
  examType: string
  status: string
  invigilator?: string
}

export interface AdminExamPayload {
  offeringId: number
  examTime: string
  room: string
  seatNo: string
  examType: string
  status: string
  invigilator?: string
}

export function adminGradesApi(params?: { term?: string; keyword?: string; page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<AdminGrade>>>('/admin/academic/grades', { params })
}

export function createAdminGradeApi(payload: AdminGradePayload) {
  return http.post<never, ApiResponse<void>>('/admin/academic/grades', payload)
}

export function updateAdminGradeApi(gradeId: number, payload: AdminGradePayload) {
  return http.put<never, ApiResponse<void>>(`/admin/academic/grades/${gradeId}`, payload)
}

export function exportAdminGradesApi(term?: string) {
  return http.get<never, ApiResponse<AdminGrade[]>>('/admin/academic/grades/export', { params: term ? { term } : undefined })
}

export function importAdminGradesApi(grades: AdminGradePayload[]) {
  return http.post<never, ApiResponse<void>>('/admin/academic/grades/import', { grades })
}

export function adminExamsApi(params?: { term?: string; page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<AdminExam>>>('/admin/academic/exams', { params })
}

export function createAdminExamApi(payload: AdminExamPayload) {
  return http.post<never, ApiResponse<void>>('/admin/academic/exams', payload)
}

export function updateAdminExamApi(examId: number, payload: AdminExamPayload) {
  return http.put<never, ApiResponse<void>>(`/admin/academic/exams/${examId}`, payload)
}

export function deleteAdminExamApi(examId: number) {
  return http.delete<never, ApiResponse<void>>(`/admin/academic/exams/${examId}`)
}
