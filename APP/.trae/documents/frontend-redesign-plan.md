# 食库管家 前端视觉重构计划

## Context（背景）

当前 App（Vue3 + Vant 4）所有 12 个视图使用 Vant 默认高饱和色（`#07c160` 绿、`#ee0a24` 红、`#ff976a` 橙、`#1890ff` 蓝、`#722ed1` 紫），各页 scoped 样式彼此割裂，缺少统一主题；食材卡片无分类底色，临期食材仅靠一个小 tag 提示，不够醒目。

本次目标：**在保持所有功能接口、按钮、表单、弹层、路由、store 逻辑完全不变的前提下**，仅改造视觉层（CSS + 少量展示型辅助函数），围绕「食物 / 食谱 / 冰箱」三元素构建统一主题，采用「暖意厨房」低饱和配色，为 5 类食材设计专属底色，并强化临期/过期预警（颜色 + 图标 + 位置优先 + 动效）。完成后输出一份 Markdown 设计说明文档至项目根目录。

用户已确认：① 配色方向 = 暖意厨房；② 范围 = 全部 12 个视图。

---

## 设计系统：暖意厨房（Warm Kitchen）

### 主色板（低饱和、自然、和谐）
| 语义 | 变量名 | 色值 | 用途 |
|------|--------|------|------|
| 页面底 | `--sk-bg` | `#FAF6EF` | 暖奶油底色，所有页面背景 |
| 卡片底 | `--sk-card` | `#FFFEFB` | 卡片/表单白底（带一点暖意） |
| 分区底 | `--sk-section` | `#F5EFE4` | 次级区块/输入提示底 |
| 主色 | `--sk-primary` | `#7C9473` | 鼠尾草绿（食物/新鲜），主按钮/激活态 |
| 主色深 | `--sk-primary-dark` | `#5E7A56` | 悬停/按压 |
| 主色浅底 | `--sk-primary-bg` | `#EDF1E8` | 浅绿底（标签/统计） |
| 暖点缀 | `--sk-accent` | `#C97B5C` | 赤陶橙（烹饪/食谱） |
| 暖点缀浅底 | `--sk-accent-bg` | `#F7E8DF` | 浅橙底 |
| 冷点缀 | `--sk-cold` | `#6E8CA8` | 雾霾蓝（冰箱/冷冻） |
| 冷点缀浅底 | `--sk-cold-bg` | `#E8EEF4` | 浅蓝底 |
| 正文 | `--sk-text` | `#3D352E` | 暖深棕（非纯黑） |
| 次文 | `--sk-text-2` | `#8B7E6F` | 暖灰棕 |
| 占位 | `--sk-text-3` | `#B5A99A` | 浅暖灰 |
| 描边 | `--sk-border` | `#ECE4D6` | 暖色描边 |

### 分类专属底色（5 类，取自食材本身的视觉联想，低饱和）
| 分类 value | label | 底色（卡片左条+浅底） | 圆点 |
|-----------|-------|----------------------|------|
| `vegetable` | 蔬菜 | `#EDF1E8` | `#7C9473` 鼠尾草绿 |
| `meat` | 肉禽蛋奶 | `#F5E6E2` | `#C97B6E` 暖玫瑰 |
| `seafood` | 水产 | `#E8EEF4` | `#6E8CA8` 雾霾蓝 |
| `dried` | 干货调料 | `#F0E9DC` | `#A88B6A` 暖驼 |
| `frozen` | 速冻食品 | `#E9EEF1` | `#8BA0B0` 冰灰蓝 |

### 临期预警体系（渐进式紧迫感，仍属低饱和系）
| 状态 | 色值 | 浅底 | 视觉强化 |
|------|------|------|---------|
| normal(>7d) | `#7C9473` | `#EDF1E8` | 仅左条+小绿点 |
| yellow/临期(≤7d) | `#D4A55B` 蜜糖琥珀 | `#FBF1DE` | 左条+琥珀点+⚠ 图标 |
| red/即将过期(≤2d) | `#C75D4D` 砖红 | `#FAE6E2` | 左条+**呼吸脉冲点**+「紧急」徽标 |
| expired(<0d) | `#9A8E80` 暖灰 | `#EDEAE5` | 左条+灰点+置灰+「已过期」徽标 |

> 临期食材本已通过 `store.sortedFoods` 按状态升序排在前面（位置优先），保留此逻辑。

---

## 实施方案

### 1. 新增全局主题样式 `src/styles/theme.css`
- 定义上述所有 CSS 变量于 `:root`
- 覆写 Vant 主题变量：`--van-primary-color`、`--van-text-color`、`--van-background`、`--van-nav-bar-background`、`--van-tabbar-background`、`--van-cell-background`、`--van-button-primary-background` 等，使 Vant 组件整体融入暖意厨房色调，避免逐页手改组件色
- 全局页面底色、字体、`van-nav-bar` 标题色与底色、`van-tabbar` 风格统一
- 临期呼吸动画 `@keyframes sk-pulse` 用于 red 状态圆点

### 2. 在 `src/main.js` 引入主题样式
在 `import 'vant/lib/index.css'` 之后引入 `./styles/theme.css`，确保覆写生效。

### 3. 新增分类色辅助函数 `src/utils/index.js`
新增 `getCategoryColors(category)` 返回 `{ bg, dot, label }`，供库存/临期/出库/报表等卡片复用。新增 `getExpiryVisual(status)` 返回 `{ color, bg, dot, pulse, badge, icon }`，统一临期视觉。**不改任何已有函数签名**，仅追加。

### 4. 逐视图重构 `<style>`（模板结构与 script 逻辑零改动）

每个视图的统一改造模式：
- 页面根容器改用 `--sk-bg` 暖奶油底
- 卡片：圆角 14px、`--sk-card` 底、`--sk-border` 描边、左侧 4px 分类/临期色条
- 食材卡片（库存/临期/出库/报表明细）：应用分类底色 + 临期色条覆盖
- 统计卡/数字卡：圆角、浅底、图标圆角块用对应语义色
- 区块标题：暖深棕、左侧 4px 主色竖条
- 临期 red 卡：脉冲点 + 紧急徽标；expired 卡：置灰 + 蒙层

具体视图要点：
- **Home.vue**：Hero 卡换成「冰箱」造型渐变（主色→冷点缀），统计宫格用语义浅底+图标，临期卡用上述强化体系，快捷操作宫格图标加圆角浅底块
- **Inventory.vue**：搜索栏暖底，筛选 tab 暖色激活态，食材卡左条=分类色+临期色优先级覆盖
- **Expiring.vue**：三统计卡用 red/amber/grey 语义浅底，卡片强化临期视觉，过期卡置灰+报废按钮
- **Recipes.vue**：顶部状态条暖底，菜谱卡左条用食谱类型色映射到暖意系（quick→主色绿、healthy→冷点缀蓝、home→暖点缀橙），「冰箱已有」食材 ing-tag 按其临期天数染色，「需购买」用暖点缀浅底；详情弹层步骤序号用主色圆
- **Reports.vue**：统计卡语义底，分类条形图用新分类色，明细卡左条分类色
- **Outbound.vue**：食材卡左条分类色，烹饪/报废/清零 tab 用主色/砖红/琥珀
- **Logs.vue**：日志卡左条按出库类型语义色（cook→主色、discard→砖红、clear→琥珀），图标圆角块
- **AddFood.vue / EditFood.vue**：表单卡暖白底圆角，主按钮主色，危险按钮砖红
- **Preferences.vue / Settings.vue / ShoppingPlan.vue**：统一暖底、卡片圆角、分区标题竖条

### 5. 输出设计说明文档
项目根目录新建 `前端设计说明.md`，含：设计理念、色彩规范表、分类底色表、临期预警机制、元素布局说明、交互逻辑说明、关键视觉元素实现细节（CSS 变量、分类色条、呼吸动画、Vant 主题覆写清单）。

---

## 关键文件清单

新增：
- `src/styles/theme.css` — 全局主题变量 + Vant 覆写 + 动画
- `前端设计说明.md` — 设计文档（项目根目录）

修改（仅 `<style>` 与少量展示型函数，模板/script 不动）：
- `src/main.js` — 引入 theme.css
- `src/utils/index.js` — 追加 `getCategoryColors` / `getExpiryVisual`（不改已有导出）
- `src/App.vue`（全局容器底色）
- `src/views/Home.vue`
- `src/views/Inventory.vue`
- `src/views/Expiring.vue`
- `src/views/Recipes.vue`
- `src/views/Reports.vue`
- `src/views/Outbound.vue`
- `src/views/Logs.vue`
- `src/views/AddFood.vue`
- `src/views/EditFood.vue`
- `src/views/Preferences.vue`
- `src/views/Settings.vue`
- `src/views/ShoppingPlan.vue`

复用（已有、不改动）：
- `src/stores/inventory.js` 的 `sortedFoods`（临期位置优先排序已具备）
- `src/utils/index.js` 的 `getExpiryStatus` / `getExpiryStatusColor` / `getExpiryStatusLabel` / `getCategoryLabel` / `getStorageZoneLabel`
- Vant 4 组件（van-nav-bar/van-tabbar/van-cell/van-tag/van-tabs/van-popup/van-picker/van-date-picker/van-field/van-button/van-grid/van-empty/van-loading/van-stepper/van-checkbox/van-action-sheet/van-dialog/van-search/van-icon）

---

## 验证方式

1. `npm run dev` 启动开发服务器（已有 Vite 代理配置不受影响）
2. 逐页核对功能完整性（所有按钮/表单/弹层/路由跳转/出库入库/报废/菜谱生成/AI 设置/导出 JSON·CSV 均可正常工作）
3. 视觉核对：
   - 整体暖奶油底、鼠尾草绿主色，无高饱和残留
   - 库存/临期/出库页食材卡左条显示分类专属色，一眼可分辨
   - 临期食材（≤2天）显示脉冲红点+「紧急」徽标并排在前列；已过期置灰
   - 菜谱页「冰箱已有」食材按临期天数染色，食谱卡左条随类型变色
   - 暗色文字为暖棕而非纯黑
4. 浏览器控制台无新增 warning/error
5. 确认 `前端设计说明.md` 已生成于项目根目录
