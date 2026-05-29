<script setup lang="ts">
// 管理端考试安排维护页面。
// 用于维护考试时间、考场、座位号、监考教师和考试状态，学生端考试安排查询会读取这些数据。
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { adminCourseOfferingsApi, type AdminCourseOffering } from '@/api/adminCourse'
import { adminExamsApi, createAdminExamApi, deleteAdminExamApi, updateAdminExamApi, type AdminExam, type AdminExamPayload } from '@/api/academicAdmin'

const DEFAULT_TERM = '2025-2026-2'
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const editing = ref<AdminExam | null>(null)
const term = ref(DEFAULT_TERM)
const rows = ref<AdminExam[]>([])
const offerings = ref<AdminCourseOffering[]>([])

const form = reactive<AdminExamPayload>({
  offeringId: 0,
  examTime: '',
  room: '',
  seatNo: '',
  examType: '期末考试',
  status: '已安排',
  invigilator: '',
})

onMounted(loadData)

// 功能：加载考试安排列表。
// 说明：管理端按学期查询考试时间、考场、座位和监考信息，学生端考试查询会使用这些安排。
async function loadData() {
  loading.value = true
  try {
    const [examResponse, offeringResponse] = await Promise.all([adminExamsApi(term.value), adminCourseOfferingsApi(term.value)])
    rows.value = examResponse.data
    offerings.value = offeringResponse.data
  } finally {
    loading.value = false
  }
}

function openCreate() {
  editing.value = null
  Object.assign(form, { offeringId: offerings.value[0]?.offeringId ?? 0, examTime: '', room: '', seatNo: '', examType: '期末考试', status: '已安排', invigilator: '' })
  dialogVisible.value = true
}

function openEdit(row: AdminExam) {
  editing.value = row
  Object.assign(form, { offeringId: row.offeringId, examTime: row.examTime, room: row.room, seatNo: row.seatNo, examType: row.examType, status: row.status, invigilator: row.invigilator ?? '' })
  dialogVisible.value = true
}

// 功能：保存考试安排。
// 说明：新增或修改考试时间、地点、座位和监考教师，后端会同步通知已选该教学班的学生。
async function save() {
  saving.value = true
  try {
    if (editing.value) {
      await updateAdminExamApi(editing.value.examId, form)
      ElMessage.success('考试安排已更新')
    } else {
      await createAdminExamApi(form)
      ElMessage.success('考试安排已新增')
    }
    dialogVisible.value = false
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '保存考试安排失败'))
  } finally {
    saving.value = false
  }
}

// 功能：删除考试安排。
// 说明：管理员确认后删除指定考试记录，用于考试安排维护和纠错。
async function remove(row: AdminExam) {
  await ElMessageBox.confirm(`确认删除 ${row.courseName} 的考试安排吗？`, '删除考试安排', { type: 'warning' })
  await deleteAdminExamApi(row.examId)
  ElMessage.success('考试安排已删除')
  await loadData()
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'response' in error && typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message === 'string') {
    return (error as { response: { data: { message: string } } }).response.data.message
  }
  return fallback
}
</script>

<template>
  <PageHeader title="考试管理" description="维护考试时间、考场、座位范围、监考信息，并联动学生考试安排。" />
  <section class="admin-toolbar">
    <div class="admin-actions">
      <el-input v-model="term" class="term-input" placeholder="学期" />
      <el-button @click="loadData">查询</el-button>
      <el-button type="primary" @click="openCreate">新增考试</el-button>
    </div>
  </section>
  <section v-loading="loading" class="work-panel">
    <el-table :data="rows" empty-text="暂无考试安排">
      <el-table-column prop="term" label="学期" width="130" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="examTime" label="考试时间" width="180" />
      <el-table-column prop="room" label="考场" width="130" />
      <el-table-column prop="seatNo" label="座位/范围" width="110" />
      <el-table-column prop="invigilator" label="监考" width="120" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="操作" width="130" fixed="right"><template #default="{ row }"><el-button type="primary" link @click="openEdit(row)">编辑</el-button><el-button type="danger" link @click="remove(row)">删除</el-button></template></el-table-column>
    </el-table>
  </section>
  <el-dialog v-model="dialogVisible" :title="editing ? '编辑考试' : '新增考试'" width="560px">
    <el-form label-width="92px" :model="form">
      <el-form-item label="教学班"><el-select v-model="form.offeringId" class="full-field" filterable><el-option v-for="item in offerings" :key="item.offeringId" :label="`${item.courseCode} ${item.courseName} / ${item.teacherName}`" :value="item.offeringId" /></el-select></el-form-item>
      <el-form-item label="考试时间"><el-date-picker v-model="form.examTime" class="full-field" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss" /></el-form-item>
      <el-form-item label="考场"><el-input v-model="form.room" /></el-form-item>
      <el-form-item label="座位/范围"><el-input v-model="form.seatNo" /></el-form-item>
      <el-form-item label="考试类型"><el-input v-model="form.examType" /></el-form-item>
      <el-form-item label="状态"><el-input v-model="form.status" /></el-form-item>
      <el-form-item label="监考"><el-input v-model="form.invigilator" /></el-form-item>
    </el-form>
    <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" :loading="saving" @click="save">保存</el-button></template>
  </el-dialog>
</template>
