<template>
  <div class="program-detail-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="goBack">返回</el-button>
        <!-- 专业名称 - 纯展示，不可编辑 -->
        <h2 class="program-name">{{ programName || '专业详情' }}</h2>
        <el-tag :type="programStatus === 'published' ? 'success' : 'warning'">
          {{ programStatus === 'published' ? '已发布' : '草稿' }}
        </el-tag>
        <!-- 版本号 - 纯展示，不可编辑 -->
        <span class="version-text">版本 v{{ programVersion }}</span>
        <!-- 状态提示 -->
        <el-tag v-if="programStatus === 'published'" type="info" size="small" effect="plain">
          <el-icon><InfoFilled /></el-icon>
          已发布，只读
        </el-tag>
        <el-tag v-else type="warning" size="small" effect="plain">
          <el-icon><Edit /></el-icon>
          草稿，可编辑
        </el-tag>
      </div>
      <div>
        <!-- 草稿：显示发布按钮 -->
        <el-button
            v-if="programStatus === 'draft'"
            type="success"
            @click="handlePublish"
        >
          发布
        </el-button>
        <!-- 已发布：显示取消发布按钮 -->
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
              :disabled="programStatus === 'published'"
              @change="handleDataChange"
          />
        </el-tab-pane>
        <el-tab-pane label="毕业要求" name="requirements">
          <RequirementsTab
              ref="requirementsRef"
              :program-id="programId"
              :disabled="programStatus === 'published'"
              @change="handleDataChange"
          />
        </el-tab-pane>
        <el-tab-pane label="支撑矩阵" name="matrix">
          <MatrixTab
              v-if="activeTab === 'matrix'"
              :key="matrixKey"
              ref="matrixRef"
              :program-id="programId"
              :disabled="programStatus === 'published'"
              @change="handleDataChange"
              @tab-change="switchTab"
              @unsaved-change="handleMatrixUnsavedChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, InfoFilled, Edit } from '@element-plus/icons-vue'
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
const matrixKey = ref(0)
const hasMatrixUnsavedChanges = ref(false)

const goalsRef = ref(null)
const requirementsRef = ref(null)
const matrixRef = ref(null)

// 切换 Tab
const switchTab = (tabName) => {
  activeTab.value = tabName
}

// 处理矩阵未保存状态变化
const handleMatrixUnsavedChange = (hasUnsaved) => {
  hasMatrixUnsavedChanges.value = hasUnsaved
}

// Tab 切换
const handleTabChange = (tabName) => {
  // 如果从矩阵tab切换到其他tab，检查是否有未保存的更改
  if (tabName !== 'matrix' && hasMatrixUnsavedChanges.value) {
    if (matrixRef.value?.hasUnsavedChanges) {
      ElMessageBox.confirm(
          '矩阵数据有未保存的更改，切换前请先保存，确定要离开吗？',
          '提示',
          {
            confirmButtonText: '确定离开',
            cancelButtonText: '继续编辑',
            type: 'warning'
          }
      ).then(() => {
        hasMatrixUnsavedChanges.value = false
        doSwitchTab(tabName)
      }).catch(() => {
        activeTab.value = 'matrix'
      })
      return
    }
  }

  // 切换到矩阵tab时强制刷新
  if (tabName === 'matrix') {
    matrixKey.value++
  }

  doSwitchTab(tabName)
}

const doSwitchTab = (tabName) => {
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
  if (hasMatrixUnsavedChanges.value) {
    ElMessageBox.confirm(
        '矩阵数据有未保存的更改，离开后将丢失，确定要离开吗？',
        '提示',
        {
          confirmButtonText: '确定离开',
          cancelButtonText: '继续编辑',
          type: 'warning'
        }
    ).then(() => {
      router.push('/curriculum/management')
    }).catch(() => {})
    return
  }
  router.push('/curriculum/management')
}

// 数据变更标记
const handleDataChange = () => {}

// 发布
const handlePublish = async () => {
  if (hasMatrixUnsavedChanges.value) {
    ElMessage.warning('请先保存矩阵数据再发布')
    activeTab.value = 'matrix'
    matrixKey.value++
    return
  }

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

// 页面离开前提示
const handleBeforeUnload = (e) => {
  if (hasMatrixUnsavedChanges.value) {
    e.preventDefault()
    e.returnValue = '矩阵数据有未保存的更改，确定要离开吗？'
  }
}

onMounted(() => {
  loadProgramInfo()
  window.addEventListener('beforeunload', handleBeforeUnload)
})

onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', handleBeforeUnload)
})
</script>

<style scoped>
.program-detail-container {
  padding: 0;
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
  flex-wrap: wrap;
}

.program-name {
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