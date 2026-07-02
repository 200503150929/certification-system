<template>
  <div class="user-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户账号管理</span>
          <div class="header-actions">
            <div class="search-group">
              <el-input
                  v-model="searchUsername"
                  placeholder="搜索工号/学号"
                  class="search-input"
                  clearable
                  @keyup.enter="handleSearch"
              />
              <el-input
                  v-model="searchName"
                  placeholder="搜索姓名"
                  class="search-input"
                  clearable
                  @keyup.enter="handleSearch"
              />
              <el-select v-model="selectedRole" placeholder="角色类别" clearable class="role-select" @change="handleSearch">
                <el-option label="管理员" value="admin" />
                <el-option label="教师" value="teacher" />
                <el-option label="学生" value="student" />
              </el-select>
              <el-select v-model="selectedStatus" placeholder="状态" clearable class="role-select" @change="handleSearch">
                <el-option label="正常" :value="1" />
                <el-option label="已禁用" :value="0" />
              </el-select>
            </div>
            <div class="action-group">
              <el-button v-permission="'user:import'" type="primary" :icon="Upload" @click="openImportDialog">
                批量导入
              </el-button>
              <el-button v-permission="'user:add'" type="primary" :icon="Plus" @click="openAddDialog">
                新增用户
              </el-button>
            </div>
          </div>
        </div>
      </template>

      <el-table :data="tableData" style="width: 100%" v-loading="loading" border>
        <el-table-column prop="username" label="工号/学号" width="120"></el-table-column>
        <el-table-column prop="name" label="姓名" width="100"></el-table-column>
        <el-table-column prop="role" label="角色" width="100">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">{{ getRoleLabel(scope.row.role) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="college" label="学院" min-width="150"></el-table-column>
        <el-table-column prop="major" label="专业" min-width="120"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="130"></el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="160"></el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === 1 ? 'success' : 'danger'">{{ scope.row.status === 1 ? '正常' : '已禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="240" fixed="right">
          <template #default="scope">
            <el-button v-permission="'user:update'" link type="primary" :icon="Edit" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button v-permission="'user:reset-password'" link type="primary" :icon="Refresh" @click="handleResetPassword(scope.row)">重置密码</el-button>
            <el-button v-permission="'user:delete'" link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
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
        <el-form-item label="学院" prop="college">
          <el-input v-model="addForm.college" placeholder="请输入学院" />
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="addForm.major" placeholder="请输入专业" />
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
    <el-dialog v-model="importDialogVisible" title="批量导入用户" width="560px" destroy-on-close @close="handleImportDialogClose">
      <el-tabs v-model="importTab" class="import-tabs">
        <!-- Excel 导入 -->
        <el-tab-pane label="Excel 导入" name="excel">
          <div class="import-excel">
            <el-upload
                ref="uploadRef"
                drag
                :action="`/api/admin/users/import`"
                :headers="uploadHeaders"
                :on-success="handleImportSuccess"
                :on-error="handleImportError"
                :before-upload="beforeUpload"
                accept=".xlsx,.xls"
                :limit="1"
            >
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  <el-icon><InfoFilled /></el-icon>
                  支持 .xlsx / .xls 格式，需包含：用户名、姓名、角色、电话、邮箱、学院、专业
                </div>
              </template>
            </el-upload>
            <div class="import-template">
              <el-button link type="primary" @click="downloadTemplate">
                <el-icon><Download /></el-icon>
                下载导入模板
              </el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 手动输入（新增） -->
        <el-tab-pane label="手动输入" name="manual">
          <div class="import-manual">
            <div class="manual-header">
              <span class="manual-label">请输入用户信息（每行一个，用逗号分隔）：</span>
              <el-button link type="primary" @click="pasteFromClipboard">
                <el-icon><Document /></el-icon>
                从剪贴板粘贴
              </el-button>
            </div>
            <el-input
                v-model="manualUserData"
                type="textarea"
                :rows="10"
                placeholder="格式：用户名,姓名,角色,电话,邮箱,学院,专业&#10;例如：&#10;202401001,张三,student,13800001001,zhangsan@example.com,计算机学院,计算机科学与技术&#10;202401002,李四,student,13800001002,lisi@example.com,计算机学院,软件工程"
                class="manual-textarea"
            />
            <div class="manual-footer">
              <span class="manual-count">
                共 <strong>{{ manualUserCount }}</strong> 条记录待导入
                <el-tag v-if="manualUserCount > 0" size="small" type="success">有效</el-tag>
                <el-tag v-else size="small" type="info">请输入数据</el-tag>
              </span>
              <el-button
                  type="primary"
                  @click="handleManualImport"
                  :loading="manualImporting"
                  :disabled="manualUserCount === 0"
              >
                <el-icon><Check /></el-icon>
                确认导入 ({{ manualUserCount }})
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import {computed, onMounted, reactive, ref, watch, nextTick} from 'vue'
import {useRoute} from 'vue-router'
import {Plus, Upload, Edit, Delete, Refresh, UploadFilled, Download, InfoFilled, Document, Check} from '@element-plus/icons-vue'
import {ElMessage, ElMessageBox} from 'element-plus'
import request from '@/api/request'
import * as XLSX from 'xlsx'

const route = useRoute()

// ============ 搜索筛选 ============
const searchUsername = ref('')
const searchName = ref('')
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
const importDialogVisible = ref(false)
const importTab = ref('excel')
const isEdit = ref(false)
const editingId = ref(null)
const submitting = ref(false)
const addFormRef = ref(null)
const uploadRef = ref(null)
const manualImporting = ref(false)
const manualUserData = ref('')

const addForm = reactive({
  username: '',
  name: '',
  role: '',
  college: '',
  major: '',
  phone: '',
  email: '',
  status: 1
})

const addRules = {
  username: [
    {required: true, message: '请输入用户名', trigger: 'blur'},
    {pattern: /^[a-zA-Z0-9_]{3,12}$/, message: '3-12位字母、数字或下划线', trigger: 'blur'}
  ],
  name: [{required: true, message: '请输入姓名', trigger: 'blur'}],
  role: [{required: true, message: '请选择角色', trigger: 'change'}]
}

// ============ 计算属性 ============
const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户')
const submitButtonText = computed(() => isEdit.value ? '保存修改' : '确认添加')

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

const manualUserCount = computed(() => {
  return manualUserData.value.split('\n')
      .map(s => s.trim())
      .filter(s => s !== '')
      .length
})

// ============ 工具方法 ============
const getRoleLabel = (role) => {
  const map = {admin: '管理员', teacher: '教师', student: '学生'}
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
    if (searchUsername.value) params.usernameFuzzy = searchUsername.value
    if (searchName.value) params.nameFuzzy = searchName.value
    if (selectedRole.value) params.role = selectedRole.value
    if (selectedStatus.value !== null && selectedStatus.value !== '') params.status = selectedStatus.value

    const res = await request.get('/admin/users/list', {params})
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

// ============ 下载模板（XLSX 格式） ============
const downloadTemplate = () => {
  try {
    const data = [
      ['用户名', '姓名', '角色', '电话', '邮箱', '学院', '专业'],
      ['202401001', '张三', 'student', '13800001001', 'zhangsan@example.com', '计算机学院', '计算机科学与技术'],
      ['202401002', '李四', 'student', '13800001002', 'lisi@example.com', '计算机学院', '软件工程'],
      ['teacher01', '王老师', 'teacher', '13800001003', 'wang@example.com', '计算机学院', '计算机科学与技术'],
      ['admin01', '管理员', 'admin', '13800001004', 'admin@example.com', '计算机学院', '计算机科学与技术'],
    ]

    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.aoa_to_sheet(data)

    ws['!cols'] = [
      { wch: 18 },
      { wch: 12 },
      { wch: 12 },
      { wch: 15 },
      { wch: 25 },
      { wch: 15 },
      { wch: 20 },
    ]

    XLSX.utils.book_append_sheet(wb, ws, '用户导入模板')

    const wbout = XLSX.write(wb, {
      bookType: 'xlsx',
      type: 'array'
    })

    const blob = new Blob([wbout], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '用户导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('下载模板失败:', error)
    ElMessage.error('模板下载失败，请检查是否已安装 xlsx 库')
  }
}

// ============ 手动导入 ============
const pasteFromClipboard = async () => {
  try {
    const text = await navigator.clipboard.readText()
    if (text) {
      manualUserData.value = text
      ElMessage.success('已粘贴剪贴板内容')
    }
  } catch (e) {
    ElMessage.warning('无法读取剪贴板，请手动粘贴 (Ctrl+V)')
  }
}

const handleManualImport = async () => {
  const lines = manualUserData.value.split('\n')
      .map(s => s.trim())
      .filter(s => s !== '')

  if (lines.length === 0) {
    ElMessage.warning('请输入用户数据')
    return
  }

  // 解析每行数据
  const users = []
  const errors = []
  lines.forEach((line, index) => {
    const parts = line.split(',').map(s => s.trim())
    if (parts.length < 3) {
      errors.push(`第 ${index + 1} 行：字段不足（需要：用户名,姓名,角色）`)
      return
    }
    users.push({
      username: parts[0],
      name: parts[1],
      role: parts[2],
      phone: parts[3] || '',
      email: parts[4] || '',
      college: parts[5] || '',
      major: parts[6] || '',
      status: 1
    })
  })

  if (errors.length > 0) {
    ElMessage.warning(`以下数据格式错误：\n${errors.join('\n')}`)
    return
  }

  if (users.length === 0) {
    ElMessage.warning('没有有效数据')
    return
  }

  manualImporting.value = true
  let successCount = 0
  let failList = []

  try {
    for (const user of users) {
      try {
        await request.post('/admin/users/add', user)
        successCount++
      } catch (e) {
        failList.push(`${user.username}(${e.message || '导入失败'})`)
      }
    }

    if (successCount > 0) {
      ElMessage.success(`成功导入 ${successCount} 名用户`)
    }
    if (failList.length > 0) {
      ElMessage.warning(`以下 ${failList.length} 个用户导入失败：${failList.join('、')}`)
    }

    importDialogVisible.value = false
    fetchUsers()
  } catch (e) {
    // 拦截器已处理
  } finally {
    manualImporting.value = false
  }
}

// ============ 导入弹窗 ============
const openImportDialog = () => {
  manualUserData.value = ''
  importTab.value = 'excel'
  importDialogVisible.value = true
  nextTick(() => {
    uploadRef.value?.clearFiles()
  })
}

const handleImportDialogClose = () => {
  uploadRef.value?.clearFiles()
  manualUserData.value = ''
}

// ============ Excel 导入 ============
const beforeUpload = (file) => {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
      file.type === 'application/vnd.ms-excel'
  if (!isExcel) {
    ElMessage.error('请上传 Excel 文件 (.xlsx / .xls)')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

const handleImportSuccess = (response) => {
  if (response.status === 'success') {
    ElMessage.success(response.data || '导入成功')
    importDialogVisible.value = false
    fetchUsers()
  } else {
    ElMessage.error(response.message || '导入失败')
  }
}

const handleImportError = () => {
  ElMessage.error('导入失败，请检查文件格式')
}

// ============ 增删改 ============
const openAddDialog = () => {
  isEdit.value = false
  editingId.value = null
  resetAddForm()
  addDialogVisible.value = true
}

const openEditDialog = (row) => {
  isEdit.value = true
  editingId.value = row.id
  addForm.username = row.username
  addForm.name = row.name
  addForm.role = row.role
  addForm.college = row.college || ''
  addForm.major = row.major || ''
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
        college: addForm.college,
        major: addForm.major,
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
      fetchUsers()
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
  addForm.college = ''
  addForm.major = ''
  addForm.phone = ''
  addForm.email = ''
  addForm.status = 1
  isEdit.value = false
  editingId.value = null
}

// ============ URL 角色参数过滤 ============
const applyRoleQuery = () => {
  const role = route.query.role
  const validRoles = ['admin', 'teacher', 'student']
  selectedRole.value = validRoles.includes(role) ? role : ''
  searchUsername.value = ''
  searchName.value = ''
  selectedStatus.value = null
  fetchUsers()
}

watch(() => route.query.role, applyRoleQuery)

onMounted(() => {
  applyRoleQuery()
})
</script>

<style scoped>
.user-management-container {
  padding: 10px;
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

.header-actions {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.search-group {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.action-group {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-input {
  width: 150px;
}

.role-select {
  width: 120px;
}

.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

/* 导入弹窗样式 */
.import-tabs {
  padding: 4px 0;
}

.import-excel {
  padding: 16px 0;
}

.import-excel .upload-icon {
  font-size: 48px;
  color: #409EFF;
}

.import-excel .el-upload__tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.import-template {
  margin-top: 16px;
  text-align: center;
}

.import-manual {
  padding: 8px 0;
}

.manual-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.manual-label {
  font-size: 14px;
  color: #606266;
}

.manual-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
}

.manual-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e8ecf1;
}

.manual-count {
  font-size: 14px;
  color: #606266;
}

.manual-count strong {
  color: #409EFF;
  font-size: 18px;
}

/* 表格样式 */
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

/* 响应式 */
@media (max-width: 768px) {
  .card-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    flex-direction: column;
    align-items: stretch;
  }

  .search-group {
    flex-direction: column;
    align-items: stretch;
  }

  .search-input,
  .role-select {
    width: 100% !important;
  }

  .action-group {
    flex-direction: column;
    align-items: stretch;
  }

  .action-group .el-button {
    width: 100%;
  }

  .manual-footer {
    flex-direction: column;
    gap: 12px;
  }

  .manual-footer .el-button {
    width: 100%;
  }
}
</style>