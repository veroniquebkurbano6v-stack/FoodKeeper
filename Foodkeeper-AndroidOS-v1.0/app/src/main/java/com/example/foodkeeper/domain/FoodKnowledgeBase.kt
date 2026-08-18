package com.example.foodkeeper.domain

import android.content.Context
import com.example.foodkeeper.data.model.FoodKnowledge
import com.example.foodkeeper.data.parser.EtsDataParser
import java.util.Locale

/**
 * 食材知识库（本地查询核心）
 *
 * 数据来源：assets/food_knowledge.ets（440 条食材知识，原样来自鸿蒙项目）
 *
 * matchFood 五级匹配算法（与原应用 hA 函数完全一致）：
 *  1. 名称精确匹配（忽略大小写）
 *  2. 别名精确匹配（忽略大小写）
 *  3. 输入长度 ≥ 2 时，名称包含匹配（name.includes(input) || input.includes(name)）
 *  4. 输入长度 ≥ 2 时，别名包含匹配
 *  5. 未命中返回 null
 *
 * 内部使用懒加载索引 nameMap / aliasMap，首次调用 matchFood 时构建。
 */
object FoodKnowledgeBase {

    private const val ASSET_NAME = "food_knowledge.ets"
    private const val ARRAY_NAME = "FOOD_KNOWLEDGE_LIST"

    @Volatile
    private var initialized = false

    private val knowledgeList: MutableList<FoodKnowledge> = mutableListOf()

    // 懒加载索引
    private var nameMap: Map<String, FoodKnowledge>? = null
    private var aliasMap: Map<String, FoodKnowledge>? = null

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        try {
            val src = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            val list = EtsDataParser.parseArray<FoodKnowledge>(src, ARRAY_NAME)
            knowledgeList.clear()
            knowledgeList.addAll(list)
        } catch (e: Exception) {
            android.util.Log.e("FoodKnowledgeBase", "初始化失败", e)
        }
        initialized = true
    }

    fun isInitialized(): Boolean = initialized

    fun getAll(): List<FoodKnowledge> = synchronized(this) {
        knowledgeList.toList()
    }

    fun size(): Int = knowledgeList.size

    /**
     * 五级匹配算法（与原 ArkTS matchFood 完全一致）
     */
    fun matchFood(input: String?): FoodKnowledge? {
        if (!initialized || input.isNullOrBlank()) return null
        val raw = input.trim()
        if (raw.isEmpty()) return null
        val lower = raw.lowercase(Locale.getDefault())

        // 懒加载索引
        ensureIndex()

        // 第1级：名称精确匹配（忽略大小写）
        nameMap?.get(lower)?.let { return it }

        // 第2级：别名精确匹配（忽略大小写）
        aliasMap?.get(lower)?.let { return it }

        // 输入长度 < 2 时不做模糊匹配，避免单字误匹配
        if (raw.length < 2) return null

        // 第3级：名称包含匹配（双向）
        for (item in knowledgeList) {
            val n = item.name.lowercase(Locale.getDefault())
            if (n.contains(lower) || lower.contains(n)) return item
        }

        // 第4级：别名包含匹配（双向）
        for (item in knowledgeList) {
            for (alias in item.aliases) {
                val a = alias.lowercase(Locale.getDefault())
                if (a.contains(lower) || lower.contains(a)) return item
            }
        }

        // 第5级：未命中
        return null
    }

    @Synchronized
    private fun ensureIndex() {
        if (nameMap != null && aliasMap != null) return
        val nMap = mutableMapOf<String, FoodKnowledge>()
        val aMap = mutableMapOf<String, FoodKnowledge>()
        for (item in knowledgeList) {
            nMap[item.name.lowercase(Locale.getDefault())] = item
            for (alias in item.aliases) {
                aMap[alias.lowercase(Locale.getDefault())] = item
            }
        }
        nameMap = nMap
        aliasMap = aMap
    }
}
