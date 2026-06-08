<script setup lang="ts">
import { onMounted, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { aiCallLogsApi, type AiCallLogRow } from '@/api/ai'

const loading = ref(false)
const rows = ref<AiCallLogRow[]>([])
const page = ref(1)
const size = ref(20)
const total = ref(0)

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const response = (await aiCallLogsApi({ page: page.value, size: size.value })).data
    rows.value = response.records
    page.value = response.page
    size.value = response.size
    total.value = response.total
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
  void loadData()
}
</script>

<template>
  <PageHeader title="AI 调用日志" description="查看 RAG、SQL、聊天、学业画像和压测解读的调用历史、耗时与错误信息。" />

  <section class="work-panel" v-loading="loading">
    <div class="panel-heading">
      <h2>最近调用</h2>
      <el-button type="primary" @click="loadData">刷新</el-button>
    </div>
    <el-table :data="rows" empty-text="暂无 AI 调用日志">
      <el-table-column prop="createdAt" label="时间" width="180" />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="functionType" label="功能" width="150" />
      <el-table-column prop="modelName" label="模型/模式" width="160" />
      <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="promptSummary" label="输入摘要" min-width="260" show-overflow-tooltip />
      <el-table-column prop="errorMessage" label="错误" min-width="220" show-overflow-tooltip />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="table-pagination"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50, 100]"
      :total="total"
      @current-change="loadData"
      @size-change="handleSizeChange"
    />
  </section>
</template>
