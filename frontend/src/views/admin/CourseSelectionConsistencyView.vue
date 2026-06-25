<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { NexusMetricCard, NexusPageHeader, NexusStatusCard } from '@/components/nexus'
import {
  checkCourseSelectionConsistencyApi,
  courseSelectionConsistencyApi,
  repairCourseSelectionConsistencyApi,
  type CourseSelectionConsistencyReport,
} from '@/api/systemMonitor'

const loading = ref(false)
const repairing = ref(false)
const report = ref<CourseSelectionConsistencyReport | null>(null)

const consistentCount = computed(() => report.value?.rows.filter((row) => row.consistent).length ?? 0)
const riskCount = computed(() => report.value?.rows.filter((row) => row.oversold || !row.consistent).length ?? 0)

onMounted(loadReport)

async function loadReport() {
  loading.value = true
  try {
    report.value = (await courseSelectionConsistencyApi({ limit: 100 })).data
  } finally {
    loading.value = false
  }
}

async function checkReport() {
  loading.value = true
  try {
    report.value = (await checkCourseSelectionConsistencyApi(100)).data
    ElMessage.success('一致性校验完成')
  } finally {
    loading.value = false
  }
}

async function repairReport() {
  await ElMessageBox.confirm('将按数据库容量和已选人数重写 Redis 库存，是否继续？', '修复 Redis 库存', {
    type: 'warning',
    confirmButtonText: '确认修复',
    cancelButtonText: '取消',
  })
  repairing.value = true
  try {
    report.value = (await repairCourseSelectionConsistencyApi(100)).data
    ElMessage.success('Redis 库存已按数据库修复')
  } finally {
    repairing.value = false
  }
}
</script>

<template>
  <NexusPageHeader
    title="选课一致性报告"
    eyebrow="Redis / Database"
    description="对照 Redis 剩余库存、数据库已选人数和教学班容量，证明抢课没有超卖，并在缓存异常时提供修复入口。"
  >
    <template #actions>
      <el-button :loading="loading" @click="checkReport">手动校验</el-button>
      <el-button type="danger" :loading="repairing" :disabled="!report?.redisReachable" @click="repairReport">修复 Redis 库存</el-button>
    </template>
  </NexusPageHeader>

  <section class="consistency-overview">
    <NexusStatusCard
      title="Redis 状态"
      :status="report?.redisReachable ? '可用' : '降级'"
      :tone="report?.redisReachable ? 'healthy' : 'degraded'"
      :description="report?.redisMessage || '等待校验'"
      :meta="`检查时间：${report?.checkedAt ? new Date(report.checkedAt).toLocaleString() : '-'}`"
    />
    <NexusMetricCard label="一致教学班" :value="consistentCount" suffix="个" tone="success" />
    <NexusMetricCard label="差异/风险" :value="riskCount" suffix="个" tone="warning" />
    <NexusMetricCard label="超卖风险" :value="report?.oversoldRisk ? '有' : '无'" tone="danger" />
  </section>

  <section v-loading="loading" class="work-panel">
    <div class="panel-heading">
      <h2>库存对照明细</h2>
      <span>{{ report?.rows.length ?? 0 }} 个教学班</span>
    </div>
    <el-table :data="report?.rows ?? []" empty-text="暂无一致性数据">
      <el-table-column prop="offeringId" label="教学班 ID" width="105" />
      <el-table-column prop="courseName" label="课程" min-width="150" />
      <el-table-column prop="capacity" label="容量" width="80" />
      <el-table-column prop="selectedCountInDb" label="DB 已选" width="95" />
      <el-table-column label="Redis 库存" width="110">
        <template #default="{ row }">{{ row.redisStock ?? '未预热' }}</template>
      </el-table-column>
      <el-table-column prop="expectedStock" label="理论库存" width="100" />
      <el-table-column label="差值" width="90">
        <template #default="{ row }">
          <el-tag :type="row.diff === 0 ? 'success' : 'warning'">{{ row.diff }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="结论" width="120">
        <template #default="{ row }">
          <el-tag :type="row.consistent ? 'success' : row.oversold ? 'danger' : 'warning'">
            {{ row.oversold ? '超卖风险' : row.consistent ? '一致' : '需检查' }}
          </el-tag>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.consistency-overview {
  display: grid;
  grid-template-columns: minmax(260px, 1.2fr) repeat(3, minmax(140px, 0.7fr));
  gap: 14px;
  margin-bottom: 18px;
}

@media (max-width: 1024px) {
  .consistency-overview {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .consistency-overview {
    grid-template-columns: 1fr;
  }
}
</style>
