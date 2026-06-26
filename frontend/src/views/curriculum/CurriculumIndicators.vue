<template>
  <div class="curriculum-indicators-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/curriculum/management' }">专业管理</el-breadcrumb-item>
              <el-breadcrumb-item :to="{ path: '/curriculum/requirements', query: { programId } }">毕业要求</el-breadcrumb-item>
              <el-breadcrumb-item>指标点管理</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ requirementCode }} - {{ requirementDesc }}</div>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增指标点</el-button>
          </div>
        </div>
      </template>

      <el-table :data="indicatorList" border style="width: 100%" v-loading="loading">
        <el-table-column prop="code" label="指标点编码" width="140" />
        <el-table-column prop="description" label="指标点描述" min-width="300" />
        <el-table-column prop="weight" label="权重" width="130">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.weight"
              :min="0"
              :max="100"
              size="small"
              controls-position="right"
              @change="handleWeightChange(scope.row)"
            />
            <span style="font-size:12px; color:#999; margin-left:4px;">%</span>
          </template>
        </el-table-column>
        <el-table-column prop="passScore" label="合格标准" width="150">
          <template #default="scope">
            <el-input-number
              v-model="scope.row.passScore"
              :min="0"
              :max="100"
              size="small"
              controls-position="right"
              @change="handlePassScoreChange(scope.row)"
            />
            <span style="font-size:12px; color:#999; margin-left:4px;">分</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :icon="Edit" @click="handleEdit(scope.row)" />
            <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row.id)" />
          </template>
        </el-table-column>
      </el-table>

      <div class="weight-summary" v-if="indicatorList.length > 0">
        <span>权重合计：</span>
        <span :style="{ color: totalWeight === 100 ? '#67c23a' : '#f56c6c' }">
          {{ totalWeight }}%
        </span>
        <span v-if="totalWeight !== 100" style="color: #f56c6c; font-size: 13px; margin-left: 8px;">
          权重总和应为 100%
        </span>
      </div>

      <el-empty v-if="!loading && indicatorList.length === 0" description="暂无指标点" />
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="指标点编码" prop="code">
          <el-input v-model="formData.code" placeholder="如：1.1、1.2" />
        </el-form-item>
        <el-form-item label="指标点描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入指标点描述"
          />
        </el-form-item>
        <el-form-item label="权重(%)" prop="weight">
          <el-input-number v-model="formData.weight" :min="0" :max="100" controls-position="right" />
          <span style="margin-left: 8px; color: #999;">%</span>
        </el-form-item>
        <el-form-item label="合格标准(分)" prop="passScore">
          <el-input-number v-model="formData.passScore" :min="0" :max="100" controls-position="right" />
          <span style="margin-left: 8px; color: #999;">分</span>
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
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/api/request'

const route = useRoute()
const requirementId = ref(route.query.requirementId || '')
const programId = ref(route.query.programId || '')
const requirementCode = ref('')
const requirementDesc = ref('')

const indicatorList = ref([])
const loading = ref(false)
const submitting = ref(false)

const totalWeight = computed(() => {
  return indicatorList.value.reduce((sum, item) => sum + (Number(item.weight) || 0), 0)
})

const dialogVisible = ref(false)
const dialogTitle = ref('新增指标点')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref('')

const formData = reactive({
  code: '',
  description: '',
  weight: 10,
  passScore: 60
})

const formRules = {
  code: [{ required: true, message: '请输入指标点编码', trigger: 'blur' }],
  description: [{ required: true, message: '请输入指标点描述', trigger: 'blur' }],
  weight: [{ required: true, message: '请输入权重', trigger: 'blur' }],
  passScore: [{ required: true, message: '请输入合格标准', trigger: 'blur' }]
}

// 加载毕业要求信息
const loadRequirementInfo = async () => {
  if (!requirementId.value) return
  try {
    const res = await request.get(`/admin/program/requirements/detail/${requirementId.value}`)
    if (res.data) {
      requirementCode.value = res.data.code || ''
      requirementDesc.value = res.data.description || ''
    }
  } catch {
    // ignore
  }
}

// 加载指标点列表
const loadIndicators = async () => {
  if (!requirementId.value) return
  loading.value = true
  try {
    const res = await request.get(`/admin/program/indicators/list/${requirementId.value}`)
    indicatorList.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载指标点失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  dialogTitle.value = '新增指标点'
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑指标点'
  isEdit.value = true
  editId.value = row.id
  formData.code = row.code || ''
  formData.description = row.description || ''
  formData.weight = Number(row.weight) || 10
  formData.passScore = Number(row.passScore) || 60
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该指标点吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/admin/program/indicators/delete/${id}`)
      ElMessage.success('删除成功')
      loadIndicators()
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
      const payload = {
        requirementId: Number(requirementId.value),
        code: formData.code,
        description: formData.description,
        weight: formData.weight,
        passScore: formData.passScore
      }
      if (isEdit.value) {
        payload.id = editId.value
        await request.put('/admin/program/indicators/update', payload)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/program/indicators/add', payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      resetForm()
      loadIndicators()
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
  formData.weight = 10
  formData.passScore = 60
  isEdit.value = false
  editId.value = ''
}

const handleWeightChange = async (row) => {
  try {
    await request.put('/admin/program/indicators/update', {
      id: row.id,
      requirementId: Number(requirementId.value),
      code: row.code,
      description: row.description,
      weight: row.weight,
      passScore: row.passScore
    })
  } catch {
    // silently fail for inline edits
  }
}

const handlePassScoreChange = async (row) => {
  try {
    await request.put('/admin/program/indicators/update', {
      id: row.id,
      requirementId: Number(requirementId.value),
      code: row.code,
      description: row.description,
      weight: row.weight,
      passScore: row.passScore
    })
  } catch {
    // silently fail
  }
}

onMounted(() => {
  loadRequirementInfo()
  loadIndicators()
})
</script>

<style scoped>
.curriculum-indicators-container { padding: 20px; }

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

.weight-summary {
  margin-top: 16px;
  padding: 12px 20px;
  background: #f7f8fa;
  border-radius: 6px;
  text-align: right;
  font-size: 14px;
}
.weight-summary span:first-child { font-weight: 600; color: #333; }
.weight-summary span:last-child { font-weight: 700; font-size: 16px; }
</style>
