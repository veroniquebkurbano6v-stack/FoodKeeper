<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Food, ShoppingCart, Warning, Delete } from '@element-plus/icons-vue'
import { pageFoodList, listExpiringFood } from '@/api/food'
import type { FoodListVO } from '@/types'

const stats = ref({
  totalFood: 0,
  expiringFood: 0,
  totalQuantity: 0,
  expiredFood: 0
})

const recentFood = ref<FoodListVO[]>([])
const loading = ref(true)

onMounted(() => {
  loadDashboard()
})

async function loadDashboard() {
  loading.value = true
  try {
    const [foodRes, expiringRes] = await Promise.all([
      pageFoodList({ pageNum: 1, pageSize: 5 }),
      listExpiringFood(7)
    ])
    recentFood.value = foodRes.data?.records || []
    stats.value.totalFood = foodRes.data?.total || 0
    stats.value.expiringFood = expiringRes.data?.length || 0
    stats.value.totalQuantity = recentFood.value.reduce((sum, f) => sum + (f.quantity || 0), 0)
    stats.value.expiredFood = recentFood.value.filter(f => f.expiryStatus === 'EXPIRED').length
  } catch (error) {
    console.error('加载数据失败:', error)
    // 使用模拟数据，确保页面正常显示
    stats.value = {
      totalFood: 0,
      expiringFood: 0,
      totalQuantity: 0,
      expiredFood: 0
    }
    recentFood.value = []
  } finally {
    loading.value = false
  }
}

function getStatusType(status: string) {
  switch (status) {
    case 'GREEN': return 'success'
    case 'YELLOW': return 'warning'
    case 'RED': return 'danger'
    case 'EXPIRED': return 'info'
    default: return 'default'
  }
}

function getStatusLabel(status: string) {
  switch (status) {
    case 'GREEN': return '正常'
    case 'YELLOW': return '临期'
    case 'RED': return '即将过期'
    case 'EXPIRED': return '已过期'
    default: return status
  }
}
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="食材品类" :value="stats.totalFood">
            <template #prefix>
              <el-icon><Food /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="临期预警" :value="stats.expiringFood">
            <template #prefix>
              <el-icon><Warning /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="库存总量" :value="stats.totalQuantity">
            <template #prefix>
              <el-icon><ShoppingCart /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card">
          <el-statistic title="已过期" :value="stats.expiredFood">
            <template #prefix>
              <el-icon><Delete /></el-icon>
            </template>
          </el-statistic>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="recent-card" style="margin-top: 20px;">
      <template #header>
        <span>最近食材</span>
      </template>
      <el-empty v-if="recentFood.length === 0" description="暂无食材数据" />
      <table v-else class="food-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>数量</th>
            <th>单位</th>
            <th>保质期截止</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="food in recentFood" :key="food.id">
            <td>{{ food.name }}</td>
            <td>{{ food.quantity }}</td>
            <td>{{ food.unit }}</td>
            <td>{{ food.expiryDate }}</td>
            <td>
              <el-tag :type="getStatusType(food.expiryStatus)">
                {{ getStatusLabel(food.expiryStatus) }}
              </el-tag>
            </td>
          </tr>
        </tbody>
      </table>
    </el-card>
  </div>
</template>

<style scoped>
.dashboard {
  padding: 0;
}

.stat-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.recent-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.food-table {
  width: 100%;
  border-collapse: collapse;
}

.food-table th,
.food-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.food-table th {
  background-color: #fafafa;
  font-weight: bold;
  color: #606266;
}
</style>