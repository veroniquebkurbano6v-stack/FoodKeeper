<template>
  <div class="home-page">
    <van-nav-bar title="食库管家" />
    
    <div class="header-card">
      <div class="header-bg"></div>
      <div class="header-content">
        <div class="header-icon">🥬</div>
        <h1>食库管家</h1>
        <p>让食材管理更简单</p>
      </div>
    </div>

    <!-- 我的菜谱入口：首屏视觉焦点区域，醒目快捷入口 -->
    <div class="my-recipes-entry" @click="goMyRecipes">
      <div class="my-recipes-icon">
        <van-icon name="bookmark-o" size="24" />
      </div>
      <div class="my-recipes-text">
        <span class="my-recipes-title">我的菜谱</span>
        <span class="my-recipes-desc">{{ recipeStore.favoriteCount }} 道收藏菜谱，点击查看</span>
      </div>
      <div class="my-recipes-badge" v-if="recipeStore.favoriteCount > 0">
        {{ recipeStore.favoriteCount }}
      </div>
      <van-icon name="arrow" size="16" class="my-recipes-arrow" />
    </div>

    <van-grid :column-num="2" :border="false" class="stats-grid">
      <van-grid-item icon="apps-o" text="食材品类" :value="store.totalCategories" />
      <van-grid-item icon="bag-o" text="总数量" :value="store.totalQuantity" />
      <van-grid-item icon="clock-o" text="临期预警" :value="store.expiringCount" :color="warnColor" />
      <van-grid-item icon="warning-o" text="已过期" :value="store.expiredCount" :color="dangerColor" />
    </van-grid>

    <div class="quick-actions">
      <van-button type="primary" icon="plus" round block size="large" @click="goAddFood">
        新增食材入库
      </van-button>
    </div>

    <div v-if="expiringFoods.length > 0" class="section">
      <div class="section-header">
        <h2><van-icon name="warning-o" size="16" /> 临期提醒</h2>
        <van-button text type="primary" size="small" @click="goExpiring">查看全部</van-button>
      </div>
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
    </div>

    <div class="section">
      <div class="section-header">
        <h2><van-icon name="apps-o" size="16" /> 快捷操作</h2>
      </div>
      <van-grid :column-num="4" :border="false">
        <van-grid-item icon="shop-o" text="库存管理" @click="goInventory" />
        <van-grid-item icon="arrow-down" text="食材出库" @click="goOutbound" />
        <van-grid-item icon="notes-o" text="AI菜谱" @click="goRecipes" />
        <van-grid-item icon="bar-chart-o" text="数据报表" @click="goReports" />
        <van-grid-item icon="clock-o" text="临期汇总" @click="goExpiring" />
        <van-grid-item icon="records-o" text="出库日志" @click="goLogs" />
        <van-grid-item icon="user-o" text="口味偏好" @click="goPreferences" />
        <van-grid-item icon="setting-o" text="系统设置" @click="goSettings" />
      </van-grid>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'
import { useRecipesStore } from '../stores/recipes'
import { calculateRemainingDays, getExpiryStatus, getExpiryStatusLabel, getExpiryStatusColor, getCategoryColors, getCategoryLabel, getFoodCardMeta } from '../utils'

const router = useRouter()
const store = useInventoryStore()
const recipeStore = useRecipesStore()

// 暖意厨房语义色（避开高饱和原色）
const warnColor = '#D4A55B'
const dangerColor = '#C75D4D'

onMounted(() => {
  store.loadData()
  recipeStore.loadData()
})

const expiringFoods = computed(() => {
  return store.sortedFoods.slice(0, 5).filter(food => {
    const status = getExpiryStatus(calculateRemainingDays(food.expiryDate))
    return status === 'yellow' || status === 'red' || status === 'expired'
  })
})

function cardMeta(food) {
  return getFoodCardMeta(food, getCategoryLabel(food.category, store.categories))
}

function getRemainingText(food) {
  const remaining = calculateRemainingDays(food.expiryDate)
  if (remaining < 0) return `已过期 ${Math.abs(remaining)} 天`
  if (remaining === 0) return '今天过期'
  return `剩余 ${remaining} 天`
}

function goAddFood() {
  router.push('/add-food')
}

function goEditFood(id) {
  router.push(`/edit-food/${id}`)
}

function goInventory() {
  router.push('/inventory')
}

function goOutbound() {
  router.push('/outbound')
}

function goRecipes() {
  router.push('/recipes')
}

// 我的菜谱快捷入口：直跳菜谱页并激活「我的菜谱」分类
function goMyRecipes() {
  router.push({ path: '/recipes', query: { tab: 'favorites' } })
}

function goReports() {
  router.push('/reports')
}

function goExpiring() {
  router.push('/expiring')
}

function goLogs() {
  router.push('/logs')
}

function goPreferences() {
  router.push('/preferences')
}

function goSettings() {
  router.push('/settings')
}
</script>

<style scoped>
.home-page {
  padding: 0 16px;
  padding-top: 16px;
}

.header-card {
  position: relative;
  height: 130px;
  border-radius: 18px;
  overflow: hidden;
  margin-bottom: 20px;
  box-shadow: 0 8px 24px rgba(94, 122, 86, 0.18);
}

.header-bg {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: linear-gradient(135deg, #7c9473 0%, #6e8ca8 100%);
}

.header-bg::after {
  content: '❄';
  position: absolute;
  right: 18px;
  top: 14px;
  font-size: 22px;
  opacity: 0.55;
  color: #fffefb;
}

.header-content {
  position: relative;
  z-index: 1;
  padding: 22px 22px;
  color: #fffefb;
  display: flex;
  flex-direction: column;
  height: 100%;
  justify-content: center;
}

.header-icon {
  font-size: 30px;
  margin-bottom: 4px;
}

.header-content h1 {
  font-size: 24px;
  font-weight: 700;
  margin-bottom: 4px;
  letter-spacing: 1px;
}

.header-content p {
  font-size: 13px;
  opacity: 0.92;
}

/* 我的菜谱入口：首屏视觉焦点，醒目快捷入口 */
.my-recipes-entry {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 20px;
  background: linear-gradient(135deg, #C97B5C 0%, #D4A55B 100%);
  border-radius: 14px;
  box-shadow: 0 6px 18px rgba(201, 123, 92, 0.28);
  cursor: pointer;
  transition: transform 0.15s ease, box-shadow 0.15s ease;
  position: relative;
  overflow: hidden;
}

.my-recipes-entry:active {
  transform: scale(0.98);
  box-shadow: 0 3px 10px rgba(201, 123, 92, 0.22);
}

.my-recipes-entry::before {
  content: '📖';
  position: absolute;
  right: 50px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 56px;
  opacity: 0.18;
  pointer-events: none;
}

.my-recipes-icon {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(255, 254, 251, 0.25);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fffefb;
  flex-shrink: 0;
  z-index: 1;
}

.my-recipes-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  z-index: 1;
}

.my-recipes-title {
  font-size: 17px;
  font-weight: 700;
  color: #fffefb;
  letter-spacing: 0.5px;
}

.my-recipes-desc {
  font-size: 12px;
  color: rgba(255, 254, 251, 0.88);
}

.my-recipes-badge {
  min-width: 22px;
  height: 22px;
  padding: 0 7px;
  border-radius: 11px;
  background: #fffefb;
  color: #C75D4D;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.my-recipes-arrow {
  color: rgba(255, 254, 251, 0.88);
  z-index: 1;
}

.stats-grid {
  margin-bottom: 20px;
  background: var(--sk-card);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 10px rgba(61, 53, 46, 0.04);
}

.quick-actions {
  margin-bottom: 24px;
}

.quick-actions .van-button {
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}

.section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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
</style>