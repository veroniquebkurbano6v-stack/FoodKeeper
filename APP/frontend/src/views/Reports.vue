<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Card, Row, Col, Statistic } from 'element-plus'
import { Food, Warning, Delete, ShoppingCart } from '@element-plus/icons-vue'
import { pageFoodList, listExpiringFood } from '@/api/food'
import type { FoodListVO } from '@/types'

const stats = ref({
  totalFood: 0,
  expiringFood: 0,
  totalQuantity: 0,
  expiredFood: 0
})

const recentFood = ref<FoodListVO[]>([])

onMounted(() => {
  loadData()
})

async function loadData() {
  try {
    const [foodRes, expiringRes] = await Promise.all([
      pageFoodList({ pageNum: 1, pageSize: 10 }),
      listExpiringFood(7)
    ])
    recentFood.value = foodRes.data.records || []
    stats.value.totalFood = foodRes.data.total || 0
    stats.value.expiringFood = expiringRes.data.length || 0
    stats.value.totalQuantity = recentFood.value.reduce((sum, f) => sum + f.quantity, 0)
    stats.value.expiredFood = recentFood.value.filter(f => f.expiryStatus === 'EXPIRED').length
  } catch {
    // 忽略错误
  }
}
</script>

<template>
  <div class="reports">
    <Row :gutter="20">
      <Col :span="6">
        <Card class="stat-card">
          <Statistic title="食材品类" :value="stats.totalFood" :prefix-icon="Food" />
        </Card>
      </Col>
      <Col :span="6">
        <Card class="stat-card">
          <Statistic title="临期预警" :value="stats.expiringFood" :prefix-icon="Warning" />
        </Card>
      </Col>
      <Col :span="6">
        <Card class="stat-card">
          <Statistic title="库存总量" :value="stats.totalQuantity" :prefix-icon="ShoppingCart" />
        </Card>
      </Col>
      <Col :span="6">
        <Card class="stat-card">
          <Statistic title="已过期" :value="stats.expiredFood" :prefix-icon="Delete" />
        </Card>
      </Col>
    </Row>

    <Card class="detail-card" style="margin-top: 20px;">
      <template #header>
        <span>库存明细</span>
      </template>
      <table class="detail-table">
        <thead>
          <tr>
            <th>名称</th>
            <th>数量</th>
            <th>单位</th>
            <th>状态</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="food in recentFood" :key="food.id">
            <td>{{ food.name }}</td>
            <td>{{ food.quantity }}</td>
            <td>{{ food.unit }}</td>
            <td>
              <el-tag :type="getStatusType(food.expiryStatus)">
                {{ getStatusLabel(food.expiryStatus) }}
              </el-tag>
            </td>
          </tr>
        </tbody>
      </table>
    </Card>
  </div>
</template>

<script lang="ts">
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

<style scoped>
.reports {
  padding: 0;
}

.stat-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.detail-card {
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.detail-table {
  width: 100%;
  border-collapse: collapse;
}

.detail-table th,
.detail-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.detail-table th {
  background-color: #fafafa;
  font-weight: bold;
  color: #606266;
}
</style>