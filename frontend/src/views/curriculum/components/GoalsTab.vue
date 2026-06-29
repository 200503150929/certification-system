<template>
  <div class="goals-tab">
    <div class="tab-header">
      <span class="tab-title">培养目标列表</span>
      <el-button
          v-permission="'program:manage'"
          type="primary"
          size="small"
          :icon="Plus"
          @click="handleAdd"
          :disabled="disabled"
      >
        新增目标
      </el-button>
    </div>

    <div class="goal-list" v-loading="loading">
      <div v-for="(item, index) in goalList" :key="item.id" class="goal-item">
        <div class="goal-header">
          <span class="goal-index">目标 {{ index + 1 }}</span>
          <span class="goal-sort">排序: {{ item.sortOrder || 0 }}</span>
          <div class="goal-actions">
            <el-button
                v-permission="'program:manage'"
                link
                type="primary"
                :icon="Edit"
                @click="handleEdit(item)"
                :disabled="disabled"
            />
            <el-button
                v-permission="'program:manage'"
                link
                type="danger"
                :icon="Delete"
                @click="handleDelete(item.id)"
                :disabled="disabled"
            />
          </div>
        </div>
        <div class="goal-content">
          <p>{{ item.description }}</p>
        </div>
      </div>
      <el-empty v-if="!loading && goalList.length === 0" description="暂无培养目标" />
    </div>

    <!-- 新增/编辑弹窗 -->
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
          <span class="form-tip">数字越小越靠前</span>
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

// 加载培养目标列表
const loadGoals = async () => {
  if (!props.programId) return
  loading.value = true
  try {
    const res = await request.get(`/admin/program/objectives/list/${props.programId}`)
    goalList.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载培养目标失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  if (props.disabled) return
  dialogTitle.value = '新增培养目标'
  isEdit.value = false
  formData.sortOrder = goalList.value.length + 1
  dialogVisible.value = true
}

const handleEdit = (row) => {
  if (props.disabled) return
  dialogTitle.value = '编辑培养目标'
  isEdit.value = true
  editId.value = row.id
  formData.description = row.description || ''
  formData.sortOrder = row.sortOrder || 0
  dialogVisible.value = true
}

const handleDelete = (id) => {
  if (props.disabled) return
  ElMessageBox.confirm('确定要删除该培养目标吗？', '提示', { type: 'warning' }).then(async () => {
    try {
      await request.delete(`/admin/program/objectives/delete/${id}`)
      ElMessage.success('删除成功')
      loadGoals()
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
        description: formData.description,
        sortOrder: formData.sortOrder
      }
      if (isEdit.value) {
        payload.id = editId.value
        await request.put('/admin/program/objectives/update', payload)
        ElMessage.success('更新成功')
      } else {
        await request.post('/admin/program/objectives/add', payload)
        ElMessage.success('新增成功')
      }
      dialogVisible.value = false
      resetForm()
      loadGoals()
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
  formData.description = ''
  formData.sortOrder = 0
  isEdit.value = false
  editId.value = ''
}

// 监听 programId 变化重新加载
watch(() => props.programId, () => {
  loadGoals()
})

onMounted(() => {
  loadGoals()
})
</script>

<style scoped>
.goals-tab {
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

.form-tip {
  margin-left: 12px;
  font-size: 13px;
  color: #909399;
}

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

/* 禁用状态样式 */
:deep(.is-disabled .el-button) {
  cursor: not-allowed;
}
</style>