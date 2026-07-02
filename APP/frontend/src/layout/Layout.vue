<script setup lang="ts">
import { ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  Menu,
  MenuItem,
  SubMenu,
  Avatar,
  Dropdown,
  DropdownMenu,
  DropdownItem,
  User,
  LogOut,
  HomeFilled,
  Food,
  Warning,
  DataAnalysis,
  Setting
} from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const collapsed = ref(false)

const menuItems = [
  { path: '/dashboard', icon: HomeFilled, label: '首页' },
  { path: '/food', icon: Food, label: '食材管理' },
  { path: '/expiring', icon: Warning, label: '临期预警' },
  { path: '/reports', icon: DataAnalysis, label: '数据报表' },
  { path: '/settings', icon: Setting, label: '系统设置' }
]

function handleLogout() {
  userStore.logout()
  router.push('/login')
}

function getActiveMenu() {
  return route.path
}
</script>

<template>
  <el-container style="height: 100vh;">
    <el-aside :width="collapsed ? '64px' : '200px'" class="aside">
      <div class="logo">
        <span v-if="!collapsed" class="logo-text">食库管家</span>
      </div>
      <el-menu
        :default-active="getActiveMenu()"
        :collapse="collapsed"
        router
        class="menu"
      >
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon :size="18"><component :is="item.icon" /></el-icon>
          <template #title>{{ item.label }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="header-left">
          <el-button @click="collapsed = !collapsed" icon="Menu" />
          <span class="title">{{ route.meta.title || '食库管家' }}</span>
        </div>
        <div class="header-right">
          <el-dropdown>
            <div class="user-info">
              <el-avatar :icon="User" />
              <span>{{ userStore.username }}</span>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="handleLogout" icon="LogOut">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.aside {
  background-color: #304156;
  color: #fff;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  border-bottom: 1px solid #263445;
}

.logo-text {
  color: #fff;
}

.menu {
  height: calc(100vh - 60px);
  border-right: none;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  background-color: #fff;
  border-bottom: 1px solid #e6e6e6;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.title {
  font-size: 16px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.main {
  background-color: #f5f7fa;
  padding: 20px;
}
</style>