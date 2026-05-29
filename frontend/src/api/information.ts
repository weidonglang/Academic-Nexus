import { http, type ApiResponse } from './http'

// 教学信息查询中心 API。
// 覆盖学业预警、毕业审核、班级课表、选课名单、教学计划、论文成绩和教学反馈等页面。
export interface AcademicWarning {
  term: string
  level: string
  reason: string
  status: string
  createdAt: string
}

export interface GraduationAudit {
  auditItem: string
  requiredValue: string
  currentValue: string
  passed: boolean
  remark?: string
  updatedAt: string
}

export interface ClassSchedule {
  term: string
  className: string
  courseCode: string
  courseName: string
  credit: number
  courseType: string
  teacherName: string
  scheduleText: string
  classroom: string
}

export interface CourseRoster {
  offeringId: number
  courseCode: string
  courseName: string
  term: string
  teacherName: string
  scheduleText: string
  classroom: string
  studentNo: string
  studentName: string
  college: string
  major: string
  className: string
  selectedAt: string
}

export interface OfferingOption {
  offeringId: number
  courseCode: string
  courseName: string
  term: string
  teacherName: string
  scheduleText: string
  classroom: string
}

export interface AcademicProgress {
  courseType: string
  courseCount: number
  totalCredits: number
  passedCredits: number
  averageScore: number
}

export interface TeachingPlan {
  term: string
  courseCode: string
  courseName: string
  credit: number
  courseType: string
  assessmentType: string
}

export interface WeeklySchedule {
  term: string
  courseCode: string
  courseName: string
  credit: number
  teacherName: string
  scheduleText: string
  classroom: string
  week: number
}

export interface ThesisGrade {
  title: string
  advisor: string
  proposalScore?: number
  midtermScore?: number
  defenseScore?: number
  finalScore?: number
  gradeLevel?: string
  status: string
  updatedAt: string
}

export interface TeachingFeedback {
  id: number
  category: string
  title: string
  content: string
  status: string
  reply?: string
  submittedAt: string
  repliedAt?: string
}

export interface SubmitFeedbackPayload {
  category: string
  title: string
  content: string
}

export function academicWarningsApi() {
  return http.get<never, ApiResponse<AcademicWarning[]>>('/information/academic-warnings/me')
}

export function graduationAuditApi() {
  return http.get<never, ApiResponse<GraduationAudit[]>>('/information/graduation-audit/me')
}

export function classSchedulesApi(params?: { className?: string; term?: string }) {
  return http.get<never, ApiResponse<ClassSchedule[]>>('/information/class-schedules', { params })
}

export function courseRostersApi(params?: { offeringId?: number; term?: string }) {
  return http.get<never, ApiResponse<CourseRoster[]>>('/information/course-rosters', { params })
}

export function offeringOptionsApi(params?: { term?: string }) {
  return http.get<never, ApiResponse<OfferingOption[]>>('/information/offering-options', { params })
}

export function academicProgressApi() {
  return http.get<never, ApiResponse<AcademicProgress[]>>('/information/academic-progress/me')
}

export function teachingPlanApi(params?: { major?: string; grade?: string }) {
  return http.get<never, ApiResponse<TeachingPlan[]>>('/information/teaching-plan', { params })
}

export function weeklyScheduleApi(params?: { week?: number }) {
  return http.get<never, ApiResponse<WeeklySchedule[]>>('/information/weekly-schedule/me', { params })
}

export function thesisGradeApi() {
  return http.get<never, ApiResponse<ThesisGrade[]>>('/information/thesis-grade/me')
}

export function teachingFeedbackApi() {
  return http.get<never, ApiResponse<TeachingFeedback[]>>('/information/feedback/me')
}

export function submitTeachingFeedbackApi(payload: SubmitFeedbackPayload) {
  return http.post<never, ApiResponse<TeachingFeedback>>('/information/feedback/me', payload)
}
