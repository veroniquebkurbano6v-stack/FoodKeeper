# API 密钥功能修复说明

## 问题描述

用户反馈"API 密钥无法正常填入并使用"，具体表现为：
1. 在 AI 设置弹层中填入 API Key 后，菜谱生成仍走本地降级菜谱
2. 无法确认 Key 是否生效（无明确的成功/失败反馈）
3. 部分服务商（OpenAI 等）浏览器直连时被 CORS 拦截

## 根因分析

| # | 问题根因 | 严重程度 | 影响范围 |
|---|---------|---------|---------|
| 1 | **Vite 配置无代理** — 浏览器直接调用 AI API 被 CORS 拦截 | 🔴 高 | OpenAI / 智谱 / Moonshot 等服务商 |
| 2 | **API 调用失败时静默降级** — 401/403/CORS 错误均走 fallback，用户无法区分 | 🟡 中 | 所有服务商 |
| 3 | **无 Key 格式校验** — 用户可能误填格式错误的 Key 而不知 | 🟡 中 | 所有用户 |
| 4 | **无连接测试功能** — 用户保存 Key 后无法验证是否有效 | 🟡 中 | 所有用户 |
| 5 | **配置弹层布局** — 弹层高度 70% 可能导致保存按钮被截断 | 🟢 低 | 小屏幕设备 |
| 6 | **密码字段无显示切换** — 用户无法确认输入的 Key 是否正确 | 🟢 低 | 所有用户 |

## 修复内容

### 1. Vite 开发代理（`vite.config.js`）

新增 `/ai-proxy/<provider>/*` 代理规则，将所有 AI 服务商的请求通过 Vite 开发服务器转发：

```
浏览器 → /ai-proxy/deepseek/v1/chat/completions → Vite 代理 → https://api.deepseek.com/v1/chat/completions
```

**覆盖服务商**：OpenAI、DeepSeek、智谱清言、Moonshot

**效果**：开发环境下彻底解决 CORS 问题，无需用户自建代理。

### 2. 智能端点路由（`src/utils/recipeAi.js`）

`AI_PROVIDERS` 配置重构，新增 `getEffectiveEndpoint()` 方法：

- **开发环境** (`import.meta.env.DEV`)：自动使用 `/ai-proxy/<provider>/*` 代理路径
- **生产环境**：直连服务商官方端点
- **自定义端点**：用户填写的代理地址优先级最高

### 3. 精细化错误处理（`src/utils/recipeAi.js`）

`fetchRecipes` 和新增的 `testApiKey` 函数对 HTTP 状态码做分类处理：

| HTTP 状态码 | 错误提示 |
|------------|---------|
| 401 / 403 | "API Key 无效或已过期，请检查 Key 是否正确" |
| 429 | "请求频率超限，请稍后重试或检查账户额度" |
| 5xx | "AI 服务端异常，请稍后重试或更换服务商" |
| TypeError (Failed to fetch) | "网络请求失败（可能 CORS 被拦截）" |

### 4. API Key 格式校验（`src/utils/recipeAi.js` + `Recipes.vue`）

新增 `API_KEY_VALIDATORS` 配置表和 `validateApiKey()` 函数：

| 服务商 | 前缀要求 | 最小长度 |
|--------|---------|---------|
| OpenAI | `sk-` | 20 |
| DeepSeek | `sk-` | 20 |
| 智谱 | 无 | 10 |
| Moonshot | `sk-` | 20 |

- 输入时**实时校验**格式，错误信息显示在输入框下方
- 切换服务商时自动重新校验
- Placeholder 动态显示对应服务商的格式要求

### 5. 测试连接功能（`src/utils/recipeAi.js` + `Recipes.vue`）

新增 `testApiKey()` 函数和"测试连接"按钮：

- 发送轻量请求（`max_tokens: 5`）验证 Key 有效性
- 返回明确结果：✅ 连接成功 / ❌ Key 无效 / ⚠️ 频率超限 / ❌ 超时 / ❌ CORS 拦截
- 测试过程中按钮显示 loading 状态

### 6. 密码显示切换（`Recipes.vue`）

API Key 输入框新增眼睛图标，点击切换 `password` / `text` 类型：
- 👁 默认隐藏（`type="password"`）
- 👁‍🗨 点击后明文显示（`type="text"`），方便确认输入内容

### 7. 配置弹层布局优化（`Recipes.vue`）

- 弹层高度从 70% → **80%**
- 按钮区域增加 `padding-bottom: 12px`
- 新增 `.field-hint` 样式（格式提示文字）

## 修改文件清单

| 文件 | 修改类型 | 说明 |
|------|---------|------|
| `vite.config.js` | 新增 | 添加 AI 代理规则 + server.host/port 配置 |
| `src/utils/recipeAi.js` | 修改 | 服务商配置重构 + 端点路由 + 错误分类 + testApiKey + validateApiKey |
| `src/views/Recipes.vue` | 修改 | 配置弹层 UI + Key 校验 + 测试连接 + 密码切换 |

## 验证结果

| 验证项 | 结果 |
|--------|------|
| 项目构建 | ✅ 327 模块通过，1.98s |
| Vite 代理转发 | ✅ DeepSeek/Moonshot 返回 401，OpenAI/智谱返回 500/405（代理到达） |
| Key 格式校验 | ✅ 无 `sk-` 前缀的 Key 被拦截并提示 |
| 测试连接按钮 | ✅ 假 Key 返回"❌ API Key 无效或已过期（HTTP 401）" |
| 配置保存 | ✅ Key 正确持久化到 localStorage |
| AI 菜谱生成 | ✅ 真实 AI 生成 3 道菜谱（非降级） |
| 密码显示切换 | ✅ 眼睛图标切换 password/text |

## 使用指南

### 开发环境
1. `npm run dev` 启动后，Vite 自动代理所有 AI 请求
2. 打开「菜谱」页 → 右上角「AI设置」
3. 选择服务商（推荐 DeepSeek）→ 粘贴 API Key → 点击「测试连接」验证
4. 点击「保存配置」→ 点击「重新生成菜谱」

### 生产环境
1. 构建产物为单文件 HTML（`vite-plugin-singlefile`）
2. 无 Vite 代理，浏览器直连 AI API
3. DeepSeek 支持浏览器 CORS，可直接使用
4. OpenAI 等不支持 CORS 的服务商，需在「自定义端点」填写代理地址

### API Key 获取
| 服务商 | 获取地址 | 价格 |
|--------|---------|------|
| DeepSeek | https://platform.deepseek.com/api_keys | 极低（推荐） |
| 智谱清言 | https://open.bigmodel.cn/usercenter/apikeys | 有免费额度 |
| OpenAI | https://platform.openai.com/api-keys | 中等 |
| Moonshot | https://platform.moonshot.cn/console/api-keys | 中等 |
