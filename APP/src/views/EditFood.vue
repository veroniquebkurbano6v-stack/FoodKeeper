<template>
  <div class="edit-food-page">
    <van-nav-bar title="编辑食材" left-arrow @click-left="goBack" />
    
    <van-form @submit="onSubmit" class="food-form">
      <van-field 
        v-model="form.name" 
        label="食材名称" 
        placeholder="请输入食材名称" 
        required
        :error="!!errors.name"
        :error-message="errors.name"
      />
      
      <van-field
        :model-value="categoryDisplay"
        label="分类"
        placeholder="请选择分类"
        readonly
        required
        :error="!!errors.category"
        :error-message="errors.category"
        @click="showCategoryPicker = true"
      >
        <template #right-icon>
          <van-icon name="arrow" size="16" />
        </template>
      </van-field>
      
      <van-field 
        v-model="form.quantity" 
        label="数量" 
        placeholder="请输入数量" 
        type="number"
        required
        :error="!!errors.quantity"
        :error-message="errors.quantity"
      />
      
      <van-field 
        v-model="form.unit" 
        label="单位" 
        placeholder="请输入单位" 
        required
        :error="!!errors.unit"
        :error-message="errors.unit"
      />
      
      <van-field
        v-model="form.expiryDate"
        label="保质期截止"
        placeholder="请选择日期"
        readonly
        required
        :error="!!errors.expiryDate"
        :error-message="errors.expiryDate"
        @click="showExpiryDatePicker = true"
      >
        <template #left-icon>
          <van-icon name="calendar-o" />
        </template>
        <template #right-icon>
          <van-icon name="question-o" size="16" @click.stop="showDateTip = true" />
        </template>
      </van-field>
      
      <van-field
        :model-value="zoneDisplay"
        label="存放分区"
        placeholder="请选择存放分区"
        readonly
        required
        :error="!!errors.storageZone"
        :error-message="errors.storageZone"
        @click="showZonePicker = true"
      >
        <template #right-icon>
          <van-icon name="arrow" size="16" />
        </template>
      </van-field>
      
      <van-field 
        v-model="form.price" 
        label="单价（元）" 
        placeholder="选填" 
        type="digit"
      />
      
      <van-field 
        v-model="form.totalPrice" 
        label="总价（元）" 
        placeholder="选填" 
        type="digit"
      />
      
      <div class="form-actions">
        <van-button type="primary" native-type="submit" round block size="large">
          保存修改
        </van-button>
        <van-button type="danger" round block size="large" @click="confirmDelete">
          删除食材
        </van-button>
      </div>
    </van-form>

    <van-popup v-model:show="showCategoryPicker" position="bottom">
      <van-picker
        :columns="store.categories"
        :columns-field-names="{ text: 'label' }"
        @confirm="onCategoryConfirm"
        @cancel="showCategoryPicker = false"
      />
    </van-popup>

    <!-- 仿真日历选择器（替代滚轮）：月份/年份切换、今天高亮、已选标记、键盘导航、过渡动画 -->
    <van-calendar
      v-model:show="showExpiryDatePicker"
      title="选择保质期截止日期"
      :min-date="minExpiryDate"
      :max-date="maxExpiryDate"
      :default-date="defaultExpiryDate"
      :show-confirm="false"
      :first-day-of-week="1"
      closeable
      round
      @confirm="onExpiryDateConfirm"
      @open="onCalendarOpen"
    />

    <!-- 保质期截止日期操作指引 -->
    <van-dialog
      v-model:show="showDateTip"
      title="保质期截止说明"
      :show-cancel-button="false"
      confirm-button-text="知道了"
    >
      <div style="padding: 16px; font-size: 14px; color: var(--sk-text-2); line-height: 1.6;">
        1. 点击日期字段打开仿真日历修改<br>
        2. 支持月份/年份快速切换<br>
        3. 打开后可用键盘方向键 ← → ↑ ↓ 切换日期，Enter 确认
      </div>
    </van-dialog>

    <van-popup v-model:show="showZonePicker" position="bottom">
      <van-picker
        :columns="store.storageZones"
        :columns-field-names="{ text: 'label' }"
        @confirm="onZoneConfirm"
        @cancel="showZonePicker = false"
      />
    </van-popup>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import { useInventoryStore } from '../stores/inventory'
import { formatDate, getCategoryLabel, getStorageZoneLabel } from '../utils'

const router = useRouter()
const route = useRoute()
const store = useInventoryStore()

const foodId = route.params.id

const form = ref({
  name: '',
  category: '',
  quantity: '',
  unit: '',
  expiryDate: '',
  storageZone: '',
  price: '',
  totalPrice: ''
})

const errors = ref({
  name: '',
  category: '',
  quantity: '',
  unit: '',
  expiryDate: '',
  storageZone: ''
})

const showCategoryPicker = ref(false)
const showExpiryDatePicker = ref(false)
const showZonePicker = ref(false)
const showDateTip = ref(false)

// ===== 仿真日历相关状态 =====
// 系统当前日期（仅初始化取一次）
const today = new Date()
today.setHours(0, 0, 0, 0)

// 保质期截止日期范围：最早为今天（已过期食材编辑时仍允许选今天及以后），最远 10 年
const minExpiryDate = new Date(today)
const maxExpiryDate = new Date(today.getFullYear() + 10, 11, 31)

// 日历默认选中日期（Date 对象），加载食材数据后回填
const defaultExpiryDate = ref(new Date(today))

// 键盘导航用：当前游标日期
const cursorDate = ref(new Date(today))

// 表单字段显示用：将 value 转为中文标签
const categoryDisplay = computed(() =>
  form.value.category ? getCategoryLabel(form.value.category, store.categories) : ''
)
const zoneDisplay = computed(() =>
  form.value.storageZone ? getStorageZoneLabel(form.value.storageZone, store.storageZones) : ''
)

onMounted(() => {
  store.loadData()
  loadFoodData()
})

function loadFoodData() {
  const food = store.foods.find(f => f.id === foodId)
  if (food) {
    form.value = {
      name: food.name,
      category: food.category,
      quantity: food.quantity.toString(),
      unit: food.unit,
      expiryDate: food.expiryDate,
      storageZone: food.storageZone,
      price: food.price ? food.price.toString() : '',
      totalPrice: food.totalPrice ? food.totalPrice.toString() : ''
    }
    // 将日期字符串转为 Date 对象，供 van-calendar :default-date 使用
    const d = new Date(food.expiryDate)
    if (!isNaN(d.getTime())) {
      defaultExpiryDate.value = new Date(d)
      cursorDate.value = new Date(d)
    }
  }
}

function onCategoryConfirm({ selectedValues }) {
  form.value.category = selectedValues[0]
  showCategoryPicker.value = false
}

function onExpiryDateConfirm(date) {
  // van-calendar @confirm 回调参数为 Date 对象
  if (!(date instanceof Date) || isNaN(date.getTime())) return
  form.value.expiryDate = formatDate(date)
  defaultExpiryDate.value = new Date(date)
  cursorDate.value = new Date(date)
  showExpiryDatePicker.value = false
}

// 日历打开时绑定键盘导航（方向键 ← → ↑ ↓ 切换日期，Enter 确认，Esc 关闭）
function onCalendarOpen() {
  cursorDate.value = defaultExpiryDate.value
    ? new Date(defaultExpiryDate.value)
    : new Date(today)
  setTimeout(() => {
    window.addEventListener('keydown', onCalendarKeydown)
  }, 50)
}

function onCalendarKeydown(e) {
  if (!showExpiryDatePicker.value) return
  const step = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }[e.key]
  if (step !== undefined) {
    e.preventDefault()
    const next = new Date(cursorDate.value)
    next.setDate(next.getDate() + step)
    // 边界约束：不超出允许范围
    if (next < minExpiryDate) next.setTime(minExpiryDate.getTime())
    if (next > maxExpiryDate) next.setTime(maxExpiryDate.getTime())
    cursorDate.value = next
    defaultExpiryDate.value = new Date(next)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    onExpiryDateConfirm(new Date(cursorDate.value))
  } else if (e.key === 'Escape') {
    showExpiryDatePicker.value = false
  }
}

// 日历关闭时解绑键盘事件
watch(showExpiryDatePicker, (val) => {
  if (!val) {
    window.removeEventListener('keydown', onCalendarKeydown)
  }
})

function onZoneConfirm({ selectedValues }) {
  form.value.storageZone = selectedValues[0]
  showZonePicker.value = false
}

function validateAll() {
  errors.value = {
    name: !form.value.name.trim() ? '请输入食材名称' : '',
    category: !form.value.category ? '请选择分类' : '',
    quantity: (!form.value.quantity || parseInt(form.value.quantity) <= 0) ? '请输入有效数量' : '',
    unit: !form.value.unit.trim() ? '请输入单位' : '',
    expiryDate: !form.value.expiryDate ? '请选择保质期截止日期' : '',
    storageZone: !form.value.storageZone ? '请选择存放分区' : ''
  }
  
  return !Object.values(errors.value).some(error => error)
}

function onSubmit() {
  if (!validateAll()) {
    return
  }

  const updatedFood = {
    name: form.value.name.trim(),
    category: form.value.category,
    quantity: parseInt(form.value.quantity),
    unit: form.value.unit.trim(),
    expiryDate: form.value.expiryDate,
    storageZone: form.value.storageZone,
    price: form.value.price ? parseFloat(form.value.price) : 0,
    totalPrice: form.value.totalPrice ? parseFloat(form.value.totalPrice) : 0
  }

  store.editFood(foodId, updatedFood)
  
  showToast({ message: '修改成功', type: 'success' })
  setTimeout(() => {
    router.push('/inventory')
  }, 1500)
}

function confirmDelete() {
  if (confirm('确定要删除这个食材吗？')) {
    store.deleteFood(foodId)
    showToast({ message: '删除成功', type: 'success' })
    setTimeout(() => {
      router.push('/inventory')
    }, 1500)
  }
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.edit-food-page {
  padding: 16px;
  padding-bottom: 60px;
  background: var(--sk-bg);
}

.food-form {
  background: var(--sk-card);
  padding: 16px;
  border-radius: 14px;
  box-shadow: 0 2px 8px rgba(61, 53, 46, 0.04);
}

.form-actions {
  margin-top: 24px;
}

.form-actions .van-button {
  margin-bottom: 12px;
}

.form-actions .van-button--primary {
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}
</style>