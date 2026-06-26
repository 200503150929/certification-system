<template>
  <div class="user-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户账号管理</span>
          <div>
            <el-input v-model="searchKeyword" placeholder="搜索工号/姓名" class="search-input" @keyup.enter="handleSearch" clearable />
            <el-select v-model="selectedRole" placeholder="角色类别" clearable class="role-select" @change="handleSearch">
              <el-option label="管理员" value="admin" />
              <el-option label="教师" value="teacher" />
              <el-option label="学生" value="student" />
            </el-select>
            <el-select v-model="selectedStatus" placeholder="状态" clearable class="role-select" @change="handleSearch">
              <el-option label="正常" :value="1" />
              <el-option label="已禁用" :value="0" />
            </el-select>
            <el-button type="primary" :icon="Upload" @click="handleImport">批量导入</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增用户</el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%" v-loading="loading">
        <el-table-column prop="username" label="工号" width="120"></el-table-column>
        <el-table-column prop="name" label="姓名" width="100"></el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">{{ getRoleLabel(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="院系" min-width="150"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="130"></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160"></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">{{ scope.row.status === 1 ? '正常' : '已禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button link type="primary" :icon="Refresh" @click="handleResetPassword(scope.row)">重置密码</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
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
          :page-sizes="[10, 20, 50]"
          @size-change="handleSizeChange"
          @current-change="handlePageChange"
        ></el-pagination>
      </div>
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="addDialogVisible"
      :title="dialogTitle"
      width="520px"
      @close="resetAddForm"
    >
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" placeholder="3-12位字母、数字或下划线" :disabled="isEdit" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="addForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="admin" />
            <el-option label="教师" value="teacher" />
            <el-option label="学生" value="student" />
          </el-select>
        </el-form-item>
        <el-form-item label="院系" prop="department">
          <el-input v-model="addForm.department" placeholder="请输入院系" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="addForm.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="addForm.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">已禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm" :loading="submitting">{{ submitButtonText }}</el-button>
      </template>
    </el-dialog>

    <!-- 批量导入弹窗 -->
    <el-dialog v-model="importVisible" title="批量导入用户" width="450px">
      <el-upload
        ref="uploadRef"
        drag
        :action="`/api/admin/users/import`"
        :headers="uploadHeaders"
        :on-success="handleImportSuccess"
        :on-error="handleImportError"
        accept=".xlsx,.xls"
      >
        <el-icon><UploadFilled /></el-icon>
        <div class="el-upload__text">将 Excel 文件拖到此处，或<em>点击上传</em></div>
        <template #tip>
          <div class="el-upload__tip">支持 .xlsx 格式，需包含：用户名、姓名、角色、电话、邮箱、院系</div>
        </template>
      </el-upload>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { Plus, Upload, Edit, Delete, Refresh, UploadFilled } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/api/request'

const route = useRoute()

// ============ 搜索筛选 ============
const searchKeyword = ref('')
const selectedRole = ref('')
const selectedStatus = ref(null)
const loading = ref(false)

// ============ 分页 ============
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ============ 表格数据 ============
const tableData = ref([])

// ============ 弹窗 ============
const addDialogVisible = ref(false)
const importVisible = ref(false)
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const addFormRef = ref(null)
const uploadRef = ref(null)

const addForm = reactive({
  username: '',
  name: '',
  role: '',
  department: '',
  phone: '',
  email: '',
  status: 1
})

const addRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{3,12}$/, message: '3-12位字母、数字或下划线', trigger: 'blur' }
  ],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

// ============ 计算属性 ============
const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')
const submitButtonText = computed(() => isEdit.value ? '保存修改' : '确认添加')

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

// ============ 工具方法 ============
const getRoleLabel = (role) => {
  const map = { admin: '管理员', teacher: '教师', student: '学生' }
  return map[role] || role
}

const getRoleTagType = (role) => {
  if (role === 'admin') return 'danger'
  if (role === 'teacher') return 'warning'
  if (role === 'student') return 'success'
  return 'info'
}

// ============ 数据加载 ============
const fetchUsers = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: pageNum.value,
      pageSize: pageSize.value
    }
    if (searchKeyword.value) {
      // 同时作为 usernameFuzzy 和 nameFuzzy 搜索
      params.usernameFuzzy = searchKeyword.value
      params.nameFuzzy = searchKeyword.value
    }
    if (selectedRole.value) params.role = selectedRole.value
    if (selectedStatus.value !== null && selectedStatus.value !== '') params.status = selectedStatus.value

    const res = await request.get('/admin/users/list', { params })
    if (res.status === 'success' && res.data) {
      tableData.value = res.data.list || []
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
  fetchUsers()
}

const handlePageChange = (page) => {
  pageNum.value = page
  fetchUsers()
}

const handleSizeChange = (size) => {
  pageSize.value = size
  pageNum.value = 1
  fetchUsers()
}

// ============ 增删改 ============
const openAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  addDialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  addForm.username = row.username
  addForm.name = row.name
  addForm.role = row.role
  addForm.department = row.department || ''
  addForm.phone = row.phone || ''
  addForm.email = row.email || ''
  addForm.status = row.status
  addDialogVisible.value = true
}

const submitAddForm = () => {
  addFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const body = {
        username: addForm.username,
        name: addForm.name,
        role: addForm.role,
        department: addForm.department,
        phone: addForm.phone,
        email: addForm.email,
        status: addForm.status
      }
      if (isEdit.value) {
        body.id = editingId.value
        await request.put('/admin/users/update', body)
        ElMessage.success('用户信息已更新')
      } else {
        await request.post('/admin/users/add', body)
        ElMessage.success('用户添加成功')
      }
      addDialogVisible.value = false
      fetchUsers()
    } catch (e) {
      // 拦截器已处理
    } finally {
      submitting.value = false
    }
  })
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户 ${row.name} 吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.delete(`/admin/users/delete/${row.id}`)
      ElMessage.success('删除成功')
      fetchUsers()
    } catch (e) {
      // 拦截器已处理
    }
  }).catch(() => {})
}

const handleResetPassword = (row) => {
  ElMessageBox.confirm(`确定重置用户 ${row.name} 的密码为 123456 吗？`, '重置密码确认', {
    type: 'warning',
    confirmButtonText: '确定重置',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.put(`/admin/users/reset-password/${row.id}`)
      ElMessage.success('密码已重置为 123456')
    } catch (e) {
      // 拦截器已处理
    }
  }).catch(() => {})
}

const resetAddForm = () => {
  addFormRef.value?.resetFields()
  addForm.username = ''
  addForm.name = ''
  addForm.role = ''
  addForm.department = ''
  addForm.phone = ''
  addForm.email = ''
  addForm.status = 1
  isEdit.value = false
  editingId.value = null
}

// ============ 批量导入 ============
const handleImport = () => {
  importVisible.value = true
}

const handleImportSuccess = (response) => {
  if (response.status === 'success') {
    ElMessage.success(response.data || '导入成功')
    importVisible.value = false
    fetchUsers()
  } else {
    ElMessage.error(response.message || '导入失败')
  }
}

const handleImportError = () => {
  ElMessage.error('导入失败，请检查文件格式')
}

// ============ URL 角色参数过滤 ============
const applyRoleQuery = () => {
  const role = route.query.role
  const validRoles = ['admin', 'teacher', 'student']
  selectedRole.value = validRoles.includes(role) ? role : ''
  fetchUsers()
}

watch(() => route.query.role, applyRoleQuery)

onMounted(() => {
  applyRoleQuery()
})
</script>

<style scoped>
.user-management-container {
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.search-input {
  width: 200px;
  margin-right: 10px;
}
.role-select {
  width: 120px;
  margin-right: 10px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 去掉所有竖向边框 */
:deep(.el-table__body-wrapper .el-table__cell),
:deep(.el-table__header-wrapper .el-table__cell),
:deep(.el-table__fixed-right .el-table__cell) {
  border-right: none !important;
  border-left: none !important;
}

:deep(.el-table__body-wrapper .el-table__row) {
  border-bottom: 1px solid #ebeef5 !important;
}

:deep(.el-table__header-wrapper) {
  border-bottom: 2px solid #ebeef5 !important;
}

:deep(.el-table__body-wrapper .el-table__row:last-child) {
  border-bottom: none !important;
}

:deep(.el-table) {
  border: none !important;
}
:deep(.el-table__inner-wrapper) {
  border: none !important;
}
</style>
