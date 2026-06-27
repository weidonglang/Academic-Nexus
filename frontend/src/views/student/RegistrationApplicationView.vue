<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import type { ApplicationStatus } from '@/api/student'
import {
  registrationApplicationsApi,
  registrationTypeText,
  submitRegistrationApplicationApi,
  type RegistrationApplication,
  type RegistrationApplicationType,
} from '@/api/registration'

const route = useRoute()
const loading = ref(false)
const submitting = ref(false)
const records = ref<RegistrationApplication[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const typeByRoute: Record<string, RegistrationApplicationType> = {
  '/registration/minor': 'MINOR_MAJOR_REGISTRATION',
  '/registration/retake': 'RETAKE_REGISTRATION',
  '/registration/credit-internal': 'INTERNAL_CREDIT_SUBSTITUTION',
  '/registration/credit-external': 'EXTERNAL_CREDIT_SUBSTITUTION',
  '/registration/score-bonus': 'SCORE_BONUS',
  '/registration/stream-confirm': 'STREAM_MAJOR_CONFIRMATION',
  '/registration/direction-confirm': 'MAJOR_DIRECTION_CONFIRMATION',
}

const form = reactive({
  targetName: '',
  courseName: '',
  reason: '',
})

const currentType = computed(() => typeByRoute[route.path] ?? 'MINOR_MAJOR_REGISTRATION')
const pageTitle = computed(() => registrationTypeText[currentType.value])
const targetLabel = computed(() => {
  switch (currentType.value) {
    case 'MINOR_MAJOR_REGISTRATION':
      return '报名微专业'
    case 'RETAKE_REGISTRATION':
      return '重修课程'
    case 'INTERNAL_CREDIT_SUBSTITUTION':
    case 'EXTERNAL_CREDIT_SUBSTITUTION':
      return '替代节点'
    case 'SCORE_BONUS':
      return '加分项目'
    case 'STREAM_MAJOR_CONFIRMATION':
      return '确认专业'
    case 'MAJOR_DIRECTION_CONFIRMATION':
      return '确认方向'
  }
})
const showCourseName = computed(() =>
  ['RETAKE_REGISTRATION', 'INTERNAL_CREDIT_SUBSTITUTION', 'EXTERNAL_CREDIT_SUBSTITUTION', 'SCORE_BONUS'].includes(
    currentType.value,
  ),
)

const statusText: Record<ApplicationStatus, string> = {
  SUBMITTED: '已提交',
  UNDER_REVIEW: '审核中',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELED: '已取消',
}

const statusType: Record<ApplicationStatus, 'info' | 'warning' | 'success' | 'danger'> = {
  SUBMITTED: 'info',
  UNDER_REVIEW: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELED: 'info',
}

onMounted(loadRecords)

watch(
  () => route.path,
  async () => {
    resetForm()
    page.value = 1
    await loadRecords()
  },
)

async function loadRecords() {
  loading.value = true
  try {
    const response = await registrationApplicationsApi({ type: currentType.value, page: page.value, size: size.value })
    records.value = response.data.records
    page.value = response.data.page
    size.value = response.data.size
    total.value = response.data.total
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '申请记录加载失败'))
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  void loadRecords()
}

async function submitApplication() {
  submitting.value = true
  try {
    await submitRegistrationApplicationApi({
      type: currentType.value,
      targetName: form.targetName,
      courseName: showCourseName.value ? form.courseName : undefined,
      reason: form.reason,
    })
    resetForm()
    ElMessage.success('申请已提交')
    await loadRecords()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '提交申请失败'))
  } finally {
    submitting.value = false
  }
}

function resetForm() {
  form.targetName = ''
  form.courseName = ''
  form.reason = ''
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
  <PageHeader :title="pageTitle" description="提交报名、确认或学分替代类申请，并跟踪审核处理结果。" />

  <section class="profile-grid">
    <article class="work-panel">
      <h2>提交申请</h2>
      <el-form label-width="98px">
        <el-form-item :label="targetLabel">
          <el-input v-model="form.targetName" maxlength="120" show-word-limit placeholder="请输入申请目标" />
        </el-form-item>
        <el-form-item v-if="showCourseName" label="关联课程">
          <el-input v-model="form.courseName" maxlength="120" show-word-limit placeholder="请输入课程或原成绩节点" />
        </el-form-item>
        <el-form-item label="申请说明">
          <el-input v-model="form.reason" type="textarea" :rows="6" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            :loading="submitting"
            :disabled="!form.targetName.trim() || !form.reason.trim()"
            @click="submitApplication"
          >
            提交申请
          </el-button>
        </el-form-item>
      </el-form>
    </article>

    <article v-loading="loading" class="work-panel">
      <h2>申请记录</h2>
      <el-table :data="records" empty-text="暂无申请记录">
        <el-table-column prop="targetName" :label="targetLabel" min-width="160" show-overflow-tooltip />
        <el-table-column prop="courseName" label="关联课程" min-width="150" show-overflow-tooltip>
          <template #default="{ row }">{{ row.courseName || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="105">
          <template #default="{ row }">
            <el-tag :type="statusType[row.status as ApplicationStatus]">
              {{ statusText[row.status as ApplicationStatus] }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column label="审核意见" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reviewComment || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        class="table-pagination"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        @current-change="loadRecords"
        @size-change="handleSizeChange"
      />
    </article>
  </section>
</template>
