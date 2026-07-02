<template>
  <div class="expiring-page">
    <van-nav-bar title="临期预警" left-arrow @click-left="goBack" />
    
    <div class="stats-bar">
      <div class="stat-item red">
        <van-icon name="fire-o" size="20" class="stat-icon" />
        <span class="stat-value">{{ redCount }}</span>
        <span class="stat-label">即将过期</span>
      </div>
      <div class="stat-item yellow">
        <van-icon name="clock-o" size="20" class="stat-icon" />
        <span class="stat-value">{{ yellowCount }}</span>
        <span class="stat-label">临期</span>
      </div>
      <div class="stat-item expired">
        <van-icon name="warning-o" size="20" class="stat-icon" />
        <span class="stat-value">{{ expiredCount }}</span>
        <span class="stat-label">已过期</span>
      </div>
    </div>

    <van-tabs v-model="activeTab">
      <van-tab title="全部" name="all">
        <div class="tab-content">
          <div class="sk-food-grid">
            <div
              v-for="food in expiringFoods"
              :key="food.id"
              class="sk-food-card"
              :class="cardMeta(food).status"
              @click="goEditFood(food.id)"
            >
              <span class="sk-card-bar" :style="{ background: cardMeta(food).catDot }"></span>
              <div class="sk-food-top">
                <span class="sk-status-dot" :class="cardMeta(food).dotClass"></span>
                <span class="sk-food-days">{{ cardMeta(food).daysText }}</span>
              </div>
              <div class="sk-food-name">{{ food.name }}</div>
              <div class="sk-food-meta">
                <span class="sk-food-qty">{{ food.quantity }}{{ food.unit }}</span>
                <span class="sk-food-sep">·</span>
                <span class="sk-food-cat">{{ cardMeta(food).catLabel }}</span>
              </div>
            </div>
          </div>

          <div v-if="expiringFoods.length === 0" class="empty-state">
            <van-empty description="暂无临期食材" />
          </div>
        </div>
      </van-tab>

      <van-tab title="即将过期" name="red">
        <div class="tab-content">
          <div class="sk-food-grid">
            <div
              v-for="food in redFoods"
              :key="food.id"
              class="sk-food-card red"
              @click="goEditFood(food.id)"
            >
              <span class="sk-card-bar" :style="{ background: cardMeta(food).catDot }"></span>
              <div class="sk-food-top">
                <span class="sk-status-dot danger"></span>
                <span class="sk-food-days">{{ cardMeta(food).daysText }}</span>
              </div>
              <div class="sk-food-name">{{ food.name }}</div>
              <div class="sk-food-meta">
                <span class="sk-food-qty">{{ food.quantity }}{{ food.unit }}</span>
                <span class="sk-food-sep">·</span>
                <span class="sk-food-cat">{{ cardMeta(food).catLabel }}</span>
              </div>
            </div>
          </div>

          <div v-if="redFoods.length === 0" class="empty-state">
            <van-empty description="暂无即将过期食材" />
          </div>
        </div>
      </van-tab>

      <van-tab title="已过期" name="expired">
        <div class="tab-content">
          <div class="sk-food-grid">
            <div
              v-for="food in expiredFoods"
              :key="food.id"
              class="sk-food-card expired"
              @click="goEditFood(food.id)"
            >
              <span class="sk-card-bar" :style="{ background: cardMeta(food).catDot }"></span>
              <div class="sk-food-top">
                <span class="sk-status-dot expired"></span>
                <span class="sk-food-days">{{ cardMeta(food).daysText }}</span>
              </div>
              <div class="sk-food-name">{{ food.name }}</div>
              <div class="sk-food-meta">
                <span class="sk-food-qty">{{ food.quantity }}{{ food.unit }}</span>
                <span class="sk-food-sep">·</span>
                <span class="sk-food-cat">{{ cardMeta(food).catLabel }}</span>
              </div>
              <div class="sk-food-actions">
                <van-button text type="danger" size="small" @click.stop="handleDiscard(food)">
                  报废处理
                </van-button>
              </div>
            </div>
          </div>

          <div v-if="expiredFoods.length === 0" class="empty-state">
            <van-empty description="暂无已过期食材" />
          </div>
        </div>
      </van-tab>
    </van-tabs>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'
import { calculateRemainingDays, getExpiryStatus, getExpiryStatusLabel, getExpiryStatusColor, getCategoryLabel, getStorageZoneLabel, getCategoryColors, getFoodCardMeta } from '../utils'

const router = useRouter()
const store = useInventoryStore()

const activeTab = ref('all')

onMounted(() => {
  store.loadData()
})

function cardMeta(food) {
  return getFoodCardMeta(food, getCategoryLabel(food.category, store.categories))
}

const expiringFoods = computed(() => {
  return store.foods.filter(food => {
    const status = getExpiryStatus(calculateRemainingDays(food.expiryDate))
    return status === 'yellow' || status === 'red' || status === 'expired'
  }).sort((a, b) => {
    const statusOrder = { expired: 0, red: 1, yellow: 2 }
    const statusA = getExpiryStatus(calculateRemainingDays(a.expiryDate))
    const statusB = getExpiryStatus(calculateRemainingDays(b.expiryDate))
    if (statusOrder[statusA] !== statusOrder[statusB]) {
      return statusOrder[statusA] - statusOrder[statusB]
    }
    return calculateRemainingDays(a.expiryDate) - calculateRemainingDays(b.expiryDate)
  })
})

const redFoods = computed(() => {
  return store.foods.filter(food => getExpiryStatus(calculateRemainingDays(food.expiryDate)) === 'red')
})

const yellowFoods = computed(() => {
  return store.foods.filter(food => getExpiryStatus(calculateRemainingDays(food.expiryDate)) === 'yellow')
})

const expiredFoods = computed(() => {
  return store.foods.filter(food => getExpiryStatus(calculateRemainingDays(food.expiryDate)) === 'expired')
})

const redCount = computed(() => redFoods.value.length)
const yellowCount = computed(() => yellowFoods.value.length)
const expiredCount = computed(() => expiredFoods.value.length)

function getRemainingText(food) {
  const remaining = calculateRemainingDays(food.expiryDate)
  if (remaining < 0) return `已过期 ${Math.abs(remaining)} 天`
  if (remaining === 0) return '今天过期'
  return `剩余 ${remaining} 天`
}

function goEditFood(id) {
  router.push(`/edit-food/${id}`)
}

function handleDiscard(food) {
  if (confirm(`确定要将 ${food.name} 标记为报废吗？`)) {
    try {
      store.discardOutbound(food.id, 'expired')
      showToast({ message: '报废成功', type: 'success' })
    } catch (error) {
      showToast({ message: error.message, type: 'error' })
    }
  }
}

function goBack() {
  router.back()
}

function showToast({ message, type }) {
  const toast = document.createElement('div')
  toast.style.cssText = `
    position: fixed;
    top: 50%;
    left: 50%;
    transform: translate(-50%, -50%);
    background: rgba(0,0,0,0.7);
    color: white;
    padding: 12px 24px;
    border-radius: 8px;
    font-size: 14px;
    z-index: 9999;
  `
  toast.textContent = message
  document.body.appendChild(toast)
  setTimeout(() => toast.remove(), 1500)
}
</script>

<style scoped>
.expiring-page {
  padding-bottom: 60px;
  background: var(--sk-bg);
}

.stats-bar {
  display: flex;
  padding: 16px;
  gap: 12px;
}

.stat-item {
  flex: 1;
  text-align: center;
  padding: 14px 8px;
  border-radius: 14px;
  border: 1px solid transparent;
}

.stat-item.red {
  background: var(--sk-danger-bg);
  border-color: rgba(199, 93, 77, 0.18);
}

.stat-item.yellow {
  background: var(--sk-warn-bg);
  border-color: rgba(212, 165, 91, 0.2);
}

.stat-item.expired {
  background: var(--sk-expired-bg);
  border-color: rgba(154, 142, 128, 0.2);
}

.stat-value {
  display: block;
  font-size: 26px;
  font-weight: 700;
  margin-bottom: 4px;
}

.stat-item.red .stat-value {
  color: var(--sk-danger);
}

.stat-item.yellow .stat-value {
  color: var(--sk-warn);
}

.stat-item.expired .stat-value {
  color: var(--sk-expired);
}

.stat-label {
  font-size: 12px;
  color: var(--sk-text-2);
}

.stat-icon {
  display: block;
  margin: 0 auto 6px;
}

.stat-item.red .stat-icon {
  color: var(--sk-danger);
}

.stat-item.yellow .stat-icon {
  color: var(--sk-warn);
}

.stat-item.expired .stat-icon {
  color: var(--sk-expired);
}

.tab-content {
  padding: 16px;
}

/* 报废按钮在已过期卡内弱化主点击区 */
.sk-food-actions .van-button--danger {
  --van-button-danger-background: var(--sk-danger);
  --van-button-danger-border-color: var(--sk-danger);
}

.empty-state {
  padding: 40px 0;
}
</style>