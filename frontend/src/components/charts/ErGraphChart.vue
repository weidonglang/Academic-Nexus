<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import type { EChartsOption } from 'echarts'
import ChartPanel from './ChartPanel.vue'

const props = defineProps<{
  graph?: {
    nodes: Array<{
      tableName: string
      module: string
      columns: Array<{
        columnName: string
        columnType: string
        primaryKey: boolean
        foreignKey: boolean
      }>
    }>
    relations: Array<{
      sourceTable: string
      sourceColumn: string
      targetTable: string
      targetColumn: string
      label: string
    }>
  }
}>()

const fullscreen = ref(false)
const fullscreenHeight = ref(720)
const fullscreenRenderKey = ref(0)
const normalRenderKey = ref(0)

const relationFocus = ref(false)
const showRelationLabels = ref(false)

const nodeCount = computed(() => props.graph?.nodes?.length ?? 0)
const relationCount = computed(() => props.graph?.relations?.length ?? 0)

const chartKey = computed(() => {
  return [
    'er-normal',
    normalRenderKey.value,
    relationFocus.value,
    showRelationLabels.value,
    nodeCount.value,
    relationCount.value,
  ].join('-')
})

const fullscreenChartKey = computed(() => {
  return [
    'er-fullscreen',
    fullscreenRenderKey.value,
    relationFocus.value,
    showRelationLabels.value,
    nodeCount.value,
    relationCount.value,
  ].join('-')
})

const option = computed(() => {
  const nodes = props.graph?.nodes ?? []
  const relations = props.graph?.relations ?? []
  const isFullscreen = fullscreen.value

  return {
    animation: true,
    animationDuration: 600,
    animationDurationUpdate: 500,
    animationEasingUpdate: 'cubicOut',

    tooltip: {
      trigger: 'item',
      confine: true,
      enterable: false,
      hideDelay: 80,
      extraCssText: 'pointer-events: none;',
      backgroundColor: 'rgba(15, 23, 42, 0.94)',
      borderWidth: 0,
      textStyle: {
        color: '#ffffff',
        fontSize: 12,
        lineHeight: 20,
      },
      formatter: (params: unknown) => {
        return (params as { data?: { tooltip?: string } }).data?.tooltip ?? ''
      },
    },

    legend: {
      top: 8,
      type: 'scroll',
      itemWidth: 10,
      itemHeight: 10,
      textStyle: {
        color: '#475569',
        fontSize: 12,
      },
    },

    series: [{
      type: 'graph',
      layout: 'force',

      left: isFullscreen ? 120 : 80,
      right: isFullscreen ? 120 : 80,
      top: isFullscreen ? 110 : 80,
      bottom: isFullscreen ? 90 : 80,

      /**
       * 改回原始交互方式：
       * 节点可拖动，空白处可拖动画布，滚轮可缩放。
       */
      roam: true,
      draggable: true,

      scaleLimit: {
        min: 0.25,
        max: 6,
      },

      /**
       * 改回更灵活的 force 手感。
       * 不再关闭 layoutAnimation，否则节点会变得很“死”。
       */
      force: {
        repulsion: isFullscreen ? 1500 : 900,
        edgeLength: isFullscreen
            ? ([180, 320] as [number, number])
            : ([120, 220] as [number, number]),
        gravity: isFullscreen ? 0.05 : 0.08,
        friction: 0.45,
        layoutAnimation: true,
      },

      label: {
        show: true,
        formatter: '{b}',
        fontWeight: 700,
        color: '#0f172a',
        fontSize: isFullscreen ? 14 : 12,
        width: isFullscreen ? 220 : 140,
        overflow: 'truncate',
      },

      edgeLabel: {
        show: showRelationLabels.value,
        color: '#334155',
        fontSize: 12,
        backgroundColor: 'rgba(255, 255, 255, 0.9)',
        padding: [2, 6],
        borderRadius: 4,
        formatter: (params: unknown) => {
          return (params as { data?: { relationLabel?: string } }).data?.relationLabel ?? ''
        },
      },

      edgeSymbol: ['none', 'arrow'],
      edgeSymbolSize: [0, isFullscreen ? 10 : 8],

      data: nodes.map((node) => {
        const pkCount = node.columns.filter((column) => column.primaryKey).length
        const fkCount = node.columns.filter((column) => column.foreignKey).length
        const visibleColumnCount = isFullscreen ? 20 : 10

        return {
          id: node.tableName,
          name: node.tableName,
          value: node.columns.length,
          symbolSize: Math.min(isFullscreen ? 116 : 96, 50 + node.columns.length * 2),
          category: node.module,

          itemStyle: {
            opacity: 1,
            borderColor: '#ffffff',
            borderWidth: 2,
            shadowBlur: isFullscreen ? 14 : 8,
            shadowColor: 'rgba(15, 23, 42, 0.16)',
          },

          tooltip: [
            `<strong style="font-size:14px;">${node.tableName}</strong>`,
            `模块：${node.module}`,
            `字段数量：${node.columns.length}`,
            `主键数量：${pkCount}`,
            `外键数量：${fkCount}`,
            '',
            ...node.columns.slice(0, visibleColumnCount).map((column) => {
              const prefix = column.primaryKey ? 'PK ' : column.foreignKey ? 'FK ' : ''
              return `${prefix}${column.columnName}: ${column.columnType}`
            }),
            node.columns.length > visibleColumnCount
                ? `... 还有 ${node.columns.length - visibleColumnCount} 个字段`
                : '',
          ].filter(Boolean).join('<br/>'),
        }
      }),

      links: relations.map((relation) => ({
        source: relation.sourceTable,
        target: relation.targetTable,
        relationLabel: relation.label,
        tooltip: [
          `<strong>外键关系</strong>`,
          `${relation.sourceTable}.${relation.sourceColumn}`,
          '↓',
          `${relation.targetTable}.${relation.targetColumn}`,
          `关系：${relation.label}`,
        ].join('<br/>'),
      })),

      categories: Array.from(new Set(nodes.map((node) => node.module))).map((name) => ({
        name,
      })),

      lineStyle: {
        color: '#94a3b8',
        width: isFullscreen ? 1.9 : 1.4,
        opacity: 0.82,
        curveness: 0.2,
      },

      emphasis: relationFocus.value
          ? {
            focus: 'adjacency',
            scale: true,
            label: {
              show: true,
              fontSize: isFullscreen ? 15 : 13,
              fontWeight: 800,
            },
            lineStyle: {
              width: isFullscreen ? 3.2 : 2.6,
              opacity: 1,
            },
            edgeLabel: {
              show: true,
            },
          }
          : {
            scale: true,
            label: {
              show: true,
              fontSize: isFullscreen ? 15 : 13,
              fontWeight: 800,
            },
            lineStyle: {
              width: isFullscreen ? 2.6 : 2,
              opacity: 1,
            },
            edgeLabel: {
              show: showRelationLabels.value,
            },
          },

      blur: relationFocus.value
          ? {
            itemStyle: {
              opacity: 0.18,
            },
            lineStyle: {
              opacity: 0.08,
            },
            label: {
              opacity: 0.25,
            },
          }
          : undefined,
    }],
  } as EChartsOption
})

function updateFullscreenHeight() {
  fullscreenHeight.value = Math.max(580, window.innerHeight - 210)
}

async function emitChartResize(delay = 0) {
  await nextTick()

  const run = () => {
    requestAnimationFrame(() => {
      window.dispatchEvent(new Event('resize'))
    })
  }

  if (delay > 0) {
    setTimeout(run, delay)
  } else {
    run()
  }
}

async function openFullscreen() {
  updateFullscreenHeight()
  fullscreen.value = true
  fullscreenRenderKey.value += 1

  await emitChartResize()
  await emitChartResize(300)
}

async function closeFullscreen() {
  fullscreen.value = false
  await emitChartResize()
}

async function handleFullscreenClosed() {
  await emitChartResize()
}

async function relayoutFullscreen() {
  fullscreenRenderKey.value += 1
  await emitChartResize()
}

async function relayoutNormal() {
  normalRenderKey.value += 1
  await emitChartResize()
}

async function handleDisplayModeChange() {
  fullscreenRenderKey.value += 1
  normalRenderKey.value += 1
  await emitChartResize()
}

function handleWindowResize() {
  if (!fullscreen.value) return

  updateFullscreenHeight()

  requestAnimationFrame(() => {
    window.dispatchEvent(new Event('resize'))
  })
}

onMounted(() => {
  window.addEventListener('resize', handleWindowResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleWindowResize)
})
</script>

<template>
  <div class="er-graph-chart">
    <div class="er-graph-toolbar">
      <div>
        <strong>数据库ER关系图</strong>
        <span>{{ nodeCount }} 张表 / {{ relationCount }} 条关系</span>
      </div>

      <div class="er-graph-toolbar-actions">
        <el-button
            size="small"
            :disabled="!nodeCount"
            @click="relayoutNormal"
        >
          重新布局
        </el-button>

        <el-button
            type="primary"
            plain
            size="small"
            :disabled="!nodeCount"
            @click="openFullscreen"
        >
          全屏讲解
        </el-button>
      </div>
    </div>

    <ChartPanel
        :key="chartKey"
        title="数据库ER关系图"
        :option="option"
        :empty="!nodeCount"
        :height="520"
    />

    <el-dialog
        v-model="fullscreen"
        class="er-fullscreen-dialog"
        fullscreen
        append-to-body
        destroy-on-close
        :show-close="false"
        @closed="handleFullscreenClosed"
    >
      <template #header>
        <div class="er-fullscreen-header">
          <div>
            <h2>数据库ER关系图</h2>
            <p>
              {{ nodeCount }} 张表 / {{ relationCount }} 条关系。
              节点可拖动，空白处可移动画布，滚轮可缩放。
            </p>
          </div>

          <div class="er-fullscreen-actions">
            <el-switch
                v-model="relationFocus"
                active-text="关系聚焦"
                inactive-text="全图展示"
                @change="handleDisplayModeChange"
            />

            <el-switch
                v-model="showRelationLabels"
                active-text="显示连线文字"
                inactive-text="隐藏连线文字"
                @change="handleDisplayModeChange"
            />

            <el-button @click="relayoutFullscreen">
              重新布局
            </el-button>

            <el-button type="primary" @click="closeFullscreen">
              退出全屏
            </el-button>
          </div>
        </div>
      </template>

      <div class="er-fullscreen-help">
        <article>
          <strong>拖动节点</strong>
          <span>按住任意表节点拖动，可以临时整理讲解位置。</span>
        </article>

        <article>
          <strong>拖动画布</strong>
          <span>按住空白区域拖动，可以移动整个关系图。</span>
        </article>

        <article>
          <strong>滚轮缩放</strong>
          <span>向上放大，向下缩小，适合查看整体或局部。</span>
        </article>

        <article>
          <strong>全图展示</strong>
          <span>默认不会淡化其他节点，适合投屏讲解整体结构。</span>
        </article>

        <article>
          <strong>关系聚焦</strong>
          <span>打开后悬停节点只突出相邻外键关系。</span>
        </article>
      </div>

      <ChartPanel
          v-if="fullscreen"
          :key="fullscreenChartKey"
          title="ER关系图全屏模式"
          :option="option"
          :empty="!nodeCount"
          :height="fullscreenHeight"
      />
    </el-dialog>
  </div>
</template>

<style scoped>
.er-graph-chart {
  width: 100%;
}

.er-graph-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 12px;
}

.er-graph-toolbar strong {
  display: block;
  font-size: 15px;
  color: #0f172a;
}

.er-graph-toolbar span {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.er-graph-toolbar-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.er-fullscreen-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding-right: 8px;
}

.er-fullscreen-header h2 {
  margin: 0;
  font-size: 20px;
  color: #0f172a;
}

.er-fullscreen-header p {
  margin: 6px 0 0;
  font-size: 13px;
  color: #64748b;
}

.er-fullscreen-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 14px;
  flex-wrap: wrap;
}

.er-fullscreen-help {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
}

.er-fullscreen-help article {
  padding: 12px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  background: #ffffff;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.05);
}

.er-fullscreen-help strong {
  display: block;
  margin-bottom: 5px;
  font-size: 13px;
  color: #0f172a;
}

.er-fullscreen-help span {
  display: block;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

:deep(.er-fullscreen-dialog .el-dialog__header) {
  margin-right: 0;
  padding: 18px 24px 12px;
  border-bottom: 1px solid #e2e8f0;
}

:deep(.er-fullscreen-dialog .el-dialog__body) {
  padding: 16px 24px 24px;
  background: #f8fafc;
}

:deep(.er-fullscreen-dialog canvas) {
  touch-action: none;
}

@media (max-width: 1280px) {
  .er-fullscreen-help {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }
}

@media (max-width: 768px) {
  .er-graph-toolbar,
  .er-fullscreen-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .er-fullscreen-help {
    grid-template-columns: 1fr;
  }

  .er-fullscreen-actions {
    justify-content: flex-start;
  }
}
</style>