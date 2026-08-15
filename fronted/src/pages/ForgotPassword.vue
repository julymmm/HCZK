<template>
  <div class="forgot-page">
    <section class="forgot-panel">
      <div class="panel-head">
        <h1>找回密码</h1>
        <p>输入注册邮箱，获取验证码后设置新密码。</p>
      </div>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @keyup.enter="submit">
        <el-form-item label="邮箱" prop="identifier">
          <el-input v-model="form.identifier" size="large" clearable prefix-icon="User" placeholder="请输入注册邮箱" />
        </el-form-item>

        <el-form-item label="验证码" prop="code">
          <div class="code-row">
            <el-input v-model="form.code" size="large" clearable placeholder="请输入验证码" />
            <el-button :disabled="countdown > 0" :loading="sending" size="large" @click="sendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>

        <el-alert v-if="debugCode" class="debug-alert" type="info" :closable="false" show-icon>
          <template #title>开发模式验证码：{{ debugCode }}</template>
        </el-alert>

        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password size="large" prefix-icon="Lock" placeholder="至少 8 位" />
        </el-form-item>

        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password size="large" prefix-icon="Lock" placeholder="请再次输入新密码" />
        </el-form-item>

        <el-button class="submit-btn" type="primary" size="large" :loading="submitting" @click="submit">重置密码</el-button>
      </el-form>

      <div class="footer">
        <router-link to="/auth/login">返回登录</router-link>
      </div>
    </section>
  </div>
</template>

<script setup>
import { reactive, ref, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore, useAuthStore } from '../stores'

const router = useRouter()
const app = useAppStore()
const auth = useAuthStore()
const formRef = ref()
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
const debugCode = ref('')
let timer = null

const form = reactive({ identifier: '', code: '', newPassword: '', confirmPassword: '' })

const rules = {
  identifier: [{ required: true, message: '请输入注册邮箱', trigger: 'blur' }],
  code: [{ required: true, message: '请输入验证码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 8, message: '密码至少 8 位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    { validator: (_, value, cb) => value === form.newPassword ? cb() : cb(new Error('两次密码不一致')), trigger: 'blur' }
  ]
}

function startCountdown(seconds = 60) {
  countdown.value = seconds
  clearInterval(timer)
  timer = setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
}

async function sendCode() {
  if (!form.identifier) {
    app.addNotification({ type: 'warning', message: '请先输入注册邮箱' })
    return
  }
  sending.value = true
  try {
    const data = await auth.sendResetPasswordCode(form.identifier)
    debugCode.value = data.debugCode || ''
    startCountdown(60)
    app.addNotification({ type: 'success', message: `验证码已发送至 ${data.identifier || '绑定邮箱'}` })
  } catch (error) {
    app.addNotification({ type: 'error', message: error.response?.data?.message || '验证码发送失败' })
  } finally {
    sending.value = false
  }
}

function submit() {
  formRef.value.validate(async valid => {
    if (!valid) return
    submitting.value = true
    try {
      await auth.resetPassword({ identifierType: 'EMAIL', identifier: form.identifier, code: form.code, newPassword: form.newPassword })
      app.addNotification({ type: 'success', message: '密码已重置，请重新登录' })
      router.replace('/auth/login')
    } catch (error) {
      app.addNotification({ type: 'error', message: error.response?.data?.message || '密码重置失败' })
    } finally {
      submitting.value = false
    }
  })
}

onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped lang="scss">
.forgot-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: #f8fafc;
}

.forgot-panel {
  width: min(440px, 100%);
  padding: 32px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  box-shadow: 0 20px 45px rgba(15, 23, 42, 0.08);
}

.panel-head {
  margin-bottom: 24px;
  text-align: center;

  h1 {
    margin: 0 0 8px;
    font-size: 26px;
    color: #111827;
  }

  p {
    margin: 0;
    color: #64748b;
    line-height: 1.6;
  }
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

.submit-btn {
  width: 100%;
}

.footer {
  margin-top: 18px;
  text-align: center;

  a {
    color: #ff6b6b;
    text-decoration: none;
    font-weight: 600;
  }
}

@media (max-width: 480px) {
  .forgot-panel { padding: 24px; }
  .code-row { grid-template-columns: 1fr; }
}
</style>