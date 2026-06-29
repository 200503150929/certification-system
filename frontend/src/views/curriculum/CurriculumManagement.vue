<template>
  <div class="curriculum-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>专业管理</span>
          <div>
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增专业</el-button>
          </div>
        </div>
      </template>

      <!-- 搜索栏 -->
      <div class="search-bar">
        <el-input
            v-model="searchName"
            placeholder="搜索专业名称"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
        />
        <el-select v-model="searchStatus" placeholder="状态筛选" clearable style="width: 140px; margin-left: 12px">
          <el-option label="已发布" value="published" />
          <el-option label="草稿" value="draft" />
        </el-select>
        <el-button type="primary" @click="handleSearch" style="margin-left: 12px">搜索</el-button>
      </div>

      <!-- 专业列表 -->
      <el-table :data="programList" border style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="majorName" label="专业名称" min-width="200" />
        <el-table-column prop="version" label="版本号" width="120">
          <template #default="scope">
            <el-tag>v{{ scope.row.version }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === 'published' ? 'success' : 'warning'">
              {{ scope.row.status === 'published' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="180" />
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="scope">
            <!-- 草稿状态：显示编辑按钮 -->
            <el-button
                v-if="scope.row.status === 'draft'"
                link
                type="primary"
                @click="handleEditBasicInfo(scope.row)"
            >
              编辑
            </el-button>
            <!-- 查看按钮：草稿和已发布都显示 -->
            <el-button
                link
                type="primary"
                @click="goToDetail(scope.row)"
            >
              查看
            </el-button>
            <!-- 草稿状态：发布 -->
            <el-button
                v-if="scope.row.status === 'draft'"
                link
                type="success"
                @click="handlePublish(scope.row.id)"
            >
              发布
            </el-button>
            <!-- 已发布状态：取消发布 -->
            <el-button
                v-if="scope.row.status === 'published'"
                link
                type="warning"
                @click="handleUnpublish(scope.row.id)"
            >
              取消发布
            </el-button>
            <!-- 删除：只有草稿状态可删除，已发布不允许删除 -->
            <el-button
                v-if="scope.row.status === 'draft'"
                link
                type="danger"
                @click="handleDelete(scope.row.id)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap" v-if="total > 0">
        <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[5, 10, 20]"
            layout="total, sizes, prev, pager, next"
            @size-change="loadPrograms"
            @current-change="loadPrograms"
        />
      </div>
    </el-card>

    <!-- ========== 新增专业弹窗 ========== -->
    <el-dialog
        v-model="addDialogVisible"
        title="新增专业"
        width="450px"
        @close="resetAddForm"
    >
      <el-form ref="addFormRef" :model="addForm" :rules="addFormRules" label-width="80px">
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="addForm.majorName" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="addForm.version" placeholder="如：1.0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm" :loading="addSubmitting">确认新增</el-button>
      </template>
    </el-dialog>

    <!-- ========== 编辑基本信息弹窗（名称 + 版本号） ========== -->
    <el-dialog
        v-model="editDialogVisible"
        title="编辑专业信息"
        width="450px"
        @close="resetEditForm"
    >
      <el-form ref="editFormRef" :model="editForm" :rules="editFormRules" label-width="80px">
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="editForm.majorName" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="editForm.version" placeholder="请输入版本号，如：1.0、2024" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitEditForm" :loading="editSubmitting">确认修改</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

defineOptions({
  name: 'CurriculumManagement'
})

const router = useRouter()

// ============ 列表数据 ============
const programList = ref([])
const loading = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchName = ref('')
const searchStatus = ref('')

// ============ 新增弹窗 ============
const addDialogVisible = ref(false)
const addFormRef = ref(null)
const addSubmitting = ref(false)
const addForm = reactive({
  majorName: '',
  version: '1.0'
})
const addFormRules = {
  majorName: [
    { required: true, message: '请输入专业名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' }
  ]
}

// ============ 编辑弹窗 ============
const editDialogVisible = ref(false)
const editFormRef = ref(null)
const editSubmitting = ref(false)
const editId = ref('')
const editForm = reactive({
  majorName: '',
  version: ''
})
const editFormRules = {
  majorName: [
    { required: true, message: '请输入专业名称', trigger: 'blur' },
    { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
  ],
  version: [
    { required: true, message: '请输入版本号', trigger: 'blur' }
  ]
}

// ============ 加载专业列表 ============
const loadPrograms = async () => {
  loading.value = true
  try {
    const params = {
      pageNum: page.value,
      pageSize: pageSize.value
    }
    if (searchName.value) params.majorNameFuzzy = searchName.value
    if (searchStatus.value) params.status = searchStatus.value
    const res = await request.get('/admin/program/list', { params })
    programList.value = res.data?.list || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error(e.message || '加载专业列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  page.value = 1
  loadPrograms()
}

// ============ 跳转到详情页 ============
const goToDetail = (row) => {
  router.push({
    path: `/app/curriculum/detail/${row.id}`
  })
}

// ============ 新增专业 ============
const handleAdd = () => {
  addForm.majorName = ''
  addForm.version = '1.0'
  addDialogVisible.value = true
}

const resetAddForm = () => {
  addFormRef.value?.resetFields()
}

const submitAddForm = () => {
  addFormRef.value.validate(async (valid) => {
    if (!valid) return
    addSubmitting.value = true
    try {
      await request.post('/admin/program/add', {
        majorName: addForm.majorName,
        version: addForm.version,
        status: 'draft'
      })
      ElMessage.success('新增成功')
      addDialogVisible.value = false
      loadPrograms()
    } catch (e) {
      ElMessage.error(e.message || '新增失败')
    } finally {
      addSubmitting.value = false
    }
  })
}

// ============ 编辑基本信息（名称 + 版本号） ============
const handleEditBasicInfo = (row) => {
  editId.value = row.id
  editForm.majorName = row.majorName || ''
  editForm.version = row.version || ''
  editDialogVisible.value = true
}

const resetEditForm = () => {
  editFormRef.value?.resetFields()
  editId.value = ''
}

const submitEditForm = () => {
  editFormRef.value.validate(async (valid) => {
    if (!valid) return
    editSubmitting.value = true
    try {
      await request.put('/admin/program/update', {
        id: Number(editId.value),
        majorName: editForm.majorName,
        version: editForm.version,
        status: 'draft'
      })
      ElMessage.success('修改成功')
      editDialogVisible.value = false
      loadPrograms()
    } catch (e) {
      ElMessage.error(e.message || '修改失败')
    } finally {
      editSubmitting.value = false
    }
  })
}

// ============ 删除 ============
const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该专业吗？删除后将级联删除培养目标、毕业要求、指标点等关联数据。', '提示', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      await request.delete(`/admin/program/delete/${id}`)
      ElMessage.success('删除成功')
      loadPrograms()
    } catch (e) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

// ============ 发布/取消发布 ============
const handlePublish = async (id) => {
  try {
    await request.put(`/admin/program/publish/${id}`)
    ElMessage.success('发布成功')
    loadPrograms()
  } catch (e) {
    ElMessage.error(e.message || '发布失败')
  }
}

const handleUnpublish = async (id) => {
  try {
    await request.put(`/admin/program/unpublish/${id}`)
    ElMessage.success('已取消发布')
    loadPrograms()
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

onMounted(() => {
  loadPrograms()
})
</script>

<style scoped>
.curriculum-management-container {
  padding: 20px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.search-bar {
  display: flex;
  align-items: center;
  padding: 16px 0;
}
.pagination-wrap {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>