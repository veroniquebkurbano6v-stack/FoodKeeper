package com.example.foodkeeper.domain

import android.content.Context
import com.example.foodkeeper.data.model.ConversionResult
import com.example.foodkeeper.data.parser.EtsDataParser

/**
 * 食材计量单位转换工具（与原 ArkTS UnitConverter.ets 完全一致）
 *
 * 三层换算策略：
 *  1. 通用重量单位互转（克/公斤/千克/斤/两/g/kg）
 *  2. 食材专属单位 → 克（如 1个鸡蛋≈50克，1袋金针菇≈200克）
 *  3. 通用估算表兜底（个=100, 根=150, 把=150, ...）
 *
 * 数据来源：assets/unit_converter.ets（原样来自鸿蒙项目）
 */
object UnitConverter {

    private const val ASSET_NAME = "unit_converter.ets"

    @Volatile
    private var initialized = false

    // 第一层：通用重量单位 → 克
    private var weightToGram: Map<String, Double> = emptyMap()

    // 第二层：食材专属单位 → 克（嵌套表）
    private var foodUnitToGram: Map<String, Map<String, Double>> = emptyMap()

    // 第三层：通用估算表（兜底）
    private var genericUnitToGram: Map<String, Double> = emptyMap()

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        try {
            val src = context.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
            weightToGram = EtsDataParser.extractStringToNumberMap(src, "WEIGHT_TO_GRAM")
            foodUnitToGram = EtsDataParser.extractNestedMap(src, "FOOD_UNIT_TO_GRAM")
            genericUnitToGram = EtsDataParser.extractStringToNumberMap(src, "GENERIC_UNIT_TO_GRAM")
        } catch (e: Exception) {
            android.util.Log.e("UnitConverter", "初始化失败", e)
        }
        initialized = true
    }

    fun isInitialized(): Boolean = initialized

    /**
     * 单位归一化（处理英文/中文同义单位）
     */
    fun normalizeUnit(unit: String): String {
        val u = unit.trim()
        if (u == "千克" || u == "kg" || u == "KG") return "公斤"
        if (u == "g" || u == "G") return "克"
        return u
    }

    /**
     * 查询某食材某单位对应的克数
     * 优先级：食材专属表 > 通用估算表 > 通用重量表
     */
    private fun getGramPerUnit(foodName: String, unit: String): Double? {
        foodUnitToGram[foodName]?.let { foodTable ->
            foodTable[unit]?.let { return it }
        }
        genericUnitToGram[unit]?.let { return it }
        weightToGram[unit]?.let { return it }
        return null
    }

    /**
     * 判断是否为通用重量单位
     */
    private fun isWeightUnit(unit: String): Boolean = weightToGram.containsKey(unit)

    /**
     * 主转换函数：将 amount 个 fromUnit 转换为 toUnit 对应的数值
     */
    fun convertAmount(amount: Double, fromUnit: String, toUnit: String, foodName: String): ConversionResult {
        if (amount < 0) {
            return ConversionResult(ok = false, value = 0.0, reason = "数量不能为负")
        }
        val from = normalizeUnit(fromUnit)
        val to = normalizeUnit(toUnit)

        // 同单位直接返回
        if (from == to) {
            return ConversionResult(ok = true, value = amount)
        }

        // 策略1：两端都是通用重量单位 → 直接按系数换算（精确）
        val fromWeight = weightToGram[from]
        val toWeight = weightToGram[to]
        if (fromWeight != null && toWeight != null) {
            val v = amount * fromWeight / toWeight
            return ConversionResult(ok = true, value = v)
        }

        // 策略2：食材专属单位换算（经"克"中转）
        val fromGram = getGramPerUnit(foodName, from)
        val toGram = getGramPerUnit(foodName, to)
        if (fromGram != null && toGram != null && toGram > 0) {
            val grams = amount * fromGram
            val v = grams / toGram
            val note = "按 1${from}≈${formatGram(fromGram)}克、1${to}≈${formatGram(toGram)}克 折算"
            return ConversionResult(ok = true, value = v, note = note)
        }

        // 换算失败
        val reason = "「$fromUnit」与「$toUnit」无法自动换算（$foodName 未配置该单位对应克数）"
        return ConversionResult(ok = false, value = 0.0, reason = reason)
    }

    private fun formatGram(v: Double): String {
        return if (v == v.toInt().toDouble()) v.toInt().toString() else v.toString()
    }

    /**
     * 判断两个单位是否可换算（用于 UI 预判，不实际计算）
     */
    fun canConvert(fromUnit: String, toUnit: String, foodName: String): Boolean {
        val from = normalizeUnit(fromUnit)
        val to = normalizeUnit(toUnit)
        if (from == to) return true
        if (isWeightUnit(from) && isWeightUnit(to)) return true
        val fromGram = getGramPerUnit(foodName, from)
        val toGram = getGramPerUnit(foodName, to)
        return fromGram != null && toGram != null
    }
}
