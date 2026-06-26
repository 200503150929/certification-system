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

      <div class="requirement-list">
        <div v-for="(item, index) in requirementList" :key="item.id" class="requirement-item">
          <div class="requirement-header">
            <span class="requirement-code">{{ item.code }}</span>
            <el-tag :type="getStandardType(item.standard)">{{ item.standard }}</el-tag>
            <div class="requirement-actions">
              <el-button link type="primary" :icon="Edit" @click="handleEdit(item)" />
              <el-button link type="danger" :icon="Delete" @click="handleDelete(item.id)" />
              <el-button link type="primary" @click="goToIndicators(item)">管理指标点</el-button>
            </div>
          </div>
          <div class="requirement-content">
            <p><strong>描述：</strong>{{ item.description }}</p>
          </div>
        </div>
        <el-empty v-if="requirementList.length === 0" description="暂无毕业要求" />
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
          <el-input v-model="formData.code" placeholder="如：1、2、3 或 1.1、1.2" />
        </el-form-item>
        <el-form-item label="要求描述" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入毕业要求描述"
          />
        </el-form-item>
        <el-form-item label="认证标准" prop="standard">
          <el-select v-model="formData.standard" placeholder="请选择认证标准" style="width: 100%">
            <el-option label="工程知识" value="工程知识" />
            <el-option label="问题分析" value="问题分析" />
            <el-option label="设计/开发解决方案" value="设计/开发解决方案" />
            <el-option label="研究" value="研究" />
            <el-option label="使用现代工具" value="使用现代工具" />
            <el-option label="工程与社会" value="工程与社会" />
            <el-option label="环境和可持续发展" value="环境和可持续发展" />
            <el-option label="职业规范" value="职业规范" />
            <el-option label="个人和团队" value="个人和团队" />
            <el-option label="沟通" value="沟通" />
            <el-option label="项目管理" value="项目管理" />
            <el-option label="终身学习" value="终身学习" />
          </el-select>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const programId = ref(route.query.programId || '')
const programName = ref('计算机科学与技术')

const requirementList = ref([
  { id: '1', code: '1', description: '工程知识：能够将数学、自然科学、工程基础和专业知识用于解决复杂工程问题', standard: '工程知识' },
  { id: '2', code: '2', description: '问题分析：能够应用数学、自然科学和工程科学的基本原理，识别、表达、并通过文献研究分析复杂工程问题', standard: '问题分析' },
  { id: '3', code: '3', description: '设计/开发解决方案：能够设计针对复杂工程问题的解决方案，并能够在设计环节中体现创新意识', standard: '设计/开发解决方案' },
  { id: '4', code: '4', description: '研究：能够基于科学原理并采用科学方法对复杂工程问题进行研究', standard: '研究' },
])

const dialogVisible = ref(false)
const dialogTitle = ref('新增毕业要求')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref('')

const formData = reactive({
  code: '',
  description: '',
  standard: ''
})

const formRules = {
  code: [{ required: true, message: '请输入要求编码', trigger: 'blur' }],
  description: [{ required: true, message: '请输入要求描述', trigger: 'blur' }],
  standard: [{ required: true, message: '请选择认证标准', trigger: 'change' }]
}

const getStandardType = (standard) => {
  const map = {
    '工程知识': 'primary',
    '问题分析': 'success',
    '设计/开发解决方案': 'warning',
    '研究': 'info',
    '使用现代工具': 'primary',
    '工程与社会': 'danger',
    '环境和可持续发展': 'success',
    '职业规范': 'info',
    '个人和团队': 'primary',
    '沟通': 'warning',
    '项目管理': 'success',
    '终身学习': 'info'
  }
  return map[standard] || 'info'
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
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = (id) => {
  ElMessageBox.confirm('确定要删除该毕业要求吗？', '提示', { type: 'warning' }).then(() => {
    const index = requirementList.value.findIndex(item => item.id === id)
    if (index !== -1) {
      requirementList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  })
}

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return
    if (isEdit.value) {
      const index = requirementList.value.findIndex(item => item.id === editId.value)
      if (index !== -1) {
        requirementList.value[index] = { ...formData, id: editId.value }
      }
      ElMessage.success('更新成功')
    } else {
      const newItem = { ...formData, id: String(Date.now()) }
      requirementList.value.push(newItem)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    resetForm()
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formData, { code: '', description: '', standard: '' })
  isEdit.value = false
  editId.value = ''
}

const goToIndicators = (row) => {
  router.push({ path: '/curriculum/indicators', query: { requirementId: row.id } })
}

onMounted(() => {})
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