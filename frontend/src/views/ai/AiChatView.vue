<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { Bot, SendHorizontal } from 'lucide-vue-next'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { aiChatApi, aiStatusApi, type AiServiceStatusResponse } from '@/api/ai'

interface Message {
  role: 'user' | 'assistant'
  content: string
  mode?: string
  modelName?: string
  searchUsed?: boolean
}

const input = ref('帮我总结一下今天应该如何准备教务系统答辩')
const loading = ref(false)
const status = ref<AiServiceStatusResponse>()
const statusError = ref('')
const chatWindow = ref<HTMLElement>()
const messages = ref<Message[]>([
  { role: 'assistant', content: '这里是通用 AI 聊天入口，不作为正式教务依据。教务规则类问题请使用“智能教务助手”。', mode: 'system' },
])

loadStatus()

async function loadStatus() {
  try {
    status.value = (await aiStatusApi()).data
    statusError.value = ''
  } catch (error) {
    status.value = undefined
    statusError.value = resolveErrorMessage(error, 'AI 状态暂不可用，请确认后端服务已经启动。')
  }
}

async function send() {
  if (loading.value) return
  const text = input.value.trim()
  if (!text) return
  messages.value.push({ role: 'user', content: text })
  input.value = ''
  loading.value = true
  await scrollToBottom()
  try {
    const response = (await aiChatApi(text)).data
    messages.value.push({
      role: 'assistant',
      content: response.answer,
      mode: response.serviceMode,
      modelName: response.modelName,
      searchUsed: response.searchUsed,
    })
  } catch (error) {
    const message = resolveErrorMessage(error, 'AI 聊天暂不可用，请确认后端和 ai-service 已启动。')
    messages.value.push({ role: 'assistant', content: `发送失败：${message}`, mode: 'error' })
    ElMessage.error(message)
  } finally {
    loading.value = false
    await loadStatus()
    await scrollToBottom()
  }
}

function handleInputKeydown(event: KeyboardEvent) {
  if (event.key !== 'Enter' || event.shiftKey) return
  event.preventDefault()
  void send()
}

async function scrollToBottom() {
  await nextTick()
  if (chatWindow.value) {
    chatWindow.value.scrollTop = chatWindow.value.scrollHeight
  }
}

function resolveErrorMessage(error: unknown, fallback: string) {
  const maybe = error as {
    response?: { status?: number; data?: { message?: string } }
    message?: string
  }
  if (maybe.response?.status === 401) {
    return '登录状态已失效，请重新登录。'
  }
  if (maybe.response?.status === 403) {
    return '当前账号没有访问 AI 功能的权限。'
  }
  return maybe.response?.data?.message || maybe.message || fallback
}
</script>

<template>
  <PageHeader title="AI 聊天" description="通用聊天入口，用于答辩准备、文本润色和普通问答；正式教务依据请使用 RAG 助手。" />

  <section class="chat-page">
    <el-alert
      v-if="status"
      :type="status.aiServiceOnline ? 'success' : 'warning'"
      :closable="false"
      :title="`ai-service ${status.aiServiceOnline ? '在线' : '离线'} / ${status.currentMode}`"
      :description="`调用：${status.discoveryEnabled ? status.serviceName : status.baseUrl}，默认模型：${status.defaultChatModel || status.chatModel || '-'}，搜索：${status.searchEnabled ? status.searchProvider : '未启用'}，耗时：${status.lastLatencyMs}ms`"
    />
    <el-alert
      v-else-if="statusError"
      type="warning"
      :closable="false"
      title="AI 状态不可用"
      :description="statusError"
    />

    <article ref="chatWindow" class="chat-window">
      <div v-for="(message, index) in messages" :key="index" :class="['chat-message', message.role]">
        <Bot v-if="message.role === 'assistant'" :size="18" />
        <p>{{ message.content }}</p>
        <small v-if="message.mode">
          {{ message.mode }}<template v-if="message.modelName"> / {{ message.modelName }}</template>
          <template v-if="message.searchUsed"> / 已联网搜索</template>
        </small>
      </div>
    </article>

    <div class="chat-input">
      <el-input
        v-model="input"
        type="textarea"
        :rows="3"
        maxlength="1000"
        show-word-limit
        @keydown="handleInputKeydown"
      />
      <el-button type="primary" :icon="SendHorizontal" :loading="loading" @click="send">发送</el-button>
    </div>
  </section>
</template>

<style scoped>
.chat-page {
  display: grid;
  gap: 16px;
}

.chat-window {
  min-height: 420px;
  max-height: 560px;
  overflow-y: auto;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 18px;
  display: grid;
  align-content: start;
  gap: 12px;
}

.chat-message {
  max-width: 78%;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  color: #1f2937;
}

.chat-message.user {
  justify-self: end;
  background: #e0f2fe;
}

.chat-message.assistant {
  justify-self: start;
}

.chat-message p {
  margin: 0;
  white-space: pre-wrap;
  line-height: 1.7;
}

.chat-message small {
  display: block;
  margin-top: 6px;
  color: #6b7280;
}

.chat-input {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 12px;
  align-items: end;
}
</style>
