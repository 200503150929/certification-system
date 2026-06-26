<template>
  <div class="teacher-course-manage-container" v-loading="pageLoading">
    <el-card>
      <template #header>
        <div class="course-header">
          <div>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/my-courses' }">课程管理</el-breadcrumb-item>
              <el-breadcrumb-item>{{ courseInfo.courseName || '加载中...' }}</el-breadcrumb-item>
            </el-breadcrumb>
            <h2 class="course-title">{{ courseInfo.courseName }}</h2>
            <div class="course-meta">
              <span>课程代码：{{ courseInfo.courseCode }}</span>
              <span>学分：{{ courseInfo.credits }}</span>
              <span>授课教师：{{ courseInfo.teacherName }}</span>
              <span>学年：{{ courseInfo.academicYear }} {{ courseInfo.semester }}</span>
            </div>
          </div>
          <el-tag type="primary" size="large">
            {{ courseInfo.academicYear || '' }}
          </el-tag>
        </div>
      </template>

      <div class="summary-grid">
        <div class="summary-item">
          <p class="summary-label">学生人数</p>
          <p class="summary-value">{{ studentCount }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">课程目标数</p>
          <p class="summary-value">{{ objectiveCount }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">考核环节数</p>
          <p class="summary-value">{{ assessmentCount }}</p>
        </div>
        <div class="summary-item">
          <p class="summary-label">资源数</p>
          <p class="summary-value">{{ resourceCount }}</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="学生名单" name="students">
          <div class="toolbar">
            <el-input v-model="studentKeyword" placeholder="搜索姓名/学号" style="width: 260px" clearable />
            <el-button type="primary" :icon="Download">导出名单</el-button>
          </div>
          <el-table :data="filteredStudents" border style="width: 100%" v-loading="studentsLoading">
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="120" />
            <el-table-column label="成绩" min-width="200">
              <template #default="scope">
                <span v-for="(g, idx) in scope.row.grades" :key="idx">
                  <el-tag size="small" style="margin-right: 4px">{{ g.assessmentName }}: {{ g.score }}</el-tag>
                </span>
                <span v-if="!scope.row.grades || scope.row.grades.length === 0" style="color: #999">未录入</span>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="成绩录入" name="grades">
          <div class="toolbar">
            <el-select v-model="selectedAssessmentId" placeholder="选择考核环节" style="width: 240px" @change="fetchGrades">
              <el-option v-for="a in assessments" :key="a.id" :label="a.name" :value="a.id" />
            </el-select>
            <el-button :icon="Download" @click="exportGrades">导出成绩</el-button>
          </div>
          <el-table :data="gradeTableData" border style="width: 100%" v-loading="gradesLoading">
            <el-table-column prop="studentId" label="学号" width="120" />
            <el-table-column prop="studentName" label="姓名" width="120" />
            <el-table-column label="成绩" width="200">
              <template #default="scope">
                <el-input-number v-model="scope.row.score" :min="0" :max="100" size="small" :precision="1" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button link type="primary" @click="saveGrade(scope.row)">保存</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="!selectedAssessmentId" style="text-align: center; color: #999; padding: 40px">
            请先选择考核环节
          </div>
          <div v-if="!assessments.length" style="text-align: center; color: #999; padding: 40px">
            暂无考核环节，请在"课程目标"中先设置考核环节
          </div>
        </el-tab-pane>

        <el-tab-pane label="课程目标" name="objectives">
          <div class="toolbar">
            <el-button type="primary" :icon="Plus" @click="openObjectiveDialog()">新增课程目标</el-button>
          </div>
          <el-table :data="objectives" border style="width: 100%" v-loading="objectivesLoading">
            <el-table-column prop="code" label="编号" width="100" />
            <el-table-column prop="description" label="课程目标描述" min-width="300" />
            <el-table-column prop="weight" label="权重" width="100" />
            <el-table-column label="操作" width="160">
              <template #default="scope">
                <el-button link type="primary" @click="openObjectiveDialog(scope.row)">编辑</el-button>
                <el-button link type="danger" @click="deleteObjective(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="教学资源" name="resources">
          <div class="toolbar">
            <el-button type="primary" :icon="Upload" @click="openResourceUpload">上传资源</el-button>
          </div>
          <el-table :data="resources" border style="width: 100%" v-loading="resourcesLoading">
            <el-table-column prop="fileName" label="文件名称" min-width="200" />
            <el-table-column prop="resourceType" label="类型" width="120">
              <template #default="scope">
                <el-tag size="small">{{ scope.row.resourceType || '-' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="createdAt" label="上传时间" width="170" />
            <el-table-column label="操作" width="180">
              <template #default="scope">
                <el-button link type="primary" @click="downloadResource(scope.row)">下载</el-button>
                <el-button link type="danger" @click="deleteResource(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 课程目标弹窗 -->
    <el-dialog v-model="objectiveDialogVisible" :title="objectiveDialogTitle" width="500px">
      <el-form ref="objFormRef" :model="objForm" label-width="100px">
        <el-form-item label="编号" required>
          <el-input v-model="objForm.code" placeholder="如 OBJ1" />
        </el-form-item>
        <el-form-item label="描述" required>
          <el-input v-model="objForm.description" type="textarea" :rows="3" placeholder="课程目标描述" />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="objForm.weight" :min="0" :max="1" :step="0.05" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="objectiveDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitObjective" :loading="objSubmitting">保存</el-button>
      </template>
    </el-dialog>

    <!-- 资源上传弹窗 -->
    <el-dialog v-model="resourceDialogVisible" title="上传教学资源" width="450px">
      <el-upload
        ref="uploadRef"
        drag
        :action="`/api/teacher/resources/upload/${offeringId}`"
        :headers="uploadHeaders"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
        accept=".pdf,.ppt,.pptx,.doc,.docx,.xls,.xlsx"
      >
        <el-icon><UploadFilled /></el-icon>
        <div class="el-upload__text">将文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 PDF/PPT/DOC/XLS 格式，最大 50MB</div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Upload, UploadFilled } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const offeringId = computed(() => route.params.id)

// ============ 状态 ============
const activeTab = ref('students')
const pageLoading = ref(false)
const studentKeyword = ref('')

// ============ 课程信息 ============
const courseInfo = ref({
  courseName: '',
  courseCode: '',
  credits: '',
  teacherName: '',
  academicYear: '',
  semester: ''
})

// ============ 学生 ============
const studentsLoading = ref(false)
const rawStudents = ref([])

const studentCount = computed(() => rawStudents.value.length)
const filteredStudents = computed(() => {
  const kw = studentKeyword.value.trim()
  if (!kw) return rawStudents.value
  return rawStudents.value.filter(s =>
    String(s.studentId).includes(kw) || (s.studentName && s.studentName.includes(kw))
  )
})

// ============ 成绩 ============
const gradesLoading = ref(false)
const assessments = ref([])
const selectedAssessmentId = ref(null)
const gradeTableData = ref([])

const assessmentCount = computed(() => assessments.value.length)

// ============ 课程目标 ============
const objectives = ref([])
const objectivesLoading = ref(false)
const objectiveDialogVisible = ref(false)
const objFormRef = ref(null)
const objSubmitting = ref(false)
const editingObjectiveId = ref(null)
const objForm = ref({ code: '', description: '', weight: 0.5 })

const objectiveCount = computed(() => objectives.value.length)
const objectiveDialogTitle = computed(() => editingObjectiveId.value ? '编辑课程目标' : '新增课程目标')

// ============ 资源 ============
const resources = ref([])
const resourcesLoading = ref(false)
const resourceDialogVisible = ref(false)
const uploadRef = ref(null)

const resourceCount = computed(() => resources.value.length)

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

// ============ 加载课程信息 ============
const fetchCourseInfo = async () => {
  pageLoading.value = true
  try {
    const res = await request.get(`/teacher/offering/detail/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      courseInfo.value = res.data
    }
  } catch (e) {
    // 拦截器已处理
  } finally {
    pageLoading.value = false
  }
}

// ============ 加载学生（通过成绩 API） ============
const fetchStudents = async () => {
  studentsLoading.value = true
  try {
    const res = await request.get('/teacher/grades/list', {
      params: { offeringId: offeringId.value, pageSize: 999 }
    })
    if (res.status === 'success' && res.data && res.data.list) {
      // 按 studentId 分组
      const studentMap = new Map()
      res.data.list.forEach(g => {
        if (!studentMap.has(g.studentId)) {
          studentMap.set(g.studentId, {
            studentId: g.studentId,
            studentName: g.studentName,
            grades: []
          })
        }
        studentMap.get(g.studentId).grades.push({
          assessmentId: g.assessmentId,
          assessmentName: g.assessmentName,
          score: g.score,
          gradeId: g.id
        })
      })
      rawStudents.value = Array.from(studentMap.values())
    }
  } catch (e) {
    rawStudents.value = []
  } finally {
    studentsLoading.value = false
  }
}

// ============ 考核环节 ============
const fetchAssessments = async () => {
  try {
    const res = await request.get(`/teacher/assessments/offering/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      assessments.value = res.data
    }
  } catch (e) {
    assessments.value = []
  }
}

// ============ 成绩管理 ============
const fetchGrades = async () => {
  if (!selectedAssessmentId.value) return
  gradesLoading.value = true
  try {
    const res = await request.get('/teacher/grades/list', {
      params: { offeringId: offeringId.value, pageSize: 999 }
    })
    if (res.status === 'success' && res.data && res.data.list) {
      // 筛选当前考核环节的成绩
      const filtered = res.data.list.filter(g => g.assessmentId === selectedAssessmentId.value)
      gradeTableData.value = filtered.map(g => ({
        id: g.id,
        studentId: g.studentId,
        studentName: g.studentName,
        score: g.score,
        assessmentId: g.assessmentId
      }))
    }
  } catch (e) {
    gradeTableData.value = []
  } finally {
    gradesLoading.value = false
  }
}

const saveGrade = async (row) => {
  try {
    await request.post('/teacher/grades/save', {
      id: row.id,
      assessmentId: row.assessmentId || selectedAssessmentId.value,
      studentId: row.studentId,
      score: row.score
    })
    ElMessage.success('成绩已保存')
  } catch (e) {
    // 已提示
  }
}

const exportGrades = () => {
  ElMessage.info('导出成绩功能待实现')
}

// ============ 课程目标 ============
const fetchObjectives = async () => {
  objectivesLoading.value = true
  try {
    const res = await request.get(`/teacher/offering/${offeringId.value}/objectives`)
    if (res.status === 'success' && res.data) {
      objectives.value = res.data
    }
  } catch (e) {
    objectives.value = []
  } finally {
    objectivesLoading.value = false
  }
}

const openObjectiveDialog = (row) => {
  if (row) {
    editingObjectiveId.value = row.id
    objForm.value = { code: row.code || '', description: row.description || '', weight: row.weight || 0.5 }
  } else {
    editingObjectiveId.value = null
    objForm.value = { code: '', description: '', weight: 0.5 }
  }
  objectiveDialogVisible.value = true
}

const submitObjective = async () => {
  objSubmitting.value = true
  try {
    const body = { ...objForm.value }
    if (editingObjectiveId.value) {
      await request.put(`/teacher/objectives/${editingObjectiveId.value}`, body)
      ElMessage.success('课程目标已更新')
    } else {
      await request.post(`/teacher/offering/${offeringId.value}/objectives`, body)
      ElMessage.success('课程目标已添加')
    }
    objectiveDialogVisible.value = false
    fetchObjectives()
  } catch (e) {
    // 已提示
  } finally {
    objSubmitting.value = false
  }
}

const deleteObjective = (row) => {
  ElMessageBox.confirm(`确定删除课程目标 "${row.code}" 吗？`, '确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.delete(`/teacher/objectives/${row.id}`)
      ElMessage.success('已删除')
      fetchObjectives()
    } catch (e) {
      // 已提示
    }
  }).catch(() => {})
}

// ============ 资源 ============
const fetchResources = async () => {
  resourcesLoading.value = true
  try {
    const res = await request.get(`/teacher/resources/offering/${offeringId.value}`)
    if (res.status === 'success' && res.data) {
      resources.value = res.data
    }
  } catch (e) {
    resources.value = []
  } finally {
    resourcesLoading.value = false
  }
}

const openResourceUpload = () => {
  resourceDialogVisible.value = true
}

const handleUploadSuccess = (response) => {
  if (response.status === 'success') {
    ElMessage.success('上传成功')
    resourceDialogVisible.value = false
    fetchResources()
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('上传失败')
}

const downloadResource = (row) => {
  const token = localStorage.getItem('token')
  const url = `/api/teacher/resources/download/${row.id}`
  const link = document.createElement('a')
  link.href = url
  // Use fetch for auth
  fetch(url, { headers: { Authorization: `Bearer ${token}` } })
    .then(res => res.blob())
    .then(blob => {
      const blobUrl = window.URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = blobUrl
      a.download = row.fileName || 'download'
      a.click()
      window.URL.revokeObjectURL(blobUrl)
    })
    .catch(() => ElMessage.error('下载失败'))
}

const deleteResource = (row) => {
  ElMessageBox.confirm(`确定删除资源 "${row.fileName}" 吗？`, '确认', {
    type: 'warning',
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.delete(`/teacher/resources/${row.id}`)
      ElMessage.success('已删除')
      fetchResources()
    } catch (e) {
      // 已提示
    }
  }).catch(() => {})
}

// ============ Tab 切换 ============
const handleTabChange = (tab) => {
  switch (tab) {
    case 'students': fetchStudents(); break
    case 'grades': fetchAssessments(); break
    case 'objectives': fetchObjectives(); break
    case 'resources': fetchResources(); break
  }
}

// ============ 初始化 ============
onMounted(() => {
  fetchCourseInfo()
  fetchStudents()
})
</script>

<style scoped>
.teacher-course-manage-container { padding: 20px; }

.course-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.course-title {
  margin: 12px 0 8px;
  font-size: 24px;
}

.course-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #666;
  font-size: 14px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(140px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.summary-item {
  padding: 16px 20px;
  background: #f7f8fa;
  border: 1px solid #ebeef5;
  border-radius: 6px;
}

.summary-label {
  margin: 0 0 8px;
  color: #909399;
  font-size: 14px;
}

.summary-value {
  margin: 0;
  color: #303133;
  font-size: 24px;
  font-weight: 700;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
