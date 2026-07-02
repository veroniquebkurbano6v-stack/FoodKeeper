<template>
  <div class="reports-page">
    <van-nav-bar title="数据报表" />
    
    <div class="stats-cards">
      <div class="stat-card">
        <div class="stat-icon fresh">
          <van-icon name="bag-o" size="24" />
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ totalQuantity }}</span>
          <span class="stat-label">库存总量</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon danger">
          <van-icon name="warning-o" size="24" />
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ expiringCount }}</span>
          <span class="stat-label">临期预警</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon warn">
          <van-icon name="delete-o" size="24" />
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ wasteCount }}</span>
          <span class="stat-label">本月报废</span>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon accent">
          <van-icon name="shopping-cart-o" size="24" />
        </div>
        <div class="stat-content">
          <span class="stat-value">{{ store.foods.length }}</span>
          <span class="stat-label">食材品类</span>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h2><van-icon name="chart-trending-o" size="16" /> 分类库存分布</h2>
      </div>
      <div class="category-chart">
        <div v-for="category in categoryStats" :key="category.value" class="chart-item">
          <div class="chart-label">
            <span>{{ category.label }}</span>
            <span>{{ category.count }}种</span>
          </div>
          <div class="chart-bar">
            <div 
              class="chart-fill" 
              :style="{ width: category.percent + '%', backgroundColor: category.color }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h2><van-icon name="description-o" size="16" /> 库存明细</h2>
      </div>
      <div class="sk-food-grid">
        <div
          v-for="food in store.foods.slice(0, 10)"
          :key="food.id"
          class="sk-food-card"
        >
          <span class="sk-card-bar" :style="{ background: getCategoryColors(food.category).dot }"></span>
          <div class="sk-food-name">{{ food.name }}</div>
          <div class="sk-food-meta">
            <span class="sk-food-qty">{{ food.quantity }}{{ food.unit }}</span>
            <span class="sk-food-sep">·</span>
            <span class="sk-reports-price">¥{{ food.totalPrice }}</span>
          </div>
          <div class="sk-food-cat">{{ getCategoryLabel(food.category, store.categories) }}</div>
        </div>
      </div>
      <div v-if="store.foods.length > 10" class="view-more">
        <van-button text size="small" @click="goInventory">查看全部</van-button>
      </div>
    </div>

    <div class="section">
      <div class="section-header">
        <h2><van-icon name="records-o" size="16" /> 近期出库记录</h2>
      </div>
      <van-cell-group>
        <van-cell 
          v-for="log in store.logs.slice(0, 5)" 
          :key="log.id" 
          :border="false" 
          class="log-item"
        >
          <div class="log-info">
            <span class="log-name">{{ log.foodName }}</span>
            <span class="log-type">{{ getOutboundTypeLabel(log.outboundType) }}</span>
          </div>
          <div class="log-value">
            <span>-{{ log.quantity }}</span>
            <span class="log-date">{{ log.createdAt }}</span>
          </div>
        </van-cell>
      </van-cell-group>
      <div v-if="store.logs.length > 5" class="view-more">
        <van-button text size="small" @click="goLogs">查看全部</van-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'
import { getCategoryLabel, getOutboundTypeLabel, getCategoryColors } from '../utils'

const router = useRouter()
const store = useInventoryStore()

onMounted(() => {
  store.loadData()
})

const totalQuantity = computed(() => {
  return store.foods.reduce((sum, food) => sum + food.quantity, 0)
})

const expiringCount = computed(() => {
  return store.foods.filter(food => {
    const remaining = (new Date(food.expiryDate) - new Date()) / (1000 * 60 * 60 * 24)
    return remaining <= 7 && remaining >= 0
  }).length
})

const wasteCount = computed(() => {
  const now = new Date()
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  return store.logs.filter(log => log.outboundType === 'discard' && log.createdAt.startsWith(currentMonth)).length
})

const categoryColors = {
  vegetable: '#7C9473',
  meat: '#C97B6E',
  seafood: '#6E8CA8',
  dried: '#A88B6A',
  frozen: '#8BA0B0',
  seasoning: '#C9A94E',
  fruit: '#C75D4D',
  drink: '#9CB088'
}

const categoryStats = computed(() => {
  const counts = {}
  store.foods.forEach(food => {
    counts[food.category] = (counts[food.category] || 0) + 1
  })
  
  const total = store.foods.length || 1
  
  return store.categories.map(cat => ({
    ...cat,
    count: counts[cat.value] || 0,
    percent: Math.round((counts[cat.value] || 0) / total * 100),
    color: categoryColors[cat.value] || '#ccc'
  }))
})

function goInventory() {
  router.push('/inventory')
}

function goLogs() {
  router.push('/logs')
}
</script>

<style scoped>
.reports-page {
  padding-bottom: 60px;
  background: var(--sk-bg);
}

.stats-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  padding: 16px;
}

.stat-card {
  display: flex;
  align-items: center;
  padding: 16px;
  background: var(--sk-card);
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
}

.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
  margin-right: 12px;
}

.stat-icon.fresh {
  background: var(--sk-primary-bg);
  color: var(--sk-primary);
}
.stat-icon.danger {
  background: var(--sk-danger-bg);
  color: var(--sk-danger);
}
.stat-icon.warn {
  background: var(--sk-warn-bg);
  color: var(--sk-warn);
}
.stat-icon.accent {
  background: var(--sk-accent-bg);
  color: var(--sk-accent);
}

.stat-content {
  flex: 1;
}

.stat-value {
  display: block;
  font-size: 20px;
  font-weight: 700;
  color: var(--sk-text);
}

.stat-label {
  font-size: 12px;
  color: var(--sk-text-2);
}

.section {
  padding: 16px;
  background: var(--sk-card);
  margin: 0 12px 12px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
}

.section-header {
  margin-bottom: 12px;
}

.section-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--sk-text);
  position: relative;
  padding-left: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.section-header h2::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 14px;
  border-radius: 2px;
  background: var(--sk-primary);
}

.category-chart {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chart-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.chart-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: var(--sk-text-2);
}

.chart-bar {
  height: 10px;
  background: var(--sk-section);
  border-radius: 5px;
  overflow: hidden;
}

.chart-fill {
  height: 100%;
  border-radius: 5px;
  transition: width 0.4s ease;
}

/* 报表明细卡：价格强调色 */
.sk-reports-price {
  font-weight: 700;
  color: var(--sk-accent);
  font-size: 13px;
}

.sk-food-card .sk-food-cat {
  margin-top: 6px;
}

.log-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  position: relative;
}

.log-info {
  flex: 1;
}

.log-name {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--sk-text);
  margin-bottom: 2px;
}

.log-type {
  font-size: 12px;
  color: var(--sk-text-3);
}

.log-value {
  text-align: right;
}

.log-value span:first-child {
  display: block;
  font-size: 15px;
  font-weight: 600;
  color: var(--sk-text);
}

.log-date {
  font-size: 12px;
  color: var(--sk-text-3);
}

.view-more {
  text-align: center;
  padding: 8px 0;
}
</style>