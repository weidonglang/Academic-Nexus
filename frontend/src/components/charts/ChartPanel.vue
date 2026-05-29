<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import type { ECharts, EChartsOption } from 'echarts'

const props = defineProps<{
  title: string
  option?: EChartsOption
  loading?: boolean
  error?: string
  empty?: boolean
  height?: number
}>()

const chartEl = ref<HTMLDivElement>()
let chart: ECharts | undefined
let resizeObserver: ResizeObserver | undefined
let renderTimer: number | undefined

const chartHeight = computed(() => `${props.height ?? 320}px`)

onMounted(async () => {
  await nextTick()

  if (!chartEl.value) return

  chart = echarts.init(chartEl.value)
  renderChart()

  window.addEventListener('resize', resizeChart)

  resizeObserver = new ResizeObserver(() => {
    resizeChart()
  })

  resizeObserver.observe(chartEl.value)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)

  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = undefined
  }

  if (renderTimer) {
    window.clearTimeout(renderTimer)
    renderTimer = undefined
  }

  chart?.dispose()
  chart = undefined
})

/**
 * 关键修复：
 * 不要 deep watch option。
 * deep watch 会干扰 ECharts graph 的拖拽、缩放和 force 布局状态。
 */
watch(
    () => props.option,
    () => {
      scheduleRender()
    }
)

watch(
    () => [props.loading, props.empty, props.error, props.height],
    () => {
      scheduleRender()
    }
)

function scheduleRender() {
  if (renderTimer) {
    window.clearTimeout(renderTimer)
  }

  renderTimer = window.setTimeout(() => {
    renderTimer = undefined
    renderChart()
  }, 0)
}

function renderChart() {
  if (!chart) return

  if (props.loading) {
    chart.showLoading('default', { text: '加载中' })
    return
  }

  chart.hideLoading()

  if (props.error || props.empty || !props.option) {
    chart.clear()
    return
  }

  /**
   * 关键修复：
   * 不要使用 chart.setOption(option, true)
   * true 会强制重建图表，导致缩放、拖拽、视图位置被重置。
   */
  chart.setOption(props.option, {
    notMerge: false,
    lazyUpdate: true,
    silent: false,
  })

  resizeChart()
}

function resizeChart() {
  if (!chart) return

  requestAnimationFrame(() => {
    chart?.resize()
  })
}

function exportPng() {
  if (!chart || props.empty || props.error) return

  const url = chart.getDataURL({
    type: 'png',
    pixelRatio: 2,
    backgroundColor: '#ffffff',
  })

  const link = document.createElement('a')
  link.href = url
  link.download = `${props.title}.png`
  link.click()
}
</script>

<template>
  <article class="chart-panel">
    <header class="chart-panel__header">
      <h3>{{ title }}</h3>
      <el-button size="small" :disabled="empty || !!error" @click="exportPng">
        导出图片
      </el-button>
    </header>

    <div v-if="error" class="chart-panel__state error">
      {{ error }}
    </div>

    <div v-else-if="empty" class="chart-panel__state">
      暂无可视化数据
    </div>

    <div
        ref="chartEl"
        class="chart-panel__chart"
        :style="{ height: chartHeight }"
    />
  </article>
</template>