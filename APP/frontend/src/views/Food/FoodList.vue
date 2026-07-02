<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageFoodList, deleteFood } from '@/api/food'
import type { FoodListVO } from '@/types'

const router = useRouter()

const tableData = ref<FoodListVO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const keyword = ref('')
const selectedCategory = ref<number | null>(null)

const categoryNames: Record<number, string> = {
  1: '蔬菜',
  2: '肉类',
  3: '海鲜',
  4: '干货',
  5: '冷冻',
  6: '调料',
  7: '水果',
  8: '饮料'
}

const categoryColors: Record<number, { bg: string; border: string; text: string; light: string }> = {
  1: { bg: '#f0f9eb', border: '#b7eb8f', text: '#52c41a', light: '#e6fffb' },
  2: { bg: '#fff2e8', border: '#ffd5b3', text: '#fa8c16', light: '#fff7e6' },
  3: { bg: '#e6f7ff', border: '#91d5ff', text: '#1890ff', light: '#f0f5ff' },
  4: { bg: '#f9f0ff', border: '#d3adf7', text: '#722ed1', light: '#f5f0ff' },
  5: { bg: '#e6fffb', border: '#87e8de', text: '#13c2c2', light: '#f0fffc' },
  6: { bg: '#fff7e6', border: '#ffd591', text: '#faad14', light: '#fffbe6' },
  7: { bg: '#fff1f0', border: '#ffccc7', text: '#f5222d', light: '#fff5f5' },
  8: { bg: '#f6ffed', border: '#b7eb8f', text: '#52c41a', light: '#f0fff0' }
}

const categories = [
  { id: null, name: '全部' },
  { id: 1, name: '蔬菜' },
  { id: 2, name: '肉类' },
  { id: 3, name: '海鲜' },
  { id: 4, name: '干货' },
  { id: 5, name: '冷冻' },
  { id: 6, name: '调料' },
  { id: 7, name: '水果' },
  { id: 8, name: '饮料' }
]

const filteredData = computed(() => {
  if (selectedCategory.value === null) {
    return tableData.value
  }
  return tableData.value.filter(item => item.categoryId === selectedCategory.value)
})

const groupedData = computed(() => {
  const groups: Record<string, FoodListVO[]> = {}
  filteredData.value.forEach(food => {
    const categoryId = food.categoryId || 0
    const categoryName = categoryNames[categoryId] || '其他'
    if (!groups[categoryName]) {
      groups[categoryName] = []
    }
    groups[categoryName].push(food)
  })
  return groups
})

onMounted(() => {
  loadData()
})

function getCategoryColor(categoryId: number) {
  return categoryColors[categoryId] || { bg: '#f5f7fa', border: '#dcdfe6', text: '#606266', light: '#f5f7fa' }
}

async function loadData() {
  try {
    const res = await pageFoodList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value,
      categoryId: selectedCategory.value || undefined
    })
    tableData.value = res.data.records || []
    total.value = res.data.total || 0
  } catch (error) {
    console.error('加载数据失败:', error)
  }
}

function handleAdd() {
  router.push('/food/add')
}

function handleEdit(id: number) {
  router.push(`/food/edit/${id}`)
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定要删除该食材吗？', '提示', { type: 'warning' })
    await deleteFood(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // 用户取消删除
  }
}

function handleSearch() {
  currentPage.value = 1
  loadData()
}

function handleSizeChange(val: number) {
  pageSize.value = val
  loadData()
}

function handleCurrentChange(val: number) {
  currentPage.value = val
  loadData()
}
</script>

<template>
  <div class="food-list">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">食材管理</h2>
        <p class="page-desc">管理库存中的所有食材</p>
      </div>
      <el-button type="primary" size="large" @click="handleAdd" class="add-btn">
        添加食材
      </el-button>
    </div>

    <div class="filter-bar">
      <el-input 
        v-model="keyword" 
        placeholder="搜索食材名称" 
        class="search-input"
        @keyup.enter="handleSearch" 
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <div class="category-filter">
        <span class="filter-label">分类筛选：</span>
        <el-radio-group v-model="selectedCategory" @change="handleSearch" class="category-radio">
          <el-radio-button v-for="cat in categories" :key="cat.id" :label="cat.id" :value="cat.id">
            {{ cat.name }}
          </el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <div v-if="filteredData.length === 0" class="empty-state">
      <el-empty description="暂无食材数据" :image-size="80" />
    </div>

    <div v-else class="food-grid">
      <div v-for="(foods, category) in groupedData" :key="category" class="category-section">
        <div class="category-header">
          <span class="category-name">{{ category }}</span>
          <span class="category-count">{{ foods.length }}种食材</span>
        </div>
        <el-row :gutter="16">
          <el-col v-for="food in foods" :key="food.id" :span="8">
            <div 
              class="food-card"
              :style="{
                borderLeftColor: getCategoryColor(food.categoryId || 1).border,
                backgroundColor: getCategoryColor(food.categoryId || 1).light
              }"
            >
              <div class="card-header">
                <span class="food-name">{{ food.name }}</span>
                <el-tag :type="getStatusType(food.expiryStatus)" size="small" class="status-tag">
                  {{ getStatusLabel(food.expiryStatus) }}
                </el-tag>
              </div>
              <div class="card-body">
                <div class="food-detail">
                  <span class="detail-label">数量：</span>
                  <span class="detail-value">{{ food.quantity }} {{ food.unit }}</span>
                </div>
                <div class="food-detail">
                  <span class="detail-label">保质期截止：</span>
                  <span class="detail-value">{{ food.expiryDate }}</span>
                </div>
                <div class="food-detail">
                  <span class="detail-label">剩余天数：</span>
                  <span class="detail-value" :class="getRemainingDaysClass(food.remainingDays)">
                    {{ food.remainingDays }}天
                  </span>
                </div>
                <div class="food-detail">
                  <span class="detail-label">存放：</span>
                  <span class="detail-value">{{ getStorageZoneLabel(food.storageZone) }}</span>
                </div>
              </div>
              <div class="card-footer">
                <el-button type="primary" size="small" @click="handleEdit(food.id)">编辑</el-button>
                <el-button type="danger" size="small" @click="handleDelete(food.id)">删除</el-button>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>
    </div>

    <div v-if="filteredData.length > 0" class="pagination-wrap">
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="currentPage"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>
  </div>
</template>

<script lang="ts">
function getStatusType(status: string) {
  switch (status) {
    case 'GREEN': return 'success'
    case 'YELLOW': return 'warning'
    case 'RED': return 'danger'
    case 'EXPIRED': return 'info'
    default: return 'default'
  }
}

function getStatusLabel(status: string) {
  switch (status) {
    case 'GREEN': return '正常'
    case 'YELLOW': return '临期'
    case 'RED': return '即将过期'
    case 'EXPIRED': return '已过期'
    default: return status
  }
}

function getStorageZoneLabel(zone: string) {
  switch (zone) {
    case 'refrigerator': return '冷藏室'
    case 'freezer': return '冷冻层'
    case 'room': return '常温'
    default: return zone
  }
}

function getRemainingDaysClass(days: number) {
  if (days <= 0) return 'expired'
  if (days <= 3) return 'danger'
  if (days <= 7) return 'warning'
  return 'normal'
}
</script>

<style scoped>
.food-list {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.page-title {
  font-size: 24px;
  font-weight: bold;
  color: #303133;
  margin: 0 0 8px 0;
}

.page-desc {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.add-btn {
  display: flex;
  align-items: center;
}

.filter-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.search-input {
  width: 280px;
}

.category-filter {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-size: 14px;
  color: #606266;
}

.category-radio {
  display: flex;
}

.empty-state {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.food-grid {
  margin-top: 10px;
}

.category-section {
  margin-bottom: 30px;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 4px solid #409eff;
}

.category-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.category-count {
  font-size: 13px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 10px;
  border-radius: 12px;
}

.food-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border-left: 4px solid;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  transition: all 0.25s ease;
  margin-bottom: 16px;
}

.food-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.food-name {
  font-size: 18px;
  font-weight: bold;
  color: #303133;
}

.status-tag {
  flex-shrink: 0;
}

.card-body {
  margin-bottom: 16px;
}

.food-detail {
  display: flex;
  align-items: center;
  margin-bottom: 8px;
  font-size: 14px;
}

.food-detail:last-child {
  margin-bottom: 0;
}

.detail-label {
  color: #909399;
  min-width: 80px;
}

.detail-value {
  color: #303133;
  font-weight: 500;
}

.detail-value.normal {
  color: #52c41a;
}

.detail-value.warning {
  color: #faad14;
}

.detail-value.danger {
  color: #f5222d;
}

.detail-value.expired {
  color: #f5222d;
  text-decoration: line-through;
}

.card-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}
</style>