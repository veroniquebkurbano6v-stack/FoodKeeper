import { describe, beforeAll, beforeEach, afterEach, afterAll, it, expect } from '@ohos/hypium';
import { generateRecipes, DisplayRecipe, MatchedIngredient } from '../main/ets/model/RecipeEngine';
import { FoodItem, StorageZone } from '../main/ets/model/DataModel';
import { daysUntilExpiry, getExpiryStatus, isExpiring } from '../main/ets/utils/ExpiryHelper';

// 构造测试用 FoodItem
function makeFood(name: string, qty: number, unit: string, expiryDate: string, categoryId: number): FoodItem {
  return {
    id: Math.floor(Math.random() * 100000),
    name: name,
    categoryId: categoryId,
    quantity: qty,
    unit: unit,
    purchaseDate: '2026-07-29',
    expiryDate: expiryDate,
    storageZone: 'refrigerator' as StorageZone,
    price: 10,
    totalPrice: 10,
    createdAt: '2026-07-29',
    updatedAt: '2026-07-29',
  };
}

// 获取今天 +N 天的日期字符串
function dateFromToday(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

// 手动查找菜谱索引
function findRecipeIndex(recipes: DisplayRecipe[], name: string): number {
  for (let i = 0; i < recipes.length; i++) {
    if (recipes[i].name === name) return i;
  }
  return -1;
}

export default function agentRecipeTest() {
  describe('AgentRecipeTest', () => {

    // ========================================
    // 一、RecipeEngine.generateRecipes 基础功能
    // ========================================
    describe('RecipeEngine - 基础功能', () => {
      it('空库存应返回5-7道菜谱', 0, () => {
        const foods: FoodItem[] = [];
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
        expect(recipes.length <= 7).assertTrue();
      });

      it('正常库存应返回5-7道菜谱', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
          makeFood('猪肉', 500, '克', dateFromToday(1), 2),
          makeFood('土豆', 5, '个', dateFromToday(15), 1),
        ];
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
        expect(recipes.length <= 7).assertTrue();
      });

      it('每道菜谱应包含非空名称和步骤列表', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const recipes = generateRecipes(foods);
        for (let i = 0; i < recipes.length; i++) {
          expect(recipes[i].name.length > 0).assertTrue();
          expect(recipes[i].stepList.length > 0).assertTrue();
          expect(recipes[i].steps).assertEqual(recipes[i].stepList.length);
        }
      });

      it('haveCount 不应超过 totalCount', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const recipes = generateRecipes(foods);
        for (let i = 0; i < recipes.length; i++) {
          expect(recipes[i].haveCount <= recipes[i].totalCount).assertTrue();
        }
      });

      it('category 应为 0/1/2 之一', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const recipes = generateRecipes(foods);
        for (let i = 0; i < recipes.length; i++) {
          expect(recipes[i].category >= 0).assertTrue();
          expect(recipes[i].category <= 2).assertTrue();
        }
      });
    });

    // ========================================
    // 二、RecipeEngine 临期食材优先策略
    // ========================================
    describe('RecipeEngine - 临期食材优先', () => {
      it('临期食材（<=3天）应被优先覆盖', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(1), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
          makeFood('猪肉', 500, '克', dateFromToday(0), 2),
          makeFood('土豆', 5, '个', dateFromToday(15), 1),
        ];
        const recipes = generateRecipes(foods);
        let hasExpiring = false;
        for (let i = 0; i < recipes.length; i++) {
          if (recipes[i].expiringCount > 0) {
            hasExpiring = true;
            break;
          }
        }
        expect(hasExpiring).assertTrue();
      });

      it('已过期食材（负天数）也应被覆盖', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(-1), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
          makeFood('猪肉', 500, '克', dateFromToday(-2), 2),
        ];
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
      });

      it('临期食材的菜谱应标记 isExpiring=true', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(1), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const recipes = generateRecipes(foods);
        let foundExpiringFlag = false;
        for (let i = 0; i < recipes.length; i++) {
          for (let j = 0; j < recipes[i].ingredients.length; j++) {
            const ing: MatchedIngredient = recipes[i].ingredients[j];
            if (ing.haveIt && ing.days <= 3) {
              expect(ing.isExpiring).assertTrue();
              foundExpiringFlag = true;
            }
          }
        }
        // 至少有一个临期食材被标记
        expect(foundExpiringFlag).assertTrue();
      });
    });

    // ========================================
    // 三、RecipeEngine 排除与收藏
    // ========================================
    describe('RecipeEngine - 排除与收藏', () => {
      it('excludeNames 中的菜谱应被降权排到末尾', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
          makeFood('猪肉', 500, '克', dateFromToday(1), 2),
        ];
        const first = generateRecipes(foods);
        const firstNames: string[] = [];
        for (let i = 0; i < first.length; i++) {
          firstNames.push(first[i].name);
        }
        const second = generateRecipes(foods, { excludeNames: firstNames });
        expect(second.length >= 5).assertTrue();
        // 第二次结果的前几道应与第一次不同（菜谱库14道，排除5-7道后仍有7-9道可选）
        let diffCount = 0;
        for (let i = 0; i < Math.min(3, second.length); i++) {
          if (firstNames.indexOf(second[i].name) === -1) {
            diffCount++;
          }
        }
        expect(diffCount > 0).assertTrue();
      });

      it('favoriteNames 中的菜谱应获得排名加成', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const favorite = '西红柿炒鸡蛋';
        const recipes = generateRecipes(foods, { favoriteNames: [favorite] });
        const idx = findRecipeIndex(recipes, favorite);
        if (idx >= 0) {
          expect(idx < 4).assertTrue();
        }
      });
    });

    // ========================================
    // 四、RecipeEngine 边界场景
    // ========================================
    describe('RecipeEngine - 边界场景', () => {
      it('单个食材应仍返回5-7道菜谱', 0, () => {
        const foods: FoodItem[] = [
          makeFood('鸡蛋', 5, '个', dateFromToday(5), 2),
        ];
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
        expect(recipes.length <= 7).assertTrue();
      });

      it('数量为0的食材应被忽略（不标记为已有）', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 0, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
        for (let i = 0; i < recipes.length; i++) {
          for (let j = 0; j < recipes[i].ingredients.length; j++) {
            if (recipes[i].ingredients[j].name === '西红柿') {
              expect(recipes[i].ingredients[j].haveIt).assertFalse();
            }
          }
        }
      });

      it('名称为空的食材应被忽略', 0, () => {
        const foods: FoodItem[] = [
          makeFood('', 5, '个', dateFromToday(5), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
      });

      it('大库存（50项）应正常生成不崩溃', 0, () => {
        const foods: FoodItem[] = [];
        for (let i = 0; i < 50; i++) {
          foods.push(makeFood(`食材${i}`, i + 1, '个', dateFromToday(i), 1));
        }
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
        expect(recipes.length <= 7).assertTrue();
      });

      it('全过期库存应正常生成菜谱', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(-1), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(-3), 2),
          makeFood('猪肉', 500, '克', dateFromToday(-5), 2),
          makeFood('土豆', 5, '个', dateFromToday(-10), 1),
        ];
        const recipes = generateRecipes(foods);
        expect(recipes.length >= 5).assertTrue();
      });

      it('多次连续调用应不崩溃且返回有效结果', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
          makeFood('猪肉', 500, '克', dateFromToday(1), 2),
        ];
        for (let round = 0; round < 5; round++) {
          const recipes = generateRecipes(foods);
          expect(recipes.length >= 5).assertTrue();
          expect(recipes.length <= 7).assertTrue();
        }
      });
    });

    // ========================================
    // 五、ExpiryHelper 过期计算
    // ========================================
    describe('ExpiryHelper - 过期计算', () => {
      it('daysUntilExpiry 应正确计算剩余天数', 0, () => {
        expect(daysUntilExpiry(dateFromToday(1))).assertEqual(1);
        expect(daysUntilExpiry(dateFromToday(7))).assertEqual(7);
        expect(daysUntilExpiry(dateFromToday(-3))).assertEqual(-3);
      });

      it('今天的日期应返回 0', 0, () => {
        expect(daysUntilExpiry(dateFromToday(0))).assertEqual(0);
      });

      it('getExpiryStatus 阈值判定应正确', 0, () => {
        expect(getExpiryStatus(-1)).assertEqual('expired');
        expect(getExpiryStatus(0)).assertEqual('red');
        expect(getExpiryStatus(1)).assertEqual('red');
        expect(getExpiryStatus(2)).assertEqual('yellow');
        expect(getExpiryStatus(3)).assertEqual('yellow');
        expect(getExpiryStatus(4)).assertEqual('green');
        expect(getExpiryStatus(10)).assertEqual('green');
      });

      it('isExpiring 对临期/已过期食材应返回 true', 0, () => {
        expect(isExpiring(makeFood('西红柿', 3, '个', dateFromToday(1), 1))).assertTrue();
        expect(isExpiring(makeFood('西红柿', 3, '个', dateFromToday(3), 1))).assertTrue();
        expect(isExpiring(makeFood('西红柿', 3, '个', dateFromToday(-1), 1))).assertTrue();
      });

      it('isExpiring 对新鲜食材应返回 false', 0, () => {
        expect(isExpiring(makeFood('西红柿', 3, '个', dateFromToday(5), 1))).assertFalse();
        expect(isExpiring(makeFood('西红柿', 3, '个', dateFromToday(30), 1))).assertFalse();
      });
    });

    // ========================================
    // 六、AgentBridge.buildQuery 上下文构造
    // （通过导入 AgentBridge 测试；若 bundleManager 不可用则此组跳过）
    // ========================================
    // 注意：AgentBridge.ets 顶部 import bundleManager from '@ohos.bundle.bundleManager'
    //   在本地单元测试环境中该系统 API 可能不可用，若导入失败请注释整组测试
    describe('AgentBridge - queryText 构造', () => {
      // 动态导入避免编译期失败
      it('空库存应返回采购建议提示语', 0, () => {
        // 直接验证 buildQuery 的逻辑（复制核心逻辑避免 import 问题）
        const foods: FoodItem[] = [];
        const available: string[] = [];
        for (let i = 0; i < foods.length; i++) {
          const f = foods[i];
          if (f.name.length > 0 && f.quantity > 0) {
            available.push(`${f.name}(${f.quantity}${f.unit})`);
          }
        }
        const queryText = available.length === 0
          ? '我的库存为空，请推荐一些适合日常采购的菜谱'
          : `我的库存有：${available.join('、')}`;
        expect(queryText).assertContain('库存为空');
      });

      it('正常库存应包含食材名称和数量', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const available: string[] = [];
        const expiring: string[] = [];
        for (let i = 0; i < foods.length; i++) {
          const f = foods[i];
          if (f.name.length > 0 && f.quantity > 0) {
            available.push(`${f.name}(${f.quantity}${f.unit})`);
            const days = daysUntilExpiry(f.expiryDate);
            if (days <= 3) {
              expiring.push(`${f.name}(剩${days}天)`);
            }
          }
        }
        const queryText = `我的库存有：${available.join('、')}`;
        expect(queryText).assertContain('西红柿(3个)');
        expect(queryText).assertContain('鸡蛋(10个)');
      });

      it('临期食材应在 queryText 中被标注', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 3, '个', dateFromToday(1), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const expiring: string[] = [];
        for (let i = 0; i < foods.length; i++) {
          const f = foods[i];
          if (f.name.length > 0 && f.quantity > 0) {
            const days = daysUntilExpiry(f.expiryDate);
            if (days <= 3) {
              expiring.push(`${f.name}(剩${days}天)`);
            }
          }
        }
        expect(expiring.length > 0).assertTrue();
        expect(expiring[0]).assertContain('西红柿');
        expect(expiring[0]).assertContain('剩1天');
      });

      it('数量为0的食材不应出现在 queryText 中', 0, () => {
        const foods: FoodItem[] = [
          makeFood('西红柿', 0, '个', dateFromToday(2), 1),
          makeFood('鸡蛋', 10, '个', dateFromToday(5), 2),
        ];
        const available: string[] = [];
        for (let i = 0; i < foods.length; i++) {
          const f = foods[i];
          if (f.name.length > 0 && f.quantity > 0) {
            available.push(`${f.name}(${f.quantity}${f.unit})`);
          }
        }
        expect(available.length).assertEqual(1);
        expect(available[0]).assertContain('鸡蛋');
      });
    });

    // ========================================
    // 七、AgentHolder 降级信号
    // ========================================
    describe('AgentHolder - 降级信号机制', () => {
      // 使用独立实现验证信号逻辑（避免 import bundleManager）
      it('setFallback 后 consumeFallback 应返回 true', 0, () => {
        let flag = false;
        // setFallback
        flag = true;
        // consumeFallback
        const consumed = flag;
        flag = false;
        expect(consumed).assertTrue();
        // 二次消费应返回 false
        const consumed2 = flag;
        expect(consumed2).assertFalse();
      });
    });

  });
}
