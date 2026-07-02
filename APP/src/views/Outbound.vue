<template>
  <div class="outbound-page">
    <van-nav-bar title="食材出库" left-arrow @click-left="goBack" />
    
    <van-tabs v-model="activeTab">
      <van-tab title="烹饪消耗" name="cook">
        <div class="tab-content">
          <van-cell-group v-for="food in availableFoods" :key="food.id">
            <van-cell :border="false" class="food-item" :class="getExpiryStatus(calculateRemainingDays(food.expiryDate))">
              <template #icon>
                <span class="cat-bar" :style="{ background: getCategoryColors(food.category).dot }"></span>
              </template>
              <div class="food-checkbox">
                <van-checkbox
                  v-model="selectedFoods[food.id]"
                  @change="onFoodSelect(food)"
                />
              </div>
              <div class="food-info">
                <div class="food-header">
                  <span class="food-name">{{ food.name }}</span>
                  <span
                    v-if="isUrgent(food)"
                    class="sk-status-dot"
                    :class="urgentDotClass(food)"
                  ></span>
                  <van-tag :color="getExpiryStatusColor(getExpiryStatus(calculateRemainingDays(food.expiryDate)))" size="small">
                    {{ getExpiryStatusLabel(getExpiryStatus(calculateRemainingDays(food.expiryDate))) }}
                  </van-tag>
                </div>
                <div class="food-meta">
                  <span>{{ food.quantity }}{{ food.unit }}</span>
                  <span>{{ getCategoryLabel(food.category, store.categories) }}</span>
                </div>
              </div>
              <div class="quantity-input" v-if="selectedFoods[food.id]">
                <van-stepper
                  v-model="quantities[food.id]"
                  :min="1"
                  :max="food.quantity"
                  @change="onQuantityChange(food)"
                />
              </div>
            </van-cell>
          </van-cell-group>
          
          <div v-if="availableFoods.length === 0" class="empty-state">
            <van-empty description="暂无可用食材" />
          </div>
          
          <div class="submit-bar">
            <span>已选择 {{ selectedCount }} 项</span>
            <van-button
              type="primary"
              :disabled="selectedCount === 0"
              icon="fire-o"
              round
              size="large"
              @click="handleCookOutbound"
            >
              确认消耗
            </van-button>
          </div>
        </div>
      </van-tab>
      
      <van-tab title="变质报废" name="discard">
        <div class="tab-content">
          <van-cell-group v-for="food in expiringOrExpiredFoods" :key="food.id">
            <van-cell :border="false" class="food-item" :class="getExpiryStatus(calculateRemainingDays(food.expiryDate))">
              <template #icon>
                <span class="cat-bar" :style="{ background: getCategoryColors(food.category).dot }"></span>
              </template>
              <div class="food-info">
                <div class="food-header">
                  <span class="food-name">{{ food.name }}</span>
                  <span class="sk-status-dot" :class="urgentDotClass(food)"></span>
                  <van-tag :color="getExpiryStatusColor(getExpiryStatus(calculateRemainingDays(food.expiryDate)))" size="small">
                    {{ getExpiryStatusLabel(getExpiryStatus(calculateRemainingDays(food.expiryDate))) }}
                  </van-tag>
                </div>
                <div class="food-meta">
                  <span>库存：{{ food.quantity }}{{ food.unit }}</span>
                </div>
              </div>
              <van-button text type="danger" size="small" @click="handleDiscard(food)">
                报废
              </van-button>
            </van-cell>
          </van-cell-group>
          
          <div v-if="expiringOrExpiredFoods.length === 0" class="empty-state">
            <van-empty description="暂无需要报废的食材" />
          </div>
        </div>
      </van-tab>
      
      <van-tab title="存量清零" name="clear">
        <div class="tab-content">
          <van-cell-group v-for="food in store.foods" :key="food.id">
            <van-cell :border="false" class="food-item">
              <div class="food-info">
                <div class="food-header">
                  <span class="food-name">{{ food.name }}</span>
                </div>
                <div class="food-meta">
                  <span>库存：{{ food.quantity }}{{ food.unit }}</span>
                </div>
              </div>
              <van-button text type="warning" size="small" @click="handleClear(food)">
                清零
              </van-button>
            </van-cell>
          </van-cell-group>
        </div>
      </van-tab>
    </van-tabs>

    <van-action-sheet v-model:show="showReasonSheet" title="选择报废原因" :actions="reasonActions" @select="onReasonSelect" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'
import { calculateRemainingDays, getExpiryStatus, getExpiryStatusLabel, getExpiryStatusColor, getCategoryLabel, getCategoryColors } from '../utils'

const router = useRouter()
const store = useInventoryStore()

const activeTab = ref('cook')
const selectedFoods = ref({})
const quantities = ref({})
const showReasonSheet = ref(false)
const currentDiscardFood = ref(null)

const reasonActions = [
  { name: '过期变质', value: 'expired' },
  { name: '储存不当', value: 'storage' },
  { name: '其他', value: 'other' }
]

onMounted(() => {
  store.loadData()
})

function isUrgent(food) {
  const status = getExpiryStatus(calculateRemainingDays(food.expiryDate))
  return status === 'red' || status === 'yellow' || status === 'expired'
}

function urgentDotClass(food) {
  const status = getExpiryStatus(calculateRemainingDays(food.expiryDate))
  if (status === 'red') return 'danger'
  if (status === 'yellow') return 'warn'
  if (status === 'expired') return 'expired'
  return 'fresh'
}

const availableFoods = computed(() => {
  return store.foods.filter(food => food.quantity > 0)
})

const expiringOrExpiredFoods = computed(() => {
  return store.foods.filter(food => {
    const status = getExpiryStatus(calculateRemainingDays(food.expiryDate))
    return food.quantity > 0 && (status === 'yellow' || status === 'red' || status === 'expired')
  })
})

const selectedCount = computed(() => {
  return Object.values(selectedFoods.value).filter(Boolean).length
})

function onFoodSelect(food) {
  if (!quantities.value[food.id]) {
    quantities.value[food.id] = 1
  }
}

function onQuantityChange(food) {
  if (quantities.value[food.id] > food.quantity) {
    quantities.value[food.id] = food.quantity
  }
}

function handleCookOutbound() {
  const foodIds = []
  const foodQuantities = []
  
  Object.keys(selectedFoods.value).forEach(id => {
    if (selectedFoods.value[id]) {
      foodIds.push(id)
      foodQuantities.push(quantities.value[id] || 1)
    }
  })
  
  try {
    store.cookOutbound(foodIds, foodQuantities)
    showToast({ message: '出库成功', type: 'success' })
    selectedFoods.value = {}
    quantities.value = {}
  } catch (error) {
    showToast({ message: error.message, type: 'error' })
  }
}

function handleDiscard(food) {
  currentDiscardFood.value = food
  showReasonSheet.value = true
}

function onReasonSelect(action) {
  showReasonSheet.value = false
  if (currentDiscardFood.value) {
    try {
      store.discardOutbound(currentDiscardFood.value.id, action.value)
      showToast({ message: '报废成功', type: 'success' })
    } catch (error) {
      showToast({ message: error.message, type: 'error' })
    }
  }
}

function handleClear(food) {
  if (confirm(`确定要将 ${food.name} 的库存清零吗？`)) {
    try {
      store.clearInventory(food.id)
      showToast({ message: '清零成功', type: 'success' })
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
.outbound-page {
  padding-bottom: 80px;
  background: var(--sk-bg);
}

.tab-content {
  padding: 16px;
}

.food-item {
  display: flex;
  align-items: center;
  padding: 12px 14px;
  background: var(--sk-card);
  border-radius: 12px;
  margin-bottom: 10px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
  position: relative;
  overflow: hidden;
}

.cat-bar {
  position: absolute;
  left: 0;
  top: 10px;
  bottom: 10px;
  width: 4px;
  border-radius: 0 2px 2px 0;
}

.food-item.red {
  background: var(--sk-danger-bg);
}
.food-item.yellow {
  background: var(--sk-warn-bg);
}
.food-item.expired {
  background: var(--sk-expired-bg);
  opacity: 0.88;
}

.food-checkbox {
  margin-right: 12px;
}

.food-info {
  flex: 1;
}

.food-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.food-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--sk-text);
  margin-right: auto;
}

.food-meta {
  display: flex;
  gap: 8px;
}

.food-meta span {
  font-size: 12px;
  color: var(--sk-text-2);
}

.quantity-input {
  margin-left: 12px;
}

.submit-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--sk-card);
  border-top: 1px solid var(--sk-border);
  z-index: 10;
}

.submit-bar span {
  font-size: 14px;
  color: var(--sk-text-2);
}

.submit-bar .van-button {
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}

.empty-state {
  padding: 40px 0;
}
</style>