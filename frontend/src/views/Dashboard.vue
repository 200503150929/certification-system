<template>
  <div class="dashboard-container" v-loading="loading">
    <div class="dashboard-header">
      <div>
        <h2>工程教育认证数据看板</h2>
        <p>全周期教学数据</p>
      </div>
      <div>
        <el-select v-model="selectedProgramId" placeholder="选择培养方案" clearable style="width: 220px; margin-right: 10px" @change="onProgramChange">
          <el-option v-for="p in programOptions" :key="p.id" :label="p.majorName" :value="p.id" />
        </el-select>
        <el-select v-model="selectedSemester" placeholder="选择学期" clearable style="width: 220px; margin-right: 10px" @change="onSemesterChange">
          <el-option label="全部学期" value="" />
          <el-option v-for="s in semesterOptions" :key="s.value" :label="s.label" :value="s.value" />
        </el-select>
        <el-button :icon="Refresh" circle @click="refreshAll" :loading="loading" />
      </div>
    </div>

    <el-row :gutter="20" class="kpi-cards">
      <el-col :span="6" v-for="kpi in kpiData" :key="kpi.name">
        <el-card shadow="never">
          <div class="kpi-card-item">
            <div class="kpi-text">
              <p class="kpi-label">{{ kpi.name }}</p>
              <p class="kpi-value">{{ formatNumber(kpi.value) }} <small>{{ kpi.unit }}</small></p>
              <p v-if="kpi.changePercent !== 0" :class="['kpi-change', kpi.positive ? 'increase' : 'decrease']">
                {{ kpi.changePercent > 0 ? '+' : '' }}{{ formatNumber(kpi.changePercent) }}%
              </p>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>毕业要求指标点达成度柱状图</span>
            <el-tooltip placement="bottom" effect="dark" raw-content>
              <template #content>
                <div style="max-width:360px;line-height:1.8">
                  <p style="margin:0 0 8px;font-weight:bold">计算说明：</p>
                  <p style="margin:0 0 4px">① 课程目标达成度 = 全班学生加权平均分 ÷ 100<br/>（加权：平时×25% + 实验×25% + 期中×25% + 期末×25%）</p>
                  <p style="margin:0 0 4px">② 指标点达成度 = Σ(课程目标达成度 × 支撑强度权重) ÷ Σ权重<br/>（支撑强度权重：H=1.0 / M=0.7 / L=0.4）</p>
                  <p style="margin:0">③ 柱状图中红色(&lt;60%)表示未达标</p>
                </div>
              </template>
              <el-icon style="margin-left:6px;cursor:help;color:#909399;vertical-align:middle"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <div ref="barChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span>毕业要求达成度雷达图</span>
            <el-tooltip placement="bottom" effect="dark" raw-content>
              <template #content>
                <div style="max-width:360px;line-height:1.8">
                  <p style="margin:0 0 8px;font-weight:bold">计算说明：</p>
                  <p style="margin:0 0 4px">① 六个维度分别对应毕业要求 GR1~GR6：<br/>工程知识、问题分析、设计/开发解决方案、研究、使用现代工具、个人和团队</p>
                  <p style="margin:0 0 4px">② 毕业要求达成度 = 其下所有指标点达成度的最小值</p>
                  <p style="margin:0">③ 维度值 = 该维度下所有毕业要求达成度的平均值 × 100</p>
                </div>
              </template>
              <el-icon style="margin-left:6px;cursor:help;color:#909399;vertical-align:middle"><QuestionFilled /></el-icon>
            </el-tooltip>
          </template>
          <div ref="radarChartRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px;">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>全院课程成绩分布分析</template>
          <div ref="gradeDistRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>各考核环节平均得分率</template>
          <div ref="assessmentRef" style="height: 300px;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Refresh, QuestionFilled } from '@element-plus/icons-vue'
import request from '@/api/request'

// ============ 状态 ============
const loading = ref(false)
const selectedProgramId = ref(null)
const programOptions = ref([])
const selectedSemester = ref('')
const semesterOptions = ref([])

const kpiData = ref([])

const barChartRef = ref(null)
const radarChartRef = ref(null)
const gradeDistRef = ref(null)
const assessmentRef = ref(null)

let barChartInstance = null
let radarChartInstance = null
let gradeDistInstance = null
let assessmentInstance = null

// ============ 工具函数 ============
const formatNumber = (val) => {
  if (val === null || val === undefined) return '0'
  const num = Number(val)
  if (Number.isInteger(num)) return num.toString()
  return num.toFixed(1)
}

/**
 * 构建通用查询参数：培养方案 + 学期
 */
const buildParams = () => {
  const params = {}
  if (selectedProgramId.value) {
    params.programId = selectedProgramId.value
  }
  if (selectedSemester.value) {
    const parts = selectedSemester.value.split('_')
    params.academicYear = parts[0]
    params.semester = parts[1]
  }
  return params
}

/**
 * 仅构建学期参数（KPI、成绩分布、考核得分率不用 programId）
 */
const buildSemesterParams = () => {
  const params = {}
  if (selectedSemester.value) {
    const parts = selectedSemester.value.split('_')
    params.academicYear = parts[0]
    params.semester = parts[1]
  }
  return params
}

// ============ 加载培养方案选项 ============
const fetchProgramOptions = async () => {
  try {
    const res = await request.get('/admin/program/options')
    if (res.status === 'success' && res.data) {
      programOptions.value = res.data
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 加载学期选项 ============
const fetchSemesterOptions = async () => {
  try {
    const res = await request.get('/dashboard/semester-options')
    if (res.status === 'success' && res.data) {
      semesterOptions.value = res.data
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 加载 KPI ============
const fetchKpi = async () => {
  try {
    const params = buildSemesterParams()
    const res = await request.get('/dashboard/kpi', { params })
    if (res.status === 'success' && res.data) {
      kpiData.value = res.data
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 柱状图 ============
const fetchBarChart = async () => {
  try {
    const params = buildParams()
    const res = await request.get('/dashboard/bar-chart', { params })
    if (res.status === 'success' && res.data) {
      const data = res.data
      barChartInstance.setOption({
        xAxis: {
          type: 'category',
          data: data.map(d => d.name)
        },
        yAxis: {
          type: 'value',
          max: 100,
          axisLabel: { formatter: '{value}%' }
        },
        series: [{
          data: data.map(d => Number(d.value)),
          type: 'bar',
          itemStyle: {
            color: (params) => params.value < 60 ? '#f56c6c' : '#409eff'
          }
        }],
        tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
        grid: { top: '10%', left: '3%', right: '4%', bottom: '3%', containLabel: true }
      })
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 雷达图 ============
const fetchRadarChart = async () => {
  try {
    const params = buildParams()
    const res = await request.get('/dashboard/radar-chart', { params })
    if (res.status === 'success' && res.data) {
      const data = res.data
      radarChartInstance.setOption({
        radar: {
          indicator: (data.dimensions || []).map(name => ({ name, max: 100 }))
        },
        series: [{
          type: 'radar',
          data: [
            { value: (data.values || []).map(v => Number(v)), name: '当前值' }
          ]
        }],
        tooltip: { trigger: 'item' }
      })
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 成绩分布 ============
const fetchGradeDistribution = async () => {
  try {
    const params = buildSemesterParams()
    const res = await request.get('/dashboard/grade-distribution', { params })
    if (res.status === 'success' && res.data) {
      const data = res.data
      gradeDistInstance.setOption({
        xAxis: {
          type: 'category',
          data: data.map(d => d.range)
        },
        yAxis: { type: 'value' },
        series: [{
          data: data.map(d => d.count),
          type: 'bar',
          itemStyle: { color: '#409eff' }
        }],
        tooltip: { trigger: 'axis', formatter: (params) => `${params[0].name}: ${params[0].value}门课` },
        grid: { top: '10%', left: '3%', right: '4%', bottom: '3%', containLabel: true }
      })
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 考核得分率 ============
const fetchAssessmentRates = async () => {
  try {
    const params = buildSemesterParams()
    const res = await request.get('/dashboard/assessment-score-rates', { params })
    if (res.status === 'success' && res.data) {
      const data = res.data
      assessmentInstance.setOption({
        yAxis: {
          type: 'category',
          data: data.map(d => d.name)
        },
        xAxis: {
          type: 'value',
          max: 100,
          axisLabel: { formatter: '{value}%' }
        },
        series: [{
          data: data.map(d => Math.round(Number(d.scoreRate) * 100)),
          type: 'bar',
          barWidth: '20px',
          itemStyle: { color: '#f9a825' }
        }],
        tooltip: { trigger: 'axis', formatter: '{b}: {c}%' },
        grid: { top: '10%', left: '3%', right: '4%', bottom: '3%', containLabel: true }
      })
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 刷新 ============
const refreshAll = async () => {
  loading.value = true
  try {
    await Promise.all([
      fetchKpi(),
      fetchBarChart(),
      fetchRadarChart(),
      fetchGradeDistribution(),
      fetchAssessmentRates()
    ])
  } finally {
    loading.value = false
  }
}

const onProgramChange = () => {
  refreshAll()
}

const onSemesterChange = () => {
  refreshAll()
}

// ============ 初始化 ============
onMounted(async () => {
  await Promise.all([fetchProgramOptions(), fetchSemesterOptions()])
  await nextTick()

  // 初始化 ECharts 实例
  barChartInstance = echarts.init(barChartRef.value)
  radarChartInstance = echarts.init(radarChartRef.value)
  gradeDistInstance = echarts.init(gradeDistRef.value)
  assessmentInstance = echarts.init(assessmentRef.value)

  // 加载所有数据
  await refreshAll()
})
</script>

<style scoped>
.dashboard-container {
  padding: 20px;
}
.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.dashboard-header h2 {
  margin: 0;
}
.dashboard-header p {
  color: #909399;
  margin: 5px 0 0;
}
.kpi-cards {
  margin-bottom: 20px;
}
.kpi-cards .el-card {
  border: none;
}
.kpi-card-item {
  display: flex;
  align-items: center;
}
.kpi-text {
  width: 100%;
}
.kpi-label {
  color: #909399;
  margin: 0;
  font-size: 14px;
}
.kpi-value {
  font-size: 28px;
  font-weight: bold;
  margin: 8px 0;
  color: #303133;
}
.kpi-value small {
  font-size: 14px;
  font-weight: normal;
  color: #909399;
}
.kpi-change {
  margin: 0;
  font-size: 13px;
}
.kpi-change.increase {
  color: #67c23a;
}
.kpi-change.decrease {
  color: #f56c6c;
}
</style>
