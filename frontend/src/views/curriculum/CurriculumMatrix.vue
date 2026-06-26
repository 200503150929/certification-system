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
            <el-button :icon="Download" @click="handleExport">导出矩阵</el-button>
            <el-button type="primary" @click="handleSave">保存矩阵</el-button>
          </div>
        </div>
      </template>

      <div class="matrix-container">
        <el-table :data="matrixData" border style="width: 100%">
          <el-table-column prop="goal" label="培养目标 / 毕业要求" width="200" fixed />
          <el-table-column
            v-for="req in requirementList"
            :key="req.id"
            :label="req.code"
            width="100"
          >
            <template #default="scope">
              <el-select
                v-model="scope.row[req.id]"
                placeholder="无"
                size="small"
                @change="handleMatrixChange(scope.row, req.id)"
              >
                <el-option label="强支撑" value="强" />
                <el-option label="弱支撑" value="弱" />
                <el-option label="无" value="" />
              </el-select>
            </template>
          </el-table-column>
        </el-table>

        <div class="matrix-legend">
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
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'

const programName = ref('计算机科学与技术')

const requirementList = ref([
  { id: 'req1', code: '1' },
  { id: 'req2', code: '2' },
  { id: 'req3', code: '3' },
  { id: 'req4', code: '4' },
  { id: 'req5', code: '5' },
  { id: 'req6', code: '6' },
  { id: 'req7', code: '7' },
  { id: 'req8', code: '8' },
])

const matrixData = ref([
  {
    id: '1',
    goal: '目标1：掌握扎实的专业知识',
    req1: '强',
    req2: '强',
    req3: '强',
    req4: '弱',
    req5: '',
    req6: '',
    req7: '',
    req8: ''
  },
  {
    id: '2',
    goal: '目标2：具备工程实践与创新能力',
    req1: '弱',
    req2: '强',
    req3: '强',
    req4: '强',
    req5: '弱',
    req6: '',
    req7: '',
    req8: ''
  },
  {
    id: '3',
    goal: '目标3：具有团队协作与国际视野',
    req1: '',
    req2: '',
    req3: '弱',
    req4: '',
    req5: '',
    req6: '强',
    req7: '强',
    req8: '弱'
  },
])

const handleMatrixChange = (row, reqId) => {
  console.log('Matrix changed:', row.goal, reqId, row[reqId])
}

const handleSave = () => {
  ElMessage.success('矩阵保存成功')
}

const handleExport = () => {
  ElMessage.success('矩阵导出成功')
}

onMounted(() => {})
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