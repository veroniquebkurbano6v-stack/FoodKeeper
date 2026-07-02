<template>
  <div class="logs-page">
    <van-nav-bar title="出库日志" left-arrow @click-left="goBack" />
    
    <div class="filter-tabs">
      <van-tabs v-model="activeTab">
        <van-tab title="全部" name="all" />
        <van-tab title="烹饪消耗" name="cook" />
        <van-tab title="变质报废" name="discard" />
        <van-tab title="存量清零" name="clear" />
      </van-tabs>
    </div>

    <div v-if="filteredLogs.length === 0" class="empty-state">
      <van-empty description="暂无出库记录" />
    </div>

    <van-cell-group v-for="log in filteredLogs" :key="log.id">
      <van-cell :border="false" class="log-card">
        <span class="type-bar" :style="{ background: getLogColor(log.outboundType) }"></span>
        <div class="log-header">
          <span class="log-icon-wrap" :style="{ background: getLogColor(log.outboundType) + '22', color: getLogColor(log.outboundType) }">
            <van-icon :name="getLogIcon(log.outboundType)" size="18" />
          </span>
          <span class="log-type">{{ getOutboundTypeLabel(log.outboundType) }}</span>
          <span class="log-date">{{ log.createdAt }}</span>
        </div>
        <div class="log-content">
          <span class="log-food-name">{{ log.foodName }}</span>
          <span class="log-quantity" :style="{ color: getLogColor(log.outboundType) }">-{{ log.quantity }}</span>
        </div>
        <div v-if="log.reason" class="log-reason">
          <span>原因：{{ getReasonLabel(log.reason) }}</span>
        </div>
        <div v-if="log.remainingQuantity !== undefined" class="log-remaining">
          <span>剩余库存：{{ log.remainingQuantity }}</span>
        </div>
      </van-cell>
    </van-cell-group>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'
import { getOutboundTypeLabel, getReasonLabel } from '../utils'

const router = useRouter()
const store = useInventoryStore()

const activeTab = ref('all')

onMounted(() => {
  store.loadData()
})

const filteredLogs = computed(() => {
  if (activeTab.value === 'all') {
    return store.logs
  }
  return store.logs.filter(log => log.outboundType === activeTab.value)
})

function getLogIcon(type) {
  const icons = {
    cook: 'fire-o',
    discard: 'delete-o',
    clear: 'clear'
  }
  return icons[type] || 'info-o'
}

function getLogColor(type) {
  const colors = {
    cook: '#7C9473',
    discard: '#C75D4D',
    clear: '#D4A55B'
  }
  return colors[type] || '#8B7E6F'
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.logs-page {
  padding-bottom: 60px;
  background: var(--sk-bg);
}

.filter-tabs {
  margin-bottom: 12px;
  background: var(--sk-card);
}

.log-card {
  padding: 14px 16px;
  background: var(--sk-card);
  margin: 0 12px 10px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
  position: relative;
  overflow: hidden;
}

.type-bar {
  position: absolute;
  left: 0;
  top: 12px;
  bottom: 12px;
  width: 4px;
  border-radius: 0 2px 2px 0;
}

.log-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.log-icon-wrap {
  width: 30px;
  height: 30px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.log-type {
  font-size: 14px;
  font-weight: 600;
  color: var(--sk-text);
}

.log-date {
  margin-left: auto;
  font-size: 12px;
  color: var(--sk-text-3);
}

.log-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 4px;
}

.log-food-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--sk-text);
}

.log-quantity {
  font-size: 16px;
  font-weight: 700;
}

.log-reason, .log-remaining {
  font-size: 13px;
  color: var(--sk-text-3);
}

.empty-state {
  padding: 40px 16px;
  text-align: center;
}
</style>