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
        <!-- 已发布状态提示 -->
        <el-tag v-if="programStatus === 'published'" type="info" size="small" effect="plain">
          <el-icon>
            <InfoFilled/>
          </el-icon>
          该专业培养方案已发布，不可编辑
        </el-tag>
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
        <el-tab-pane label="课程体系" name="courses">
          <CoursesTab
              v-if="activeTab === 'courses'"
              :key="coursesKey"
              ref="coursesRef"
              :program-id="programId"
              :disabled="programStatus === 'published'"
              @change="handleDataChange"
          />
        </el-tab-pane>
        <!-- 培养目标-毕业要求矩阵 -->
        <el-tab-pane label="目标-要求矩阵" name="objectiveMatrix">
          <ObjectiveMatrixTab
              v-if="activeTab === 'objectiveMatrix'"
              :key="`objectiveMatrix-${programId}`"
              ref="objectiveMatrixRef"
              :program-id="programId"
              :disabled="programStatus === 'published'"
              @change="handleDataChange"
              @tab-change="switchTab"
              @unsaved-change="handleObjectiveMatrixUnsavedChange"
          />
        </el-tab-pane>
        <!-- 课程-毕业要求矩阵 -->
        <el-tab-pane label="课程-要求矩阵" name="courseMatrix">
          <CourseMatrixTab
              v-if="activeTab === 'courseMatrix'"
              :key="`courseMatrix-${programId}`"
              ref="courseMatrixRef"
              :program-id="programId"
              :disabled="programStatus === 'published'"
              @change="handleDataChange"
              @tab-change="switchTab"
              @unsaved-change="handleCourseMatrixUnsavedChange"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, InfoFilled } from '@element-plus/icons-vue'
import request from '@/api/request'
import GoalsTab from './components/GoalsTab.vue'
import RequirementsTab from './components/RequirementsTab.vue'
import CoursesTab from './components/CoursesTab.vue'
import ObjectiveMatrixTab from './components/ObjectiveMatrixTab.vue'
import CourseMatrixTab from './components/CourseMatrixTab.vue'
import { useProgramValidation } from '@/composables/useProgramValidation'

defineOptions({
  name: 'ProgramDetail'
})

const route = useRoute()
const router = useRouter()
const { validateProgramData } = useProgramValidation()

// ============ 专业信息 ============
const programId = ref(route.params.id || '')
const programName = ref('')
const programVersion = ref('')
const programStatus = ref('draft')

// ============ Tab 状态 ============
const activeTab = ref('goals')
const coursesKey = ref(0)
const hasObjectiveMatrixUnsavedChanges = ref(false)
const hasCourseMatrixUnsavedChanges = ref(false)

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

// 处理培养目标-毕业要求矩阵未保存状态变化
const handleObjectiveMatrixUnsavedChange = (hasUnsaved) => {
  hasObjectiveMatrixUnsavedChanges.value = hasUnsaved
}

// 处理课程-毕业要求矩阵未保存状态变化
const handleCourseMatrixUnsavedChange = (hasUnsaved) => {
  hasCourseMatrixUnsavedChanges.value = hasUnsaved
}

// 检查是否有任何未保存的矩阵
const hasAnyUnsavedChanges = () => {
  return hasObjectiveMatrixUnsavedChanges.value || hasCourseMatrixUnsavedChanges.value
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
  if (hasAnyUnsavedChanges()) {
    ElMessageBox.confirm(
        '有矩阵数据未保存的更改，离开后将丢失，确定要离开吗？',
        '提示',
        {
          confirmButtonText: '确定离开',
          cancelButtonText: '继续编辑',
          type: 'warning'
        }
    ).then(() => {
      router.push('/app/curriculum/management')
    }).catch(() => {})
    return
  }
  router.push('/app/curriculum/management')
}

// ============ 数据变更标记 ============
const handleDataChange = () => {
  // 可以在这里添加未保存提示逻辑
}

// ============ 发布 ============
const handlePublish = async () => {
  // 1. 检查是否有未保存的矩阵数据
  if (hasAnyUnsavedChanges()) {
    ElMessage.warning('请先保存所有矩阵数据再发布')
    if (hasObjectiveMatrixUnsavedChanges.value) {
      activeTab.value = 'objectiveMatrix'
    } else if (hasCourseMatrixUnsavedChanges.value) {
      activeTab.value = 'courseMatrix'
    }
    return
  }

  // 2. 数据完整性校验
  const loadingInstance = ElMessage.info({
    message: '正在校验数据完整性...',
    duration: 0
  })

  let errors = []
  try {
    const result = await validateProgramData(programId.value)
    errors = result.errors || []
  } catch (e) {
    ElMessage.error('校验数据失败，请稍后重试')
    loadingInstance.close()
    return
  }
  loadingInstance.close()

  // 3. 如果有错误，显示错误信息
  if (errors.length > 0) {
    // 使用 <br> 实现换行，每条错误独立一行
    const errorItems = errors.map((err, idx) => `  ${idx + 1}. ${err.message}`).join('<br>')

    await ElMessageBox.alert(
        `以下数据项不完整，无法发布：<br><br>${errorItems}<br><br>💡 提示：支撑矩阵是达成度计算的基础数据，必须配置完整后才能发布。`,
        '数据不完整，无法发布',
        {
          dangerouslyUseHTMLString: true,
          type: 'warning',
          confirmButtonText: '知道了'
        }
    )
    return
  }

  // 4. 二次确认
  try {
    await ElMessageBox.confirm(
        '确认发布该培养方案吗？<br><br>⚠️ <b>发布后将变为只读状态，不可再编辑。</b>',
        '发布确认',
        {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '确认发布',
          cancelButtonText: '取消',
          type: 'info'
        }
    )
  } catch {
    return
  }

  // 5. 执行发布
  try {
    await request.put(`/admin/program/publish/${programId.value}`)
    ElMessage.success('发布成功')
    await loadProgramInfo()
  } catch (e) {
    ElMessage.error(e.message || '发布失败，请稍后重试')
  }
}

// ============ 取消发布 ============
const handleUnpublish = async () => {
  try {
    await ElMessageBox.confirm(
        '确认取消发布该培养方案吗？<br><br>取消后将变为草稿状态，可以重新编辑。',
        '取消发布确认',
        {
          dangerouslyUseHTMLString: true,
          confirmButtonText: '确认取消',
          cancelButtonText: '取消',
          type: 'warning'
        }
    )
  } catch {
    return
  }

  try {
    await request.put(`/admin/program/unpublish/${programId.value}`)
    ElMessage.success('已取消发布')
    await loadProgramInfo()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

// ============ 监听路由参数变化 ============
watch(() => route.params.id, (newId) => {
  if (newId) {
    programId.value = newId
    loadProgramInfo()
  }
}, { immediate: true })

// ============ 页面离开前提示 ============
const handleBeforeUnload = (e) => {
  if (hasAnyUnsavedChanges()) {
    e.preventDefault()
    e.returnValue = '有矩阵数据未保存的更改，确定要离开吗？'
  }
}

// ============ 生命周期 ============
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