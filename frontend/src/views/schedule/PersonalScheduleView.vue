<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { personalScheduleApi, type ScheduleEntry } from '@/api/schedule'

const loading = ref(false)
const entries = ref<ScheduleEntry[]>([])
const days = [
  { label: '周一', value: 1 },
  { label: '周二', value: 2 },
  { label: '周三', value: 3 },
  { label: '周四', value: 4 },
  { label: '周五', value: 5 },
]
const slots = [
  { label: '1-2节', value: '1-2' },
  { label: '3-4节', value: '3-4' },
  { label: '5-6节', value: '5-6' },
  { label: '7-8节', value: '7-8' },
  { label: '9-10节', value: '9-10' },
]

const totalCredits = computed(() => entries.value.length)

onMounted(async () => {
  loading.value = true
  try {
    const response = await personalScheduleApi()
    entries.value = response.data
  } finally {
    loading.value = false
  }
})

function cellEntries(dayOfWeek: number, slot: string) {
  return entries.value.filter((entry) => entry.dayOfWeek === dayOfWeek && entry.slot === slot)
}
</script>

<template>
  <PageHeader title="个人课表" description="根据已选课程自动生成本学期周课表。" />

  <section v-loading="loading" class="work-panel">
    <div class="panel-heading">
      <h2>2025-2026-2 学期</h2>
      <span>{{ totalCredits }} 门已选课程</span>
    </div>

    <div class="schedule-grid">
      <div class="schedule-head"></div>
      <div v-for="day in days" :key="day.value" class="schedule-head">{{ day.label }}</div>

      <template v-for="slot in slots" :key="slot.value">
        <div class="schedule-slot">{{ slot.label }}</div>
        <div v-for="day in days" :key="`${slot.value}-${day.value}`" class="schedule-cell">
          <article v-for="entry in cellEntries(day.value, slot.value)" :key="entry.courseCode" class="course-block">
            <strong>{{ entry.courseName }}</strong>
            <span>{{ entry.teacherName }}</span>
            <small>{{ entry.classroom }}</small>
          </article>
        </div>
      </template>
    </div>
  </section>
</template>
