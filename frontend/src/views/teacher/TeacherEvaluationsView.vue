<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { teacherEvaluationsApi } from '@/api/teacher'
import type { EvaluationSummary } from '@/api/evaluation'

const DEFAULT_TERM = '2025-2026-2'
const loading = ref(false)
const term = ref(DEFAULT_TERM)
const rows = ref<EvaluationSummary[]>([])

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    rows.value = (await teacherEvaluationsApi(term.value)).data
  } finally {
    loading.value = false
  }
}

function score(value?: number) {
  return typeof value === 'number' ? value.toFixed(2) : '-'
}
</script>

<template>
  <PageHeader title="评价结果" description="查看本人任课课程的教学评价完成率和平均分。" />
  <section class="admin-toolbar">
    <div class="admin-actions">
      <el-input v-model="term" class="term-input" placeholder="学期" />
      <el-button type="primary" @click="loadData">查询</el-button>
    </div>
  </section>
  <section v-loading="loading" class="work-panel">
    <el-table :data="rows" empty-text="暂无评价结果">
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column label="完成情况" width="120">
        <template #default="{ row }">{{ row.submittedCount }}/{{ row.selectedCount }}</template>
      </el-table-column>
      <el-table-column label="教学态度" width="110"><template #default="{ row }">{{ score(row.averageTeachingScore) }}</template></el-table-column>
      <el-table-column label="教学内容" width="110"><template #default="{ row }">{{ score(row.averageContentScore) }}</template></el-table-column>
      <el-table-column label="课堂互动" width="110"><template #default="{ row }">{{ score(row.averageInteractionScore) }}</template></el-table-column>
      <el-table-column label="综合评价" width="110"><template #default="{ row }">{{ score(row.averageOverallScore) }}</template></el-table-column>
    </el-table>
  </section>
</template>
