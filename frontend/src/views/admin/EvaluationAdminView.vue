<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  adminEvaluationRecordsApi,
  adminEvaluationSummariesApi,
  type EvaluationRecord,
  type EvaluationSummary,
} from '@/api/evaluation'

const DEFAULT_TERM = '2025-2026-2'

const loading = ref(false)
const term = ref(DEFAULT_TERM)
const selectedOfferingId = ref<number | undefined>()
const summaries = ref<EvaluationSummary[]>([])
const records = ref<EvaluationRecord[]>([])
const summaryPage = ref(1)
const summarySize = ref(10)
const recordPage = ref(1)
const recordSize = ref(10)
const pagedSummaries = computed(() => summaries.value.slice((summaryPage.value - 1) * summarySize.value, summaryPage.value * summarySize.value))
const pagedRecords = computed(() => records.value.slice((recordPage.value - 1) * recordSize.value, recordPage.value * recordSize.value))

const submittedTotal = computed(() => summaries.value.reduce((total, item) => total + item.submittedCount, 0))
const selectedTotal = computed(() => summaries.value.reduce((total, item) => total + item.selectedCount, 0))
const completionRate = computed(() => {
  if (!selectedTotal.value) {
    return '0%'
  }
  return `${Math.round((submittedTotal.value / selectedTotal.value) * 100)}%`
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [summaryResponse, recordResponse] = await Promise.all([
      adminEvaluationSummariesApi(term.value),
      adminEvaluationRecordsApi({ term: term.value, offeringId: selectedOfferingId.value }),
    ])
    summaries.value = summaryResponse.data
    records.value = recordResponse.data
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '评价数据加载失败'))
  } finally {
    loading.value = false
  }
}

async function selectSummary(row: EvaluationSummary) {
  selectedOfferingId.value = row.offeringId
  recordPage.value = 1
  await loadData()
}

async function clearOfferingFilter() {
  const hadFilter = selectedOfferingId.value !== undefined
  selectedOfferingId.value = undefined
  recordPage.value = 1
  await loadData()
  ElMessage.info(hadFilter ? '已切换为全部评价明细' : '当前已经是全部评价明细')
}

function search() {
  summaryPage.value = 1
  recordPage.value = 1
  void loadData()
}

function handleSummarySizeChange() {
  summaryPage.value = 1
}

function handleRecordSizeChange() {
  recordPage.value = 1
}

function formatScore(value?: number) {
  return typeof value === 'number' ? value.toFixed(2) : '-'
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
  <PageHeader title="教学评价统计" description="按课程查看评价完成率、平均分和学生反馈明细。" />

  <section class="admin-toolbar">
    <div class="admin-summary">
      <article>
        <span>应评人次</span>
        <strong>{{ selectedTotal }}</strong>
      </article>
      <article>
        <span>已评人次</span>
        <strong>{{ submittedTotal }}</strong>
      </article>
      <article>
        <span>完成率</span>
        <strong>{{ completionRate }}</strong>
      </article>
    </div>
    <div class="admin-actions">
      <el-input v-model="term" class="term-input" placeholder="学期" clearable />
      <el-button @click="clearOfferingFilter">全部明细</el-button>
      <el-button type="primary" @click="search">查询</el-button>
    </div>
  </section>

  <section v-loading="loading" class="work-panel selection-panel">
    <div class="panel-heading">
      <h2>课程评价概览</h2>
      <span>点击课程行查看该课程明细</span>
    </div>
    <el-table :data="pagedSummaries" empty-text="暂无评价统计" @row-click="selectSummary">
      <el-table-column prop="term" label="学期" width="130" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column label="完成情况" width="120">
        <template #default="{ row }">{{ row.submittedCount }}/{{ row.selectedCount }}</template>
      </el-table-column>
      <el-table-column label="教学态度" width="110">
        <template #default="{ row }">{{ formatScore(row.averageTeachingScore) }}</template>
      </el-table-column>
      <el-table-column label="教学内容" width="110">
        <template #default="{ row }">{{ formatScore(row.averageContentScore) }}</template>
      </el-table-column>
      <el-table-column label="课堂互动" width="110">
        <template #default="{ row }">{{ formatScore(row.averageInteractionScore) }}</template>
      </el-table-column>
      <el-table-column label="综合评价" width="110">
        <template #default="{ row }">{{ formatScore(row.averageOverallScore) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="summaryPage"
      v-model:page-size="summarySize"
      class="table-pagination"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50, 100]"
      :total="summaries.length"
      @size-change="handleSummarySizeChange"
    />
  </section>

  <section v-loading="loading" class="work-panel">
    <div class="panel-heading">
      <h2>评价明细</h2>
      <span>{{ records.length }} 条</span>
    </div>
    <el-table :data="pagedRecords" empty-text="暂无评价明细">
      <el-table-column prop="studentNo" label="学号" width="110" />
      <el-table-column prop="studentName" label="姓名" width="100" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="overallScore" label="综合" width="80" />
      <el-table-column prop="comment" label="反馈意见" min-width="220" show-overflow-tooltip />
      <el-table-column label="提交时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="recordPage"
      v-model:page-size="recordSize"
      class="table-pagination"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50, 100]"
      :total="records.length"
      @size-change="handleRecordSizeChange"
    />
  </section>
</template>
