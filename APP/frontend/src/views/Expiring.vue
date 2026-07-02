<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { listExpiringFood, deleteFood } from '@/api/food'
import type { FoodListVO } from '@/types'

const tableData = ref<FoodListVO[]>([])

onMounted(() => {
  loadData()
})

async function loadData() {
  try {
    const res = await listExpiringFood(7)
    tableData.value = res.data || []
  } catch {
    ElMessage.error('加载数据失败')
  }
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
</script>

<template>
  <div class="expiring">
    <el-table :data="tableData" border style="width: 100%;">
      <el-table-column prop="name" label="食材名称" />
      <el-table-column prop="quantity" label="数量" />
      <el-table-column prop="unit" label="单位" />
      <el-table-column prop="expiryDate" label="保质期截止" />
      <el-table-column prop="remainingDays" label="剩余天数" />
      <el-table-column prop="expiryStatus" label="状态">
        <template #default="{ row }">
          <el-tag :type="getStatusType(row.expiryStatus)">
            {{ getStatusLabel(row.expiryStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作">
        <template #default="{ row }">
          <el-button type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
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
</script>

<style scoped>
.expiring {
  padding: 0;
}
</style>