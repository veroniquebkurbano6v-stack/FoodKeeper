<template>
  <div class="settings-page">
    <van-nav-bar title="系统设置" />
    
    <van-cell-group class="settings-group">
      <van-cell title="数据管理" value="备份/恢复" icon="records-o" is-link @click="showDataModal = true" />
      <van-cell title="导出数据" value="JSON/CSV" icon="description-o" is-link @click="showExportModal = true" />
      <van-cell title="重置数据" value="恢复初始状态" icon="replay" is-link @click="confirmReset" />
    </van-cell-group>

    <van-cell-group class="settings-group">
      <van-cell title="关于应用" value="食库管家 v1.0.0" icon="info-o" is-link @click="showAbout = true" />
      <van-cell title="隐私设置" value="本地存储" icon="shield-o" is-link />
    </van-cell-group>

    <van-dialog v-model:show="showDataModal" title="数据管理" show-cancel-button>
      <div class="data-actions">
        <van-button type="primary" block size="large" @click="backupData">备份数据</van-button>
        <van-button block size="large" @click="restoreData">恢复数据</van-button>
      </div>
    </van-dialog>

    <van-dialog v-model:show="showExportModal" title="导出数据" show-cancel-button>
      <div class="export-actions">
        <van-button type="primary" block size="large" @click="exportJson">导出 JSON</van-button>
        <van-button block size="large" @click="exportCsv">导出 CSV</van-button>
      </div>
    </van-dialog>

    <van-dialog v-model:show="showAbout" title="关于食库管家">
      <div class="about-content">
        <p>版本：v1.0.0</p>
        <p>食库管家 - 让食材管理更简单</p>
        <p>家庭冰箱食材进销存管理 + 智能配菜决策 AI Agent</p>
        <p>所有数据均存储在本地浏览器中，不会上传到服务器</p>
      </div>
    </van-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useInventoryStore } from '../stores/inventory'

const store = useInventoryStore()

const showDataModal = ref(false)
const showExportModal = ref(false)
const showAbout = ref(false)

onMounted(() => {
  store.loadData()
})

function backupData() {
  store.saveData()
  showToast({ message: '备份成功', type: 'success' })
  showDataModal.value = false
}

function restoreData() {
  store.loadData()
  showToast({ message: '恢复成功', type: 'success' })
  showDataModal.value = false
}

function confirmReset() {
  if (confirm('确定要重置所有数据吗？这将恢复到初始状态。')) {
    store.resetAllData()
    showToast({ message: '重置成功', type: 'success' })
  }
}

function exportJson() {
  const data = store.exportData('json')
  downloadFile(data, 'food-inventory.json', 'application/json')
  showExportModal.value = false
}

function exportCsv() {
  const data = store.exportData('csv')
  downloadFile(data, 'food-inventory.csv', 'text/csv')
  showExportModal.value = false
}

function downloadFile(content, filename, type) {
  const blob = new Blob([content], { type })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  showToast({ message: '导出成功', type: 'success' })
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
.settings-page {
  padding: 12px 12px 60px;
  background: var(--sk-bg);
}

.settings-group {
  margin-bottom: 12px;
  background: var(--sk-card);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
}

.data-actions, .export-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
}

.about-content {
  text-align: center;
  padding: 16px;
}

.about-content p {
  margin-bottom: 8px;
  font-size: 14px;
  color: var(--sk-text-2);
  line-height: 1.6;
}
</style>