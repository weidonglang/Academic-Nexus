import { http, type ApiResponse } from '@/api/http'

// 学生端教务查询 API 封装。
// 这里把成绩查询、考试安排和空闲教室查询统一封装，页面组件只负责展示数据，
// 不直接拼接后端地址，便于维护接口路径和返回类型。
export interface GradeRecord {
  term: string
  courseCode: string
  courseName: string
  credit: number
  courseType: string
  score: number
  gradePoint: number
  examType: string
}

export interface ExamSchedule {
  term: string
  courseCode: string
  courseName: string
  examTime: string
  room: string
  seatNo: string
  examType: string
  status: string
}

export interface FreeClassroom {
  campus: string
  building: string
  room: string
  capacity: number
  roomType: string
  availableSlot: string
}

export interface FreeClassroomQuery {
  campus?: string
  building?: string
  slot?: string
}

export async function fetchGradesApi() {
  const response = await http.get<never, ApiResponse<GradeRecord[]>>('/grades/me')
  return response.data
}

export async function fetchExamsApi() {
  const response = await http.get<never, ApiResponse<ExamSchedule[]>>('/exams/me')
  return response.data
}

export async function fetchFreeClassroomsApi(params: FreeClassroomQuery) {
  const response = await http.get<never, ApiResponse<FreeClassroom[]>>('/classrooms/free', { params })
  return response.data
}
