<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PageHeader from '@/components/PageHeader.vue'
import {
  academicProgressApi,
  academicWarningsApi,
  classSchedulesApi,
  courseRostersApi,
  graduationAuditApi,
  offeringOptionsApi,
  teachingPlanApi,
  weeklyScheduleApi,
  type AcademicProgress,
  type AcademicWarning,
  type ClassSchedule,
  type CourseRoster,
  type GraduationAudit,
  type OfferingOption,
  type TeachingPlan,
  type WeeklySchedule,
} from '@/api/information'

type QueryMode =
  | 'warning'
  | 'graduation'
  | 'classSchedule'
  | 'roster'
  | 'progress'
  | 'plan'
  | 'weeklySchedule'

const route = useRoute()
const loading = ref(false)
const rows = ref<
  Array<AcademicWarning | GraduationAudit | ClassSchedule | CourseRoster | AcademicProgress | TeachingPlan | WeeklySchedule>
>([])
const offerings = ref<OfferingOption[]>([])

const filters = reactive({
  term: '2025-2026-2',
  className: '软件工程 23-1',
  offeringId: undefined as number | undefined,
  major: '软件工程',
  grade: '2023',
  week: 1,
})

const modeByPath: Record<string, QueryMode> = {
  '/information/academic-warning': 'warning',
  '/information/graduation-audit': 'graduation',
  '/information/class-schedule': 'classSchedule',
  '/information/course-roster': 'roster',
  '/information/academic-progress': 'progress',
  '/information/teaching-plan': 'plan',
  '/information/weekly-schedule': 'weeklySchedule',
}

const mode = computed(() => modeByPath[route.path] ?? 'warning')
const pageTitle = computed(() => {
  switch (mode.value) {
    case 'warning':
      return '学籍预警查询'
    case 'graduation':
      return '毕业审核结果核查'
    case 'classSchedule':
      return '班级课表查询'
    case 'roster':
      return '选课名单查询'
    case 'progress':
      return '学生学业情况查询'
    case 'plan':
      return '教学执行计划查看'
    case 'weeklySchedule':
      return '学生课表查询（按周次）'
  }
})

const description = computed(() => {
  switch (mode.value) {
    case 'warning':
      return '查看学籍预警、处理状态和预警原因。'
    case 'graduation':
      return '核查毕业条件、当前完成情况和审核结论。'
    case 'classSchedule':
      return '按班级和学期查看教学班课表。'
    case 'roster':
      return '按教学班查看已选课学生名单。'
    case 'progress':
      return '按课程类别汇总已修学分和平均成绩。'
    case 'plan':
      return '按年级和专业查看教学执行计划。'
    case 'weeklySchedule':
      return '按周次查看个人课表安排。'
  }
})

onMounted(async () => {
  await loadOptions()
  await loadRows()
})

watch(
  () => route.path,
  async () => {
    rows.value = []
    await loadOptions()
    await loadRows()
  },
)

async function loadOptions() {
  if (mode.value !== 'roster') return
  const response = await offeringOptionsApi({ term: filters.term })
  offerings.value = response.data
  if (!filters.offeringId && offerings.value.length) {
    filters.offeringId = offerings.value[0].offeringId
  }
}

async function loadRows() {
  loading.value = true
  try {
    if (mode.value === 'warning') rows.value = (await academicWarningsApi()).data
    if (mode.value === 'graduation') rows.value = (await graduationAuditApi()).data
    if (mode.value === 'classSchedule') {
      rows.value = (await classSchedulesApi({ className: filters.className, term: filters.term })).data
    }
    if (mode.value === 'roster') {
      rows.value = (await courseRostersApi({ offeringId: filters.offeringId, term: filters.term })).data
    }
    if (mode.value === 'progress') rows.value = (await academicProgressApi()).data
    if (mode.value === 'plan') {
      rows.value = (await teachingPlanApi({ major: filters.major, grade: filters.grade })).data
    }
    if (mode.value === 'weeklySchedule') {
      rows.value = (await weeklyScheduleApi({ week: filters.week })).data
    }
  } finally {
    loading.value = false
  }
}

async function query() {
  await loadOptions()
  await loadRows()
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<template>
  <PageHeader :title="pageTitle" :description="description" />

  <section class="work-panel">
    <el-form v-if="['classSchedule', 'roster', 'plan', 'weeklySchedule'].includes(mode)" class="filter-form" inline>
      <el-form-item v-if="mode === 'classSchedule'" label="班级">
        <el-input v-model="filters.className" class="keyword-input" />
      </el-form-item>
      <el-form-item v-if="mode === 'classSchedule' || mode === 'roster'" label="学期">
        <el-input v-model="filters.term" class="term-input" />
      </el-form-item>
      <el-form-item v-if="mode === 'roster'" label="教学班">
        <el-select v-model="filters.offeringId" class="keyword-input">
          <el-option
            v-for="item in offerings"
            :key="item.offeringId"
            :label="`${item.courseCode} ${item.courseName}`"
            :value="item.offeringId"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="mode === 'plan'" label="专业">
        <el-input v-model="filters.major" class="keyword-input" />
      </el-form-item>
      <el-form-item v-if="mode === 'plan'" label="年级">
        <el-input v-model="filters.grade" class="term-input" />
      </el-form-item>
      <el-form-item v-if="mode === 'weeklySchedule'" label="周次">
        <el-input-number v-model="filters.week" :min="1" :max="24" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="query">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-if="mode === 'warning'" v-loading="loading" :data="rows" empty-text="暂无学籍预警">
      <el-table-column prop="term" label="学期" width="130" />
      <el-table-column prop="level" label="预警等级" width="110" />
      <el-table-column prop="reason" label="原因" min-width="260" />
      <el-table-column prop="status" label="状态" width="110" />
      <el-table-column label="时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <el-table v-if="mode === 'graduation'" v-loading="loading" :data="rows" empty-text="暂无毕业审核记录">
      <el-table-column prop="auditItem" label="审核项" width="150" />
      <el-table-column prop="requiredValue" label="要求" width="140" />
      <el-table-column prop="currentValue" label="当前情况" width="180" />
      <el-table-column label="结果" width="100">
        <template #default="{ row }">
          <el-tag :type="row.passed ? 'success' : 'warning'">{{ row.passed ? '通过' : '未完成' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="说明" min-width="220" />
    </el-table>

    <el-table v-if="mode === 'classSchedule'" v-loading="loading" :data="rows" empty-text="暂无班级课表">
      <el-table-column prop="term" label="学期" width="130" />
      <el-table-column prop="className" label="班级" width="150" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="scheduleText" label="时间" width="130" />
      <el-table-column prop="classroom" label="教室" width="130" />
    </el-table>

    <el-table v-if="mode === 'roster'" v-loading="loading" :data="rows" empty-text="暂无选课名单">
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column prop="studentNo" label="学号" width="120" />
      <el-table-column prop="studentName" label="姓名" width="100" />
      <el-table-column prop="className" label="班级" width="150" />
      <el-table-column prop="selectedAt" label="选课时间" width="170">
        <template #default="{ row }">{{ formatDateTime(row.selectedAt) }}</template>
      </el-table-column>
    </el-table>

    <el-table v-if="mode === 'progress'" v-loading="loading" :data="rows" empty-text="暂无学业进度">
      <el-table-column prop="courseType" label="课程类别" min-width="150" />
      <el-table-column prop="courseCount" label="课程数" width="100" />
      <el-table-column prop="totalCredits" label="总学分" width="100" />
      <el-table-column prop="passedCredits" label="已通过学分" width="130" />
      <el-table-column label="平均成绩" width="120">
        <template #default="{ row }">{{ Number(row.averageScore).toFixed(1) }}</template>
      </el-table-column>
    </el-table>

    <el-table v-if="mode === 'plan'" v-loading="loading" :data="rows" empty-text="暂无教学计划">
      <el-table-column prop="term" label="学期" width="130" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程" min-width="160" />
      <el-table-column prop="credit" label="学分" width="90" />
      <el-table-column prop="courseType" label="课程类别" width="130" />
      <el-table-column prop="assessmentType" label="考核方式" width="110" />
    </el-table>

    <el-table v-if="mode === 'weeklySchedule'" v-loading="loading" :data="rows" empty-text="暂无周课表">
      <el-table-column prop="week" label="周次" width="90" />
      <el-table-column prop="courseCode" label="课程号" width="110" />
      <el-table-column prop="courseName" label="课程" min-width="160" />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="scheduleText" label="时间" width="130" />
      <el-table-column prop="classroom" label="教室" width="130" />
    </el-table>
  </section>
</template>
