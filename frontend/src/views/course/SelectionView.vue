<script setup lang="ts">
// 学生自主选课页面。
// 页面负责分页展示可选教学班、已选课程和选课/退课按钮；真正的并发扣库存、
// 幂等请求和短锁判断在后端 Redis 抢课服务中完成。
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  courseOfferingsApi,
  dropCourseApi,
  selectCourseApi,
  selectedCoursesApi,
  type CourseOffering,
  type CourseSelection,
} from '@/api/courseSelection'

const loading = ref(false)
const actionLoading = ref<number | null>(null)
const offerings = ref<CourseOffering[]>([])
const selectedCourses = ref<CourseSelection[]>([])
const offeringPage = ref(1)
const offeringPageSize = ref(20)
const offeringTotal = ref(0)
const selectedPage = ref(1)
const selectedPageSize = ref(10)
const selectedTotal = ref(0)

const statusText: Record<CourseOffering['windowStatus'], string> = {
  NOT_STARTED: '未开始',
  OPEN: '进行中',
  ENDED: '已结束',
}

const statusType: Record<CourseOffering['windowStatus'], 'info' | 'success' | 'warning'> = {
  NOT_STARTED: 'info',
  OPEN: 'success',
  ENDED: 'warning',
}

onMounted(loadData)

// 功能：加载选课页面全部数据。
// 说明：同时查询可选教学班和当前学生已选课程，保证页面容量、按钮状态和已选列表同步。
async function loadData() {
  loading.value = true
  try {
    await Promise.all([loadOfferings(), loadSelectedCourses()])
  } finally {
    loading.value = false
  }
}

// 功能：分页查询可选教学班。
// 说明：后端返回教学班容量、已选人数、选课时间窗口等信息，前端据此展示是否可抢。
async function loadOfferings() {
  const response = await courseOfferingsApi({ page: offeringPage.value, size: offeringPageSize.value })
  offerings.value = response.data.records
  offeringTotal.value = response.data.total
}

// 功能：分页查询已选课程。
// 说明：学生成功抢课或退课后重新加载，确保已选列表与数据库选课记录一致。
async function loadSelectedCourses() {
  const response = await selectedCoursesApi({ page: selectedPage.value, size: selectedPageSize.value })
  selectedCourses.value = response.data.records
  selectedTotal.value = response.data.total
}

function handleOfferingPageChange(nextPage: number) {
  offeringPage.value = nextPage
  loadOfferings()
}

function handleOfferingSizeChange(nextSize: number) {
  offeringPageSize.value = nextSize
  offeringPage.value = 1
  loadOfferings()
}

function handleSelectedPageChange(nextPage: number) {
  selectedPage.value = nextPage
  loadSelectedCourses()
}

function handleSelectedSizeChange(nextSize: number) {
  selectedPageSize.value = nextSize
  selectedPage.value = 1
  loadSelectedCourses()
}

async function selectCourse(offering: CourseOffering) {
  // 功能：点击抢课按钮后调用后端抢课接口。
  // 说明：API 层会生成 requestId，后端通过 Redis 幂等、短锁和库存扣减判断抢课结果。
  actionLoading.value = offering.offeringId
  try {
    await selectCourseApi(offering.offeringId)
    ElMessage.success('选课成功')
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '选课失败'))
  } finally {
    actionLoading.value = null
  }
}

async function dropCourse(selection: CourseSelection) {
  // 功能：执行退课操作。
  // 说明：退课成功后重新加载可选教学班和已选课程，使容量和按钮状态立即刷新。
  actionLoading.value = selection.offeringId
  try {
    await dropCourseApi(selection.selectionId)
    ElMessage.success('退课成功')
    if (selectedCourses.value.length === 1 && selectedPage.value > 1) {
      selectedPage.value -= 1
    }
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '退课失败'))
  } finally {
    actionLoading.value = null
  }
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
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
  <PageHeader title="自主选课" description="查看本学期可选课程，在开放时间内完成选课和退课操作。" />

  <section v-loading="loading" class="work-panel selection-panel">
    <div class="panel-heading">
      <h2>可选课程</h2>
      <el-tag type="info">2025-2026-2</el-tag>
    </div>
    <el-table :data="offerings" empty-text="暂无可选课程">
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="category" label="类别" width="130" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column label="容量" width="110">
        <template #default="{ row }">{{ row.selectedCount }}/{{ row.capacity }}</template>
      </el-table-column>
      <el-table-column label="选课状态" width="110">
        <template #default="{ row }">
          <el-tag :type="statusType[row.windowStatus as CourseOffering['windowStatus']]">
            {{ statusText[row.windowStatus as CourseOffering['windowStatus']] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开放时间" width="210">
        <template #default="{ row }">
          {{ formatDateTime(row.selectionStartAt) }} - {{ formatDateTime(row.selectionEndAt) }}
        </template>
      </el-table-column>
      <el-table-column prop="scheduleText" label="上课时间" width="150" />
      <el-table-column prop="classroom" label="地点" width="150" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button
            type="primary"
            link
            :loading="actionLoading === row.offeringId"
            :disabled="row.selected || !row.selectableNow || row.selectedCount >= row.capacity"
            @click="selectCourse(row)"
          >
            {{ row.selected ? '已选' : '选课' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="offeringPage"
        v-model:page-size="offeringPageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="offeringTotal"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handleOfferingPageChange"
        @size-change="handleOfferingSizeChange"
      />
    </div>
  </section>

  <section v-loading="loading" class="work-panel selected-panel">
    <div class="panel-heading">
      <h2>已选课程</h2>
      <span>{{ selectedTotal }} 门</span>
    </div>
    <el-table :data="selectedCourses" empty-text="暂无已选课程">
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程名称" min-width="150" />
      <el-table-column prop="teacherName" label="教师" width="110" />
      <el-table-column prop="credit" label="学分" width="80" />
      <el-table-column prop="scheduleText" label="上课时间" width="150" />
      <el-table-column prop="classroom" label="地点" width="150" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="danger" link :loading="actionLoading === row.offeringId" @click="dropCourse(row)">
            退课
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <div class="pagination-bar">
      <el-pagination
        v-model:current-page="selectedPage"
        v-model:page-size="selectedPageSize"
        :page-sizes="[5, 10, 20, 50]"
        :total="selectedTotal"
        layout="total, sizes, prev, pager, next, jumper"
        background
        @current-change="handleSelectedPageChange"
        @size-change="handleSelectedSizeChange"
      />
    </div>
  </section>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
}
</style>
