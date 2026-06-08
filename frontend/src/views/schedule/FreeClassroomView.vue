<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import PageHeader from '@/components/PageHeader.vue'
import { fetchFreeClassroomsApi, type FreeClassroom } from '@/api/academic'

const loading = ref(false)
const rooms = ref<FreeClassroom[]>([])
const page = ref(1)
const size = ref(10)
const pagedRooms = computed(() => rooms.value.slice((page.value - 1) * size.value, page.value * size.value))
const query = reactive({
  campus: '',
  building: '',
  slot: '',
})

async function loadRooms() {
  loading.value = true
  try {
    rooms.value = await fetchFreeClassroomsApi(query)
  } finally {
    loading.value = false
  }
}

function search() {
  page.value = 1
  void loadRooms()
}

function handleSizeChange() {
  page.value = 1
}

onMounted(loadRooms)
</script>

<template>
  <PageHeader title="空闲教室" description="按校区、教学楼和节次查询可用教室。" />
  <section class="work-panel">
    <el-form class="filter-form" inline @submit.prevent>
      <el-form-item label="校区">
        <el-select v-model="query.campus" clearable placeholder="全部校区" style="width: 140px">
          <el-option label="主校区" value="主校区" />
          <el-option label="东区" value="东区" />
        </el-select>
      </el-form-item>
      <el-form-item label="教学楼">
        <el-select v-model="query.building" clearable placeholder="全部教学楼" style="width: 150px">
          <el-option label="A教学楼" value="A教学楼" />
          <el-option label="B教学楼" value="B教学楼" />
          <el-option label="C教学楼" value="C教学楼" />
        </el-select>
      </el-form-item>
      <el-form-item label="节次">
        <el-select v-model="query.slot" clearable placeholder="全部节次" style="width: 140px">
          <el-option label="1-2节" value="1-2节" />
          <el-option label="3-4节" value="3-4节" />
          <el-option label="5-6节" value="5-6节" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
    </el-form>

    <el-table v-loading="loading" :data="pagedRooms">
      <el-table-column prop="campus" label="校区" width="110" />
      <el-table-column prop="building" label="教学楼" width="120" />
      <el-table-column prop="room" label="教室" width="110" />
      <el-table-column prop="capacity" label="容量" width="90" />
      <el-table-column prop="roomType" label="类型" width="120" />
      <el-table-column prop="availableSlot" label="空闲节次" />
    </el-table>
    <el-pagination
      v-model:current-page="page"
      v-model:page-size="size"
      class="table-pagination"
      layout="total, sizes, prev, pager, next"
      :page-sizes="[10, 20, 50, 100]"
      :total="rooms.length"
      @size-change="handleSizeChange"
    />
  </section>
</template>
