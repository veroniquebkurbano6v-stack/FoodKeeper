// 食材计量单位转换工具
// 解决"完成烹饪"时菜谱单位与库存单位不一致的扣减问题
// 支持两层换算策略：
//   1. 通用重量单位互转（克/公斤/千克/斤/两/g/kg）
//   2. 食材专属单位与克互转（如 1个鸡蛋≈50克、1袋金针菇≈200克）

// 转换结果
export interface ConversionResult {
  ok: boolean;          // 是否换算成功
  value: number;        // 换算后的数值（ok=false 时为 0）
  reason?: string;      // 失败原因（ok=false 时有效）
  note?: string;        // 换算备注（如"按 1个≈150克 折算"，便于用户核对）
}

// ===== 第一层：通用重量单位 → 克 的换算系数 =====
// 这类单位不依赖具体食材，可直接互转
const WEIGHT_TO_GRAM: Record<string, number> = {
  '克': 1,
  'g': 1,
  '公斤': 1000,
  '千克': 1000,
  'kg': 1000,
  '斤': 500,
  '两': 50,
};

// ===== 第二层：食材专属单位 → 克 的换算表 =====
// 每个食材下记录其常用单位对应 1 单位的克数（市售常见规格的平均值）
// 覆盖 RECIPE_DATABASE 与 FoodKnowledgeBase 中常见的"个数型/包装型"食材
const FOOD_UNIT_TO_GRAM: Record<string, Record<string, number>> = {
  // —— 蔬菜类 ——
  '西红柿': { '个': 150 },
  '黄瓜': { '根': 200 },
  '茄子': { '根': 200, '个': 200 },
  '青椒': { '个': 100 },
  '辣椒': { '个': 5, '袋': 100 },
  '土豆': { '个': 150 },
  '红薯': { '个': 200 },
  '紫薯': { '个': 100 },
  '洋葱': { '个': 150 },
  '大葱': { '根': 50 },
  '小葱': { '把': 50 },
  '大蒜': { '头': 50, '瓣': 5 },
  '蒜苗': { '把': 150 },
  '蒜薹': { '把': 200 },
  '蒜黄': { '把': 150 },
  '生姜': { '块': 30, '片': 3 },
  '嫩姜': { '块': 30, '片': 3 },
  '胡萝卜': { '根': 150 },
  '白萝卜': { '根': 300 },
  '心里美': { '根': 300 },
  '莴笋': { '根': 300 },
  '菠菜': { '把': 200 },
  '芹菜': { '把': 200 },
  '水芹': { '把': 150 },
  '香菜': { '把': 50 },
  '韭菜': { '把': 200 },
  '韭黄': { '把': 200 },
  '生菜': { '颗': 200 },
  '油菜': { '把': 200 },
  '卷心菜': { '颗': 500 },
  '紫甘蓝': { '颗': 500 },
  '花菜': { '颗': 400 },
  '西蓝花': { '颗': 300 },
  '豆角': { '把': 200 },
  '豇豆': { '把': 200 },
  '豌豆': { '袋': 200 },
  '毛豆': { '袋': 200 },
  '荷兰豆': { '袋': 200 },
  '冬瓜': { '个': 2000 },
  '南瓜': { '个': 1000 },
  '丝瓜': { '根': 300 },
  '苦瓜': { '根': 300 },
  '西葫芦': { '个': 300 },
  '蘑菇': { '盒': 200 },
  '香菇': { '盒': 200, '朵': 20 },
  '金针菇': { '袋': 200 },
  '杏鲍菇': { '个': 200 },
  '平菇': { '袋': 200 },
  '茶树菇': { '盒': 150 },
  '海鲜菇': { '盒': 150 },
  '玉米': { '根': 300 },
  '莲藕': { '节': 300 },
  '山药': { '根': 300 },
  '芋头': { '个': 200 },
  '红芋': { '个': 200 },
  '竹笋': { '根': 300 },
  '冬笋': { '根': 300 },
  '茭白': { '根': 200 },
  '茭白笋': { '根': 200 },
  '秋葵': { '袋': 150 },
  '芦笋': { '把': 200 },
  '苋菜': { '把': 200 },
  '红苋菜': { '把': 200 },
  '空心菜': { '把': 200 },
  '木耳菜': { '把': 200 },
  '茼蒿': { '把': 200 },
  '黄豆芽': { '袋': 250 },
  '绿豆芽': { '袋': 250 },
  '豌豆苗': { '盒': 150 },
  '荠菜': { '袋': 200 },
  '马齿苋': { '袋': 200 },
  '鸡毛菜': { '把': 150 },
  '芥蓝': { '把': 200 },
  '菜心': { '把': 200 },
  '佛手瓜': { '个': 300 },
  '白菜': { '颗': 1000 },
  // —— 肉蛋类 ——
  '鸡蛋': { '个': 50 },
  '鸭蛋': { '个': 70 },
  '鹌鹑蛋': { '个': 12 },
  '皮蛋': { '个': 60 },
  '咸蛋': { '个': 70 },
  '猪肉': { '斤': 500 },
  '牛肉': { '斤': 500 },
  '羊肉': { '斤': 500 },
  '鸡肉': { '只': 1200, '斤': 500 },
  '鸭肉': { '只': 1500, '斤': 500 },
  '鹅肉': { '只': 3000, '斤': 500 },
  '驴肉': { '斤': 500 },
  '兔肉': { '只': 1500, '斤': 500 },
  '排骨': { '斤': 500 },
  '五花肉': { '斤': 500 },
  '里脊肉': { '斤': 500 },
  '前腿肉': { '斤': 500 },
  '后腿肉': { '斤': 500 },
  '猪肝': { '斤': 500, '个': 400 },
  '猪心': { '个': 300, '斤': 500 },
  '猪肺': { '个': 400, '斤': 500 },
  '猪肠': { '斤': 500 },
  '猪肚': { '个': 500, '斤': 500 },
  '猪蹄': { '只': 400, '斤': 500 },
  '香肠': { '根': 100 },
  // —— 海鲜类 ——
  '鲈鱼': { '条': 500 },
  '虾仁': { '袋': 200 },
  // —— 干货/主食类 ——
  '大米': { '碗': 200, '袋': 5000 },
  '小米': { '碗': 200, '袋': 1000 },
  '粉条': { '袋': 250 },
  '木耳': { '袋': 100 },
  '百合': { '袋': 50 },
  '松子': { '袋': 100 },
  // —— 调料/油类 ——
  '生抽': { '勺': 15, '瓶': 500 },
  '老抽': { '勺': 15, '瓶': 500 },
  '料酒': { '勺': 15, '瓶': 500 },
  '蚝油': { '勺': 15, '瓶': 500 },
  '陈醋': { '勺': 15, '瓶': 500 },
  '香醋': { '勺': 15, '瓶': 500 },
  '醋': { '勺': 15, '瓶': 500 },
  '蒸鱼豉油': { '勺': 15, '瓶': 500 },
  '番茄酱': { '勺': 15, '瓶': 250 },
  '沙拉酱': { '勺': 15, '瓶': 250 },
  '豆瓣酱': { '勺': 15, '瓶': 500 },
  '香油': { '勺': 10, '瓶': 250 },
  '菜籽油': { '瓶': 1000 },
  '白糖': { '勺': 15, '袋': 400 },
  '食盐': { '勺': 5, '袋': 400 },
  '淀粉': { '勺': 10, '袋': 250 },
  '腐乳': { '块': 30, '瓶': 250 },
  '桂皮': { '块': 5 },
  '八角': { '个': 2 },
  '花椒': { '袋': 50 },
  '干辣椒': { '个': 2, '袋': 100 },
  // —— 水果/饮料类 ——
  '苹果': { '个': 200 },
  '酸奶': { '杯': 200 },
  '啤酒': { '罐': 330, '瓶': 500 },
};

// 通用估算表：当食材未在 FOOD_UNIT_TO_GRAM 中时，按单位给出默认克数（粗略估算）
// 用于兜底，避免常见单位（如"个""根""把"）完全无法换算
const GENERIC_UNIT_TO_GRAM: Record<string, number> = {
  '个': 100,
  '根': 150,
  '把': 150,
  '颗': 200,
  '块': 50,
  '条': 300,
  '只': 800,
  '头': 50,
  '瓣': 5,
  '片': 3,
  '朵': 20,
  '节': 200,
  '袋': 200,
  '盒': 200,
  '瓶': 500,
  '杯': 200,
  '碗': 200,
  '桶': 500,
  '罐': 300,
  '勺': 15,
};

// 单位归一化（处理英文/中文同义单位）
function normalizeUnit(unit: string): string {
  const u = (unit || '').trim();
  if (u === '千克' || u === 'kg' || u === 'KG') return '公斤';
  if (u === 'g' || u === 'G') return '克';
  return u;
}

// 查询某食材某单位对应的克数（优先食材专属表，其次通用估算表，再次通用重量表）
function getGramPerUnit(foodName: string, unit: string): number | undefined {
  const foodTable = FOOD_UNIT_TO_GRAM[foodName];
  if (foodTable !== undefined) {
    const v = foodTable[unit];
    if (v !== undefined) return v;
  }
  const generic = GENERIC_UNIT_TO_GRAM[unit];
  if (generic !== undefined) return generic;
  const weight = WEIGHT_TO_GRAM[unit];
  if (weight !== undefined) return weight;
  return undefined;
}

// 判断是否为通用重量单位
function isWeightUnit(unit: string): boolean {
  return WEIGHT_TO_GRAM[unit] !== undefined;
}

// 主转换函数：将 amount 个 fromUnit 转换为 toUnit 对应的数值
// foodName 用于食材专属单位换算（如"鸡蛋"的"个"=50克）
// 返回 ConversionResult：ok=true 时 value 为换算后的数值；ok=false 时 reason 为失败原因
export function convertAmount(amount: number, fromUnit: string, toUnit: string, foodName: string): ConversionResult {
  if (amount < 0) {
    return { ok: false, value: 0, reason: '数量不能为负' };
  }
  const from = normalizeUnit(fromUnit);
  const to = normalizeUnit(toUnit);

  // 同单位直接返回
  if (from === to) {
    return { ok: true, value: amount };
  }

  // 策略1：两端都是通用重量单位 → 直接按系数换算（精确）
  const fromWeight = WEIGHT_TO_GRAM[from];
  const toWeight = WEIGHT_TO_GRAM[to];
  if (fromWeight !== undefined && toWeight !== undefined) {
    const v = amount * fromWeight / toWeight;
    return { ok: true, value: v };
  }

  // 策略2：食材专属单位换算（经"克"中转）
  const fromGram = getGramPerUnit(foodName, from);
  const toGram = getGramPerUnit(foodName, to);
  if (fromGram !== undefined && toGram !== undefined && toGram > 0) {
    const grams = amount * fromGram;
    const v = grams / toGram;
    // 标注换算依据，便于用户核对
    const note = `按 1${from}≈${fromGram}克、1${to}≈${toGram}克 折算`;
    return { ok: true, value: v, note: note };
  }

  // 换算失败：给出明确指引
  const reason = `「${fromUnit}」与「${toUnit}」无法自动换算（${foodName}未配置该单位对应克数）`;
  return { ok: false, value: 0, reason: reason };
}

// 判断两个单位是否可换算（用于UI预判，不实际计算）
export function canConvert(fromUnit: string, toUnit: string, foodName: string): boolean {
  const from = normalizeUnit(fromUnit);
  const to = normalizeUnit(toUnit);
  if (from === to) return true;
  if (isWeightUnit(from) && isWeightUnit(to)) return true;
  const fromGram = getGramPerUnit(foodName, from);
  const toGram = getGramPerUnit(foodName, to);
  return fromGram !== undefined && toGram !== undefined;
}
