<template>
  <div class="course-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>课程管理</el-breadcrumb-item>
              <el-breadcrumb-item>{{ currentProgram.name }}</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ currentProgram.name }}</div>
          </div>
          <div class="header-right">
            <el-button :icon="Download" @click="handleExport">导出矩阵</el-button>
            <el-button type="primary" @click="handleSave">保存设置</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-table :data="courseList" border style="width: 100%">
            <el-table-column prop="code" label="课程代码" width="120" />
            <el-table-column prop="name" label="课程名称" min-width="180" />
            <el-table-column label="课程类型" width="150">
              <template #default="scope">
                <el-select v-model="scope.row.type" size="small">
                  <el-option label="专业必修" value="专业必修" />
                  <el-option label="专业选修" value="专业选修" />
                  <el-option label="公共基础" value="公共基础" />
                  <el-option label="实践环节" value="实践环节" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="学分" width="120">
              <template #default="scope">
                <el-input-number
                  v-model="scope.row.credits"
                  :min="0"
                  :max="10"
                  :step="0.5"
                  size="small"
                  controls-position="right"
                />
              </template>
            </el-table-column>
            <el-table-column label="授课老师" width="160">
              <template #default="scope">
                <el-input v-model="scope.row.teacher" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="开课学期" width="170">
              <template #default="scope">
                <el-select v-model="scope.row.semester" size="small">
                  <el-option label="2024-2025第一学期" value="2024-1" />
                  <el-option label="2024-2025第二学期" value="2024-2" />
                  <el-option label="2025-2026第一学期" value="2025-1" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="考核方式" width="140">
              <template #default="scope">
                <el-select v-model="scope.row.assessment" size="small">
                  <el-option label="考试" value="考试" />
                  <el-option label="考查" value="考查" />
                  <el-option label="项目制" value="项目制" />
                </el-select>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="课程-毕业要求支撑矩阵" name="matrix">
          <div class="matrix-container">
            <el-table :data="courseList" border style="width: 100%">
              <el-table-column prop="name" label="课程 / 毕业要求" width="220" fixed />
              <el-table-column
                v-for="item in requirementList"
                :key="item.id"
                :label="item.code"
                width="120"
              >
                <template #default="scope">
                  <el-select v-model="scope.row.supports[item.id]" placeholder="无" size="small">
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
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Download } from '@element-plus/icons-vue'

const route = useRoute()
const activeTab = ref('basic')

const programList = [
  { id: '1', name: '计算机科学与技术' },
  { id: '2', name: '软件工程' },
  { id: '3', name: '数据科学与大数据技术' },
  { id: '4', name: '人工智能' }
]

const requirementList = ref([
  { id: 'req1', code: '1' },
  { id: 'req2', code: '2' },
  { id: 'req3', code: '3' },
  { id: 'req4', code: '4' },
  { id: 'req5', code: '5' },
  { id: 'req6', code: '6' }
])

const courseTemplates = {
  '1': [
    createCourse('CS301', '数据结构与算法', '专业必修', 3, '王教授', '2024-1', '考试', { req1: '强', req2: '强', req3: '弱' }),
    createCourse('CS302', '操作系统', '专业必修', 3, '李教授', '2024-1', '考试', { req1: '强', req2: '弱', req4: '强' }),
    createCourse('CS303', '数据库系统', '专业必修', 2, '张教授', '2024-2', '考查', { req1: '弱', req3: '强', req5: '弱' })
  ],
  '2': [
    createCourse('SE201', '软件工程导论', '专业必修', 3, '刘教授', '2024-1', '项目制', { req2: '强', req3: '强', req6: '弱' }),
    createCourse('SE302', '软件测试技术', '专业选修', 2, '陈教授', '2024-2', '考查', { req3: '弱', req4: '强', req5: '弱' })
  ],
  '3': [
    createCourse('DS201', '数据分析基础', '专业必修', 3, '赵教授', '2024-1', '考试', { req1: '强', req2: '强', req5: '弱' }),
    createCourse('DS305', '机器学习', '专业必修', 3, '周教授', '2024-2', '项目制', { req2: '强', req3: '强', req4: '弱' })
  ],
  '4': [
    createCourse('AI201', '人工智能导论', '专业必修', 3, '孙教授', '2024-1', '考试', { req1: '强', req2: '弱', req3: '强' }),
    createCourse('AI302', '深度学习实践', '实践环节', 2, '吴教授', '2024-2', '项目制', { req3: '强', req4: '强', req6: '弱' })
  ]
}

const currentProgram = computed(() => {
  return programList.find(item => item.id === route.params.programId) || programList[0]
})

const courseList = ref([])

function createCourse(code, name, type, credits, teacher, semester, assessment, supports) {
  return {
    code,
    name,
    type,
    credits,
    teacher,
    semester,
    assessment,
    supports: {
      req1: '',
      req2: '',
      req3: '',
      req4: '',
      req5: '',
      req6: '',
      ...supports
    }
  }
}

const loadCourses = () => {
  courseList.value = (courseTemplates[currentProgram.value.id] || courseTemplates['1']).map(item => ({
    ...item,
    supports: { ...item.supports }
  }))
}

const handleSave = () => {
  ElMessage.success('课程信息与支撑矩阵已保存')
}

const handleExport = () => {
  ElMessage.success('课程-毕业要求支撑矩阵已导出')
}

watch(() => route.params.programId, loadCourses, { immediate: true })
</script>

<style scoped>
.course-management-container { padding: 20px; }

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

.matrix-container { overflow-x: auto; }

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
