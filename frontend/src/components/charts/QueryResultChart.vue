<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import ChartPanel from './ChartPanel.vue'

type Row = Record<string, unknown>
type ChartType = 'bar' | 'line' | 'pie' | 'scatter'

const props = defineProps<{
  rows: Row[]
}>()

const chartType = ref<ChartType>('bar')
const xField = ref('')
const yField = ref('')

const fields = computed(() => Object.keys(props.rows[0] ?? {}))
const numericFields = computed(() => fields.value.filter((field) => props.rows.some((row) => isNumberLike(row[field]))))
const textFields = computed(() => fields.value.filter((field) => !numericFields.value.includes(field)))
const canChart = computed(() => props.rows.length > 0 && numericFields.value.length > 0 && fields.value.length > 1)

watch(() => props.rows, selectDefaultFields, { immediate: true })

const option = computed<EChartsOption>(() => {
  if (!canChart.value || !xField.value || !yField.value) return {}
  const labels = props.rows.map((row) => String(row[xField.value] ?? ''))
  const values = props.rows.map((row) => Number(row[yField.value] ?? 0))
  if (chartType.value === 'pie') {
    return {
      tooltip: { trigger: 'item' },
      series: [{ type: 'pie', radius: ['38%', '70%'], data: labels.map((name, index) => ({ name, value: values[index] })) }],
    }
  }
  if (chartType.value === 'scatter') {
    return {
      tooltip: { trigger: 'item' },
      xAxis: { type: 'category', data: labels },
      yAxis: { type: 'value' },
      series: [{ type: 'scatter', data: values }],
    }
  }
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: 52, right: 20, top: 28, bottom: 70 },
    xAxis: { type: 'category', data: labels, axisLabel: { rotate: labels.length > 8 ? 35 : 0 } },
    yAxis: { type: 'value' },
    series: [{ type: chartType.value, data: values, smooth: chartType.value === 'line', areaStyle: chartType.value === 'line' ? {} : undefined }],
  }
})

function selectDefaultFields() {
  xField.value = textFields.value[0] || fields.value.find((field) => field !== numericFields.value[0]) || fields.value[0] || ''
  yField.value = numericFields.value[0] || ''
}

function isNumberLike(value: unknown) {
  return value !== null && value !== '' && value !== undefined && Number.isFinite(Number(value))
}
</script>

<template>
  <section class="query-chart">
    <div class="query-chart__tools">
      <el-select v-model="chartType" class="small-select" placeholder="图表类型">
        <el-option label="柱状图" value="bar" />
        <el-option label="折线/面积图" value="line" />
        <el-option label="饼图" value="pie" />
        <el-option label="散点图" value="scatter" />
      </el-select>
      <el-select v-model="xField" class="small-select" placeholder="X轴字段">
        <el-option v-for="field in fields" :key="field" :label="field" :value="field" />
      </el-select>
      <el-select v-model="yField" class="small-select" placeholder="Y轴字段">
        <el-option v-for="field in numericFields" :key="field" :label="field" :value="field" />
      </el-select>
    </div>
    <el-alert
      v-if="rows.length && !canChart"
      type="warning"
      :closable="false"
      title="当前查询结果缺少数值字段，无法生成图表"
    />
    <ChartPanel title="查询结果图表" :option="option" :empty="!canChart" :height="300" />
  </section>
</template>
