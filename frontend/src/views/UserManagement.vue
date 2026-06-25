<template>
  <div class="user-management-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户账号管理</span>
          <div>
            <el-input v-model="searchKeyword" placeholder="搜索工号/姓名" class="search-input"></el-input>
            <el-select v-model="selectedRole" placeholder="角色类别" clearable class="role-select">
              <el-option label="管理员" value="管理员" />
              <el-option label="教师" value="教师" />
              <el-option label="学生" value="学生" />
            </el-select>
            <el-button type="primary" :icon="Upload">批量导入</el-button>
            <el-button type="primary" :icon="Plus" @click="openAddDialog">新增用户</el-button>
          </div>
        </div>
      </template>

      <!-- 去掉 border 属性，只保留横向分隔线 -->
      <el-table :data="filteredTableData" style="width: 100%">
        <el-table-column prop="id" label="工号" width="100"></el-table-column>
        <el-table-column prop="name" label="姓名" width="100"></el-table-column>
        <el-table-column prop="role" label="角色" width="110">
          <template #default="scope">
            <el-tag :type="getRoleTagType(scope.row.role)">{{ scope.row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="major" label="专业" min-width="150"></el-table-column>
        <el-table-column prop="grade" label="年级" width="100"></el-table-column>
        <el-table-column prop="phone" label="手机号" width="130"></el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="scope">
            <el-tag :type="scope.row.status === '正常' ? 'success' : 'danger'">{{ scope.row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220" fixed="right">
          <template #default="scope">
            <el-button link type="primary" :icon="Edit" @click="openEditDialog(scope.row)">编辑</el-button>
            <el-button link type="primary" :icon="Refresh">重置密码</el-button>
            <el-button link type="danger" :icon="Delete" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="128"
          :page-size="10"
        ></el-pagination>
      </div>

      <div class="bottom-sections">
        <el-card class="upload-section">
          <template #header>批量导入用户</template>
          <div class="upload-box">
            <el-icon><UploadFilled /></el-icon>
            <p>点击或将 Excel 文件拖拽到此处上传</p>
            <small>支持 .xls, .xlsx 格式，文件大小不超过10MB</small>
            <el-button type="text">下载导入模板</el-button>
          </div>
        </el-card>
        <el-card class="tips-section">
          <template #header>账号管理说明</template>
          <ul>
            <li>管理员可修改所有用户的密码、角色和状态。</li>
            <li>学生和教师的角色可批量导入，一个用户可拥有多个角色和班级。</li>
            <li>新创建的用户的初始密码统一为 Edu@2026，首次登录需强制修改。</li>
          </ul>
        </el-card>
      </div>
    </el-card>

    <el-dialog
      v-model="addDialogVisible"
      :title="dialogTitle"
      width="520px"
      @close="resetAddForm"
    >
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-form-item label="工号" prop="id">
          <el-input v-model="addForm.id" placeholder="请输入工号" />
        </el-form-item>
        <el-form-item label="姓名" prop="name">
          <el-input v-model="addForm.name" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="addForm.role" placeholder="请选择角色" style="width: 100%">
            <el-option label="管理员" value="管理员" />
            <el-option label="教师" value="教师" />
            <el-option label="学生" value="学生" />
          </el-select>
        </el-form-item>
        <el-form-item label="专业" prop="major">
          <el-input v-model="addForm.major" placeholder="请输入专业" />
        </el-form-item>
        <el-form-item label="年级" prop="grade">
          <el-input v-model="addForm.grade" placeholder="请输入年级，教师/管理员可填 -" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="addForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="addForm.status">
            <el-radio label="正常">正常</el-radio>
            <el-radio label="已禁用">已禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitAddForm">{{ submitButtonText }}</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import { Plus, Upload, Edit, Delete, Refresh, UploadFilled } from '@element-plus/icons-vue';
import { ElMessage, ElMessageBox } from 'element-plus';

const route = useRoute();
const searchKeyword = ref('');
const selectedRole = ref('');
const addDialogVisible = ref(false);
const addFormRef = ref(null);
const isEdit = ref(false);
const editingId = ref('');

const addForm = reactive({
  id: '',
  name: '',
  role: '',
  major: '',
  grade: '',
  phone: '',
  status: '正常',
});

const addRules = {
  id: [{ required: true, message: '请输入工号', trigger: 'blur' }],
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  major: [{ required: true, message: '请输入专业', trigger: 'blur' }],
  grade: [{ required: true, message: '请输入年级', trigger: 'blur' }],
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }],
};

const tableData = ref([
  { id: '1001', name: '张萌', role: '管理员', major: '计算机科学', grade: '-', phone: '1380001', status: '正常' },
  { id: '1002', name: '李雪', role: '教师', major: '软件工程', grade: '-', phone: '1380002', status: '正常' },
  { id: '1003', name: '王强', role: '教师', major: '数据科学', grade: '-', phone: '1380003', status: '正常' },
  { id: '1004', name: '赵瑶', role: '学生', major: '计算机科学', grade: '2023届', phone: '1380004', status: '正常' },
  { id: '1005', name: '刘芳', role: '学生', major: '软件工程', grade: '2023届', phone: '1380005', status: '正常' },
  { id: '1006', name: '陈杰', role: '学生', major: '数据科学', grade: '2024届', phone: '1380006', status: '正常' },
  { id: '1007', name: '周敏', role: '学生', major: '计算机科学', grade: '2024届', phone: '1380007', status: '已禁用' },
  { id: '1008', name: '吴涛', role: '教师', major: '-', grade: '-', phone: '1380008', status: '正常' },
]);

const filteredTableData = computed(() => {
  const keyword = searchKeyword.value.trim();
  return tableData.value.filter((item) => {
    const matchesKeyword = !keyword || item.id.includes(keyword) || item.name.includes(keyword);
    const matchesRole = !selectedRole.value || item.role === selectedRole.value;
    return matchesKeyword && matchesRole;
  });
});

const dialogTitle = computed(() => isEdit.value ? '编辑用户' : '新增用户');
const submitButtonText = computed(() => isEdit.value ? '保存修改' : '确认导入');

const getRoleTagType = (role) => {
  if (role === '管理员') return 'primary';
  if (role === '教师') return 'success';
  if (role === '学生') return 'warning';
  return 'info';
};

const openAddDialog = () => {
  isEdit.value = false;
  editingId.value = '';
  addDialogVisible.value = true;
};

const openEditDialog = (row) => {
  isEdit.value = true;
  editingId.value = row.id;
  Object.assign(addForm, row);
  addDialogVisible.value = true;
};

const submitAddForm = () => {
  addFormRef.value.validate((valid) => {
    if (!valid) return;
    if (isEdit.value) {
      const index = tableData.value.findIndex(item => item.id === editingId.value);
      if (index !== -1) {
        tableData.value[index] = { ...addForm };
      }
      ElMessage.success('用户信息已更新');
    } else {
      tableData.value.unshift({ ...addForm });
      ElMessage.success('用户导入成功');
    }
    addDialogVisible.value = false;
  });
};

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除用户 ${row.name} 吗？`, '删除确认', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
  }).then(() => {
    const index = tableData.value.findIndex(item => item.id === row.id);
    if (index !== -1) {
      tableData.value.splice(index, 1);
      ElMessage.success('删除成功');
    }
  }).catch(() => {});
};

const resetAddForm = () => {
  addFormRef.value?.resetFields();
  Object.assign(addForm, {
    id: '',
    name: '',
    role: '',
    major: '',
    grade: '',
    phone: '',
    status: '正常',
  });
  isEdit.value = false;
  editingId.value = '';
};

const applyRoleQuery = () => {
  const role = route.query.role;
  selectedRole.value = ['管理员', '教师', '学生'].includes(role) ? role : '';
};

watch(() => route.query.role, applyRoleQuery);

onMounted(() => {
  applyRoleQuery();
});
</script>

<style scoped>
.user-management-container {
  padding: 20px;
  width: 100%;
  box-sizing: border-box;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
}
.search-input {
  width: 200px;
  margin-right: 10px;
}
.role-select {
  width: 120px;
  margin-right: 10px;
}
.pagination-container {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
.bottom-sections {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-top: 20px;
}
.upload-section .upload-box {
  text-align: center;
  padding: 40px 0;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
}
.upload-box .el-icon {
  font-size: 67px;
  color: #c0c4cc;
  margin-bottom: 16px;
}
.tips-section ul {
  list-style-type: disc;
  padding-left: 20px;
}
.tips-section li {
  margin-bottom: 10px;
  color: #606266;
}

/* ========== 只保留横向分隔线，去掉竖向分隔线 ========== */
:deep(.el-table) {
  width: 100% !important;
}

/* 去掉所有竖向边框（列分隔线） */
:deep(.el-table__body-wrapper .el-table__cell),
:deep(.el-table__header-wrapper .el-table__cell),
:deep(.el-table__fixed-right .el-table__cell) {
  border-right: none !important;
  border-left: none !important;
}

/* 保留横向边框（行分隔线）- 底部边框 */
:deep(.el-table__body-wrapper .el-table__row) {
  border-bottom: 1px solid #ebeef5 !important;
}

/* 表头底部边框加粗突出 */
:deep(.el-table__header-wrapper) {
  border-bottom: 2px solid #ebeef5 !important;
}

/* 最后一行去掉底部边框（避免与表格底部重复） */
:deep(.el-table__body-wrapper .el-table__row:last-child) {
  border-bottom: none !important;
}

/* 去掉表格外部边框 */
:deep(.el-table) {
  border: none !important;
}
:deep(.el-table__inner-wrapper) {
  border: none !important;
}
</style>
