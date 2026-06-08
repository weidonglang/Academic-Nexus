<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import PageHeader from '@/components/PageHeader.vue'
import {
  submitTeachingFeedbackApi,
  teachingFeedbackApi,
  type TeachingFeedback,
} from '@/api/information'

const loading = ref(false)
const submitting = ref(false)
const records = ref<TeachingFeedback[]>([])
const page = ref(1)
const size = ref(10)
const pagedRecords = computed(() => records.value.slice((page.value - 1) * size.value, page.value * size.value))
const form = reactive({
  category: '教学运行',
  title: '',
  content: '',
})

onMounted(loadRecords)

async function loadRecords() {
  loading.value = true
  try {
    records.value = (await teachingFeedbackApi()).data
  } finally {
    loading.value = false
  }
}

function handleSizeChange() {
  page.value = 1
}

async function submit() {
  submitting.value = true
  try {
    await submitTeachingFeedbackApi(form)
    form.title = ''
    form.content = ''
    ElMessage.success('反馈已提交')
    await loadRecords()
  } finally {
    submitting.value = false
  }
}

function formatDateTime(value?: string) {
  if (!value) return '-'
  return new Date(value).toLocaleString('zh-CN')
}
</script>

<template>
  <PageHeader title="教学信息反馈" description="提交课程、教师、教室、教学运行等反馈，并查看处理状态。" />

  <section class="profile-grid">
    <article class="work-panel">
      <h2>提交反馈</h2>
      <el-form label-width="86px">
        <el-form-item label="反馈类别">
          <el-select v-model="form.category" class="full-field">
            <el-option label="教学运行" value="教学运行" />
            <el-option label="课程安排" value="课程安排" />
            <el-option label="教室设备" value="教室设备" />
            <el-option label="教师授课" value="教师授课" />
          </el-select>
        </el-form-item>
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="120" show-word-limit />
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="7" maxlength="1000" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" :disabled="!form.title.trim() || !form.content.trim()" @click="submit">
            提交反馈
          </el-button>
        </el-form-item>
      </el-form>
    </article>

    <article class="work-panel">
      <h2>反馈记录</h2>
      <el-table v-loading="loading" :data="pagedRecords" empty-text="暂无反馈记录">
        <el-table-column prop="category" label="类别" width="110" />
        <el-table-column prop="title" label="标题" min-width="150" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="100" />
        <el-table-column label="提交时间" width="170">
          <template #default="{ row }">{{ formatDateTime(row.submittedAt) }}</template>
        </el-table-column>
        <el-table-column prop="reply" label="回复" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.reply || '-' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        class="table-pagination"
        layout="total, sizes, prev, pager, next"
        :page-sizes="[10, 20, 50, 100]"
        :total="records.length"
        @size-change="handleSizeChange"
      />
    </article>
  </section>
</template>
