<template>
  <div class="course-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item>课程管理</el-breadcrumb-item>
              <el-breadcrumb-item>{{ programName }}</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ programName }}</div>
          </div>
          <div class="header-right">
            <el-button :icon="Download" @click="handleExport" :loading="exporting">导出矩阵</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增课程</el-button>
          </div>
        </div>
      </template>

      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="基本信息" name="basic">
          <el-table :data="courseList" border style="width: 100%" v-loading="loading">
            <el-table-column prop="code" label="课程代码" width="120" />
            <el-table-column prop="name" label="课程名称" min-width="180" />
            <el-table-column label="课程类型" width="130">
              <template #default="scope">
                <el-tag>{{ getCourseTypeLabel(scope.row.courseType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="学分" width="80">
              <template #default="scope">
                {{ scope.row.credits }}
              </template>
            </el-table-column>
            <el-table-column label="总学时" width="80">
              <template #default="scope">
                {{ scope.row.totalHours || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="必修" width="70">
              <template #default="scope">
                <el-tag :type="scope.row.isRequired ? 'success' : 'info'">{{ scope.row.isRequired ? '是' : '否' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="开课学期" width="110">
              <template #default="scope">
                {{ scope.row.semester || '-' }}
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="180" fixed="right">
              <template #default="scope">
                <el-button link type="primary" :icon="Edit" @click="openEditDialog(scope.row)">编辑</el-button>
                <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-container">
            <el-pagination
              background
              layout="total, prev, pager, next"
              :total="total"
              :page-size="pageSize"
              :current-page="pageNum"
              @current-change="handlePageChange"
            ></el-pagination>
          </div>
        </el-tab-pane>

        <el-tab-pane label="课程-毕业要求支撑矩阵" name="matrix">
          <div class="matrix-container" v-loading="matrixLoading">
            <el-table :data="matrixCourseList" border style="width: 100%">
              <el-table-column prop="name" label="课程 / 毕业要求" width="220" fixed />
              <el-table-column
                v-for="req in graduationRequirements"
                :key="req.id"
                :label="req.code"
                width="140"
              >
                <template #default="scope">
                  <el-select v-model="scope.row.supports[req.id]" placeholder="无" size="small">
                    <el-option label="强支撑" value="H" />
                    <el-option label="中支撑" value="M" />
                    <el-option label="弱支撑" value="L" />
                    <el-option label="无" value="" />
                  </el-select>
                </template>
              </el-table-column>
            </el-table>

            <div class="matrix-legend">
              <span class="legend-title">支撑强度图例：</span>
              <span class="legend-item strong">■ 强支撑 (H)</span>
              <span class="legend-item medium">■ 中支撑 (M)</span>
              <span class="legend-item weak">■ 弱支撑 (L)</span>
              <span class="legend-item none">■ 无支撑</span>
            </div>

            <div class="matrix-actions">
              <el-button type="primary" @click="handleSaveMatrix" :loading="savingMatrix">保存支撑矩阵</el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 新增/编辑课程弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="520px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="课程代码" prop="code">
          <el-input v-model="form.code" placeholder="如 CS301" />
        </el-form-item>
        <el-form-item label="课程名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程类型" prop="courseType">
          <el-select v-model="form.courseType" placeholder="请选择" style="width: 100%">
            <el-option label="专业必修" value="专业必修" />
            <el-option label="专业选修" value="专业选修" />
            <el-option label="公共基础" value="公共基础" />
            <el-option label="实践环节" value="实践环节" />
          </el-select>
        </el-form-item>
        <el-form-item label="学分" prop="credits">
          <el-input-number v-model="form.credits" :min="0.5" :max="10" :step="0.5" />
        </el-form-item>
        <el-form-item label="总学时" prop="totalHours">
          <el-input-number v-model="form.totalHours" :min="0" :max="500" />
        </el-form-item>
        <el-form-item label="理论学时" prop="theoryHours">
          <el-input-number v-model="form.theoryHours" :min="0" :max="500" />
        </el-form-item>
        <el-form-item label="实验学时" prop="labHours">
          <el-input-number v-model="form.labHours" :min="0" :max="500" />
        </el-form-item>
        <el-form-item label="开课学期" prop="semester">
          <el-input v-model="form.semester" placeholder="如 2024-2025-1" />
        </el-form-item>
        <el-form-item label="是否必修">
          <el-switch v-model="form.isRequired" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">{{ submitButtonText }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Download, Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()

// ============ 状态 ============
const activeTab = ref('basic')
const loading = ref(false)
const matrixLoading = ref(false)
const savingMatrix = ref(false)
const exporting = ref(false)

// ============ 专业信息 ============
const programName = ref('')
const programId = computed(() => route.params.programId)

// ============ 课程列表 ============
const courseList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ============ 矩阵相关 ============
const matrixCourseList = ref([])
const graduationRequirements = ref([])

// ============ 弹窗 ============
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const formRef = ref(null)

const form = ref({
  code: '',
  name: '',
  courseType: '',
  credits: 3,
  totalHours: 48,
  theoryHours: 32,
  labHours: 16,
  semester: '',
  isRequired: true
})

const formRules = {
  code: [{ required: true, message: '请输入课程代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入课程名称', trigger: 'blur' }],
  courseType: [{ required: true, message: '请选择课程类型', trigger: 'change' }],
  credits: [{ required: true, message: '请输入学分', trigger: 'blur' }]
}

// ============ 计算属性 ============
const dialogTitle = computed(() => isEdit.value ? '编辑课程' : '新增课程')
const submitButtonText = computed(() => isEdit.value ? '保存修改' : '确认添加')

// ============ 标签映射 ============
const getCourseTypeLabel = (type) => {
  const map = { '专业必修': '专业必修', '专业选修': '专业选修', '公共基础': '公共基础', '实践环节': '实践环节' }
  return map[type] || type || '-'
}

// ============ 加载专业信息 ============
const fetchProgramInfo = async () => {
  if (!programId.value) return
  try {
    const res = await request.get(`/admin/program/get/${programId.value}`)
    if (res.status === 'success' && res.data) {
      programName.value = res.data.majorName || '未知专业'
    }
  } catch (e) {
    // 忽略
  }
}

// ============ 加载课程列表 ============
const fetchCourses = async () => {
  loading.value = true
  try {
    const res = await request.get('/admin/course/list', {
      params: {
        programId: programId.value,
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (res.status === 'success' && res.data) {
      courseList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  pageNum.value = page
  fetchCourses()
}

// ============ 增删改 ============
const openAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  form.value = {
    code: '',
    name: '',
    courseType: '',
    credits: 3,
    totalHours: 48,
    theoryHours: 32,
    labHours: 16,
    semester: '',
    isRequired: true
  }
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.value = {
    code: row.code || '',
    name: row.name || '',
    courseType: row.courseType || '',
    credits: row.credits || 3,
    totalHours: row.totalHours || 48,
    theoryHours: row.theoryHours || 32,
    labHours: row.labHours || 16,
    semester: row.semester || '',
    isRequired: row.isRequired !== false
  }
  dialogVisible.value = true
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const body = {
        ...form.value,
        programId: Number(programId.value)
      }
      if (isEdit.value) {
        body.id = editingId.value
        await request.put('/admin/course/update', body)
        ElMessage.success('课程已更新')
      } else {
        await request.post('/admin/course/add', body)
        ElMessage.success('课程已添加')
      }
      dialogVisible.value = false
      fetchCourses()
    } catch (e) {
      // 拦截器已处理
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除课程 "${row.name}" 吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.delete(`/admin/course/delete/${row.id}`)
      ElMessage.success('删除成功')
      fetchCourses()
    } catch (e) {
      // 拦截器已处理
    }
  }).catch(() => {})
}

const resetForm = () => {
  formRef.value?.resetFields()
}

// ============ 支撑矩阵 ============
const fetchMatrixData = async () => {
  matrixLoading.value = true
  try {
    // 加载毕业要求（含指标点）
    const reqRes = await request.get(`/admin/program/requirements/detail-list/${programId.value}`)
    if (reqRes.status === 'success' && reqRes.data) {
      graduationRequirements.value = reqRes.data.map(r => ({
        id: r.id,
        code: r.code,
        description: r.description
      }))
    }

    // 加载课程列表（用于矩阵）
    const courseRes = await request.get('/admin/course/list', {
      params: { programId: programId.value, pageSize: 999 }
    })
    if (courseRes.status === 'success' && courseRes.data) {
      const courses = courseRes.data.list || []
      // 为每门课程加载矩阵数据
      const matrixData = []
      for (const course of courses) {
        const detailRes = await request.get(`/admin/course/detail/${course.id}`)
        const supports = {}
        if (detailRes.status === 'success' && detailRes.data && detailRes.data.matrixItems) {
          detailRes.data.matrixItems.forEach(item => {
            if (item.supportLevel) {
              supports[item.indicatorId] = item.supportLevel
            }
          })
        }
        matrixData.push({
          ...course,
          supports
        })
      }
      matrixCourseList.value = matrixData
    }
  } catch (e) {
    // 拦截器已处理
  } finally {
    matrixLoading.value = false
  }
}

const handleSaveMatrix = async () => {
  savingMatrix.value = true
  try {
    // 批量保存每门课程的支撑矩阵
    for (const course of matrixCourseList.value) {
      const matrixItems = []
      for (const [indicatorId, supportLevel] of Object.entries(course.supports)) {
        if (supportLevel) {
          matrixItems.push({
            indicatorId: Number(indicatorId),
            supportLevel
          })
        }
      }
      if (matrixItems.length > 0) {
        await request.post('/admin/course/matrix/batch-save', {
          courseId: course.id,
          items: matrixItems
        })
      }
    }
    ElMessage.success('支撑矩阵已保存')
  } catch (e) {
    // 拦截器已处理
  } finally {
    savingMatrix.value = false
  }
}

const handleTabChange = (tab) => {
  if (tab === 'matrix') {
    fetchMatrixData()
  }
}

// ============ 导出 ============
const handleExport = async () => {
  exporting.value = true
  try {
    const token = localStorage.getItem('token')
    const url = `/api/admin/course/export`
    const a = document.createElement('a')
    a.href = url
    a.download = ''
    // 通过 fetch + blob 方式下载以携带 Authorization
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` }
    })
    const blob = await response.blob()
    const blobUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = blobUrl
    link.download = '课程体系数据.xlsx'
    link.click()
    window.URL.revokeObjectURL(blobUrl)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}

// ============ 监听路由 ============
watch(() => route.params.programId, (newVal) => {
  if (newVal) {
    fetchProgramInfo()
    fetchCourses()
    matrixCourseList.value = []
  }
}, { immediate: true })

onMounted(() => {
  if (programId.value) {
    fetchProgramInfo()
    fetchCourses()
  }
})
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

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
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
.matrix-legend .medium { color: #e6a23c; }
.matrix-legend .weak { color: #67c23a; }
.matrix-legend .none { color: #c0c4cc; }

.matrix-actions {
  margin-top: 20px;
  text-align: right;
}
</style>
