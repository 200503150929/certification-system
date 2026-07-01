<template>
  <div class="requirements-tab">
    <div class="tab-header">
      <span class="tab-title">毕业要求列表</span>
      <el-button
          type="primary"
          size="small"
          :icon="Plus"
          @click="handleAdd"
          :disabled="disabled"
      >
        新增要求
      </el-button>
    </div>

    <div class="requirement-list" v-loading="loading">
      <div v-for="item in requirementList" :key="item.id" class="requirement-item">
        <div class="requirement-header">
          <span class="requirement-code">{{ item.code }}</span>
          <div class="requirement-actions">
            <el-button
                link
                type="primary"
                :icon="Edit"
                @click="handleEdit(item)"
                :disabled="disabled"
            />
            <el-button
                link
                type="danger"
                :icon="Delete"
                @click="handleDelete(item.id)"
                :disabled="disabled"
            />
            <el-button
                link
                type="primary"
                size="small"
                @click="manageIndicators(item)"
            >
              管理指标点
            </el-button>
          </div>
        </div>
        <div class="requirement-content">
          <p>{{ item.description }}</p>
        </div>

        <!-- 指标点子列表 -->
        <div v-if="expandedRequirementId === item.id" class="indicators-sub-list">
          <div class="sub-header">
            <span class="sub-title">指标点列表</span>
            <el-button
                type="primary"
                size="small"
                :icon="Plus"
                @click="addIndicator(item.id)"
                :disabled="disabled"
            >
              新增指标点
            </el-button>
          </div>
          <el-table :data="indicatorMap[item.id] || []" size="small" border>
            <el-table-column prop="code" label="编码" width="120" />
            <el-table-column prop="description" label="描述" min-width="200" />
            <el-table-column prop="weight" label="权重" width="100">
              <template #default="scope">
                {{ formatWeight(scope.row.weight) }}%
              </template>
            </el-table-column>
            <el-table-column prop="passScore" label="合格标准" width="120">
              <template #default="scope">
                {{ formatPassScore(scope.row.passScore) }}分
              </template>
            </el-table-column>
            <el-table-column label="操作" width="120">
              <template #default="scope">
                <el-button
                    link
                    type="primary"
                    size="small"
                    @click="editIndicator(scope.row)"
                    :disabled="disabled"
                >
                  编辑
                </el-button>
                <el-button
                    link
                    type="danger"
                    size="small"
                    @click="deleteIndicator(scope.row.id)"
                    :disabled="disabled"
                >
                  删除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!indicatorMap[item.id]?.length" description="暂无指标点" :image-size="60" />
        </div>
      </div>
      <el-empty v-if="!loading && requirementList.length === 0" description="暂无毕业要求" />
    </div>

    <!-- 毕业要求弹窗 -->
    <el-dialog
        v-model="dialogVisible"
        :title="dialogTitle"
        width="600px"
        @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="要求编码" prop="code">
          <el-input v-model="formData.code" placeholder="如：1" />
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

    <!-- 指标点弹窗 -->
    <el-dialog
        v-model="indicatorDialogVisible"
        :title="indicatorDialogTitle"
        width="600px"
        @close="resetIndicatorForm"
    >
      <el-form ref="indicatorFormRef" :model="indicatorFormData" :rules="indicatorFormRules" label-width="120px">
        <el-form-item label="指标点编码" prop="code">
          <el-input v-model="indicatorFormData.code" placeholder="如：1.1" />
        </el-form-item>
        <el-form-item label="指标点描述" prop="description">
          <el-input
              v-model="indicatorFormData.description"
              type="textarea"
              :rows="3"
              placeholder="请输入指标点描述"
          />
        </el-form-item>
        <el-form-item label="权重(%)" prop="weight">
          <el-input-number v-model="indicatorFormData.weight" :min="0" :max="100" controls-position="right" />
          <span style="margin-left: 8px; color: #999;">%</span>
        </el-form-item>
        <el-form-item label="合格标准(分)" prop="passScore">
          <el-input-number v-model="indicatorFormData.passScore" :min="0" :max="100" controls-position="right" />
          <span style="margin-left: 8px; color: #999;">分</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="indicatorDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitIndicatorForm" :loading="submittingIndicator">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import request from '@/api/request'

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

const requirementList = ref([])
const indicatorMap = ref({})
const expandedRequirementId = ref(null)
const loading = ref(false)
const submitting = ref(false)

// 毕业要求弹窗
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

// 指标点弹窗
const indicatorDialogVisible = ref(false)
const indicatorDialogTitle = ref('新增指标点')
const indicatorFormRef = ref(null)
const isIndicatorEdit = ref(false)
const indicatorEditId = ref('')
const currentRequirementId = ref('')
const submittingIndicator = ref(false)

const indicatorFormData = reactive({
  code: '',
  description: '',
  weight: 10,
  passScore: 60
})

const indicatorFormRules = {
  code: [{ required: true, message: '请输入指标点编码', trigger: 'blur' }],
  description: [{ required: true, message: '请输入指标点描述', trigger: 'blur' }],
  weight: [{ required: true, message: '请输入权重', trigger: 'blur' }],
  passScore: [{ required: true, message: '请输入合格标准', trigger: 'blur' }]
}

// ==================== 单位换算工具函数 ====================

/**
 * 后端权重（小数）→ 前端显示（百分比）
 * 0.1 → 10
 */
const formatWeight = (backendWeight) => {
  if (backendWeight === null || backendWeight === undefined) return 0
  return Math.round(Number(backendWeight) * 100)
}

/**
 * 后端合格标准（小数）→ 前端显示（分数）
 * 0.6 → 60
 */
const formatPassScore = (backendPassScore) => {
  if (backendPassScore === null || backendPassScore === undefined) return 0
  return Math.round(Number(backendPassScore) * 100)
}

/**
 * 前端权重（百分比）→ 后端存储（小数）
 * 10 → 0.1
 */
const toBackendWeight = (frontendWeight) => {
  return Number(frontendWeight) / 100
}

/**
 * 前端合格标准（分数）→ 后端存储（小数）
 * 60 → 0.6
 */
const toBackendPassScore = (frontendPassScore) => {
  return Number(frontendPassScore) / 100
}

// ==================== 数据加载 ====================

// 加载毕业要求列表
const loadRequirements = async () => {
  if (!props.programId) return
  loading.value = true
  try {
    const res = await request.get(`/admin/program/requirements/list/${props.programId}`)
    requirementList.value = res.data || []
    // 加载每个毕业要求的指标点
    for (const req of requirementList.value) {
      await loadIndicators(req.id)
    }
  } catch (e) {
    ElMessage.error(e.message || '加载毕业要求失败')
  } finally {
    loading.value = false
  }
}

// 加载指标点
const loadIndicators = async (requirementId) => {
  try {
    const res = await request.get(`/admin/program/indicators/list/${requirementId}`)
    indicatorMap.value[requirementId] = res.data || []
  } catch (e) {
    indicatorMap.value[requirementId] = []
  }
}

// ==================== 指标点管理 ====================

// 展开管理指标点
const manageIndicators = (item) => {
  expandedRequirementId.value = expandedRequirementId.value === item.id ? null : item.id
}

// 新增指标点
const addIndicator = (requirementId) => {
  if (props.disabled) return
  currentRequirementId.value = requirementId
  indicatorDialogTitle.value = '新增指标点'
  isIndicatorEdit.value = false

  // 获取当前毕业要求的编码作为前缀
  const req = requirementList.value.find(r => r.id === requirementId)
  const prefix = req ? req.code : ''
  const count = (indicatorMap.value[requirementId]?.length || 0) + 1
  indicatorFormData.code = `${prefix}.${count}`

  indicatorFormData.description = ''
  indicatorFormData.weight = 10
  indicatorFormData.passScore = 60
  indicatorDialogVisible.value = true
}

// 编辑指标点
const editIndicator = (row) => {
  if (props.disabled) return
  indicatorDialogTitle.value = '编辑指标点'
  isIndicatorEdit.value = true
  indicatorEditId.value = row.id
  currentRequirementId.value = row.requirementId
  indicatorFormData.code = row.code || ''
  indicatorFormData.description = row.description || ''
  // 【修复】后端小数 → 前端显示值（乘以100）
  indicatorFormData.weight = formatWeight(row.weight)
  indicatorFormData.passScore = formatPassScore(row.passScore)
  indicatorDialogVisible.value = true
}

// 删除指标点
const deleteIndicator = (id) => {
  if (props.disabled) return
  ElMessageBox.confirm('确定要删除该指标点吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/admin/program/indicators/delete/${id}`)
      ElMessage.success('删除成功')
      if (expandedRequirementId.value) {
        await loadIndicators(expandedRequirementId.value)
      }
      emit('change')
    } catch (e) {
      ElMessage.error(e.message || '删除失败')
    }
  }).catch(() => {})
}

// 提交指标点
const submitIndicatorForm = () => {
  indicatorFormRef.value.validate(async (valid) => {
    if (!valid) return
    submittingIndicator.value = true
    try {
      const payload = {
        requirementId: Number(currentRequirementId.value),
        code: indicatorFormData.code,
        description: indicatorFormData.description,
        // 【修复】前端显示值 → 后端存储值（除以100）
        weight: toBackendWeight(indicatorFormData.weight),
        passScore: toBackendPassScore(indicatorFormData.passScore)
      }
      if (isIndicatorEdit.value) {
        payload.id = indicatorEditId.value
        await request.put('/admin/program/indicators/update', payload)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/program/indicators/add', payload)
        ElMessage.success('新增成功')
      }
      indicatorDialogVisible.value = false
      resetIndicatorForm()
      await loadIndicators(currentRequirementId.value)
      emit('change')
    } catch (e) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submittingIndicator.value = false
    }
  })
}

const resetIndicatorForm = () => {
  indicatorFormRef.value?.resetFields()
  indicatorFormData.code = ''
  indicatorFormData.description = ''
  indicatorFormData.weight = 10
  indicatorFormData.passScore = 60
  isIndicatorEdit.value = false
  indicatorEditId.value = ''
}

// ==================== 毕业要求管理 ====================

const handleAdd = () => {
  if (props.disabled) return
  dialogTitle.value = '新增毕业要求'
  isEdit.value = false
  formData.code = `${requirementList.value.length + 1}`
  dialogVisible.value = true
}

const handleEdit = (row) => {
  if (props.disabled) return
  dialogTitle.value = '编辑毕业要求'
  isEdit.value = true
  editId.value = row.id
  formData.code = row.code || ''
  formData.description = row.description || ''
  dialogVisible.value = true
}

const handleDelete = (id) => {
  if (props.disabled) return
  ElMessageBox.confirm('确定要删除该毕业要求吗？删除后其下指标点也将被删除。', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/admin/program/requirements/delete/${id}`)
      ElMessage.success('删除成功')
      loadRequirements()
      emit('change')
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
        programId: Number(props.programId),
        code: formData.code,
        description: formData.description
      }
      if (isEdit.value) {
        payload.id = editId.value
        await request.put('/admin/program/requirements/update', payload)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/program/requirements/add', payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      resetForm()
      loadRequirements()
      emit('change')
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

// ==================== 生命周期 ====================

watch(() => props.programId, () => {
  loadRequirements()
})

onMounted(() => {
  loadRequirements()
})
</script>

<style scoped>
.requirements-tab {
  padding: 4px 0;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tab-title {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
}

.requirement-item {
  padding: 16px 20px;
  margin-bottom: 12px;
  border: 1px solid #e8ecf1;
  border-radius: 8px;
  background: #fafafa;
}

.requirement-item:hover {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

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

.requirement-actions {
  margin-left: auto;
  display: flex;
  gap: 4px;
}

.requirement-content p {
  margin: 0;
  color: #555;
  line-height: 1.6;
}

/* 指标点子列表 */
.indicators-sub-list {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #e8ecf1;
}

.sub-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.sub-title {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}
</style>