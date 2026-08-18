# 食库管家 Foodkeeper - Android 版

由鸿蒙 HarmonyOS（ArkTS + ArkUI）无损迁移至 Android（Kotlin + Jetpack Compose），保留全部本地查询与食材管理功能，数据完全存储在设备本地，不上传任何服务器。

## 技术栈

| 项目 | 鸿蒙原版 | Android 版 |
|------|---------|-----------|
| 语言 | ArkTS | Kotlin |
| UI 框架 | ArkUI 声明式 | Jetpack Compose |
| 本地存储 | @ohos.data.preferences | SharedPreferences + Gson |
| 最低系统 | HarmonyOS 3.1 | Android 8.0 (API 26) |
| 目标系统 | HarmonyOS | Android 14 (API 34) |

## 核心功能

- **食材库存管理**：新增/编辑/删除食材，支持 8 大分类（蔬菜/肉类/海鲜/干货/冷冻/调料/水果/饮料）和 3 种存储分区（冷藏/冷冻/常温）
- **本地食材知识库**：440 条食材知识数据，五级匹配算法（精确→别名→包含匹配），自动填充保质期、存储建议、分类等信息
- **过期智能提醒**：四级状态（新鲜/临期/紧急/已过期），按紧急度排序，一目了然
- **AI 菜谱推荐**：基于库存食材两阶段生成算法（全覆盖临期食材 + 得分填充），349 道菜谱数据库，智能匹配库存与单位换算
- **完成烹饪扣减**：批量扣减库存，支持单位自动换算（克/公斤/斤/个等），记录每条扣减明细
- **数据完全本地化**：所有数据存储在应用沙箱，无网络请求，卸载即清除

## 项目结构

```
app/src/main/
├── assets/                          # 原样迁移的鸿蒙数据文件
│   ├── food_knowledge.ets           # 440 条食材知识库
│   ├── recipe_engine.ets            # 349 道菜谱数据库
│   └── unit_converter.ets           # 单位换算表
├── java/com/example/foodkeeper/
│   ├── FoodkeeperApp.kt             # Application 入口，初始化数据加载
│   ├── MainActivity.kt              # 主界面 + 底部导航（4 Tab）
│   ├── data/
│   │   ├── model/
│   │   │   ├── DataTypes.kt         # 全部数据类定义
│   │   │   └── DataConstants.kt     # 分类/主题色/保质期常量
│   │   ├── parser/
│   │   │   └── EtsDataParser.kt     # .ets 源码解析器（无损迁移核心）
│   │   └── repository/
│   │       └── InventoryRepository.kt  # 库存存储层（SharedPreferences）
│   ├── domain/
│   │   ├── ExpiryHelper.kt          # 过期状态计算
│   │   ├── FoodKnowledgeBase.kt     # 食材知识库查询（五级匹配）
│   │   ├── RecipeEngine.kt          # 菜谱生成引擎（两阶段算法）
│   │   └── UnitConverter.kt         # 单位换算（三层策略）
│   └── ui/
│       ├── theme/Theme.kt           # 主题色与排版
│       ├── components/FoodCard.kt   # 食材卡片组件
│       ├── home/HomeScreen.kt       # 首页（概览统计）
│       ├── inventory/InventoryScreen.kt  # 库存管理
│       ├── recipe/RecipeScreen.kt   # AI 菜谱推荐
│       ├── recipedetail/RecipeDetailActivity.kt  # 菜谱详情与烹饪扣减
│       ├── addfood/AddFoodActivity.kt    # 新增/编辑食材
│       └── settings/SettingsScreen.kt    # 设置与历史记录
└── res/                             # 图标、颜色、主题等资源
```

## 无损迁移说明

### 数据文件迁移

三个核心数据文件（`food_knowledge.ets`、`recipe_engine.ets`、`unit_converter.ets`）原样从鸿蒙项目复制到 `assets/` 目录，**未做任何手动修改**。通过 `EtsDataParser` 在运行时将 ArkTS 语法（单引号、行注释、trailing 逗号、无引号字段名）自动转换为合法 JSON，再用 Gson 反序列化为 Kotlin 对象。

### 算法迁移

| 鸿蒙原版 | Android 版 | 说明 |
|---------|-----------|------|
| `hA()` 五级匹配 | `FoodKnowledgeBase.matchFood()` | 精确→别名→包含匹配 |
| 两阶段菜谱生成 | `RecipeEngine.generateRecipes()` | 临期全覆盖 + 得分填充 |
| `He()/Qe()` 过期计算 | `ExpiryHelper` 系列 | 四级状态阈值完全一致 |
| `convertAmount()` | `UnitConverter.convertAmount()` | 三层换算策略 |
| `Ra()` 种子数据 | `InventoryRepository.SEED_DATA` | 15 条初始数据 |
| `consumeIngredients()` | `InventoryRepository.consumeIngredients()` | 批量扣减 + 单位换算 |

## 构建方法

### 环境要求

- Android Studio Hedgehog (2023.1) 或更高版本
- JDK 17
- Android SDK 34（compileSdk）
- Gradle 8.5（项目自带 wrapper）

### 步骤

1. 用 Android Studio 打开项目根目录
2. 等待 Gradle Sync 完成（首次会自动下载依赖）
3. 点击 Run 或执行命令行构建：

```bash
# Windows
gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

4. 生成的 APK 位于 `app/build/outputs/apk/debug/foodkeeper-debug-1.0.0.apk`

## 本地数据说明

- **存储位置**：`/data/data/com.example.foodkeeper/shared_prefs/foodkeeper.xml`
- **存储方式**：SharedPreferences + JSON 序列化
- **数据安全**：存于应用沙箱，其他应用无法直接访问
- **数据备份**：已配置 `backup_rules.xml`，支持系统级备份
- **卸载行为**：应用卸载后数据随之清除

## 与鸿蒙原版的功能对照

| 功能模块 | 鸿蒙页面 | Android 页面 | 状态 |
|---------|---------|-------------|------|
| 首页概览 | Index.ets | HomeScreen.kt | 已迁移 |
| 库存管理 | InventoryPage.ets | InventoryScreen.kt | 已迁移 |
| 新增食材 | AddFoodPage.ets | AddFoodActivity.kt | 已迁移 |
| 菜谱推荐 | RecipePage.ets | RecipeScreen.kt | 已迁移 |
| 菜谱详情 | RecipeDetailPage.ets | RecipeDetailActivity.kt | 已迁移 |
| 设置页 | SettingsPage.ets | SettingsScreen.kt | 已迁移 |
| 食材卡片 | FoodCard.ets | FoodCard.kt | 已迁移 |
