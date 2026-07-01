<template>
  <div class="courses-tab">
    <div class="toolbar">
      <div class="toolbar-left">
        <el-button type="primary" :icon="Plus" @click="openAddDialog" :disabled="disabled">
          添加课程
        </el-button>
      </div>
      <div class="toolbar-right">
        <el-input
            v-model="searchKeyword"
            placeholder="搜索课程名称/代码"
            clearable
            style="width: 220px"
            @keyup.enter="handleSearch"
        />
        <el-button @click="handleSearch">搜索</el-button>
        <el-button :icon="Download" v-if="!disabled" @click="handleExport" :loading="exporting">
          导出
        </el-button>
      </div>
    </div>

    <el-table :data="courseList" border v-loading="loading" style="width: 100%">
      <el-table-column prop="code" label="课程代码" width="130" />
      <el-table-column prop="name" label="课程名称" min-width="180" />
      <el-table-column label="课程类型" width="130">
        <template #default="scope">
          <el-tag>{{ scope.row.courseType || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="学分" width="80" align="center">
        <template #default="scope">
          {{ scope.row.credits }}
        </template>
      </el-table-column>
      <el-table-column label="总学时" width="80" align="center">
        <template #default="scope">
          {{ scope.row.totalHours || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="理论学时" width="90" align="center">
        <template #default="scope">
          {{ scope.row.theoryHours || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="实验学时" width="90" align="center">
        <template #default="scope">
          {{ scope.row.labHours || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="必修" width="70" align="center">
        <template #default="scope">
          <el-tag :type="scope.row.isRequired ? 'success' : 'info'" size="small">
            {{ scope.row.isRequired ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开课学期" width="120">
        <template #default="scope">
          {{ scope.row.semester || '-' }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="scope">
          <el-button link type="primary" :icon="Edit" @click="openEditDialog(scope.row)" :disabled="disabled">
            编辑
          </el-button>
          <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)" :disabled="disabled">
            删除
          </el-button>
          <!-- 新增：开课管理按钮 -->
          <el-button link type="primary" @click="openOfferingManagement(scope.row)">
            开课管理
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-container">
      <el-pagination
          background
          layout="total, prev, pager, next, sizes"
          :total="total"
          :page-size="pageSize"
          :current-page="pageNum"
          :page-sizes="[5, 10, 20, 50]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
      />
    </div>

    <!-- 新增/编辑课程弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="560px"
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
        <el-button type="primary" @click="submitForm" :loading="submitting">
          {{ isEdit ? '保存修改' : '确认添加' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 开课管理弹窗 -->
    <OfferingManagement
        v-model:visible="offeringDialogVisible"
        :course-id="selectedCourseId"
        :course-name="selectedCourseName"
        @success="handleOfferingSuccess"
    />
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, Download } from '@element-plus/icons-vue'
import request from '@/api/request'
import OfferingManagement from './OfferingManagement.vue'

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

const emit = defineEmits(['change'])

// ============ 状态 ============
const loading = ref(false)
const exporting = ref(false)
const submitting = ref(false)
const courseList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchKeyword = ref('')

// ============ 弹窗 ============
const dialogVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
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

const dialogTitle = computed(() => isEdit.value ? '编辑课程' : '新增课程')

// ============ 开课管理 ============
const offeringDialogVisible = ref(false)
const selectedCourseId = ref(null)
const selectedCourseName = ref('')

// ============ 加载课程列表 ============
const fetchCourses = async () => {
  if (!props.programId) return
  loading.value = true
  try {
    const params = {
      programId: props.programId,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) {
      params.keyword = searchKeyword.value
    }
    const res = await request.get('/admin/course/list', { params })
    if (res.status === 'success' && res.data) {
      courseList.value = res.data.list || []
      total.value = res.data.total || 0
      pageNum.value = res.data.pageNum || 1
      pageSize.value = res.data.pageSize || 10
    }
  } catch (e) {
    // 拦截器已处理
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  pageNum.value = 1
  fetchCourses()
}

const handlePageChange = (page) => {
  pageNum.value = page
  fetchCourses()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  fetchCourses()
}

// ============ 导出 ============
const handleExport = async () => {
  exporting.value = true
  try {
    const token = localStorage.getItem('token')
    const url = `/api/admin/course/export`
    const response = await fetch(url, {
      headers: { Authorization: `Bearer ${token}` }
    })
    if (!response.ok) {
      throw new Error('导出失败')
    }
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

// ============ 增删改 ============
const openAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  form.code = row.code || ''
  form.name = row.name || ''
  form.courseType = row.courseType || ''
  form.credits = row.credits || 3
  form.totalHours = row.totalHours || 48
  form.theoryHours = row.theoryHours || 32
  form.labHours = row.labHours || 16
  form.semester = row.semester || ''
  form.isRequired = row.isRequired !== false
  dialogVisible.value = true
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const body = {
        ...form,
        programId: Number(props.programId)
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
      emit('change')
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
      emit('change')
    } catch (e) {
      // 拦截器已处理
    }
  }).catch(() => {})
}

const resetForm = () => {
  nextTick(() => {
    formRef.value?.resetFields()
  })
  form.code = ''
  form.name = ''
  form.courseType = ''
  form.credits = 3
  form.totalHours = 48
  form.theoryHours = 32
  form.labHours = 16
  form.semester = ''
  form.isRequired = true
  isEdit.value = false
  editingId.value = null
}

// ============ 开课管理 ============
const openOfferingManagement = (row) => {
  selectedCourseId.value = row.id
  selectedCourseName.value = row.name
  offeringDialogVisible.value = true
}

const handleOfferingSuccess = () => {
  // 开课记录变更后刷新课程列表（显示最新的开课状态）
  fetchCourses()
}

// ============ 监听 ============
watch(() => props.programId, () => {
  pageNum.value = 1
  searchKeyword.value = ''
  fetchCourses()
}, { immediate: true })
</script>

<style scoped>
.courses-tab {
  padding: 10px 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
</style>