package com.example.foodkeeper.domain

import com.example.foodkeeper.data.model.FoodCardInfo
import com.example.foodkeeper.data.model.FoodItem
import com.example.foodkeeper.data.model.getCategoryColor
import com.example.foodkeeper.data.model.getCategoryLabel
import java.util.Calendar
import java.util.Locale

/**
 * 过期状态计算工具（与原 ArkTS ExpiryHelper.ets 完全一致）
 *
 * 阈值：已过期(<0) / 紧急(0-1天) / 临期(2-3天) / 新鲜(>3天)
 */

const val STATUS_GREEN = "green"
const val STATUS_YELLOW = "yellow"
const val STATUS_RED = "red"
const val STATUS_EXPIRED = "expired"

val STATUS_COLOR = mapOf(
    STATUS_GREEN to "#7C9473",
    STATUS_YELLOW to "#D4A55B",
    STATUS_RED to "#C75D4D",
    STATUS_EXPIRED to "#9A8E80"
)

val STATUS_BG = mapOf(
    STATUS_GREEN to "#EDF1E8",
    STATUS_YELLOW to "#FBF1DE",
    STATUS_RED to "#FAE6E2",
    STATUS_EXPIRED to "#EDEAE5"
)

val STATUS_CLASS = mapOf(
    STATUS_GREEN to "fresh",
    STATUS_YELLOW to "warn",
    STATUS_RED to "danger",
    STATUS_EXPIRED to "expired"
)

/**
 * 获取状态优先级（用于排序：expired < red < yellow < green）
 */
fun getStatusPriority(status: String): Int = when (status) {
    STATUS_EXPIRED -> 0
    STATUS_RED -> 1
    STATUS_YELLOW -> 2
    STATUS_GREEN -> 3
    else -> 99
}

fun getStatusColor(status: String): String = STATUS_COLOR[status] ?: "#9A8E80"
fun getStatusBg(status: String): String = STATUS_BG[status] ?: "#EDEAE5"
fun getStatusClass(status: String): String = STATUS_CLASS[status] ?: "expired"

/**
 * 计算距过期天数（负数表示已过期）
 * 与原 ArkTS daysUntilExpiry 一致：按整天计算，截断小数
 */
fun daysUntilExpiry(expiryDate: String): Int {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val expiry = Calendar.getInstance().apply {
        // 解析 YYYY-MM-DD，格式非法时按今天处理
        val parts = expiryDate.split("-")
        if (parts.size == 3) {
            try {
                set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            } catch (_: NumberFormatException) {
                // 日期格式非法，保持当前时间
            }
        }
    }
    val diff = expiry.timeInMillis - today.timeInMillis
    return (diff / (24 * 60 * 60 * 1000)).toInt()
}

/**
 * 根据天数判定状态（对应原 Qe()）
 * 阈值：已过期(<0) / 紧急(0-1天) / 临期(2-3天) / 新鲜(>3天)
 */
fun getExpiryStatus(days: Int): String = when {
    days < 0 -> STATUS_EXPIRED
    days <= 1 -> STATUS_RED
    days <= 3 -> STATUS_YELLOW
    else -> STATUS_GREEN
}

/**
 * 获取天数描述文本（对应原 Cc().daysText）
 */
fun getDaysText(days: Int): String = when {
    days < 0 -> "已过期 ${abs(days)}天"
    days == 0 -> "今天过期"
    else -> "剩 $days 天"
}

private fun abs(n: Int): Int = if (n < 0) -n else n

/**
 * 计算食材卡片展示信息（对应原 Cc()）
 */
fun getFoodCardInfo(food: FoodItem): FoodCardInfo {
    val days = daysUntilExpiry(food.expiryDate)
    val status = getExpiryStatus(days)
    return FoodCardInfo(
        status = status,
        statusClass = getStatusClass(status),
        statusColor = getStatusColor(status),
        statusBg = getStatusBg(status),
        daysText = getDaysText(days),
        catDot = getCategoryColor(food.categoryId),
        catLabel = getCategoryLabel(food.categoryId)
    )
}

/**
 * 判断是否为临期食材（yellow/red/expired）
 */
fun isExpiring(food: FoodItem): Boolean {
    val status = getExpiryStatus(daysUntilExpiry(food.expiryDate))
    return status == STATUS_YELLOW || status == STATUS_RED || status == STATUS_EXPIRED
}
