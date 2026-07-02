# 实现计划：菜谱收藏 + 图标系统 — 收尾阶段

## 摘要

本次为延续性任务。前序会话已完成「我的菜谱」收藏功能（stores/recipes.js + Recipes.vue 改造）、AI 菜谱缓存机制、以及首批图标修复（Home/App/Logs/Reports + Recipes.vue 的 18 处 emoji 替换）。经代码核查确认这些工作均已落地生效。

**本计划仅覆盖剩余收尾工作**：6 个页面的图标补齐（Task #4）+ 全量回归验证（Task #5）。

---

## 当前状态分析（已核查）

| 文件 | 状态 |
|------|------|
| `src/stores/recipes.js` | ✅ 已完成（favorites + cache，signature 去重） |
| `src/utils/recipeAi.js` | ✅ 已完成（signature 字段 + getRecipeSignature 导出） |
| `src/utils/index.js` | ✅ 已完成（formatRelativeTime） |
| `src/views/Recipes.vue` | ✅ 已完成（5th tab、收藏按钮、缓存逻辑、emoji→icon） |
| `src/views/Home.vue` | ✅ 已完成（stats-grid + quick-actions + 2 h2 图标） |
| `src/App.vue` | ✅ 已完成（tabbar 图标） |
| `src/views/Logs.vue` | ✅ 已完成（getLogIcon 三项） |
| `src/views/Reports.vue` | ✅ 已完成（4 stat-icon + 3 h2 图标） |
| `src/views/Settings.vue` | ❌ 5 个 van-cell 无 icon prop |
| `src/views/AddFood.vue` | ❌ 3 个日期字段无 left-icon |
| `src/views/EditFood.vue` | ❌ 保质期截止字段无 left-icon |
| `src/views/Expiring.vue` | ❌ stats-bar 3 项无图标 |
| `src/views/ShoppingPlan.vue` | ❌ 2 个 h2 无图标 |
| `src/views/Outbound.vue` | ❌ 确认消耗按钮无 icon |

---

## 提议变更（6 个文件）

### 1. `src/views/Settings.vue` — 5 个 van-cell 加 icon + is-link

van-cell 的 `icon` prop 会在标题左侧渲染对应 van-icon，`is-link` 会渲染右侧箭头，符合设置页交互惯例。

| 行 | 标题 | 新增 icon | 新增 is-link |
|----|------|-----------|-------------|
| 6 | 数据管理 | `records-o` | 是 |
| 7 | 导出数据 | `description-o` | 是 |
| 8 | 重置数据 | `replay` | 是 |
| 12 | 关于应用 | `info-o` | 是 |
| 13 | 隐私设置 | `shield-o` | 是 |

**改法示例（行 6）**：
```vue
<!-- 旧 -->
<van-cell title="数据管理" value="备份/恢复" @click="showDataModal = true" />
<!-- 新 -->
<van-cell title="数据管理" value="备份/恢复" icon="records-o" is-link @click="showDataModal = true" />
```

### 2. `src/views/AddFood.vue` — 3 个日期字段加 `#left-icon` 插槽

| 字段 | 行 | icon |
|------|-----|------|
| 采购日期 | 52-61 | `calendar-o` |
| 保质期天数 | 63-71 | `clock-o` |
| 保质期截止 | 73-78 | `calendar-o` |

**改法示例（采购日期）**：在 `<van-field>` 内追加 `<template #left-icon>`：
```vue
<van-field
  v-model="form.purchaseDate"
  label="采购日期"
  placeholder="请选择采购日期"
  readonly
  required
  :error="!!errors.purchaseDate"
  :error-message="errors.purchaseDate"
  @click="showPurchaseDatePicker = true"
>
  <template #left-icon>
    <van-icon name="calendar-o" />
  </template>
</van-field>
```

### 3. `src/views/EditFood.vue` — 保质期截止字段加 `#left-icon`

| 字段 | 行 | icon |
|------|-----|------|
| 保质期截止 | 49-58 | `calendar-o` |

同 AddFood.vue 改法：在字段内追加 `<template #left-icon><van-icon name="calendar-o" /></template>`。

### 4. `src/views/Expiring.vue` — stats-bar 3 项加图标 + 配色

在 3 个 `stat-item` 内、`stat-value` 之前插入 `<van-icon>`，并新增 `.stat-icon` CSS（display:block、margin:auto、继承各 stat-item 的语义色）。

**模板改法**：
```vue
<div class="stat-item red">
  <van-icon name="fire-o" size="20" class="stat-icon" />
  <span class="stat-value">{{ redCount }}</span>
  <span class="stat-label">即将过期</span>
</div>
<div class="stat-item yellow">
  <van-icon name="clock-o" size="20" class="stat-icon" />
  <span class="stat-value">{{ yellowCount }}</span>
  <span class="stat-label">临期</span>
</div>
<div class="stat-item expired">
  <van-icon name="warning-o" size="20" class="stat-icon" />
  <span class="stat-value">{{ expiredCount }}</span>
  <span class="stat-label">已过期</span>
</div>
```

**CSS 新增**（在 `.stat-label` 规则之后）：
```css
.stat-icon {
  display: block;
  margin: 0 auto 6px;
}

.stat-item.red .stat-icon {
  color: var(--sk-danger);
}

.stat-item.yellow .stat-icon {
  color: var(--sk-warn);
}

.stat-item.expired .stat-icon {
  color: var(--sk-expired);
}
```

### 5. `src/views/ShoppingPlan.vue` — 2 个 h2 加图标 + flex 布局

当前 h2 使用 `::before` 伪元素画橙色色条（`--sk-accent`）。保留色条，h2 改 flex 布局，图标置于色条与文字之间。

**模板改法**：
```vue
<!-- 行 7 -->
<h2><van-icon name="warning-o" size="16" /> 缺料清单</h2>
<!-- 行 28 -->
<h2><van-icon name="shopping-cart-o" size="16" /> 采购清单</h2>
```

**CSS 改法**（`.section-header h2` 规则追加 flex 属性）：
```css
.section-header h2 {
  font-size: 16px;
  font-weight: 600;
  color: var(--sk-text);
  position: relative;
  padding-left: 10px;
  display: flex;          /* 新增 */
  align-items: center;    /* 新增 */
  gap: 6px;               /* 新增 */
}
```
注：`::before` 为 `position: absolute`，脱离文档流，flex 布局不影响其定位。

### 6. `src/views/Outbound.vue` — 确认消耗按钮加 `icon="fire-o"`

```vue
<!-- 行 53-61 -->
<van-button
  type="primary"
  :disabled="selectedCount === 0"
  icon="fire-o"
  round
  size="large"
  @click="handleCookOutbound"
>
  确认消耗
</van-button>
```

---

## 假设与决策

1. **图标全部使用 Vant 4.8 内置图标**：`records-o`、`description-o`、`replay`、`info-o`、`shield-o`、`calendar-o`、`clock-o`、`fire-o`、`warning-o`、`shopping-cart-o` 均已在前序核查中确认存在于 `node_modules/vant/es/icon/index.css`。
2. **Settings.vue 加 `is-link`**：设置项惯例带右箭头，与 Vant Cell 的 `is-link` 语义一致，且不改变现有 `@click` 行为。
3. **Expiring.vue 图标用语义色**：复用现有 `--sk-danger`/`--sk-warn`/`--sk-expired` CSS 变量，与 stat-value 颜色保持一致，不引入新色值。
4. **ShoppingPlan.vue 保留橙色色条**：该页 h2 色条用 `--sk-accent`（橙），与其他页（用 `--sk-primary`）不同，这是既有设计，不改动。
5. **不触碰 showToast 等自定义逻辑**：6 个文件均用 DOM 自建 showToast，不在本次范围内重构。
6. **不加 `is-link` 到 AddFood/EditFood 日期字段**：这些字段已有 readonly + click 弹 picker 模式，加 is-link 会与现有 `#right-icon`（分类/分区字段的箭头）风格冲突；日期字段用 `#left-icon` 足矣。

---

## 验证步骤

### 步骤 1：启动开发服务器
```bash
npm run dev
```
- DevTools 控制台无 vant-icon「Unknown icon」告警
- 无 Vue 编译错误

### 步骤 2：逐页核对图标渲染
| 页面 | 路由 | 核对点 |
|------|------|--------|
| 首页 | `/` | stats-grid 4 图标、quick-actions 8 图标、2 h2 图标 |
| 底部导航 | — | tabbar 4 图标（首页/库存/菜谱/报表） |
| 菜谱 | `/recipes` | 5 tab、卡片收藏按钮、详情弹层按钮、缓存时间条、18 处 icon（无 emoji） |
| 报表 | `/reports` | 4 stat-icon、3 h2 图标 |
| 出库日志 | `/logs` | cook=fire-o、discard=delete-o、clear=clear 三类各不同 |
| 设置 | `/settings` | 5 cell 各有左侧 icon + 右侧箭头 |
| 食材入库 | `/add-food` | 采购日期/保质期天数/保质期截止 各有左 icon |
| 编辑食材 | `/edit-food/:id` | 保质期截止有左 icon |
| 临期预警 | `/expiring` | stats-bar 3 项各有顶部 icon + 语义色 |
| 采购计划 | `/shopping-plan` | 缺料清单/采购清单 2 h2 各有 icon + 色条保留 |
| 食材出库 | `/outbound` | 确认消耗按钮带 fire-o icon |

### 步骤 3：收藏功能回归
- 菜谱页卡片点收藏 → toast + 变「已收藏」（bookmark 实心）
- 详情弹层底部按钮联动
- 切「我的菜谱」tab → 看到已收藏菜谱
- 重复收藏被拦截（addFavorite 返回 false）
- 移除后实时更新
- 空状态展示 + 引导按钮
- 刷新页面后收藏仍在（localStorage `recipeFavorites`）

### 步骤 4：缓存机制回归
- 首次进入菜谱页 → 不自动调 AI，显示空状态 + 「生成菜谱」按钮
- 点「生成菜谱」→ 生成后 `recipeAi.cache` 写入 localStorage
- 刷新页面 → 直接显示缓存菜谱 + 「上次生成：x 分钟前」
- 点「重新生成菜谱」→ 覆盖缓存
- 生成失败但有缓存 → 保留缓存菜谱不清空
- 降级场景 → 缓存 `useFallback:true`，刷新后降级条仍在

### 步骤 5：表单回归
- AddFood 表单提交正常（left-icon 不影响 v-model/校验/提交）
- EditFood 表单提交正常
- Outbound 三 tab 操作正常（cook 确认消耗/discard 报废/clear 清零）

---

## 实施顺序

1. Settings.vue（5 cell，最简单，纯加 prop）
2. Outbound.vue（1 button，最简单）
3. AddFood.vue（3 字段加 left-icon 插槽）
4. EditFood.vue（1 字段加 left-icon 插槽）
5. Expiring.vue（3 stat-item 加 icon + CSS）
6. ShoppingPlan.vue（2 h2 加 icon + CSS flex）
7. 全量回归验证（上述步骤 1-5）
