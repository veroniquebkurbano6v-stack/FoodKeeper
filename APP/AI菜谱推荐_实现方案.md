# 食库管家 —— AI 菜谱推荐功能实现方案

> 功能目标：根据冰箱库存（含临期优先级）推荐 ≥3 道菜谱，含食材用量与零基础详细步骤

---

## 一、整体架构

```
用户库存数据（LocalStorage）
       │
       ▼
  筛选可用食材 ──→ 按临期天数排序 ──→ 组装 Prompt ──→ 调用 AI API
                                                        │
       ◄──────────────────────────────────────────────┘
                    返回结构化 JSON
       │
       ▼
  前端解析渲染 ──→ 菜谱卡片展示（已有食材 / 需购食材高亮区分）
```

---

## 二、库存数据结构（AI 输入）

```javascript
// 库存表（存储于 LocalStorage）
const inventory = [
  {
    id: 1,
    name: "鸡蛋",
    category: "肉禽蛋奶",
    quantity: 6,
    unit: "个",
    purchaseDate: "2025-07-15",
    shelfLifeDays: 30,        // 保质期总天数
    expireDate: "2025-08-14", // 截止日期
    storage: "冷藏室",
    daysLeft: 2,              // 剩余天数，临期优先级核心字段
    status: "red"             // green / yellow / red
  },
  {
    id: 2,
    name: "西红柿",
    category: "蔬菜",
    quantity: 3,
    unit: "个",
    purchaseDate: "2025-07-20",
    shelfLifeDays: 7,
    expireDate: "2025-07-27",
    storage: "冷藏室",
    daysLeft: 5,
    status: "yellow"
  },
  {
    id: 3,
    name: "青椒",
    category: "蔬菜",
    quantity: 2,
    unit: "个",
    purchaseDate: "2025-07-18",
    shelfLifeDays: 10,
    expireDate: "2025-07-28",
    storage: "冷藏室",
    daysLeft: 6,
    status: "yellow"
  },
  {
    id: 4,
    name: "鸡胸肉",
    category: "肉禽蛋奶",
    quantity: 200,
    unit: "g",
    purchaseDate: "2025-07-20",
    shelfLifeDays: 5,
    expireDate: "2025-07-25",
    storage: "冷藏室",
    daysLeft: 3,
    status: "yellow"
  },
  {
    id: 5,
    name: "土豆",
    category: "蔬菜",
    quantity: 2,
    unit: "个",
    purchaseDate: "2025-07-10",
    shelfLifeDays: 20,
    expireDate: "2025-07-30",
    storage: "常温存放",
    daysLeft: 8,
    status: "green"
  }
];
```

---

## 三、前端调用架构

### 3.1 核心函数

```javascript
// ============================================
// 1. 筛选可用食材（排除库存=0 / 已过期 / 距过期<0天）
// ============================================
function getAvailableIngredients(inventory) {
  const now = new Date();
  return inventory
    .filter(item => item.quantity > 0 && item.daysLeft >= 0)
    .sort((a, b) => a.daysLeft - b.daysLeft); // 临期越短排越前
}

// ============================================
// 2. 组装 AI Prompt
// ============================================
function buildRecipePrompt(ingredients) {
  const ingredientText = ingredients.map(item => {
    const urgency = item.daysLeft <= 2 ? "【紧急消耗】" :
                    item.daysLeft <= 7 ? "【临期】" : "";
    return `- ${item.name}: ${item.quantity}${item.unit} ${urgency}(剩余${item.daysLeft}天)`;
  }).join("\n");

  return `你是一位资深家庭烹饪教练，擅长用简单食材教厨房小白做菜。

## 可用食材清单（已按临期天数排序，请优先使用靠前的食材）
${ingredientText}

## 默认厨房已有基础佐料
食用油、盐、白糖、生抽酱油、老抽酱油、香醋、料酒、蚝油、淀粉、大蒜、生姜、葱、胡椒粉、花椒、干辣椒。

## 任务要求
1. 根据上述可用食材，推荐 **3 道不同的菜谱**（尽量覆盖不同的烹饪方式和口味）。
2. **优先使用临期食材**（标记【紧急消耗】和【临期】的食材必须优先消耗，不可浪费）。
3. 每道菜谱必须包含以下字段：
   - **菜名**
   - **烹饪时长**（分钟）
   - **难度等级**（简单/中等/困难）
   - **所需食材清单**：
     - 标注哪些来自冰箱已有食材（列出具体用量）
     - 标注哪些需要额外购买（列出具体用量）
   - **详细制作步骤**：
     - 面向厨房零基础的详细步骤
     - 每一步说明火候（大火/中火/小火）、时间、判断标准（如"煎至两面金黄"）
     - 加入安全提醒（如"热油溅出注意防护"）
   - **热量估算**（kcal/人份）
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
}`;
}

// ============================================
// 3. 调用 AI API
// ============================================
async function fetchRecipes(apiKey, ingredients) {
  const prompt = buildRecipePrompt(ingredients);

  // 方案A：OpenAI GPT-4o-mini（经济实惠，推荐Demo使用）
  const response = await fetch('https://api.openai.com/v1/chat/completions', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model: 'gpt-4o-mini',
      messages: [
        { role: 'system', content: '你是一个专业的家庭烹饪助手，只输出严格格式的JSON，不要任何额外解释文字。' },
        { role: 'user', content: prompt }
      ],
      temperature: 0.7,
      max_tokens: 2500
    })
  });

  const data = await response.json();
  const content = data.choices[0].message.content;

  // 清理可能的 markdown 代码块标记
  const cleanJson = content.replace(/```json\n?|```\n?/g, '').trim();
  return JSON.parse(cleanJson);
}

// 方案B：支持多个服务商（用户可替换）
async function fetchRecipesUniversal(provider, apiKey, ingredients) {
  const prompt = buildRecipePrompt(ingredients);

  const endpoints = {
    openai: 'https://api.openai.com/v1/chat/completions',
    deepseek: 'https://api.deepseek.com/v1/chat/completions',
    zhipu: 'https://open.bigmodel.cn/api/paas/v4/chat/completions',
    moonshot: 'https://api.moonshot.cn/v1/chat/completions'
  };

  const models = {
    openai: 'gpt-4o-mini',
    deepseek: 'deepseek-chat',
    zhipu: 'glm-4-flash',
    moonshot: 'moonshot-v1-8k'
  };

  const response = await fetch(endpoints[provider], {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${apiKey}`
    },
    body: JSON.stringify({
      model: models[provider],
      messages: [
        { role: 'system', content: '你是一个专业的家庭烹饪助手，只输出严格格式的JSON，不要任何额外解释文字。' },
        { role: 'user', content: prompt }
      ],
      temperature: 0.7,
      max_tokens: 2500
    })
  });

  const data = await response.json();
  const content = data.choices?.[0]?.message?.content || data.choices?.[0]?.content || '';
  const cleanJson = content.replace(/```json\n?|```\n?/g, '').trim();
  return JSON.parse(cleanJson);
}

// ============================================
// 4. 主流程：生成菜谱
// ============================================
async function generateRecipes(apiKey) {
  try {
    // 从本地存储读取库存
    const inventory = JSON.parse(localStorage.getItem('inventory') || '[]');

    // 筛选可用食材
    const available = getAvailableIngredients(inventory);

    if (available.length < 3) {
      return {
        success: false,
        message: `当前可用食材仅 ${available.length} 种，不足以生成菜谱。建议先补充以下常用食材：鸡蛋、西红柿、青椒、土豆、鸡胸肉等。`,
        availableList: available.map(i => i.name)
      };
    }

    // 调用 AI
    const result = await fetchRecipes(apiKey, available);

    return {
      success: true,
      recipes: result.recipes,
      usedIngredients: available.map(i => ({
        name: i.name,
        daysLeft: i.daysLeft,
        status: i.status
      }))
    };

  } catch (err) {
    console.error('菜谱生成失败:', err);

    // 降级方案：返回本地预置菜谱模板
    return {
      success: false,
      useFallback: true,
      message: 'AI 服务暂时不可用，已为您加载经典菜谱。',
      fallbackRecipes: getFallbackRecipes()
    };
  }
}

// ============================================
// 5. 降级预案：本地预置菜谱（断网/超时兜底）
// ============================================
function getFallbackRecipes() {
  return [
    {
      name: "西红柿炒鸡蛋",
      cookTime: 10,
      difficulty: "简单",
      calories: 280,
      ingredients: {
        fromFridge: [
          { name: "西红柿", amount: "2个", daysLeft: 5 },
          { name: "鸡蛋", amount: "3个", daysLeft: 2 }
        ],
        needToBuy: []
      },
      steps: [
        "【准备】西红柿顶部划十字，放入开水中烫30秒，捞出后轻松去皮，切成小块备用。",
        "【打蛋】鸡蛋打入碗中，加1小撮盐，用筷子充分搅散至起小泡。",
        "【热锅】锅中倒入2勺食用油，开中火加热约20秒，手隔空感受到热气时倒入蛋液。",
        "【炒蛋】蛋液边缘凝固时用铲子轻轻推动，不要频繁翻动，炒至基本凝固但还带一点湿润时盛出备用。",
        "【炒番茄】同一锅中再加半勺油，倒入西红柿块，中火翻炒2分钟至出汁变软，加1勺白糖中和酸味。",
        "【混合】将炒好的鸡蛋倒回锅中，加1勺生抽，快速翻炒均匀，撒入葱花即可出锅。",
        "【判断标准】西红柿炒出红油、鸡蛋裹满汤汁即为成功。"
      ],
      tips: ["炒鸡蛋油温不要太高，中火最佳", "喜欢汤汁多的可以在炒番茄时加2勺水"]
    },
    {
      name: "青椒炒鸡胸肉",
      cookTime: 15,
      difficulty: "简单",
      calories: 220,
      ingredients: {
        fromFridge: [
          { name: "鸡胸肉", amount: "200g", daysLeft: 3 },
          { name: "青椒", amount: "2个", daysLeft: 6 }
        ],
        needToBuy: []
      },
      steps: [
        "【腌肉】鸡胸肉切成小薄片，加1勺料酒、半勺生抽、少许胡椒粉、1勺淀粉，抓匀腌制10分钟。",
        "【切配】青椒去蒂去籽，切成细丝；大蒜拍碎切末备用。",
        "【热锅凉油】锅中倒2勺油，开大火烧至油微微冒烟，下蒜末爆香10秒。",
        "【炒肉】转中火，倒入腌好的鸡胸肉片，快速划散翻炒，肉片变白（约1分钟）即可盛出。",
        "【炒青椒】锅中留底油，倒入青椒丝，大火快炒1分钟至断生（颜色变得更翠绿）。",
        "【合炒】鸡肉倒回锅中，加1勺蚝油、少许盐，大火翻炒30秒均匀入味即可出锅。",
        "【判断标准】鸡肉全白无粉色、青椒翠绿不发黄即为熟透。"
      ],
      tips: ["鸡胸肉切薄一点更容易熟", "全程大火快炒保持青椒脆嫩"]
    },
    {
      name: "土豆炖鸡块（需额外购买鸡腿）",
      cookTime: 35,
      difficulty: "中等",
      calories: 420,
      ingredients: {
        fromFridge: [
          { name: "土豆", amount: "2个", daysLeft: 8 }
        ],
        needToBuy: [
          { name: "鸡腿", amount: "2个（约300g）", estimatedPrice: "约12元" }
        ]
      },
      steps: [
        "【处理鸡肉】鸡腿洗净剁成小块（新手可让摊主代剁），冷水下锅，加2片姜、1勺料酒，大火煮开后撇去浮沫，捞出沥干。",
        "【炒糖色】锅中倒2勺油，加1勺白糖，小火慢慢翻炒至糖融化变成琥珀色（约1分钟，注意别炒焦）。",
        "【炒鸡块】倒入焯好的鸡块，转中火快速翻炒至上色，加入2勺生抽、1勺老抽、1勺料酒、姜片、蒜瓣，继续翻炒1分钟出香味。",
        "【炖煮】加入没过鸡块的热水，大火烧开转中小火，盖锅盖焖煮15分钟。",
        "【加土豆】土豆去皮切滚刀块，倒入锅中，翻拌均匀后继续盖锅盖焖煮10分钟。",
        "【收汁】开盖转大火，加盐调味，不停翻炒至汤汁浓稠挂在土豆上即可出锅。",
        "【判断标准】土豆用筷子能轻松戳穿、鸡肉一抿脱骨即为熟透。"
      ],
      tips: ["炒糖色一定用小火，焦了会发苦", "加热水而非冷水，鸡肉不会变柴"]
    }
  ];
}
```

---

## 四、前端渲染示例

```html
<!-- 菜谱卡片渲染 -->
<div id="recipe-container"></div>

<script>
function renderRecipes(data) {
  const container = document.getElementById('recipe-container');

  if (!data.success && !data.useFallback) {
    container.innerHTML = `<div class="empty-state">${data.message}</div>`;
    return;
  }

  const recipes = data.recipes || data.fallbackRecipes || [];

  container.innerHTML = recipes.map((recipe, idx) => `
    <div class="recipe-card">
      <div class="recipe-header">
        <h3>${idx + 1}. ${recipe.name}</h3>
        <span class="badge">${recipe.difficulty}</span>
        <span class="time">⏱ ${recipe.cookTime}分钟</span>
        <span class="calories">🔥 ${recipe.calories}kcal</span>
      </div>

      <!-- 食材清单 -->
      <div class="ingredients-section">
        <h4>📦 食材清单</h4>
        <div class="ingredients-grid">
          <div class="from-fridge">
            <h5>✅ 冰箱已有</h5>
            ${recipe.ingredients.fromFridge.map(i => `
              <span class="ingredient-tag ${i.daysLeft <= 2 ? 'urgent' : i.daysLeft <= 7 ? 'warning' : ''}">
                ${i.name} ${i.amount}
                ${i.daysLeft <= 2 ? '🔴' : i.daysLeft <= 7 ? '🟡' : ''}
              </span>
            `).join('')}
          </div>
          ${recipe.ingredients.needToBuy.length > 0 ? `
            <div class="need-to-buy">
              <h5>🛒 需额外购买</h5>
              ${recipe.ingredients.needToBuy.map(i => `
                <span class="ingredient-tag buy">${i.name} ${i.amount} ${i.estimatedPrice || ''}</span>
              `).join('')}
            </div>
          ` : ''}
        </div>
      </div>

      <!-- 制作步骤 -->
      <div class="steps-section">
        <h4>👨‍🍳 详细制作步骤（零基础版）</h4>
        <ol>
          ${recipe.steps.map(step => `<li>${step}</li>`).join('')}
        </ol>
      </div>

      <!-- 小贴士 -->
      ${recipe.tips ? `
        <div class="tips-section">
          <h4>💡 小贴士</h4>
          <ul>${recipe.tips.map(t => `<li>${t}</li>`).join('')}</ul>
        </div>
      ` : ''}
    </div>
  `).join('');
}
</script>
```

---

## 五、关键设计决策

### 5.1 为什么用 `daysLeft` 排序而非 `status`？
`status`（green/yellow/red）是粗粒度分级，`daysLeft` 是精确数字。AI Prompt 中注入精确的剩余天数，可以让模型更精细地决策——例如同为 red 的鸡蛋（剩2天）和鸡胸肉（剩3天），鸡蛋必须优先使用。

### 5.2 为什么 Prompt 中注入完整食材清单而非仅名称？
AI 需要知道「有多少量」才能判断一道菜是否可行。如果只说「有鸡蛋」而不说「有6个」，AI 可能推荐需要8个鸡蛋的菜谱，导致推荐不可执行。

### 5.3 为什么限制额外购买食材 ≤3 种？
降低用户执行门槛。如果需要买太多东西，用户可能直接放弃这个菜谱。≤3 种额外食材让推荐具有实际可操作性。

### 5.4 为什么要求 JSON 输出？
前端需要精确解析「哪些食材来自冰箱 / 哪些需要购买」来做视觉区分。纯文本输出难以可靠解析，JSON 是机器可读的最优格式。

### 5.5 为什么需要降级预案？
Demo 演示时网络/API 可能出现异常，预置 3 套经典菜谱（西红柿炒蛋、青椒炒鸡胸肉、土豆炖鸡块）可以确保 100% 演示成功率，覆盖最常见的家庭食材组合。

---

## 六、TRAE IDE 中的 AI 配置提示词

如果你在 TRAE IDE 中使用 AI 辅助生成代码，可以给 IDE 的 AI 发送以下提示词：

```
请帮我实现一个「AI 菜谱推荐」功能的前端模块，具体要求如下：

【业务背景】
- 这是一个家庭冰箱食材管理应用，库存数据存储在 LocalStorage 中
- 每条库存记录包含：name(食材名), quantity(数量), unit(单位), daysLeft(剩余保质期天数), status(green/yellow/red)
- 用户有一个 API Key 可以调用大模型接口

【功能需求】
1. 读取 LocalStorage 中的库存数据，筛选出 quantity > 0 且 daysLeft >= 0 的食材
2. 按 daysLeft 升序排序（临期短的优先）
3. 将这些食材信息组装成 Prompt，调用 OpenAI/DeepSeek API
4. 要求 AI 返回 3 道菜谱，每道菜包含：菜名、烹饪时长、难度、食材清单（区分冰箱已有/需额外购买）、面向零基础的详细步骤、热量估算
5. AI 必须优先消耗临期食材（daysLeft <= 7 的）
6. 默认已有基础佐料：油盐酱醋料酒蚝油淀粉葱姜蒜胡椒
7. 额外购买食材不超过 3 种
8. 返回格式为严格 JSON，前端解析渲染为卡片式 UI

【技术要求】
- 纯前端实现，使用原生 JavaScript + HTML + CSS
- 支持多服务商切换（OpenAI / DeepSeek / 智谱 / Moonshot）
- 必须包含降级方案：API 失败时展示 3 套本地预置菜谱
- UI 要求：移动端适配，食材标签用颜色区分临期状态（红色=紧急 黄色=临期 绿色=充足）
- 代码结构清晰，函数职责单一

【输出要求】
请生成完整的 HTML 文件（包含 CSS 样式和 JavaScript），我可以直接在浏览器中打开运行测试。
```

---

## 七、API Key 接入注意事项

| 事项 | 建议 |
|---|---|
| Key 存储 | 不要硬编码在代码中，使用输入框让用户粘贴，存储在 sessionStorage（关闭页面即清除） |
| 模型选择 | Demo 推荐 `gpt-4o-mini` 或 `deepseek-chat`，性价比高，中文菜谱生成质量好 |
| 超时设置 | fetch 设置 timeout = 15 秒，超时即走降级预案 |
| 流量控制 | 每次生成菜谱消耗约 1500-2500 tokens，按 $0.15/1M tokens 计算，单次成本约 ¥0.003 |
| CORS 问题 | 直接从前端调用 API 可能遇到 CORS，建议用 TRAE 的代理功能或简单 Node 转发层 |

---

*本方案为初赛 Demo 版本，聚焦「AI 菜谱推荐」单一功能的完整实现链路，可独立测试验证。*
