<template>
  <div class="password-page">
    <div class="password-card">
      <h3>修改密码</h3>
      <p class="subtitle">请先验证原密码，再设置新密码</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
        size="large"
        style="max-width: 480px"
      >
        <el-form-item label="原密码" prop="oldPassword">
          <el-input
            v-model="form.oldPassword"
            type="password"
            show-password
            placeholder="请输入原密码"
          />
        </el-form-item>

        <el-form-item label="新密码" prop="newPassword">
          <el-input
            v-model="form.newPassword"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>

        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="请再次输入新密码"
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSubmit">
            确认修改
          </el-button>
          <el-button @click="handleCancel">取消</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const formRef = ref(null)
const loading = ref(false)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 自定义校验：新密码不能与原密码相同
const validateNewPassword = (rule, value, callback) => {
  if (value === form.oldPassword) {
    callback(new Error('新密码不能与旧密码相同'))
  } else {
    callback()
  }
}

// 自定义校验：确认密码必须与新密码一致
const validateConfirmPassword = (rule, value, callback) => {
  if (value !== form.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
  } else {
    callback()
  }
}

const rules = {
  oldPassword: [
    { required: true, message: '请输入原密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码至少6位', trigger: 'blur' },
    { validator: validateNewPassword, trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const handleSubmit = () => {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const res = await changePassword({
        oldPassword: form.oldPassword,
        newPassword: form.newPassword,
        confirmPassword: form.confirmPassword
      })

      if (res.status === 'success') {
        ElMessage.success('密码修改成功，请重新登录')

        // 清除登录态并跳转到登录页
        authStore.logout()
        router.push('/login')
      } else {
        ElMessage.error(res.info || '修改失败')
      }
    } catch (error) {
      // 响应拦截器已经显示错误，无需再次显示
    } finally {
      loading.value = false
    }
  })
}

const handleCancel = () => {
  router.back()
}
</script>

<style scoped>
.password-page {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding-top: 60px;
  min-height: 100%;
}

.password-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06);
  padding: 40px;
  width: 560px;
}

.password-card h3 {
  font-size: 22px;
  margin: 0 0 8px 0;
  color: #1a1a2e;
}

.password-card .subtitle {
  color: #999;
  margin: 0 0 32px 0;
  font-size: 14px;
}
</style>
