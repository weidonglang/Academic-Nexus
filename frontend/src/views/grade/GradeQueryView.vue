<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchGradesApi, type GradeRecord } from '@/api/academic'

const loading = ref(false)
const grades = ref<GradeRecord[]>([])

const summary = computed(() => {
  const credit = grades.value.reduce((total, item) => total + item.credit, 0)
  const weightedPoint = grades.value.reduce((total, item) => total + Number(item.gradePoint) * item.credit, 0)
  return {
    credit,
    averagePoint: credit ? (weightedPoint / credit).toFixed(2) : '-',
  }
})

onMounted(async () => {
  loading.value = true
  try {
    grades.value = await fetchGradesApi()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <PageHeader title="成绩查询" description="按学年学期查看成绩、学分、绩点和课程性质。" />
  <section class="query-summary">
    <article>
      <span>已获得学分</span>
      <strong>{{ summary.credit }}</strong>
    </article>
    <article>
      <span>平均绩点</span>
      <strong>{{ summary.averagePoint }}</strong>
    </article>
  </section>
  <section class="work-panel">
    <el-table v-loading="loading" :data="grades">
      <el-table-column prop="term" label="学期" width="140" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="courseType" label="课程性质" width="120" />
      <el-table-column prop="credit" label="学分" width="90" />
      <el-table-column prop="score" label="成绩" width="90" />
      <el-table-column prop="gradePoint" label="绩点" width="90" />
      <el-table-column prop="examType" label="考试类型" width="120" />
    </el-table>
  </section>
</template>
