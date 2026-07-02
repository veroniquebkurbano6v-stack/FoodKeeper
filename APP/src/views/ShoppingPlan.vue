<template>
  <div class="shopping-plan-page">
    <van-nav-bar title="采购计划" left-arrow @click-left="goBack" />
    
    <div v-if="missingIngredients.length > 0" class="plan-section">
      <div class="section-header">
        <h2><van-icon name="warning-o" size="16" /> 缺料清单</h2>
        <van-button text type="primary" size="small" @click="generateShoppingList">生成清单</van-button>
      </div>
      
      <van-cell-group v-for="item in missingIngredients" :key="item.name">
        <van-cell :border="false" class="shopping-item">
          <div class="item-info">
            <span class="item-name">{{ item.name }}</span>
            <span class="item-usage">用于：{{ item.usage }}</span>
          </div>
          <van-checkbox v-model="selectedItems[item.name]" @change="onSelectItem" />
        </van-cell>
      </van-cell-group>
    </div>

    <div v-else class="empty-state">
      <van-empty description="暂无缺料，库存充足" />
    </div>

    <div v-if="shoppingList.length > 0" class="plan-section">
      <div class="section-header">
        <h2><van-icon name="shopping-cart-o" size="16" /> 采购清单</h2>
        <van-button text type="danger" size="small" @click="clearShoppingList">清空</van-button>
      </div>
      
      <van-cell-group v-for="item in shoppingList" :key="item.name">
        <van-cell :border="false" class="shopping-item">
          <div class="item-info">
            <span class="item-name">{{ item.name }}</span>
            <span class="item-usage">{{ item.quantity }}</span>
          </div>
          <van-button text type="success" size="small" @click="markAsPurchased(item)">已买</van-button>
        </van-cell>
      </van-cell-group>
    </div>

    <div v-if="shoppingList.length > 0" class="bottom-bar">
      <span>共 {{ shoppingList.length }} 项</span>
      <van-button type="primary" round block size="large" @click="exportShoppingList">导出清单</van-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'

const router = useRouter()
const store = useInventoryStore()

const selectedItems = ref({})
const shoppingList = ref([])

const mockMissingIngredients = [
  { name: '姜蒜', usage: '青椒肉丝' },
  { name: '生抽', usage: '青椒肉丝' },
  { name: '葱花', usage: '虾仁蒸水蛋' },
  { name: '醋', usage: '土豆丝炒肉' },
  { name: '豆腐', usage: '青菜豆腐汤' },
  { name: '香油', usage: '青菜豆腐汤' }
]

onMounted(() => {
  store.loadData()
})

const missingIngredients = computed(() => {
  return mockMissingIngredients
})

function onSelectItem() {
}

function generateShoppingList() {
  const selected = Object.keys(selectedItems.value).filter(key => selectedItems.value[key])
  if (selected.length === 0) {
    showToast({ message: '请先选择需要采购的物品', type: 'error' })
    return
  }
  
  shoppingList.value = selected.map(name => {
    const item = missingIngredients.value.find(i => i.name === name)
    return {
      name,
      quantity: '适量',
      usage: item?.usage || ''
    }
  })
  
  showToast({ message: '采购清单已生成', type: 'success' })
}

function markAsPurchased(item) {
  shoppingList.value = shoppingList.value.filter(i => i.name !== item.name)
  showToast({ message: `${item.name} 已标记为已买`, type: 'success' })
}

function clearShoppingList() {
  shoppingList.value = []
}

function exportShoppingList() {
  const text = shoppingList.value.map(item => `- ${item.name} (${item.quantity})`).join('\n')
  navigator.clipboard.writeText(text).then(() => {
    showToast({ message: '已复制到剪贴板', type: 'success' })
  })
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
.shopping-plan-page {
  padding-bottom: 100px;
  background: var(--sk-bg);
}

.plan-section {
  padding: 16px;
  background: var(--sk-card);
  margin: 0 12px 12px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
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
  background: var(--sk-accent);
}

.shopping-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px dashed var(--sk-border);
}

.shopping-item:last-child {
  border-bottom: none;
}

.item-info {
  flex: 1;
}

.item-name {
  display: block;
  font-size: 16px;
  font-weight: 600;
  color: var(--sk-text);
  margin-bottom: 4px;
}

.item-usage {
  font-size: 12px;
  color: var(--sk-text-3);
}

.empty-state {
  padding: 40px 16px;
  text-align: center;
}

.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background: var(--sk-card);
  border-top: 1px solid var(--sk-border);
}

.bottom-bar span {
  display: block;
  text-align: center;
  font-size: 14px;
  color: var(--sk-text-2);
  margin-bottom: 8px;
}

.bottom-bar .van-button {
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}
</style>