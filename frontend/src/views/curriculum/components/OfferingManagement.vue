<template>
  <el-dialog
      v-model="dialogVisible"
      :title="`开课管理 - ${courseName}`"
      width="900px"
      @close="handleClose"
      destroy-on-close
  >
    <div class="offering-management">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button type="primary" :icon="Plus" @click="openAddDialog">
          新增开课
        </el-button>
        <span class="tip-text">为课程分配授课教师，每个学年学期同一教师只能开课一次</span>
      </div>

      <!-- 开课记录列表 -->
      <el-table :data="offeringList" border v-loading="loading" style="width: 100%">
        <el-table-column prop="academicYear" label="学年" width="140" />
        <el-table-column prop="semester" label="学期" width="100" />
        <el-table-column label="授课教师" min-width="120">
          <template #default="scope">
            <span v-if="scope.row.teacherName">{{ scope.row.teacherName }}</span>
            <span v-else style="color: #909399">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <el-button link type="primary" @click="openEditDialog(scope.row)">
              编辑
            </el-button>
            <el-button link type="danger" @click="handleDelete(scope.row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <el-pagination
            background
            layout="total, prev, pager, next"
            :total="total"
            :page-size="pageSize"
            v-model:current-page="pageNum"
            @current-change="loadOfferings"
        />
      </div>

      <!-- 空状态 -->
      <el-empty v-if="!loading && offeringList.length === 0" description="暂无开课记录" />
    </div>

    <!-- 新增/编辑开课弹窗 -->
    <el-dialog
        v-model="formDialogVisible"
        :title="formDialogTitle"
        width="500px"
        @close="resetForm"
        append-to-body
    >
      <el-form ref="formRef" :model="form" :rules="formRules" label-width="100px">
        <el-form-item label="学年" prop="academicYear">
          <el-input v-model="form.academicYear" placeholder="如 2025-2026" />
        </el-form-item>
        <el-form-item label="学期" prop="semester">
          <el-select v-model="form.semester" placeholder="请选择学期" style="width: 100%">
            <el-option label="第1学期" value="第1学期" />
            <el-option label="第2学期" value="第2学期" />
          </el-select>
        </el-form-item>
        <el-form-item label="授课教师" prop="teacherId">
          <el-select
              v-model="form.teacherId"
              placeholder="请选择授课教师"
              filterable
              style="width: 100%"
          >
            <el-option
                v-for="teacher in teacherList"
                :key="teacher.id"
                :label="teacher.name + (teacher.college ? ' (' + teacher.college + ')' : '')"
                :value="teacher.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm" :loading="submitting">
          确认{{ isEdit ? '修改' : '添加' }}
        </el-button>
      </template>
    </el-dialog>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '@/api/request'

// ==================== Props ====================
const props = defineProps({
  visible: {
    type: Boolean,
    default: false
  },
  courseId: {
    type: [String, Number],
    required: true
  },
  courseName: {
    type: String,
    default: ''
  }
})

// ==================== Emits ====================
const emit = defineEmits(['update:visible', 'success'])

// ==================== 弹窗可见性 ====================
const dialogVisible = computed({
  get: () => props.visible,
  set: (val) => emit('update:visible', val)
})

// ==================== 状态 ====================
const loading = ref(false)
const submitting = ref(false)
const offeringList = ref([])
const teacherList = ref([])
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)

// ==================== 表单 ====================
const formDialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const form = ref({
  id: null,
  academicYear: '',
  semester: '',
  teacherId: null
})

const formRules = {
  academicYear: [
    { required: true, message: '请输入学年', trigger: 'blur' },
    { pattern: /^\d{4}-\d{4}$/, message: '格式如 2025-2026', trigger: 'blur' }
  ],
  semester: [
    { required: true, message: '请选择学期', trigger: 'change' }
  ],
  teacherId: [
    { required: true, message: '请选择授课教师', trigger: 'change' }
  ]
}

const formDialogTitle = computed(() => isEdit.value ? '编辑开课记录' : '新增开课记录')

// ==================== 加载数据 ====================
const loadOfferings = async () => {
  if (!props.courseId) return
  loading.value = true
  try {
    const res = await request.get(`/admin/course/offering/list/${props.courseId}`, {
      params: {
        pageNum: pageNum.value,
        pageSize: pageSize.value
      }
    })
    if (res.status === 'success' && res.data) {
      offeringList.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } catch (e) {
    // 拦截器已处理
    offeringList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

const loadTeachers = async () => {
  try {
    const res = await request.get('/admin/course/offering/teachers')
    if (res.status === 'success' && res.data) {
      teacherList.value = res.data || []
    }
  } catch (e) {
    teacherList.value = []
  }
}

// ==================== 打开弹窗 ====================
const openAddDialog = () => {
  isEdit.value = false
  form.value = {
    id: null,
    academicYear: '',
    semester: '',
    teacherId: null
  }
  // 预填默认学年
  const currentYear = new Date().getFullYear()
  form.value.academicYear = `${currentYear}-${currentYear + 1}`
  formDialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

const openEditDialog = (row) => {
  isEdit.value = true
  form.value = {
    id: row.id,
    academicYear: row.academicYear || '',
    semester: row.semester || '',
    teacherId: row.teacherId || null
  }
  formDialogVisible.value = true
  nextTick(() => {
    formRef.value?.clearValidate()
  })
}

// ==================== 提交表单 ====================
const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    submitting.value = true
    try {
      const payload = {
        courseId: Number(props.courseId),
        teacherId: form.value.teacherId,
        academicYear: form.value.academicYear,
        semester: form.value.semester
      }

      if (isEdit.value) {
        payload.id = form.value.id
        await request.put('/admin/course/offering/update', payload)
        ElMessage.success('开课记录已更新')
      } else {
        await request.post('/admin/course/offering/add', payload)
        ElMessage.success('开课记录已添加')
      }

      formDialogVisible.value = false
      loadOfferings()
      emit('success')
    } catch (error) {
      // 拦截器已处理，但我们需要显示更友好的错误信息
      // 如果拦截器没有显示错误信息，在这里兜底
      const message = error?.message || error?.info || '操作失败，请重试'
      ElMessage.error(message)
    } finally {
      submitting.value = false
    }
  })
}

// ==================== 删除 ====================
const handleDelete = (row) => {
  // 检查是否有学生选课（后端会校验，前端提前提示）
  ElMessageBox.confirm(
      `确定要删除该开课记录吗？<br><br>
    学年：${row.academicYear}<br>
    学期：${row.semester}<br>
    教师：${row.teacherName || '未分配'}<br><br>
    ⚠️ 如果已有学生选课，将无法删除。`,
      '删除确认',
      {
        dangerouslyUseHTMLString: true,
        type: 'warning',
        confirmButtonText: '确定删除',
        cancelButtonText: '取消'
      }
  ).then(async () => {
    try {
      await request.delete(`/admin/course/offering/delete/${row.id}`)
      ElMessage.success('删除成功')
      loadOfferings()
      emit('success')
    } catch (error) {
      const message = error?.message || error?.info || '删除失败，请重试'
      ElMessage.error(message)
    }
  }).catch(() => {})
}

// ==================== 重置表单 ====================
const resetForm = () => {
  nextTick(() => {
    formRef.value?.resetFields()
  })
  form.value = {
    id: null,
    academicYear: '',
    semester: '',
    teacherId: null
  }
  isEdit.value = false
}

// ==================== 关闭弹窗 ====================
const handleClose = () => {
  emit('update:visible', false)
}

// ==================== 监听 visible 变化 ====================
watch(() => props.visible, (newVal) => {
  if (newVal) {
    pageNum.value = 1
    loadOfferings()
    loadTeachers()
  }
}, { immediate: true })

// ==================== 暴露方法 ====================
defineExpose({
  loadOfferings,
  loadTeachers
})
</script>

<style scoped>
.offering-management {
  min-height: 300px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}

.tip-text {
  font-size: 13px;
  color: #909399;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

:deep(.el-dialog__body) {
  padding: 20px 20px 10px 20px;
}
</style>