package com.example.foodkeeper.domain

import android.content.Context
import com.example.foodkeeper.data.model.DisplayRecipe
import com.example.foodkeeper.data.model.FoodItem
import com.example.foodkeeper.data.model.MatchedIngredient
import com.example.foodkeeper.data.model.RecipeGenOptions
import com.example.foodkeeper.data.model.RecipeTemplate
import com.example.foodkeeper.data.parser.EtsDataParser
import kotlin.math.abs

/**
 * 智能菜谱生成引擎（与原 ArkTS RecipeEngine.ets 完全一致）
 *
 * 数据来源：assets/recipe_engine.ets（349 道菜谱，约 600KB，原样来自鸿蒙项目）
 *
 * generateRecipes 两阶段算法：
 *   阶段1（全覆盖临期食材）：遍历所有临期食材（按紧急度升序），
 *     为每个尚未被覆盖的临期食材挑选得分最高且未选中的菜谱。
 *   阶段2（填充剩余名额）：从剩余高分菜谱中按得分降序补充至 5-7 道。
 *
 * 评分规则 scoreRecipe：
 *   库存拥有该食材 +5；已过期 +8；紧急临期(0-1天) +15；临期(2-3天) +10；
 *   库存没有 -2；使用 2+ 临期食材额外 +10；收藏 +5；上次已生成 -10000。
 */
object RecipeEngine {

    private const val ASSET_NAME = "recipe_engine.ets"
    private const val ARRAY_NAME = "RECIPE_DATABASE"

    @Volatile
    private var initialized = false

    private val recipeDatabase: MutableList<RecipeTemplate> = mutableListOf()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        try {
            val src = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val list = EtsDataParser.parseArray<RecipeTemplate>(src, ARRAY_NAME)
            recipeDatabase.clear()
            recipeDatabase.addAll(list)
        } catch (e: Exception) {
            android.util.Log.e("RecipeEngine", "初始化失败", e)
        }
        initialized = true
    }

    fun isInitialized(): Boolean = initialized

    fun size(): Int = recipeDatabase.size

    fun getAll(): List<RecipeTemplate> = synchronized(this) { recipeDatabase.toList() }

    /**
     * 查找库存中某食材的剩余天数；-999 表示未找到
     */
    private fun findFoodDays(foods: List<FoodItem>, name: String): Int {
        for (f in foods) {
            if (f.name == name && f.quantity > 0) {
                return daysUntilExpiry(f.expiryDate)
            }
        }
        return -999
    }

    /**
     * 判断是否临期（剩余 0-3 天，含已过期）
     */
    private fun checkExpiring(days: Int): Boolean = days <= 3

    /**
     * 获取库存中所有临期食材名称（去重），按紧急程度升序（最紧急在前）
     */
    private fun getExpiringIngredientNames(foods: List<FoodItem>): List<String> {
        val names = mutableListOf<String>()
        val daysArr = mutableListOf<Int>()
        for (f in foods) {
            if (f.quantity <= 0) continue
            val days = daysUntilExpiry(f.expiryDate)
            if (days <= 3) {
                val idx = names.indexOf(f.name)
                if (idx == -1) {
                    names.add(f.name)
                    daysArr.add(days)
                } else if (days < daysArr[idx]) {
                    daysArr[idx] = days
                }
            }
        }
        // 按紧急程度升序排序（days 越小越紧急）
        // 简单冒泡排序（保持与原 ArkTS 一致的行为）
        for (i in daysArr.indices) {
            for (j in i + 1 until daysArr.size) {
                if (daysArr[j] < daysArr[i]) {
                    val tmpD = daysArr[i]; daysArr[i] = daysArr[j]; daysArr[j] = tmpD
                    val tmpN = names[i]; names[i] = names[j]; names[j] = tmpN
                }
            }
        }
        return names
    }

    /**
     * 判断菜谱是否使用了指定食材
     */
    private fun recipeUsesIngredient(recipe: DisplayRecipe, ingredientName: String): Boolean {
        return recipe.ingredients.any { it.name == ingredientName }
    }

    // ===== 评分结果 =====
    private data class RecipeScore(
        val score: Int,
        val ings: List<MatchedIngredient>,
        val expiringCount: Int,
        val haveCount: Int,
        val needBuyNames: List<String>,
    )

    /**
     * 计算单道菜谱的优先级得分
     */
    private fun scoreRecipe(recipe: RecipeTemplate, foods: List<FoodItem>): RecipeScore {
        var score = 0
        var expiringCount = 0
        var haveCount = 0
        val ings = mutableListOf<MatchedIngredient>()
        val needBuyNames = mutableListOf<String>()

        for (ing in recipe.ingredients) {
            val days = findFoodDays(foods, ing.name)
            if (days != -999) {
                val expiring = checkExpiring(days)
                ings.add(
                    MatchedIngredient(
                        name = ing.name,
                        quantity = ing.quantity,
                        haveIt = true,
                        days = days,
                        isExpiring = expiring
                    )
                )
                haveCount++
                score += 5  // 拥有该食材
                if (days < 0) {
                    score += 8  // 已过期
                    expiringCount++
                } else if (days <= 1) {
                    score += 15  // 紧急临期
                    expiringCount++
                } else if (days <= 3) {
                    score += 10  // 临期
                    expiringCount++
                }
            } else {
                ings.add(
                    MatchedIngredient(
                        name = ing.name,
                        quantity = ing.quantity,
                        haveIt = false,
                        days = 0,
                        isExpiring = false
                    )
                )
                needBuyNames.add(ing.name)
                score -= 2  // 需购买
            }
        }

        // 使用 2 个及以上临期食材，额外加成
        if (expiringCount >= 2) {
            score += 10
        }

        return RecipeScore(score, ings, expiringCount, haveCount, needBuyNames)
    }

    /**
     * 生成菜谱（返回 5-7 道，临期食材全覆盖 + 用户偏好）
     */
    fun generateRecipes(foods: List<FoodItem>, options: RecipeGenOptions? = null): List<DisplayRecipe> {
        val scored = mutableListOf<DisplayRecipe>()
        val excludeSet = (options?.excludeNames ?: emptyList()).toSet()
        val favoriteSet = (options?.favoriteNames ?: emptyList()).toSet()

        for (t in recipeDatabase) {
            val result = scoreRecipe(t, foods)
            // 上次已出现过的菜谱：施加大惩罚（-10000），使其排到末尾
            val penalty = if (excludeSet.contains(t.name)) -10000 else 0
            // 收藏菜谱加成
            val favoriteBonus = if (favoriteSet.contains(t.name)) 5 else 0
            val recipe = DisplayRecipe(
                name = t.name,
                time = t.time,
                calories = t.calories,
                caloriesNum = t.caloriesNum,
                difficulty = t.difficulty,
                category = t.category,
                ingredients = result.ings,
                stepList = t.stepList,
                steps = t.stepList.size,
                needBuyNames = result.needBuyNames,
                haveCount = result.haveCount,
                totalCount = t.ingredients.size,
                expiringCount = result.expiringCount,
                priorityScore = result.score + Math.random() * 3 + penalty + favoriteBonus
            )
            scored.add(recipe)
        }

        // 按得分降序排序
        scored.sortByDescending { it.priorityScore }

        val count = 5 + (Math.random() * 3).toInt()  // 5, 6, 或 7
        val maxCount = minOf(count, scored.size)

        // ===== 阶段1：全覆盖临期食材 =====
        val expiringNames = getExpiringIngredientNames(foods)
        val selected = mutableListOf<DisplayRecipe>()
        val selectedNames = mutableSetOf<String>()
        val coveredExpiring = mutableSetOf<String>()

        for (name in expiringNames) {
            if (coveredExpiring.contains(name)) continue
            // 在已排序的候选菜谱中查找使用了该临期食材且未选中的最佳菜谱
            var picked: DisplayRecipe? = null
            for (candidate in scored) {
                if (selectedNames.contains(candidate.name)) continue
                if (recipeUsesIngredient(candidate, name)) {
                    picked = candidate
                    break
                }
            }
            picked?.let { p ->
                selected.add(p)
                selectedNames.add(p.name)
                // 标记该菜谱包含的所有临期食材为已覆盖
                for (ing in p.ingredients) {
                    if (ing.isExpiring) {
                        coveredExpiring.add(ing.name)
                    }
                }
                if (selected.size >= maxCount) return@let
            }
            if (selected.size >= maxCount) break
        }

        // ===== 阶段2：填充剩余名额 =====
        if (selected.size < maxCount) {
            for (recipe in scored) {
                if (selected.size >= maxCount) break
                if (selectedNames.contains(recipe.name)) continue
                selected.add(recipe)
                selectedNames.add(recipe.name)
            }
        }

        // 按得分降序排序最终结果
        selected.sortByDescending { it.priorityScore }

        return selected
    }
}

/**
 * 菜谱详情页数据传递 Holder（对应原 ArkTS RecipeHolder）
 *
 * 由于 DisplayRecipe 含嵌套数组，不便通过 Intent 传递，使用单例 Holder 中转
 * 同时记录最近完成烹饪的菜谱名，供 RecipePage 返回后标记 completedSet
 */
object RecipeHolder {
    @Volatile
    private var current: DisplayRecipe? = null

    @Volatile
    private var lastCompleted: String? = null

    fun set(recipe: DisplayRecipe) { current = recipe }
    fun get(): DisplayRecipe? = current
    fun clear() { current = null }

    fun setLastCompleted(name: String) { lastCompleted = name }
    fun getLastCompleted(): String? = lastCompleted
    fun clearLastCompleted() { lastCompleted = null }
}
