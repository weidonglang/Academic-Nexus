<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { academicProfileApi, type AcademicProfileResponse } from '@/api/ai'

const loading = ref(false)
const profile = ref<AcademicProfileResponse>()
const progressPage = ref(1)
const progressSize = ref(10)
const failedPage = ref(1)
const failedSize = ref(10)
const auditPage = ref(1)
const auditSize = ref(10)

const pagedProgress = computed(() => (profile.value?.progress ?? []).slice((progressPage.value - 1) * progressSize.value, progressPage.value * progressSize.value))
const pagedFailedCourses = computed(() => (profile.value?.failedCourses ?? []).slice((failedPage.value - 1) * failedSize.value, failedPage.value * failedSize.value))
const pagedGraduationAudits = computed(() => (profile.value?.graduationAudits ?? []).slice((auditPage.value - 1) * auditSize.value, auditPage.value * auditSize.value))

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    profile.value = (await academicProfileApi()).data
  } finally {
    loading.value = false
  }
}

function handleProgressSizeChange() {
  progressPage.value = 1
}

function handleFailedSizeChange() {
  failedPage.value = 1
}

function handleAuditSizeChange() {
  auditPage.value = 1
}
</script>

<template>
  <PageHeader title="学业画像" description="整合成绩、学分、毕业审核和学业预警，生成毕业风险解释与建议。" />

  <section v-loading="loading" class="profile-page">
    <section v-if="profile" class="profile-cards">
      <article>
        <span>已修学分</span>
        <strong>{{ profile.earnedCredits }}</strong>
      </article>
      <article>
        <span>计划学分</span>
        <strong>{{ profile.plannedCredits }}</strong>
      </article>
      <article>
        <span>剩余学分</span>
        <strong>{{ profile.remainingCredits }}</strong>
      </article>
      <article>
        <span>毕业风险</span>
        <strong>{{ profile.graduationRiskLevel }}</strong>
      </article>
    </section>

    <article v-if="profile" class="work-panel">
      <h2>{{ profile.studentName }} / {{ profile.studentNo }}</h2>
      <p>{{ profile.college }} · {{ profile.major }} · {{ profile.className }} · {{ profile.status }}</p>
      <el-alert type="info" :closable="false" :title="profile.aiSuggestion" />
    </article>

    <section v-if="profile" class="profile-grid">
      <article class="work-panel">
        <h2>课程完成情况</h2>
        <el-table :data="pagedProgress" empty-text="暂无学业进度">
          <el-table-column prop="courseType" label="课程类型" />
          <el-table-column prop="courseCount" label="课程数" width="100" />
          <el-table-column prop="totalCredits" label="总学分" width="100" />
          <el-table-column prop="passedCredits" label="通过学分" width="110" />
          <el-table-column label="平均分" width="100">
            <template #default="{ row }">{{ row.averageScore.toFixed(1) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="progressPage"
          v-model:page-size="progressSize"
          class="table-pagination"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="profile.progress.length"
          @size-change="handleProgressSizeChange"
        />
      </article>

      <article class="work-panel">
        <h2>挂科 / 重修风险</h2>
        <el-table :data="pagedFailedCourses" empty-text="暂无未通过课程">
          <el-table-column prop="courseCode" label="课程号" width="110" />
          <el-table-column prop="courseName" label="课程" />
          <el-table-column prop="credit" label="学分" width="80" />
          <el-table-column prop="score" label="成绩" width="80" />
          <el-table-column prop="term" label="学期" width="130" />
        </el-table>
        <el-pagination
          v-model:current-page="failedPage"
          v-model:page-size="failedSize"
          class="table-pagination"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="profile.failedCourses.length"
          @size-change="handleFailedSizeChange"
        />
      </article>

      <article class="work-panel wide">
        <h2>毕业审核项</h2>
        <el-table :data="pagedGraduationAudits" empty-text="暂无毕业审核记录">
          <el-table-column prop="auditItem" label="审核项" />
          <el-table-column prop="requiredValue" label="要求" />
          <el-table-column prop="currentValue" label="当前值" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.passed ? 'success' : 'warning'">{{ row.passed ? '通过' : '未完成' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remark" label="说明" />
        </el-table>
        <el-pagination
          v-model:current-page="auditPage"
          v-model:page-size="auditSize"
          class="table-pagination"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="profile.graduationAudits.length"
          @size-change="handleAuditSizeChange"
        />
      </article>
    </section>
  </section>
</template>

<style scoped>
.profile-page {
  display: grid;
  gap: 18px;
}

.profile-cards {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.profile-cards article,
.work-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 16px;
}

.profile-cards span {
  color: #6b7280;
}

.profile-cards strong {
  display: block;
  margin-top: 8px;
  font-size: 28px;
}

.profile-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.wide {
  grid-column: 1 / -1;
}
</style>
