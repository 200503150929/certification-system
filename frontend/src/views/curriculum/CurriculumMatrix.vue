<template>
  <div class="curriculum-matrix-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/curriculum/management' }">专业管理</el-breadcrumb-item>
              <el-breadcrumb-item>支撑矩阵</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ programName }}</div>
          </div>
          <div class="header-right">
            <el-button type="primary" @click="handleSaveAll" :loading="saving">保存矩阵</el-button>
          </div>
        </div>
      </template>

      <div class="matrix-container" v-loading="loading">
        <el-table v-if="requirements.length > 0" :data="matrixData" border style="width: 100%">
          <el-table-column prop="description" label="培养目标 / 毕业要求" width="240" fixed>
            <template #default="scope">
              <span class="objective-label">{{ scope.row.description }}</span>
            </template>
          </el-table-column>
          <el-table-column
            v-for="req in requirements"
            :key="req.id"
            :label="req.code"
            width="110"
          >
            <template #default="scope">
              <el-select
                v-model="scope.row.supportMap[req.id]"
                placeholder="无"
                size="small"
              >
                <el-option label="强支撑" value="强" />
                <el-option label="弱支撑" value="弱" />
                <el-option label="无" value="" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="!loading && (objectives.length === 0 || requirements.length === 0)" description="请先配置培养目标和毕业要求" />

        <div class="matrix-legend" v-if="requirements.length > 0">
          <span class="legend-title">支撑强度图例：</span>
          <span class="legend-item strong">■ 强支撑</span>
          <span class="legend-item weak">■ 弱支撑</span>
          <span class="legend-item none">■ 无支撑</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'

const route = useRoute()
const programId = ref(route.query.programId || '')
const programName = ref('')

const objectives = ref([])
const requirements = ref([])
const matrixData = ref([])
const loading = ref(false)
const saving = ref(false)

// 加载专业信息
const loadProgramInfo = async () => {
  if (!programId.value) return
  try {
    const res = await request.get(`/admin/program/get/${programId.value}`)
    if (res.data) {
      programName.value = res.data.majorName || ''
    }
  } catch {
    // ignore
  }
}

// 加载所有数据
const loadData = async () => {
  if (!programId.value) return
  loading.value = true
  try {
    // 并行加载目标和毕业要求
    const [objRes, reqRes] = await Promise.all([
      request.get(`/admin/program/objectives/list/${programId.value}`),
      request.get(`/admin/program/requirements/list/${programId.value}`)
    ])
    objectives.value = objRes.data || []
    requirements.value = reqRes.data || []

    // 为每个目标加载其支撑矩阵
    const matrixPromises = objectives.value.map(obj =>
      request.get(`/admin/program/matrix/list/${obj.id}`).then(res => ({
        objectiveId: obj.id,
        items: res.data || []
      })).catch(() => ({ objectiveId: obj.id, items: [] }))
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
      // 填充已有的支撑关系
      const existingItems = matrixMap[obj.id] || []
      for (const item of existingItems) {
        row.supportMap[item.requirementId] = item.supportLevel || ''
      }
      return row
    })
  } catch (e) {
    ElMessage.error(e.message || '加载矩阵数据失败')
  } finally {
    loading.value = false
  }
}

// 保存所有目标的矩阵
const handleSaveAll = async () => {
  saving.value = true
  try {
    let savedCount = 0
    for (const row of matrixData.value) {
      const items = []
      for (const req of requirements.value) {
        const supportLevel = row.supportMap[req.id]
        if (supportLevel) {
          items.push({
            requirementId: req.id,
            supportLevel: supportLevel
          })
        }
      }
      // 即使 items 为空也保存（清空支撑关系）
      await request.post('/admin/program/matrix/batch-save', {
        objectiveId: row.id,
        items: items
      })
      savedCount++
    }
    ElMessage.success(`矩阵保存成功（已保存 ${savedCount} 个培养目标的支撑关系）`)
  } catch (e) {
    ElMessage.error(e.message || '矩阵保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProgramInfo()
  loadData()
})
</script>

<style scoped>
.curriculum-matrix-container { padding: 20px; }

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  width: 100%;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.header-right {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
  margin-top: 2px;
}

.page-subtitle {
  font-size: 18px;
  font-weight: 600;
  color: #409EFF;
  margin-left: 2px;
}

.matrix-container { margin-top: 16px; overflow-x: auto; }

.objective-label {
  font-weight: 500;
  color: #333;
}

.matrix-legend {
  margin-top: 16px;
  padding: 12px 20px;
  background: #f7f8fa;
  border-radius: 6px;
  display: flex;
  align-items: center;
  gap: 20px;
}
.matrix-legend .legend-title { font-weight: 600; color: #333; }
.matrix-legend .legend-item { font-size: 14px; }
.matrix-legend .strong { color: #f56c6c; }
.matrix-legend .weak { color: #e6a23c; }
.matrix-legend .none { color: #c0c4cc; }
</style>
