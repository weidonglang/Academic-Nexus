<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  evaluationTasksApi,
  submitEvaluationApi,
  type EvaluationTask,
  type SubmitEvaluationPayload,
} from '@/api/evaluation'

const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const tasks = ref<EvaluationTask[]>([])
const currentTask = ref<EvaluationTask | null>(null)

const form = reactive<SubmitEvaluationPayload>({
  teachingScore: 5,
  contentScore: 5,
  interactionScore: 5,
  overallScore: 5,
  comment: '',
})

const pendingCount = computed(() => tasks.value.filter((task) => !task.evaluated).length)
const completedCount = computed(() => tasks.value.filter((task) => task.evaluated).length)

onMounted(loadTasks)

async function loadTasks() {
  loading.value = true
  try {
    const response = await evaluationTasksApi()
    tasks.value = response.data
  } finally {
    loading.value = false
  }
}

function openDialog(task: EvaluationTask) {
  currentTask.value = task
  form.teachingScore = 5
  form.contentScore = 5
  form.interactionScore = 5
  form.overallScore = 5
  form.comment = ''
  dialogVisible.value = true
}

async function submitEvaluation() {
  if (!currentTask.value) {
    return
  }
  saving.value = true
  try {
    await submitEvaluationApi(currentTask.value.offeringId, form)
    ElMessage.success('评价已提交')
    dialogVisible.value = false
    await loadTasks()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '提交评价失败'))
  } finally {
    saving.value = false
  }
}

function scoreText(task: EvaluationTask) {
  if (!task.evaluated) {
    return '-'
  }
  return `${task.overallScore ?? '-'} / 5`
}

function formatDateTime(value?: string) {
  if (!value) {
    return '-'
  }
  return new Date(value).toLocaleString('zh-CN')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (
    typeof error === 'object' &&
    error !== null &&
    'response' in error &&
    typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message === 'string'
  ) {
    return (error as { response: { data: { message: string } } }).response.data.message
  }
  return fallback
}
</script>

<template>
  <PageHeader title="教学评价" description="查看评价任务，填写量表并提交教学反馈。" />

  <section class="admin-toolbar">
    <div class="admin-summary">
      <article>
        <span>评价任务</span>
        <strong>{{ tasks.length }}</strong>
      </article>
      <article>
        <span>待评价</span>
        <strong>{{ pendingCount }}</strong>
      </article>
      <article>
        <span>已完成</span>
        <strong>{{ completedCount }}</strong>
      </article>
    </div>
  </section>

  <section v-loading="loading" class="work-panel">
    <el-table :data="tasks" empty-text="暂无评价任务，选课后会自动生成">
      <el-table-column prop="term" label="学期" width="130" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="scheduleText" label="上课时间" width="150" />
      <el-table-column prop="classroom" label="地点" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.evaluated ? 'success' : 'warning'">
            {{ row.evaluated ? '已评价' : '待评价' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="综合评分" width="100">
        <template #default="{ row }">{{ scoreText(row) }}</template>
      </el-table-column>
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :disabled="row.evaluated" @click="openDialog(row)">
            评价
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>

  <el-dialog v-model="dialogVisible" title="提交教学评价" width="560px">
    <div v-if="currentTask" class="review-context">
      <strong>{{ currentTask.courseName }} / {{ currentTask.teacherName }}</strong>
      <span>{{ currentTask.scheduleText }} · {{ currentTask.classroom }}</span>
    </div>
    <el-form label-width="110px" :model="form">
      <el-form-item label="教学态度">
        <el-rate v-model="form.teachingScore" />
      </el-form-item>
      <el-form-item label="教学内容">
        <el-rate v-model="form.contentScore" />
      </el-form-item>
      <el-form-item label="课堂互动">
        <el-rate v-model="form.interactionScore" />
      </el-form-item>
      <el-form-item label="综合评价">
        <el-rate v-model="form.overallScore" />
      </el-form-item>
      <el-form-item label="反馈意见">
        <el-input v-model="form.comment" type="textarea" :rows="5" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submitEvaluation">提交</el-button>
    </template>
  </el-dialog>
</template>
