import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getRecipeSignature } from '../utils/recipeAi'

/**
 * 菜谱 Store
 *
 * 职责：
 *  1. 管理用户收藏的菜谱（「我的菜谱」），持久化到 localStorage
 *  2. 管理 AI 菜谱生成的本地缓存（避免每次进入页面重复调用 AI）
 *
 * 持久化 key：
 *  - `recipeFavorites`：收藏菜谱数组（每项 = normalize 后的 recipe + savedAt）
 *  - `recipeAi.cache`：AI 缓存对象 { recipes, timestamp, useFallback, message }
 *
 * 设计说明：与 inventory store（食材域）分离，避免 saveData() 连带写入耦合；
 * 复用 inventory store 的 ref + computed + loadData/saveData 模式。
 */
export const useRecipesStore = defineStore('recipes', () => {
  // ===== State =====
  const favorites = ref([])
  const cache = ref(null)

  // ===== localStorage Keys =====
  const FAVORITES_KEY = 'recipeFavorites'
  const CACHE_KEY = 'recipeAi.cache'

  // ===== Computed =====
  // 收藏签名集合（O(1) 查重，供卡片批量渲染时快速判定是否已收藏）
  const favoriteSignatures = computed(() => {
    return new Set(favorites.value.map(r => getRecipeSignature(r)))
  })

  const favoriteCount = computed(() => favorites.value.length)

  const hasCache = computed(() => {
    return !!cache.value && Array.isArray(cache.value.recipes) && cache.value.recipes.length > 0
  })

  const cacheTimestamp = computed(() => cache.value?.timestamp || null)

  // ===== 持久化 =====
  function loadData() {
    try {
      const favData = localStorage.getItem(FAVORITES_KEY)
      if (favData) {
        const parsed = JSON.parse(favData)
        favorites.value = Array.isArray(parsed) ? parsed : []
      }

      const cacheData = localStorage.getItem(CACHE_KEY)
      if (cacheData) {
        cache.value = JSON.parse(cacheData)
      }
    } catch (error) {
      console.error('菜谱数据加载失败:', error)
      favorites.value = []
      cache.value = null
    }
  }

  function saveFavorites() {
    try {
      localStorage.setItem(FAVORITES_KEY, JSON.stringify(favorites.value))
    } catch (error) {
      console.error('收藏数据保存失败:', error)
    }
  }

  function saveCache(payload) {
    cache.value = payload
    try {
      localStorage.setItem(CACHE_KEY, JSON.stringify(payload))
    } catch (error) {
      console.error('菜谱缓存保存失败:', error)
    }
  }

  function clearCache() {
    cache.value = null
    localStorage.removeItem(CACHE_KEY)
  }

  // ===== 收藏操作 =====
  // 判断某菜谱是否已收藏（基于签名）
  function isFavorite(recipe) {
    if (!recipe) return false
    return favoriteSignatures.value.has(getRecipeSignature(recipe))
  }

  // 添加收藏：已存在返回 false，否则插入并持久化
  function addFavorite(recipe) {
    if (!recipe) return false
    const signature = getRecipeSignature(recipe)
    if (favoriteSignatures.value.has(signature)) {
      return false
    }
    // 深拷贝 + 追加收藏时间，避免引用污染原列表
    favorites.value.unshift({
      ...recipe,
      signature,
      savedAt: Date.now()
    })
    saveFavorites()
    return true
  }

  // 按菜谱对象移除收藏
  function removeFavoriteByRecipe(recipe) {
    if (!recipe) return false
    return removeFavoriteBySignature(getRecipeSignature(recipe))
  }

  // 按签名移除收藏
  function removeFavoriteBySignature(signature) {
    if (!signature) return false
    const len = favorites.value.length
    favorites.value = favorites.value.filter(r => getRecipeSignature(r) !== signature)
    if (favorites.value.length !== len) {
      saveFavorites()
      return true
    }
    return false
  }

  // 获取收藏列表（供「我的菜谱」tab 渲染）
  function getFavoriteRecipes() {
    return favorites.value
  }

  return {
    favorites,
    cache,
    favoriteSignatures,
    favoriteCount,
    hasCache,
    cacheTimestamp,
    loadData,
    saveFavorites,
    saveCache,
    clearCache,
    isFavorite,
    addFavorite,
    removeFavoriteByRecipe,
    removeFavoriteBySignature,
    getFavoriteRecipes
  }
})
