/**
 * AI 菜谱推荐工具模块
 *
 * 职责：
 *  1. 从库存数据筛选可用食材（quantity > 0 且未过期），按临期天数升序排序
 *  2. 组装面向大模型的 Prompt（注入完整食材清单 + 用户口味偏好）
 *  3. 调用大模型接口（支持 OpenAI / DeepSeek / 智谱 / Moonshot，可自定义代理端点）
 *  4. 解析模型返回的严格 JSON
 *  5. 提供断网 / 超时 / 接口异常时的本地降级菜谱
 *
 * 依赖说明：复用 utils/index.js 中的 calculateRemainingDays 与 getExpiryStatus，
 * 保证临期判断逻辑与全站一致。
 */

import { calculateRemainingDays, getExpiryStatus } from './index'

// ============================================================
// 服务商配置
// ============================================================
// 每个服务商同时配置直连端点（生产）和代理端点（开发，规避 CORS）
// 开发环境通过 Vite 代理 /ai-proxy/<provider>/* 转发到目标 API
const IS_DEV = import.meta.env.DEV

function buildProvider(key, label, directEndpoint, model) {
  // 从直连端点提取 path 用于构建代理端点
  // 例如 https://api.deepseek.com/v1/chat/completions -> /ai-proxy/deepseek/v1/chat/completions
  const url = new URL(directEndpoint)
  const proxyEndpoint = `/ai-proxy/${key}${url.pathname}`

  return {
    label,
    endpoint: directEndpoint,
    proxyEndpoint,
    model,
    // 开发环境优先用代理（规避 CORS），生产环境用直连或用户自定义端点
    getEffectiveEndpoint(customEndpoint) {
      if (customEndpoint) return customEndpoint
      return IS_DEV ? this.proxyEndpoint : this.endpoint
    }
  }
}

export const AI_PROVIDERS = {
  openai: buildProvider(
    'openai',
    'OpenAI (GPT-4o-mini)',
    'https://api.openai.com/v1/chat/completions',
    'gpt-4o-mini'
  ),
  deepseek: buildProvider(
    'deepseek',
    'DeepSeek (deepseek-chat)',
    'https://api.deepseek.com/v1/chat/completions',
    'deepseek-chat'
  ),
  zhipu: buildProvider(
    'zhipu',
    '智谱清言 (glm-4-flash)',
    'https://open.bigmodel.cn/api/paas/v4/chat/completions',
    'glm-4-flash'
  ),
  moonshot: buildProvider(
    'moonshot',
    'Moonshot (kimi-8k)',
    'https://api.moonshot.cn/v1/chat/completions',
    'moonshot-v1-8k'
  )
}

// API Key 格式校验规则（前缀 + 最小长度）
export const API_KEY_VALIDATORS = {
  openai: { prefix: 'sk-', minLength: 20, hint: 'OpenAI Key 以 "sk-" 开头，通常 50+ 字符' },
  deepseek: { prefix: 'sk-', minLength: 20, hint: 'DeepSeek Key 以 "sk-" 开头' },
  zhipu: { prefix: '', minLength: 10, hint: '智谱 Key 为一长串字母数字' },
  moonshot: { prefix: 'sk-', minLength: 20, hint: 'Moonshot Key 以 "sk-" 开头' }
}

// 校验 API Key 格式，返回 { valid, message }
export function validateApiKey(provider, apiKey) {
  const rule = API_KEY_VALIDATORS[provider]
  if (!rule) return { valid: true, message: '' }
  if (!apiKey || !apiKey.trim()) {
    return { valid: false, message: '请输入 API Key' }
  }
  const key = apiKey.trim()
  if (rule.prefix && !key.startsWith(rule.prefix)) {
    return { valid: false, message: `${AI_PROVIDERS[provider]?.label || '该服务商'} 的 Key 通常以 "${rule.prefix}" 开头` }
  }
  if (key.length < rule.minLength) {
    return { valid: false, message: `Key 长度不足（${key.length} 字符），正常应 ≥ ${rule.minLength} 字符` }
  }
  return { valid: true, message: '格式正确' }
}

// 分类 value -> 中文标签（用于让 AI 更好地理解食材类型）
// 八大分类，与食材知识库文档对齐
const CATEGORY_LABELS = {
  vegetable: '蔬菜',
  meat: '肉类',
  seafood: '海鲜',
  dried: '干货',
  frozen: '冷冻',
  seasoning: '调料',
  fruit: '水果',
  drink: '饮料'
}

const SPICINESS_LABELS = {
  '': '不限',
  'non-spicy': '不辣',
  'mild': '微辣',
  'medium': '中辣',
  'hot': '特辣'
}

const SYSTEM_PROMPT =
  '你是一个专业的家庭烹饪助手，只输出严格格式的JSON，不要任何额外解释文字，不要使用markdown代码块。'

// ============================================================
// 1. 筛选可用食材（排除库存=0 / 已过期 / 距过期<0天），按临期升序
// ============================================================
export function buildIngredientsForAI(foods) {
  if (!Array.isArray(foods)) return []
  return foods
    .filter(item => {
      if (!item || item.quantity <= 0) return false
      const daysLeft = calculateRemainingDays(item.expiryDate)
      return daysLeft !== null && daysLeft >= 0
    })
    .map(item => {
      const daysLeft = calculateRemainingDays(item.expiryDate)
      return {
        id: item.id,
        name: item.name,
        category: CATEGORY_LABELS[item.category] || item.category,
        quantity: item.quantity,
        unit: item.unit,
        storage: item.storageZone,
        daysLeft,
        status: getExpiryStatus(daysLeft)
      }
    })
    .sort((a, b) => a.daysLeft - b.daysLeft)
}

// ============================================================
// 2. 组装 AI Prompt
// ============================================================
export function buildRecipePrompt(ingredients, preferences = {}) {
  const ingredientText = ingredients
    .map(item => {
      const urgency =
        item.daysLeft <= 2 ? '【紧急消耗】' : item.daysLeft <= 7 ? '【临期】' : ''
      return `- ${item.name}（${item.category}）: ${item.quantity}${item.unit} ${urgency}(剩余${item.daysLeft}天)`
    })
    .join('\n')

  // 用户口味偏好（可选）
  const prefLines = []
  if (preferences.dietaryRestrictions && preferences.dietaryRestrictions.length) {
    prefLines.push(`- 忌口：${preferences.dietaryRestrictions.join('、')}`)
  }
  if (preferences.preferredCuisine && preferences.preferredCuisine.length) {
    prefLines.push(`- 偏好菜系：${preferences.preferredCuisine.join('、')}`)
  }
  if (preferences.spiciness) {
    prefLines.push(
      `- 辣度偏好：${SPICINESS_LABELS[preferences.spiciness] || preferences.spiciness}`
    )
  }
  const prefSection = prefLines.length
    ? `## 用户口味偏好\n${prefLines.join('\n')}\n`
    : ''

  return `你是一位资深家庭烹饪教练，擅长用简单食材教厨房小白做菜。

## 可用食材清单（已按临期天数排序，请优先使用靠前的食材）
${ingredientText}

${prefSection}## 默认厨房已有基础佐料
食用油、盐、白糖、生抽酱油、老抽酱油、香醋、料酒、蚝油、淀粉、大蒜、生姜、葱、胡椒粉、花椒、干辣椒。

## 任务要求
1. 根据上述可用食材，推荐 **5 到 6 道不同的菜谱**（尽量覆盖不同的烹饪方式和口味，包含快手懒人餐、减脂轻食餐、家常正餐等不同类型）。
2. **优先使用临期食材**（标记【紧急消耗】和【临期】的食材必须优先消耗，不可浪费）。
3. 每道菜谱必须包含以下字段：
   - **菜名**
   - **烹饪时长**（分钟，整数）
   - **难度等级**（简单/中等/困难）
   - **所需食材清单**：
     - 标注哪些来自冰箱已有食材（列出具体用量）
     - 标注哪些需要额外购买（列出具体用量）
   - **详细制作步骤**：
     - 面向厨房零基础的详细步骤
     - 每一步说明火候（大火/中火/小火）、时间、判断标准（如"煎至两面金黄"）
     - 加入安全提醒（如"热油溅出注意防护"）
   - **热量估算**（kcal/人份，整数）
4. 每道菜使用的额外购买食材 **不超过 3 种**，且价格亲民、超市易购。
5. 如果冰箱食材不足以做一道完整的菜，可以坦诚告知用户并建议补充哪些食材。

## 输出格式（严格 JSON，不要 markdown 代码块标记）
{
  "recipes": [
    {
      "name": "菜名",
      "cookTime": 15,
      "difficulty": "简单",
      "calories": 320,
      "ingredients": {
        "fromFridge": [
          {"name": "食材名", "amount": "用量", "daysLeft": 2}
        ],
        "needToBuy": [
          {"name": "食材名", "amount": "用量", "estimatedPrice": "约X元"}
        ]
      },
      "steps": [
        "步骤1：具体动作 + 火候 + 时间 + 判断标准 + 安全提醒",
        "步骤2：..."
      ],
      "tips": ["小贴士1", "小贴士2"]
    }
  ]
}`
}

// ============================================================
// 3. 调用 AI API
//    - 开发环境自动走 Vite 代理（/ai-proxy/<provider>/*）规避 CORS
//    - 生产环境直连；如遇 CORS 可在配置中填写自定义端点
//    - 401/403 明确提示 Key 无效，CORS/网络错误给出可操作建议
// ============================================================
export async function fetchRecipes(options) {
  const {
    provider = 'deepseek',
    apiKey,
    customEndpoint,
    ingredients,
    preferences,
    signal,
    timeoutMs = 60000
  } = options

  if (!apiKey || !apiKey.trim()) {
    throw new Error('未配置 API Key，请点击右上角「AI设置」填入')
  }

  const config = AI_PROVIDERS[provider] || AI_PROVIDERS.deepseek
  // 使用 getEffectiveEndpoint：开发环境自动走代理，生产环境直连或用自定义端点
  const endpoint = config.getEffectiveEndpoint
    ? config.getEffectiveEndpoint(customEndpoint)
    : customEndpoint || config.endpoint
  const prompt = buildRecipePrompt(ingredients, preferences)

  // 超时控制：AbortController + 自动超时
  // 5-6 道详细菜谱生成耗时较长（DeepSeek/智谱通常 20-40s），默认 60s
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  // 若外部传入 signal，则把外部中止传递到内部
  if (signal) {
    signal.addEventListener('abort', () => controller.abort())
  }

  const requestBody = {
    model: config.model,
    messages: [
      { role: 'system', content: SYSTEM_PROMPT },
      { role: 'user', content: prompt }
    ],
    temperature: 0.7,
    // 5-6 道含详细步骤的菜谱约需 3000-4000 tokens；4096 兼容所有服务商上限
    max_tokens: 4096,
    stream: false
  }

  // 请求日志（脱敏 API Key，仅保留前缀+长度便于排查）
  console.info('[recipeAi] 生成菜谱请求', {
    provider,
    model: config.model,
    endpoint,
    isDev: IS_DEV,
    ingredientCount: ingredients.length,
    maxTokens: requestBody.max_tokens,
    timeoutMs,
    keyInfo: `${apiKey.trim().slice(0, 6)}***(${apiKey.trim().length}位)`
  })

  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey.trim()}`
      },
      body: JSON.stringify(requestBody),
      signal: controller.signal
    })

    if (!response.ok) {
      const errText = await response.text().catch(() => '')
      console.error('[recipeAi] 接口返回错误', {
        status: response.status,
        statusText: response.statusText,
        body: errText.slice(0, 500)
      })
      // 针对常见 HTTP 错误码给出可操作的提示
      if (response.status === 401 || response.status === 403) {
        throw new Error(`API Key 无效或已过期（HTTP ${response.status}），请检查 Key 是否正确`)
      }
      if (response.status === 429) {
        throw new Error('请求频率超限（HTTP 429），请稍后重试或检查账户额度')
      }
      if (response.status >= 500) {
        throw new Error(`AI 服务端异常（HTTP ${response.status}），请稍后重试或更换服务商`)
      }
      throw new Error(`接口返回错误（HTTP ${response.status}）：${errText.slice(0, 200)}`)
    }

    const data = await response.json()
    const content =
      data?.choices?.[0]?.message?.content ||
      data?.choices?.[0]?.content ||
      data?.output?.text ||
      ''
    // 记录 finish_reason 用于判断是否因 max_tokens 截断
    const finishReason = data?.choices?.[0]?.finish_reason
    const tokenUsage = data?.usage

    console.info('[recipeAi] 响应接收', {
      contentLength: content.length,
      finishReason,
      tokenUsage,
      contentPreview: content.slice(0, 120)
    })

    if (!content) {
      throw new Error('AI 返回内容为空，请稍后重试')
    }

    // 检测是否因 max_tokens 不足导致响应被截断
    if (finishReason === 'length') {
      console.warn('[recipeAi] 响应因 max_tokens 限制被截断（finish_reason=length），尝试容错解析')
    }

    return parseRecipeJson(content, finishReason)
  } catch (err) {
    if (err.name === 'AbortError') {
      console.error('[recipeAi] 请求超时被中止', { timeoutMs })
      throw new Error(`请求超时（${timeoutMs / 1000}s），菜谱生成耗时较长，请重试或在 AI 设置中更换更快的模型`)
    }
    // 区分 CORS / 网络错误：fetch 抛 TypeError 通常是网络层失败
    if (err instanceof TypeError && err.message.includes('Failed to fetch')) {
      console.error('[recipeAi] 网络请求失败', { message: err.message })
      throw new Error(
        IS_DEV
          ? '网络请求失败（可能 CORS 被拦截或网络不通），请检查代理配置或网络'
          : '网络请求失败。浏览器直连 AI 接口可能被 CORS 拦截，请在「AI设置」中填写自定义代理端点'
      )
    }
    console.error('[recipeAi] 生成菜谱异常', { message: err.message, stack: err.stack })
    throw err
  } finally {
    clearTimeout(timer)
  }
}

// ============================================================
// 测试 API Key 是否可用（轻量请求，用于配置页"测试连接"按钮）
// 返回 { ok, message }
// ============================================================
export async function testApiKey(provider, apiKey, customEndpoint) {
  if (!apiKey || !apiKey.trim()) {
    return { ok: false, message: '请先输入 API Key' }
  }

  const config = AI_PROVIDERS[provider] || AI_PROVIDERS.deepseek
  const endpoint = config.getEffectiveEndpoint
    ? config.getEffectiveEndpoint(customEndpoint)
    : customEndpoint || config.endpoint

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), 15000)

  try {
    const response = await fetch(endpoint, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${apiKey.trim()}`
      },
      body: JSON.stringify({
        model: config.model,
        messages: [{ role: 'user', content: '你好' }],
        max_tokens: 5
      }),
      signal: controller.signal
    })

    if (response.ok) {
      return { ok: true, message: '✅ 连接成功，API Key 有效！' }
    }
    if (response.status === 401 || response.status === 403) {
      return { ok: false, message: `❌ API Key 无效或已过期（HTTP ${response.status}）` }
    }
    if (response.status === 429) {
      return { ok: false, message: '⚠️ 请求频率超限，Key 有效但当前不可用（HTTP 429）' }
    }
    return { ok: false, message: `❌ 服务端返回 HTTP ${response.status}` }
  } catch (err) {
    if (err.name === 'AbortError') {
      return { ok: false, message: '❌ 连接超时（15秒），请检查网络或更换端点' }
    }
    if (err instanceof TypeError && err.message.includes('Failed to fetch')) {
      return {
        ok: false,
        message: IS_DEV
          ? '❌ 网络请求失败，请检查 Vite 代理配置或网络连接'
          : '❌ 浏览器直连被 CORS 拦截，请在「自定义端点」中填写代理地址'
      }
    }
    return { ok: false, message: `❌ ${err.message || '未知错误'}` }
  } finally {
    clearTimeout(timer)
  }
}

// ============================================================
// 4. 解析 AI 返回的 JSON（容错处理：去除 markdown 包裹、提取首尾花括号、截断修复）
// ============================================================
export function parseRecipeJson(content, finishReason) {
  let clean = String(content)
    .replace(/```json\s*/gi, '')
    .replace(/```\s*/g, '')
    .trim()

  // 直接尝试
  try {
    const parsed = JSON.parse(clean)
    return Array.isArray(parsed.recipes) ? parsed.recipes : parsed
  } catch (_) {
    // 忽略，继续尝试下方提取
  }

  // 提取第一个 { 到最后一个 } 的子串
  const firstBrace = clean.indexOf('{')
  const lastBrace = clean.lastIndexOf('}')
  if (firstBrace !== -1 && lastBrace > firstBrace) {
    const extracted = clean.slice(firstBrace, lastBrace + 1)
    try {
      const parsed = JSON.parse(extracted)
      return Array.isArray(parsed.recipes) ? parsed.recipes : parsed
    } catch (_) {
      // 继续尝试截断修复
    }
  }

  // 截断修复：当 finish_reason=length（max_tokens 不足）时，JSON 可能不完整
  // 尝试补全缺失的闭合符号，提取已完整的菜谱项
  if (finishReason === 'length' || clean.includes('"recipes"')) {
    const repaired = tryRepairTruncatedJson(clean)
    if (repaired && repaired.length > 0) {
      console.warn('[recipeAi] 截断 JSON 修复成功，共挽救 ' + repaired.length + ' 道菜谱')
      return repaired
    }
  }

  // 解析彻底失败，输出诊断信息便于排查
  console.error('[recipeAi] JSON 解析失败', {
    finishReason,
    contentLength: clean.length,
    head: clean.slice(0, 200),
    tail: clean.slice(-200)
  })
  throw new Error(
    finishReason === 'length'
      ? 'AI 返回内容因长度限制被截断，导致 JSON 不完整。请重试，或在 AI 设置中减少菜谱数量'
      : '无法解析 AI 返回的菜谱数据，请重试或更换服务商'
  )
}

// 尝试修复被截断的 JSON：补全闭合符号，提取已完整的 recipe 对象
function tryRepairTruncatedJson(content) {
  try {
    let clean = String(content)
      .replace(/```json\s*/gi, '')
      .replace(/```\s*/g, '')
      .trim()

    // 定位 recipes 数组起始
    const recipesStart = clean.indexOf('"recipes"')
    if (recipesStart === -1) return null

    // 找到 recipes 数组的左括号
    const arrStart = clean.indexOf('[', recipesStart)
    if (arrStart === -1) return null

    // 逐个提取完整的 { ... } 对象（菜谱项）
    const recipes = []
    let depth = 0
    let objStart = -1
    let inString = false
    let escape = false

    for (let i = arrStart + 1; i < clean.length; i++) {
      const ch = clean[i]
      if (escape) {
        escape = false
        continue
      }
      if (ch === '\\') {
        escape = true
        continue
      }
      if (ch === '"') {
        inString = !inString
        continue
      }
      if (inString) continue

      if (ch === '{') {
        if (depth === 0) objStart = i
        depth++
      } else if (ch === '}') {
        depth--
        if (depth === 0 && objStart !== -1) {
          // 提取一个完整的菜谱对象
          const objStr = clean.slice(objStart, i + 1)
          try {
            const recipe = JSON.parse(objStr)
            // 基本完整性校验：至少有 name 字段
            if (recipe && recipe.name) {
              recipes.push(recipe)
            }
          } catch (_) {
            // 该对象不完整，跳过
          }
          objStart = -1
        }
      }
    }

    return recipes.length > 0 ? recipes : null
  } catch (_) {
    return null
  }
}

// ============================================================
// 5. 主流程：生成菜谱（含降级预案）
// ============================================================
export async function generateRecipes({
  foods,
  preferences,
  provider,
  apiKey,
  customEndpoint,
  signal
}) {
  const available = buildIngredientsForAI(foods)

  if (available.length < 3) {
    return {
      success: false,
      useFallback: false,
      message: `当前可用食材仅 ${available.length} 种，不足以生成菜谱。建议先补充常用食材：鸡蛋、西红柿、青椒、土豆、猪肉等。`,
      availableList: available.map(i => i.name),
      recipes: []
    }
  }

  try {
    const recipes = await fetchRecipes({
      provider,
      apiKey,
      customEndpoint,
      ingredients: available,
      preferences,
      signal
    })

    console.info('[recipeAi] 菜谱生成成功', { count: recipes.length })

    return {
      success: true,
      recipes: recipes.map(normalizeRecipe),
      usedIngredients: available.map(i => ({
        name: i.name,
        daysLeft: i.daysLeft,
        status: i.status
      })),
      useFallback: false
    }
  } catch (err) {
    // 降级方案：返回本地预置菜谱，保证演示成功率
    console.error('[recipeAi] 菜谱生成失败，启用降级预案', {
      error: err.message,
      provider,
      ingredientCount: available.length
    })
    return {
      success: false,
      useFallback: true,
      message: `AI 服务暂时不可用（${err.message || '未知错误'}），已为您加载本地经典菜谱。`,
      recipes: getFallbackRecipes().map(normalizeRecipe),
      usedIngredients: available.map(i => ({
        name: i.name,
        daysLeft: i.daysLeft,
        status: i.status
      }))
    }
  }
}

// ============================================================
// 规范化菜谱结构，方便前端统一渲染
// ============================================================
export function normalizeRecipe(recipe, index = 0) {
  const fromFridge = (recipe.ingredients && recipe.ingredients.fromFridge) || []
  const needToBuy = (recipe.ingredients && recipe.ingredients.needToBuy) || []

  const cookTime = Number(recipe.cookTime) || 0
  const caloriesNum = Number(recipe.calories) || 0
  const name = recipe.name || '未知菜谱'
  const type = deriveRecipeType(cookTime, caloriesNum)

  return {
    id: recipe.id || `recipe-${Date.now()}-${index}`,
    name,
    type,
    // 稳定签名：用于跨重新生成的去重（name::type），不随 Date.now() 变化
    signature: buildRecipeSignature(name, type),
    cookTime,
    duration: `${cookTime}分钟`,
    difficulty: recipe.difficulty || '简单',
    calories: caloriesNum > 0 ? `${caloriesNum} kcal` : '未知',
    caloriesNum,
    // 合并的食材清单（兼容旧 UI）
    ingredients: [
      ...fromFridge.map(i => ({
        name: i.name,
        quantity: i.amount,
        fromFridge: true,
        daysLeft: i.daysLeft
      })),
      ...needToBuy.map(i => ({
        name: i.name,
        quantity: i.amount,
        fromFridge: false,
        estimatedPrice: i.estimatedPrice
      }))
    ],
    fromFridge,
    needToBuy,
    missingIngredients: needToBuy.map(i => i.name),
    steps: Array.isArray(recipe.steps) ? recipe.steps : [],
    tips: Array.isArray(recipe.tips) ? recipe.tips : []
  }
}

// 构建菜谱稳定签名（name::type，name 归一化为小写去空白）
export function buildRecipeSignature(name, type) {
  const norm = String(name || '').trim().toLowerCase()
  return `${norm}::${type || ''}`
}

// 获取菜谱签名：优先用既有 signature 字段，否则按规则推导（兜底未 normalize 的对象）
export function getRecipeSignature(recipe) {
  if (!recipe) return ''
  if (recipe.signature) return recipe.signature
  return buildRecipeSignature(recipe.name, recipe.type)
}

// 根据时长与热量推导菜谱类型（用于 Tab 分类）
export function deriveRecipeType(cookTime, calories) {
  if (cookTime > 0 && cookTime <= 15) return 'quick'
  if (calories > 0 && calories <= 250) return 'healthy'
  return 'home'
}

// ============================================================
// 6. 降级预案：本地预置菜谱（断网 / 超时兜底）
// ============================================================
export function getFallbackRecipes() {
  return [
    {
      name: '西红柿炒鸡蛋',
      cookTime: 10,
      difficulty: '简单',
      calories: 280,
      ingredients: {
        fromFridge: [
          { name: '西红柿', amount: '2个', daysLeft: 5 },
          { name: '鸡蛋', amount: '3个', daysLeft: 2 }
        ],
        needToBuy: []
      },
      steps: [
        '【准备】西红柿顶部划十字，放入开水中烫30秒，捞出后轻松去皮，切成小块备用。',
        '【打蛋】鸡蛋打入碗中，加1小撮盐，用筷子充分搅散至起小泡。',
        '【热锅】锅中倒入2勺食用油，开中火加热约20秒，手隔空感受到热气时倒入蛋液。',
        '【炒蛋】蛋液边缘凝固时用铲子轻轻推动，不要频繁翻动，炒至基本凝固但还带一点湿润时盛出备用。',
        '【炒番茄】同一锅中再加半勺油，倒入西红柿块，中火翻炒2分钟至出汁变软，加1勺白糖中和酸味。',
        '【混合】将炒好的鸡蛋倒回锅中，加1勺生抽，快速翻炒均匀，撒入葱花即可出锅。',
        '【判断标准】西红柿炒出红油、鸡蛋裹满汤汁即为成功。'
      ],
      tips: ['炒鸡蛋油温不要太高，中火最佳', '喜欢汤汁多的可以在炒番茄时加2勺水']
    },
    {
      name: '青椒炒猪肉',
      cookTime: 15,
      difficulty: '简单',
      calories: 320,
      ingredients: {
        fromFridge: [
          { name: '猪肉', amount: '200g', daysLeft: 1 },
          { name: '青椒', amount: '2个', daysLeft: 6 }
        ],
        needToBuy: []
      },
      steps: [
        '【腌肉】猪肉切成细丝，加1勺料酒、半勺生抽、少许胡椒粉、1勺淀粉，抓匀腌制10分钟。',
        '【切配】青椒去蒂去籽，切成细丝；大蒜拍碎切末备用。',
        '【热锅凉油】锅中倒2勺油，开大火烧至油微微冒烟，下蒜末爆香10秒。',
        '【炒肉】转中火，倒入腌好的肉丝，快速划散翻炒，肉丝变白（约1分钟）即可盛出。',
        '【炒青椒】锅中留底油，倒入青椒丝，大火快炒1分钟至断生（颜色变得更翠绿）。',
        '【合炒】肉丝倒回锅中，加1勺蚝油、少许盐，大火翻炒30秒均匀入味即可出锅。',
        '【判断标准】肉丝全白无粉色、青椒翠绿不发黄即为熟透。'
      ],
      tips: ['肉丝切薄一点更容易熟', '全程大火快炒保持青椒脆嫩']
    },
    {
      name: '土豆炖鸡块（需额外购买鸡腿）',
      cookTime: 35,
      difficulty: '中等',
      calories: 420,
      ingredients: {
        fromFridge: [{ name: '土豆', amount: '2个', daysLeft: 8 }],
        needToBuy: [
          { name: '鸡腿', amount: '2个（约300g）', estimatedPrice: '约12元' }
        ]
      },
      steps: [
        '【处理鸡肉】鸡腿洗净剁成小块（新手可让摊主代剁），冷水下锅，加2片姜、1勺料酒，大火煮开后撇去浮沫，捞出沥干。',
        '【炒糖色】锅中倒2勺油，加1勺白糖，小火慢慢翻炒至糖融化变成琥珀色（约1分钟，注意别炒焦）。',
        '【炒鸡块】倒入焯好的鸡块，转中火快速翻炒至上色，加入2勺生抽、1勺老抽、1勺料酒、姜片、蒜瓣，继续翻炒1分钟出香味。',
        '【炖煮】加入没过鸡块的热水，大火烧开转中小火，盖锅盖焖煮15分钟。',
        '【加土豆】土豆去皮切滚刀块，倒入锅中，翻拌均匀后继续盖锅盖焖煮10分钟。',
        '【收汁】开盖转大火，加盐调味，不停翻炒至汤汁浓稠挂在土豆上即可出锅。',
        '【判断标准】土豆用筷子能轻松戳穿、鸡肉一抿脱骨即为熟透。'
      ],
      tips: ['炒糖色一定用小火，焦了会发苦', '加热水而非冷水，鸡肉不会变柴']
    },
    {
      name: '清炒时蔬',
      cookTime: 8,
      difficulty: '简单',
      calories: 150,
      ingredients: {
        fromFridge: [
          { name: '青菜', amount: '300g', daysLeft: 3 }
        ],
        needToBuy: []
      },
      steps: [
        '【准备】青菜掰开洗净沥干，大蒜拍碎切末备用。',
        '【热锅】锅中倒2勺食用油，大火烧至油微热（约5秒），下蒜末爆香10秒。',
        '【快炒】倒入青菜，大火快速翻炒1分钟至菜叶变软、颜色更翠绿。',
        '【调味】加半勺盐、少许鸡精，继续翻炒30秒均匀入味即可出锅。',
        '【判断标准】菜叶萎蔫出水、菜梗仍保持脆嫩即为熟透，切忌炒过头。'
      ],
      tips: ['全程大火快炒保留维生素', '青菜洗净后一定要沥干，否则下锅会溅油']
    },
    {
      name: '凉拌黄瓜',
      cookTime: 5,
      difficulty: '简单',
      calories: 80,
      ingredients: {
        fromFridge: [
          { name: '黄瓜', amount: '2根', daysLeft: 4 }
        ],
        needToBuy: []
      },
      steps: [
        '【处理】黄瓜洗净切去两头，用刀面拍裂后切成小段，撒少许盐拌匀腌制5分钟，倒掉多余水分。',
        '【调汁】碗中加入2勺香醋、1勺生抽、1勺白糖、1勺辣椒油、蒜末、香油少许，搅拌均匀至糖融化。',
        '【拌合】将调好的汁倒入黄瓜中，翻拌均匀，静置2分钟入味即可食用。',
        '【判断标准】黄瓜入味微酸微辣、口感爽脆即为成功。'
      ],
      tips: ['拍裂的黄瓜比切的更易入味', '腌制后倒掉水分能让黄瓜更脆']
    }
  ]
}
