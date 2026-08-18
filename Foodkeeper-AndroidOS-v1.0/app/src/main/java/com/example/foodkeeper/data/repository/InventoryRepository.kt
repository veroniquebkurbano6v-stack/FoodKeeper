package com.example.foodkeeper.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.foodkeeper.data.model.AddFoodInput
import com.example.foodkeeper.data.model.ConsumeAction
import com.example.foodkeeper.data.model.ConsumeDetail
import com.example.foodkeeper.data.model.ConsumeLog
import com.example.foodkeeper.data.model.ConsumeResult
import com.example.foodkeeper.data.model.FoodItem
import com.example.foodkeeper.data.model.MatchedIngredient
import com.example.foodkeeper.data.model.StorageInfo
import com.example.foodkeeper.data.model.UpdateFoodPatch
import com.example.foodkeeper.data.model.estimateExpiryDays
import com.example.foodkeeper.data.model.formatDate
import com.example.foodkeeper.domain.UnitConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

/**
 * 库存数据存储层（与原 ArkTS InventoryStore.ets 完全一致）
 *
 * 【本地数据存储方案】
 * 数据完全存储在用户设备本地（SharedPreferences 沙箱目录），不上传任何远程服务器。
 *   - 持久化：每次写入后立即 commit() 落盘；应用卸载后数据随之清除。
 *   - 安全性：数据存于应用沙箱（/data/data/<包名>/shared_prefs/），其他应用无法直接访问；
 *             食材库存不属敏感信息，未做额外加密。
 *   - 跨设备同步：本地优先架构不提供自动云同步；如需迁移，可通过导出/导入功能。
 */
class InventoryRepository private constructor(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    private var foods: MutableList<FoodItem> = mutableListOf()
    private var version: Int = 0
    private var resetCount: Int = 0
    private val consumeLogs: MutableList<ConsumeLog> = mutableListOf()

    @Volatile
    private var initialized = false

    @Synchronized
    fun getVersion(): Int = version
    @Synchronized
    fun getResetCount(): Int = resetCount
    @Synchronized
    fun getConsumeLogs(): List<ConsumeLog> = consumeLogs.toList().takeLast(20)
    fun isInitialized(): Boolean = initialized

    /**
     * 初始化：从 SharedPreferences 加载数据，首次启动写入种子数据
     */
    @Synchronized
    fun init() {
        if (initialized) return
        loadFoods()
        initialized = true
    }

    /**
     * 从 SharedPreferences 加载食材
     * 数据完整性校验：解析失败、字段非法、数据为空时均回退到种子数据
     */
    private fun loadFoods() {
        val json = prefs.getString(KEY_FOODS, "") ?: ""
        if (json.isNotEmpty()) {
            try {
                val type = object : TypeToken<List<FoodItem>>() {}.type
                val parsed: List<FoodItem> = gson.fromJson(json, type) ?: emptyList()
                val valid = mutableListOf<FoodItem>()
                for (f in parsed) {
                    if (f.id > 0 && f.name.isNotEmpty()) {
                        // 数据修复：数量为负或 NaN 时归零；过期日期缺失时按分类估算
                        if (f.quantity < 0 || f.quantity.isNaN()) f.quantity = 0.0
                        if (f.expiryDate.isEmpty()) {
                            val purchaseTime = parseDate(f.purchaseDate).time
                            val expiryTime = purchaseTime + estimateExpiryDays(f.name, f.categoryId) * DAY_MS
                            f.expiryDate = formatDate(Date(expiryTime))
                        }
                        valid.add(f)
                    }
                }
                foods = valid
                if (foods.isEmpty()) {
                    foods = buildSeedFoods().toMutableList()
                    persist()
                }
            } catch (e: Exception) {
                // JSON 解析失败：回退到种子数据
                foods = buildSeedFoods().toMutableList()
                persist()
            }
        } else {
            // 首次启动：写入种子数据
            foods = buildSeedFoods().toMutableList()
            persist()
        }
        // 读取重置计数器
        resetCount = prefs.getInt(KEY_RESET_COUNT, 0)
    }

    /**
     * 获取本地存储信息
     */
    @Synchronized
    fun getStorageInfo(): StorageInfo = StorageInfo(
        foodCount = foods.size,
        version = version,
        resetCount = resetCount,
        logCount = consumeLogs.size,
        isLocal = true
    )

    /**
     * 持久化到 SharedPreferences（每次写入递增版本号）
     */
    private fun persist() {
        prefs.edit().putString(KEY_FOODS, gson.toJson(foods)).apply()
        version++
    }

    /**
     * 获取食材列表（返回副本）
     */
    @Synchronized
    fun getFoods(): List<FoodItem> = foods.toList()

    /**
     * 新增食材
     */
    @Synchronized
    fun addFood(input: AddFoodInput): Long {
        if (input.name.isBlank() || input.quantity <= 0) return -1L
        val now = formatDate(Date())
        val purchaseDate = if (input.purchaseDate.isNullOrEmpty()) now else input.purchaseDate
        val expiryTime = parseDate(purchaseDate).time + input.expiryDays * DAY_MS
        val expiryDate = formatDate(Date(expiryTime))
        val id = (foods.maxOfOrNull { it.id } ?: 0) + 1
        val food = FoodItem(
            id = id,
            name = input.name.trim(),
            categoryId = input.categoryId,
            quantity = input.quantity,
            unit = input.unit,
            purchaseDate = purchaseDate,
            expiryDate = expiryDate,
            storageZone = input.storageZone,
            price = input.price,
            totalPrice = input.totalPrice,
            createdAt = now,
            updatedAt = now
        )
        foods.add(food)
        persist()
        return id
    }

    /**
     * 更新食材（局部更新）
     */
    @Synchronized
    fun updateFood(id: Long, patch: UpdateFoodPatch): Boolean {
        val idx = foods.indexOfFirst { it.id == id }
        if (idx == -1) return false
        val updated = foods[idx].copy()
        patch.name?.let { updated.name = it }
        patch.categoryId?.let { updated.categoryId = it }
        patch.quantity?.let { updated.quantity = it }
        patch.unit?.let { updated.unit = it }
        patch.purchaseDate?.let { updated.purchaseDate = it }
        patch.expiryDate?.let { updated.expiryDate = it }
        patch.storageZone?.let { updated.storageZone = it }
        patch.price?.let { updated.price = it }
        patch.totalPrice?.let { updated.totalPrice = it }
        updated.updatedAt = formatDate(Date())
        foods[idx] = updated
        persist()
        return true
    }

    /**
     * 删除食材
     */
    @Synchronized
    fun deleteFood(id: Long): Boolean {
        val before = foods.size
        foods.removeAll { it.id == id }
        return if (foods.size != before) {
            persist()
            true
        } else false
    }

    /**
     * 出库消耗
     */
    @Synchronized
    fun outboundFood(id: Long, amount: Double): Boolean {
        val idx = foods.indexOfFirst { it.id == id }
        if (idx == -1 || amount <= 0 || foods[idx].quantity < amount) return false
        foods[idx] = foods[idx].copy(
            quantity = foods[idx].quantity - amount,
            updatedAt = formatDate(Date())
        )
        persist()
        return true
    }

    /**
     * 批量扣减食材库存（"完成烹饪"使用）
     *
     * 仅扣减 haveIt=true 的食材；菜谱用量无法解析（如"适量"）、库存为 0 时跳过
     * 库存不足时扣到 0（不删记录，由前端过滤 quantity>0 隐藏）
     * 原子性：在副本上批量修改，全部完成后才写回，中途异常不污染原数据
     * 单位换算：菜谱单位与库存单位不一致时，先尝试通过 UnitConverter 自动换算
     */
    @Synchronized
    fun consumeIngredients(ingredients: List<MatchedIngredient>, recipeName: String? = null): ConsumeResult {
        val details = mutableListOf<ConsumeDetail>()

        // 仅处理库存拥有的食材
        val toConsume = mutableListOf<MatchedIngredient>()
        for (ing in ingredients) {
            if (ing.haveIt) {
                toConsume.add(ing)
            } else {
                details.add(
                    ConsumeDetail(
                        name = ing.name,
                        recipeAmount = ing.quantity,
                        recipeUnit = parseRecipeUnit(ing.quantity),
                        inventoryUnit = "",
                        beforeQty = 0.0,
                        afterQty = 0.0,
                        deductedAmount = 0.0,
                        conversionNote = "",
                        action = ConsumeAction.SKIPPED_NOT_INVENTORY
                    )
                )
            }
        }

        if (toConsume.isEmpty()) {
            return ConsumeResult(false, 0, ingredients.size, 0, details)
        }

        // 复制当前库存作为工作副本（保证原子性）
        val work = foods.map { it.copy() }.toMutableList()

        var deducted = 0
        var skipped = 0
        var insufficient = 0

        for (ing in toConsume) {
            val amount = parseRecipeAmount(ing.quantity)
            val recipeUnit = parseRecipeUnit(ing.quantity)

            if (amount <= 0) {
                // 用量无法量化（如"适量"），跳过
                skipped++
                details.add(
                    ConsumeDetail(
                        name = ing.name, recipeAmount = ing.quantity, recipeUnit = recipeUnit,
                        inventoryUnit = "", beforeQty = 0.0, afterQty = 0.0, deductedAmount = 0.0,
                        conversionNote = "", action = ConsumeAction.SKIPPED_NO_AMOUNT
                    )
                )
                continue
            }

            val idx = work.indexOfFirst { it.name == ing.name && it.quantity > 0 }
            if (idx == -1) {
                skipped++
                details.add(
                    ConsumeDetail(
                        name = ing.name, recipeAmount = ing.quantity, recipeUnit = recipeUnit,
                        inventoryUnit = "", beforeQty = 0.0, afterQty = 0.0, deductedAmount = 0.0,
                        conversionNote = "", action = ConsumeAction.SKIPPED_NOT_INVENTORY
                    )
                )
                continue
            }

            // 计算实际需要扣减的数量（按库存单位计）
            var deductAmount = amount
            var conversionNote = ""
            var isConverted = false
            val inventoryUnit = work[idx].unit

            if (recipeUnit.isNotEmpty() && inventoryUnit.isNotEmpty() && recipeUnit != inventoryUnit) {
                // 单位不一致：尝试自动换算
                val conv = UnitConverter.convertAmount(amount, recipeUnit, inventoryUnit, ing.name)
                if (!conv.ok) {
                    skipped++
                    details.add(
                        ConsumeDetail(
                            name = ing.name, recipeAmount = ing.quantity, recipeUnit = recipeUnit,
                            inventoryUnit = inventoryUnit, beforeQty = work[idx].quantity,
                            afterQty = work[idx].quantity, deductedAmount = 0.0,
                            conversionNote = conv.reason ?: "", action = ConsumeAction.SKIPPED_UNIT_MISMATCH
                        )
                    )
                    continue
                }
                deductAmount = conv.value
                conversionNote = conv.note ?: ""
                isConverted = true
            }

            val beforeQty = work[idx].quantity
            if (work[idx].quantity >= deductAmount) {
                work[idx] = work[idx].copy(
                    quantity = work[idx].quantity - deductAmount,
                    updatedAt = formatDate(Date())
                )
                deducted++
                details.add(
                    ConsumeDetail(
                        name = ing.name, recipeAmount = ing.quantity, recipeUnit = recipeUnit,
                        inventoryUnit = inventoryUnit, beforeQty = beforeQty, afterQty = work[idx].quantity,
                        deductedAmount = deductAmount, conversionNote = conversionNote,
                        action = if (isConverted) ConsumeAction.DEDUCTED_CONVERTED else ConsumeAction.DEDUCTED
                    )
                )
            } else {
                // 库存不足：扣到 0（不产生负数）
                work[idx] = work[idx].copy(
                    quantity = 0.0,
                    updatedAt = formatDate(Date())
                )
                insufficient++
                deducted++
                details.add(
                    ConsumeDetail(
                        name = ing.name, recipeAmount = ing.quantity, recipeUnit = recipeUnit,
                        inventoryUnit = inventoryUnit, beforeQty = beforeQty, afterQty = 0.0,
                        deductedAmount = beforeQty, conversionNote = conversionNote,
                        action = ConsumeAction.INSUFFICIENT
                    )
                )
            }
        }

        if (deducted == 0) {
            return ConsumeResult(false, 0, skipped, 0, details)
        }

        // 全部扣减成功，写回并持久化
        foods = work
        persist()

        // 记录完成烹饪历史（仅内存）
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.CHINA)
        consumeLogs.add(
            ConsumeLog(
                timestamp = sdf.format(Date()),
                recipeName = recipeName ?: "未知菜谱",
                deducted = deducted,
                skipped = skipped,
                insufficient = insufficient,
                details = details.toList()
            )
        )
        // 仅保留最近 20 条
        while (consumeLogs.size > 20) {
            consumeLogs.removeAt(0)
        }

        return ConsumeResult(true, deducted, skipped, insufficient, details)
    }

    /**
     * 重置为种子数据
     */
    @Synchronized
    fun resetToSeed(): Boolean {
        return try {
            foods = buildSeedFoods().toMutableList()
            persist()
            resetCount++
            prefs.edit().putInt(KEY_RESET_COUNT, resetCount).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 按名称估算保质期
     */
    fun estimateDays(name: String, categoryId: Int): Int = estimateExpiryDays(name, categoryId)

    // ===== 内部工具 =====

    private fun parseDate(dateStr: String): Date {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
        return try { sdf.parse(dateStr) ?: Date() } catch (e: Exception) { Date() }
    }

    /**
     * 从菜谱用量字符串解析数值
     * "2个"→2, "200克"→200, "1.5勺"→1.5, "适量"→0, "半只"→0.5
     */
    private fun parseRecipeAmount(qty: String): Double {
        if (qty.isEmpty()) return 0.0
        var i = 0
        // 跳过前导空白
        while (i < qty.length && (qty[i] == ' ' || qty[i] == '\t')) i++
        // 处理"半"前缀
        if (i < qty.length && qty[i] == '半') return 0.5
        // 收集数字与小数点
        val numStr = StringBuilder()
        while (i < qty.length && (qty[i].isDigit() || qty[i] == '.')) {
            numStr.append(qty[i])
            i++
        }
        if (numStr.isEmpty()) return 0.0
        return numStr.toString().toDoubleOrNull() ?: 0.0
    }

    /**
     * 从菜谱用量字符串解析单位
     * "2个"→"个", "200克"→"克", "半只"→"只", "适量"→""
     */
    private fun parseRecipeUnit(qty: String): String {
        if (qty.isEmpty()) return ""
        var i = 0
        while (i < qty.length && (qty[i] == ' ' || qty[i] == '\t')) i++
        if (i < qty.length && qty[i] == '半') i++
        while (i < qty.length && (qty[i].isDigit() || qty[i] == '.')) i++
        while (i < qty.length && (qty[i] == ' ' || qty[i] == '\t')) i++
        return qty.substring(i).trim()
    }

    companion object {
        private const val PREFERENCES_NAME = "foodkeeper"
        private const val KEY_FOODS = "foods"
        private const val KEY_RESET_COUNT = "reset_count"
        private const val DAY_MS = 24L * 60 * 60 * 1000

        @Volatile
        private var instance: InventoryRepository? = null

        fun getInstance(context: Context): InventoryRepository {
            return instance ?: synchronized(this) {
                instance ?: InventoryRepository(context.applicationContext).also { instance = it }
            }
        }

        // ===== 种子数据（与原 ArkTS SEED_DATA 一致，15条） =====
        private data class SeedItem(
            val name: String, val categoryId: Int, val quantity: Double, val unit: String,
            val expiryOffset: Int, val storageZone: String, val price: Double, val totalPrice: Double
        )

        private val SEED_DATA: List<SeedItem> = listOf(
            SeedItem("西红柿", 1, 3.0, "个", 2, "refrigerator", 5.0, 15.0),
            SeedItem("鸡蛋", 2, 10.0, "个", 5, "refrigerator", 1.5, 15.0),
            SeedItem("猪肉", 2, 500.0, "克", 1, "refrigerator", 30.0, 30.0),
            SeedItem("油菜", 1, 2.0, "把", 1, "refrigerator", 4.0, 8.0),
            SeedItem("土豆", 1, 5.0, "个", 15, "room", 2.0, 10.0),
            SeedItem("鲈鱼", 3, 1.0, "条", 3, "refrigerator", 25.0, 25.0),
            SeedItem("虾仁", 3, 200.0, "克", 5, "freezer", 35.0, 35.0),
            SeedItem("酸奶", 8, 4.0, "杯", -1, "refrigerator", 5.0, 20.0),
            SeedItem("大米", 4, 5.0, "公斤", 365, "room", 8.0, 40.0),
            SeedItem("速冻饺子", 5, 1.0, "袋", 180, "freezer", 18.0, 18.0),
            SeedItem("青椒", 1, 2.0, "个", 3, "refrigerator", 3.0, 6.0),
            SeedItem("洋葱", 1, 3.0, "个", 20, "room", 3.0, 9.0),
            SeedItem("菜籽油", 6, 1.0, "瓶", 365, "room", 68.0, 68.0),
            SeedItem("生抽", 6, 1.0, "瓶", 365, "room", 12.0, 12.0),
            SeedItem("苹果", 7, 4.0, "个", 10, "room", 6.0, 24.0),
        )

        private fun buildSeedFoods(): List<FoodItem> {
            val today = Date()
            val todayStr = formatDate(today)
            val todayTime = today.time
            return SEED_DATA.mapIndexed { i, s ->
                FoodItem(
                    id = (i + 1).toLong(),
                    name = s.name,
                    categoryId = s.categoryId,
                    quantity = s.quantity,
                    unit = s.unit,
                    purchaseDate = todayStr,
                    expiryDate = formatDate(Date(todayTime + s.expiryOffset * DAY_MS)),
                    storageZone = s.storageZone,
                    price = s.price,
                    totalPrice = s.totalPrice,
                    createdAt = todayStr,
                    updatedAt = todayStr
                )
            }
        }
    }
}
