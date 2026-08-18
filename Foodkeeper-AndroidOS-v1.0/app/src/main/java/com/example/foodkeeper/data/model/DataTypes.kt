package com.example.foodkeeper.data.model

import com.google.gson.annotations.SerializedName

// ===== 枚举 =====
typealias StorageZone = String // "refrigerator" | "freezer" | "room"
typealias ExpiryStatus = String // "green" | "yellow" | "red" | "expired"

// ===== 食材项（对应 ArkTS FoodItem） =====
data class FoodItem(
    var id: Long = 0,
    var name: String = "",
    var categoryId: Int = 0,
    var quantity: Double = 0.0,
    var unit: String = "",
    var purchaseDate: String = "",   // YYYY-MM-DD
    var expiryDate: String = "",     // YYYY-MM-DD
    var storageZone: StorageZone = "refrigerator",
    var price: Double = 0.0,
    var totalPrice: Double = 0.0,
    var createdAt: String = "",
    var updatedAt: String = "",
)

// ===== 分类信息 =====
data class CategoryInfo(
    val value: String,
    val label: String,
    val id: Int,
    val color: String,
    val bg: String,
)

// ===== 存储分区 =====
data class StorageZoneInfo(
    val value: String,
    val label: String,
)

// ===== 食材知识库条目（对应 ArkTS FoodKnowledge） =====
data class FoodKnowledge(
    val name: String,
    val aliases: List<String>,
    val expiryDays: Int,
    val storageZone: StorageZone,
    val unit: String,
    val categoryId: Int,
    val tip: String,
)

// ===== 菜谱食材模板 =====
data class RecipeIngredientTemplate(
    val name: String,
    val quantity: String,
)

// ===== 匹配库存后的食材 =====
data class MatchedIngredient(
    val name: String,
    val quantity: String,
    val haveIt: Boolean,
    val days: Int,
    val isExpiring: Boolean,
)

// ===== 结构化烹饪步骤 =====
data class RecipeStep(
    val title: String,
    val detail: String,
    val heat: String,
    val duration: String,
    val tip: String,
)

// ===== 菜谱模板 =====
data class RecipeTemplate(
    val name: String,
    val time: String,
    val calories: String,
    val caloriesNum: Int,
    val difficulty: String,
    val category: Int,  // 0=快手懒人餐, 1=家常正餐, 2=减脂轻食餐
    val ingredients: List<RecipeIngredientTemplate>,
    val stepList: List<RecipeStep>,
)

// ===== 运行时菜谱（匹配库存后） =====
data class DisplayRecipe(
    val name: String,
    val time: String,
    val calories: String,
    val caloriesNum: Int,
    val difficulty: String,
    val category: Int,
    val ingredients: List<MatchedIngredient>,
    val stepList: List<RecipeStep>,
    val steps: Int,
    val needBuyNames: List<String>,
    val haveCount: Int,
    val totalCount: Int,
    val expiringCount: Int,
    val priorityScore: Double,
)

// ===== 菜谱生成选项 =====
data class RecipeGenOptions(
    val excludeNames: List<String> = emptyList(),
    val favoriteNames: List<String> = emptyList(),
)

// ===== 单位换算结果 =====
data class ConversionResult(
    val ok: Boolean,
    val value: Double,
    val reason: String? = null,
    val note: String? = null,
)

// ===== 食材卡片展示信息 =====
data class FoodCardInfo(
    val status: ExpiryStatus,
    val statusClass: String,
    val statusColor: String,
    val statusBg: String,
    val daysText: String,
    val catDot: String,
    val catLabel: String,
)

// ===== 新增食材输入 =====
data class AddFoodInput(
    val name: String,
    val categoryId: Int,
    val quantity: Double,
    val unit: String,
    val storageZone: StorageZone,
    val price: Double,
    val totalPrice: Double,
    val expiryDays: Int,
    val purchaseDate: String? = null,
)

// ===== 更新食材参数（所有字段可选） =====
data class UpdateFoodPatch(
    val name: String? = null,
    val categoryId: Int? = null,
    val quantity: Double? = null,
    val unit: String? = null,
    val purchaseDate: String? = null,
    val expiryDate: String? = null,
    val storageZone: StorageZone? = null,
    val price: Double? = null,
    val totalPrice: Double? = null,
)

// ===== 单个食材扣减明细 =====
enum class ConsumeAction {
    DEDUCTED,               // 正常扣减（单位一致）
    DEDUCTED_CONVERTED,     // 单位不一致但已成功换算后扣减
    INSUFFICIENT,           // 库存不足扣到0
    SKIPPED_NO_AMOUNT,      // 用量无法量化（如"适量"）
    SKIPPED_NOT_INVENTORY,  // 库存无匹配
    SKIPPED_UNIT_MISMATCH,  // 单位不匹配且无法自动换算
}

data class ConsumeDetail(
    val name: String,
    val recipeAmount: String,
    val recipeUnit: String,
    val inventoryUnit: String,
    val beforeQty: Double,
    val afterQty: Double,
    val deductedAmount: Double,
    val conversionNote: String,
    val action: ConsumeAction,
)

// ===== 批量扣减结果 =====
data class ConsumeResult(
    val ok: Boolean,
    val deducted: Int,
    val skipped: Int,
    val insufficient: Int,
    val details: List<ConsumeDetail>,
)

// ===== 完成烹饪历史记录 =====
data class ConsumeLog(
    val timestamp: String,
    val recipeName: String,
    val deducted: Int,
    val skipped: Int,
    val insufficient: Int,
    val details: List<ConsumeDetail>,
)

// ===== 本地存储信息 =====
data class StorageInfo(
    val foodCount: Int,
    val version: Int,
    val resetCount: Int,
    val logCount: Int,
    val isLocal: Boolean,
)
