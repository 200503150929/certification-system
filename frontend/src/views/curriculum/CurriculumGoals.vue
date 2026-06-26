<template>
  <div class="curriculum-goals-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/curriculum/management' }">专业管理</el-breadcrumb-item>
              <el-breadcrumb-item>培养目标管理</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ programName }}</div>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增目标</el-button>
          </div>
        </div>
      </template>

      <div class="goal-list">
        <div v-loading="loading">
          <div v-for="(item, index) in goalList" :key="item.id" class="goal-item">
            <div class="goal-header">
              <span class="goal-index">目标 {{ index + 1 }}</span>
              <span class="goal-sort">排序: {{ item.sortOrder || 0 }}</span>
              <div class="goal-actions">
                <el-button link type="primary" :icon="Edit" @click="handleEdit(item)" />
                <el-button link type="danger" :icon="Delete" @click="handleDelete(item.id)" />
              </div>
            </div>
            <div class="goal-content">
              <p>{{ item.description }}</p>
            </div>
          </div>
        </div>

        <el-empty v-if="!loading && goalList.length === 0" description="暂无培养目标" />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="目标描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="4"
            placeholder="请输入培养目标描述"
          />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="formData.sortOrder" :min="0" :max="999" controls-position="right" />
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
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const programId = ref(route.query.programId || '')
const programName = ref('')

const goalList = ref([])
const loading = ref(false)
const submitting = ref(false)

const dialogVisible = ref(false)
const dialogTitle = ref('新增培养目标')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref('')

const formData = reactive({
  description: '',
  sortOrder: 0
})

const formRules = {
  description: [{ required: true, message: '请输入目标描述', trigger: 'blur' }]
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

// 加载培养目标列表
const loadGoals = async () => {
  if (!programId.value) return
  loading.value = true
  try {
    const res = await request.get(`/admin/program/objectives/list/${programId.value}`)
    goalList.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载培养目标失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增培养目标'
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑培养目标'
  isEdit.value = true
  editId.value = row.id
  formData.description = row.description || ''
  formData.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该培养目标吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/admin/program/objectives/delete/${id}`)
      ElMessage.success('删除成功')
      loadGoals()
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
        await request.put('/admin/program/objectives/update', {
          id: editId.value,
          programId: Number(programId.value),
          description: formData.description,
          sortOrder: formData.sortOrder
        })
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/program/objectives/add', {
          programId: Number(programId.value),
          description: formData.description,
          sortOrder: formData.sortOrder
        })
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      resetForm()
      loadGoals()
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submitting.value = false
    }
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  formData.description = ''
  formData.sortOrder = 0
  isEdit.value = false
  editId.value = ''
}

onMounted(() => {
  loadProgramInfo()
  loadGoals()
})
</script>

<style scoped>
.curriculum-goals-container { padding: 20px; }

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

.goal-list { margin-top: 16px; }
.goal-item {
  padding: 16px 20px;
  margin-bottom: 12px;
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  background: #fafafa;
  transition: all 0.3s;
}
.goal-item:hover { box-shadow: 0 2px 12px rgba(0,0,0,0.08); }
.goal-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.goal-index { font-weight: 600; color: #333; }
.goal-sort { font-size: 12px; color: #999; }
.goal-actions { margin-left: auto; display: flex; gap: 4px; }
.goal-content p { margin: 4px 0; color: #555; line-height: 1.6; }
</style>
