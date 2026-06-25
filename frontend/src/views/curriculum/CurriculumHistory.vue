<template>
  <div class="curriculum-history-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/curriculum/management' }">专业管理</el-breadcrumb-item>
              <el-breadcrumb-item>方案版本</el-breadcrumb-item>
            </el-breadcrumb>
            <div class="page-subtitle">{{ programName }}</div>
          </div>
          <div class="header-right">
            <el-button type="primary" :icon="Edit" @click="handleNewVersion">新建版本</el-button>
          </div>
        </div>
      </template>

      <el-timeline>
        <el-timeline-item
          v-for="item in historyList"
          :key="item.id"
          :timestamp="item.time"
          placement="top"
          :type="item.status === '已发布' ? 'primary' : 'info'"
          :icon="item.status === '已发布' ? SuccessFilled : Edit"
        >
          <el-card>
            <div class="version-header">
              <span class="version-tag">v{{ item.version }}</span>
              <el-tag :type="item.status === '已发布' ? 'success' : 'warning'">
                {{ item.status }}
              </el-tag>
              <span class="version-operator">操作人：{{ item.operator }}</span>
            </div>
            <div class="version-content">
              <p><strong>变更说明：</strong>{{ item.description }}</p>
              <div class="version-details">
                <span>变更类型：{{ item.type }}</span>
                <span>变更范围：{{ item.scope }}</span>
              </div>
            </div>
            <div class="version-actions">
              <el-button link type="primary" @click="handleViewDetail(item)">查看详情</el-button>
              <el-button v-if="item.status === '草稿'" link type="primary" @click="handlePublish(item)">发布</el-button>
              <el-button v-if="item.status === '草稿'" link type="danger" @click="handleDeleteVersion(item.id)">删除</el-button>
            </div>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建版本" width="500px">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="100px">
        <el-form-item label="版本号" prop="version">
          <el-input v-model="formData.version" placeholder="如：2.1" />
        </el-form-item>
        <el-form-item label="变更说明" prop="description">
          <el-input
            v-model="formData.description"
            type="textarea"
            :rows="3"
            placeholder="请描述本次变更内容"
          />
        </el-form-item>
        <el-form-item label="变更类型" prop="type">
          <el-radio-group v-model="formData.type">
            <el-radio label="内容调整">内容调整</el-radio>
            <el-radio label="结构优化">结构优化</el-radio>
            <el-radio label="错误修正">错误修正</el-radio>
            <el-radio label="其他">其他</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="变更范围" prop="scope">
          <el-radio-group v-model="formData.scope">
            <el-radio label="培养目标">培养目标</el-radio>
            <el-radio label="毕业要求">毕业要求</el-radio>
            <el-radio label="指标点">指标点</el-radio>
            <el-radio label="支撑矩阵">支撑矩阵</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Edit, SuccessFilled } from '@element-plus/icons-vue'

const programName = ref('计算机科学与技术')

const historyList = ref([
  {
    id: '1',
    version: '2.0',
    time: '2024-01-15 14:30:00',
    status: '已发布',
    operator: 'admin',
    description: '新增培养目标3，调整毕业要求2的描述，优化支撑矩阵',
    type: '结构优化',
    scope: '培养目标、毕业要求'
  },
  {
    id: '2',
    version: '1.5',
    time: '2023-12-20 10:15:00',
    status: '已发布',
    operator: 'admin',
    description: '修正指标点1.2的描述错误，调整权重分配',
    type: '错误修正',
    scope: '指标点'
  },
  {
    id: '3',
    version: '1.2',
    time: '2023-11-05 09:00:00',
    status: '已发布',
    operator: 'admin',
    description: '初始版本发布',
    type: '内容调整',
    scope: '全部'
  },
  {
    id: '4',
    version: '2.1',
    time: '2024-01-20 16:20:00',
    status: '草稿',
    operator: 'admin',
    description: '调整毕业要求4的描述，新增指标点4.3',
    type: '内容调整',
    scope: '毕业要求、指标点'
  },
])

const dialogVisible = ref(false)
const formRef = ref(null)

const formData = reactive({
  version: '',
  description: '',
  type: '内容调整',
  scope: '培养目标'
})

const formRules = {
  version: [{ required: true, message: '请输入版本号', trigger: 'blur' }],
  description: [{ required: true, message: '请输入变更说明', trigger: 'blur' }]
}

const handleNewVersion = () => {
  const lastVersion = historyList.value[0]?.version || '1.0'
  const parts = lastVersion.split('.')
  const newVersion = `${parts[0]}.${parseInt(parts[1]) + 1}`
  formData.version = newVersion
  dialogVisible.value = true
}

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return
    const newVersion = {
      id: String(Date.now()),
      version: formData.version,
      time: new Date().toLocaleString('zh-CN'),
      status: '草稿',
      operator: 'admin',
      description: formData.description,
      type: formData.type,
      scope: formData.scope
    }
    historyList.value.unshift(newVersion)
    ElMessage.success('新版本创建成功')
    dialogVisible.value = false
  })
}

const handleViewDetail = (item) => {
  ElMessage.info(`查看版本 ${item.version} 详情`)
}

const handlePublish = (item) => {
  ElMessageBox.confirm('确定要发布该版本吗？', '提示', { type: 'warning' }).then(() => {
    item.status = '已发布'
    ElMessage.success('发布成功')
  })
}

const handleDeleteVersion = (id) => {
  ElMessageBox.confirm('确定要删除该版本吗？', '提示', { type: 'warning' }).then(() => {
    const index = historyList.value.findIndex(item => item.id === id)
    if (index !== -1) {
      historyList.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  })
}

onMounted(() => {})
</script>

<style scoped>
.curriculum-history-container { padding: 20px; }

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

.version-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.version-tag {
  font-weight: 700;
  font-size: 16px;
  color: #409EFF;
}
.version-operator { color: #999; font-size: 13px; margin-left: auto; }
.version-content p { margin: 4px 0; color: #555; line-height: 1.6; }
.version-details {
  display: flex;
  gap: 24px;
  margin-top: 8px;
  font-size: 13px;
  color: #888;
}
.version-actions {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  gap: 8px;
}
</style>