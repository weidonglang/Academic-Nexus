<script setup lang="ts">
// 管理端成绩管理页面。
// 管理员可以分页查看成绩、录入成绩、导出基础数据，并维护发布、锁定、补考、重修、缓考状态。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { adminCoursesApi, type AdminCourse } from '@/api/adminCourse'
import {
  adminGradesApi,
  createAdminGradeApi,
  exportAdminGradesApi,
  importAdminGradesApi,
  updateAdminGradeApi,
  type AdminGrade,
  type AdminGradePayload,
} from '@/api/academicAdmin'

const DEFAULT_TERM = '2025-2026-2'
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const importVisible = ref(false)
const editing = ref<AdminGrade | null>(null)
const term = ref(DEFAULT_TERM)
const keyword = ref('')
const rows = ref<AdminGrade[]>([])
const courses = ref<AdminCourse[]>([])
const importText = ref('')

const form = reactive<AdminGradePayload>({
  studentNo: '',
  courseId: 0,
  term: DEFAULT_TERM,
  score: 90,
  gradePoint: 4,
  examType: '正常考试',
  gradeStatus: 'PUBLISHED',
  locked: false,
})

onMounted(loadData)

// 功能：加载管理端成绩列表。
// 说明：按学期和关键字查询成绩数据，展示学生、课程、分数、绩点、成绩状态和锁定状态。
async function loadData() {
  loading.value = true
  try {
    const [gradeResponse, courseResponse] = await Promise.all([
      adminGradesApi({ term: term.value, keyword: keyword.value.trim() }),
      adminCoursesApi(),
    ])
    rows.value = gradeResponse.data
    courses.value = courseResponse.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { studentNo: '', courseId: courses.value[0]?.courseId ?? 0, term: term.value, score: 90, gradePoint: 4, examType: '正常考试', gradeStatus: 'PUBLISHED', locked: false })
  dialogVisible.value = true
}

function openEdit(row: AdminGrade) {
  editing.value = row
  Object.assign(form, { studentNo: row.studentNo, courseId: row.courseId, term: row.term, score: row.score, gradePoint: row.gradePoint, examType: row.examType, gradeStatus: row.gradeStatus, locked: row.locked })
  dialogVisible.value = true
}

// 功能：保存成绩。
// 说明：新增或修改成绩时调用后端成绩管理接口，后端会校验学生、课程和成绩锁定状态。
async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await updateAdminGradeApi(editing.value.gradeId, form)
      ElMessage.success('成绩已更新')
    } else {
      await createAdminGradeApi(form)
      ElMessage.success('成绩已录入')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存成绩失败'))
  } finally {
    saving.value = false
  }
}

// 功能：导出成绩。
// 说明：调用后端导出接口获取当前学期成绩数据，作为成绩导入导出基础功能展示。
async function exportGrades() {
  const response = await exportAdminGradesApi(term.value)
  importText.value = JSON.stringify(response.data, null, 2)
  importVisible.value = true
}

async function importGrades() {
  try {
    const grades = JSON.parse(importText.value) as AdminGradePayload[]
    await importAdminGradesApi(grades)
    ElMessage.success('成绩已导入')
    importVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '导入失败，请检查 JSON 格式'))
  }
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'response' in error && typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message === 'string') {
    return (error as { response: { data: { message: string } } }).response.data.message
  }
  return fallback
}
</script>

<template>
  <PageHeader title="成绩管理" description="录入、修改、发布、锁定成绩，并预留导入导出入口。" />
  <section class="admin-toolbar">
    <div class="admin-actions">
      <el-input v-model="term" class="term-input" placeholder="学期" />
      <el-input v-model="keyword" class="keyword-input" placeholder="学号、姓名、课程" clearable />
      <el-button @click="loadData">查询</el-button>
      <el-button @click="exportGrades">导出 JSON</el-button>
      <el-button @click="importVisible = true">导入 JSON</el-button>
      <el-button type="primary" @click="openCreate">录入成绩</el-button>
    </div>
  </section>
  <section v-loading="loading" class="work-panel">
    <el-table :data="rows" empty-text="暂无成绩">
      <el-table-column prop="studentNo" label="学号" width="110" />
      <el-table-column prop="studentName" label="姓名" width="100" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column prop="score" label="成绩" width="80" />
      <el-table-column prop="gradePoint" label="绩点" width="80" />
      <el-table-column prop="examType" label="考试类型" width="120" />
      <el-table-column prop="gradeStatus" label="发布状态" width="110" />
      <el-table-column label="锁定" width="80"><template #default="{ row }">{{ row.locked ? '是' : '否' }}</template></el-table-column>
      <el-table-column label="操作" width="100" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="openEdit(row)">编辑</el-button></template></el-table-column>
    </el-table>
  </section>

  <el-dialog v-model="dialogVisible" :title="editing ? '编辑成绩' : '录入成绩'" width="560px">
    <el-form label-width="92px" :model="form">
      <el-form-item label="学号"><el-input v-model="form.studentNo" /></el-form-item>
      <el-form-item label="课程"><el-select v-model="form.courseId" class="full-field"><el-option v-for="course in courses" :key="course.courseId" :label="`${course.code} ${course.name}`" :value="course.courseId" /></el-select></el-form-item>
      <el-form-item label="学期"><el-input v-model="form.term" /></el-form-item>
      <el-form-item label="成绩"><el-input-number v-model="form.score" :min="0" :max="100" /></el-form-item>
      <el-form-item label="绩点"><el-input-number v-model="form.gradePoint" :min="0" :max="5" :step="0.1" /></el-form-item>
      <el-form-item label="考试类型"><el-select v-model="form.examType" class="full-field"><el-option label="正常考试" value="正常考试" /><el-option label="补考" value="补考" /><el-option label="重修" value="重修" /><el-option label="缓考" value="缓考" /></el-select></el-form-item>
      <el-form-item label="发布状态"><el-select v-model="form.gradeStatus" class="full-field"><el-option label="草稿" value="DRAFT" /><el-option label="已发布" value="PUBLISHED" /></el-select></el-form-item>
      <el-form-item label="锁定"><el-switch v-model="form.locked" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>

  <el-dialog v-model="importVisible" title="成绩导入/导出 JSON" width="720px">
    <el-input v-model="importText" type="textarea" :rows="16" />
    <template #footer><el-button @click="importVisible = false">关闭</el-button><el-button type="primary" @click="importGrades">导入</el-button></template>
  </el-dialog>
</template>
