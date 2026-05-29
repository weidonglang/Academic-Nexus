<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
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
  } finally {
    loading.value = false
  }
}

async function selectSummary(row: EvaluationSummary) {
  selectedOfferingId.value = row.offeringId
  await loadData()
}

async function clearOfferingFilter() {
  selectedOfferingId.value = undefined
  await loadData()
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
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
  </section>

  <section v-loading="loading" class="work-panel selection-panel">
    <div class="panel-heading">
      <h2>课程评价概览</h2>
      <span>点击课程行查看该课程明细</span>
    </div>
    <el-table :data="summaries" empty-text="暂无评价统计" @row-click="selectSummary">
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
  </section>

  <section v-loading="loading" class="work-panel">
    <div class="panel-heading">
      <h2>评价明细</h2>
      <span>{{ records.length }} 条</span>
    </div>
    <el-table :data="records" empty-text="暂无评价明细">
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
  </section>
</template>
