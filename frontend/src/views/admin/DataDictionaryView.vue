<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { NexusMetricCard, NexusPageHeader, NexusRiskBadge } from '@/components/nexus'
import {
  dataDictionaryFieldsApi,
  dataDictionaryTablesApi,
  type DataDictionaryField,
  type DataDictionaryTable,
} from '@/api/governance'

const loading = ref(false)
const fieldLoading = ref(false)
const moduleFilter = ref('')
const tables = ref<DataDictionaryTable[]>([])
const selectedTable = ref<DataDictionaryTable | null>(null)
const fields = ref<DataDictionaryField[]>([])

const modules = computed(() => [...new Set(tables.value.map((item) => item.module))])
const sensitiveCount = computed(() => fields.value.filter((field) => field.sensitive).length)
const blockedFieldCount = computed(() => fields.value.filter((field) => !field.exportAllowed).length)

onMounted(loadTables)

async function loadTables() {
  loading.value = true
  try {
    tables.value = (await dataDictionaryTablesApi(moduleFilter.value ? { module: moduleFilter.value } : undefined)).data
    selectedTable.value = tables.value[0] ?? null
    if (selectedTable.value) await loadFields(selectedTable.value)
  } finally {
    loading.value = false
  }
}

async function loadFields(table: DataDictionaryTable) {
  selectedTable.value = table
  fieldLoading.value = true
  try {
    fields.value = (await dataDictionaryFieldsApi(table.tableName)).data
  } finally {
    fieldLoading.value = false
  }
}

function risk(level: string) {
  return level.toLowerCase() as 'low' | 'medium' | 'high'
}
</script>

<template>
  <NexusPageHeader
    title="数据字典"
    eyebrow="Data Dictionary"
    description="集中说明表、字段、敏感级别与导出规则，为数据库浏览、CSV 导出和 AI SQL 安全检查提供依据。"
  />

  <section class="dictionary-metrics">
    <NexusMetricCard label="登记表" :value="tables.length" suffix="张" tone="info" />
    <NexusMetricCard label="当前字段" :value="fields.length" suffix="个" tone="success" />
    <NexusMetricCard label="敏感字段" :value="sensitiveCount" suffix="个" tone="warning" />
    <NexusMetricCard label="禁止导出字段" :value="blockedFieldCount" suffix="个" tone="danger" />
  </section>

  <section class="admin-toolbar">
    <div class="admin-actions">
      <el-select v-model="moduleFilter" placeholder="模块筛选" clearable>
        <el-option v-for="module in modules" :key="module" :label="module" :value="module" />
      </el-select>
      <el-button :loading="loading" @click="loadTables">查询</el-button>
    </div>
  </section>

  <section class="dictionary-layout">
    <article v-loading="loading" class="work-panel">
      <div class="panel-heading">
        <h2>数据表</h2>
        <span>{{ tables.length }} 张</span>
      </div>
      <el-table :data="tables" highlight-current-row @row-click="loadFields">
        <el-table-column prop="displayName" label="中文名" min-width="130" />
        <el-table-column prop="tableName" label="表名" min-width="150" show-overflow-tooltip />
        <el-table-column label="敏感级别" width="110">
          <template #default="{ row }"><NexusRiskBadge :level="risk(row.sensitiveLevel)" /></template>
        </el-table-column>
        <el-table-column label="可导出" width="90">
          <template #default="{ row }">
            <el-tag :type="row.exportAllowed ? 'success' : 'danger'">{{ row.exportAllowed ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </article>

    <article v-loading="fieldLoading" class="work-panel">
      <div class="panel-heading">
        <h2>{{ selectedTable?.displayName || '字段规则' }}</h2>
        <span>{{ selectedTable?.tableName || '-' }}</span>
      </div>
      <el-table :data="fields" empty-text="请选择数据表">
        <el-table-column prop="displayName" label="中文名" min-width="130" />
        <el-table-column prop="fieldName" label="字段" min-width="140" />
        <el-table-column label="敏感" width="90">
          <template #default="{ row }">
            <el-tag :type="row.sensitive ? 'warning' : 'info'">{{ row.sensitive ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="maskingRule" label="脱敏规则" width="110" />
        <el-table-column label="可导出" width="90">
          <template #default="{ row }">
            <el-tag :type="row.exportAllowed ? 'success' : 'danger'">{{ row.exportAllowed ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="180" show-overflow-tooltip />
      </el-table>
    </article>
  </section>
</template>

<style scoped>
.dictionary-metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.dictionary-layout {
  display: grid;
  grid-template-columns: minmax(360px, 0.9fr) minmax(0, 1.1fr);
  gap: 18px;
}

@media (max-width: 1100px) {
  .dictionary-metrics,
  .dictionary-layout {
    grid-template-columns: 1fr;
  }
}
</style>
