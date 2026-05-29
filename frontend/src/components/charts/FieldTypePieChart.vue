<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import ChartPanel from './ChartPanel.vue'

const props = defineProps<{
  rows: Array<{ name: string; value: number }>
  loading?: boolean
}>()

const option = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'item' },
  legend: { type: 'scroll', bottom: 0 },
  series: [{
    name: '字段类型',
    type: 'pie',
    radius: ['42%', '70%'],
    center: ['50%', '45%'],
    data: props.rows,
    label: { formatter: '{b}: {d}%' },
  }],
}))
</script>

<template>
  <ChartPanel title="字段类型分布" :option="option" :loading="loading" :empty="!rows.length" />
</template>
