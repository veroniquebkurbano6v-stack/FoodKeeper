package com.example.foodkeeper.data.model

// ===== 八大分类（对应 ArkTS CATEGORIES） =====
val CATEGORIES: List<CategoryInfo> = listOf(
    CategoryInfo("vegetable", "蔬菜", 1, "#7C9473", "#EDF1E8"),
    CategoryInfo("meat", "肉类", 2, "#C97B6E", "#F5E6E2"),
    CategoryInfo("seafood", "海鲜", 3, "#6E8CA8", "#E8EEF4"),
    CategoryInfo("dried", "干货", 4, "#A88B6A", "#F0E9DC"),
    CategoryInfo("frozen", "冷冻", 5, "#8BA0B0", "#E9EEF1"),
    CategoryInfo("seasoning", "调料", 6, "#B5765A", "#F0E2DC"),
    CategoryInfo("fruit", "水果", 7, "#C75D4D", "#F5E3DF"),
    CategoryInfo("drink", "饮料", 8, "#9CB088", "#EDF0E4"),
)

// ===== 三大存储分区 =====
val STORAGE_ZONES: List<StorageZoneInfo> = listOf(
    StorageZoneInfo("refrigerator", "冷藏室"),
    StorageZoneInfo("freezer", "冷冻层"),
    StorageZoneInfo("room", "常温"),
)

// ===== 常用单位 =====
val UNITS: List<String> = listOf("个", "克", "公斤", "把", "条", "瓶", "袋", "桶", "杯", "颗", "块", "盒")

// ===== 默认保质期表（按名称查找） =====
val DEFAULT_EXPIRY_DAYS: Map<String, Int> = mapOf(
    // 蔬菜
    "西红柿" to 7, "黄瓜" to 7, "土豆" to 15, "胡萝卜" to 14, "青椒" to 5, "青菜" to 3, "白菜" to 7,
    "洋葱" to 30, "大蒜" to 30, "生姜" to 14, "西兰花" to 5, "芹菜" to 7, "蘑菇" to 3, "茄子" to 7,
    "豆角" to 5, "菠菜" to 3, "生菜" to 2, "番茄" to 7, "冬瓜" to 14, "南瓜" to 30,
    // 肉类
    "猪肉" to 5, "牛肉" to 7, "羊肉" to 5, "鸡肉" to 5, "鸡蛋" to 14, "鸭蛋" to 14,
    "牛奶" to 7, "酸奶" to 7, "奶酪" to 30, "黄油" to 90,
    // 海鲜
    "鱼" to 3, "虾" to 3, "蟹" to 3, "贝类" to 2, "鱿鱼" to 3, "三文鱼" to 3, "带鱼" to 3, "虾仁" to 7, "扇贝" to 2, "生蚝" to 1,
    // 干货
    "大米" to 365, "面粉" to 180, "酱油" to 365, "醋" to 365, "盐" to 365, "糖" to 365,
    "食用油" to 365, "花椒" to 365, "八角" to 365, "桂皮" to 365, "干辣椒" to 365, "豆瓣酱" to 180,
    "豆腐乳" to 180, "木耳" to 365, "香菇" to 365,
    // 冷冻
    "速冻水饺" to 180, "速冻包子" to 180, "速冻汤圆" to 180, "速冻馄饨" to 180, "冰淇淋" to 180, "冻鱼" to 90, "冻肉" to 90,
    // 调料
    "料酒" to 365, "蚝油" to 180, "番茄酱" to 180, "沙拉酱" to 180, "芝麻油" to 365, "孜然" to 365, "五香粉" to 365,
    // 水果
    "苹果" to 10, "香蕉" to 5, "橙子" to 14, "柠檬" to 20, "葡萄" to 5, "草莓" to 3, "西瓜" to 3, "梨" to 10, "桃子" to 5,
    // 饮料
    "矿泉水" to 365, "可乐" to 365, "果汁" to 30, "啤酒" to 180, "功能饮料" to 365, "茶饮料" to 180
)

// ===== 分类默认保质期 =====
val CATEGORY_DEFAULT_DAYS: Map<Int, Int> = mapOf(
    1 to 7,   // 蔬菜
    2 to 5,   // 肉类
    3 to 3,   // 海鲜
    4 to 180, // 干货
    5 to 90,  // 冷冻
    6 to 365, // 调料
    7 to 10,  // 水果
    8 to 30,  // 饮料
)

// ===== 主题色（对应 ArkTS THEME） =====
object Theme {
    const val BG = "#FAF6EF"
    const val CARD = "#FFFEFB"
    const val SECTION = "#F5EFE4"
    const val PRIMARY = "#7C9473"
    const val PRIMARY_DARK = "#5E7A56"
    const val PRIMARY_BG = "#EDF1E8"
    const val ACCENT = "#C97B5C"
    const val ACCENT_BG = "#F7E8DF"
    const val COLD = "#6E8CA8"
    const val COLD_BG = "#E8EEF4"
    const val TEXT = "#3D352E"
    const val TEXT2 = "#8B7E6F"
    const val TEXT3 = "#B5A99A"
    const val BORDER = "#ECE4D6"
    const val FRESH = "#7C9473"
    const val FRESH_BG = "#EDF1E8"
    const val WARN = "#D4A55B"
    const val WARN_BG = "#FBF1DE"
    const val DANGER = "#C75D4D"
    const val DANGER_BG = "#FAE6E2"
    const val EXPIRED = "#9A8E80"
    const val EXPIRED_BG = "#EDEAE5"
}

// ===== 查询函数 =====
fun getCategoryByValue(value: String): CategoryInfo? = CATEGORIES.find { it.value == value }
fun getCategoryById(id: Int): CategoryInfo? = CATEGORIES.find { it.id == id }
fun getCategoryColor(categoryId: Int): String = getCategoryById(categoryId)?.color ?: "#B5A99A"
fun getCategoryLabel(categoryId: Int): String = getCategoryById(categoryId)?.label ?: "其他"
fun getStorageZoneLabel(zone: String): String = STORAGE_ZONES.find { it.value == zone }?.label ?: zone

// ===== 格式化日期为 YYYY-MM-DD =====
fun formatDate(date: java.util.Date): String {
    val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.CHINA)
    return sdf.format(date)
}

// ===== 估算保质期天数：优先按名称，其次按分类 =====
fun estimateExpiryDays(name: String, categoryId: Int): Int {
    DEFAULT_EXPIRY_DAYS[name]?.let { return it }
    return CATEGORY_DEFAULT_DAYS[categoryId] ?: 7
}
