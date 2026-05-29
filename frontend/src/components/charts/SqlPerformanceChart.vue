<script setup lang="ts">
import { computed } from 'vue'
import type { EChartsOption } from 'echarts'
import ChartPanel from './ChartPanel.vue'

const props = defineProps<{
  trend: Array<{ name: string; value: number }>
  ranking: Array<{ name: string; value: number }>
  loading?: boolean
}>()

const option = computed<EChartsOption>(() => ({
  tooltip: { trigger: 'axis' },
  legend: { top: 0 },
  grid: [
    { left: 48, right: '55%', top: 48, bottom: 40 },
    { left: '55%', right: 28, top: 48, bottom: 40 },
  ],
  xAxis: [
    { type: 'category', gridIndex: 0, data: props.trend.map((item) => item.name) },
    { type: 'category', gridIndex: 1, data: props.ranking.map((item) => item.name), axisLabel: { rotate: 35 } },
  ],
  yAxis: [
    { type: 'value', gridIndex: 0 },
    { type: 'value', gridIndex: 1 },
  ],
  series: [
    { name: '最近操作趋势', type: 'line', smooth: true, xAxisIndex: 0, yAxisIndex: 0, data: props.trend.map((item) => item.value), areaStyle: {} },
    { name: '高频操作', type: 'bar', xAxisIndex: 1, yAxisIndex: 1, data: props.ranking.map((item) => item.value), itemStyle: { color: '#0f766e' } },
  ],
}))
</script>

<template>
  <ChartPanel title="SQL/数据库操作性能可视化" :option="option" :loading="loading" :empty="!trend.length && !ranking.length" :height="360" />
</template>
