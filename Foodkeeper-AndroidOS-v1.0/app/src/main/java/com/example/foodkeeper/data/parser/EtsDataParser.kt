package com.example.foodkeeper.data.parser

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * .ets 源码数据解析器
 *
 * 设计目标：将 HarmonyOS ArkTS 源文件（.ets）中的 `export const XXX: YYY[] = [...]`
 * 数据数组原样读取并转换为 Kotlin 对象，**完全无损**，避免手动重写 440+349 条数据出错。
 *
 * .ets 数据语法与 JSON 的差异：
 *  1. 单引号字符串（'白菜'）→ 需转为双引号
 *  2. 行注释 `// ...` → 需去除
 *  3. trailing 逗号（`{...,}` / `[...,]`）→ JSON 不允许，需去除
 *  4. 字段名无引号（`name:` 而非 `"name":`）→ 需加引号
 *  5. 数字、布尔、null 与 JSON 一致
 *
 * 实测原 .ets 文件中字符串内容均为中文食材名/步骤说明，不含单引号字符，可安全全局替换。
 */
object EtsDataParser {

    val externalGson: Gson = Gson()
    internal val gson: Gson = externalGson

    /**
     * 从 .ets 源码中提取指定数组变量并解析为指定类型列表
     *
     * @param src .ets 文件全部内容
     * @param arrayName 数组变量名（如 "FOOD_KNOWLEDGE_LIST"）
     * @return 解析后的对象列表
     */
    inline fun <reified T> parseArray(src: String, arrayName: String): List<T> {
        val json = extractJsonArray(src, arrayName)
        android.util.Log.d("EtsDataParser", "parseArray: arrayName=$arrayName, json长度=${json.length}, json前100字符=${json.take(100)}")
        val type = object : TypeToken<List<T>>() {}.type
        return try {
            externalGson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("EtsDataParser", "JSON解析失败: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 提取数组内容并转为 JSON 字符串
     */
    fun extractJsonArray(src: String, arrayName: String): String {
        // 1. 去除行注释
        val cleaned = stripLineComments(src)

        // 2. 定位数组起始 `[`
        val startIdx = locateArrayStart(cleaned, arrayName)
        if (startIdx < 0) {
            throw IllegalArgumentException("未在 .ets 源码中找到数组变量：$arrayName")
        }

        // 3. 从 `[` 开始匹配到对应的 `]`（处理嵌套 [] 和 {}，以及字符串边界）
        val endIdx = findMatchingBracket(cleaned, startIdx, '[', ']')
        if (endIdx < 0) {
            throw IllegalArgumentException("数组 $arrayName 的括号未闭合")
        }

        // 4. 提取数组内容并清洗为合法 JSON
        val raw = cleaned.substring(startIdx, endIdx + 1)
        return toJsonArray(raw)
    }

    /**
     * 去除 `// ...` 行注释
     * 注意：原 .ets 数据文件中字符串内不含 `//`，可简单按行处理
     */
    private fun stripLineComments(src: String): String {
        val sb = StringBuilder(src.length)
        src.split('\n').forEach { line ->
            val idx = line.indexOf("//")
            val kept = if (idx >= 0) line.substring(0, idx) else line
            sb.append(kept).append('\n')
        }
        return sb.toString()
    }

    /**
     * 定位 `arrayName ... = [` 中的数组赋值 `[` 位置
     * 支持形如：`export const FOOD_KNOWLEDGE_LIST: FoodKnowledge[] = [`
     *
     * 关键：必须跳过类型声明中的 `Type[]` 方括号对，只匹配 `= [` 赋值后的 `[`
     */
    private fun locateArrayStart(src: String, arrayName: String): Int {
        // 找到变量名位置
        val nameIdx = src.indexOf(arrayName)
        if (nameIdx < 0) return -1

        // 从变量名后开始扫描，跳过类型声明中的成对 `[]`，找到 `=` 后的 `[`
        var i = nameIdx + arrayName.length
        while (i < src.length) {
            val c = src[i]
            if (c == '=') {
                // 找到赋值号，继续往后找 `[`
                val bracketIdx = src.indexOf('[', i)
                if (bracketIdx < 0) return -1
                // 确保中间没有 `;`（避免越界到下一条语句）
                val between = src.substring(i + 1, bracketIdx)
                if (between.contains(';')) return -1
                return bracketIdx
            }
            // 跳过类型声明中的 `Type[]`：遇到 `[` 且后面紧接 `]` 就跳过
            if (c == '[') {
                val next = src.indexOf(']', i)
                if (next == i + 1) {
                    i = next + 1
                    continue
                }
            }
            i++
        }
        return -1
    }

    /**
     * 从 startIdx 位置（必须是 openCh）开始，找到匹配的 closeCh 位置
     * 处理嵌套 [] 和 {}，以及单引号/双引号字符串
     */
    private fun findMatchingBracket(src: String, startIdx: Int, openCh: Char, closeCh: Char): Int {
        var depth = 0
        var i = startIdx
        var inStr = false
        var strChar = ' '
        while (i < src.length) {
            val c = src[i]
            if (inStr) {
                if (c == '\\') {
                    i += 2  // 跳过转义字符
                    continue
                }
                if (c == strChar) inStr = false
            } else {
                when (c) {
                    '\'', '"' -> { inStr = true; strChar = c }
                    '{', '[' -> depth++
                    '}', ']' -> {
                        depth--
                        if (depth == 0) return i
                    }
                }
            }
            i++
        }
        return -1
    }

    /**
     * 把 .ets 数组/对象字面量转为合法 JSON 字符串
     *
     * 核心难点：.ets 使用单引号字符串，字符串内可能包含双引号（如 "滋啦"）。
     * 简单的 replace("'", "\"") 会破坏含双引号的字符串。
     * 必须逐字符扫描，正确处理字符串边界与转义。
     */
    private fun convertEtsToJson(raw: String): String {
        val sb = StringBuilder(raw.length * 2)
        var i = 0
        var inStr = false

        while (i < raw.length) {
            val c = raw[i]

            if (inStr) {
                // 字符串内部：处理转义序列
                if (c == '\\' && i + 1 < raw.length) {
                    val next = raw[i + 1]
                    when (next) {
                        '\'' -> { sb.append("'"); i += 2; continue }      // \' → ' (JSON 不需要转义单引号)
                        '"' -> { sb.append("\\\""); i += 2; continue }    // \" → \" (保持转义)
                        '\\' -> { sb.append("\\\\"); i += 2; continue }   // \\ → \\ (保持转义)
                        'n' -> { sb.append("\\n"); i += 2; continue }
                        't' -> { sb.append("\\t"); i += 2; continue }
                        'r' -> { sb.append("\\r"); i += 2; continue }
                        else -> { sb.append('\\').append(next); i += 2; continue }
                    }
                }
                if (c == '\'') {
                    // 单引号结束 → 输出双引号
                    inStr = false
                    sb.append('"')
                    i++
                    continue
                }
                if (c == '"') {
                    // 字符串内的双引号 → 必须转义为 \"
                    sb.append("\\\"")
                    i++
                    continue
                }
                if (c == '\n') { sb.append("\\n"); i++; continue }
                if (c == '\r') { sb.append("\\r"); i++; continue }
                if (c == '\t') { sb.append("\\t"); i++; continue }
                sb.append(c)
                i++
            } else {
                // 字符串外部
                if (c == '\'' || c == '"') {
                    inStr = true
                    sb.append('"')  // JSON 字符串统一用双引号开头
                    i++
                    continue
                }
                sb.append(c)
                i++
            }
        }

        // 字段名加引号：匹配 `{` 或 `,` 后（允许空白/换行）的 `fieldName:` → `"fieldName":`
        val step1 = sb.toString().replace(Regex("([\\{,]\\s*)(\\w+)\\s*:"), "$1\"$2\":")
        // 去除 trailing 逗号
        val step2 = step1
            .replace(Regex(",\\s*\\}"), "}")
            .replace(Regex(",\\s*\\]"), "]")
        return step2
    }

    private fun toJsonArray(rawArray: String): String = convertEtsToJson(rawArray)

    /**
     * 提取 Record<string, number> 类型的字典表
     * 例如 WEIGHT_TO_GRAM: Record<string, number> = { '克': 1, '公斤': 1000, ... }
     */
    fun extractStringToNumberMap(src: String, varName: String): Map<String, Double> {
        val cleaned = stripLineComments(src)
        val startIdx = locateObjectStart(cleaned, varName)
        if (startIdx < 0) return emptyMap()
        val endIdx = findMatchingBracket(cleaned, startIdx, '{', '}')
        if (endIdx < 0) return emptyMap()
        val raw = cleaned.substring(startIdx, endIdx + 1)
        val json = toJsonObject(raw)
        val type = object : TypeToken<Map<String, Double>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    /**
     * 提取 Record<string, Record<string, number>> 类型的嵌套字典表
     * 例如 FOOD_UNIT_TO_GRAM: Record<string, Record<string, number>> = { '西红柿': { '个': 150 }, ... }
     */
    fun extractNestedMap(src: String, varName: String): Map<String, Map<String, Double>> {
        val cleaned = stripLineComments(src)
        val startIdx = locateObjectStart(cleaned, varName)
        if (startIdx < 0) return emptyMap()
        val endIdx = findMatchingBracket(cleaned, startIdx, '{', '}')
        if (endIdx < 0) return emptyMap()
        val raw = cleaned.substring(startIdx, endIdx + 1)
        val json = toJsonObject(raw)
        val type = object : TypeToken<Map<String, Map<String, Double>>>() {}.type
        return gson.fromJson(json, type) ?: emptyMap()
    }

    private fun locateObjectStart(src: String, varName: String): Int {
        val nameIdx = src.indexOf(varName)
        if (nameIdx < 0) return -1
        val braceIdx = src.indexOf('{', nameIdx)
        if (braceIdx < 0) return -1
        return braceIdx
    }

    private fun toJsonObject(rawObj: String): String = convertEtsToJson(rawObj)
}
