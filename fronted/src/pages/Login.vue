<template>
  <main class="login-page">
    <section class="brand-panel" aria-label="华创智库">
      <img src="../assets/about-image.jpg" alt="华创智库" class="brand-logo" />
      <p class="eyebrow">华创智库</p>
      <h1>欢迎回到知识协作平台</h1>
      <p class="brand-copy">登录后继续浏览资源、参与问答与管理个人学习档案。</p>
      <div class="feature-list">
        <div class="feature-item">
          <font-awesome-icon icon="lightbulb" />
          <span>知识资源沉淀</span>
        </div>
        <div class="feature-item">
          <font-awesome-icon icon="users" />
          <span>团队协作交流</span>
        </div>
        <div class="feature-item">
          <font-awesome-icon icon="rocket" />
          <span>竞赛项目成长</span>
        </div>
      </div>
    </section>

    <section class="form-panel">
      <div class="auth-card">
        <header class="card-header">
          <h2>登录账号</h2>
          <p>请输入手机号或邮箱登录</p>
        </header>

        <el-form
          ref="loginFormRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
          @keyup.enter="onSubmit"
        >
          <div class="login-mode">
            <el-radio-group v-model="mode" size="large">
              <el-radio-button label="password">密码登录</el-radio-button>
              <el-radio-button label="code">验证码登录</el-radio-button>
            </el-radio-group>
          </div>

          <el-form-item label="手机号/邮箱" prop="identifier">
            <el-input
              v-model.trim="form.identifier"
              placeholder="请输入手机号或邮箱"
              clearable
              size="large"
              prefix-icon="User"
            />
          </el-form-item>

          <el-form-item v-if="mode === 'password'" label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              show-password
              size="large"
              prefix-icon="Lock"
            />
          </el-form-item>

          <el-form-item v-else label="验证码" prop="code">
            <div class="code-row">
              <el-input v-model.trim="form.code" size="large" clearable placeholder="请输入验证码" />
              <el-button :disabled="countdown > 0" :loading="sendingCode" size="large" @click="sendLoginCode">
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>

          <el-alert v-if="debugCode" class="debug-alert" type="info" :closable="false" show-icon>
            <template #title>开发模式验证码：{{ debugCode }}</template>
          </el-alert>

          <div class="form-links">
            <router-link to="/auth/forgot-password">忘记密码？</router-link>
          </div>

          <el-button
            type="primary"
            :loading="submitting"
            class="submit-btn"
            size="large"
            @click="onSubmit"
          >
            {{ submitting ? '登录中...' : '登录' }}
          </el-button>
        </el-form>

        <footer class="card-footer">
          <span>还没有账号？</span>
          <router-link to="/auth/register">立即注册</router-link>
        </footer>
      </div>
    </section>
  </main>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore, useAuthStore } from '../stores'

const router = useRouter()
const authStore = useAuthStore()
const appStore = useAppStore()

const loginFormRef = ref()
const submitting = ref(false)
const sendingCode = ref(false)
const countdown = ref(0)
const debugCode = ref('')
const mode = ref('password')
let timer = null
const initialIdentifier = new URLSearchParams(location.search).get('u') || ''
const form = reactive({ identifier: initialIdentifier, password: '', code: '' })

const rules = {
  identifier: [{ required: true, message: '请输入手机号或邮箱', trigger: 'blur' }],
  password: [{ validator: (_, value, cb) => mode.value === 'password' && !value ? cb(new Error('请输入密码')) : cb(), trigger: 'blur' }],
  code: [{ validator: (_, value, cb) => mode.value === 'code' && !value ? cb(new Error('请输入验证码')) : cb(), trigger: 'blur' }]
}

function inferIdentifierType(identifier) {
  const value = String(identifier || '').trim()
  return value.includes('@') ? 'EMAIL' : 'PHONE'
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function sendLoginCode() {
  if (!form.identifier) {
    appStore.addNotification({ type: 'warning', message: '请先输入手机号或邮箱' })
    return
  }
  sendingCode.value = true
  try {
    const data = await authStore.sendAuthCode({ scene: 'LOGIN', identifierType: inferIdentifierType(form.identifier), identifier: form.identifier })
    debugCode.value = data.debugCode || ''
    startCountdown(60)
    appStore.addNotification({ type: 'success', message: `验证码已发送至 ${data.identifier || form.identifier}` })
  } catch (error) {
    appStore.addNotification({ type: 'error', message: error?.response?.data?.message || '验证码发送失败' })
  } finally {
    sendingCode.value = false
  }
}
function onSubmit() {
  loginFormRef.value?.validate(async (valid) => {
    if (!valid || submitting.value) return
    submitting.value = true
    try {
      const res = await authStore.login({ identifierType: inferIdentifierType(form.identifier), identifier: form.identifier, password: mode.value === 'password' ? form.password : '', code: mode.value === 'code' ? form.code : '' })
      if (res?.success) {
        appStore.addNotification({ type: 'success', message: '登录成功' })
        const redirect = new URLSearchParams(location.search).get('redirect') || '/portal'
        router.replace(redirect)
      } else {
        appStore.addNotification({ type: 'error', message: '登录失败，请稍后重试' })
      }
    } catch (error) {
      const message = error?.response?.data?.message || '账号或密码错误'
      appStore.addNotification({ type: 'error', message })
    } finally {
      submitting.value = false
    }
  })
}
onBeforeUnmount(() => clearInterval(timer))
</script>

<style lang="scss">
body,
html,
#app,
.app-container {
  margin: 0 !important;
  padding: 0 !important;
}
</style>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: minmax(360px, 1fr) minmax(360px, 520px);
  background: #f6f8fb;
}

.brand-panel {
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: clamp(32px, 6vw, 88px);
  background: linear-gradient(135deg, #f7fbff 0%, #fff7f2 100%);
  color: #172033;
}

.brand-logo {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 18px;
  margin-bottom: 28px;
  box-shadow: 0 14px 34px rgba(15, 34, 67, 0.14);
}

.eyebrow {
  margin: 0 0 12px;
  color: #e45a3d;
  font-weight: 700;
  letter-spacing: 0;
}

h1 {
  max-width: 560px;
  margin: 0;
  font-size: 42px;
  line-height: 1.16;
  color: #111827;
}

.brand-copy {
  max-width: 520px;
  margin: 18px 0 0;
  color: #5f6f86;
  font-size: 16px;
  line-height: 1.8;
}

.feature-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  max-width: 620px;
  margin-top: 36px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 14px;
  border: 1px solid #e7ecf3;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  color: #334155;
  font-weight: 600;
  white-space: nowrap;

  svg {
    color: #e45a3d;
  }
}

.form-panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background: #ffffff;
}

.auth-card {
  width: 100%;
  max-width: 420px;
}

.card-header {
  margin-bottom: 28px;

  h2 {
    margin: 0 0 8px;
    font-size: 28px;
    color: #111827;
  }

  p {
    margin: 0;
    color: #718096;
  }
}

.login-form {
  :deep(.el-form-item__label) {
    color: #334155;
    font-weight: 700;
  }

  :deep(.el-input__wrapper) {
    border-radius: 8px;
  }
}

.login-mode {
  margin-bottom: 18px;
}
.code-row {
  display: grid;
  grid-template-columns: 1fr 124px;
  gap: 10px;
  width: 100%;
}
.debug-alert {
  margin: -4px 0 18px;
}
.form-links {
  display: flex;
  justify-content: flex-end;
  margin: -4px 0 18px;

  a {
    color: #2563eb;
    font-size: 14px;
    text-decoration: none;
  }
}

.submit-btn {
  width: 100%;
  height: 46px;
  border-radius: 8px;
  font-weight: 700;
}

.card-footer {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 24px;
  color: #64748b;

  a {
    color: #e45a3d;
    font-weight: 700;
    text-decoration: none;
  }
}

@media (max-width: 900px) {
  .login-page {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    padding: 32px 22px;
  }

  h1 {
    font-size: 32px;
  }

  .feature-list {
    grid-template-columns: 1fr;
  }

  .form-panel {
    padding: 28px 20px 40px;
  }
}
</style>