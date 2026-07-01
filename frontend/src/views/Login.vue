<template>
  <div class="login-container">
    <!-- 左侧登录表单 - 占 2/3 -->
    <div class="login-box">
      <div class="login-left">
        <div class="login-form">
          <div class="title-container">
            <el-icon class="logo"><School /></el-icon>
            <h2 class="title">工程教育专业认证智能服务系统</h2>
          </div>
          <h3>欢迎登录</h3>
          <p class="subtitle">请输入您的账号（工号/学号）和密码访问系统</p>

          <el-form
              ref="loginFormRef"
              :model="loginForm"
              :rules="loginRules"
              size="large"
              label-position="top"
          >
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入账号" @input="onInputChange">
                <template #prefix>
                  <el-icon><User /></el-icon>
                </template>
              </el-input>
            </el-form-item>

            <el-form-item prop="password">
              <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="密码"
                  show-password
                  @input="onPasswordInput"
                  @focus="showPasswordTips = true"
                  @blur="handlePasswordBlur"
                  @keyup.enter="handleLogin"
              >
                <template #prefix>
                  <el-icon><Lock /></el-icon>
                </template>
              </el-input>
            </el-form-item>
          </el-form>

          <!-- 锁定提示 -->
          <el-alert
              v-if="lockStatus.isLocked"
              title="IP 已被锁定"
              type="warning"
              :description="`检测到异常登录尝试，请 ${lockStatus.remainingMinutes} 分钟后重试。`"
              show-icon
              :closable="false"
              style="margin-bottom: 16px"
          />

          <div class="options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary" :underline="false" @click="showForgotDialog = true">
              忘记密码?
            </el-link>
          </div>

          <el-button
              type="primary"
              style="width:100%;"
              size="large"
              :loading="loading"
              :disabled="lockStatus.isLocked"
              @click="handleLogin"
          >
            登录系统
          </el-button>

          <!-- 登录失败次数提示（从后端获取） -->
          <div v-if="lockStatus.attemptCount > 0 && !lockStatus.isLocked" class="attempts-container">
            <div class="attempts-header">
              <span class="attempts-label">
                <el-icon><Warning /></el-icon>
                登录尝试
              </span>
              <span class="attempts-count" :style="{ color: attemptsColor }">
                {{ lockStatus.attemptCount }} / {{ MAX_ATTEMPTS }}
              </span>
            </div>
            <el-progress
                :percentage="attemptsPercentage"
                :color="attemptsProgressColor"
                :stroke-width="6"
                :show-text="false"
                class="attempts-progress"
            />
            <div class="attempts-tip-text">
              连续失败 {{ MAX_ATTEMPTS }} 次后将锁定 {{ LOCK_DURATION }} 分钟
            </div>
          </div>

          <p class="footer-tip">首次登录请修改初始密码</p>
        </div>
      </div>

      <!-- 右侧安全策略提示 -->
      <div class="login-right">
        <div class="safety-tips">
          <h4><el-icon><WarningFilled /></el-icon> 安全策略提示</h4>
          <ul>
            <li>
              <el-icon color="green"><SuccessFilled /></el-icon>
              登录限制：连续5次失败后该 IP 将被锁定15分钟。
            </li>
          </ul>
          <el-alert
              v-if="lockStatus.isLocked"
              title="IP 已被锁定"
              type="warning"
              :description="`检测到异常登录尝试，请 ${lockStatus.remainingMinutes} 分钟后重试。`"
              show-icon
              :closable="false"
          />
        </div>
      </div>
    </div>

    <!-- ========== 忘记密码弹窗 ========== -->
    <el-dialog
        v-model="showForgotDialog"
        title="忘记密码"
        width="420px"
        :close-on-click-modal="true"
        :close-on-press-escape="true"
    >
      <div class="forgot-password-content">
        <el-icon class="info-icon"><InfoFilled /></el-icon>
        <p class="info-text">
          您的账号由管理员统一管理，<br />
          如需重置密码，请自行联系管理员处理。
        </p>
      </div>

      <template #footer>
        <el-button type="primary" @click="showForgotDialog = false">
          我知道了
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import request from '@/api/request'
import {
  School,
  WarningFilled,
  SuccessFilled,
  User,
  Lock,
  Warning,
  InfoFilled,
} from '@element-plus/icons-vue'

const router = useRouter()
const authStore = useAuthStore()
const loginFormRef = ref(null)
const loading = ref(false)

// ============ 登录表单 ============
const loginForm = ref({
  username: '',
  password: ''
})

// ============ 密码校验状态 ============
const showPasswordTips = ref(false)

// ===== 密码校验规则（特殊字符改为可选） =====
const validateLength = computed(() => loginForm.value.password.length >= 8)
const validateNumber = computed(() => /\d/.test(loginForm.value.password))
const validateLetter = computed(() => /[A-Za-z]/.test(loginForm.value.password))
const validateSpecialChar = computed(() => /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(loginForm.value.password))

// 计算已满足的校验项数量（特殊字符不计入）
const validCount = computed(() => {
  let count = 0
  if (validateLength.value) count++
  if (validateNumber.value) count++
  if (validateLetter.value) count++
  return count
})



// ============ 登录锁定策略（从后端获取） ============
const MAX_ATTEMPTS = 5
const LOCK_DURATION = 15

// 锁定状态（从后端获取）
const lockStatus = ref({
  isLocked: false,
  attemptCount: 0,
  remainingMinutes: 0
})

// 计算进度百分比
const attemptsPercentage = computed(() => {
  return Math.round((lockStatus.value.attemptCount / MAX_ATTEMPTS) * 100)
})

const attemptsProgressColor = computed(() => {
  const pct = attemptsPercentage.value
  if (pct < 40) return '#67c23a'
  if (pct < 70) return '#e6a23c'
  return '#f56c6c'
})

const attemptsColor = computed(() => {
  const pct = attemptsPercentage.value
  if (pct < 40) return '#67c23a'
  if (pct < 70) return '#e6a23c'
  return '#f56c6c'
})

/**
 * 从后端获取当前 IP 的锁定状态
 */
const fetchLockStatus = async () => {
  try {
    const res = await request.get('/auth/lock-status')
    if (res.status === 'success' && res.data) {
      lockStatus.value = {
        isLocked: res.data.isLocked || false,
        attemptCount: res.data.attemptCount || 0,
        remainingMinutes: res.data.remainingMinutes || 0
      }
      console.log('锁定状态已更新:', lockStatus.value)
    }
  } catch (error) {
    console.error('获取锁定状态失败:', error)
    // 失败时使用默认值，不影响登录
  }
}

// ============ 表单校验规则 ============
const loginRules = {
  username: [
    { required: true, message: '请输入工号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
  ]
}

const rememberMe = ref(false)

// ============ 忘记密码弹窗 ============
const showForgotDialog = ref(false)

// ============ 输入事件 ============
const onInputChange = () => {
  loginFormRef.value?.clearValidate(['password'])
}

const onPasswordInput = () => {
  showPasswordTips.value = true
  loginFormRef.value?.clearValidate(['password'])
}

const handlePasswordBlur = () => {
  setTimeout(() => {
    showPasswordTips.value = false
  }, 300)
}

// ============ 登录方法 ============
const handleLogin = async () => {
  // 先获取最新的锁定状态
  await fetchLockStatus()

  if (lockStatus.value.isLocked) {
    ElMessage.warning(`IP 已被锁定，请 ${lockStatus.value.remainingMinutes} 分钟后重试`)
    return
  }

  loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true

    try {
      const result = await authStore.login(
          loginForm.value.username,
          loginForm.value.password
      )

      if (result.success) {
        // 登录成功
        if (rememberMe.value) {
          localStorage.setItem('rememberMe', 'true')
          localStorage.setItem('savedUsername', loginForm.value.username)
        } else {
          localStorage.removeItem('rememberMe')
          localStorage.removeItem('savedUsername')
        }

        const userName = authStore.userInfo?.name ||
            authStore.userInfo?.username ||
            loginForm.value.username
        ElMessage.success(`欢迎回来，${userName}`)

        // 跳转到首页
        const homePath = authStore.getHomePath()
        router.push(homePath)
      } else {
        // 登录失败，重新获取锁定状态（后端已记录失败次数）
        await fetchLockStatus()
        ElMessage.error(result.message || '登录失败，请重试')
      }
    } catch (error) {
      // 登录异常，重新获取锁定状态
      await fetchLockStatus()
      const errorMsg = error?.message || '登录失败，请检查网络连接'
      ElMessage.error(errorMsg)
      console.error('登录异常:', error)
    } finally {
      loading.value = false
    }
  })
}

// ============ 恢复记住的账号 ============
const loadRemembered = () => {
  if (localStorage.getItem('rememberMe') === 'true') {
    const savedUsername = localStorage.getItem('savedUsername')
    if (savedUsername) {
      loginForm.value.username = savedUsername
      rememberMe.value = true
    }
  }
}

// ============ 生命周期 ============
onMounted(() => {
  loadRemembered()
  // 页面加载时获取锁定状态
  fetchLockStatus()
  // 每 30 秒刷新一次锁定状态（可选）
  // setInterval(fetchLockStatus, 30000)
})
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: linear-gradient(135deg, #e6f7ff 0%, #bae7ff 100%);
}

.login-box {
  display: flex;
  width: 960px;
  height: 680px;
  background-color: #fff;
  border-radius: 16px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  overflow: hidden;
}

.login-left {
  flex: 2;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px 36px;
  overflow-y: auto;
}

.login-right {
  flex: 1;
  background: linear-gradient(135deg, #f7f8fa 0%, #eef1f5 100%);
  padding: 40px 30px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-form {
  width: 100%;
  max-width: 420px;
}

.title-container {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.logo {
  font-size: 36px;
  color: #409EFF;
}

.title {
  font-size: 20px;
  margin-left: 10px;
  color: #333;
  font-weight: 700;
}

h3 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
  color: #1a1a2e;
}

.subtitle {
  color: #999;
  margin-bottom: 18px;
  font-size: 14px;
}

.item-progress :deep(.el-progress-bar__outer) {
  border-radius: 2px;
  background-color: #e8ecf1;
}

.item-progress :deep(.el-progress-bar__inner) {
  border-radius: 2px;
  transition: width 0.3s ease;
}

/* ========== 登录尝试进度条 ========== */
.attempts-container {
  margin-top: 10px;
  padding: 10px 14px;
  background: #fef8f0;
  border-radius: 8px;
  border: 1px solid #fde8d0;
}

.attempts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.attempts-label {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 4px;
}

.attempts-count {
  font-size: 13px;
  font-weight: 600;
}

.attempts-progress {
  margin-bottom: 4px;
}

.attempts-tip-text {
  font-size: 12px;
  color: #999;
  text-align: right;
}

.options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
  margin-top: 4px;
}

.footer-tip {
  margin-top: 10px;
  text-align: center;
  color: #999;
  font-size: 12px;
}

/* ========== 右侧安全策略 ========== */
.safety-tips h4 {
  font-size: 16px;
  margin-bottom: 20px;
  display: flex;
  align-items: center;
  color: #1a1a2e;
}

.safety-tips h4 .el-icon {
  margin-right: 8px;
  color: #E6A23C;
  font-size: 20px;
}

.safety-tips ul {
  list-style: none;
  padding: 0;
  margin-bottom: 24px;
}

.safety-tips li {
  margin-bottom: 12px;
  color: #555;
  display: flex;
  align-items: flex-start;
  font-size: 13px;
  line-height: 1.6;
}

.safety-tips li .el-icon {
  margin-right: 10px;
  flex-shrink: 0;
  margin-top: 2px;
}

.safety-tips :deep(.el-alert) {
  padding: 14px 16px;
  border-radius: 8px;
  margin-top: 8px;
}

.safety-tips :deep(.el-alert__description) {
  font-size: 13px;
}

/* ========== 忘记密码弹窗 ========== */
.forgot-password-content {
  text-align: center;
  padding: 8px 0;
}

.info-icon {
  font-size: 48px;
  color: #409EFF;
  margin-bottom: 12px;
}

.info-text {
  font-size: 15px;
  color: #333;
  margin-bottom: 20px;
  line-height: 1.8;
}

.contact-info {
  text-align: left;
  background: #f7f8fa;
  border-radius: 8px;
  padding: 16px 20px;
  margin-bottom: 8px;
}

.contact-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 0;
  font-size: 14px;
  color: #555;
}

.contact-item .el-icon {
  font-size: 18px;
  color: #409EFF;
  flex-shrink: 0;
  width: 24px;
}

.contact-item strong {
  color: #333;
}

.contact-item a {
  color: #409EFF;
  text-decoration: none;
}

.contact-item a:hover {
  text-decoration: underline;
}

/* 对话框 footer 居中 */
:deep(.el-dialog__footer) {
  text-align: center;
  padding-top: 0;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .login-box {
    flex-direction: column;
    width: 95%;
    height: auto;
    max-height: 98vh;
    border-radius: 12px;
  }
  .login-left {
    flex: none;
    padding: 20px 16px;
  }
  .login-right {
    flex: none;
    padding: 16px 20px;
  }
  .login-form {
    max-width: 100%;
  }
  .title {
    font-size: 16px;
  }
  .test-accounts {
    flex-wrap: wrap;
  }
  .strength-items {
    grid-template-columns: 1fr;
    gap: 2px;
  }
  .strength-item {
    padding: 1px 0;
  }
  .item-label {
    min-width: 70px;
    font-size: 11px;
  }
}
</style>