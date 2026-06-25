<template>
  <div class="curriculum-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>专业管理</span>
          <el-button type="primary" :icon="Plus" @click="handleAdd">新增专业</el-button>
        </div>
      </template>

      <!-- 当前专业 -->
      <div class="current-program">
        <div class="program-selector">
          <span class="label">当前专业：</span>
          <el-select
            v-model="currentProgram"
            placeholder="请选择专业"
            style="width: 300px"
            @change="handleProgramChange"
          >
            <el-option
              v-for="item in programList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
          <el-tag type="success" size="large" style="margin-left: 12px">
            {{ getProgramStatus(currentProgram) }}
          </el-tag>
        </div>
        <div class="program-actions">
          <el-button :icon="Edit" text @click="handleEditProgram">编辑</el-button>
          <el-button :icon="Delete" text type="danger" @click="handleDeleteProgram">删除</el-button>
        </div>
      </div>

      <!-- 专业列表 -->
      <el-table :data="programList" border style="width: 100%; margin-top: 20px">
        <el-table-column prop="code" label="专业代码" width="120" />
        <el-table-column prop="name" label="专业名称" />
        <el-table-column prop="department" label="所属院系" width="150" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="scope">
            <el-tag :type="scope.row.status === '已发布' ? 'success' : 'warning'">
              {{ scope.row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="version" label="当前版本" width="120" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="goToGoals(scope.row)">培养目标</el-button>
            <el-button link type="primary" @click="goToRequirements(scope.row)">毕业要求</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑专业弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="500px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="专业代码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入专业代码" />
        </el-form-item>
        <el-form-item label="专业名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入专业名称" />
        </el-form-item>
        <el-form-item label="所属院系" prop="department">
          <el-input v-model="formData.department" placeholder="请输入所属院系" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio label="草稿">草稿</el-radio>
            <el-radio label="已发布">已发布</el-radio>
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
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete } from '@element-plus/icons-vue'

const router = useRouter()

const currentProgram = ref('')
const programList = ref([
  { id: '1', code: '080901', name: '计算机科学与技术', department: '计算机学院', status: '已发布', version: 'v2.0' },
  { id: '2', code: '080902', name: '软件工程', department: '计算机学院', status: '已发布', version: 'v1.5' },
  { id: '3', code: '080903', name: '数据科学与大数据技术', department: '计算机学院', status: '草稿', version: 'v0.8' },
  { id: '4', code: '080904', name: '人工智能', department: '计算机学院', status: '草稿', version: 'v0.3' },
])

const dialogVisible = ref(false)
const dialogTitle = ref('新增专业')
const formRef = ref(null)
const isEdit = ref(false)
const editId = ref('')

const formData = reactive({
  code: '',
  name: '',
  department: '',
  status: '草稿'
})

const formRules = {
  code: [{ required: true, message: '请输入专业代码', trigger: 'blur' }],
  name: [{ required: true, message: '请输入专业名称', trigger: 'blur' }],
  department: [{ required: true, message: '请输入所属院系', trigger: 'blur' }]
}

const getProgramStatus = (id) => {
  const program = programList.value.find(p => p.id === id)
  return program ? program.status : ''
}

const handleProgramChange = (val) => {
  ElMessage.success(`已切换到：${programList.value.find(p => p.id === val)?.name}`)
}

const handleAdd = () => {
  dialogTitle.value = '新增专业'
  isEdit.value = false
  dialogVisible.value = true
}

const handleEditProgram = () => {
  if (!currentProgram.value) {
    ElMessage.warning('请先选择专业')
    return
  }
  const program = programList.value.find(p => p.id === currentProgram.value)
  if (program) {
    dialogTitle.value = '编辑专业'
    isEdit.value = true
    editId.value = program.id
    Object.assign(formData, program)
    dialogVisible.value = true
  }
}

const handleDeleteProgram = () => {
  if (!currentProgram.value) {
    ElMessage.warning('请先选择专业')
    return
  }
  ElMessageBox.confirm('确定要删除该专业吗？', '提示', { type: 'warning' }).then(() => {
    const index = programList.value.findIndex(p => p.id === currentProgram.value)
    if (index !== -1) {
      programList.value.splice(index, 1)
      currentProgram.value = programList.value.length > 0 ? programList.value[0].id : ''
      ElMessage.success('删除成功')
    }
  })
}

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return
    if (isEdit.value) {
      const index = programList.value.findIndex(p => p.id === editId.value)
      if (index !== -1) {
        programList.value[index] = { ...formData, id: editId.value }
      }
      ElMessage.success('更新成功')
    } else {
      const newProgram = {
        ...formData,
        id: String(Date.now()),
        version: 'v0.1'
      }
      programList.value.push(newProgram)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    resetForm()
  })
}

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formData, { code: '', name: '', department: '', status: '草稿' })
  isEdit.value = false
  editId.value = ''
}

const goToGoals = (row) => {
  router.push({ path: '/curriculum/goals', query: { programId: row.id } })
}
const goToRequirements = (row) => {
  router.push({ path: '/curriculum/requirements', query: { programId: row.id } })
}

onMounted(() => {
  if (programList.value.length > 0) {
    currentProgram.value = programList.value[0].id
  }
})
</script>

<style scoped>
.curriculum-management-container { padding: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.current-program {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  background: #f7f8fa;
  border-radius: 8px;
}
.program-selector { display: flex; align-items: center; gap: 10px; }
.program-selector .label { font-weight: 600; color: #333; }
.program-actions { display: flex; gap: 8px; }
</style>