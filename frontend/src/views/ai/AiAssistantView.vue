<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Bot, FileSearch, SendHorizontal } from 'lucide-vue-next'
import PageHeader from '@/components/PageHeader.vue'
import { aiStatusApi, askAiAssistantApi, type AiAssistantResponse, type AiServiceStatusResponse } from '@/api/ai'

const question = ref('我现在如果想申请重修，需要满足什么条件？')
const loading = ref(false)
const result = ref<AiAssistantResponse>()
const status = ref<AiServiceStatusResponse>()
const sourcePage = ref(1)
const sourceSize = ref(10)
const examples = [
  '我现在还差多少学分毕业？',
  '为什么我不能选这门课？',
  '重修申请需要什么条件？',
  '学籍异动审核通过后会发生什么？',
  '学生提交教学评价后老师能看到什么？',
]

const answerParagraphs = computed(() => {
  if (!result.value?.answer) return []
  return result.value.answer.split('\n').filter((line) => line.trim())
})
const sources = computed(() => result.value?.sources ?? [])
const pagedSources = computed(() => sources.value.slice((sourcePage.value - 1) * sourceSize.value, sourcePage.value * sourceSize.value))

onMounted(loadStatus)

async function loadStatus() {
  status.value = (await aiStatusApi()).data
}

async function ask() {
  const text = question.value.trim()
  if (!text) {
    ElMessage.warning('请输入问题')
    return
  }
  loading.value = true
  try {
    result.value = (await askAiAssistantApi(text)).data
    sourcePage.value = 1
  } catch (error) {
    ElMessage.error(resolveError(error, '智能教务助手暂不可用'))
  } finally {
    loading.value = false
  }
}

function handleSourceSizeChange() {
  sourcePage.value = 1
}

function useExample(text: string) {
  question.value = text
  ask()
}

function resolveError(error: unknown, fallback: string) {
  if (
    typeof error === 'object'
    && error !== null
    && 'response' in error
    && typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message === 'string'
  ) {
    return (error as { response: { data: { message: string } } }).response.data.message
  }
  return fallback
}
</script>

<template>
  <PageHeader
    title="智能教务助手"
    description="基于公告、教务规则、教学计划和个人学业数据检索回答，并展示引用来源。"
  />

  <section class="ai-assistant-page">
    <article class="ai-question-panel">
      <div class="panel-title">
        <Bot :size="20" />
        <h2>教务问答</h2>
      </div>

      <el-input
        v-model="question"
        type="textarea"
        :rows="5"
        maxlength="500"
        show-word-limit
        placeholder="输入教务问题，例如：为什么我不能选这门课？"
      />

      <div class="question-actions">
        <el-button
          type="primary"
          :icon="SendHorizontal"
          :loading="loading"
          @click="ask"
        >
          提问
        </el-button>
      </div>

      <div class="example-list">
        <button
          v-for="item in examples"
          :key="item"
          type="button"
          @click="useExample(item)"
        >
          {{ item }}
        </button>
      </div>
    </article>

    <article class="ai-answer-panel" v-loading="loading">
      <div class="panel-title">
        <FileSearch :size="20" />
        <h2>回答</h2>
        <el-tag v-if="result" size="small" type="info">{{ result.serviceMode }}</el-tag>
        <el-tag v-if="result?.answerType === 'REFUSAL' || result?.answerType === 'NO_ANSWER'" size="small" type="warning">
          {{ result.refusalReason || '无法回答' }}
        </el-tag>
      </div>

      <el-alert
        v-if="status"
        class="status-alert"
        :type="status.aiServiceOnline ? 'success' : 'warning'"
        :closable="false"
        :title="`ai-service ${status.aiServiceOnline ? '在线' : '离线'} / ${status.currentMode} / 耗时 ${status.lastLatencyMs}ms`"
        :description="status.aiServiceOnline ? `Ollama：${status.ollamaReachable ? '可用' : '不可用'}，模型：${status.chatModel || '-'} / ${status.sqlModel || '-'}` : status.lastError"
      />

      <div v-if="result" class="answer-box">
        <p v-for="line in answerParagraphs" :key="line">{{ line }}</p>
      </div>
      <el-empty v-else description="输入问题后生成回答" :image-size="90" />

      <div v-if="sources.length" class="source-list">
        <h3>参考依据 / 命中来源</h3>
        <el-table :data="pagedSources" empty-text="暂无来源">
          <el-table-column label="来源类型" width="120">
            <template #default="{ row }">
              <el-tag size="small">{{ row.type }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
          <el-table-column prop="content" label="命中内容" min-width="360" show-overflow-tooltip />
          <el-table-column label="相关度" width="90">
            <template #default="{ row }">{{ row.score.toFixed(1) }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="sourcePage"
          v-model:page-size="sourceSize"
          class="table-pagination"
          layout="total, sizes, prev, pager, next"
          :page-sizes="[10, 20, 50, 100]"
          :total="sources.length"
          @size-change="handleSourceSizeChange"
        />
      </div>
    </article>
  </section>
</template>

<style scoped>
.ai-assistant-page {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 18px;
}

.ai-question-panel,
.ai-answer-panel {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
}

.panel-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 14px;
}

.panel-title h2 {
  margin: 0;
  font-size: 18px;
}

.question-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.example-list {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.example-list button {
  text-align: left;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
  border-radius: 6px;
  padding: 10px 12px;
  color: #1f2937;
  cursor: pointer;
}

.example-list button:hover {
  border-color: #2563eb;
  color: #1d4ed8;
}

.answer-box {
  background: #f8fafc;
  border-radius: 8px;
  padding: 14px 16px;
  line-height: 1.75;
  color: #1f2937;
}

.answer-box p {
  margin: 0 0 8px;
  white-space: pre-wrap;
}

.source-list {
  margin-top: 18px;
  display: grid;
  gap: 10px;
}

.status-alert {
  margin-bottom: 14px;
}

.source-list h3 {
  margin: 0;
  font-size: 16px;
}

.source-list article {
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 12px;
}

.source-list header {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.source-list p {
  margin: 8px 0;
  color: #4b5563;
  line-height: 1.6;
}

.source-list small {
  color: #6b7280;
}

@media (max-width: 900px) {
  .ai-assistant-page {
    grid-template-columns: 1fr;
  }
}
</style>
