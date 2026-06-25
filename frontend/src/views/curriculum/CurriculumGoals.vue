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
        <div class="goal-list-normal">
          <div v-for="(item, index) in goalList" :key="item.id" class="goal-item">
            <div class="goal-header">
              <span class="goal-index">目标 {{ index + 1 }}</span>
              <el-tag :type="item.level === '核心' ? 'danger' : 'info'">
                {{ item.level }}
              </el-tag>
              <div class="goal-actions">
                <el-button link type="primary" :icon="Edit" @click="handleEdit(item)" />
                <el-button link type="danger" :icon="Delete" @click="handleDelete(item.id)" />
              </div>
            </div>
            <div class="goal-content">
              <p><strong>描述：</strong>{{ item.description }}</p>
              <p><strong>能力需求：</strong>{{ item.capability }}</p>
            </div>
          </div>
        </div>

        <el-empty v-if="goalList.length === 0" description="暂无培养目标" />
      </div>
    </el-card>

    <CurriculumMatrix class="goals-matrix" />

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
            :rows="3"
            placeholder="请输入培养目标描述"
          />
        </el-form-item>
        <el-form-item label="能力需求" prop="capability">
          <el-input
            v-model="formData.capability"
            type="textarea"
            :rows="2"
            placeholder="请输入对应的行业能力需求说明"
          />
        </el-form-item>
        <el-form-item label="重要程度" prop="level">
          <el-radio-group v-model="formData.level">
            <el-radio label="核心">核心</el-radio>
            <el-radio label="辅助">辅助</el-radio>
          </el-radio-group>
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'
import CurriculumMatrix from './CurriculumMatrix.vue'

const route = useRoute()
const programId = ref(route.query.programId || '')
const programName = ref('计算机科学与技术')

const goalList = ref([
  { id: '1', description: '培养具备扎实的计算机科学理论基础和工程实践能力的高级专门人才', capability: '能够运用计算机科学理论解决复杂工程问题', level: '核心' },
  { id: '2', description: '培养具有创新意识和团队协作精神的复合型人才', capability: '具备跨学科合作和创新能力', level: '核心' },
  { id: '3', description: '培养具备国际视野和可持续发展意识的技术人才', capability: '能够关注技术发展前沿和社会责任', level: '辅助' },
])

const dialogVisible = ref(false)
const dialogTitle = ref('新增培养目标')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref('')

const formData = reactive({
  description: '',
  capability: '',
  level: '核心'
})

const formRules = {
  description: [{ required: true, message: '请输入目标描述', trigger: 'blur' }],
  capability: [{ required: true, message: '请输入能力需求', trigger: 'blur' }]
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
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该培养目标吗？', '提示', { type: 'warning' }).then(() => {
    const index = goalList.value.findIndex(item => item.id === id)
    if (index !== -1) {
      goalList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  })
}

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return
    if (isEdit.value) {
      const index = goalList.value.findIndex(item => item.id === editId.value)
      if (index !== -1) {
        goalList.value[index] = { ...formData, id: editId.value }
      }
      ElMessage.success('更新成功')
    } else {
      const newItem = { ...formData, id: String(Date.now()) }
      goalList.value.push(newItem)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    resetForm()
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formData, { description: '', capability: '', level: '核心' })
  isEdit.value = false
  editId.value = ''
}

onMounted(() => {})
</script>

<style scoped>
.curriculum-goals-container { padding: 20px; }

.goals-matrix {
  margin-top: 20px;
  padding: 0;
}

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
.goal-actions { margin-left: auto; display: flex; gap: 4px; }
.goal-content p { margin: 4px 0; color: #555; line-height: 1.6; }
</style>
