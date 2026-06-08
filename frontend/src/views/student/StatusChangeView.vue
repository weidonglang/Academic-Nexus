<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  statusChangeApplicationsApi,
  submitStatusChangeApplicationApi,
  type ApplicationStatus,
  type StatusChangeApplication,
  type StatusChangeType,
} from '@/api/student'

const loading = ref(false)
const submitting = ref(false)
const records = ref<StatusChangeApplication[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const form = reactive({
  type: 'SUSPEND' as StatusChangeType,
  reason: '',
})

const typeText: Record<StatusChangeType, string> = {
  SUSPEND: '休学',
  RESUME: '复学',
  TRANSFER_MAJOR: '转专业',
  OTHER: '其他',
}

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

async function loadRecords() {
  loading.value = true
  try {
    const response = await statusChangeApplicationsApi({ page: page.value, size: size.value })
    records.value = response.data.records
    page.value = response.data.page
    size.value = response.data.size
    total.value = response.data.total
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
    await submitStatusChangeApplicationApi({ type: form.type, reason: form.reason })
    form.reason = ''
    ElMessage.success('申请已提交')
    await loadRecords()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '提交申请失败'))
  } finally {
    submitting.value = false
  }
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
  <PageHeader title="学籍异动申请" description="提交休学、复学、转专业等申请，并跟踪审核状态。" />

  <section class="profile-grid">
    <article class="work-panel">
      <h2>提交申请</h2>
      <el-form label-width="90px">
        <el-form-item label="异动类型">
          <el-select v-model="form.type" class="full-field">
            <el-option label="休学" value="SUSPEND" />
            <el-option label="复学" value="RESUME" />
            <el-option label="转专业" value="TRANSFER_MAJOR" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请原因">
          <el-input v-model="form.reason" type="textarea" :rows="6" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" :disabled="!form.reason.trim()" @click="submitApplication">
            提交申请
          </el-button>
        </el-form-item>
      </el-form>
    </article>

    <article v-loading="loading" class="work-panel">
      <h2>申请记录</h2>
      <el-table :data="records" empty-text="暂无申请记录">
        <el-table-column label="类型" width="110">
          <template #default="{ row }">{{ typeText[row.type as StatusChangeType] }}</template>
        </el-table-column>
        <el-table-column prop="reason" label="原因" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
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
