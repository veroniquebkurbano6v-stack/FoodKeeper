# 实现计划：我的菜谱收藏 + AI 菜谱缓存 + 图标系统修复

## Context（背景与动机）

当前 `Recipes.vue` 存在三个痛点：
1. AI 生成的菜谱无法保存，用户刷新即丢失，缺少「我的菜谱」收藏能力。
2. 每次进入页面都自动调用 AI 接口，既浪费额度又让用户等待；无本地缓存机制。
3. 图标系统混乱：经核实，项目中使用的 **7 个 Vant 图标名在 Vant 4.8 中并不存在**（`goods-o`、`package-o`、`alert-circle-o`、`chef-hat-o`、`arrow-down-o`、`list-o`、`trash-o`），导致首页快捷操作等多个位置图标空白不显示；`Recipes.vue` 还用 18 处 emoji 替代图标，跨平台渲染不一致。

本方案在现有「纯前端 + localStorage」架构下完成：新增独立菜谱 store、重构 Recipes.vue 的 tab 与缓存逻辑、全站修复并补齐图标。已与用户确认：持久化用 localStorage；「我的菜谱」入口放在 Recipes.vue 顶部 Tab。

---

## 第一部分：我的菜谱（收藏功能）

### 1.1 新建独立 store `src/stores/recipes.js`
不扩展 `inventory.js`（食材域与菜谱域分离，避免 `saveData()` 连带写入耦合）。复用 inventory store 的 `ref + computed + loadData/saveData` 模式。

- localStorage key：`recipeFavorites`（收藏数组）、`recipeAi.cache`（AI 缓存对象，与既有 `recipeAi.provider/apiKey/customEndpoint` 同命名空间）
- state：`favorites = ref([])`、`cache = ref(null)`
- computed：`favoriteSignatures`（Set，O(1) 查重）、`favoriteCount`、`hasCache`、`cacheTimestamp`
- methods：`loadData()`、`saveFavorites()`、`saveCache(payload)`、`clearCache()`、`isFavorite(recipe)`、`addFavorite(recipe)`（已存在返回 false）、`removeFavoriteByRecipe(recipe)`、`getFavoriteRecipes()`

### 1.2 稳定菜谱签名（去重身份）
当前 `recipeAi.js` 的 `normalizeRecipe` 用 `recipe-${Date.now()}-${index}` 作 id，重新生成后变化，无法去重。

- 修改 `src/utils/recipeAi.js` 的 `normalizeRecipe`：新增 `signature` 字段 = `${name.trim().toLowerCase()}::${type}`
- 新增导出函数 `getRecipeSignature(recipe)`（优先返回 `recipe.signature`，否则按规则推导）
- `Recipes.vue` 卡片 `:key` 改为 `recipe.signature || recipe.id || idx`

### 1.3 Recipes.vue 改造
- 顶部 `van-tabs` 新增第 5 个 tab：「我的菜谱」(name=`favorites`)
- `filteredRecipes` 计算属性分支：`favorites` → 返回 `recipeStore.getFavoriteRecipes()`；其余维持原筛选逻辑
- 卡片模板（`.recipe-card-header` 内）新增收藏按钮，`@click.stop` 阻止冒泡：
  - 未收藏：`<van-icon name="bookmark-o" />` + 「收藏」，色 `var(--sk-text-3)`
  - 已收藏：`<van-icon name="bookmark" />` + 「已收藏」，色 `var(--sk-accent)`
  - 点击切换 + `showToast` 反馈
- 详情弹层底部新增 sticky 操作栏：未收藏「添加到我的菜谱」(icon=`bookmark-o`) / 已收藏「已收藏·点击移除」(icon=`bookmark`)
- 收藏 tab 空状态：`<van-empty>` + 引导文案 + 切回「全部」按钮
- 区块守卫：`recipe-header`/`fallback-banner`/`generate-bar`/缓存时间条 仅在 `activeTab !== 'favorites'` 时显示

---

## 第二部分：AI 菜谱缓存机制

### 2.1 缓存放 recipes store
`cache` state + `saveCache`/`clearCache`/`hasCache`/`cacheTimestamp`，响应式显示时间戳。

### 2.2 onMounted 行为变更（`Recipes.vue`）
当前：`loadData()` + 自动 `handleGenerate()`。
改为：`store.loadData()` + `recipeStore.loadData()`；若 `hasCache` 则 `recipes.value = cache.recipes`、`lastResult` 还原；**无缓存时不自动调 AI**，进入空状态。

### 2.3 handleGenerate 改造
成功后 `recipeStore.saveCache({ recipes, timestamp: Date.now(), useFallback, message })`。失败分支：当 `hasCache` 时不清空 `recipes.value`（保留上次缓存供继续查看），仅 toast。

### 2.4 按钮与时间戳
- 无缓存空状态按钮文案「生成菜谱」；列表底部按钮「重新生成菜谱」加 `icon="replay"`
- `recipe-tabs` 下方新增 `cache-info` 条（仅 AI tab 且有缓存时显示）：`<van-icon name="clock-o" /> 上次生成：{{ formatRelativeTime(recipeStore.cacheTimestamp) }}`

### 2.5 新增工具函数 `src/utils/index.js`
`formatRelativeTime(timestamp)`：<60s「刚刚」/<60min「x 分钟前」/<24h「x 小时前」/否则「x 天前」/null→''。

缓存写入 localStorage，刷新后读回，满足「会话期间有效」（且跨会话保留，更优）。

---

## 第三部分：图标系统修复与补齐

### 3.1 修复 7 个无效图标（已逐一核对 Vant 4.8 icon CSS）

| 旧（无效） | 新（有效） | 位置 |
|-----------|-----------|------|
| `goods-o` | `apps-o` | Home stats-grid「食材品类」 |
| `goods-o` | `shop-o` | Home quick-actions「库存管理」、App tabbar「库存」 |
| `package-o` | `bag-o` | Home stats-grid「总数量」、Reports stat-icon「库存总量」 |
| `alert-circle-o` | `warning-o` | Home stats-grid「已过期」、Reports stat-icon「临期预警」 |
| `chef-hat-o` | `notes-o` | Home quick-actions「AI菜谱」、App tabbar「菜谱」 |
| `chef-hat-o` | `fire-o` | Logs `getLogIcon.cook` |
| `arrow-down-o` | `arrow-down` | Home quick-actions「食材出库」 |
| `list-o` | `records-o` | Home quick-actions「出库日志」 |
| `trash-o` | `delete-o` | Reports stat-icon「本月报废」、Logs `getLogIcon.discard` |
| `delete-o`→`clear` | `clear` | Logs `getLogIcon.clear`（避免与 discard 冲突） |

### 3.2 Recipes.vue 18 处 emoji → van-icon
⏱→`clock-o`、🔥→`fire-o`、📊→`chart-trending-o`、✅→`checked`、🛒→`shopping-cart-o`、📦→`bag-o`、👨‍🍳→`todo-list-o`、💡→`bulb-o`、🔧→`setting-o`、🔒→`shield-o`。`getUrgencyText` 拆为 `getUrgencyIcon` + 纯文本。行内 size=14，h4 标题 size=16。

### 3.3 补齐缺失图标
- **Home.vue** 2 个 h2：临期提醒→`warning-o`、快捷操作→`apps-o`
- **Reports.vue** 3 个 h2：分类库存分布→`chart-trending-o`、库存明细→`description-o`、近期出库记录→`records-o`
- **ShoppingPlan.vue** 2 个 h2：缺料清单→`warning-o`、采购清单→`shopping-cart-o`
- **Settings.vue** 5 个 van-cell 加 `icon` prop：数据管理→`records-o`、导出数据→`description-o`、重置数据→`replay`、关于应用→`info-o`、隐私设置→`shield-o`
- **AddFood.vue / EditFood.vue** 日期字段加 `#left-icon`：采购日期/保质期截止→`calendar-o`、保质期天数→`clock-o`
- **Expiring.vue** stats-bar 3 项：即将过期→`fire-o`、临期→`clock-o`、已过期→`warning-o`
- **Outbound.vue**：「确认消耗」按钮加 `icon="fire-o"`（轻量）

### 3.4 风格约束
统一线框 `-o` 系；实心态仅用于「已激活」语义（bookmark/star/fire）。行内 size=14、h4 size=16、stat size=20-24，`vertical-align: middle`。颜色：meta 用 `var(--sk-text-3)`，强调用主题色。Home header 的 🥬/❄ 为品牌装饰，保持原样。

---

## 修改文件清单

| 文件 | 变更 |
|------|------|
| `src/stores/recipes.js` | **新建**：收藏 + AI 缓存 store |
| `src/utils/recipeAi.js` | `normalizeRecipe` 增 `signature`；导出 `getRecipeSignature` |
| `src/utils/index.js` | 新增 `formatRelativeTime` |
| `src/views/Recipes.vue` | tab 增「我的菜谱」；卡片+详情收藏按钮；onMounted 读缓存不自动生成；handleGenerate 写缓存/失败保留；缓存时间条；18 处 emoji→van-icon；getUrgencyText 拆分；引入 useRecipesStore |
| `src/views/Home.vue` | 修 6 处无效图标 + 2 个 h2 加图标 |
| `src/App.vue` | tabbar 修 2 处图标 |
| `src/views/Logs.vue` | `getLogIcon` 三项重排 |
| `src/views/Reports.vue` | 修 3 处 stat-icon + 3 个 h2 加图标 |
| `src/views/Settings.vue` | 5 个 cell 加 icon prop |
| `src/views/AddFood.vue` | 3 个日期字段加 left-icon |
| `src/views/EditFood.vue` | 保质期截止字段加 calendar-o |
| `src/views/Expiring.vue` | stats-bar 3 项加图标 |
| `src/views/ShoppingPlan.vue` | 2 个 h2 加图标 |
| `src/views/Outbound.vue` | 确认消耗按钮加 icon |

`main.js`/`router/index.js`/`package.json` 无需改动。

---

## 实施顺序
1. 基础设施：`recipeAi.js`(signature) + `utils/index.js`(formatRelativeTime) + `stores/recipes.js`(新建)
2. Recipes.vue：缓存 + 收藏 + emoji 替换（改动最大，集中测试）
3. 批量修图标：Home/App/Logs/Reports/Settings/AddFood/EditFood/Expiring/ShoppingPlan/Outbound（纯替换可并行）
4. 全量回归验证

---

## 验证步骤
1. `npm run dev` 启动，DevTools 控制台无 vant-icon「Unknown icon」告警。
2. **图标**：逐页核对 Home(stats-grid 4 + quick-actions 8 + 2 h2)、App tabbar 4、Logs 三类日志、Reports(4 stat-icon + 3 h2)、Settings 5 cell、AddFood/EditFood 日期字段、Expiring stats-bar、ShoppingPlan 2 h2；Recipes 18 处 emoji 全部变为线框图标。
3. **收藏**：卡片点收藏→toast+变「已收藏」；详情弹层按钮联动；切「我的菜谱」tab 看到收藏；重复收藏被拦截；移除实时更新；空状态展示；LocalStorage `recipeFavorites` 存在；刷新后收藏仍在。
4. **缓存**：首次进入不自动调 AI、显示空状态+「生成菜谱」；生成后 `recipeAi.cache` 写入；刷新后直接显示缓存菜谱+「上次生成：x 分钟前」；点「重新生成菜谱」覆盖缓存；生成中取消不清空缓存；降级场景缓存 `useFallback:true` 并刷新后降级条仍在。
5. **回归**：食材卡片/临期色条/状态点不受影响；AddFood/EditFood 表单提交正常；Logs 三类日志图标各不相同。
