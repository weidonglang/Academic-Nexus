<script setup lang="ts">
// 管理端通知公告发布页面。
// 管理员填写公告标题、分类、内容和目标角色后提交，后端会保存公告并生成对应用户通知。
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import { adminNoticeStatsApi, homeNoticesApi, publishNoticeApi, type Notice, type NoticeStat } from '@/api/notice'

const loading = ref(false)
const saving = ref(false)
const rows = ref<Notice[]>([])
const stats = ref<NoticeStat[]>([])
const form = reactive({
  title: '',
  content: '',
  category: 'GENERAL',
  pinned: false,
  roleCode: 'ALL',
})

const totalTargets = computed(() => stats.value.reduce((sum, item) => sum + Number(item.targetTotal || 0), 0))
const totalRead = computed(() => stats.value.reduce((sum, item) => sum + Number(item.readCount || 0), 0))
const totalUnread = computed(() => stats.value.reduce((sum, item) => sum + Number(item.unreadCount || 0), 0))

onMounted(loadData)

// 功能：加载公告统计数据。
// 说明：管理端公告页展示已发布公告、目标人数、已读数和未读数，用于说明公告发布闭环。
async function loadData() {
  loading.value = true
  try {
    const noticeResponse = await homeNoticesApi({ page: 1, size: 20 })
    rows.value = noticeResponse.data.records
    try {
      const statResponse = await adminNoticeStatsApi()
      stats.value = statResponse.data
    } catch {
      stats.value = []
      console.warn('通知已读未读统计接口暂不可用，请重启后端后再刷新统计。')
    }
  } finally {
    loading.value = false
  }
}

// 功能：发布公告。
// 说明：管理员填写标题、内容、类别和接收角色后调用后端接口，
// 后端保存公告并为目标用户生成通知记录。
async function publish() {
  saving.value = true
  try {
    const roleCode = form.roleCode === 'ALL' ? undefined : form.roleCode
    await publishNoticeApi({ ...form, roleCode })
    ElMessage.success('通知已发布')
    Object.assign(form, { title: '', content: '', category: 'GENERAL', pinned: false, roleCode: 'ALL' })
    await loadData()
  } catch (error) {
    ElMessage.error(resolveErrorMessage(error, '发布失败'))
  } finally {
    saving.value = false
  }
}

function readRate(row: NoticeStat) {
  const total = Number(row.targetTotal || 0)
  if (total === 0) return '0%'
  return `${Math.round((Number(row.readCount || 0) / total) * 100)}%`
}

function formatDateTime(value: string) {
  return new Date(value).toLocaleString('zh-CN')
}

function resolveErrorMessage(error: unknown, fallback: string) {
  if (typeof error === 'object' && error !== null && 'response' in error && typeof (error as { response?: { data?: { message?: string } } }).response?.data?.message === 'string') {
    return (error as { response: { data: { message: string } } }).response.data.message
  }
  return fallback
}
</script>

<template>
  <PageHeader title="通知公告" description="发布首页公告、选课通知、考试通知和审核通知，并查看站内信已读未读统计。" />

  <section class="admin-toolbar">
    <div class="admin-summary">
      <article><span>通知条数</span><strong>{{ stats.length }}</strong></article>
      <article><span>接收人次</span><strong>{{ totalTargets }}</strong></article>
      <article><span>已读 / 未读</span><strong>{{ totalRead }} / {{ totalUnread }}</strong></article>
    </div>
    <div class="admin-actions">
      <el-button @click="loadData">刷新统计</el-button>
    </div>
  </section>

  <section class="profile-grid">
    <article class="work-panel">
      <h2>发布通知</h2>
      <el-form label-width="86px" :model="form">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="类别">
          <el-select v-model="form.category" class="full-field">
            <el-option label="首页公告" value="GENERAL" />
            <el-option label="选课通知" value="COURSE" />
            <el-option label="考试通知" value="EXAM" />
            <el-option label="审核通知" value="STATUS" />
          </el-select>
        </el-form-item>
        <el-form-item label="接收角色">
          <el-select v-model="form.roleCode" class="full-field">
            <el-option label="全部用户" value="ALL" />
            <el-option label="学生 STUDENT" value="STUDENT" />
            <el-option label="教师 TEACHER" value="TEACHER" />
            <el-option label="管理员 ADMIN" value="ADMIN" />
          </el-select>
        </el-form-item>
        <el-form-item label="置顶"><el-switch v-model="form.pinned" /></el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="8" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item><el-button type="primary" :loading="saving" @click="publish">发布</el-button></el-form-item>
      </el-form>
    </article>

    <article v-loading="loading" class="work-panel">
      <h2>公告列表</h2>
      <el-table :data="rows" empty-text="暂无公告">
        <el-table-column prop="title" label="标题" min-width="160" />
        <el-table-column prop="category" label="类别" width="110" />
        <el-table-column label="置顶" width="80"><template #default="{ row }">{{ row.pinned ? '是' : '否' }}</template></el-table-column>
        <el-table-column prop="publisher" label="发布人" width="110" />
      </el-table>
    </article>
  </section>

  <section v-loading="loading" class="work-panel">
    <div class="section-heading">
      <h2>已读 / 未读统计</h2>
    </div>
    <el-table :data="stats" empty-text="暂无通知统计">
      <el-table-column prop="title" label="标题" min-width="180" />
      <el-table-column prop="category" label="类别" width="110" />
      <el-table-column prop="publisher" label="发布人" width="110" />
      <el-table-column label="发布时间" width="180">
        <template #default="{ row }">{{ formatDateTime(row.publishedAt) }}</template>
      </el-table-column>
      <el-table-column prop="targetTotal" label="接收人次" width="100" />
      <el-table-column prop="readCount" label="已读" width="90" />
      <el-table-column prop="unreadCount" label="未读" width="90" />
      <el-table-column label="已读率" width="90">
        <template #default="{ row }">{{ readRate(row) }}</template>
      </el-table-column>
    </el-table>
  </section>
</template>
