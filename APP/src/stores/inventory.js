import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { generateId, formatDate, calculateRemainingDays, getExpiryStatus } from '../utils'

export const useInventoryStore = defineStore('inventory', () => {
  const foods = ref([])
  const logs = ref([])
  const preferences = ref({
    dietaryRestrictions: [],
    preferredCuisine: [],
    spiciness: ''
  })
  const systemConfig = ref({
    lastInspectionDate: '',
    backupSnapshot: '',
    alertCount: {}
  })

  // 八大分类（与食材知识库文档分类对齐：蔬菜/肉类/海鲜/干货/冷冻/调料/水果/饮料）
  // value 保持向后兼容（原有 vegetable/meat/seafood/dried/frozen 不变），新增 seasoning/fruit/drink
  const categories = [
    { value: 'vegetable', label: '蔬菜' },
    { value: 'meat', label: '肉类' },
    { value: 'seafood', label: '海鲜' },
    { value: 'dried', label: '干货' },
    { value: 'frozen', label: '冷冻' },
    { value: 'seasoning', label: '调料' },
    { value: 'fruit', label: '水果' },
    { value: 'drink', label: '饮料' }
  ]

  const storageZones = [
    { value: 'refrigerator', label: '冷藏室' },
    { value: 'freezer', label: '冷冻层' },
    { value: 'room', label: '常温' }
  ]

  const defaultExpiryDays = {
    vegetable: {
      '西红柿': 7, '黄瓜': 7, '土豆': 15, '胡萝卜': 14, '青椒': 5,
      '青菜': 3, '白菜': 7, '洋葱': 30, '大蒜': 30, '生姜': 14,
      '西兰花': 5, '芹菜': 7, '蘑菇': 3, '茄子': 7, '豆角': 5,
      '菠菜': 3, '生菜': 2, '番茄': 7, '冬瓜': 14, '南瓜': 30
    },
    meat: {
      '猪肉': 5, '牛肉': 7, '羊肉': 5, '鸡肉': 5, '鸡蛋': 14,
      '鸭蛋': 14, '牛奶': 7, '酸奶': 7, '奶酪': 30, '黄油': 90
    },
    seafood: {
      '鱼': 3, '虾': 3, '蟹': 3, '贝类': 2, '鱿鱼': 3,
      '三文鱼': 3, '带鱼': 3, '虾仁': 7, '扇贝': 2, '生蚝': 1
    },
    dried: {
      '大米': 365, '面粉': 180, '酱油': 365, '醋': 365, '盐': 365,
      '糖': 365, '食用油': 365, '花椒': 365, '八角': 365, '桂皮': 365,
      '干辣椒': 365, '豆瓣酱': 180, '豆腐乳': 180, '木耳': 365, '香菇': 365
    },
    frozen: {
      '速冻水饺': 180, '速冻包子': 180, '速冻汤圆': 180, '冻肉': 365,
      '冻虾': 180, '冻鱼': 180, '冰淇淋': 180, '冻蔬菜': 180
    }
  }

  const totalCategories = computed(() => foods.value.length)

  const totalQuantity = computed(() => {
    return foods.value.reduce((sum, food) => sum + food.quantity, 0)
  })

  const expiringCount = computed(() => {
    return foods.value.filter(food => {
      const status = getExpiryStatus(calculateRemainingDays(food.expiryDate))
      return status === 'yellow' || status === 'red' || status === 'expired'
    }).length
  })

  const expiredCount = computed(() => {
    return foods.value.filter(food => {
      return getExpiryStatus(calculateRemainingDays(food.expiryDate)) === 'expired'
    }).length
  })

  const sortedFoods = computed(() => {
    return [...foods.value].sort((a, b) => {
      const statusOrder = { expired: 0, red: 1, yellow: 2, green: 3, normal: 4 }
      const statusA = getExpiryStatus(calculateRemainingDays(a.expiryDate))
      const statusB = getExpiryStatus(calculateRemainingDays(b.expiryDate))
      if (statusOrder[statusA] !== statusOrder[statusB]) {
        return statusOrder[statusA] - statusOrder[statusB]
      }
      return calculateRemainingDays(a.expiryDate) - calculateRemainingDays(b.expiryDate)
    })
  })

  const validFoods = computed(() => {
    return foods.value.filter(food => {
      const remaining = calculateRemainingDays(food.expiryDate)
      return food.quantity > 0 && remaining >= 0
    })
  })

  const redFoods = computed(() => {
    return foods.value.filter(food => {
      const remaining = calculateRemainingDays(food.expiryDate)
      return getExpiryStatus(remaining) === 'red'
    })
  })

  function loadData() {
    try {
      const foodsData = localStorage.getItem('foods')
      const logsData = localStorage.getItem('logs')
      const preferencesData = localStorage.getItem('preferences')
      const systemConfigData = localStorage.getItem('systemConfig')

      if (foodsData) {
        foods.value = JSON.parse(foodsData)
      } else {
        foods.value = getDemoData()
        saveData()
      }

      if (logsData) {
        logs.value = JSON.parse(logsData)
      }

      if (preferencesData) {
        preferences.value = JSON.parse(preferencesData)
      }

      if (systemConfigData) {
        systemConfig.value = JSON.parse(systemConfigData)
      }

      checkInspection()
    } catch (error) {
      console.error('加载数据失败:', error)
      foods.value = getDemoData()
      saveData()
    }
  }

  function saveData() {
    localStorage.setItem('foods', JSON.stringify(foods.value))
    localStorage.setItem('logs', JSON.stringify(logs.value))
    localStorage.setItem('preferences', JSON.stringify(preferences.value))
    localStorage.setItem('systemConfig', JSON.stringify(systemConfig.value))
  }

  function getDemoData() {
    const today = new Date()
    const formatDateStr = (date) => {
      const d = new Date(date)
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
    }

    return [
      { id: '1', name: '西红柿', category: 'vegetable', quantity: 3, unit: '个', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 2 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 5.00, totalPrice: 15.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '2', name: '鸡蛋', category: 'meat', quantity: 10, unit: '个', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 5 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 1.50, totalPrice: 15.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '3', name: '青椒', category: 'vegetable', quantity: 2, unit: '个', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 3 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 3.00, totalPrice: 6.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '4', name: '猪肉', category: 'meat', quantity: 500, unit: '克', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 1 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 30.00, totalPrice: 30.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '5', name: '土豆', category: 'vegetable', quantity: 5, unit: '个', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 10 * 24 * 60 * 60 * 1000)), storageZone: 'room', price: 2.00, totalPrice: 10.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '6', name: '牛奶', category: 'meat', quantity: 2, unit: '盒', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 7 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 6.50, totalPrice: 13.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '7', name: '大米', category: 'dried', quantity: 5, unit: '公斤', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 365 * 24 * 60 * 60 * 1000)), storageZone: 'room', price: 8.00, totalPrice: 40.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '8', name: '速冻水饺', category: 'frozen', quantity: 1, unit: '袋', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 180 * 24 * 60 * 60 * 1000)), storageZone: 'freezer', price: 18.00, totalPrice: 18.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '9', name: '鱼', category: 'seafood', quantity: 1, unit: '条', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 3 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 25.00, totalPrice: 25.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '10', name: '食用油', category: 'dried', quantity: 1, unit: '桶', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 365 * 24 * 60 * 60 * 1000)), storageZone: 'room', price: 68.00, totalPrice: 68.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '11', name: '酸奶', category: 'meat', quantity: 4, unit: '杯', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() - 1 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 5.00, totalPrice: 20.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '12', name: '青菜', category: 'vegetable', quantity: 2, unit: '把', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 1 * 24 * 60 * 60 * 1000)), storageZone: 'refrigerator', price: 4.00, totalPrice: 8.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '13', name: '酱油', category: 'dried', quantity: 1, unit: '瓶', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 365 * 24 * 60 * 60 * 1000)), storageZone: 'room', price: 12.00, totalPrice: 12.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '14', name: '虾仁', category: 'seafood', quantity: 200, unit: '克', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 5 * 24 * 60 * 60 * 1000)), storageZone: 'freezer', price: 35.00, totalPrice: 35.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) },
      { id: '15', name: '洋葱', category: 'vegetable', quantity: 3, unit: '个', purchaseDate: formatDateStr(today), expiryDate: formatDateStr(new Date(today.getTime() + 20 * 24 * 60 * 60 * 1000)), storageZone: 'room', price: 3.00, totalPrice: 9.00, photo: '', status: 'normal', createdAt: formatDateStr(today), updatedAt: formatDateStr(today) }
    ]
  }

  function checkInspection() {
    const today = formatDate(new Date())
    if (systemConfig.value.lastInspectionDate !== today) {
      updateExpiryStatus()
      systemConfig.value.lastInspectionDate = today
      systemConfig.value.backupSnapshot = JSON.stringify(foods.value)
      systemConfig.value.alertCount = {}
      saveData()
    }
  }

  function updateExpiryStatus() {
    foods.value.forEach(food => {
      const remaining = calculateRemainingDays(food.expiryDate)
      food.status = getExpiryStatus(remaining)
    })
  }

  function addFood(food) {
    const existingFood = foods.value.find(f => 
      f.name === food.name && f.category === food.category && f.storageZone === food.storageZone
    )

    let addedFood
    if (existingFood) {
      existingFood.quantity += food.quantity
      existingFood.totalPrice += food.totalPrice || 0
      existingFood.updatedAt = formatDate(new Date())
      addedFood = existingFood
    } else {
      addedFood = {
        id: generateId(),
        ...food,
        status: 'normal',
        createdAt: formatDate(new Date()),
        updatedAt: formatDate(new Date())
      }
      foods.value.push(addedFood)
    }

    logs.value.unshift({
      id: generateId(),
      foodId: addedFood.id,
      foodName: addedFood.name,
      inboundType: 'purchase',
      quantity: food.quantity,
      remainingQuantity: addedFood.quantity,
      createdAt: formatDate(new Date())
    })

    updateExpiryStatus()
    saveData()
    showAlertIfNeeded(addedFood)
  }

  function showAlertIfNeeded(food) {
    const remaining = calculateRemainingDays(food.expiryDate)
    const status = getExpiryStatus(remaining)
    if (status === 'red' || status === 'yellow') {
      const today = formatDate(new Date())
      if (!systemConfig.value.alertCount[food.id] || systemConfig.value.alertCount[food.id] !== today) {
        systemConfig.value.alertCount[food.id] = today
        saveData()
      }
    }
  }

  function editFood(id, updatedFood) {
    const index = foods.value.findIndex(f => f.id === id)
    if (index !== -1) {
      foods.value[index] = {
        ...foods.value[index],
        ...updatedFood,
        updatedAt: formatDate(new Date())
      }
      updateExpiryStatus()
      saveData()
    }
  }

  function deleteFood(id) {
    foods.value = foods.value.filter(f => f.id !== id)
    saveData()
  }

  function cookOutbound(foodIds, quantities, reason = '') {
    // 校验阶段：收集所有问题后统一抛出（前端等价事务校验，避免部分扣减导致数据不一致）
    const outboundItems = []
    const errors = []

    for (let i = 0; i < foodIds.length; i++) {
      const food = foods.value.find(f => f.id === foodIds[i])
      const qty = quantities[i]
      if (!food) {
        errors.push(`食材不存在（id=${foodIds[i]}）`)
        continue
      }
      if (!Number.isFinite(qty) || qty <= 0) {
        errors.push(`${food.name}：消耗数量必须大于 0`)
        continue
      }
      if (food.quantity < qty) {
        errors.push(`${food.name} 库存不足（当前 ${food.quantity}${food.unit}，需 ${qty}）`)
        continue
      }
      outboundItems.push({ food, quantity: qty })
    }

    if (errors.length > 0) {
      throw new Error(errors.join('；'))
    }
    if (outboundItems.length === 0) {
      throw new Error('没有可消耗的食材')
    }

    // 执行阶段：全部校验通过后才统一扣减，保证数据一致性
    outboundItems.forEach(({ food, quantity }) => {
      food.quantity -= quantity
      food.updatedAt = formatDate(new Date())

      logs.value.unshift({
        id: generateId(),
        foodId: food.id,
        foodName: food.name,
        outboundType: 'cook',
        quantity,
        reason,
        remainingQuantity: food.quantity,
        createdAt: formatDate(new Date())
      })
    })

    updateExpiryStatus()
    saveData()
  }

  function discardOutbound(foodId, reason) {
    const food = foods.value.find(f => f.id === foodId)
    if (!food) {
      throw new Error('食材不存在')
    }

    if (food.quantity === 0) {
      throw new Error('该食材已无库存，无需报废')
    }

    const discardedQuantity = food.quantity
    food.quantity = 0
    food.updatedAt = formatDate(new Date())

    logs.value.unshift({
      id: generateId(),
      foodId: food.id,
      foodName: food.name,
      outboundType: 'discard',
      quantity: discardedQuantity,
      reason,
      remainingQuantity: 0,
      createdAt: formatDate(new Date())
    })

    updateExpiryStatus()
    saveData()
  }

  function clearInventory(foodId) {
    const food = foods.value.find(f => f.id === foodId)
    if (!food) {
      throw new Error('食材不存在')
    }

    const clearedQuantity = food.quantity
    food.quantity = 0
    food.updatedAt = formatDate(new Date())

    logs.value.unshift({
      id: generateId(),
      foodId: food.id,
      foodName: food.name,
      outboundType: 'clear',
      quantity: clearedQuantity,
      reason: '',
      remainingQuantity: 0,
      createdAt: formatDate(new Date())
    })

    updateExpiryStatus()
    saveData()
  }

  function setPreferences(newPreferences) {
    preferences.value = { ...newPreferences }
    saveData()
  }

  function resetAllData() {
    foods.value = getDemoData()
    logs.value = []
    preferences.value = {
      dietaryRestrictions: [],
      preferredCuisine: [],
      spiciness: ''
    }
    systemConfig.value = {
      lastInspectionDate: '',
      backupSnapshot: '',
      alertCount: {}
    }
    saveData()
  }

  function exportData(format) {
    const data = {
      foods: foods.value,
      logs: logs.value,
      preferences: preferences.value,
      exportTime: formatDate(new Date())
    }

    if (format === 'json') {
      return JSON.stringify(data, null, 2)
    } else {
      let csv = ''
      const headers = ['名称', '分类', '数量', '单位', '采购日期', '保质期截止', '存放分区', '单价', '总价']
      csv += headers.join(',') + '\n'
      foods.value.forEach(food => {
        const categoryLabel = categories.find(c => c.value === food.category)?.label || food.category
        const zoneLabel = storageZones.find(z => z.value === food.storageZone)?.label || food.storageZone
        csv += [food.name, categoryLabel, food.quantity, food.unit, food.purchaseDate, food.expiryDate, zoneLabel, food.price, food.totalPrice].join(',') + '\n'
      })
      return csv
    }
  }

  return {
    foods,
    logs,
    preferences,
    systemConfig,
    categories,
    storageZones,
    defaultExpiryDays,
    totalCategories,
    totalQuantity,
    expiringCount,
    expiredCount,
    sortedFoods,
    validFoods,
    redFoods,
    loadData,
    saveData,
    addFood,
    editFood,
    deleteFood,
    cookOutbound,
    discardOutbound,
    clearInventory,
    setPreferences,
    resetAllData,
    exportData,
    updateExpiryStatus
  }
})