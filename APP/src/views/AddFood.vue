<template>
  <div class="add-food-page">
    <van-nav-bar title="食材入库" left-arrow @click-left="goBack" />
    
    <van-form @submit="onSubmit" class="food-form">
      <van-field
        v-model="form.name"
        label="食材名称"
        placeholder="如：白菜/番茄/土豆/牛肉"
        required
        :error="!!errors.name"
        :error-message="errors.name"
        @blur="onNameBlur"
        @keyup.enter="onNameEnter"
      />

      <!-- 食材知识库自动匹配状态 -->
      <div v-if="matchStatus !== 'idle'" class="knowledge-status" :class="matchStatus">
        <van-icon :name="matchStatus === 'matched' ? 'success' : 'info-o'" size="14" />
        <span v-if="matchStatus === 'matched'">已匹配「{{ lastMatchedName }}」，下方字段已自动填充，可手动修改</span>
        <span v-else>未匹配到知识库条目，请手动填写下方信息</span>
        <span v-if="matchStatus === 'matched' && matchedTip" class="tip" :title="matchedTip">{{ matchedTip }}</span>
      </div>

      <van-field
        :model-value="categoryDisplay"
        label="分类"
        placeholder="请选择分类"
        readonly
        required
        :error="!!errors.category"
        :error-message="errors.category"
        :class="{ 'field-flash': isFlashing('category') }"
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
        @blur="validateQuantity"
      />
      
      <van-field
        v-model="form.unit"
        label="单位"
        placeholder="请输入单位（个/斤/克/升等）"
        required
        :error="!!errors.unit"
        :error-message="errors.unit"
        :class="{ 'field-flash': isFlashing('unit') }"
        @input="markDirty('unit')"
        @blur="validateUnit"
      />
      
      <van-field
        v-model="form.purchaseDate"
        label="采购日期"
        placeholder="请选择采购日期"
        readonly
        required
        :error="!!errors.purchaseDate"
        :error-message="errors.purchaseDate"
        @click="showPurchaseDatePicker = true"
      >
        <template #left-icon>
          <van-icon name="calendar-o" />
        </template>
        <template #right-icon>
          <van-icon name="question-o" size="16" @click.stop="showDateTip = true" />
        </template>
      </van-field>
      
      <van-field
        v-model="form.expiryDays"
        label="保质期天数"
        placeholder="系统自动填充"
        type="number"
        :error="!!errors.expiryDays"
        :error-message="errors.expiryDays"
        :class="{ 'field-flash': isFlashing('expiryDays') }"
        @input="markDirty('expiryDays')"
        @blur="validateExpiryDays"
      >
        <template #left-icon>
          <van-icon name="clock-o" />
        </template>
      </van-field>
      
      <van-field
        v-model="expiryDateText"
        label="保质期截止"
        placeholder="自动计算"
        readonly
      >
        <template #left-icon>
          <van-icon name="calendar-o" />
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
        :class="{ 'field-flash': isFlashing('storageZone') }"
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
          确认入库
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
      v-model:show="showPurchaseDatePicker"
      title="选择采购日期"
      :min-date="minPurchaseDate"
      :max-date="maxPurchaseDate"
      :default-date="defaultPurchaseDate"
      :show-confirm="false"
      :first-day-of-week="1"
      closeable
      round
      @confirm="onPurchaseDateConfirm"
      @open="onCalendarOpen"
    />

    <!-- 采购日期操作指引 -->
    <van-dialog
      v-model:show="showDateTip"
      title="采购日期说明"
      :show-cancel-button="false"
      confirm-button-text="知道了"
    >
      <div style="padding: 16px; font-size: 14px; color: var(--sk-text-2); line-height: 1.6;">
        1. 默认填写系统当天日期，可直接提交<br>
        2. 点击日期字段打开仿真日历修改<br>
        3. 支持月份/年份快速切换<br>
        4. 打开后可用键盘方向键 ← → ↑ ↓ 切换日期，Enter 确认
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
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { useInventoryStore } from '../stores/inventory'
import { formatDate, getCategoryLabel, getStorageZoneLabel, debounce } from '../utils'
import { matchFood, categoryLabelToValue, zoneLabelToValue } from '../utils/foodKnowledge'

const router = useRouter()
const store = useInventoryStore()

const form = ref({
  name: '',
  category: '',
  quantity: '',
  unit: '',
  purchaseDate: '',
  expiryDays: '',
  storageZone: '',
  price: '',
  totalPrice: ''
})

const errors = ref({
  name: '',
  category: '',
  quantity: '',
  unit: '',
  purchaseDate: '',
  expiryDays: '',
  storageZone: ''
})

const showCategoryPicker = ref(false)
const showPurchaseDatePicker = ref(false)
const showZonePicker = ref(false)
const showDateTip = ref(false)

// ===== 食材知识库自动填充状态 =====
// dirtyFields：用户已手动修改的字段集合，自动填充时不覆盖这些字段
//   - 文本字段（unit/expiryDays）通过 @input 标记
//   - 选择字段（category/storageZone）通过 picker confirm 标记
//   - 匹配到「不同」食材时重置（允许新食材重新填充全部字段）
const dirtyFields = ref(new Set())
const matchStatus = ref('idle')        // idle | matched | nomatch
const matchedTip = ref('')             // 知识库中的保存提示
const lastMatchedName = ref('')        // 上次命中的食材名，用于检测食材切换
const flashing = ref(new Set())        // 正在闪烁高亮的字段（视觉反馈）

function markDirty(field) {
  dirtyFields.value.add(field)
  dirtyFields.value = new Set(dirtyFields.value) // 触发响应式
}

function isFlashing(field) {
  return flashing.value.has(field)
}

function flashField(field) {
  flashing.value.add(field)
  flashing.value = new Set(flashing.value)
  setTimeout(() => {
    flashing.value.delete(field)
    flashing.value = new Set(flashing.value)
  }, 1200)
}

/**
 * 知识库匹配核心：按食材名称/别名查找，命中则填充未 dirty 的字段
 * 填充字段：分类(category)、单位(unit)、保质期天数(expiryDays)、存放分区(storageZone)
 */
function tryMatchKnowledge() {
  const name = form.value.name.trim()
  if (!name) {
    matchStatus.value = 'idle'
    matchedTip.value = ''
    lastMatchedName.value = ''
    return
  }
  const matched = matchFood(name)
  if (!matched) {
    matchStatus.value = 'nomatch'
    matchedTip.value = ''
    console.info('[知识库匹配] 无匹配:', name)
    return
  }
  // 匹配到不同食材 → 重置 dirty，允许全部字段重新填充
  if (matched.name !== lastMatchedName.value) {
    dirtyFields.value = new Set()
    lastMatchedName.value = matched.name
  }
  matchStatus.value = 'matched'
  matchedTip.value = matched.tip
  console.info('[知识库匹配] 命中:', name, '→', matched.name, { ...matched })

  const catVal = categoryLabelToValue(matched.category)
  const zoneVal = zoneLabelToValue(matched.storageZone)

  if (catVal && !dirtyFields.value.has('category') && form.value.category !== catVal) {
    form.value.category = catVal
    flashField('category')
  }
  if (matched.unit && !dirtyFields.value.has('unit') && form.value.unit !== matched.unit) {
    form.value.unit = matched.unit
    flashField('unit')
  }
  if (!dirtyFields.value.has('expiryDays') && form.value.expiryDays !== String(matched.expiryDays)) {
    form.value.expiryDays = String(matched.expiryDays)
    flashField('expiryDays')
  }
  if (zoneVal && !dirtyFields.value.has('storageZone') && form.value.storageZone !== zoneVal) {
    form.value.storageZone = zoneVal
    flashField('storageZone')
  }
}

// 名称输入实时匹配（400ms 防抖，边输入边匹配）
const debouncedMatch = debounce(tryMatchKnowledge, 400)
watch(() => form.value.name, () => {
  debouncedMatch()
})

// 失焦/回车时立即匹配（不等防抖），保证用户离开字段时已填充
function onNameBlur() {
  tryMatchKnowledge()
  validateName()
}
function onNameEnter() {
  tryMatchKnowledge()
  validateName()
}

// ===== 仿真日历相关状态 =====
// 系统当前日期（仅在初始化时取一次，避免渲染过程中时间漂移）
const today = new Date()
today.setHours(0, 0, 0, 0)

// 采购日期范围约束：禁止未来日期（max-date = 今天）
const minPurchaseDate = new Date(today.getFullYear() - 5, 0, 1)
const maxPurchaseDate = new Date(today)

// 日历默认选中日期（Date 对象），用于 van-calendar :default-date
const defaultPurchaseDate = ref(new Date(today))

// 键盘导航用：当前高亮日期（与 defaultPurchaseDate 联动）
const cursorDate = ref(new Date(today))

// 表单字段显示用：将 value（如 'vegetable'）转为中文标签（如 '蔬菜'）
const categoryDisplay = computed(() =>
  form.value.category ? getCategoryLabel(form.value.category, store.categories) : ''
)
const zoneDisplay = computed(() =>
  form.value.storageZone ? getStorageZoneLabel(form.value.storageZone, store.storageZones) : ''
)

const expiryDateText = computed(() => {
  if (!form.value.purchaseDate || !form.value.expiryDays) return ''
  const purchase = new Date(form.value.purchaseDate)
  const expiry = new Date(purchase.getTime() + parseInt(form.value.expiryDays) * 24 * 60 * 60 * 1000)
  return formatDate(expiry)
})

onMounted(() => {
  store.loadData()
  // 采购日期默认当天（YYYY-MM-DD），用户可点击修改
  form.value.purchaseDate = formatDate(new Date())
})

function validateName() {
  if (!form.value.name.trim()) {
    errors.value.name = '请输入食材名称'
  } else {
    errors.value.name = ''
  }
}

function validateCategory() {
  if (!form.value.category) {
    errors.value.category = '请选择分类'
  } else {
    errors.value.category = ''
  }
}

function validateQuantity() {
  if (!form.value.quantity || parseInt(form.value.quantity) <= 0) {
    errors.value.quantity = '请输入有效数量'
  } else {
    errors.value.quantity = ''
  }
}

function validateUnit() {
  if (!form.value.unit.trim()) {
    errors.value.unit = '请输入单位'
  } else {
    errors.value.unit = ''
  }
}

function validatePurchaseDate() {
  if (!form.value.purchaseDate) {
    errors.value.purchaseDate = '请选择采购日期'
  } else {
    errors.value.purchaseDate = ''
  }
}

function validateExpiryDays() {
  if (!form.value.expiryDays || parseInt(form.value.expiryDays) <= 0) {
    errors.value.expiryDays = '请输入有效保质期天数'
  } else {
    errors.value.expiryDays = ''
  }
}

function validateStorageZone() {
  if (!form.value.storageZone) {
    errors.value.storageZone = '请选择存放分区'
  } else {
    errors.value.storageZone = ''
  }
}

function onCategoryConfirm({ selectedValues }) {
  form.value.category = selectedValues[0]
  markDirty('category')
  showCategoryPicker.value = false
  validateCategory()
}

function onPurchaseDateConfirm(date) {
  // van-calendar @confirm 回调参数为 Date 对象
  if (!(date instanceof Date) || isNaN(date.getTime())) return
  form.value.purchaseDate = formatDate(date)
  defaultPurchaseDate.value = new Date(date)
  cursorDate.value = new Date(date)
  showPurchaseDatePicker.value = false
  validatePurchaseDate()
}

// 日历打开时绑定键盘导航（方向键 ← → ↑ ↓ 切换日期，Enter 确认，Esc 关闭）
function onCalendarOpen() {
  // 同步游标到当前选中日期
  cursorDate.value = defaultPurchaseDate.value
    ? new Date(defaultPurchaseDate.value)
    : new Date(today)
  setTimeout(() => {
    window.addEventListener('keydown', onCalendarKeydown)
  }, 50)
}

function onCalendarKeydown(e) {
  if (!showPurchaseDatePicker.value) return
  const step = { ArrowLeft: -1, ArrowRight: 1, ArrowUp: -7, ArrowDown: 7 }[e.key]
  if (step !== undefined) {
    e.preventDefault()
    const next = new Date(cursorDate.value)
    next.setDate(next.getDate() + step)
    // 边界约束：不超出允许范围
    if (next < minPurchaseDate) next.setTime(minPurchaseDate.getTime())
    if (next > maxPurchaseDate) next.setTime(maxPurchaseDate.getTime())
    cursorDate.value = next
    defaultPurchaseDate.value = new Date(next)
  } else if (e.key === 'Enter') {
    e.preventDefault()
    onPurchaseDateConfirm(new Date(cursorDate.value))
  } else if (e.key === 'Escape') {
    showPurchaseDatePicker.value = false
  }
}

// 日历关闭时解绑键盘事件（通过 watch show 状态）
watch(showPurchaseDatePicker, (val) => {
  if (!val) {
    window.removeEventListener('keydown', onCalendarKeydown)
  }
})

function onZoneConfirm({ selectedValues }) {
  form.value.storageZone = selectedValues[0]
  markDirty('storageZone')
  showZonePicker.value = false
  validateStorageZone()
}

function validateAll() {
  validateName()
  validateCategory()
  validateQuantity()
  validateUnit()
  validatePurchaseDate()
  validateExpiryDays()
  validateStorageZone()
  
  return !Object.values(errors.value).some(error => error)
}

function onSubmit() {
  if (!validateAll()) {
    return
  }

  const purchase = new Date(form.value.purchaseDate)
  const expiry = new Date(purchase.getTime() + parseInt(form.value.expiryDays) * 24 * 60 * 60 * 1000)

  const food = {
    name: form.value.name.trim(),
    category: form.value.category,
    quantity: parseInt(form.value.quantity),
    unit: form.value.unit.trim(),
    purchaseDate: form.value.purchaseDate,
    expiryDate: formatDate(expiry),
    storageZone: form.value.storageZone,
    price: form.value.price ? parseFloat(form.value.price) : 0,
    totalPrice: form.value.totalPrice ? parseFloat(form.value.totalPrice) : 0
  }

  store.addFood(food)

  showToast({ message: '入库成功', type: 'success' })
  setTimeout(() => {
    router.push('/inventory')
  }, 1500)
}

function goBack() {
  router.back()
}
</script>

<style scoped>
.add-food-page {
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
  box-shadow: 0 6px 16px rgba(124, 148, 115, 0.28);
}

/* ===== 食材知识库自动匹配状态提示 ===== */
.knowledge-status {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  margin: -4px 0 8px;
  font-size: 12px;
  border-radius: 8px;
  line-height: 1.4;
}

.knowledge-status.matched {
  background: #EDF1E8;
  color: #5C7A52;
}

.knowledge-status.nomatch {
  background: #F5EFE4;
  color: #9A8E80;
}

.knowledge-status .tip {
  margin-left: auto;
  color: #7C9473;
  font-size: 11px;
  max-width: 45%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* ===== 自动填充字段高亮闪烁动画（视觉反馈） ===== */
.field-flash :deep(.van-field__body) {
  animation: field-flash-anim 1.2s ease-out;
}

@keyframes field-flash-anim {
  0%   { background-color: rgba(124, 148, 115, 0.22); }
  60%  { background-color: rgba(124, 148, 115, 0.10); }
  100% { background-color: transparent; }
}
</style>