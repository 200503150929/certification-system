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
          <p class="subtitle">请输入您的工号和密码访问系统</p>

          <el-form
            ref="loginFormRef"
            :model="loginForm"
            :rules="loginRules"
            size="large"
            label-position="top"
          >
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入工号" @input="onInputChange">
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

              <!-- ===== 密码复杂度进度条 ===== -->
              <div v-if="showPasswordTips" class="password-strength-container">
                <div class="strength-header">
                  <span class="strength-label">密码强度</span>
                  <span class="strength-score" :style="{ color: strengthColor }">
                    {{ strengthText }}
                  </span>
                </div>
                <el-progress
                  :percentage="strengthPercentage"
                  :color="strengthProgressColor"
                  :stroke-width="8"
                  :show-text="false"
                  class="strength-progress"
                />

                <div class="strength-items">
                  <!-- 至少8位 -->
                  <div class="strength-item">
                    <span class="item-label">
                      <el-icon :class="validateLength ? 'valid-icon' : 'invalid-icon'">
                        <Check v-if="validateLength" /><Close v-else />
                      </el-icon>
                      至少8位
                    </span>
                    <el-progress
                      :percentage="validateLength ? 100 : 0"
                      :color="validateLength ? '#67c23a' : '#e8ecf1'"
                      :stroke-width="4"
                      :show-text="false"
                      class="item-progress"
                    />
                  </div>
                  <!-- 包含数字 -->
                  <div class="strength-item">
                    <span class="item-label">
                      <el-icon :class="validateNumber ? 'valid-icon' : 'invalid-icon'">
                        <Check v-if="validateNumber" /><Close v-else />
                      </el-icon>
                      包含数字
                    </span>
                    <el-progress
                      :percentage="validateNumber ? 100 : 0"
                      :color="validateNumber ? '#67c23a' : '#e8ecf1'"
                      :stroke-width="4"
                      :show-text="false"
                      class="item-progress"
                    />
                  </div>
                  <!-- 包含大小写字母 -->
                  <div class="strength-item">
                    <span class="item-label">
                      <el-icon :class="validateLetter ? 'valid-icon' : 'invalid-icon'">
                        <Check v-if="validateLetter" /><Close v-else />
                      </el-icon>
                      包含大小写字母
                    </span>
                    <el-progress
                      :percentage="validateLetter ? 100 : 0"
                      :color="validateLetter ? '#67c23a' : '#e8ecf1'"
                      :stroke-width="4"
                      :show-text="false"
                      class="item-progress"
                    />
                  </div>
                  <!-- 包含特殊字符（可选，灰色显示） -->
                  <div class="strength-item optional-item">
                    <span class="item-label">
                      <el-icon :class="validateSpecialChar ? 'valid-icon' : 'optional-icon'">
                        <Check v-if="validateSpecialChar" /><Close v-else />
                      </el-icon>
                      包含特殊字符
                      <span class="optional-tag">可选</span>
                    </span>
                    <el-progress
                      :percentage="validateSpecialChar ? 100 : 0"
                      :color="validateSpecialChar ? '#67c23a' : '#e8ecf1'"
                      :stroke-width="4"
                      :show-text="false"
                      class="item-progress"
                    />
                  </div>
                </div>
              </div>
            </el-form-item>
          </el-form>

          <!-- 锁定提示 -->
          <el-alert
            v-if="isLocked"
            title="账号已锁定"
            type="warning"
            description="检测到异常登录尝试，请15分钟后重试。"
            show-icon
            :closable="false"
            style="margin-bottom: 16px"
          />

          <div class="options">
            <el-checkbox v-model="rememberMe">记住我</el-checkbox>
            <el-link type="primary" :underline="false">忘记密码?</el-link>
          </div>

          <el-button
            type="primary"
            style="width:100%;"
            size="large"
            :loading="loading"
            :disabled="isLocked"
            @click="handleLogin"
          >
            登录系统
          </el-button>

          <!-- 快速测试入口 -->
          <div class="test-accounts">
            <span class="test-label">⚡ 快速测试：</span>
            <el-button size="small" type="success" plain @click="quickLogin('学生')">
              学生
            </el-button>
            <el-button size="small" type="warning" plain @click="quickLogin('教师')">
              教师
            </el-button>
            <el-button size="small" type="danger" plain @click="quickLogin('管理员')">
              管理员
            </el-button>
          </div>

          <!-- 登录失败次数提示 -->
          <div v-if="loginAttempts > 0 && !isLocked" class="attempts-container">
            <div class="attempts-header">
              <span class="attempts-label">
                <el-icon><Warning /></el-icon>
                登录尝试
              </span>
              <span class="attempts-count" :style="{ color: attemptsColor }">
                {{ loginAttempts }} / {{ MAX_ATTEMPTS }}
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
              建议密码复杂度：至少8位，包含数字、大小写字母。
            </li>
            <li>
              <el-icon color="green"><SuccessFilled /></el-icon>
              登录限制：连续5次失败后账号将被锁定15分钟。
            </li>
          </ul>
          <el-alert
            v-if="isLocked"
            title="账号已锁定"
            type="warning"
            description="检测到异常登录尝试，请15分钟后重试。"
            show-icon
            :closable="false"
          />
          <el-alert
            v-else-if="loginAttempts >= 3"
            title="登录风险提示"
            type="info"
            :description="`已连续失败 ${loginAttempts} 次，还剩 ${5 - loginAttempts} 次尝试机会`"
            show-icon
            :closable="false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import {
  School,
  WarningFilled,
  SuccessFilled,
  User,
  Lock,
  Check,
  Close,
  Warning
} from '@element-plus/icons-vue'

const router = useRouter()
const loginFormRef = ref(null)
const loading = ref(false)

// ============ 登录表单 ============
const loginForm = ref({
  username: '',
  password: ''
})

// ============ 密码校验状态 ============
const showPasswordTips = ref(false)

// ===== 修改：密码校验规则（特殊字符改为可选） =====
const validateLength = computed(() => loginForm.value.password.length >= 8)
const validateNumber = computed(() => /\d/.test(loginForm.value.password))
const validateLetter = computed(() => /[A-Za-z]/.test(loginForm.value.password)) // 合并大小写
const validateSpecialChar = computed(() => /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(loginForm.value.password))

// 计算已满足的校验项数量（特殊字符不计入）
const validCount = computed(() => {
  let count = 0
  if (validateLength.value) count++
  if (validateNumber.value) count++
  if (validateLetter.value) count++
  // 特殊字符不计入进度，仅作为额外加分项
  return count
})

// ===== 修改：密码有效条件（只需满足3项） =====
const isPasswordValid = computed(() => {
  return validateLength.value && validateNumber.value && validateLetter.value
})

// 密码强度百分比（基础3项 + 特殊字符额外加分）
const strengthPercentage = computed(() => {
  let base = Math.round((validCount.value / 3) * 100)
  // 如果特殊字符满足，额外加10%（但不超过100%）
  if (validateSpecialChar.value) {
    base = Math.min(base + 10, 100)
  }
  // 特殊处理：密码为空
  if (loginForm.value.password.length === 0) return 0
  return base
})

// 密码强度文字
const strengthText = computed(() => {
  const pct = strengthPercentage.value
  if (pct === 0) return '请设置密码'
  if (pct <= 33) return '弱'
  if (pct <= 66) return '一般'
  if (pct <= 90) return '强'
  return '非常强'
})

// 密码强度颜色
const strengthColor = computed(() => {
  const pct = strengthPercentage.value
  if (pct === 0) return '#c0c4cc'
  if (pct <= 33) return '#f56c6c'
  if (pct <= 66) return '#e6a23c'
  if (pct <= 90) return '#409eff'
  return '#67c23a'
})

// 进度条颜色配置
const strengthProgressColor = computed(() => {
  const pct = strengthPercentage.value
  if (pct === 0) return '#e8ecf1'
  if (pct <= 33) return '#f56c6c'
  if (pct <= 66) return '#e6a23c'
  if (pct <= 90) return '#409eff'
  return '#67c23a'
})

// ============ 登录锁定策略 ============
const MAX_ATTEMPTS = 5
const LOCK_DURATION = 15

const loginAttempts = ref(0)
const lockUntil = ref(null)
const isLocked = ref(false)
const remainingLockTime = ref(0)
let lockTimer = null

const attemptsPercentage = computed(() => {
  return Math.round((loginAttempts.value / MAX_ATTEMPTS) * 100)
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

const loadLockState = () => {
  const savedAttempts = localStorage.getItem('loginAttempts')
  const savedLockUntil = localStorage.getItem('lockUntil')

  if (savedAttempts) {
    loginAttempts.value = parseInt(savedAttempts)
  }

  if (savedLockUntil) {
    lockUntil.value = new Date(parseInt(savedLockUntil))
    checkLockStatus()
  }
}

const checkLockStatus = () => {
  if (!lockUntil.value) {
    isLocked.value = false
    return
  }

  const now = new Date()
  if (now < lockUntil.value) {
    isLocked.value = true
    updateRemainingTime()
    startLockTimer()
  } else {
    isLocked.value = false
    lockUntil.value = null
    loginAttempts.value = 0
    localStorage.removeItem('loginAttempts')
    localStorage.removeItem('lockUntil')
    if (lockTimer) {
      clearInterval(lockTimer)
      lockTimer = null
    }
  }
}

const updateRemainingTime = () => {
  if (!lockUntil.value) return
  const now = new Date()
  const diffMs = lockUntil.value - now
  if (diffMs <= 0) {
    remainingLockTime.value = 0
    isLocked.value = false
    loginAttempts.value = 0
    localStorage.removeItem('loginAttempts')
    localStorage.removeItem('lockUntil')
    if (lockTimer) {
      clearInterval(lockTimer)
      lockTimer = null
    }
    return
  }
  remainingLockTime.value = Math.ceil(diffMs / (1000 * 60))
}

const startLockTimer = () => {
  if (lockTimer) {
    clearInterval(lockTimer)
  }
  lockTimer = setInterval(() => {
    updateRemainingTime()
    if (!isLocked.value) {
      clearInterval(lockTimer)
      lockTimer = null
    }
  }, 10000)
}

const recordFailedAttempt = () => {
  loginAttempts.value += 1
  localStorage.setItem('loginAttempts', loginAttempts.value.toString())

  if (loginAttempts.value >= MAX_ATTEMPTS) {
    const now = new Date()
    lockUntil.value = new Date(now.getTime() + LOCK_DURATION * 60 * 1000)
    localStorage.setItem('lockUntil', lockUntil.value.getTime().toString())
    isLocked.value = true
    updateRemainingTime()
    startLockTimer()
    ElMessage.warning(`连续失败${MAX_ATTEMPTS}次，账号已被锁定${LOCK_DURATION}分钟`)
  }
}

const resetLoginAttempts = () => {
  loginAttempts.value = 0
  isLocked.value = false
  lockUntil.value = null
  localStorage.removeItem('loginAttempts')
  localStorage.removeItem('lockUntil')
  if (lockTimer) {
    clearInterval(lockTimer)
    lockTimer = null
  }
}

// ============ 表单校验规则 ============
const loginRules = {
  username: [
    { required: true, message: '请输入工号', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' }
    // 测试环境：不强制密码复杂度，生产环境可取消注释
    /* ,{
      validator: (rule, value, callback) => {
        if (!value) {
          callback(new Error('请输入密码'))
          return
        }
        if (!isPasswordValid.value) {
          callback(new Error('密码至少8位，需包含数字和大小写字母'))
          return
        }
        callback()
      },
      trigger: 'blur'
    } */
  ]
}

const rememberMe = ref(false)

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
const handleLogin = () => {
  if (isLocked.value) {
    ElMessage.warning(`账号已被锁定，请 ${remainingLockTime.value} 分钟后重试`)
    return
  }

  loginFormRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true

    try {
      // 调用后端登录 API
      const res = await request.post('/auth/login', {
        username: loginForm.value.username,
        password: loginForm.value.password
      })

      if (res.status === 'success' && res.data) {
        const { token, userId, username, name, role } = res.data
        // 保存登录信息
        localStorage.setItem('token', token)
        localStorage.setItem('userId', userId)
        localStorage.setItem('userRole', role)      // 后端返回的是英文: admin, teacher, student
        localStorage.setItem('username', username)
        localStorage.setItem('displayName', name)

        resetLoginAttempts()

        if (rememberMe.value) {
          localStorage.setItem('rememberMe', 'true')
          localStorage.setItem('savedUsername', loginForm.value.username)
        } else {
          localStorage.removeItem('rememberMe')
          localStorage.removeItem('savedUsername')
        }

        ElMessage.success(`欢迎回来，${name || username}`)
        
        // 根据角色跳转
        router.push(getHomePath(role || 'student'))
      }
    } catch (error) {
      recordFailedAttempt()
      // 响应拦截器已显示错误消息，不需要再次显示
    } finally {
      loading.value = false
    }
  })
}

// ============ 快速登录（使用测试账号） ============
const quickLogin = async (displayRole) => {
  const accounts = {
    '学生': { username: 'student01', password: '123456' },
    '教师': { username: 'teacher01', password: '123456' },
    '管理员': { username: 'admin', password: '123456' }
  }

  const account = accounts[displayRole]
  if (!account) {
    ElMessage.error('未知角色')
    return
  }

  resetLoginAttempts()
  loading.value = true

  try {
    const res = await request.post('/auth/login', account)

    if (res.status === 'success' && res.data) {
      const { token, userId, username, name, role } = res.data
      localStorage.setItem('token', token)
      localStorage.setItem('userId', userId)
      localStorage.setItem('userRole', role)
      localStorage.setItem('username', username)
      localStorage.setItem('displayName', name)

      ElMessage.success(`以 ${name || displayRole} 身份快速登录成功`)
      
      router.push(getHomePath(role || 'student'))
    }
  } catch (error) {
    recordFailedAttempt()
    // 响应拦截器已显示错误消息
  } finally {
    loading.value = false
  }
}

const getHomePath = (role) => {
  return role === 'student' ? '/my-courses' : '/dashboard'
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
  loadLockState()
  if (isLocked.value) {
    startLockTimer()
  }
})

onUnmounted(() => {
  if (lockTimer) {
    clearInterval(lockTimer)
    lockTimer = null
  }
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

/* ========== 密码强度进度条 ========== */
.password-strength-container {
  margin-top: 8px;
  padding: 12px 14px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #e8ecf1;
}

.strength-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.strength-label {
  font-size: 13px;
  color: #666;
  font-weight: 500;
}

.strength-score {
  font-size: 13px;
  font-weight: 600;
}

.strength-progress {
  margin-bottom: 10px;
}

.strength-items {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px 16px;
}

.strength-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 2px 0;
}

/* 特殊字符项 - 可选样式 */
.optional-item .item-label {
  opacity: 0.7;
}
.optional-item .optional-tag {
  font-size: 10px;
  color: #999;
  background: #e8ecf1;
  padding: 0 6px;
  border-radius: 3px;
  margin-left: 2px;
}
.optional-icon {
  color: #c0c4cc !important;
}

.item-label {
  font-size: 12px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 3px;
  min-width: 88px;
  flex-shrink: 0;
}

.item-label .el-icon {
  font-size: 14px;
}

.valid-icon {
  color: #67c23a;
}

.invalid-icon {
  color: #c0c4cc;
}

.item-progress {
  flex: 1;
  min-width: 40px;
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

/* ========== 测试账号 ========== */
.test-accounts {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 16px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px dashed #d9d9d9;
}

.test-label {
  font-size: 12px;
  color: #999;
  margin-right: 2px;
}

.test-accounts .el-button {
  padding: 4px 14px;
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
