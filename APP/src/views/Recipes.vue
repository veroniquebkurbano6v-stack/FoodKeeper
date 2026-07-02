<template>
  <div class="recipes-page">
    <van-nav-bar title="AI菜谱推荐" right-text="AI设置" @click-right="openConfig" />

    <!-- 顶部状态条：仅 AI tab 显示 -->
    <div v-if="activeTab !== 'favorites'" class="recipe-header">
      <div class="available-foods">
        <van-tag color="#7C9473" size="medium">可用食材 {{ availableFoods.length }} 种</van-tag>
        <van-tag v-if="urgentCount > 0" color="#C75D4D" size="medium">临期 {{ urgentCount }} 种</van-tag>
      </div>
      <van-button text type="primary" size="small" icon="user-o" @click="goPreferences">口味偏好</van-button>
    </div>

    <!-- 库存不足：仅 AI tab -->
    <div v-if="activeTab !== 'favorites' && availableFoods.length < 3" class="empty-state">
      <van-empty description="库存不足，至少需要 3 种可用食材才能生成菜谱" />
      <van-button type="primary" round block size="large" icon="plus" @click="goAddFood">去采购入库</van-button>
    </div>

    <div v-else>
      <!-- 分类筛选 -->
      <div class="recipe-tabs">
        <van-tabs v-model="activeTab">
          <van-tab title="全部" name="all" />
          <van-tab title="快手懒人餐" name="quick" />
          <van-tab title="减脂轻食餐" name="healthy" />
          <van-tab title="家常正餐" name="home" />
          <van-tab title="我的菜谱" name="favorites" />
        </van-tabs>
      </div>

      <!-- 缓存时间条：仅 AI tab 且有缓存 -->
      <div v-if="activeTab !== 'favorites' && recipeStore.hasCache" class="cache-info">
        <van-icon name="clock-o" size="14" />
        <span>上次生成：{{ formatRelativeTime(recipeStore.cacheTimestamp) }}</span>
      </div>

      <!-- 降级提示条：仅 AI tab -->
      <div v-if="activeTab !== 'favorites' && lastResult && lastResult.useFallback" class="fallback-banner">
        <van-icon name="warning-o" />
        <span>{{ lastResult.message }}</span>
      </div>

      <!-- 加载中 -->
      <div v-if="loading" class="loading-state">
        <van-loading type="spinner" color="#7C9473" size="32px">
          AI 正在为您定制菜谱...
        </van-loading>
        <div class="loading-tip">已等待 {{ elapsedSeconds }} 秒，请稍候</div>
        <van-button plain type="default" size="small" @click="cancelGenerate">取消</van-button>
      </div>

      <!-- 收藏 tab 空状态 -->
      <div v-else-if="activeTab === 'favorites' && recipeStore.favoriteCount === 0" class="empty-state">
        <van-empty description="还没有收藏的菜谱">
          <van-button type="primary" round size="small" @click="activeTab = 'all'">去 AI 菜谱收藏</van-button>
        </van-empty>
      </div>

      <!-- AI tab 空结果（无缓存） -->
      <div v-else-if="activeTab !== 'favorites' && recipes.length === 0" class="empty-state">
        <van-empty description="暂无推荐菜谱" />
        <van-button type="primary" round block size="large" icon="replay" @click="handleGenerate">生成菜谱</van-button>
      </div>

      <!-- 指定分类无匹配菜谱（内容隔离：分类切换后该类为空时提示） -->
      <div v-else-if="activeTab !== 'favorites' && activeTab !== 'all' && filteredRecipes.length === 0" class="empty-state">
        <van-empty :description="`「${getRecipeTagLabel(activeTab)}」分类暂无菜谱`" />
        <div class="category-empty-actions">
          <van-button type="primary" round size="small" @click="activeTab = 'all'">查看全部菜谱</van-button>
          <van-button plain type="primary" round size="small" icon="replay" @click="handleGenerate">重新生成</van-button>
        </div>
      </div>

      <!-- 菜谱列表（AI 推荐 / 收藏 共用同一套卡片） -->
      <div v-else>
        <van-cell-group v-for="(recipe, idx) in filteredRecipes" :key="recipe.signature || recipe.id || idx">
          <van-cell :border="false" class="recipe-card" @click="showRecipeDetail(recipe)">
            <span class="recipe-accent" :style="{ background: getRecipeTagColor(recipe.type) }"></span>
            <div class="recipe-info">
              <div class="recipe-card-header">
                <span class="recipe-name">{{ recipe.name }}</span>
              </div>
              <div class="recipe-card-sub">
                <van-tag :color="getRecipeTagColor(recipe.type)" size="medium">
                  {{ getRecipeTagLabel(recipe.type) }}
                </van-tag>
                <span class="recipe-index">第 {{ idx + 1 }} 道</span>
              </div>
              <div class="recipe-meta">
                <span><van-icon name="clock-o" size="14" /> {{ recipe.duration }}</span>
                <span><van-icon name="fire-o" size="14" /> {{ recipe.calories }}</span>
                <span><van-icon name="chart-trending-o" size="14" /> {{ recipe.difficulty }}</span>
              </div>

              <!-- 来自冰箱的食材 -->
              <div v-if="recipe.fromFridge && recipe.fromFridge.length" class="ing-group">
                <span class="ing-label"><van-icon name="checked" size="14" /> 冰箱已有：</span>
                <span
                  v-for="(ing, i) in recipe.fromFridge"
                  :key="'f' + i"
                  class="ing-tag"
                  :class="getUrgencyClass(ing.daysLeft)"
                >
                  {{ ing.name }} {{ ing.amount }}
                  <em v-if="ing.daysLeft !== undefined">{{ ing.daysLeft }}天</em>
                </span>
              </div>

              <!-- 需额外购买 -->
              <div v-if="recipe.needToBuy && recipe.needToBuy.length" class="ing-group">
                <span class="ing-label buy"><van-icon name="shopping-cart-o" size="14" /> 需购买：</span>
                <span v-for="(ing, i) in recipe.needToBuy" :key="'b' + i" class="ing-tag buy">
                  {{ ing.name }} {{ ing.amount }}
                  <em v-if="ing.estimatedPrice">（{{ ing.estimatedPrice }}）</em>
                </span>
              </div>

              <div class="recipe-card-footer">
                <div class="recipe-steps-preview">
                  <van-icon name="todo-list-o" size="14" /> {{ recipe.steps.length }} 步 · 点击查看详细做法
                </div>
                <div class="footer-actions">
                  <!-- 我的菜谱 tab：快捷完成食谱按钮 -->
                  <div
                    v-if="activeTab === 'favorites' && hasCompletableIngredients(recipe)"
                    class="complete-mini-btn"
                    :class="{ disabled: completing }"
                    @click.stop="handleCompleteRecipe(recipe)"
                  >
                    <van-icon name="success" size="14" />
                    <span>完成食谱</span>
                  </div>
                  <!-- 收藏按钮 -->
                  <div
                    class="fav-btn"
                    :class="{ active: recipeStore.isFavorite(recipe) }"
                    @click.stop="toggleFavorite(recipe)"
                  >
                    <van-icon :name="recipeStore.isFavorite(recipe) ? 'bookmark' : 'bookmark-o'" size="16" />
                    <span>{{ recipeStore.isFavorite(recipe) ? '已收藏' : '收藏' }}</span>
                  </div>
                </div>
              </div>
            </div>
          </van-cell>
        </van-cell-group>

        <!-- 重新生成按钮：仅 AI tab -->
        <div v-if="activeTab !== 'favorites'" class="generate-bar">
          <van-button type="primary" round block size="large" icon="replay" @click="handleGenerate" :loading="loading">
            重新生成菜谱
          </van-button>
        </div>
      </div>
    </div>

    <!-- 菜谱详情弹层 -->
    <van-popup v-model:show="showDetail" position="bottom" :style="{ height: '85%' }" round>
      <div v-if="selectedRecipe" class="recipe-detail">
        <div class="detail-header">
          <h3>{{ selectedRecipe.name }}</h3>
          <van-icon name="cross" size="20" @click="showDetail = false" />
        </div>
        <div class="detail-content">
          <div class="detail-meta">
            <van-tag :color="getRecipeTagColor(selectedRecipe.type)" size="medium">
              {{ getRecipeTagLabel(selectedRecipe.type) }}
            </van-tag>
            <span><van-icon name="clock-o" size="14" /> {{ selectedRecipe.duration }}</span>
            <span><van-icon name="fire-o" size="14" /> {{ selectedRecipe.calories }}</span>
            <span><van-icon name="chart-trending-o" size="14" /> {{ selectedRecipe.difficulty }}</span>
          </div>

          <!-- 食材清单：冰箱已有 -->
          <div v-if="selectedRecipe.fromFridge && selectedRecipe.fromFridge.length" class="detail-section">
            <h4><van-icon name="bag-o" size="16" /> 冰箱已有食材</h4>
            <div class="ing-list">
              <div
                v-for="(ing, i) in selectedRecipe.fromFridge"
                :key="'df' + i"
                class="ing-row"
                :class="getUrgencyClass(ing.daysLeft)"
              >
                <span class="ing-name">{{ ing.name }}</span>
                <span class="ing-amount">{{ ing.amount }}</span>
                <span class="ing-status">
                  <van-icon :name="getUrgencyIcon(ing.daysLeft)" size="14" :color="getUrgencyColor(ing.daysLeft)" />
                  {{ getUrgencyText(ing.daysLeft) }}
                </span>
              </div>
            </div>
          </div>

          <!-- 食材清单：需购买 -->
          <div v-if="selectedRecipe.needToBuy && selectedRecipe.needToBuy.length" class="detail-section">
            <h4><van-icon name="shopping-cart-o" size="16" /> 需额外购买</h4>
            <div class="ing-list">
              <div v-for="(ing, i) in selectedRecipe.needToBuy" :key="'db' + i" class="ing-row buy">
                <span class="ing-name">{{ ing.name }}</span>
                <span class="ing-amount">{{ ing.amount }}</span>
                <span class="ing-status">{{ ing.estimatedPrice || '' }}</span>
              </div>
            </div>
          </div>

          <!-- 详细步骤 -->
          <div class="detail-section">
            <h4><van-icon name="todo-list-o" size="16" /> 详细制作步骤（零基础版）</h4>
            <div class="steps">
              <div v-for="(step, index) in selectedRecipe.steps" :key="index" class="step">
                <span class="step-number">{{ index + 1 }}</span>
                <span class="step-text">{{ step }}</span>
              </div>
            </div>
          </div>

          <!-- 小贴士 -->
          <div v-if="selectedRecipe.tips && selectedRecipe.tips.length" class="detail-section">
            <h4><van-icon name="bulb-o" size="16" /> 小贴士</h4>
            <ul class="tips-list">
              <li v-for="(tip, i) in selectedRecipe.tips" :key="i">{{ tip }}</li>
            </ul>
          </div>
        </div>

        <!-- 详情底部操作栏：完成食谱 + 收藏 -->
        <div class="detail-action-bar">
          <!-- 完成食谱：赤陶橙主操作按钮，视觉突出 -->
          <van-button
            v-if="hasCompletableIngredients(selectedRecipe)"
            type="primary"
            round
            block
            size="large"
            icon="success"
            :loading="completing"
            :disabled="completing"
            class="complete-btn"
            @click="handleCompleteRecipe(selectedRecipe)"
          >{{ completing ? '处理中...' : '完成食谱' }}</van-button>
          <div v-else class="complete-empty-hint">
            <van-icon name="info-o" size="14" />
            <span>该菜谱暂无可消耗的冰箱食材</span>
          </div>

          <van-button
            v-if="!recipeStore.isFavorite(selectedRecipe)"
            type="primary"
            round
            block
            icon="bookmark-o"
            class="fav-toggle-btn"
            @click="toggleFavorite(selectedRecipe)"
          >添加到我的菜谱</van-button>
          <van-button
            v-else
            plain
            type="primary"
            round
            block
            icon="bookmark"
            class="fav-toggle-btn"
            @click="toggleFavorite(selectedRecipe)"
          >已收藏 · 点击移除</van-button>
        </div>
      </div>
    </van-popup>

    <!-- AI 配置弹层 -->
    <van-popup v-model:show="showConfig" position="bottom" :style="{ height: '80%' }" round>
      <div class="config-panel">
        <div class="detail-header">
          <h3>AI 接口设置</h3>
          <van-icon name="cross" size="20" @click="showConfig = false" />
        </div>
        <div class="config-content">
          <p class="config-tip">
            选择服务商并填入 API Key，即可调用大模型生成定制菜谱。未配置或调用失败时将自动使用本地经典菜谱兜底。
          </p>

          <van-field label="服务商" :model-value="providerLabel" readonly is-link @click="showProviderPicker = true" />

          <van-field
            v-model="apiKey"
            label="API Key"
            :placeholder="keyPlaceholder"
            :type="showKey ? 'text' : 'password'"
            clearable
            :error="!!keyError"
            :error-message="keyError"
            @update:model-value="onKeyInput"
          >
            <template #right-icon>
              <van-icon
                :name="showKey ? 'eye-o' : 'closed-eye'"
                size="20"
                @click="showKey = !showKey"
                style="color: #969799"
              />
            </template>
          </van-field>

          <div v-if="keyHint" class="field-hint">{{ keyHint }}</div>

          <van-field
            v-model="customEndpoint"
            label="自定义端点"
            placeholder="可选，填写代理地址以规避 CORS"
            clearable
          >
            <template #left-icon>
              <van-icon name="link-o" />
            </template>
          </van-field>

          <div class="config-actions">
            <van-button
              type="primary"
              round
              block
              size="large"
              :loading="testing"
              @click="testConnection"
            >
              {{ testing ? '测试中...' : '测试连接' }}
            </van-button>
            <van-button type="success" round block size="large" @click="saveConfig">保存配置</van-button>
            <van-button v-if="savedKey" plain type="danger" block @click="clearConfig">清除已保存的 Key</van-button>
          </div>

          <div class="config-hint">
            <p><van-icon name="bulb-o" size="14" /> 推荐 DeepSeek 或 智谱 GLM，中文菜谱生成质量好且价格低。</p>
            <p><van-icon name="setting-o" size="14" /> 开发环境已自动通过 Vite 代理转发请求，无需担心 CORS。</p>
            <p><van-icon name="shield-o" size="14" /> API Key 仅保存在本地浏览器 LocalStorage，不会上传服务器。</p>
          </div>
        </div>
      </div>
    </van-popup>

    <!-- 服务商选择 -->
    <van-popup v-model:show="showProviderPicker" position="bottom" round>
      <van-picker
        :columns="providerColumns"
        @confirm="onProviderConfirm"
        @cancel="showProviderPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast, showConfirmDialog } from 'vant'
import { useInventoryStore } from '../stores/inventory'
import { useRecipesStore } from '../stores/recipes'
import {
  generateRecipes,
  AI_PROVIDERS,
  API_KEY_VALIDATORS,
  validateApiKey,
  testApiKey
} from '../utils/recipeAi'
import { calculateRemainingDays, getExpiryStatus, formatRelativeTime } from '../utils'

const router = useRouter()
const route = useRoute()
const store = useInventoryStore()
const recipeStore = useRecipesStore()

// ===== 状态 =====
// 支持从主页「我的菜谱」入口通过 query.tab 直接激活对应分类
const validTabs = ['all', 'quick', 'healthy', 'home', 'favorites']
const initialTab = validTabs.includes(route.query.tab) ? route.query.tab : 'all'
const activeTab = ref(initialTab)
const loading = ref(false)
const recipes = ref([])
const showDetail = ref(false)
const selectedRecipe = ref(null)
const lastResult = ref(null)
const elapsedSeconds = ref(0)
const completing = ref(false)  // 完成食谱防重复点击锁
let elapsedTimer = null
let abortController = null

// ===== AI 配置 =====
const showConfig = ref(false)
const showProviderPicker = ref(false)
const provider = ref(localStorage.getItem('recipeAi.provider') || 'deepseek')
const apiKey = ref(localStorage.getItem('recipeAi.apiKey') || '')
const customEndpoint = ref(localStorage.getItem('recipeAi.customEndpoint') || '')
const savedKey = ref(!!localStorage.getItem('recipeAi.apiKey'))

// 新增：密钥显示切换、格式校验、测试连接
const showKey = ref(false)
const keyError = ref('')
const testing = ref(false)

const providerColumns = Object.entries(AI_PROVIDERS).map(([value, cfg]) => ({
  text: cfg.label,
  value
}))

const providerLabel = computed(() => AI_PROVIDERS[provider.value]?.label || '请选择')

// Key 输入框 placeholder 动态显示对应服务商的格式提示
const keyPlaceholder = computed(() => {
  const rule = API_KEY_VALIDATORS[provider.value]
  return rule?.prefix ? `以 "${rule.prefix}" 开头的 Key` : '粘贴您的 API Key'
})

// Key 格式提示（输入框下方灰色说明文字）
const keyHint = computed(() => API_KEY_VALIDATORS[provider.value]?.hint || '')

// Key 输入时实时校验格式
function onKeyInput() {
  if (!apiKey.value) {
    keyError.value = ''
    return
  }
  const result = validateApiKey(provider.value, apiKey.value)
  keyError.value = result.valid ? '' : result.message
}

// ===== 库存计算 =====
const availableFoods = computed(() => {
  return store.foods.filter(food => {
    const remaining = calculateRemainingDays(food.expiryDate)
    return food.quantity > 0 && remaining !== null && remaining >= 0
  })
})

const urgentCount = computed(() => {
  return availableFoods.value.filter(food => {
    const remaining = calculateRemainingDays(food.expiryDate)
    return getExpiryStatus(remaining) === 'red'
  }).length
})

const filteredRecipes = computed(() => {
  // 收藏 tab：返回 store 中的收藏列表
  if (activeTab.value === 'favorites') {
    return recipeStore.getFavoriteRecipes()
  }
  if (activeTab.value === 'all') return recipes.value
  return recipes.value.filter(r => r.type === activeTab.value)
})

// ===== 生命周期 =====
onMounted(() => {
  store.loadData()
  recipeStore.loadData()
  // 优先读本地缓存，避免每次进入都调用 AI
  if (recipeStore.hasCache) {
    recipes.value = recipeStore.cache.recipes
    lastResult.value = {
      useFallback: recipeStore.cache.useFallback,
      message: recipeStore.cache.message
    }
  }
  // 无缓存时不自动调用 AI，进入空状态等待用户点击「生成菜谱」
})

onUnmounted(() => {
  stopElapsedTimer()
  if (abortController) abortController.abort()
})

// ===== 生成菜谱 =====
async function handleGenerate() {
  if (availableFoods.value.length < 3) {
    showToast({ message: '库存不足，请先采购入库', position: 'bottom' })
    return
  }

  loading.value = true
  elapsedSeconds.value = 0
  startElapsedTimer()

  // 取消上一次未完成的请求
  if (abortController) abortController.abort()
  abortController = new AbortController()

  try {
    const result = await generateRecipes({
      foods: store.foods,
      preferences: store.preferences,
      provider: provider.value,
      apiKey: apiKey.value,
      customEndpoint: customEndpoint.value,
      signal: abortController.signal
    })

    lastResult.value = result

    if (!result.success && !result.useFallback && result.recipes.length === 0) {
      // 食材不足等业务错误：无缓存时清空，有缓存时保留旧菜谱
      if (!recipeStore.hasCache) {
        recipes.value = []
      }
      showToast({ message: result.message, position: 'bottom' })
    } else {
      recipes.value = result.recipes
      // 写入本地缓存（含时间戳，供下次进入直接展示）
      recipeStore.saveCache({
        recipes: result.recipes,
        timestamp: Date.now(),
        useFallback: !!result.useFallback,
        message: result.message || ''
      })
      if (result.useFallback) {
        showToast({ message: '已加载本地菜谱', position: 'bottom' })
      } else {
        showToast({ message: '菜谱生成成功', position: 'bottom' })
      }
    }
  } catch (err) {
    // 理论上 generateRecipes 内部已捕获并降级，此处兜底
    lastResult.value = {
      useFallback: true,
      message: `生成失败：${err.message || '未知错误'}`
    }
    // 有缓存时不清空，保留上次结果供继续查看
    if (!recipeStore.hasCache) {
      recipes.value = []
    }
    showToast({ message: `生成失败：${err.message || '未知错误'}`, position: 'bottom', duration: 3000 })
  } finally {
    loading.value = false
    stopElapsedTimer()
  }
}

function cancelGenerate() {
  if (abortController) abortController.abort()
  loading.value = false
  stopElapsedTimer()
  showToast({ message: '已取消', position: 'bottom' })
}

function startElapsedTimer() {
  stopElapsedTimer()
  elapsedTimer = setInterval(() => {
    elapsedSeconds.value += 1
  }, 1000)
}

function stopElapsedTimer() {
  if (elapsedTimer) {
    clearInterval(elapsedTimer)
    elapsedTimer = null
  }
}

// ===== 收藏操作 =====
function toggleFavorite(recipe) {
  if (recipeStore.isFavorite(recipe)) {
    recipeStore.removeFavoriteByRecipe(recipe)
    showToast({ message: '已从我的菜谱移除', position: 'bottom' })
  } else {
    const ok = recipeStore.addFavorite(recipe)
    if (ok) {
      showToast({ message: '已添加到我的菜谱', position: 'bottom' })
    } else {
      showToast({ message: '该菜谱已收藏', position: 'bottom' })
    }
  }
}

// ===== 完成食谱：按菜谱用量扣减库存 =====
// 是否存在可消耗的冰箱食材（needToBuy 不在库存中，不参与扣减）
function hasCompletableIngredients(recipe) {
  return !!(recipe && Array.isArray(recipe.fromFridge) && recipe.fromFridge.length > 0)
}

// 从用量字符串解析数值："2个"→2、"200g"→200、"少许"→1
function parseAmount(amountStr) {
  if (amountStr === null || amountStr === undefined) return 1
  const match = String(amountStr).trim().match(/^(\d+(\.\d+)?)/)
  if (match) {
    const num = parseFloat(match[1])
    return num > 0 ? Math.max(1, Math.floor(num)) : 1
  }
  return 1
}

// 按食材名匹配库存：精确优先 → 包含匹配；多个候选时优先消耗临期的
function matchFoodByName(name) {
  if (!name) return null
  const target = String(name).trim().toLowerCase()
  if (!target) return null

  // 仅在库存 > 0 且未过期的食材中匹配
  const candidates = store.foods.filter(f => {
    if (f.quantity <= 0) return false
    const remaining = calculateRemainingDays(f.expiryDate)
    return remaining !== null && remaining >= 0
  })

  let matched = candidates.filter(f => f.name.trim().toLowerCase() === target)
  if (matched.length === 0 && target.length >= 2) {
    matched = candidates.filter(f => f.name.trim().toLowerCase().includes(target))
    if (matched.length === 0) {
      matched = candidates.filter(f => target.includes(f.name.trim().toLowerCase()))
    }
  }
  if (matched.length === 0) return null

  // 多个匹配：优先消耗临期的（red < yellow < green），同状态取库存少的
  const statusOrder = { expired: 0, red: 1, yellow: 2, green: 3, normal: 4 }
  matched.sort((a, b) => {
    const sa = statusOrder[getExpiryStatus(calculateRemainingDays(a.expiryDate))] ?? 5
    const sb = statusOrder[getExpiryStatus(calculateRemainingDays(b.expiryDate))] ?? 5
    if (sa !== sb) return sa - sb
    return a.quantity - b.quantity
  })
  return matched[0]
}

function handleCompleteRecipe(recipe) {
  if (!recipe || completing.value) return

  const fromFridge = recipe.fromFridge || []
  if (fromFridge.length === 0) {
    showToast({ message: '该菜谱没有可消耗的冰箱食材', position: 'bottom' })
    return
  }

  // 预匹配：构建消耗清单 + 收集未匹配项
  const plan = []
  const unmatched = []
  fromFridge.forEach(ing => {
    const food = matchFoodByName(ing.name)
    if (!food) {
      unmatched.push(ing.name)
      return
    }
    plan.push({ ing, food, qty: parseAmount(ing.amount) })
  })

  // 全部未匹配：直接提示，不弹确认
  if (plan.length === 0) {
    showToast({ message: `库存中未找到：${unmatched.join('、')}`, position: 'bottom', duration: 3000 })
    return
  }

  // 上锁（含对话框期间，防止重复点击重复弹出）
  completing.value = true

  const summary = plan.map(p => `· ${p.ing.name} ${p.ing.amount}`).join('\n')
  const needToBuyNote = (recipe.needToBuy && recipe.needToBuy.length > 0)
    ? `\n\n（需购买的 ${recipe.needToBuy.length} 种食材不会扣减）`
    : ''
  const unmatchedNote = unmatched.length > 0
    ? `\n\n（${unmatched.join('、')} 库存未找到，将跳过）`
    : ''

  showConfirmDialog({
    title: unmatched.length > 0 ? '部分食材未匹配' : '确认完成菜谱',
    message: `完成「${recipe.name}」将自动消耗：\n${summary}${needToBuyNote}${unmatchedNote}`,
    confirmButtonText: '确认完成',
    cancelButtonText: '再想想'
  }).then(() => {
    try {
      const foodIds = plan.map(p => p.food.id)
      const quantities = plan.map(p => p.qty)
      // cookOutbound 内部先全量校验再统一扣减（事务性），失败抛错且不会部分扣减
      store.cookOutbound(foodIds, quantities, `菜谱：${recipe.name}`)
      showToast({ message: `已完成「${recipe.name}」，消耗 ${plan.length} 种食材`, position: 'bottom', duration: 2000 })
      // 关闭详情弹层；库存状态由 store.foods 响应式驱动自动刷新
      showDetail.value = false
    } catch (error) {
      showToast({ message: `完成失败：${error.message}`, position: 'bottom', duration: 3000 })
    }
  }).catch(() => {
    // 用户取消，不做处理
  }).finally(() => {
    completing.value = false
  })
}

// ===== 详情 =====
function showRecipeDetail(recipe) {
  selectedRecipe.value = recipe
  showDetail.value = true
}

// ===== 配置 =====
function openConfig() {
  showConfig.value = true
}

function onProviderConfirm({ value }) {
  provider.value = value
  showProviderPicker.value = false
  // 切换服务商时重新校验 Key 格式
  onKeyInput()
}

// 测试 API Key 是否有效（轻量请求）
async function testConnection() {
  if (!apiKey.value || !apiKey.value.trim()) {
    showToast({ message: '请先输入 API Key', position: 'bottom' })
    return
  }

  // 先做本地格式校验
  const formatCheck = validateApiKey(provider.value, apiKey.value)
  if (!formatCheck.valid) {
    keyError.value = formatCheck.message
    showToast({ message: formatCheck.message, position: 'bottom' })
    return
  }

  testing.value = true
  try {
    const result = await testApiKey(provider.value, apiKey.value, customEndpoint.value)
    showToast({ message: result.message, position: 'bottom', duration: 3000 })
  } catch (err) {
    showToast({ message: `测试失败：${err.message}`, position: 'bottom' })
  } finally {
    testing.value = false
  }
}

function saveConfig() {
  if (!provider.value) {
    showToast({ message: '请选择服务商', position: 'bottom' })
    return
  }

  // 保存前做格式校验（不阻断保存，但给出提示）
  if (apiKey.value) {
    const formatCheck = validateApiKey(provider.value, apiKey.value)
    if (!formatCheck.valid) {
      keyError.value = formatCheck.message
      showToast({ message: `格式警告：${formatCheck.message}`, position: 'bottom', duration: 3000 })
    }
  }

  localStorage.setItem('recipeAi.provider', provider.value)
  localStorage.setItem('recipeAi.apiKey', apiKey.value.trim())
  localStorage.setItem('recipeAi.customEndpoint', customEndpoint.value.trim())
  savedKey.value = !!apiKey.value.trim()
  showToast({ message: '配置已保存', position: 'bottom' })
  showConfig.value = false
}

function clearConfig() {
  showConfirmDialog({
    title: '确认清除',
    message: '将清除已保存的 API Key 与配置，确认继续？'
  })
    .then(() => {
      apiKey.value = ''
      customEndpoint.value = ''
      keyError.value = ''
      localStorage.removeItem('recipeAi.apiKey')
      localStorage.removeItem('recipeAi.customEndpoint')
      savedKey.value = false
      showToast({ message: '已清除', position: 'bottom' })
    })
    .catch(() => {})
}

// ===== 工具函数 =====
function getRecipeTagColor(type) {
  const colors = { quick: '#7C9473', healthy: '#6E8CA8', home: '#C97B5C' }
  return colors[type] || '#7C9473'
}

function getRecipeTagLabel(type) {
  const labels = { quick: '快手懒人餐', healthy: '减脂轻食餐', home: '家常正餐' }
  return labels[type] || '推荐菜谱'
}

function getUrgencyClass(daysLeft) {
  if (daysLeft === undefined || daysLeft === null) return ''
  if (daysLeft <= 2) return 'urgent'
  if (daysLeft <= 7) return 'warning'
  return 'fresh'
}

// 临期等级图标（拆分自原 getUrgencyText，emoji → van-icon）
function getUrgencyIcon(daysLeft) {
  if (daysLeft === undefined || daysLeft === null) return 'info-o'
  if (daysLeft < 0) return 'warning-o'
  if (daysLeft <= 2) return 'fire-o'
  if (daysLeft <= 7) return 'clock-o'
  return 'success'
}

// 临期等级颜色
function getUrgencyColor(daysLeft) {
  if (daysLeft === undefined || daysLeft === null) return 'var(--sk-text-3)'
  if (daysLeft < 0) return 'var(--sk-expired)'
  if (daysLeft <= 2) return 'var(--sk-danger)'
  if (daysLeft <= 7) return 'var(--sk-warn)'
  return 'var(--sk-primary)'
}

// 临期等级纯文本（不再含 emoji，配合 getUrgencyIcon 使用）
function getUrgencyText(daysLeft) {
  if (daysLeft === undefined || daysLeft === null) return ''
  if (daysLeft < 0) return `已过期 ${Math.abs(daysLeft)} 天`
  if (daysLeft === 0) return '今天过期'
  return `剩 ${daysLeft} 天`
}

function goAddFood() {
  router.push('/add-food')
}

function goPreferences() {
  router.push('/preferences')
}
</script>

<style scoped>
.recipes-page {
  padding-bottom: 60px;
  background: var(--sk-bg);
}

.recipe-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--sk-card);
  margin-bottom: 12px;
  border-bottom: 1px solid var(--sk-border);
}

.available-foods {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.recipe-tabs {
  margin-bottom: 12px;
  background: var(--sk-card);
}

/* 缓存时间条 */
.cache-info {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 0 16px 10px;
  padding: 8px 12px;
  background: var(--sk-primary-bg);
  border-radius: 10px;
  font-size: 12px;
  color: var(--sk-primary-dark);
}

.loading-state {
  padding: 48px 16px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.loading-tip {
  font-size: 13px;
  color: var(--sk-text-3);
}

.empty-state {
  padding: 40px 16px;
  text-align: center;
}

.empty-state .van-button {
  margin-top: 16px;
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}

/* 分类空状态操作按钮组 */
.category-empty-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
}

.fallback-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 16px 12px;
  padding: 10px 12px;
  background: var(--sk-warn-bg);
  border: 1px solid rgba(212, 165, 91, 0.3);
  border-radius: 10px;
  font-size: 12px;
  color: #9a6b2e;
}

.recipe-card {
  padding: 16px 16px 14px;
  margin: 0 12px 10px;
  background: var(--sk-card);
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
  position: relative;
  overflow: hidden;
}

.recipe-accent {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
}

.recipe-info {
  width: 100%;
}

.recipe-card-header {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-bottom: 6px;
}

.recipe-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--sk-text);
  text-align: center;
  letter-spacing: 0.5px;
  line-height: 1.4;
}

/* 卡片副标题：分类标签 + 序号，居中排列 */
.recipe-card-sub {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.recipe-index {
  font-size: 12px;
  color: var(--sk-text-3);
}

.recipe-meta {
  display: flex;
  gap: 12px;
  margin-bottom: 10px;
  flex-wrap: wrap;
}

.recipe-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--sk-text-3);
}

.ing-group {
  font-size: 13px;
  margin-bottom: 6px;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
}

.ing-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: var(--sk-text-2);
}

.ing-label.buy {
  color: var(--sk-accent);
}

.ing-tag {
  display: inline-block;
  padding: 3px 9px;
  border-radius: 6px;
  background: var(--sk-primary-bg);
  color: var(--sk-text);
  font-size: 12px;
}

.ing-tag em {
  font-style: normal;
  color: var(--sk-text-3);
  margin-left: 2px;
}

.ing-tag.urgent {
  background: var(--sk-danger-bg);
  color: var(--sk-danger);
  font-weight: 600;
}

.ing-tag.urgent em {
  color: var(--sk-danger);
}

.ing-tag.warning {
  background: var(--sk-warn-bg);
  color: var(--sk-warn);
}

.ing-tag.warning em {
  color: var(--sk-warn);
}

.ing-tag.buy {
  background: var(--sk-accent-bg);
  color: var(--sk-accent);
}

.ing-tag.buy em {
  color: var(--sk-accent);
}

/* 卡片底部：步骤预览 + 收藏按钮 */
.recipe-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 8px;
  gap: 8px;
}

.recipe-steps-preview {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--sk-primary);
  flex: 1;
}

/* 收藏按钮 */
.fav-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 16px;
  background: var(--sk-section);
  color: var(--sk-text-3);
  font-size: 12px;
  font-weight: 500;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.fav-btn:active {
  transform: scale(0.94);
}

.fav-btn.active {
  background: var(--sk-accent-bg);
  color: var(--sk-accent);
}

/* 卡片底部操作区：完成按钮 + 收藏按钮 */
.footer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

/* 我的菜谱卡片：完成食谱快捷按钮（赤陶橙，醒目） */
.complete-mini-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 16px;
  background: var(--sk-accent);
  color: #fffefb;
  font-size: 12px;
  font-weight: 600;
  transition: all 0.2s ease;
  flex-shrink: 0;
  box-shadow: 0 2px 6px rgba(201, 123, 92, 0.3);
}

.complete-mini-btn:active {
  transform: scale(0.94);
}

.complete-mini-btn.disabled {
  opacity: 0.5;
  pointer-events: none;
}

.generate-bar {
  padding: 16px;
  padding-bottom: 32px;
}

.generate-bar .van-button {
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}

/* 详情弹层 */
.recipe-detail,
.config-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid var(--sk-border);
  flex-shrink: 0;
}

.detail-header h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--sk-text);
}

.detail-content,
.config-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: var(--sk-bg);
}

.config-tip {
  font-size: 13px;
  color: var(--sk-text-2);
  line-height: 1.6;
  margin-bottom: 16px;
  padding: 12px;
  background: var(--sk-section);
  border-radius: 10px;
}

.field-hint {
  font-size: 12px;
  color: var(--sk-text-3);
  padding: 4px 16px 8px;
  margin-top: -8px;
}

.config-actions {
  margin-top: 24px;
  padding-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.config-hint {
  margin-top: 16px;
  font-size: 12px;
  color: var(--sk-text-3);
  line-height: 1.8;
}

.config-hint p {
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
  align-items: center;
}

.detail-meta span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: var(--sk-text-2);
}

.detail-section {
  margin-bottom: 20px;
  background: var(--sk-card);
  border-radius: 12px;
  padding: 14px;
}

.detail-section h4 {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 10px;
  color: var(--sk-text);
  position: relative;
  padding-left: 10px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.detail-section h4::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 13px;
  border-radius: 2px;
  background: var(--sk-primary);
}

.ing-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ing-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  background: var(--sk-section);
  border-radius: 8px;
  font-size: 14px;
}

.ing-row.urgent {
  background: var(--sk-danger-bg);
}

.ing-row.warning {
  background: var(--sk-warn-bg);
}

.ing-row.buy {
  background: var(--sk-accent-bg);
}

.ing-name {
  font-weight: 500;
  color: var(--sk-text);
}

.ing-amount {
  color: var(--sk-text-2);
  flex: 1;
  margin: 0 12px;
}

.ing-status {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  color: var(--sk-text-3);
}

.steps {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.step {
  display: flex;
  gap: 10px;
}

.step-number {
  width: 24px;
  height: 24px;
  background: var(--sk-primary);
  color: #fffefb;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-text {
  font-size: 14px;
  color: var(--sk-text-2);
  line-height: 1.6;
  flex: 1;
}

.tips-list {
  padding-left: 20px;
  margin: 0;
}

.tips-list li {
  font-size: 13px;
  color: var(--sk-text-2);
  line-height: 1.8;
  margin-bottom: 4px;
}

/* 详情底部收藏操作栏 */
.detail-action-bar {
  flex-shrink: 0;
  padding: 12px 16px calc(12px + env(safe-area-inset-bottom));
  border-top: 1px solid var(--sk-border);
  background: var(--sk-card);
}

/* 完成食谱主按钮：赤陶橙，视觉突出（覆写 primary 配色） */
.complete-btn {
  --van-button-primary-background: var(--sk-accent);
  --van-button-primary-border-color: var(--sk-accent);
  box-shadow: 0 6px 16px rgba(201, 123, 92, 0.35);
  font-weight: 600;
  letter-spacing: 1px;
  margin-bottom: 10px;
}

.fav-toggle-btn {
  flex-shrink: 0;
}

/* 无可消耗食材时的占位提示 */
.complete-empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px;
  margin-bottom: 10px;
  background: var(--sk-section);
  border-radius: 999px;
  font-size: 13px;
  color: var(--sk-text-3);
}
</style>
