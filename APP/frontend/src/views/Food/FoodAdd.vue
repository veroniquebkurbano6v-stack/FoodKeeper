<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { addFood, listFavoriteFoods, addFavoriteFood, getFavoriteFoodByName } from '@/api/food'
import type { FoodAddDTO, FavoriteFoodListVO } from '@/types'

const router = useRouter()

const form = ref<FoodAddDTO>({
  name: '',
  categoryId: 1,
  quantity: 1,
  unit: '',
  purchaseDate: new Date().toISOString().split('T')[0],
  expiryDays: 7,
  storageZone: 'refrigerator'
})

const favoriteFoods = ref<FavoriteFoodListVO[]>([])
const showFavoriteSelect = ref(false)
const selectedFavorite = ref<FavoriteFoodListVO | null>(null)

const categoryColors: Record<number, { bg: string; border: string; text: string }> = {
  1: { bg: '#f0f9eb', border: '#b7eb8f', text: '#52c41a' },
  2: { bg: '#fff2e8', border: '#ffd5b3', text: '#fa8c16' },
  3: { bg: '#e6f7ff', border: '#91d5ff', text: '#1890ff' },
  4: { bg: '#f9f0ff', border: '#d3adf7', text: '#722ed1' },
  5: { bg: '#e6fffb', border: '#87e8de', text: '#13c2c2' },
  6: { bg: '#fff7e6', border: '#ffd591', text: '#faad14' },
  7: { bg: '#fff1f0', border: '#ffccc7', text: '#f5222d' },
  8: { bg: '#f6ffed', border: '#b7eb8f', text: '#52c41a' }
}

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

const groupedFavoriteFoods = computed(() => {
  const groups: Record<string, FavoriteFoodListVO[]> = {}
  favoriteFoods.value.forEach(food => {
    const categoryName = food.categoryName || categoryNames[food.categoryId] || '其他'
    if (!groups[categoryName]) {
      groups[categoryName] = []
    }
    groups[categoryName].push(food)
  })
  return groups
})

const rules = {
  name: [{ required: true, message: '请输入食材名称', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入单位', trigger: 'blur' }],
  purchaseDate: [{ required: true, message: '请选择采购日期', trigger: 'blur' }],
  expiryDays: [{ required: true, message: '请输入保质期天数', trigger: 'blur' }],
  storageZone: [{ required: true, message: '请选择存放分区', trigger: 'blur' }]
}

onMounted(async () => {
  try {
    const res = await listFavoriteFoods()
    if (res.code === 200) {
      favoriteFoods.value = res.data
    }
  } catch {
    console.log('获取常用食品列表失败')
  }
})

function getCategoryColor(categoryId: number) {
  return categoryColors[categoryId] || { bg: '#f5f7fa', border: '#dcdfe6', text: '#606266' }
}

function handleSelectFavorite(favorite: FavoriteFoodListVO) {
  selectedFavorite.value = favorite
  form.value.name = favorite.name
  form.value.categoryId = favorite.categoryId
  form.value.unit = favorite.unit
  form.value.storageZone = favorite.storageZone
  if (favorite.defaultExpiryDays) {
    form.value.expiryDays = favorite.defaultExpiryDays
  }
  showFavoriteSelect.value = false
  ElMessage.success('已选择常用食品，只需填写数量即可')
}

async function handleSubmit() {
  if (!form.value.name || !form.value.unit) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    const res = await addFood(form.value)
    if (res.code === 200) {
      ElMessage.success('添加成功')

      // 检查是否已经是常用食品（独立try-catch，不影响主流程）
      let isAlreadyFavorite = false
      try {
        const favoriteRes = await getFavoriteFoodByName(form.value.name)
        isAlreadyFavorite = favoriteRes.code === 200 && favoriteRes.data !== null
      } catch {
        // 查询常用食品失败，不影响主流程，默认当做不是常用食品
        isAlreadyFavorite = false
      }

      // 如果不是常用食品,询问用户是否添加为常用食品
      if (!isAlreadyFavorite) {
        try {
          await ElMessageBox.confirm(
            '是否将此食材添加为常用食品？下次添加时可以快速选择。',
            '添加为常用食品',
            {
              confirmButtonText: '确定',
              cancelButtonText: '取消',
              type: 'info'
            }
          )
          // 用户确认添加
          try {
            await addFavoriteFood({
              name: form.value.name,
              categoryId: form.value.categoryId,
              unit: form.value.unit,
              storageZone: form.value.storageZone,
              defaultExpiryDays: form.value.expiryDays
            })
            ElMessage.success('已添加为常用食品')
          } catch {
            ElMessage.error('添加常用食品失败')
          }
        } catch {
          // 用户取消,不做任何操作
        }
      }

      router.push('/food')
    }
  } catch {
    ElMessage.error('添加失败')
  }
}

function handleCancel() {
  router.push('/food')
}

function toggleFavoriteSelect() {
  showFavoriteSelect.value = !showFavoriteSelect.value
}
</script>

<template>
  <div class="food-add">
    <div class="page-header">
      <h2 class="page-title">添加食材</h2>
      <p class="page-desc">快速添加新食材到库存管理系统</p>
    </div>

    <el-form :model="form" :rules="rules" label-width="130px" class="food-form">
      <div class="favorite-section">
        <div class="favorite-header">
          <el-button 
            type="success" 
            @click="toggleFavoriteSelect"
            :icon="showFavoriteSelect ? 'Fold' : 'Expand'"
            class="toggle-btn"
          >
            {{ showFavoriteSelect ? '收起常用食品' : '展开常用食品' }}
          </el-button>
          <span v-if="favoriteFoods.length === 0" class="empty-tip">暂无常用食品，添加食材后可设置为常用</span>
        </div>

        <div v-if="showFavoriteSelect && favoriteFoods.length > 0" class="favorite-list">
          <el-divider content-position="left">
            <span class="divider-title">常用食品列表</span>
            <span class="divider-count">（{{ favoriteFoods.length }}个）</span>
          </el-divider>

          <div v-for="(foods, category) in groupedFavoriteFoods" :key="category" class="category-group">
            <div class="category-header">
              <span class="category-label">{{ category }}</span>
              <span class="category-count">{{ foods.length }}种</span>
            </div>
            <el-row :gutter="16">
              <el-col v-for="favorite in foods" :key="favorite.id" :span="8">
                <div 
                  class="favorite-card"
                  @click="handleSelectFavorite(favorite)"
                  :style="{
                    backgroundColor: getCategoryColor(favorite.categoryId).bg,
                    borderColor: getCategoryColor(favorite.categoryId).border
                  }"
                >
                  <div class="favorite-name">{{ favorite.name }}</div>
                  <div class="favorite-category" :style="{ color: getCategoryColor(favorite.categoryId).text }">
                    {{ favorite.categoryName }}
                  </div>
                  <div class="favorite-footer">
                    <span class="use-count">使用 {{ favorite.useCount }} 次</span>
                    <span class="storage-zone">{{ favorite.storageZoneName }}</span>
                  </div>
                </div>
              </el-col>
            </el-row>
          </div>
        </div>
      </div>

      <div class="form-section">
        <el-divider content-position="left">
          <span class="divider-title">食材信息</span>
        </el-divider>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="食材名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入食材名称" size="large" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" size="large">
                <el-option :label="'蔬菜'" :value="1" />
                <el-option :label="'肉类'" :value="2" />
                <el-option :label="'海鲜'" :value="3" />
                <el-option :label="'干货'" :value="4" />
                <el-option :label="'冷冻'" :value="5" />
                <el-option :label="'调料'" :value="6" />
                <el-option :label="'水果'" :value="7" />
                <el-option :label="'饮料'" :value="8" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="数量" prop="quantity">
              <el-input v-model="form.quantity" placeholder="请输入数量" size="large">
                <template #prepend>
                  <el-button @click="form.quantity = Math.max(1, Number(form.quantity) - 1)">-</el-button>
                </template>
                <template #append>
                  <el-button @click="form.quantity = Number(form.quantity) + 1">+</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="单位" prop="unit">
              <el-input v-model="form.unit" placeholder="如：个、斤、袋" size="large" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="保质期天数" prop="expiryDays">
              <el-input v-model="form.expiryDays" placeholder="请输入保质期天数" size="large">
                <template #prepend>
                  <el-button @click="form.expiryDays = Math.max(1, Number(form.expiryDays) - 1)">-</el-button>
                </template>
                <template #append>
                  <el-button @click="form.expiryDays = Number(form.expiryDays) + 1">+</el-button>
                </template>
              </el-input>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="24">
          <el-col :span="12">
            <el-form-item label="采购日期" prop="purchaseDate">
              <el-date-picker v-model="form.purchaseDate" type="date" placeholder="选择采购日期" size="large" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="存放分区" prop="storageZone">
              <el-select v-model="form.storageZone" placeholder="请选择存放分区" size="large">
                <el-option :label="'冷藏室'" :value="'refrigerator'" />
                <el-option :label="'冷冻层'" :value="'freezer'" />
                <el-option :label="'常温'" :value="'room'" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <div class="form-actions">
          <el-button type="primary" size="large" @click="handleSubmit" class="submit-btn">
            提交
          </el-button>
          <el-button size="large" @click="handleCancel" class="cancel-btn">
            取消
          </el-button>
        </div>
      </div>
    </el-form>
  </div>
</template>

<style scoped>
.food-add {
  padding: 0;
  max-width: 900px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
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

.food-form {
  background: #fff;
  padding: 30px;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.favorite-section {
  margin-bottom: 30px;
}

.favorite-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toggle-btn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.empty-tip {
  font-size: 13px;
  color: #909399;
}

.favorite-list {
  margin-top: 20px;
}

.divider-title {
  font-size: 16px;
  font-weight: bold;
  color: #303133;
}

.divider-count {
  font-size: 14px;
  color: #909399;
  font-weight: normal;
}

.category-group {
  margin-bottom: 24px;
}

.category-group:last-child {
  margin-bottom: 0;
}

.category-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
  padding-left: 8px;
  border-left: 4px solid #409eff;
}

.category-label {
  font-size: 14px;
  font-weight: bold;
  color: #303133;
}

.category-count {
  font-size: 12px;
  color: #909399;
  background: #f5f7fa;
  padding: 2px 8px;
  border-radius: 10px;
}

.favorite-card {
  cursor: pointer;
  padding: 16px;
  border-radius: 8px;
  border: 2px solid;
  transition: all 0.25s ease;
  text-align: center;
  margin-bottom: 12px;
}

.favorite-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.favorite-name {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
  margin-bottom: 6px;
}

.favorite-category {
  font-size: 12px;
  font-weight: 500;
  margin-bottom: 10px;
}

.favorite-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.use-count {
  font-size: 11px;
  color: #909399;
}

.storage-zone {
  font-size: 11px;
  color: #909399;
  background: rgba(0, 0, 0, 0.04);
  padding: 2px 6px;
  border-radius: 4px;
}

.form-section {
  padding-top: 10px;
}

.form-actions {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.submit-btn {
  width: 140px;
}

.cancel-btn {
  width: 140px;
}
</style>