<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import type { EChartsOption } from 'echarts'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { init, use, type ECharts } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, BarChart, PieChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps<{
  title: string
  option: EChartsOption
  height?: number
}>()

const chartRef = ref<HTMLDivElement | null>(null)
let chart: ECharts | null = null

function renderChart() {
  if (!chartRef.value) {
    return
  }
  chart = chart ?? init(chartRef.value)
  chart.setOption(props.option, true)
}

function resizeChart() {
  chart?.resize()
}

onMounted(() => {
  renderChart()
  window.addEventListener('resize', resizeChart)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resizeChart)
  chart?.dispose()
  chart = null
})

watch(() => props.option, renderChart, { deep: true })
</script>

<template>
  <section class="chart-panel surface">
    <header>
      <h2>{{ title }}</h2>
    </header>
    <div ref="chartRef" class="chart" :style="{ height: `${height ?? 320}px` }" role="img" :aria-label="title" />
  </section>
</template>

<style scoped>
.chart-panel {
  padding: 18px;
}

header {
  margin-bottom: 10px;
}

h2 {
  margin: 0;
  font-size: 18px;
}

.chart {
  width: 100%;
}
</style>
