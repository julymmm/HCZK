<template>
  <div class="app-container">
    <RouterView />
  </div>
</template>

<script setup>
import { RouterView } from 'vue-router'
import { onMounted, onUnmounted } from 'vue'
import { useAuthStore } from './stores'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

// 定期检查token有效性
let tokenCheckInterval = null

onMounted(() => {
  // 初始化时加载认证状态
  auth.loadFromStorage()
  
  // 如果已登录，立即检查token有效性
  if (auth.isLoggedIn) {
    checkTokenValidity()
  }
  
  // 设置定期检查token有效性（每5分钟检查一次）
  tokenCheckInterval = setInterval(() => {
    if (auth.isLoggedIn) {
      checkTokenValidity()
    }
  }, 5 * 60 * 1000)
})

onUnmounted(() => {
  if (tokenCheckInterval) {
    clearInterval(tokenCheckInterval)
  }
})

async function checkTokenValidity() {
  try {
    // 首先检查本地token是否已过期
    if (auth.isTokenExpired()) {
      console.log('Token已过期，静默清除登录状态')
      auth.logout()
      // 不跳转页面，让用户继续浏览
      return
    }
    
    // 定期验证token有效性（减少频率，避免过多请求）
    const now = Date.now()
    const lastCheck = auth.lastTokenCheck || 0
    const checkInterval = 10 * 60 * 1000 // 10分钟检查一次
    
    if (now - lastCheck > checkInterval) {
      const isValid = await auth.checkTokenValidity()
      if (!isValid) {
        console.log('Token验证失败，静默清除登录状态')
        auth.logout()
        // 不跳转页面，让用户继续浏览
      }
      auth.lastTokenCheck = now
    }
  } catch (error) {
    console.error('Token有效性检查失败:', error)
  }
}
</script>

<style lang="scss">
.app-container {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
</style> 