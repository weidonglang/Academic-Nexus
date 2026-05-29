import { http, type ApiResponse } from './http'

export interface PageResponse<T> {
  records: T[]
  page: number
  size: number
  total: number
}

export interface CourseOffering {
  offeringId: number
  courseCode: string
  courseName: string
  credit: number
  category: string
  teacherName: string
  term: string
  capacity: number
  selectedCount: number
  scheduleText: string
  classroom: string
  selectionStartAt: string
  selectionEndAt: string
  windowStatus: 'NOT_STARTED' | 'OPEN' | 'ENDED'
  selectableNow: boolean
  selected: boolean
}

export interface CourseSelection {
  selectionId: number
  offeringId: number
  courseCode: string
  courseName: string
  credit: number
  teacherName: string
  scheduleText: string
  classroom: string
  selectedAt: string
}

// 功能：查询当前学期可选教学班。
// 说明：选课页面用该接口加载课程表格，后端返回容量、已选人数、时间窗口和是否已选状态。
export function courseOfferingsApi(params?: { page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<CourseOffering>>>('/course-selection/offerings', { params })
}

// 功能：查询当前学生已选课程。
// 说明：用于选课页面下方“已选课程”分页表格，数据来自 course_selection 选课记录。
export function selectedCoursesApi(params?: { page?: number; size?: number }) {
  return http.get<never, ApiResponse<PageResponse<CourseSelection>>>('/course-selection/selected', { params })
}

// 功能：提交抢课请求。
// 说明：前端每次点击抢课都会生成唯一 requestId，后端用 Redis 幂等 key 防止重复提交，
// 并通过 Redis 库存 key 完成并发扣减和满员判断。
export function selectCourseApi(offeringId: number) {
  return http.post<never, ApiResponse<CourseSelection>>('/course-selection/grab', {
    offeringId,
    requestId: crypto.randomUUID(),
  })
}

// 功能：提交退课请求。
// 说明：根据选课记录 ID 删除当前学生自己的选课数据，页面随后重新加载容量和已选课程。
export function dropCourseApi(selectionId: number) {
  return http.delete<never, ApiResponse<void>>(`/course-selection/selected/${selectionId}`)
}
