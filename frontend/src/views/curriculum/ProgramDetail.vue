<template>
  <div class="program-detail-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2>{{ programName || '专业详情' }}</h2>
        <el-tag :type="programStatus === 'published' ? 'success' : 'warning'">
          {{ programStatus === 'published' ? '已发布' : '草稿' }}
        </el-tag>
        <span class="version-text">v{{ programVersion }}</span>
      </div>
      <div>
        <el-button
            v-if="programStatus === 'draft'"
            type="success"
            @click="handlePublish"
        >
          发布
        </el-button>
        <el-button
            v-if="programStatus === 'published'"
            type="warning"
            @click="handleUnpublish"
        >
          取消发布
        </el-button>
      </div>
    </div>

    <!-- Tab 切换 -->
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="培养目标" name="goals">
          <GoalsTab
              ref="goalsRef"
              :program-id="programId"
              @change="handleDataChange"
          />
        </el-tab-pane>
        <el-tab-pane label="毕业要求" name="requirements">
          <RequirementsTab
              ref="requirementsRef"
              :program-id="programId"
              @change="handleDataChange"
          />
        </el-tab-pane>
        <el-tab-pane label="支撑矩阵" name="matrix">
          <MatrixTab
              ref="matrixRef"
              :program-id="programId"
              @change="handleDataChange"
              @tab-change="switchTab"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import {ref, onMounted} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {ArrowLeft} from '@element-plus/icons-vue'
import request from '@/api/request'
import GoalsTab from './components/GoalsTab.vue'
import RequirementsTab from './components/RequirementsTab.vue'
import MatrixTab from './components/MatrixTab.vue'

defineOptions({
  name: 'ProgramDetail'
})

const route = useRoute()
const router = useRouter()

const programId = ref(route.params.id || '')
const programName = ref('')
const programVersion = ref('')
const programStatus = ref('draft')
const activeTab = ref('goals')

const goalsRef = ref(null)
const requirementsRef = ref(null)
const matrixRef = ref(null)

// 切换 Tab
const switchTab = (tabName) => {
  activeTab.value = tabName
}

// 加载专业信息
const loadProgramInfo = async () => {
  if (!programId.value) {
    ElMessage.warning('缺少专业ID')
    return
  }
  try {
    const res = await request.get(`/admin/program/get/${programId.value}`)
    if (res.data) {
      programName.value = res.data.majorName || ''
      programVersion.value = res.data.version || ''
      programStatus.value = res.data.status || 'draft'
    }
  } catch (e) {
    ElMessage.error('加载专业信息失败')
  }
}

// 返回列表页
const goBack = () => {
  router.push('/curriculum/management')
}

// Tab 切换
const handleTabChange = (tabName) => {
  // 切换时加载对应 Tab 数据
}

// 数据变更标记
const handleDataChange = () => {
  // 可以在这里添加未保存提示逻辑
}

// 发布
const handlePublish = async () => {
  try {
    await request.put(`/admin/program/publish/${programId.value}`)
    ElMessage.success('发布成功')
    loadProgramInfo()
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  }
}

// 取消发布
const handleUnpublish = async () => {
  try {
    await request.put(`/admin/program/unpublish/${programId.value}`)
    ElMessage.success('已取消发布')
    loadProgramInfo()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  loadProgramInfo()
})
</script>

<style scoped>
.program-detail-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-left h2 {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: #303133;
}

.version-text {
  color: #909399;
  font-size: 14px;
}
</style>