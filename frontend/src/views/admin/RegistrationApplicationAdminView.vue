<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import type { ApplicationStatus } from '@/api/student'
import {
  adminRegistrationApplicationsApi,
  batchReviewRegistrationApplicationsApi,
  registrationTypeText,
  reviewRegistrationApplicationApi,
  type AdminRegistrationApplication,
  type RegistrationApplicationType,
} from '@/api/registration'

const loading = ref(false)
const reviewing = ref(false)
const records = ref<AdminRegistrationApplication[]>([])
const selectedRows = ref<AdminRegistrationApplication[]>([])
const currentApplication = ref<AdminRegistrationApplication | null>(null)
const reviewDialogVisible = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)

const filters = reactive({
  status: '' as ApplicationStatus | '',
  type: '' as RegistrationApplicationType | '',
  keyword: '',
})

const reviewForm = reactive({
  decision: 'APPROVE' as 'APPROVE' | 'REJECT',
  comment: '',
})

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
    const response = await adminRegistrationApplicationsApi({
      status: filters.status,
      type: filters.type,
      keyword: filters.keyword || undefined,
      page: page.value,
      size: size.value,
    })
    records.value = response.data.records
    page.value = response.data.page
    size.value = response.data.size
    total.value = response.data.total
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadRecords()
}

function handleSizeChange() {
  page.value = 1
  void loadRecords()
}

function openReviewDialog(row: AdminRegistrationApplication, decision: 'APPROVE' | 'REJECT') {
  currentApplication.value = row
  reviewForm.decision = decision
  reviewForm.comment = decision === 'APPROVE' ? '同意该报名申请。' : ''
  reviewDialogVisible.value = true
}

async function submitReview() {
  if (!currentApplication.value) return
  reviewing.value = true
  try {
    await reviewRegistrationApplicationApi(currentApplication.value.id, {
      decision: reviewForm.decision,
      comment: reviewForm.comment,
    })
    ElMessage.success(reviewForm.decision === 'APPROVE' ? '审核已通过' : '申请已驳回')
    reviewDialogVisible.value = false
    await loadRecords()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '审核失败'))
  } finally {
    reviewing.value = false
  }
}

async function batchReview(decision: 'APPROVE' | 'REJECT') {
  const ids = selectedRows.value.filter(canReview).map((row) => row.id)
  if (!ids.length) {
    ElMessage.warning('请选择待审核记录')
    return
  }
  const comment = decision === 'APPROVE'
    ? '批量审核通过。'
    : window.prompt('请输入统一驳回原因') || ''
  if (decision === 'REJECT' && !comment.trim()) {
    ElMessage.warning('批量驳回必须填写原因')
    return
  }
  reviewing.value = true
  try {
    const result = (await batchReviewRegistrationApplicationsApi({ ids, decision, comment })).data
    ElMessage.success(`批量审核完成：成功 ${result.successCount} 条，失败 ${result.failureCount} 条，任务 #${result.taskId}`)
    selectedRows.value = []
    await loadRecords()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '批量审核失败'))
  } finally {
    reviewing.value = false
  }
}

function canReview(row: AdminRegistrationApplication) {
  return row.status === 'SUBMITTED' || row.status === 'UNDER_REVIEW'
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
  <PageHeader title="报名申请审核" description="审核微专业、重修、学分替代、成绩加分和专业确认类申请。" />

  <section class="work-panel">
    <el-form class="filter-form" inline>
      <el-form-item label="状态">
        <el-select v-model="filters.status" class="status-filter" clearable>
          <el-option label="已提交" value="SUBMITTED" />
          <el-option label="审核中" value="UNDER_REVIEW" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
        </el-select>
      </el-form-item>
      <el-form-item label="类型">
        <el-select v-model="filters.type" class="keyword-input" clearable>
          <el-option
            v-for="(label, value) in registrationTypeText"
            :key="value"
            :label="label"
            :value="value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="filters.keyword" class="keyword-input" clearable placeholder="学号/姓名/目标/课程" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="search">查询</el-button>
        <el-button :disabled="!selectedRows.length" :loading="reviewing" @click="batchReview('APPROVE')">批量通过</el-button>
        <el-button type="danger" plain :disabled="!selectedRows.length" :loading="reviewing" @click="batchReview('REJECT')">批量驳回</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="records" empty-text="暂无报名申请" @selection-change="selectedRows = $event">
      <el-table-column type="selection" width="45" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="100" />
      <el-table-column label="申请类型" min-width="180">
        <template #default="{ row }">{{ registrationTypeText[row.type as RegistrationApplicationType] }}</template>
      </el-table-column>
      <el-table-column prop="targetName" label="申请目标" min-width="150" show-overflow-tooltip />
      <el-table-column prop="courseName" label="关联课程" min-width="140" show-overflow-tooltip>
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
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button type="success" link :disabled="!canReview(row)" @click="openReviewDialog(row, 'APPROVE')">
            通过
          </el-button>
          <el-button type="danger" link :disabled="!canReview(row)" @click="openReviewDialog(row, 'REJECT')">
            驳回
          </el-button>
        </template>
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
  </section>

  <el-dialog
    v-model="reviewDialogVisible"
    :title="reviewForm.decision === 'APPROVE' ? '通过申请' : '驳回申请'"
    width="520px"
  >
    <div v-if="currentApplication" class="review-context">
      <span>学生：{{ currentApplication.studentName }}（{{ currentApplication.studentNo }}）</span>
      <span>申请：{{ registrationTypeText[currentApplication.type] }} - {{ currentApplication.targetName }}</span>
      <span>说明：{{ currentApplication.reason }}</span>
    </div>
    <el-form label-width="90px">
      <el-form-item label="审核意见">
        <el-input v-model="reviewForm.comment" type="textarea" :rows="5" maxlength="500" show-word-limit />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="reviewDialogVisible = false">取消</el-button>
      <el-button
        type="primary"
        :loading="reviewing"
        :disabled="!reviewForm.comment.trim()"
        @click="submitReview"
      >
        确认
      </el-button>
    </template>
  </el-dialog>
</template>
