<template>
  <div class="profile-container">
    <el-card class="profile-card" v-loading="loading">
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
            <span class="avatar-text">{{ userInfo.name?.charAt(0) || 'U' }}</span>
          </el-avatar>
          <h3>{{ userInfo.name || '用户' }}</h3>
          <el-tag :type="roleTagType">{{ roleText }}</el-tag>
        </div>

        <!-- 右侧：信息列表 -->
        <div class="profile-info">
          <el-descriptions :column="2" border>
            <!-- ========== 账号字段（根据角色显示不同标签） ========== -->
            <el-descriptions-item :label="accountLabel">
              {{ userInfo.username || '-' }}
            </el-descriptions-item>

            <!-- ========== 姓名字段 ========== -->
            <el-descriptions-item label="姓名">
              {{ userInfo.name || '-' }}
            </el-descriptions-item>

            <!-- ========== 邮箱 ========== -->
            <el-descriptions-item label="邮箱">
              {{ userInfo.email || '-' }}
            </el-descriptions-item>

            <!-- ========== 联系电话 ========== -->
            <el-descriptions-item label="联系电话">
              {{ userInfo.phone || '-' }}
            </el-descriptions-item>

            <!-- ========== 角色特有字段 ========== -->
            <!-- 管理员特有字段 -->
            <template v-if="userInfo.role === 'admin'">
              <el-descriptions-item label="角色">
                <el-tag type="danger" size="small">管理员</el-tag>
              </el-descriptions-item>
            </template>

            <!-- 教师特有字段 -->
            <template v-if="userInfo.role === 'teacher'">
              <el-descriptions-item label="学院">
                {{ userInfo.college || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="专业">
                {{ userInfo.major || '-' }}
              </el-descriptions-item>
            </template>

            <!-- 学生特有字段 -->
            <template v-if="userInfo.role === 'student'">
              <el-descriptions-item label="学院">
                {{ userInfo.college || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="专业">
                {{ userInfo.major || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="年级">
                {{ userInfo.grade || '-' }}
              </el-descriptions-item>
              <el-descriptions-item label="班级">
                {{ userInfo.className || '-' }}
              </el-descriptions-item>
            </template>
          </el-descriptions>
        </div>
      </div>
    </el-card>

    <!-- ========== 编辑对话框 ========== -->
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
        <el-button type="primary" @click="submitForm" :loading="submitting">
          保存
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue';
import { Edit } from '@element-plus/icons-vue';
import { ElMessage } from 'element-plus';
import request from '@/api/request';
import { useAuthStore } from '@/stores/auth';

const authStore = useAuthStore();
const loading = ref(false);
const submitting = ref(false);

// ============ 用户信息 ============
const userInfo = ref({
  username: '',
  name: '',
  role: '',
  college: '',
  major: '',
  grade: '',
  className: '',
  phone: '',
  email: '',
  title: '' // 教师职称
});

// ============ 计算属性 ============
const roleText = computed(() => {
  const map = { admin: '管理员', teacher: '教师', student: '学生' };
  return map[userInfo.value.role] || userInfo.value.role;
});

const roleTagType = computed(() => {
  const map = { admin: 'danger', teacher: 'warning', student: 'success' };
  return map[userInfo.value.role] || 'info';
});

/**
 * 账号字段标签（根据角色动态显示）
 */
const accountLabel = computed(() => {
  const role = userInfo.value.role;
  const labelMap = {
    admin: '管理员账号',
    teacher: '工号',
    student: '学号'
  };
  return labelMap[role] || '账号';
});

// ============ 编辑对话框 ============
const dialogVisible = ref(false);
const formRef = ref(null);
const formData = reactive({
  phone: '',
  email: ''
});

const formRules = {
  phone: [
    { required: true, message: '请输入联系电话', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
  ]
};

// ============ 数据加载 ============
const loadProfile = async () => {
  loading.value = true;
  try {
    const res = await request.get('/user/profile');
    if (res.status === 'success' && res.data) {
      userInfo.value = res.data;

      // 同步更新 authStore 中的用户信息
      if (authStore.userInfo) {
        authStore.userInfo = { ...authStore.userInfo, ...res.data };
        localStorage.setItem('userInfo', JSON.stringify(authStore.userInfo));
      }
    } else {
      ElMessage.warning('获取个人信息失败');
    }
  } catch (error) {
    console.error('加载个人信息失败:', error);
    ElMessage.error('加载个人信息失败');
  } finally {
    loading.value = false;
  }
};

// ============ 编辑操作 ============
const handleEdit = () => {
  formData.phone = userInfo.value.phone || '';
  formData.email = userInfo.value.email || '';
  dialogVisible.value = true;
};

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return;

    submitting.value = true;
    try {
      const res = await request.put('/user/profile', {
        phone: formData.phone,
        email: formData.email
      });

      if (res.status === 'success') {
        // 更新本地数据
        userInfo.value.phone = formData.phone;
        userInfo.value.email = formData.email;

        // 同步更新 authStore
        if (authStore.userInfo) {
          authStore.userInfo.phone = formData.phone;
          authStore.userInfo.email = formData.email;
          localStorage.setItem('userInfo', JSON.stringify(authStore.userInfo));
        }

        dialogVisible.value = false;
        ElMessage.success('个人信息保存成功');
      } else {
        ElMessage.error(res.info || '保存失败');
      }
    } catch (error) {
      console.error('保存个人信息失败:', error);
      ElMessage.error('保存失败，请重试');
    } finally {
      submitting.value = false;
    }
  });
};

const resetForm = () => {
  formRef.value?.resetFields();
};

// ============ 生命周期 ============
onMounted(() => {
  loadProfile();
});
</script>

<style scoped>
.profile-container {
  padding: 20px;
}
.profile-card {
  max-width: 900px;
  margin: 0 auto;
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
.profile-avatar .el-avatar {
  background-color: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}
.avatar-text {
  font-size: 48px;
  font-weight: 700;
}
.profile-avatar h3 {
  margin: 8px 0 4px 0;
  font-size: 18px;
}
.profile-info {
  flex: 1;
}
:deep(.el-descriptions) {
  width: 100%;
}
:deep(.el-descriptions__label) {
  width: 120px;
  font-weight: 600;
}
:deep(.el-descriptions__cell) {
  padding: 12px 16px;
}
</style>