<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { thesisGradeApi, type ThesisGrade } from '@/api/information'

const loading = ref(false)
const records = ref<ThesisGrade[]>([])

onMounted(async () => {
  loading.value = true
  try {
    records.value = (await thesisGradeApi()).data
  } finally {
    loading.value = false
  }
})

function formatDateTime(value?: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<template>
  <PageHeader title="毕业设计（论文）成绩查看" description="查看开题、中期、答辩和最终成绩状态。" />

  <section class="work-panel">
    <el-table v-loading="loading" :data="records" empty-text="暂无毕业设计成绩">
      <el-table-column prop="title" label="论文题目" min-width="220" show-overflow-tooltip />
      <el-table-column prop="advisor" label="指导教师" width="110" />
      <el-table-column prop="proposalScore" label="开题" width="90">
        <template #default="{ row }">{{ row.proposalScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="midtermScore" label="中期" width="90">
        <template #default="{ row }">{{ row.midtermScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="defenseScore" label="答辩" width="90">
        <template #default="{ row }">{{ row.defenseScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="finalScore" label="最终成绩" width="110">
        <template #default="{ row }">{{ row.finalScore ?? '-' }}</template>
      </el-table-column>
      <el-table-column prop="gradeLevel" label="等级" width="90">
        <template #default="{ row }">{{ row.gradeLevel || '-' }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="更新时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.updatedAt) }}</template>
      </el-table-column>
    </el-table>
  </section>
</template>
