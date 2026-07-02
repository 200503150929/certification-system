<template>
  <div class="student-import">
    <!-- 统计和工具栏 -->
    <div class="import-toolbar">
      <div class="toolbar-left">
        <span class="total-count">共 {{ studentList.length }} 名学生</span>
        <el-tag v-if="studentList.length > 0" type="info" size="small">
          已导入
        </el-tag>
      </div>
      <div class="toolbar-right">
        <el-button type="primary" :icon="Upload" @click="openImportDialog">
          导入学生名单
        </el-button>
        <el-button
            type="danger"
            plain
            :icon="Delete"
            :disabled="selectedIds.length === 0"
            @click="handleBatchRemove"
        >
          批量移除 ({{ selectedIds.length }})
        </el-button>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="search-bar">
      <el-input
          v-model="keyword"
          placeholder="搜索姓名/学号"
          style="width: 260px"
          clearable
          @input="handleSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 学生列表 -->
    <el-table
        :data="filteredList"
        border
        v-loading="loading"
        style="width: 100%"
        @selection-change="handleSelectionChange"
        max-height="500"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="studentNo" label="学号" width="140" />
      <el-table-column prop="studentName" label="姓名" width="120" />
      <el-table-column label="最终成绩" width="140" align="center">
        <template #default="scope">
          <span v-if="scope.row.totalScore != null" style="font-weight: 600; color: #303133">
            {{ scope.row.totalScore }}
          </span>
          <span v-else style="color: #c0c4cc">未录入</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120" fixed="right" align="center">
        <template #default="scope">
          <el-button link type="danger" size="small" @click="handleRemove(scope.row)">
            移除
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 空状态 -->
    <el-empty
        v-if="!loading && studentList.length === 0"
        description="暂无学生，请导入"
        :image-size="120"
    >
      <el-button type="primary" @click="openImportDialog">导入学生名单</el-button>
    </el-empty>

    <!-- 导入弹窗 -->
    <el-dialog
        v-model="importDialogVisible"
        title="导入学生名单"
        width="560px"
        destroy-on-close
        @close="handleDialogClose"
    >
      <el-tabs v-model="importTab" class="import-tabs">
        <!-- Excel 导入 -->
        <el-tab-pane label="Excel 导入" name="excel">
          <div class="import-excel">
            <el-upload
                ref="uploadRef"
                drag
                :action="`/api/teacher/student-course/import-excel/${offeringId}`"
                :headers="uploadHeaders"
                :on-success="handleUploadSuccess"
                :on-error="handleUploadError"
                :before-upload="beforeUpload"
                accept=".xlsx,.xls"
                :limit="1"
            >
              <el-icon class="upload-icon"><UploadFilled /></el-icon>
              <div class="el-upload__text">
                将文件拖到此处，或 <em>点击上传</em>
              </div>
              <template #tip>
                <div class="el-upload__tip">
                  <el-icon><InfoFilled /></el-icon>
                  支持 .xlsx / .xls 格式，第一列为学号（必填），第二列为姓名（选填）
                </div>
              </template>
            </el-upload>
            <div class="import-template">
              <el-button link type="primary" @click="downloadTemplate">
                <el-icon><Download /></el-icon>
                下载导入模板
              </el-button>
            </div>
          </div>
        </el-tab-pane>

        <!-- 手动输入 -->
        <el-tab-pane label="手动输入学号" name="manual">
          <div class="import-manual">
            <div class="manual-header">
              <span class="manual-label">请输入学号（每行一个）：</span>
              <el-button link type="primary" @click="pasteFromClipboard">
                <el-icon><Document /></el-icon>
                从剪贴板粘贴
              </el-button>
            </div>
            <el-input
                v-model="manualStudentNos"
                type="textarea"
                :rows="10"
                placeholder="每行一个学号，例如：&#10;2024001&#10;2024002&#10;2024003&#10;&#10;支持从 Excel 直接复制粘贴"
                class="manual-textarea"
            />
            <div class="manual-footer">
              <span class="manual-count">
                共 <strong>{{ manualStudentCount }}</strong> 个学号待导入
                <el-tag v-if="manualStudentCount > 0" size="small" type="success">
                  有效
                </el-tag>
                <el-tag v-else size="small" type="info">
                  请输入学号
                </el-tag>
              </span>
              <el-button
                  type="primary"
                  @click="handleManualImport"
                  :loading="importing"
                  :disabled="manualStudentCount === 0"
              >
                <el-icon><Check /></el-icon>
                确认导入 ({{ manualStudentCount }})
              </el-button>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="importDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Upload,
  Delete,
  Search,
  UploadFilled,
  InfoFilled,
  Download,
  Document,
  Check
} from '@element-plus/icons-vue'
import { getStudentList, importStudents, removeStudent } from '@/api/studentCourse'
// ==================== 新增：引入 xlsx 库 ====================
import * as XLSX from 'xlsx'

// ==================== Props ====================
const props = defineProps({
  offeringId: {
    type: [String, Number],
    required: true
  }
})

// ==================== Emits ====================
const emit = defineEmits(['refresh', 'student-count-change'])

// ==================== 状态 ====================
const loading = ref(false)
const importing = ref(false)
const studentList = ref([])
const keyword = ref('')
const selectedIds = ref([])

// ==================== 导入弹窗 ====================
const importDialogVisible = ref(false)
const importTab = ref('excel')
const manualStudentNos = ref('')
const uploadRef = ref(null)

// ==================== 计算属性 ====================
const filteredList = computed(() => {
  if (!keyword.value.trim()) return studentList.value
  const kw = keyword.value.trim().toLowerCase()
  return studentList.value.filter(s =>
      s.studentNo?.toLowerCase().includes(kw) ||
      s.studentName?.toLowerCase().includes(kw)
  )
})

const manualStudentCount = computed(() => {
  return manualStudentNos.value.split('\n')
      .map(s => s.trim())
      .filter(s => s !== '')
      .length
})

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${localStorage.getItem('token')}`
}))

// ==================== 加载数据 ====================
const loadStudents = async () => {
  loading.value = true
  try {
    const res = await getStudentList(props.offeringId)
    if (res.status === 'success' && res.data) {
      studentList.value = res.data
      emit('student-count-change', studentList.value.length)
    }
  } catch (e) {
    studentList.value = []
  } finally {
    loading.value = false
  }
}

// ==================== 搜索 ====================
const handleSearch = () => {
  // 计算属性自动过滤
}

// ==================== 选择 ====================
const handleSelectionChange = (selection) => {
  selectedIds.value = selection.map(s => s.studentId)
}

// ==================== 导入弹窗 ====================
const openImportDialog = () => {
  manualStudentNos.value = ''
  importTab.value = 'excel'
  importDialogVisible.value = true
  nextTick(() => {
    uploadRef.value?.clearFiles()
  })
}

const handleDialogClose = () => {
  uploadRef.value?.clearFiles()
  manualStudentNos.value = ''
}

// ==================== Excel 导入 ====================
const beforeUpload = (file) => {
  const isExcel = file.type === 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' ||
      file.type === 'application/vnd.ms-excel'
  if (!isExcel) {
    ElMessage.error('请上传 Excel 文件 (.xlsx / .xls)')
    return false
  }
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  return true
}

const handleUploadSuccess = (response) => {
  if (response.status === 'success') {
    const data = response.data || {}
    const successCount = data.successCount || 0
    const totalCount = data.totalCount || 0
    const failedList = data.failedStudentNos || []

    if (successCount > 0) {
      ElMessage.success(`成功导入 ${successCount} 名学生`)
    }
    if (failedList.length > 0) {
      ElMessage.warning(`以下 ${failedList.length} 个学号不存在或已存在：${failedList.join('、')}`)
    }
    if (successCount === 0 && totalCount > 0) {
      ElMessage.warning('未导入任何学生，请检查学号是否正确')
    }

    importDialogVisible.value = false
    loadStudents()
    emit('refresh')
  } else {
    ElMessage.error(response.message || '导入失败')
  }
}

const handleUploadError = () => {
  ElMessage.error('导入失败，请检查文件格式或网络')
}

// ==================== 手动导入 ====================
const pasteFromClipboard = async () => {
  try {
    const text = await navigator.clipboard.readText()
    if (text) {
      manualStudentNos.value = text
      ElMessage.success('已粘贴剪贴板内容')
    }
  } catch (e) {
    ElMessage.warning('无法读取剪贴板，请手动粘贴 (Ctrl+V)')
  }
}

const handleManualImport = async () => {
  const nos = manualStudentNos.value.split('\n')
      .map(s => s.trim())
      .filter(s => s !== '')

  if (nos.length === 0) {
    ElMessage.warning('请输入学号')
    return
  }

  // 去重
  const uniqueNos = [...new Set(nos)]
  if (uniqueNos.length < nos.length) {
    ElMessage.warning(`检测到 ${nos.length - uniqueNos.length} 个重复学号，已自动去重`)
  }

  importing.value = true
  try {
    const res = await importStudents(props.offeringId, uniqueNos)
    if (res.status === 'success') {
      const data = res.data || {}
      const successCount = data.successCount || 0
      const failedList = data.failedStudentNos || []

      if (successCount > 0) {
        ElMessage.success(`成功导入 ${successCount} 名学生`)
      }
      if (failedList.length > 0) {
        ElMessage.warning(`以下 ${failedList.length} 个学号不存在或已存在：${failedList.join('、')}`)
      }
      if (successCount === 0) {
        ElMessage.warning('未导入任何学生，请检查学号是否正确')
      }

      importDialogVisible.value = false
      loadStudents()
      emit('refresh')
    }
  } catch (e) {
    // 拦截器已处理
  } finally {
    importing.value = false
  }
}

// ==================== 下载模板（XLSX 格式） ====================
const downloadTemplate = () => {
  try {
    // 准备数据：表头 + 示例数据
    const data = [
      ['学号', '姓名'],
      ['202401001', '张三'],
      ['202401002', '李四'],
      ['202401003', '王五']
    ]

    // 创建工作簿和工作表
    const wb = XLSX.utils.book_new()
    const ws = XLSX.utils.aoa_to_sheet(data)

    // 设置列宽
    ws['!cols'] = [
      { wch: 18 }, // 学号列宽
      { wch: 15 }  // 姓名列宽
    ]

    // 将工作表添加到工作簿
    XLSX.utils.book_append_sheet(wb, ws, '学生名单导入模板')

    // 生成 Excel 文件（ArrayBuffer）
    const wbout = XLSX.write(wb, {
      bookType: 'xlsx',
      type: 'array'
    })

    // 创建 Blob 并下载
    const blob = new Blob([wbout], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '学生名单导入模板.xlsx'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
    URL.revokeObjectURL(url)

    ElMessage.success('模板下载成功')
  } catch (error) {
    console.error('下载模板失败:', error)
    ElMessage.error('模板下载失败，请检查是否已安装 xlsx 库')
  }
}

// ==================== 移除学生 ====================
const handleRemove = (row) => {
  ElMessageBox.confirm(
      `确定要将学生 "${row.studentName} (${row.studentNo})" 移出该课程吗？`,
      '移除确认',
      {
        type: 'warning',
        confirmButtonText: '确定移除',
        cancelButtonText: '取消'
      }
  ).then(async () => {
    try {
      await removeStudent(props.offeringId, row.studentId)
      ElMessage.success('移除成功')
      loadStudents()
      emit('refresh')
    } catch (e) {
      // 拦截器已处理
    }
  }).catch(() => {})
}

// ==================== 批量移除 ====================
const handleBatchRemove = () => {
  if (selectedIds.value.length === 0) return

  const selectedStudents = studentList.value.filter(s => selectedIds.value.includes(s.studentId))
  const names = selectedStudents.map(s => `${s.studentName}(${s.studentNo})`).join('、')

  ElMessageBox.confirm(
      `确定要移除以下 ${selectedStudents.length} 名学生吗？<br><br>${names}`,
      '批量移除确认',
      {
        dangerouslyUseHTMLString: true,
        type: 'warning',
        confirmButtonText: '确定移除',
        cancelButtonText: '取消'
      }
  ).then(async () => {
    try {
      let successCount = 0
      let failCount = 0
      for (const studentId of selectedIds.value) {
        try {
          await removeStudent(props.offeringId, studentId)
          successCount++
        } catch (e) {
          failCount++
        }
      }
      if (successCount > 0) {
        ElMessage.success(`成功移除 ${successCount} 名学生${failCount > 0 ? `，${failCount} 名移除失败` : ''}`)
      } else {
        ElMessage.error('移除失败，请检查学生是否已有成绩')
      }
      selectedIds.value = []
      loadStudents()
      emit('refresh')
    } catch (e) {
      // 拦截器已处理
    }
  }).catch(() => {})
}

// ==================== 初始化 ====================
onMounted(() => {
  loadStudents()
})

// ==================== 暴露方法 ====================
defineExpose({
  loadStudents,
  studentList
})
</script>

<style scoped>
.student-import {
  padding: 4px 0;
}

.import-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.total-count {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}

.toolbar-right {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.search-bar {
  margin-bottom: 16px;
}

/* 导入弹窗样式 */
.import-tabs {
  padding: 4px 0;
}

.import-excel {
  padding: 16px 0;
}

.import-excel .upload-icon {
  font-size: 48px;
  color: #409EFF;
}

.import-excel .el-upload__tip {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  color: #909399;
  font-size: 13px;
}

.import-template {
  margin-top: 16px;
  text-align: center;
}

.import-manual {
  padding: 8px 0;
}

.manual-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.manual-label {
  font-size: 14px;
  color: #606266;
}

.manual-textarea :deep(.el-textarea__inner) {
  font-family: 'Courier New', monospace;
  font-size: 14px;
  line-height: 1.8;
}

.manual-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #e8ecf1;
}

.manual-count {
  font-size: 14px;
  color: #606266;
}

.manual-count strong {
  color: #409EFF;
  font-size: 18px;
}

/* 响应式 */
@media (max-width: 768px) {
  .import-toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: stretch;
  }

  .toolbar-right .el-button {
    flex: 1;
  }

  .manual-footer {
    flex-direction: column;
    gap: 12px;
  }

  .manual-footer .el-button {
    width: 100%;
  }
}
</style>