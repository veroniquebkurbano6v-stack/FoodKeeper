<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getFoodDetail, updateFood } from '@/api/food'
import type { FoodUpdateDTO, FoodDetailVO } from '@/types'

const router = useRouter()
const route = useRoute()

const form = ref<FoodDetailVO | null>(null)

onMounted(() => {
  loadData()
})

async function loadData() {
  const id = Number(route.params.id)
  try {
    const res = await getFoodDetail(id)
    form.value = res.data
  } catch {
    ElMessage.error('加载食材详情失败')
    router.push('/food')
  }
}

async function handleSubmit() {
  if (!form.value) return
  
  const data: FoodUpdateDTO = {
    id: form.value.id,
    name: form.value.name,
    quantity: form.value.quantity,
    storageZone: form.value.storageZone
  }
  
  try {
    await updateFood(data)
    ElMessage.success('更新成功')
    router.push('/food')
  } catch {
    ElMessage.error('更新失败')
  }
}

function handleCancel() {
  router.push('/food')
}
</script>

<template>
  <div class="food-edit">
    <el-form :model="form" label-width="120px">
      <el-form-item label="食材名称">
        <el-input v-model="form?.name" placeholder="请输入食材名称" />
      </el-form-item>

      <el-form-item label="分类">
        <el-input :value="form?.categoryName" disabled />
      </el-form-item>

      <el-form-item label="数量">
        <div class="number-input-group">
          <el-button @click="form && (form.quantity = Math.max(1, form.quantity - 1))">-</el-button>
          <el-input v-model="form?.quantity" type="number" :min="1" class="number-input" />
          <el-button @click="form && form.quantity++">+</el-button>
        </div>
      </el-form-item>

      <el-form-item label="单位">
        <el-input :value="form?.unit" disabled />
      </el-form-item>

      <el-form-item label="采购日期">
        <el-input :value="form?.purchaseDate" disabled />
      </el-form-item>

      <el-form-item label="保质期截止">
        <el-input :value="form?.expiryDate" disabled />
      </el-form-item>

      <el-form-item label="存放分区">
        <el-select v-model="form?.storageZone" placeholder="请选择存放分区">
          <el-option :label="'冷藏室'" :value="'refrigerator'" />
          <el-option :label="'冷冻层'" :value="'freezer'" />
          <el-option :label="'常温'" :value="'room'" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="handleSubmit">提交</el-button>
        <el-button @click="handleCancel">取消</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<style scoped>
.food-edit {
  padding: 0;
}

.number-input-group {
  display: flex;
  align-items: center;
  width: 200px;
}

.number-input-group .el-button {
  width: 40px;
  border-radius: 4px 0 0 4px;
}

.number-input-group .el-button:last-child {
  border-radius: 0 4px 4px 0;
}

.number-input-group .number-input {
  flex: 1;
  border-radius: 0;
  border-left: none;
  border-right: none;
}

.number-input-group .number-input .el-input__inner {
  text-align: center;
}
</style>