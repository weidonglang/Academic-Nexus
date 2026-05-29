<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchExamsApi, type ExamSchedule } from '@/api/academic'

const loading = ref(false)
const exams = ref<ExamSchedule[]>([])

onMounted(async () => {
  loading.value = true
  try {
    exams.value = await fetchExamsApi()
  } finally {
    loading.value = false
  }
})
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
  </section>
</template>
