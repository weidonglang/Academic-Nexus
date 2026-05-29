<script setup lang="ts">
// 压测历史报告页面。
// 读取 reports 目录下自动生成的 JSON/HTML 报告，用表格汇总请求数、成功数、
// FULL 数量、吞吐量和延迟指标，方便答辩时回看不同压测结果。
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  loadTestReportHtmlApi,
  loadTestReportsApi,
  type LoadTestReportRow,
} from '@/api/systemMonitor'

const loading = ref(false)
const rows = ref<LoadTestReportRow[]>([])
const apiError = ref('')

const latest = computed(() => rows.value[0])
const totals = computed(() => {
  return rows.value.reduce(
    (acc, row) => {
      acc.requests += row.requestCount
      acc.success += row.successCount
      acc.full += row.fullCount
      return acc
    },
    { requests: 0, success: 0, full: 0 },
  )
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  apiError.value = ''
  try {
    rows.value = (await loadTestReportsApi()).data
  } catch {
    rows.value = []
    apiError.value = '后端接口连接失败，请确认 Spring Boot 已在 http://localhost:8080 启动。'
  } finally {
    loading.value = false
  }
}

async function openReport(row: LoadTestReportRow) {
  if (!row.htmlName) {
    ElMessage.warning('这条记录没有对应的 HTML 报告')
    return
  }
  const html = await loadTestReportHtmlApi(row.htmlName)
  const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  window.open(url, '_blank', 'noopener,noreferrer')
  window.setTimeout(() => URL.revokeObjectURL(url), 60000)
}

function percent(row: LoadTestReportRow) {
  if (!row.requestCount) return '0.00%'
  return `${((row.successCount / row.requestCount) * 100).toFixed(2)}%`
}
</script>

<template>
  <PageHeader title="压测历史报告" description="查看 reports 目录中自动生成的万人抢课压测 JSON 和 HTML 报告。" />

  <section class="admin-toolbar">
    <div class="admin-summary">
      <article>
        <span>报告数量</span>
        <strong>{{ rows.length }}</strong>
      </article>
      <article>
        <span>累计请求</span>
        <strong>{{ totals.requests }}</strong>
      </article>
      <article>
        <span>累计成功</span>
        <strong>{{ totals.success }}</strong>
      </article>
    </div>
    <div class="admin-actions">
      <el-button type="primary" @click="loadData">刷新列表</el-button>
    </div>
  </section>

  <section v-if="apiError" class="report-warning">
    {{ apiError }}
  </section>

  <section v-if="latest" class="report-highlight">
    <div>
      <span>最新报告</span>
      <strong>{{ latest.modifiedAt }}</strong>
      <small>{{ latest.smartMode }} / {{ latest.concurrency }} 并发 / Redis {{ latest.redisReachable ? '正常' : '未连接' }}</small>
    </div>
    <div class="report-bars">
      <div>
        <span>成功 {{ latest.successCount }}</span>
        <i :style="{ width: percent(latest) }" />
      </div>
      <div>
        <span>满员 {{ latest.fullCount }}</span>
        <i class="full" :style="{ width: latest.requestCount ? `${(latest.fullCount / latest.requestCount) * 100}%` : '0%' }" />
      </div>
    </div>
  </section>

  <section v-loading="loading" class="work-panel">
    <div class="panel-heading">
      <h2>历史报告</h2>
      <span>{{ rows.length }} 条</span>
    </div>
    <el-table :data="rows" empty-text="reports 目录暂无压测报告">
      <el-table-column prop="modifiedAt" label="生成时间" width="170" />
      <el-table-column prop="smartMode" label="模式" width="110" />
      <el-table-column prop="requestCount" label="请求数" width="110" />
      <el-table-column prop="successCount" label="成功" width="100" />
      <el-table-column prop="fullCount" label="满员" width="100" />
      <el-table-column label="成功率" width="110">
        <template #default="{ row }">{{ percent(row) }}</template>
      </el-table-column>
      <el-table-column label="Redis" width="100">
        <template #default="{ row }">
          <el-tag :type="row.redisReachable ? 'success' : 'warning'">
            {{ row.redisReachable ? '正常' : '未连接' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="吞吐量" width="130">
        <template #default="{ row }">{{ row.throughput.toFixed(2) }} /s</template>
      </el-table-column>
      <el-table-column label="平均响应" width="130">
        <template #default="{ row }">{{ row.avgLatency.toFixed(1) }} ms</template>
      </el-table-column>
      <el-table-column label="P95" width="120">
        <template #default="{ row }">{{ row.p95.toFixed(1) }} ms</template>
      </el-table-column>
      <el-table-column prop="jsonName" label="JSON 文件" min-width="260" show-overflow-tooltip />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button type="primary" link :disabled="!row.htmlName" @click="openReport(row)">预览</el-button>
        </template>
      </el-table-column>
    </el-table>
  </section>
</template>

<style scoped>
.report-highlight {
  display: grid;
  grid-template-columns: minmax(320px, 0.55fr) minmax(0, 1fr);
  gap: 18px;
  margin-bottom: 18px;
  padding: 18px;
  background: #ffffff;
  border: 1px solid var(--line);
  border-radius: 8px;
  box-shadow: 0 10px 26px rgba(28, 45, 65, 0.05);
}

.report-warning {
  margin-bottom: 18px;
  padding: 12px 14px;
  color: #9a3412;
  background: #fff7ed;
  border: 1px solid #fed7aa;
  border-radius: 8px;
}

.report-highlight span,
.report-highlight strong,
.report-highlight small {
  display: block;
}

.report-highlight span,
.report-highlight small {
  color: var(--muted);
}

.report-highlight strong {
  margin-top: 8px;
  font-size: 24px;
}

.report-highlight small {
  margin-top: 8px;
}

.report-bars {
  display: grid;
  gap: 12px;
  align-content: center;
}

.report-bars div {
  display: grid;
  gap: 7px;
}

.report-bars div::after {
  content: "";
  height: 10px;
  background: #e4edf0;
  border-radius: 999px;
  grid-row: 2;
}

.report-bars i {
  height: 10px;
  background: var(--success);
  border-radius: 999px;
  grid-row: 2;
  z-index: 1;
}

.report-bars i.full {
  background: var(--accent);
}

@media (max-width: 1000px) {
  .report-highlight {
    grid-template-columns: 1fr;
  }
}
</style>
