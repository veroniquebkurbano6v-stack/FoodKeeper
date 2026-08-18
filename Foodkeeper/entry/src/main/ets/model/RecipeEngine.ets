// 智能菜谱生成引擎
// 基于库存食材 + 临期优先算法，自动生成 5-7 道菜谱推荐
// 优先使用临期食材（剩余天数越少优先级越高），确保食材不浪费

import { FoodItem } from './DataModel';
import { daysUntilExpiry } from '../utils/ExpiryHelper';

// ===== 菜谱食材模板（仅需名称与用量，剩余天数运行时匹配库存） =====
export interface RecipeIngredientTemplate {
  name: string;
  quantity: string;
}

// ===== 匹配库存后的食材 =====
export interface MatchedIngredient {
  name: string;
  quantity: string;
  haveIt: boolean;       // 库存是否拥有
  days: number;          // 拥有时的剩余天数
  isExpiring: boolean;   // 是否临期（剩余 <= 3 天）
}

// ===== 菜谱模板 =====
export interface RecipeTemplate {
  name: string;
  time: string;
  calories: string;
  caloriesNum: number;
  difficulty: string;
  category: number;      // 0=快手懒人餐, 1=家常正餐, 2=减脂轻食餐
  ingredients: RecipeIngredientTemplate[];
  stepList: string[];
}

// ===== 运行时菜谱（匹配库存后） =====
export interface DisplayRecipe {
  name: string;
  time: string;
  calories: string;
  caloriesNum: number;
  difficulty: string;
  category: number;
  ingredients: MatchedIngredient[];
  stepList: string[];
  steps: number;
  needBuyNames: string[];
  haveCount: number;
  totalCount: number;
  expiringCount: number;   // 使用的临期食材数量
  priorityScore: number;   // 优先级得分
}

// ===== 菜谱数据库（14 道，覆盖常见库存食材） =====
const RECIPE_DATABASE: RecipeTemplate[] = [
  {
    name: '西红柿炒鸡蛋',
    time: '10分钟',
    calories: '280kcal',
    caloriesNum: 280,
    difficulty: '简单',
    category: 0,
    ingredients: [
      { name: '西红柿', quantity: '2个' },
      { name: '鸡蛋', quantity: '3个' },
    ],
    stepList: [
      '西红柿洗净切块，鸡蛋打散加少许盐',
      '热锅冷油，倒入蛋液',
      '蛋液凝固后划散盛出',
      '锅中再加少许油，下西红柿翻炒',
      '炒出汁水，加少许糖提鲜',
      '倒入鸡蛋翻炒均匀',
      '加盐调味出锅',
    ],
  },
  {
    name: '番茄蛋花汤',
    time: '8分钟',
    calories: '150kcal',
    caloriesNum: 150,
    difficulty: '简单',
    category: 2,
    ingredients: [
      { name: '西红柿', quantity: '1个' },
      { name: '鸡蛋', quantity: '2个' },
    ],
    stepList: [
      '西红柿切小丁，鸡蛋打散',
      '锅中加水烧开',
      '下西红柿丁煮 2 分钟',
      '加少许盐、生抽调味',
      '转小火，沿锅边淋入蛋液',
      '蛋花浮起后关火，撒葱花',
    ],
  },
  {
    name: '青椒炒肉丝',
    time: '15分钟',
    calories: '320kcal',
    caloriesNum: 320,
    difficulty: '简单',
    category: 0,
    ingredients: [
      { name: '猪肉', quantity: '200克' },
      { name: '青椒', quantity: '2个' },
    ],
    stepList: [
      '猪肉切薄片，加生抽、淀粉腌制 10 分钟',
      '青椒去籽切块',
      '热锅下油，爆香葱姜',
      '下肉片滑炒至变色盛出',
      '锅中下青椒翻炒',
      '青椒断生后倒回肉片',
      '加盐、生抽调味炒匀',
    ],
  },
  {
    name: '洋葱炒肉',
    time: '12分钟',
    calories: '300kcal',
    caloriesNum: 300,
    difficulty: '简单',
    category: 0,
    ingredients: [
      { name: '猪肉', quantity: '150克' },
      { name: '洋葱', quantity: '1个' },
    ],
    stepList: [
      '猪肉切薄片，加料酒、生抽腌制',
      '洋葱切丝',
      '热锅下油，下肉片炒至变色',
      '盛出肉片，锅中下洋葱丝',
      '洋葱炒软出香味',
      '倒回肉片翻炒均匀',
      '加盐、黑胡椒调味出锅',
    ],
  },
  {
    name: '土豆炒青椒',
    time: '10分钟',
    calories: '220kcal',
    caloriesNum: 220,
    difficulty: '简单',
    category: 2,
    ingredients: [
      { name: '土豆', quantity: '2个' },
      { name: '青椒', quantity: '1个' },
    ],
    stepList: [
      '土豆切薄片泡水去淀粉',
      '青椒切块',
      '热锅下油，下蒜片爆香',
      '下土豆片大火翻炒',
      '土豆片透明后下青椒',
      '加盐、醋调味炒匀',
      '翻炒断生出锅',
    ],
  },
  {
    name: '土豆炖肉',
    time: '35分钟',
    calories: '450kcal',
    caloriesNum: 450,
    difficulty: '中等',
    category: 1,
    ingredients: [
      { name: '猪肉', quantity: '300克' },
      { name: '土豆', quantity: '2个' },
    ],
    stepList: [
      '猪肉切块焯水去血沫',
      '土豆去皮切滚刀块',
      '热锅下油，放冰糖炒糖色',
      '下肉块翻炒上色',
      '加葱姜蒜、八角、桂皮炒香',
      '加生抽、老抽、料酒',
      '加没过肉块的热水，大火烧开',
      '小火炖 15 分钟',
      '下土豆继续炖 10 分钟',
      '大火收汁，加盐调味',
    ],
  },
  {
    name: '清蒸鱼',
    time: '20分钟',
    calories: '250kcal',
    caloriesNum: 250,
    difficulty: '中等',
    category: 2,
    ingredients: [
      { name: '鱼', quantity: '1条' },
    ],
    stepList: [
      '鱼处理干净，两面划刀',
      '抹少许盐、料酒腌制 10 分钟',
      '盘底铺姜片，放上鱼',
      '蒸锅水烧开，放入鱼',
      '大火蒸 8-10 分钟',
      '取出倒掉蒸汁，铺葱丝',
      '淋蒸鱼豉油，浇热油激香',
    ],
  },
  {
    name: '红烧鱼块',
    time: '25分钟',
    calories: '380kcal',
    caloriesNum: 380,
    difficulty: '中等',
    category: 1,
    ingredients: [
      { name: '鱼', quantity: '1条' },
      { name: '酱油', quantity: '2勺' },
    ],
    stepList: [
      '鱼切块，沥干水分',
      '鱼块煎至两面金黄盛出',
      '锅中留底油，爆香葱姜蒜',
      '加酱油、料酒、少许糖',
      '放入鱼块，加适量热水',
      '中火烧 8 分钟入味',
      '大火收汁，撒葱花出锅',
    ],
  },
  {
    name: '虾仁滑蛋',
    time: '8分钟',
    calories: '200kcal',
    caloriesNum: 200,
    difficulty: '简单',
    category: 2,
    ingredients: [
      { name: '虾仁', quantity: '100克' },
      { name: '鸡蛋', quantity: '3个' },
    ],
    stepList: [
      '虾仁去虾线，加料酒腌制',
      '鸡蛋打散，加少许盐',
      '热锅下油，虾仁炒至变色',
      '倒入蛋液',
      '小火慢推至蛋液凝固',
      '撒葱花出锅',
    ],
  },
  {
    name: '虾仁青菜汤',
    time: '15分钟',
    calories: '180kcal',
    caloriesNum: 180,
    difficulty: '简单',
    category: 2,
    ingredients: [
      { name: '虾仁', quantity: '50克' },
      { name: '青菜', quantity: '1把' },
    ],
    stepList: [
      '虾仁去虾线洗净',
      '青菜洗净掰开',
      '锅中加水烧开',
      '下虾仁煮至变红',
      '下青菜煮 2 分钟',
      '加盐、香油调味',
    ],
  },
  {
    name: '蛋炒饭',
    time: '10分钟',
    calories: '350kcal',
    caloriesNum: 350,
    difficulty: '简单',
    category: 0,
    ingredients: [
      { name: '大米', quantity: '1碗' },
      { name: '鸡蛋', quantity: '2个' },
    ],
    stepList: [
      '隔夜米饭打散，鸡蛋打散',
      '热锅下油，倒入蛋液',
      '蛋液半凝固时下米饭',
      '大火快速翻炒，使米饭裹匀蛋液',
      '加盐、生抽调味',
      '撒葱花翻炒均匀出锅',
    ],
  },
  {
    name: '酸奶苹果沙拉',
    time: '5分钟',
    calories: '120kcal',
    caloriesNum: 120,
    difficulty: '简单',
    category: 2,
    ingredients: [
      { name: '酸奶', quantity: '1杯' },
      { name: '苹果', quantity: '1个' },
    ],
    stepList: [
      '苹果洗净去核切小块',
      '放入碗中',
      '倒入酸奶搅拌均匀',
      '可加少许蜂蜜调味',
    ],
  },
  {
    name: '洋葱炒蛋',
    time: '8分钟',
    calories: '250kcal',
    caloriesNum: 250,
    difficulty: '简单',
    category: 0,
    ingredients: [
      { name: '洋葱', quantity: '1个' },
      { name: '鸡蛋', quantity: '2个' },
    ],
    stepList: [
      '洋葱切丝，鸡蛋打散',
      '热锅下油，倒入蛋液炒散盛出',
      '锅中再加少许油，下洋葱丝',
      '洋葱炒软至微焦',
      '倒回鸡蛋翻炒均匀',
      '加盐调味出锅',
    ],
  },
  {
    name: '青菜炒鸡蛋',
    time: '8分钟',
    calories: '200kcal',
    caloriesNum: 200,
    difficulty: '简单',
    category: 0,
    ingredients: [
      { name: '青菜', quantity: '1把' },
      { name: '鸡蛋', quantity: '2个' },
    ],
    stepList: [
      '青菜洗净切段，鸡蛋打散',
      '热锅下油，蛋液炒散盛出',
      '锅中下青菜大火翻炒',
      '青菜变软后倒回鸡蛋',
      '加盐调味炒匀出锅',
    ],
  },
];

// ===== 优先级算法 =====

// 菜谱评分结果
interface RecipeScore {
  score: number;
  ings: MatchedIngredient[];
  expiringCount: number;
  haveCount: number;
  needBuyNames: string[];
}

// 查找库存中某食材的剩余天数；-999 表示未找到
function findFoodDays(foods: FoodItem[], name: string): number {
  for (let i = 0; i < foods.length; i++) {
    const f = foods[i];
    if (f.name === name && f.quantity > 0) {
      return daysUntilExpiry(f.expiryDate);
    }
  }
  return -999;
}

// 判断是否临期（剩余 0-3 天，含已过期）
function checkExpiring(days: number): boolean {
  return days <= 3;
}

// 计算单道菜谱的优先级得分
// 得分规则：
//   库存拥有该食材      +5  分
//   已过期（days < 0）  +8  分（优先消耗，避免浪费）
//   紧急临期（0-1 天）  +15 分（最高优先级）
//   临期（2-3 天）      +10 分
//   库存没有该食材      -2  分（需购买，降低优先级）
//   使用 2+ 临期食材    +10 分（加成奖励）
function scoreRecipe(recipe: RecipeTemplate, foods: FoodItem[]): RecipeScore {
  let score = 0;
  let expiringCount = 0;
  let haveCount = 0;
  const ings: MatchedIngredient[] = [];
  const needBuyNames: string[] = [];

  for (let i = 0; i < recipe.ingredients.length; i++) {
    const ing = recipe.ingredients[i];
    const days = findFoodDays(foods, ing.name);
    if (days !== -999) {
      const expiring = checkExpiring(days);
      const matched: MatchedIngredient = {
        name: ing.name,
        quantity: ing.quantity,
        haveIt: true,
        days: days,
        isExpiring: expiring,
      };
      ings.push(matched);
      haveCount++;
      score += 5; // 拥有该食材
      if (days < 0) {
        score += 8;  // 已过期
        expiringCount++;
      } else if (days <= 1) {
        score += 15; // 紧急临期
        expiringCount++;
      } else if (days <= 3) {
        score += 10; // 临期
        expiringCount++;
      }
    } else {
      const matched: MatchedIngredient = {
        name: ing.name,
        quantity: ing.quantity,
        haveIt: false,
        days: 0,
        isExpiring: false,
      };
      ings.push(matched);
      needBuyNames.push(ing.name);
      score -= 2; // 需购买
    }
  }

  // 使用 2 个及以上临期食材，额外加成
  if (expiringCount >= 2) {
    score += 10;
  }

  const result: RecipeScore = {
    score: score,
    ings: ings,
    expiringCount: expiringCount,
    haveCount: haveCount,
    needBuyNames: needBuyNames,
  };
  return result;
}

// ===== 生成菜谱（返回 5-7 道，按临期优先排序） =====
export function generateRecipes(foods: FoodItem[]): DisplayRecipe[] {
  const scored: DisplayRecipe[] = [];

  for (let i = 0; i < RECIPE_DATABASE.length; i++) {
    const t = RECIPE_DATABASE[i];
    const result = scoreRecipe(t, foods);
    const recipe: DisplayRecipe = {
      name: t.name,
      time: t.time,
      calories: t.calories,
      caloriesNum: t.caloriesNum,
      difficulty: t.difficulty,
      category: t.category,
      ingredients: result.ings,
      stepList: t.stepList,
      steps: t.stepList.length,
      needBuyNames: result.needBuyNames,
      haveCount: result.haveCount,
      totalCount: t.ingredients.length,
      expiringCount: result.expiringCount,
      priorityScore: result.score + Math.random() * 3, // 小随机扰动，增加重新生成的多样性
    };
    scored.push(recipe);
  }

  // 按得分降序排序
  scored.sort((a: DisplayRecipe, b: DisplayRecipe) => b.priorityScore - a.priorityScore);

  // 随机选择 5-7 道
  const count = 5 + Math.floor(Math.random() * 3); // 5, 6, 或 7
  const maxCount = Math.min(count, scored.length);

  return scored.slice(0, maxCount);
}
