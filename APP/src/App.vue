<template>
  <div class="app-container">
    <router-view />
    <van-tabbar v-model="active" route mode="horizontal" v-if="showTabbar">
      <van-tabbar-item icon="home-o" to="/">首页</van-tabbar-item>
      <van-tabbar-item icon="shop-o" to="/inventory">库存</van-tabbar-item>
      <van-tabbar-item icon="notes-o" to="/recipes">菜谱</van-tabbar-item>
      <van-tabbar-item icon="bar-chart-o" to="/reports">报表</van-tabbar-item>
    </van-tabbar>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()

const showTabbar = computed(() => {
  const hiddenRoutes = ['/add-food', '/edit-food', '/outbound', '/expiring', '/preferences', '/shopping-plan', '/logs', '/settings']
  return !hiddenRoutes.includes(route.path)
})

const active = computed(() => {
  const pathMap = {
    '/': 0,
    '/inventory': 1,
    '/recipes': 2,
    '/reports': 3
  }
  return pathMap[route.path] || 0
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto,
    'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', sans-serif;
  background-color: var(--sk-bg, #faf6ef);
  color: var(--sk-text, #3d352e);
}

.app-container {
  min-height: 100vh;
  padding-bottom: 50px;
  background-color: var(--sk-bg, #faf6ef);
}
</style>