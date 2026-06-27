<template>
  <div class="course-matrix-tab">
    <div class="tab-header">
      <div class="header-left">
        <span class="tab-title">课程-毕业要求支撑矩阵</span>
        <el-tag v-if="hasUnsavedChanges" type="warning" size="small" effect="plain">
          <el-icon><Warning /></el-icon>
          未保存
        </el-tag>
        <span v-if="hasUnsavedChanges" class="unsaved-tip">有未保存的更改，请点击「保存矩阵」</span>
      </div>
      <el-button
          type="primary"
          size="small"
          @click="handleSaveAll"
          :loading="saving"
          :disabled="disabled || (!hasUnsavedChanges && matrixData.length === 0)"
      >
        {{ hasUnsavedChanges ? '保存矩阵 *' : '保存矩阵' }}
      </el-button>
    </div>

    <div class="matrix-container" v-loading="loading">
      <!-- 加载中状态 -->
      <div v-if="loading" style="min-height: 200px; display: flex; align-items: center; justify-content: center;">
        <span style="color: #909399;">加载矩阵数据...</span>
      </div>

      <!-- 数据加载完成后的内容 -->
      <template v-else>
        <!-- 提示信息 -->
        <el-alert
            v-if="courses.length === 0 || indicators.length === 0"
            type="warning"
            :closable="false"
            show-icon
        >
          <template v-if="courses.length === 0 && indicators.length === 0">
            请先配置<a href="javascript:;" @click="$emit('tabChange', 'courses')">课程</a>和
            <a href="javascript:;" @click="$emit('tabChange', 'requirements')">毕业要求指标点</a>
          </template>
          <template v-else-if="courses.length === 0">
            请先配置<a href="javascript:;" @click="$emit('tabChange', 'courses')">课程</a>
          </template>
          <template v-else-if="indicators.length === 0">
            请先配置<a href="javascript:;" @click="$emit('tabChange', 'requirements')">毕业要求指标点</a>
          </template>
        </el-alert>

        <!-- 矩阵表格 -->
        <el-table
            v-if="courses.length > 0 && indicators.length > 0"
            :data="matrixData"
            border
            style="width: 100%; margin-top: 16px"
            max-height="600"
        >
          <el-table-column prop="courseName" label="课程" width="200" fixed>
            <template #default="scope">
              <span class="course-label">
                <span class="course-code">{{ scope.row.courseCode }}</span>
                <span class="course-name">{{ scope.row.courseName }}</span>
              </span>
            </template>
          </el-table-column>
          <!-- 按毕业要求分组显示指标点 -->
          <el-table-column
              v-for="(indicator, idx) in indicators"
              :key="indicator.id"
              :label="indicator.code"
              width="100"
              align="center"
          >
            <template #header>
              <div class="indicator-header">
                <el-tooltip :content="indicator.description" placement="top" effect="dark">
                  <span class="indicator-code">{{ indicator.code }}</span>
                </el-tooltip>
                <span class="indicator-requirement">{{ indicator.requirementCode }}</span>
              </div>
            </template>
            <template #default="scope">
              <el-select
                  v-model="scope.row.supportMap[indicator.id]"
                  placeholder="-"
                  size="small"
                  style="width: 70px"
                  @change="onSupportChange"
                  :disabled="disabled"
                  :class="getSelectClass(scope.row.supportMap[indicator.id])"
              >
                <el-option value="H">
                  <span class="support-option support-strong">H</span>
                </el-option>
                <el-option value="M">
                  <span class="support-option support-medium">M</span>
                </el-option>
                <el-option value="L">
                  <span class="support-option support-weak">L</span>
                </el-option>
                <el-option value="">
                  <span class="support-option support-none">-</span>
                </el-option>
              </el-select>
            </template>
          </el-table-column>
        </el-table>

        <!-- 图例 -->
        <div class="matrix-legend" v-if="courses.length > 0 && indicators.length > 0">
          <span class="legend-title">支撑强度图例：</span>
          <span class="legend-item support-strong">■ H（强支撑）</span>
          <span class="legend-item support-medium">■ M（中支撑）</span>
          <span class="legend-item support-weak">■ L（弱支撑）</span>
          <span class="legend-item support-none">■ -（无支撑）</span>
          <span class="legend-tip">（在表格下拉框中选择）</span>
        </div>
      </template>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Warning } from '@element-plus/icons-vue'
import request from '@/api/request'

const props = defineProps({
  programId: {
    type: [String, Number],
    required: true
  },
  disabled: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['change', 'tabChange', 'unsaved-change'])

// ============ 状态 ============
const loading = ref(false)
const saving = ref(false)
const courses = ref([])
const indicators = ref([])
const matrixData = ref([])
const hasUnsavedChanges = ref(false)

// 原始数据快照
let originalDataSnapshot = ''

// 生成数据快照
const createSnapshot = (data) => {
  if (!data || data.length === 0) return ''
  return JSON.stringify(data.map(row => ({
    courseId: row.courseId,
    supportMap: row.supportMap
  })))
}

// 获取下拉框样式类
const getSelectClass = (value) => {
  const map = {
    'H': 'select-strong',
    'M': 'select-medium',
    'L': 'select-weak',
    '': 'select-none'
  }
  return map[value] || ''
}

// 检查是否有未保存的更改
const checkUnsavedChanges = () => {
  if (props.disabled) {
    hasUnsavedChanges.value = false
    return
  }
  if (matrixData.value.length === 0) {
    hasUnsavedChanges.value = false
    return
  }
  const currentSnapshot = createSnapshot(matrixData.value)
  hasUnsavedChanges.value = currentSnapshot !== originalDataSnapshot
  emit('unsaved-change', hasUnsavedChanges.value)
}

// 支撑关系变化时标记为未保存
const onSupportChange = () => {
  if (props.disabled) return
  hasUnsavedChanges.value = true
  emit('unsaved-change', true)
}

// ============ 加载数据 ============
const loadData = async () => {
  if (!props.programId) return

  hasUnsavedChanges.value = false
  originalDataSnapshot = ''
  matrixData.value = []
  courses.value = []
  indicators.value = []
  emit('unsaved-change', false)

  loading.value = true

  try {
    // 1. 获取该专业下的所有课程
    const courseRes = await request.get('/admin/course/list', {
      params: {
        programId: props.programId,
        pageNum: 1,
        pageSize: 999 // 获取全部
      }
    })
    courses.value = courseRes.data?.list || []

    // 2. 获取该专业下的所有毕业要求及其指标点
    const reqRes = await request.get(`/admin/program/requirements/list/${props.programId}`)
    const requirements = reqRes.data || []

    // 3. 获取所有指标点
    const allIndicators = []
    for (const req of requirements) {
      const indRes = await request.get(`/admin/program/indicators/list/${req.id}`)
      const inds = (indRes.data || []).map(ind => ({
        ...ind,
        requirementCode: req.code,
        requirementDescription: req.description
      }))
      allIndicators.push(...inds)
    }
    indicators.value = allIndicators

    // 4. 构建矩阵数据
    if (courses.value.length > 0 && indicators.value.length > 0) {
      // 获取每个课程的支撑矩阵
      const matrixPromises = courses.value.map(course =>
          request.get(`/admin/course/detail/${course.id}`)
              .then(res => ({
                courseId: course.id,
                items: res.data?.matrixItems || []
              }))
              .catch(() => ({ courseId: course.id, items: [] }))
      )
      const matrixResults = await Promise.all(matrixPromises)

      // 构建矩阵数据
      const matrixMap = {}
      for (const result of matrixResults) {
        matrixMap[result.courseId] = result.items
      }

      matrixData.value = courses.value.map(course => {
        const row = {
          courseId: course.id,
          courseCode: course.code,
          courseName: course.name,
          supportMap: reactive({})
        }
        // 初始化所有指标点为无支撑
        for (const ind of indicators.value) {
          row.supportMap[ind.id] = ''
        }
        // 填充已有的支撑关系
        const existingItems = matrixMap[course.id] || []
        for (const item of existingItems) {
          const supportLevel = item.supportLevel || ''
          if (['H', 'M', 'L'].includes(supportLevel)) {
            row.supportMap[item.indicatorId] = supportLevel
          } else {
            row.supportMap[item.indicatorId] = ''
          }
        }
        return row
      })

      // 保存初始快照
      originalDataSnapshot = createSnapshot(matrixData.value)
      hasUnsavedChanges.value = false
      emit('unsaved-change', false)
    } else {
      matrixData.value = []
      originalDataSnapshot = ''
      hasUnsavedChanges.value = false
      emit('unsaved-change', false)
    }

  } catch (e) {
    ElMessage.error(e.message || '加载矩阵数据失败')
  } finally {
    loading.value = false
  }
}

// ============ 保存矩阵 ============
const handleSaveAll = async () => {
  if (props.disabled) {
    ElMessage.warning('已发布的专业不可修改')
    return
  }
  if (matrixData.value.length === 0) {
    ElMessage.warning('没有可保存的矩阵数据')
    return
  }

  saving.value = true
  let saveSuccess = true

  try {
    // 逐课程保存支撑矩阵
    for (const row of matrixData.value) {
      const items = []
      for (const ind of indicators.value) {
        const supportLevel = row.supportMap[ind.id]
        if (supportLevel && ['H', 'M', 'L'].includes(supportLevel)) {
          items.push({
            indicatorId: ind.id,
            supportLevel: supportLevel
          })
        }
      }

      await request.post('/admin/course/matrix/batch-save', {
        courseId: row.courseId,
        items: items
      })
    }

    // 更新快照，清除未保存状态
    originalDataSnapshot = createSnapshot(matrixData.value)
    hasUnsavedChanges.value = false
    emit('unsaved-change', false)

    ElMessage.success('矩阵保存成功')
    emit('change')
  } catch (e) {
    saveSuccess = false
    ElMessage.error(e.message || '矩阵保存失败')
  } finally {
    saving.value = false
  }
}

// 保存全部（供父组件调用）
const saveAll = async () => {
  await handleSaveAll()
}

// 离开页面前检查是否有未保存的更改
const checkBeforeLeave = () => {
  if (hasUnsavedChanges.value) {
    return ElMessageBox.confirm(
        '当前有未保存的课程-毕业要求矩阵数据，离开后将丢失更改，确定离开吗？',
        '提示',
        {
          confirmButtonText: '确定离开',
          cancelButtonText: '继续编辑',
          type: 'warning'
        }
    )
  }
  return Promise.resolve()
}

// ============ 监听 ============
watch(() => props.programId, () => {
  hasUnsavedChanges.value = false
  emit('unsaved-change', false)
  loadData()
}, { immediate: true })

watch(() => props.disabled, (newVal) => {
  if (newVal) {
    hasUnsavedChanges.value = false
    emit('unsaved-change', false)
  }
})

// ============ 生命周期 ============
onMounted(() => {
  loadData()
})

onBeforeUnmount(() => {
  if (hasUnsavedChanges.value) {
    emit('unsaved-change', true)
  }
})

// 暴露方法给父组件
defineExpose({
  saveAll,
  checkBeforeLeave,
  hasUnsavedChanges
})
</script>

<style scoped>
.course-matrix-tab {
  padding: 4px 0;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.tab-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.unsaved-tip {
  font-size: 13px;
  color: #e6a23c;
}

.matrix-container {
  margin-top: 4px;
  overflow-x: auto;
}

/* 课程列样式 */
.course-label {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
}

.course-code {
  font-weight: 600;
  color: #409EFF;
  font-size: 13px;
}

.course-name {
  font-size: 13px;
  color: #333;
}

/* 指标点头部样式 */
.indicator-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.indicator-code {
  font-weight: 600;
  color: #303133;
  font-size: 13px;
  cursor: help;
}

.indicator-requirement {
  font-size: 11px;
  color: #909399;
}

/* ========== 支撑强度颜色样式 ========== */
.support-option {
  font-weight: 700;
  font-size: 14px;
}

.support-strong {
  color: #f56c6c;
}

.support-medium {
  color: #e6a23c;
}

.support-weak {
  color: #67c23a;
}

.support-none {
  color: #c0c4cc;
}

/* ========== 下拉框选中后的背景色 ========== */
:deep(.select-strong .el-input__wrapper) {
  background-color: #fef0f0 !important;
  border-color: #f56c6c !important;
}
:deep(.select-strong .el-input__wrapper:hover) {
  border-color: #f56c6c !important;
}
:deep(.select-strong .el-input__wrapper.is-focus) {
  border-color: #f56c6c !important;
  box-shadow: 0 0 0 2px rgba(245, 108, 108, 0.2) !important;
}
:deep(.select-strong .el-select__selected-item) {
  color: #f56c6c !important;
  font-weight: 700;
}

:deep(.select-medium .el-input__wrapper) {
  background-color: #fdf6ec !important;
  border-color: #e6a23c !important;
}
:deep(.select-medium .el-input__wrapper:hover) {
  border-color: #e6a23c !important;
}
:deep(.select-medium .el-input__wrapper.is-focus) {
  border-color: #e6a23c !important;
  box-shadow: 0 0 0 2px rgba(230, 162, 60, 0.2) !important;
}
:deep(.select-medium .el-select__selected-item) {
  color: #e6a23c !important;
  font-weight: 700;
}

:deep(.select-weak .el-input__wrapper) {
  background-color: #f0f9eb !important;
  border-color: #67c23a !important;
}
:deep(.select-weak .el-input__wrapper:hover) {
  border-color: #67c23a !important;
}
:deep(.select-weak .el-input__wrapper.is-focus) {
  border-color: #67c23a !important;
  box-shadow: 0 0 0 2px rgba(103, 194, 58, 0.2) !important;
}
:deep(.select-weak .el-select__selected-item) {
  color: #67c23a !important;
  font-weight: 700;
}

:deep(.select-none .el-input__wrapper) {
  background-color: #f5f7fa !important;
  border-color: #dcdfe6 !important;
}
:deep(.select-none .el-input__wrapper:hover) {
  border-color: #dcdfe6 !important;
}
:deep(.select-none .el-select__selected-item) {
  color: #c0c4cc !important;
}

/* 下拉框选项样式 */
.support-option.support-strong { color: #f56c6c; }
.support-option.support-medium { color: #e6a23c; }
.support-option.support-weak { color: #67c23a; }
.support-option.support-none { color: #c0c4cc; }

/* ========== 图例 ========== */
.matrix-legend {
  margin-top: 16px;
  padding: 12px 20px;
  background: #f7f8fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 20px;
  flex-wrap: wrap;
}

.matrix-legend .legend-title {
  font-weight: 600;
  color: #333;
}

.matrix-legend .legend-item {
  font-size: 14px;
  font-weight: 500;
}

.matrix-legend .legend-item.support-strong { color: #f56c6c; }
.matrix-legend .legend-item.support-medium { color: #e6a23c; }
.matrix-legend .legend-item.support-weak { color: #67c23a; }
.matrix-legend .legend-item.support-none { color: #c0c4cc; }

.matrix-legend .legend-tip {
  font-size: 12px;
  color: #909399;
}

/* 表格内下拉框样式优化 */
:deep(.el-select .el-input__wrapper) {
  min-height: 32px;
  transition: all 0.3s ease;
}

:deep(.el-select-dropdown__item) {
  display: flex;
  justify-content: center;
}

/* 链接样式 */
.course-matrix-tab a {
  color: #409EFF;
  text-decoration: none;
}

.course-matrix-tab a:hover {
  text-decoration: underline;
}
</style>