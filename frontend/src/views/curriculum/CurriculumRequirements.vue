<template>
  <div class="curriculum-requirements-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/curriculum/management' }">专业管理</el-breadcrumb-item>
              <el-breadcrumb-item>毕业要求管理</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ programName }}</div>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增要求</el-button>
          </div>
        </div>
      </template>

      <div class="requirement-list" v-loading="loading">
        <div v-for="(item, index) in requirementList" :key="item.id" class="requirement-item">
          <div class="requirement-header">
            <span class="requirement-code">{{ item.code }}</span>
            <div class="requirement-actions">
              <el-button link type="primary" :icon="Edit" @click="handleEdit(item)" />
              <el-button link type="danger" :icon="Delete" @click="handleDelete(item.id)" />
              <el-button link type="primary" @click="goToIndicators(item)">管理指标点</el-button>
            </div>
          </div>
          <div class="requirement-content">
            <p>{{ item.description }}</p>
          </div>
        </div>
        <el-empty v-if="!loading && requirementList.length === 0" description="暂无毕业要求" />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="要求编码" prop="code">
          <el-input v-model="formData.code" placeholder="如：1、2、3" />
        </el-form-item>
        <el-form-item label="要求描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入毕业要求描述"
          />
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const programId = ref(route.query.programId || '')
const programName = ref('')

const requirementList = ref([])
const loading = ref(false)
const submitting = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增毕业要求')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref('')

const formData = reactive({
  code: '',
  description: ''
})

const formRules = {
  code: [{ required: true, message: '请输入要求编码', trigger: 'blur' }],
  description: [{ required: true, message: '请输入要求描述', trigger: 'blur' }]
}

// 加载专业信息
const loadProgramInfo = async () => {
  if (!programId.value) return
  try {
    const res = await request.get(`/admin/program/get/${programId.value}`)
    if (res.data) {
      programName.value = res.data.majorName || ''
    }
  } catch {
    // ignore
  }
}

// 加载毕业要求列表
const loadRequirements = async () => {
  if (!programId.value) return
  loading.value = true
  try {
    const res = await request.get(`/admin/program/requirements/list/${programId.value}`)
    requirementList.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载毕业要求失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增毕业要求'
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑毕业要求'
  isEdit.value = true
  editId.value = row.id
  formData.code = row.code || ''
  formData.description = row.description || ''
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该毕业要求吗？删除后其下指标点也将被删除。', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/admin/program/requirements/delete/${id}`)
      ElMessage.success('删除成功')
      loadRequirements()
    } catch (e) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      if (isEdit.value) {
        await request.put('/admin/program/requirements/update', {
          id: editId.value,
          programId: Number(programId.value),
          code: formData.code,
          description: formData.description
        })
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/program/requirements/add', {
          programId: Number(programId.value),
          code: formData.code,
          description: formData.description
        })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      resetForm()
      loadRequirements()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  formData.code = ''
  formData.description = ''
  isEdit.value = false
  editId.value = ''
}

const goToIndicators = (row) => {
  router.push({
    path: '/curriculum/indicators',
    query: { requirementId: row.id, programId: programId.value }
  })
}

onMounted(() => {
  loadProgramInfo()
  loadRequirements()
})
</script>

<style scoped>
.curriculum-requirements-container { padding: 20px; }

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

.requirement-list { margin-top: 16px; }
.requirement-item {
  padding: 16px 20px;
  margin-bottom: 12px;
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  background: #fafafa;
}
.requirement-item:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.requirement-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}
.requirement-code {
  font-weight: 700;
  font-size: 16px;
  color: #409EFF;
  background: #ecf5ff;
  padding: 2px 12px;
  border-radius: 4px;
}
.requirement-actions { margin-left: auto; display: flex; gap: 4px; }
.requirement-content p { margin: 0; color: #555; line-height: 1.6; }
</style>
