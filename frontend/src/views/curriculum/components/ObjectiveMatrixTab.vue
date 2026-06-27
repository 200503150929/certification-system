<template>
  <div class="matrix-tab">
    <div class="tab-header">
      <div class="header-left">
        <span class="tab-title">支撑矩阵管理</span>
        <el-tag v-if="hasUnsavedChanges" type="warning" size="small" effect="plain">
          <el-icon><Warning /></el-icon>
          未保存
        </el-tag>
        <span v-if="hasUnsavedChanges" class="unsaved-tip">有未保存的更改，如需保存请点击「保存矩阵」</span>
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
            v-if="objectives.length === 0 || requirements.length === 0"
            type="warning"
            :closable="false"
            show-icon
        >
          <template v-if="objectives.length === 0 && requirements.length === 0">
            请先配置<a href="javascript:;" @click="$emit('tabChange', 'goals')">培养目标</a>和
            <a href="javascript:;" @click="$emit('tabChange', 'requirements')">毕业要求</a>
          </template>
          <template v-else-if="objectives.length === 0">
            请先配置<a href="javascript:;" @click="$emit('tabChange', 'goals')">培养目标</a>
          </template>
          <template v-else-if="requirements.length === 0">
            请先配置<a href="javascript:;" @click="$emit('tabChange', 'requirements')">毕业要求</a>
          </template>
        </el-alert>

        <!-- 矩阵表格 -->
        <el-table
            v-if="requirements.length > 0 && objectives.length > 0"
            :data="matrixData"
            border
            style="width: 100%; margin-top: 16px"
        >
          <el-table-column prop="description" label="培养目标 / 毕业要求" width="240" fixed>
            <template #default="scope">
              <span class="objective-label">{{ scope.row.description }}</span>
            </template>
          </el-table-column>
          <el-table-column
              v-for="req in requirements"
              :key="req.id"
              :label="req.code"
              width="120"
              align="center"
          >
            <template #default="scope">
              <el-select
                  v-model="scope.row.supportMap[req.id]"
                  placeholder="-"
                  size="small"
                  style="width: 80px"
                  @change="onSupportChange"
                  :disabled="disabled"
                  :class="getSelectClass(scope.row.supportMap[req.id])"
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
        <div class="matrix-legend" v-if="requirements.length > 0 && objectives.length > 0">
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

const objectives = ref([])
const requirements = ref([])
const matrixData = ref([])
const loading = ref(false)
const saving = ref(false)
const hasUnsavedChanges = ref(false)
const isInitialLoad = ref(true)

const emptyText = ref('')

// 原始数据快照（用于对比是否修改）
let originalDataSnapshot = ''

// 生成数据快照
const createSnapshot = (data) => {
  if (!data || data.length === 0) return ''
  return JSON.stringify(data.map(row => ({
    id: row.id,
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
  // 如果已发布，不允许修改，直接返回 false
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

// 加载所有数据
const loadData = async () => {
  if (!props.programId) return

  // 重置所有状态
  hasUnsavedChanges.value = false
  originalDataSnapshot = ''
  matrixData.value = []
  objectives.value = []
  requirements.value = []

  // 通知父组件未保存状态为 false
  emit('unsaved-change', false)

  loading.value = true

  try {
    // 并行加载目标和毕业要求
    const [objRes, reqRes] = await Promise.all([
      request.get(`/admin/program/objectives/list/${props.programId}`),
      request.get(`/admin/program/requirements/list/${props.programId}`)
    ])
    objectives.value = objRes.data || []
    requirements.value = reqRes.data || []

    // 如果都没有数据，显示提示
    if (objectives.value.length === 0 && requirements.value.length === 0) {
      emptyText.value = '请先配置培养目标和毕业要求'
    } else if (objectives.value.length === 0) {
      emptyText.value = '请先配置培养目标'
    } else if (requirements.value.length === 0) {
      emptyText.value = '请先配置毕业要求'
    }

    // 为每个目标加载其支撑矩阵
    if (objectives.value.length > 0 && requirements.value.length > 0) {
      const matrixPromises = objectives.value.map(obj =>
          request.get(`/admin/program/matrix/list/${obj.id}`)
              .then(res => ({
                objectiveId: obj.id,
                items: res.data || []
              }))
              .catch(() => ({ objectiveId: obj.id, items: [] }))
      )
      const matrixResults = await Promise.all(matrixPromises)

      // 构建矩阵数据
      const matrixMap = {}
      for (const result of matrixResults) {
        matrixMap[result.objectiveId] = result.items
      }

      matrixData.value = objectives.value.map(obj => {
        const row = {
          id: obj.id,
          description: obj.description,
          supportMap: reactive({})
        }
        // 初始化所有要求为无支撑
        for (const req of requirements.value) {
          row.supportMap[req.id] = ''
        }
        // 填充已有的支撑关系（数据库直接存的是 H/M/L，无需转换）
        const existingItems = matrixMap[obj.id] || []
        for (const item of existingItems) {
          const supportLevel = item.supportLevel || ''
          // 直接使用数据库中的值，支持 H/M/L
          if (supportLevel === 'H' || supportLevel === 'M' || supportLevel === 'L') {
            row.supportMap[item.requirementId] = supportLevel
          } else {
            row.supportMap[item.requirementId] = ''
          }
        }
        return row
      })

      // 保存初始快照
      originalDataSnapshot = createSnapshot(matrixData.value)
      // 确保未保存状态为 false
      hasUnsavedChanges.value = false
      emit('unsaved-change', false)
    } else {
      matrixData.value = []
      originalDataSnapshot = ''
      hasUnsavedChanges.value = false
      emit('unsaved-change', false)
    }

    isInitialLoad.value = false
  } catch (e) {
    ElMessage.error(e.message || '加载矩阵数据失败')
  } finally {
    loading.value = false
  }
}

// 保存所有目标的矩阵
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
  try {
    for (const row of matrixData.value) {
      const items = []
      for (const req of requirements.value) {
        const supportLevel = row.supportMap[req.id]
        if (supportLevel) {
          items.push({
            requirementId: req.id,
            supportLevel: supportLevel // 直接存 H/M/L，无需转换
          })
        }
      }
      await request.post('/admin/program/matrix/batch-save', {
        objectiveId: row.id,
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
        '当前有未保存的矩阵数据，离开后将丢失更改，确定离开吗？',
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

// 监听 programId 变化重新加载
watch(() => props.programId, () => {
  // 重置未保存状态
  hasUnsavedChanges.value = false
  emit('unsaved-change', false)
  loadData()
}, { immediate: true })

// 监听 disabled 变化，当变为 true（已发布）时重置未保存状态
watch(() => props.disabled, (newVal) => {
  if (newVal) {
    hasUnsavedChanges.value = false
    emit('unsaved-change', false)
  }
})

onMounted(() => {
  loadData()
})

// 组件销毁前检查未保存状态
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
.matrix-tab {
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

.objective-label {
  font-weight: 500;
  color: #333;
  font-size: 13px;
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
.matrix-tab a {
  color: #409EFF;
  text-decoration: none;
}

.matrix-tab a:hover {
  text-decoration: underline;
}
</style>