<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { NexusPageHeader, NexusRiskBadge } from '@/components/nexus'
import {
  createSensitiveWordApi,
  moderationLogsApi,
  sensitiveWordsApi,
  type ModerationLogRow,
  type SensitiveWordRow,
} from '@/api/governance'

const loading = ref(false)
const saving = ref(false)
const words = ref<SensitiveWordRow[]>([])
const logs = ref<ModerationLogRow[]>([])
const form = reactive({
  word: '',
  category: 'DEMO',
  riskLevel: 'HIGH',
  enabled: true,
})

onMounted(loadData)

async function loadData() {
  loading.value = true
  try {
    const [wordResponse, logResponse] = await Promise.all([
      sensitiveWordsApi({ page: 1, size: 50 }),
      moderationLogsApi({ page: 1, size: 20 }),
    ])
    words.value = wordResponse.data.records
    logs.value = logResponse.data.records
  } finally {
    loading.value = false
  }
}

async function createWord() {
  if (!form.word.trim()) return
  saving.value = true
  try {
    await createSensitiveWordApi({ ...form, word: form.word.trim() })
    form.word = ''
    ElMessage.success('敏感词已新增')
    await loadData()
  } catch (error) {
    ElMessage.error(resolveError(error))
  } finally {
    saving.value = false
  }
}

function risk(level: string) {
  return level.toLowerCase() as 'low' | 'medium' | 'high'
}

function resolveError(error: unknown) {
  const message = (error as { response?: { data?: { message?: string } } })?.response?.data?.message
  return message || '操作失败'
}
</script>

<template>
  <NexusPageHeader
    title="敏感词与内容安全"
    eyebrow="Content Moderation"
    description="维护敏感词词库，检测通知、反馈、评价和 AI 输入输出等文本，高风险内容会被拦截并写入检测日志。"
  />

  <section class="moderation-layout">
    <article class="work-panel">
      <div class="panel-heading">
        <h2>新增敏感词</h2>
        <span>HIGH 会阻断发布</span>
      </div>
      <el-form label-position="top">
        <el-form-item label="敏感词">
          <el-input v-model="form.word" placeholder="例如：示例敏感词A" />
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="风险等级">
          <el-select v-model="form.riskLevel">
            <el-option label="HIGH" value="HIGH" />
            <el-option label="MEDIUM" value="MEDIUM" />
            <el-option label="LOW" value="LOW" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-checkbox v-model="form.enabled">启用</el-checkbox>
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="createWord">保存</el-button>
      </el-form>
    </article>

    <article v-loading="loading" class="work-panel">
      <div class="panel-heading">
        <h2>词库</h2>
        <span>{{ words.length }} 条</span>
      </div>
      <el-table :data="words">
        <el-table-column prop="word" label="词条" min-width="150" />
        <el-table-column prop="category" label="分类" width="110" />
        <el-table-column label="风险" width="110">
          <template #default="{ row }"><NexusRiskBadge :level="risk(row.riskLevel)" /></template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'">{{ row.enabled ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </article>
  </section>

  <section v-loading="loading" class="work-panel">
    <div class="panel-heading">
      <h2>检测日志</h2>
      <span>{{ logs.length }} 条</span>
    </div>
    <el-table :data="logs" empty-text="暂无检测日志">
      <el-table-column prop="scene" label="场景" width="130" />
      <el-table-column prop="matchedWords" label="命中词" min-width="160" show-overflow-tooltip />
      <el-table-column label="风险" width="100">
        <template #default="{ row }"><NexusRiskBadge :level="risk(row.riskLevel)" /></template>
      </el-table-column>
      <el-table-column prop="action" label="动作" width="100" />
      <el-table-column prop="operator" label="操作人" width="120" />
      <el-table-column prop="traceId" label="TraceId" min-width="180" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="时间" width="190" />
    </el-table>
  </section>
</template>

<style scoped>
.moderation-layout {
  display: grid;
  grid-template-columns: minmax(280px, 0.45fr) minmax(0, 1fr);
  gap: 18px;
  margin-bottom: 18px;
}

@media (max-width: 980px) {
  .moderation-layout {
    grid-template-columns: 1fr;
  }
}
</style>
