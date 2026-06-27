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
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="goToDetail(scope.row)">编辑</el-button>
            <el-button
                v-if="scope.row.status === 'draft'"
                link type="success"
                @click="handlePublish(scope.row.id)"
            >
              发布
            </el-button>
            <el-button
                v-if="scope.row.status === 'published'"
                link type="warning"
                @click="handleUnpublish(scope.row.id)"
            >
              取消发布
            </el-button>
            <el-button link type="danger" @click="handleDelete(scope.row.id)">删除</el-button>
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

    <!-- 新增/编辑专业弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="500px"
        @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="专业名称" prop="majorName">
          <el-input v-model="formData.majorName" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="版本号" prop="version">
          <el-input v-model="formData.version" placeholder="如：1.0" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio value="draft">草稿</el-radio>
            <el-radio value="published">已发布</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">确认</el-button>
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

const programList = ref([])
const loading = ref(false)
const submitting = ref(false)
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const searchName = ref('')
const searchStatus = ref('')

const dialogVisible = ref(false)
const dialogTitle = ref('新增专业')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref('')

const formData = reactive({
  majorName: '',
  version: '1.0',
  status: 'draft'
})

const formRules = {
  majorName: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }]
}

// 加载专业列表
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

// 跳转到专业详情页
const goToDetail = (row) => {
  router.push({
    path: `/curriculum/detail/${row.id}`
  })
}

const handleAdd = () => {
  dialogTitle.value = '新增专业'
  isEdit.value = false
  formData.majorName = ''
  formData.version = '1.0'
  formData.status = 'draft'
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑专业'
  isEdit.value = true
  editId.value = row.id
  formData.majorName = row.majorName || ''
  formData.version = row.version || ''
  formData.status = row.status || 'draft'
  dialogVisible.value = true
}

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

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      const payload = {
        majorName: formData.majorName,
        version: formData.version,
        status: formData.status
      }
      if (isEdit.value) {
        payload.id = editId.value
        await request.put('/admin/program/update', payload)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/program/add', payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      resetForm()
      loadPrograms()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  isEdit.value = false
  editId.value = ''
}

onMounted(() => {
  loadPrograms()
})
</script>

<style scoped>
.curriculum-management-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
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
</style>