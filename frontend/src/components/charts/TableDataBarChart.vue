<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import ChartPanel from './ChartPanel.vue'

const props = defineProps<{
  title?: string
  rows: Array<{ name: string; value: number }>
  loading?: boolean
}>()

const sortedRows = computed(() => [...props.rows].sort((a, b) => Number(b.value) - Number(a.value)).slice(0, 20))
const option = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  grid: { left: 56, right: 20, top: 30, bottom: 76 },
  xAxis: { type: 'category', data: sortedRows.value.map((item) => item.name), axisLabel: { rotate: 42 } },
  yAxis: { type: 'value', name: '行数' },
  series: [{
    type: 'bar',
    data: sortedRows.value.map((item) => item.value),
    itemStyle: { color: '#2563eb' },
  }],
}))
</script>

<template>
  <ChartPanel :title="title || '表数据量统计'" :option="option" :loading="loading" :empty="!rows.length" />
</template>
