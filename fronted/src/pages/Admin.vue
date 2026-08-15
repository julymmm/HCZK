<template>
  <main class="admin-page">
    <section class="admin-shell">
      <header class="admin-header">
        <div>
          <p class="eyebrow">Admin</p>
          <h1>管理员控制台</h1>
        </div>
        <el-button :loading="loading" @click="loadUsers">刷新</el-button>
      </header>

      <section class="action-row">
        <el-input v-model="targetUserId" placeholder="输入用户ID" clearable />
        <el-button type="danger" :loading="kicking" @click="kickByInput">强制下线</el-button>
      </section>

      <el-table :data="users" v-loading="loading" class="admin-table" border>
        <el-table-column prop="id" label="ID" width="90" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="nickname" label="昵称" min-width="120" />
        <el-table-column prop="email" label="邮箱" min-width="180" />
        <el-table-column prop="phone" label="手机号" min-width="130" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="status" label="状态" width="90" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="scope">
            <el-button size="small" type="danger" plain @click="kickUser(scope.row.id)">下线</el-button>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </main>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../utils/http'

const users = ref([])
const loading = ref(false)
const kicking = ref(false)
const targetUserId = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const resp = await http.get('/admin/users', { params: { page: 0, size: 50 } })
    users.value = resp.data?.data || []
  } finally {
    loading.value = false
  }
}

async function kickUser(userId) {
  if (!userId) return
  try {
    await ElMessageBox.confirm(`确认撤销用户 ${userId} 的所有刷新令牌吗？`, '强制下线', {
      confirmButtonText: '确认',
      cancelButtonText: '取消',
      type: 'warning'
    })
  } catch (_) {
    return
  }
  kicking.value = true
  try {
    await http.post(`/admin/users/${userId}/kick`)
    ElMessage.success('已撤销该用户所有刷新令牌')
  } finally {
    kicking.value = false
  }
}

function kickByInput() {
  const id = Number(targetUserId.value)
  if (!Number.isInteger(id) || id <= 0) {
    ElMessage.warning('请输入有效的用户ID')
    return
  }
  kickUser(id)
}

onMounted(loadUsers)
</script>

<style lang="scss" scoped>
.admin-page {
  min-height: 100vh;
  padding: 96px 24px 48px;
  background: #f6f7fb;
}

.admin-shell {
  max-width: 1180px;
  margin: 0 auto;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 4px;
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
  text-transform: uppercase;
}

h1 {
  margin: 0;
  color: #111827;
  font-size: 28px;
}

.action-row {
  display: grid;
  grid-template-columns: minmax(180px, 320px) auto;
  gap: 12px;
  margin-bottom: 16px;
}

.admin-table {
  width: 100%;
}

@media (max-width: 640px) {
  .admin-page {
    padding: 84px 12px 32px;
  }

  .admin-header,
  .action-row {
    grid-template-columns: 1fr;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

