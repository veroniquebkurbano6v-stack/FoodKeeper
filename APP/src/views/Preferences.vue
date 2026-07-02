<template>
  <div class="preferences-page">
    <van-nav-bar title="口味偏好" left-arrow @click-left="goBack" />
    
    <van-form @submit="onSubmit" class="preferences-form">
      <van-field 
        v-model="form.dietaryRestrictions" 
        label="忌口食物" 
        placeholder="请输入忌口食物，用逗号分隔"
      />
      
      <van-field 
        v-model="form.preferredCuisine" 
        label="偏好菜系" 
        placeholder="请输入偏好菜系，用逗号分隔"
      />
      
      <van-field 
        v-model="form.spiciness" 
        label="辣度偏好" 
        placeholder="请选择辣度" 
        readonly
        @click="showSpicinessPicker = true"
      >
        <template #right-icon>
          <van-icon name="arrow" size="16" />
        </template>
      </van-field>
      
      <div class="form-actions">
        <van-button type="primary" native-type="submit" round block size="large">
          保存偏好
        </van-button>
      </div>
    </van-form>

    <van-popup v-model:show="showSpicinessPicker" position="bottom">
      <van-picker 
        :columns="spicinessOptions" 
        @confirm="onSpicinessConfirm"
        @cancel="showSpicinessPicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useInventoryStore } from '../stores/inventory'

const router = useRouter()
const store = useInventoryStore()

const form = ref({
  dietaryRestrictions: '',
  preferredCuisine: '',
  spiciness: ''
})

const showSpicinessPicker = ref(false)

const spicinessOptions = [
  { value: '', label: '不限' },
  { value: 'non-spicy', label: '不辣' },
  { value: 'mild', label: '微辣' },
  { value: 'medium', label: '中辣' },
  { value: 'hot', label: '特辣' }
]

onMounted(() => {
  store.loadData()
  loadPreferences()
})

function loadPreferences() {
  const prefs = store.preferences
  form.value = {
    dietaryRestrictions: prefs.dietaryRestrictions.join(',') || '',
    preferredCuisine: prefs.preferredCuisine.join(',') || '',
    spiciness: prefs.spiciness || ''
  }
}

function onSpicinessConfirm({ value }) {
  form.value.spiciness = value
  showSpicinessPicker.value = false
}

function onSubmit() {
  const newPreferences = {
    dietaryRestrictions: form.value.dietaryRestrictions 
      ? form.value.dietaryRestrictions.split(',').map(s => s.trim()).filter(Boolean) 
      : [],
    preferredCuisine: form.value.preferredCuisine 
      ? form.value.preferredCuisine.split(',').map(s => s.trim()).filter(Boolean) 
      : [],
    spiciness: form.value.spiciness
  }
  
  store.setPreferences(newPreferences)
  
  showToast({ message: '保存成功', type: 'success' })
  setTimeout(() => {
    router.back()
  }, 1500)
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
.preferences-page {
  padding: 16px;
  padding-bottom: 60px;
  background: var(--sk-bg);
}

.preferences-form {
  background: var(--sk-card);
  padding: 16px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
}

.form-actions {
  margin-top: 24px;
}

.form-actions .van-button {
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}
</style>