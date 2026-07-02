export function generateId() {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

export function formatDate(date) {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

export function formatDateTime(date) {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}`
}

export function calculateRemainingDays(expiryDate) {
  if (!expiryDate) return null
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  const expiry = new Date(expiryDate)
  expiry.setHours(0, 0, 0, 0)
  const diffTime = expiry - today
  return Math.ceil(diffTime / (1000 * 60 * 60 * 24))
}

export function getExpiryStatus(remainingDays) {
  if (remainingDays === null) return 'normal'
  if (remainingDays < 0) return 'expired'
  if (remainingDays <= 2) return 'red'
  if (remainingDays <= 7) return 'yellow'
  return 'green'
}

export function getExpiryStatusLabel(status) {
  const labels = {
    green: '正常',
    yellow: '临期',
    red: '即将过期',
    expired: '已过期',
    normal: '正常'
  }
  return labels[status] || '正常'
}

export function getExpiryStatusColor(status) {
  const colors = {
    green: '#07c160',
    yellow: '#ff976a',
    red: '#ee0a24',
    expired: '#999999',
    normal: '#07c160'
  }
  return colors[status] || '#07c160'
}

export function getCategoryLabel(value, categories) {
  const category = categories.find(c => c.value === value)
  return category ? category.label : value
}

export function getStorageZoneLabel(value, zones) {
  const zone = zones.find(z => z.value === value)
  return zone ? zone.label : value
}

export function getOutboundTypeLabel(type) {
  const labels = {
    cook: '烹饪消耗',
    discard: '变质报废',
    clear: '存量清零'
  }
  return labels[type] || type
}

export function getReasonLabel(reason) {
  const labels = {
    expired: '过期变质',
    storage: '储存不当',
    other: '其他'
  }
  return labels[reason] || reason
}

export function calculateMonthExpenses(foods, logs) {
  const now = new Date()
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  
  let totalExpense = 0
  foods.forEach(food => {
    if (food.purchaseDate.startsWith(currentMonth) && food.totalPrice) {
      totalExpense += food.totalPrice
    }
  })
  
  return totalExpense
}

export function calculateMonthWaste(logs) {
  const now = new Date()
  const currentMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  
  let wasteQuantity = 0
  let wasteCost = 0
  
  logs.forEach(log => {
    if (log.outboundType === 'discard' && log.createdAt.startsWith(currentMonth)) {
      wasteQuantity += log.quantity
    }
  })
  
  return { wasteQuantity, wasteCost }
}

export function debounce(func, wait) {
  let timeout
  return function executedFunction(...args) {
    const later = () => {
      clearTimeout(timeout)
      func(...args)
    }
    clearTimeout(timeout)
    timeout = setTimeout(later, wait)
  }
}

/**
 * 将时间戳格式化为相对时间文案（用于 AI 菜谱缓存「上次生成」展示）
 * @param {number} timestamp - 毫秒时间戳
 * @returns {string} 如「刚刚」「5 分钟前」「3 小时前」「2 天前」；无值返回 ''
 */
export function formatRelativeTime(timestamp) {
  if (!timestamp || typeof timestamp !== 'number') return ''
  const diff = Date.now() - timestamp
  if (diff < 0) return '刚刚'
  const seconds = Math.floor(diff / 1000)
  if (seconds < 60) return '刚刚'
  const minutes = Math.floor(seconds / 60)
  if (minutes < 60) return `${minutes} 分钟前`
  const hours = Math.floor(minutes / 60)
  if (hours < 24) return `${hours} 小时前`
  const days = Math.floor(hours / 24)
  return `${days} 天前`
}

/**
 * 食材分类专属配色（暖意厨房主题，低饱和）
 * @param {string} category - 分类 value
 * @returns {{bg:string, dot:string, label:string}} 分类底色 / 圆点色 / 中文标签
 */
export function getCategoryColors(category) {
  // 八大分类配色（暖意厨房主题，低饱和；与食材知识库文档分类对齐）
  const palette = {
    vegetable: { bg: '#EDF1E8', dot: '#7C9473', label: '蔬菜' },
    meat:      { bg: '#F5E6E2', dot: '#C97B6E', label: '肉类' },
    seafood:   { bg: '#E8EEF4', dot: '#6E8CA8', label: '海鲜' },
    dried:     { bg: '#F0E9DC', dot: '#A88B6A', label: '干货' },
    frozen:    { bg: '#E9EEF1', dot: '#8BA0B0', label: '冷冻' },
    seasoning: { bg: '#F5EDD8', dot: '#C9A94E', label: '调料' },
    fruit:     { bg: '#F5E3DF', dot: '#C75D4D', label: '水果' },
    drink:     { bg: '#EDF0E4', dot: '#9CB088', label: '饮料' }
  }
  return palette[category] || { bg: '#F5EFE4', dot: '#B5A99A', label: category || '其他' }
}

/**
 * 临期视觉信息（统一临期预警展示）
 * @param {string} status - getExpiryStatus 返回值：green/yellow/red/expired/normal
 * @returns {{color:string, bg:string, dotClass:string, pulse:boolean, badge:string, icon:string}}
 */
export function getExpiryVisual(status) {
  const map = {
    green:  { color: '#7C9473', bg: '#EDF1E8', dotClass: 'fresh',    pulse: false, badge: '',          icon: '' },
    normal: { color: '#7C9473', bg: '#EDF1E8', dotClass: 'fresh',    pulse: false, badge: '',          icon: '' },
    yellow: { color: '#D4A55B', bg: '#FBF1DE', dotClass: 'warn',     pulse: false, badge: '临期',      icon: 'warning-o' },
    red:    { color: '#C75D4D', bg: '#FAE6E2', dotClass: 'danger',   pulse: true,  badge: '紧急消耗',  icon: 'fire-o' },
    expired:{ color: '#9A8E80', bg: '#EDEAE5', dotClass: 'expired',  pulse: false, badge: '已过期',    icon: 'clock-o' }
  }
  return map[status] || map.normal
}

/**
 * 紧凑食材卡统一展示元信息（供首页/库存/临期/报表 网格卡片复用）
 * @param {object} food - 食材对象
 * @param {string} categoryLabel - 分类中文标签（可选，缺省用分类内置标签）
 * @returns {{status:string, dotClass:string, daysText:string, catDot:string, catLabel:string}}
 */
export function getFoodCardMeta(food, categoryLabel) {
  const remaining = calculateRemainingDays(food.expiryDate)
  const status = getExpiryStatus(remaining)
  const cat = getCategoryColors(food.category)
  let daysText
  if (remaining < 0) daysText = `已过期 ${Math.abs(remaining)}天`
  else if (remaining === 0) daysText = '今天过期'
  else daysText = `剩 ${remaining} 天`
  const dotClassMap = { red: 'danger', yellow: 'warn', expired: 'expired', green: 'fresh', normal: 'fresh' }
  return {
    status,
    dotClass: dotClassMap[status] || 'fresh',
    daysText,
    catDot: cat.dot,
    catLabel: categoryLabel || cat.label
  }
}