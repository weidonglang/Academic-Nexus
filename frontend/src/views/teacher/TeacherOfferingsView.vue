<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { teacherOfferingsApi, type TeacherOffering } from '@/api/teacher'

const DEFAULT_TERM = '2025-2026-2'
const loading = ref(false)
const term = ref(DEFAULT_TERM)
const rows = ref<TeacherOffering[]>([])
const page = ref(1)
const size = ref(10)
const pagedRows = computed(() => rows.value.slice((page.value - 1) * size.value, page.value * size.value))

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    rows.value = (await teacherOfferingsApi(term.value)).data
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadData()
}

function handleSizeChange() {
  page.value = 1
}
</script>

<template>
  <PageHeader title="任课课程" description="查看当前教师承担的教学班、容量和上课安排。" />
  <section class="admin-toolbar">
    <div class="admin-actions">
      <el-input v-model="term" class="term-input" placeholder="学期" />
      <el-button type="primary" @click="search">查询</el-button>
    </div>
  </section>
  <section v-loading="loading" class="work-panel">
    <el-table :data="pagedRows" empty-text="暂无任课课程">
      <el-table-column prop="term" label="学期" width="130" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="160" />
      <el-table-column prop="category" label="类别" width="120" />
      <el-table-column label="人数" width="100">
        <template #default="{ row }">{{ row.selectedCount }}/{{ row.capacity }}</template>
      </el-table-column>
      <el-table-column prop="scheduleText" label="上课时间" width="160" />
      <el-table-column prop="classroom" label="教室" width="150" />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="table-pagination"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50, 100]"
      :total="rows.length"
      @size-change="handleSizeChange"
    />
  </section>
</template>
