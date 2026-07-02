<template>
  <div class="inventory-page">
    <van-nav-bar title="库存清单" right-text="新增" @click-right="goAddFood" />

    <div class="search-bar">
      <van-search v-model="searchQuery" placeholder="搜索食材名称" />
    </div>

    <div class="filter-tabs">
      <van-tabs
        v-model:active="activeFilter"
        :line-width="24"
        :line-height="3"
        :ellipsis="false"
        @change="onTabChange"
      >
        <van-tab
          v-for="cat in filterCategories"
          :key="cat.value"
          :name="cat.value"
        >
          <template #title>
            <span class="cat-tab" :style="{ '--cat-color': cat.color }">
              <span class="cat-tab-label">{{ cat.label }}</span>
              <span
                v-if="getCategoryCount(cat.value) > 0"
                class="cat-tab-count"
              >{{ getCategoryCount(cat.value) }}</span>
            </span>
          </template>
        </van-tab>
      </van-tabs>
    </div>

    <div class="sort-bar">
      <van-button text size="small" @click="toggleSort">
        {{ sortLabel }}
      </van-button>
      <span v-if="!initialLoading" class="result-count">
        共 {{ filteredFoods.length }} 项
      </span>
    </div>

    <!-- 初始加载骨架屏：数据就绪前提供加载状态反馈 -->
    <div v-if="initialLoading" class="skeleton-grid">
      <div v-for="n in 6" :key="n" class="skeleton-card">
        <van-skeleton :row="3" :animate="true" class="skeleton-inner" />
      </div>
    </div>

    <!-- 加载失败错误状态（含重试） -->
    <div v-else-if="loadError" class="error-state">
      <van-empty image="error" description="数据加载失败，请重试" />
      <div class="error-retry">
        <van-button size="small" type="primary" round @click="retryLoad">重新加载</van-button>
      </div>
    </div>

    <!-- 食材网格 / 空状态：分类切换时淡入淡出过渡 -->
    <transition v-else name="grid-fade" mode="out-in">
      <div
        v-if="filteredFoods.length > 0"
        class="sk-food-grid"
        :key="activeFilter + '|' + sortBy"
      >
        <div
          v-for="food in filteredFoods"
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
      <div v-else class="empty-state" :key="'empty-' + activeFilter">
        <van-empty :description="emptyDescription" :image="emptyImage" />
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'
import {
  calculateRemainingDays,
  getExpiryStatus,
  getCategoryLabel,
  getCategoryColors,
  getFoodCardMeta
} from '../utils'

const router = useRouter()
const route = useRoute()
const store = useInventoryStore()

const searchQuery = ref('')
const activeFilter = ref('all')
const sortBy = ref('expiry')
// 初始加载态：挂载后置 true，数据就绪 + 下一帧后关闭，保证骨架屏可见（即便 localStorage 读取很快）
const initialLoading = ref(true)
// 加载错误态：loadData 抛异常时置 true，展示错误占位 + 重试按钮
const loadError = ref(false)

// 顶部筛选分类（「全部」+ 八大分类），每项附分类色用于激活态角标强调
const filterCategories = computed(() => {
  const all = [{ value: 'all', label: '全部', color: 'var(--sk-primary)' }]
  return all.concat(
    store.categories.map(c => {
      const colors = getCategoryColors(c.value)
      return { value: c.value, label: c.label, color: colors.dot }
    })
  )
})

// 加载数据：含异常捕获，失败时切到错误态而非吞掉异常
function loadInventoryData() {
  initialLoading.value = true
  loadError.value = false
  try {
    store.loadData()
  } catch (e) {
    console.error('[库存清单] 加载数据失败:', e)
    loadError.value = true
  }
  nextTick(() => {
    initialLoading.value = false
  })
}

onMounted(() => {
  // 从路由 query 恢复筛选状态（刷新 / 路由切换返回后保持分类）
  const cat = route.query.cat
  if (typeof cat === 'string' && filterCategories.value.some(c => c.value === cat)) {
    activeFilter.value = cat
  }
  loadInventoryData()
})

// tab 切换：同步筛选到路由 query（replace 避免污染浏览历史栈）
function onTabChange(name) {
  router.replace({ query: { ...route.query, cat: name } })
}

function retryLoad() {
  loadInventoryData()
}

function cardMeta(food) {
  return getFoodCardMeta(food, getCategoryLabel(food.category, store.categories))
}

const sortLabel = computed(() => {
  return sortBy.value === 'expiry' ? '按保质期排序' : '按入库时间排序'
})

// 按分类统计数量（用于 tab 角标）
function getCategoryCount(value) {
  if (value === 'all') return store.foods.length
  return store.foods.filter(f => f.category === value).length
}

const filteredFoods = computed(() => {
  // 防御性过滤：剔除异常数据条目，避免单条脏数据导致整列表渲染崩溃
  let result = store.foods.filter(f => f && f.name && f.category)

  if (searchQuery.value) {
    result = result.filter(food => food.name.includes(searchQuery.value))
  }

  if (activeFilter.value !== 'all') {
    result = result.filter(food => food.category === activeFilter.value)
  }

  if (sortBy.value === 'expiry') {
    result.sort((a, b) => {
      const statusOrder = { expired: 0, red: 1, yellow: 2, green: 3, normal: 4 }
      const statusA = getExpiryStatus(calculateRemainingDays(a.expiryDate))
      const statusB = getExpiryStatus(calculateRemainingDays(b.expiryDate))
      if (statusOrder[statusA] !== statusOrder[statusB]) {
        return statusOrder[statusA] - statusOrder[statusB]
      }
      return calculateRemainingDays(a.expiryDate) - calculateRemainingDays(b.expiryDate)
    })
  } else {
    result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  }

  return result
})

// 空状态文案区分：搜索无结果 / 分类无食材 / 全库为空
const emptyDescription = computed(() => {
  if (searchQuery.value) {
    return `未找到包含「${searchQuery.value}」的食材`
  }
  if (activeFilter.value === 'all') {
    return '暂无食材，点击右上角「新增」添加'
  }
  const cat = store.categories.find(c => c.value === activeFilter.value)
  return `「${cat?.label || activeFilter}」分类暂无食材`
})

const emptyImage = computed(() => {
  if (searchQuery.value) return 'search'
  return 'default'
})

function toggleSort() {
  sortBy.value = sortBy.value === 'expiry' ? 'date' : 'expiry'
}

function goAddFood() {
  router.push('/add-food')
}

function goEditFood(id) {
  router.push(`/edit-food/${id}`)
}
</script>

<style scoped>
.inventory-page {
  padding-bottom: 60px;
  background: var(--sk-bg);
  min-height: 100vh;
}

.search-bar {
  padding: 12px 16px;
  background: var(--sk-card);
}

.filter-tabs {
  margin-bottom: 8px;
  background: var(--sk-card);
}

/* 分类 tab 标题：标签 + 数量角标 */
.cat-tab {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  --cat-color: var(--sk-primary);
}

.cat-tab-label {
  line-height: 1;
}

.cat-tab-count {
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--sk-section);
  color: var(--sk-text-2);
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  transition: background 0.2s ease, color 0.2s ease;
}

/* 激活态：角标用分类专属色强调，提供强视觉反馈 */
.van-tab--active .cat-tab-count {
  background: var(--cat-color);
  color: #fff;
}

.sort-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 16px;
  margin-bottom: 12px;
}

.sort-bar .van-button {
  color: var(--sk-text-2);
}

.result-count {
  font-size: 12px;
  color: var(--sk-text-3);
}

/* 卡片网格留出左右页边距 */
.sk-food-grid {
  padding: 0 12px;
}

/* 响应式：宽屏自适应多列（移动端 2 列 / 平板 3 列 / 桌面 4 列） */
@media (min-width: 600px) {
  .sk-food-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (min-width: 900px) {
  .sk-food-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

/* 骨架屏网格（与真实网格断点一致） */
.skeleton-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  padding: 0 12px;
}
@media (min-width: 600px) {
  .skeleton-grid {
    grid-template-columns: repeat(3, 1fr);
  }
}
@media (min-width: 900px) {
  .skeleton-grid {
    grid-template-columns: repeat(4, 1fr);
  }
}

.skeleton-card {
  background: var(--sk-card);
  border-radius: 14px;
  padding: 14px 12px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.05);
}

.skeleton-card :deep(.van-skeleton) {
  padding: 0;
}

/* 加载失败错误态 */
.error-state {
  padding: 40px 16px;
  text-align: center;
}

.error-retry {
  margin-top: 8px;
}

.empty-state {
  padding: 40px 0;
}

/* 网格淡入淡出动画：分类/排序切换时提供视觉反馈 */
.grid-fade-enter-active,
.grid-fade-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}
.grid-fade-enter-from {
  opacity: 0;
  transform: translateY(6px);
}
.grid-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
