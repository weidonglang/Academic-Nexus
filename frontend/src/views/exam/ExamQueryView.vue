<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchExamsApi, type ExamSchedule } from '@/api/academic'

const loading = ref(false)
const exams = ref<ExamSchedule[]>([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

onMounted(loadExams)

async function loadExams() {
  loading.value = true
  try {
    const response = await fetchExamsApi({ page: page.value, size: size.value })
    exams.value = response.records
    page.value = response.page
    size.value = response.size
    total.value = response.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  void loadExams()
}
</script>

<template>
  <PageHeader title="考试安排" description="查看考试课程、时间、地点、座位和安排状态。" />
  <section class="work-panel">
    <el-table v-loading="loading" :data="exams">
      <el-table-column prop="term" label="学期" width="140" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="examTime" label="考试时间" width="180" />
      <el-table-column prop="room" label="考场" width="140" />
      <el-table-column prop="seatNo" label="座位" width="90" />
      <el-table-column prop="examType" label="类型" width="110" />
      <el-table-column prop="status" label="状态" width="100">
        <template #default="{ row }">
          <el-tag type="success">{{ row.status }}</el-tag>
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
      @current-change="loadExams"
      @size-change="handleSizeChange"
    />
  </section>
</template>
