<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchGradesApi, type GradeRecord } from '@/api/academic'

const loading = ref(false)
const grades = ref<GradeRecord[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const summary = computed(() => {
  const credit = grades.value.reduce((total, item) => total + item.credit, 0)
  const weightedPoint = grades.value.reduce((total, item) => total + Number(item.gradePoint) * item.credit, 0)
  return {
    credit,
    averagePoint: credit ? (weightedPoint / credit).toFixed(2) : '-',
  }
})

onMounted(loadGrades)

async function loadGrades() {
  loading.value = true
  try {
    const response = await fetchGradesApi({ page: page.value, size: size.value })
    grades.value = response.records
    page.value = response.page
    size.value = response.size
    total.value = response.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  void loadGrades()
}
</script>

<template>
  <PageHeader title="成绩查询" description="按学年学期查看成绩、学分、绩点和课程性质。" />
  <section class="query-summary">
    <article>
      <span>当前页学分</span>
      <strong>{{ summary.credit }}</strong>
    </article>
    <article>
      <span>当前页平均绩点</span>
      <strong>{{ summary.averagePoint }}</strong>
    </article>
    <article>
      <span>成绩总数</span>
      <strong>{{ total }}</strong>
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
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="table-pagination"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      @current-change="loadGrades"
      @size-change="handleSizeChange"
    />
  </section>
</template>
