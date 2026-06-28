import { http, type ApiResponse } from './http'

export interface ScheduleEntry {
  courseCode: string
  courseName: string
  teacherName: string
  classroom: string
  scheduleText: string
  dayOfWeek: number
  slot: string
  scheduleValid: boolean
  scheduleMessage?: string
}

export function personalScheduleApi() {
  return http.get<never, ApiResponse<ScheduleEntry[]>>('/schedules/me/personal')
}
