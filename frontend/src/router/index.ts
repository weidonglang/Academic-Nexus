import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/auth/LoginView.vue'),
    meta: { public: true },
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue') },
      { path: 'ai/assistant', component: () => import('@/views/ai/AiAssistantView.vue') },
      { path: 'ai/chat', component: () => import('@/views/ai/AiChatView.vue') },
      { path: 'ai/academic-profile', component: () => import('@/views/ai/AcademicProfileView.vue') },
      { path: 'student/profile', component: () => import('@/views/student/ProfileView.vue') },
      { path: 'student/status-change', component: () => import('@/views/student/StatusChangeView.vue') },
      { path: 'registration/minor', component: () => import('@/views/student/RegistrationApplicationView.vue') },
      { path: 'registration/retake', component: () => import('@/views/student/RegistrationApplicationView.vue') },
      { path: 'registration/credit-internal', component: () => import('@/views/student/RegistrationApplicationView.vue') },
      { path: 'registration/credit-external', component: () => import('@/views/student/RegistrationApplicationView.vue') },
      { path: 'registration/score-bonus', component: () => import('@/views/student/RegistrationApplicationView.vue') },
      { path: 'registration/stream-confirm', component: () => import('@/views/student/RegistrationApplicationView.vue') },
      { path: 'registration/direction-confirm', component: () => import('@/views/student/RegistrationApplicationView.vue') },
      { path: 'course/selection', component: () => import('@/views/course/SelectionView.vue') },
      { path: 'schedule/personal', component: () => import('@/views/schedule/PersonalScheduleView.vue') },
      { path: 'classroom/free', component: () => import('@/views/schedule/FreeClassroomView.vue') },
      { path: 'information/academic-warning', component: () => import('@/views/information/InformationQueryView.vue') },
      { path: 'information/graduation-audit', component: () => import('@/views/information/InformationQueryView.vue') },
      { path: 'information/class-schedule', component: () => import('@/views/information/InformationQueryView.vue') },
      { path: 'information/course-roster', component: () => import('@/views/information/InformationQueryView.vue') },
      { path: 'information/academic-progress', component: () => import('@/views/information/InformationQueryView.vue') },
      { path: 'information/teaching-plan', component: () => import('@/views/information/InformationQueryView.vue') },
      { path: 'information/weekly-schedule', component: () => import('@/views/information/InformationQueryView.vue') },
      { path: 'information/feedback', component: () => import('@/views/information/TeachingFeedbackView.vue') },
      { path: 'information/thesis-grade', component: () => import('@/views/information/ThesisGradeView.vue') },
      { path: 'grade/query', component: () => import('@/views/grade/GradeQueryView.vue') },
      { path: 'exam/query', component: () => import('@/views/exam/ExamQueryView.vue') },
      { path: 'evaluation', component: () => import('@/views/evaluation/EvaluationView.vue') },
      { path: 'teacher/offerings', component: () => import('@/views/teacher/TeacherOfferingsView.vue') },
      { path: 'teacher/grades', component: () => import('@/views/teacher/TeacherGradesView.vue') },
      { path: 'teacher/exams', component: () => import('@/views/teacher/TeacherExamsView.vue') },
      { path: 'teacher/evaluations', component: () => import('@/views/teacher/TeacherEvaluationsView.vue') },
      { path: 'admin/course-offerings', component: () => import('@/views/admin/CourseOfferingAdminView.vue') },
      { path: 'admin/status-changes', component: () => import('@/views/admin/StatusChangeAdminView.vue') },
      { path: 'admin/role-permissions', component: () => import('@/views/admin/RolePermissionAdminView.vue') },
      { path: 'admin/users', component: () => import('@/views/admin/UserAdminView.vue') },
      { path: 'admin/evaluations', component: () => import('@/views/admin/EvaluationAdminView.vue') },
      { path: 'admin/grades', component: () => import('@/views/admin/GradeAdminView.vue') },
      { path: 'admin/exams', component: () => import('@/views/admin/ExamAdminView.vue') },
      { path: 'admin/notices', component: () => import('@/views/admin/NoticeAdminView.vue') },
      { path: 'admin/files', component: () => import('@/views/admin/FileAdminView.vue') },
      { path: 'admin/audit-logs', component: () => import('@/views/admin/AuditLogView.vue') },
      { path: 'admin/registration-applications', component: () => import('@/views/admin/RegistrationApplicationAdminView.vue') },
      { path: 'admin/redis-monitor', component: () => import('@/views/admin/RedisMonitorView.vue') },
      { path: 'admin/load-test-reports', component: () => import('@/views/admin/LoadTestReportsView.vue') },
      { path: 'admin/database-browser', component: () => import('@/views/admin/DatabaseBrowserView.vue') },
      { path: 'admin/ai-sql', component: () => import('@/views/admin/NaturalSqlAdminView.vue') },
      { path: 'admin/ai-logs', component: () => import('@/views/admin/AiCallLogAdminView.vue') },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// 功能：实现前端路由访问控制。
// 说明：未登录用户访问业务页面时跳转到登录页；已登录用户访问登录页时跳转首页；
// 同时根据角色限制学生、教师、管理员进入各自允许的页面范围。
router.beforeEach((to) => {
  const auth = useAuthStore()
  if (!to.meta.public && !auth.isAuthenticated) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && auth.isAuthenticated) {
    return '/dashboard'
  }
  if (!to.meta.public && auth.user) {
    // 开发者综合账号用于答辩演示，允许访问学生、教师和管理员全部页面。
    if (auth.user.username === '23111141') {
      return
    }
    const roles = auth.user.roles
    if (to.path.startsWith('/ai')) {
      return
    }
    if (roles.includes('ADMIN') && to.path !== '/dashboard' && !to.path.startsWith('/admin')) {
      return '/dashboard'
    }
    if (roles.includes('TEACHER') && !roles.includes('ADMIN') && to.path !== '/dashboard' && !to.path.startsWith('/teacher')) {
      return '/dashboard'
    }
    if (roles.includes('STUDENT') && !roles.includes('ADMIN') && !roles.includes('TEACHER')) {
      if (to.path.startsWith('/admin') || to.path.startsWith('/teacher')) {
        return '/dashboard'
      }
    }
  }
})

export default router
