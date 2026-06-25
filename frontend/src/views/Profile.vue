<template>
  <div class="profile-container">
    <el-card class="profile-card">
      <template #header>
        <div class="card-header">
          <span>个人信息</span>
          <el-button type="primary" :icon="Edit" @click="handleEdit">
            编辑信息
          </el-button>
        </div>
      </template>

      <div class="profile-content">
        <!-- 左侧：头像区 -->
        <div class="profile-avatar">
          <el-avatar :size="120" :src="userInfo.avatar || ''">
            <span class="avatar-text">{{ userInfo.name?.charAt(0) }}</span>
          </el-avatar>
          <h3>{{ userInfo.name }}</h3>
        </div>

        <!-- 右侧：信息列表 -->
        <div class="profile-info">
          <el-descriptions :column="2" border>
            <!-- 共同字段 -->
            <el-descriptions-item label="工号/学号">
              {{ userInfo.id }}
            </el-descriptions-item>
            <el-descriptions-item label="姓名">
              {{ userInfo.name }}
            </el-descriptions-item>

            <!-- 教师特有字段 -->
            <template v-if="userInfo.role === '教师'">
              <el-descriptions-item label="院系">
                {{ userInfo.department }}
              </el-descriptions-item>
              <el-descriptions-item label="职称">
                {{ userInfo.title }}
              </el-descriptions-item>
              <el-descriptions-item label="办公地点">
                {{ userInfo.office }}
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">
                {{ userInfo.email }}
              </el-descriptions-item>
              <el-descriptions-item label="联系电话">
                {{ userInfo.phone }}
              </el-descriptions-item>
            </template>

            <!-- 学生特有字段 -->
            <template v-if="userInfo.role === '学生'">
              <el-descriptions-item label="专业">
                {{ userInfo.major }}
              </el-descriptions-item>
              <el-descriptions-item label="年级">
                {{ userInfo.grade }}
              </el-descriptions-item>
              <el-descriptions-item label="班级">
                {{ userInfo.class }}
              </el-descriptions-item>
              <el-descriptions-item label="联系电话">
                {{ userInfo.phone }}
              </el-descriptions-item>
              <el-descriptions-item label="邮箱">
                {{ userInfo.email }}
              </el-descriptions-item>
            </template>
          </el-descriptions>
        </div>
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      title="编辑信息"
      width="460px"
      @close="resetForm"
    >
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="90px">
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue';
import { Edit } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';

// 模拟当前用户角色（实际从 store 获取）
const userRole = ref('学生'); // 或 '教师'

const userInfo = ref({
  id: '2024001',
  name: '张明',
  role: '学生',
  major: '计算机科学与技术',
  grade: '2024级',
  class: '1班',
  phone: '138****1234',
  email: 'zhangming@edu.cn'
});

// 教师数据示例
const teacherInfo = ref({
  id: 'T2024001',
  name: '王教授',
  role: '教师',
  department: '计算机学院',
  title: '副教授',
  office: '计算机楼 A305',
  phone: '139****5678',
  email: 'wang@edu.cn'
});

const dialogVisible = ref(false);
const formRef = ref(null);
const formData = reactive({
  phone: '',
  email: ''
});

const formRules = {
  phone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
};

onMounted(() => {
  // 实际从 API 获取用户信息
  // userInfo.value = await getUserInfo()
  // 根据角色加载不同数据
  const currentRole = localStorage.getItem('userRole') || '学生';
  const currentName = localStorage.getItem('username');
  userRole.value = currentRole;
  if (currentRole === '教师') {
    userInfo.value = {
      ...teacherInfo.value,
      name: currentName || teacherInfo.value.name
    };
  } else {
    userInfo.value = {
      ...userInfo.value,
      name: currentName || userInfo.value.name
    };
  }
});

const handleEdit = () => {
  formData.phone = userInfo.value.phone;
  formData.email = userInfo.value.email;
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value.validate((valid) => {
    if (!valid) return;
    userInfo.value.phone = formData.phone;
    userInfo.value.email = formData.email;
    dialogVisible.value = false;
    ElMessage.success('保存成功');
  });
};

const resetForm = () => {
  formRef.value?.resetFields();
};
</script>

<style scoped>
.profile-container {
  padding: 20px;
}
.profile-card {
  width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.profile-content {
  display: flex;
  gap: 40px;
  padding: 20px 0;
}
.profile-avatar {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  min-width: 150px;
}
.avatar-text {
  font-size: 36px;
  font-weight: 700;
}
.profile-info {
  flex: 1;
}
:deep(.el-descriptions) {
  width: 100%;
}
</style>
