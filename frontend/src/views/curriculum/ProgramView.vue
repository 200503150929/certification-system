<template>
  <div class="program-view-container">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <el-button link :icon="ArrowLeft" @click="goBack">返回</el-button>
        <h2>{{ programName || '专业详情' }}</h2>
        <el-tag :type="programStatus === 'published' ? 'success' : 'warning'">
          {{ programStatus === 'published' ? '已发布' : '草稿' }}
        </el-tag>
        <span class="version-text">v{{ programVersion }}</span>
        <el-tag type="info" size="small" effect="plain">
          <el-icon><InfoFilled /></el-icon>
          只读模式，仅供查看
        </el-tag>
      </div>
    </div>

    <!-- Tab 切换 -->
    <el-card>
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="培养目标" name="goals">
          <GoalsTab
              ref="goalsRef"
              :program-id="programId"
              :disabled="true"
          />
        </el-tab-pane>
        <el-tab-pane label="毕业要求" name="requirements">
          <RequirementsTab
              ref="requirementsRef"
              :program-id="programId"
              :disabled="true"
          />
        </el-tab-pane>
        <el-tab-pane label="课程体系" name="courses">
          <CoursesTab
              v-if="activeTab === 'courses'"
              :key="coursesKey"
              ref="coursesRef"
              :program-id="programId"
              :disabled="true"
          />
        </el-tab-pane>
        <!-- 培养目标-毕业要求矩阵 -->
        <el-tab-pane label="目标-要求矩阵" name="objectiveMatrix">
          <ObjectiveMatrixTab
              v-if="activeTab === 'objectiveMatrix'"
              :key="`objectiveMatrix-${programId}`"
              ref="objectiveMatrixRef"
              :program-id="programId"
              :disabled="true"
              @tab-change="switchTab"
          />
        </el-tab-pane>
        <!-- 课程-毕业要求矩阵 -->
        <el-tab-pane label="课程-要求矩阵" name="courseMatrix">
          <CourseMatrixTab
              v-if="activeTab === 'courseMatrix'"
              :key="`courseMatrix-${programId}`"
              ref="courseMatrixRef"
              :program-id="programId"
              :disabled="true"
              @tab-change="switchTab"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import GoalsTab from './components/GoalsTab.vue'
import RequirementsTab from './components/RequirementsTab.vue'
import CoursesTab from './components/CoursesTab.vue'
import ObjectiveMatrixTab from './components/ObjectiveMatrixTab.vue'
import CourseMatrixTab from './components/CourseMatrixTab.vue'

defineOptions({
  name: 'ProgramView'
})

const route = useRoute()
const router = useRouter()

// ============ 专业信息 ============
const programId = ref(route.params.id || '')
const programName = ref('')
const programVersion = ref('')
const programStatus = ref('draft')

// ============ Tab 状态 ============
const activeTab = ref('goals')
const coursesKey = ref(0)

// ============ 组件引用 ============
const goalsRef = ref(null)
const requirementsRef = ref(null)
const coursesRef = ref(null)
const objectiveMatrixRef = ref(null)
const courseMatrixRef = ref(null)

// ============ 方法 ============

// 切换 Tab（供 MatrixTab 调用）
const switchTab = (tabName) => {
  activeTab.value = tabName
}

// Tab 切换
const handleTabChange = (tabName) => {
  if (tabName === 'courses') {
    coursesKey.value++
  }
}

// ============ 加载专业信息 ============
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

// ============ 返回列表页 ============
const goBack = () => {
  router.push('/curriculum/view')
}

// ============ 监听路由参数变化 ============
watch(() => route.params.id, (newId) => {
  if (newId) {
    programId.value = newId
    loadProgramInfo()
  }
}, { immediate: true })

// ============ 生命周期 ============
onMounted(() => {
  loadProgramInfo()
})
</script>

<style scoped>
.program-view-container {
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

:deep(.el-card__body) {
  padding: 10px 20px 20px 20px;
}

:deep(.el-tabs__content) {
  padding-top: 16px;
}
</style>
