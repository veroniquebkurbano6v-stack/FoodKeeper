<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.clear()
  router.push('/login')
  ElMessage.success('已退出登录')
}
</script>

<template>
  <div class="settings">
    <el-card>
      <template #header>
        <span>账户设置</span>
      </template>
      <div class="setting-item">
        <span class="label">用户名</span>
        <span class="value">{{ userStore.username }}</span>
      </div>
      <div class="setting-item">
        <span class="label">用户ID</span>
        <span class="value">{{ userStore.userId }}</span>
      </div>
    </el-card>

    <el-card style="margin-top: 20px;">
      <template #header>
        <span>系统设置</span>
      </template>
      <el-button type="danger" block @click="handleLogout">退出登录</el-button>
    </el-card>
  </div>
</template>

<style scoped>
.settings {
  padding: 0;
}

.setting-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f0f0f0;
}

.setting-item:last-child {
  border-bottom: none;
}

.label {
  font-size: 14px;
  color: #606266;
}

.value {
  font-size: 14px;
  color: #303133;
}
</style>