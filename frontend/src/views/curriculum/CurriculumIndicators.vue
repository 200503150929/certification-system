<template>
  <div class="curriculum-indicators-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/curriculum/management' }">专业管理</el-breadcrumb-item>
              <el-breadcrumb-item :to="{ path: '/curriculum/requirements' }">毕业要求</el-breadcrumb-item>
              <el-breadcrumb-item>指标点管理</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ requirementCode }} - {{ requirementName }}</div>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Plus" @click="handleAdd">新增指标点</el-button>
          </div>
        </div>
      </template>

      <el-table :data="indicatorList" border style="width: 100%">
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

      <div class="weight-summary">
        <span>权重合计：</span>
        <span :style="{ color: totalWeight === 100 ? '#67c23a' : '#f56c6c' }">
          {{ totalWeight }}%
        </span>
        <span v-if="totalWeight !== 100" style="color: #f56c6c; font-size: 13px; margin-left: 8px;">
          ⚠️ 权重总和应为 100%
        </span>
      </div>

      <el-empty v-if="indicatorList.length === 0" description="暂无指标点" />
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
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const route = useRoute()
const requirementId = ref(route.query.requirementId || '')
const requirementCode = ref('毕业要求 1')
const requirementName = ref('工程知识')

const indicatorList = ref([
  { id: '1', code: '1.1', description: '能将数学、自然科学知识用于工程问题的表述', weight: 25, passScore: 60 },
  { id: '2', code: '1.2', description: '能针对具体工程问题建立数学模型并求解', weight: 30, passScore: 60 },
  { id: '3', code: '1.3', description: '能够将相关知识和数学模型用于专业工程问题解决方案的比较与综合', weight: 25, passScore: 60 },
  { id: '4', code: '1.4', description: '能运用工程原理分析工程问题的关键环节和影响因素', weight: 20, passScore: 60 },
])

const totalWeight = computed(() => {
  return indicatorList.value.reduce((sum, item) => sum + (item.weight || 0), 0)
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

const handleAdd = () => {
  dialogTitle.value = '新增指标点'
  isEdit.value = false
  dialogVisible.value = true
}

const handleEdit = (row) => {
  dialogTitle.value = '编辑指标点'
  isEdit.value = true
  editId.value = row.id
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该指标点吗？', '提示', { type: 'warning' }).then(() => {
    const index = indicatorList.value.findIndex(item => item.id === id)
    if (index !== -1) {
      indicatorList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  })
}

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return
    if (isEdit.value) {
      const index = indicatorList.value.findIndex(item => item.id === editId.value)
      if (index !== -1) {
        indicatorList.value[index] = { ...formData, id: editId.value }
      }
      ElMessage.success('更新成功')
    } else {
      const newItem = { ...formData, id: String(Date.now()) }
      indicatorList.value.push(newItem)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    resetForm()
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formData, { code: '', description: '', weight: 10, passScore: 60 })
  isEdit.value = false
  editId.value = ''
}

const handleWeightChange = (row) => {}
const handlePassScoreChange = (row) => {}

onMounted(() => {})
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