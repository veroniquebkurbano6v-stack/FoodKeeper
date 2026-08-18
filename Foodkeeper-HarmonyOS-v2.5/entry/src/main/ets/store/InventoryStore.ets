// 库存数据存储层 —— 基于 @ohos.data.preferences 实现本地持久化
// 对应原应用 localStorage('foods') 的读写逻辑

import dataPreferences from '@ohos.data.preferences';
import common from '@ohos.app.ability.common';
import { FoodItem, StorageZone, formatDate, estimateExpiryDays } from '../model/DataModel';
import { MatchedIngredient } from '../model/RecipeEngine';
import { convertAmount } from '../utils/UnitConverter';

const PREFERENCES_NAME = 'foodkeeper';
const KEY_FOODS = 'foods';

// 种子数据（与原应用 Ra() 一致）
interface SeedItem {
  name: string;
  categoryId: number;
  quantity: number;
  unit: string;
  expiryOffset: number;
  storageZone: StorageZone;
  price: number;
  totalPrice: number;
}

const SEED_DATA: SeedItem[] = [
  { name: '西红柿', categoryId: 1, quantity: 3, unit: '个', expiryOffset: 2, storageZone: 'refrigerator', price: 5, totalPrice: 15 },
  { name: '鸡蛋', categoryId: 2, quantity: 10, unit: '个', expiryOffset: 5, storageZone: 'refrigerator', price: 1.5, totalPrice: 15 },
  { name: '猪肉', categoryId: 2, quantity: 500, unit: '克', expiryOffset: 1, storageZone: 'refrigerator', price: 30, totalPrice: 30 },
  { name: '油菜', categoryId: 1, quantity: 2, unit: '把', expiryOffset: 1, storageZone: 'refrigerator', price: 4, totalPrice: 8 },
  { name: '土豆', categoryId: 1, quantity: 5, unit: '个', expiryOffset: 15, storageZone: 'room', price: 2, totalPrice: 10 },
  { name: '鲈鱼', categoryId: 3, quantity: 1, unit: '条', expiryOffset: 3, storageZone: 'refrigerator', price: 25, totalPrice: 25 },
  { name: '虾仁', categoryId: 3, quantity: 200, unit: '克', expiryOffset: 5, storageZone: 'freezer', price: 35, totalPrice: 35 },
  { name: '酸奶', categoryId: 8, quantity: 4, unit: '杯', expiryOffset: -1, storageZone: 'refrigerator', price: 5, totalPrice: 20 },
  { name: '大米', categoryId: 4, quantity: 5, unit: '公斤', expiryOffset: 365, storageZone: 'room', price: 8, totalPrice: 40 },
  { name: '速冻饺子', categoryId: 5, quantity: 1, unit: '袋', expiryOffset: 180, storageZone: 'freezer', price: 18, totalPrice: 18 },
  { name: '青椒', categoryId: 1, quantity: 2, unit: '个', expiryOffset: 3, storageZone: 'refrigerator', price: 3, totalPrice: 6 },
  { name: '洋葱', categoryId: 1, quantity: 3, unit: '个', expiryOffset: 20, storageZone: 'room', price: 3, totalPrice: 9 },
  { name: '菜籽油', categoryId: 6, quantity: 1, unit: '瓶', expiryOffset: 365, storageZone: 'room', price: 68, totalPrice: 68 },
  { name: '生抽', categoryId: 6, quantity: 1, unit: '瓶', expiryOffset: 365, storageZone: 'room', price: 12, totalPrice: 12 },
  { name: '苹果', categoryId: 7, quantity: 4, unit: '个', expiryOffset: 10, storageZone: 'room', price: 6, totalPrice: 24 },
];

// 新增食材输入参数
export interface AddFoodInput {
  name: string;
  categoryId: number;
  quantity: number;
  unit: string;
  storageZone: StorageZone;
  price: number;
  totalPrice: number;
  expiryDays: number;
  purchaseDate?: string;
}

// 更新食材参数
export interface UpdateFoodPatch {
  name?: string;
  categoryId?: number;
  quantity?: number;
  unit?: string;
  purchaseDate?: string;
  expiryDate?: string;
  storageZone?: StorageZone;
  price?: number;
  totalPrice?: number;
}

// 单个食材扣减明细（用于详细反馈与历史记录）
export interface ConsumeDetail {
  name: string;
  recipeAmount: string;        // 菜谱原始用量字符串（如 "2个"）
  recipeUnit: string;          // 菜谱解析出的单位（如 "个"）
  inventoryUnit: string;       // 库存实际单位
  beforeQty: number;           // 扣减前库存数量
  afterQty: number;            // 扣减后库存数量
  deductedAmount: number;      // 实际扣减数量（按库存单位计）
  conversionNote: string;      // 单位换算备注（空字符串表示无需换算；非空时说明换算依据或失败原因）
  // 'deducted' 正常扣减（单位一致）
  // 'deducted_converted' 单位不一致但已成功换算后扣减
  // 'insufficient' 库存不足扣到0（含换算后不足的情况）
  // 'skipped_no_amount' 用量无法量化（如"适量"）
  // 'skipped_not_in_inventory' 库存无匹配
  // 'skipped_unit_mismatch' 单位不匹配且无法自动换算
  action: 'deducted' | 'deducted_converted' | 'insufficient' | 'skipped_no_amount' | 'skipped_not_in_inventory' | 'skipped_unit_mismatch';
}

// 批量扣减结果（"完成烹饪"返回）
export interface ConsumeResult {
  ok: boolean;          // 是否成功扣减至少一项
  deducted: number;     // 实际扣减的食材项数
  skipped: number;      // 跳过项（用量无法量化或库存无匹配或单位不匹配）
  insufficient: number; // 库存不足被扣到 0 的项数
  details: ConsumeDetail[]; // 每个食材的扣减明细
}

// 完成烹饪历史记录（仅内存，不持久化）
export interface ConsumeLog {
  timestamp: string;     // 操作时间 YYYY-MM-DD HH:mm:ss
  recipeName: string;    // 菜谱名称
  deducted: number;      // 扣减项数
  skipped: number;       // 跳过项数
  insufficient: number;  // 不足项数
  details: ConsumeDetail[]; // 明细
}

// 本地存储信息（供设置页展示）
export interface StorageInfo {
  foodCount: number;
  version: number;
  resetCount: number;
  logCount: number;
  isLocal: boolean;
}

// 生成种子数据
function buildSeedFoods(): FoodItem[] {
  const today = new Date();
  const todayStr = formatDate(today);
  const DAY_MS = 24 * 60 * 60 * 1000;
  const result: FoodItem[] = [];
  for (let i = 0; i < SEED_DATA.length; i++) {
    const s = SEED_DATA[i];
    const expiry = new Date(today.getTime() + s.expiryOffset * DAY_MS);
    result.push({
      id: i + 1,
      name: s.name,
      categoryId: s.categoryId,
      quantity: s.quantity,
      unit: s.unit,
      purchaseDate: todayStr,
      expiryDate: formatDate(expiry),
      storageZone: s.storageZone,
      price: s.price,
      totalPrice: s.totalPrice,
      createdAt: todayStr,
      updatedAt: todayStr,
    });
  }
  return result;
}

// 安全地将解析后的对象转为 FoodItem
function toFoodItem(raw: Record<string, Object>): FoodItem {
  return {
    id: Number(raw['id']),
    name: String(raw['name']),
    categoryId: Number(raw['categoryId']),
    quantity: Number(raw['quantity']),
    unit: String(raw['unit']),
    purchaseDate: String(raw['purchaseDate']),
    expiryDate: String(raw['expiryDate']),
    storageZone: String(raw['storageZone']) as StorageZone,
    price: Number(raw['price']),
    totalPrice: Number(raw['totalPrice']),
    createdAt: String(raw['createdAt']),
    updatedAt: String(raw['updatedAt']),
  };
}

// 复制食材对象（替代展开运算符）
function copyFoodItem(src: FoodItem): FoodItem {
  return {
    id: src.id,
    name: src.name,
    categoryId: src.categoryId,
    quantity: src.quantity,
    unit: src.unit,
    purchaseDate: src.purchaseDate,
    expiryDate: src.expiryDate,
    storageZone: src.storageZone,
    price: src.price,
    totalPrice: src.totalPrice,
    createdAt: src.createdAt,
    updatedAt: src.updatedAt,
  };
}

// 从菜谱用量字符串解析数值（"2个"→2, "200克"→200, "1.5勺"→1.5, "适量"→0, "半只"→0.5）
// 仅解析前导数字部分；支持"半"前缀（0.5）；无数字返回 0（调用方按"跳过"处理）
function parseRecipeAmount(qty: string): number {
  if (!qty || qty.length === 0) return 0;
  let i = 0;
  // 跳过前导空白
  while (i < qty.length && (qty[i] === ' ' || qty[i] === '\t')) {
    i++;
  }
  // 处理"半"前缀（如"半只"→0.5）
  if (i < qty.length && qty[i] === '半') {
    return 0.5;
  }
  let numStr = '';
  // 收集数字与小数点
  while (i < qty.length && ((qty[i] >= '0' && qty[i] <= '9') || qty[i] === '.')) {
    numStr += qty[i];
    i++;
  }
  if (numStr.length === 0) return 0;
  const n = parseFloat(numStr);
  return isNaN(n) ? 0 : n;
}

// 从菜谱用量字符串解析单位（"2个"→"个", "200克"→"克", "半只"→"只", "适量"→""）
// 跳过前导"半"/数字与空白，返回剩余非空白部分作为单位
function parseRecipeUnit(qty: string): string {
  if (!qty || qty.length === 0) return '';
  let i = 0;
  // 跳过前导空白
  while (i < qty.length && (qty[i] === ' ' || qty[i] === '\t')) {
    i++;
  }
  // 跳过"半"前缀
  if (i < qty.length && qty[i] === '半') {
    i++;
  }
  // 跳过数字与小数点
  while (i < qty.length && ((qty[i] >= '0' && qty[i] <= '9') || qty[i] === '.')) {
    i++;
  }
  // 跳过数字与单位间的空白
  while (i < qty.length && (qty[i] === ' ' || qty[i] === '\t')) {
    i++;
  }
  // 收集剩余部分作为单位
  let unit = '';
  while (i < qty.length) {
    unit += qty[i];
    i++;
  }
  return unit.trim();
}

// 单例库存存储
// 【本地数据存储方案】
// 数据完全存储在用户设备本地（@ohos.data.preferences 沙箱目录），不上传任何远程服务器。
//   - 持久化：每次写入后立即 flush() 落盘；应用卸载后数据随之清除。
//   - 安全性：数据存于应用沙箱（/data/data/<包名>/files/preferences/），其他应用无法直接访问；
//             食材库存不属敏感信息，未做额外加密；如需增强可接入 @ohos.security.huks 加密。
//   - 跨设备同步：本地优先架构不提供自动云同步；如需迁移，可通过导出/导入功能（见 getStorageInfo）。
export class InventoryStore {
  private static instance: InventoryStore | null = null;
  private prefs: dataPreferences.Preferences | null = null;
  private foods: FoodItem[] = [];
  private initialized: boolean = false;
  private version: number = 0;
  // 重置计数器：每次 resetToSeed 递增，供页面监听重置事件以清理自身状态
  private resetCount: number = 0;
  // 完成烹饪历史记录（仅内存，不持久化；应用重启后清空）
  private consumeLogs: ConsumeLog[] = [];

  private constructor() {}

  static getInstance(): InventoryStore {
    if (InventoryStore.instance === null) {
      InventoryStore.instance = new InventoryStore();
    }
    return InventoryStore.instance;
  }

  // 获取库存版本号（每次库存变更后递增，供页面检测是否需要重新生成菜谱）
  getVersion(): number {
    return this.version;
  }

  // 获取重置计数器（页面可对比前后值判断是否发生重置）
  getResetCount(): number {
    return this.resetCount;
  }

  // 获取完成烹饪历史记录（仅内存，最近 20 条）
  getConsumeLogs(): ConsumeLog[] {
    return this.consumeLogs.slice(-20);
  }

  // 初始化（在 EntryAbility 中调用）
  async init(context: common.UIAbilityContext): Promise<void> {
    if (this.initialized) return;
    this.prefs = await dataPreferences.getPreferences(context, PREFERENCES_NAME);
    await this.loadFoods();
    this.initialized = true;
  }

  // 检查是否已初始化完成（供页面在 aboutToAppear 时判断时序）
  isInitialized(): boolean {
    return this.initialized;
  }

  // 等待初始化完成（轮询方式，供页面 aboutToAppear 使用）
  async waitForInit(): Promise<void> {
    while (!this.initialized) {
      await new Promise<void>(resolve => setTimeout(resolve, 50));
    }
  }

  // 从 preferences 加载食材
  // 数据完整性校验：解析失败、字段非法、数据为空时均回退到种子数据，避免崩溃
  async loadFoods(): Promise<void> {
    if (this.prefs === null) return;
    const json = await this.prefs.get(KEY_FOODS, '') as string;
    if (json && json.length > 0) {
      try {
        const parsed = JSON.parse(json) as Object[];
        if (!Array.isArray(parsed)) {
          // 数据格式异常（非数组）：回退到种子数据
          this.foods = buildSeedFoods();
          await this.persist();
          return;
        }
        const items = parsed.map((item: Object) => toFoodItem(item as Record<string, Object>));
        // 校验解析结果：过滤掉 id 无效/名称空的项；修正负数数量
        const valid: FoodItem[] = [];
        for (let i = 0; i < items.length; i++) {
          const f = items[i];
          if (!isNaN(f.id) && f.id > 0 && f.name && f.name.length > 0) {
            // 数据修复：数量为负或 NaN 时归零；过期日期缺失时按分类估算
            if (isNaN(f.quantity) || f.quantity < 0) f.quantity = 0;
            if (!f.expiryDate || f.expiryDate.length === 0) {
              f.expiryDate = formatDate(new Date(new Date(f.purchaseDate).getTime() +
                estimateExpiryDays(f.name, f.categoryId) * 24 * 60 * 60 * 1000));
            }
            valid.push(f);
          }
        }
        this.foods = valid;
        if (this.foods.length === 0) {
          // 数据全部无效，回退到种子数据
          this.foods = buildSeedFoods();
          await this.persist();
        }
      } catch (e) {
        // JSON 解析失败：回退到种子数据，避免数据丢失
        this.foods = buildSeedFoods();
        await this.persist();
      }
    } else {
      // 首次启动：写入种子数据
      this.foods = buildSeedFoods();
      await this.persist();
    }
  }

  // 获取本地存储信息（供设置页展示数据存储状态）
  getStorageInfo(): StorageInfo {
    const info: StorageInfo = {
      foodCount: this.foods.length,
      version: this.version,
      resetCount: this.resetCount,
      logCount: this.consumeLogs.length,
      isLocal: true, // 强调：数据存储在本地设备，未上传服务器
    };
    return info;
  }

  // 持久化到 preferences（每次写入递增版本号，供菜谱页面检测库存变更）
  private async persist(): Promise<void> {
    if (this.prefs === null) return;
    await this.prefs.put(KEY_FOODS, JSON.stringify(this.foods));
    await this.prefs.flush();
    this.version++;
  }

  // 获取食材列表（返回副本，避免外部直接修改）
  getFoods(): FoodItem[] {
    return this.foods.slice();
  }

  // 新增食材（对应原 aE）
  async addFood(input: AddFoodInput): Promise<number> {
    // 输入校验：名称非空、数量大于0
    if (!input.name || input.name.trim().length === 0) {
      return -1;
    }
    if (input.quantity <= 0) {
      return -1;
    }
    const now = formatDate(new Date());
    const purchaseDate = input.purchaseDate && input.purchaseDate.length > 0 ? input.purchaseDate : now;
    const DAY_MS = 24 * 60 * 60 * 1000;
    const expiryDate = formatDate(new Date(new Date(purchaseDate).getTime() + input.expiryDays * DAY_MS));
    const id = this.foods.reduce((max, f) => Math.max(max, f.id), 0) + 1;
    const food: FoodItem = {
      id,
      name: input.name.trim(),
      categoryId: input.categoryId,
      quantity: input.quantity,
      unit: input.unit,
      purchaseDate,
      expiryDate,
      storageZone: input.storageZone,
      price: input.price,
      totalPrice: input.totalPrice,
      createdAt: now,
      updatedAt: now,
    };
    this.foods.push(food);
    await this.persist();
    return id;
  }

  // 更新食材（对应原 lE）
  async updateFood(id: number, patch: UpdateFoodPatch): Promise<boolean> {
    const idx = this.foods.findIndex(f => f.id === id);
    if (idx === -1) return false;
    const updated: FoodItem = copyFoodItem(this.foods[idx]);
    if (patch.name !== undefined) updated.name = patch.name;
    if (patch.categoryId !== undefined) updated.categoryId = patch.categoryId;
    if (patch.quantity !== undefined) updated.quantity = patch.quantity;
    if (patch.unit !== undefined) updated.unit = patch.unit;
    if (patch.purchaseDate !== undefined) updated.purchaseDate = patch.purchaseDate;
    if (patch.expiryDate !== undefined) updated.expiryDate = patch.expiryDate;
    if (patch.storageZone !== undefined) updated.storageZone = patch.storageZone;
    if (patch.price !== undefined) updated.price = patch.price;
    if (patch.totalPrice !== undefined) updated.totalPrice = patch.totalPrice;
    updated.updatedAt = formatDate(new Date());
    this.foods[idx] = updated;
    await this.persist();
    return true;
  }

  // 删除食材（对应原 sE）
  async deleteFood(id: number): Promise<boolean> {
    const before = this.foods.length;
    this.foods = this.foods.filter(f => f.id !== id);
    if (this.foods.length !== before) {
      await this.persist();
      return true;
    }
    return false;
  }

  // 出库消耗（对应原 Yi）
  async outboundFood(id: number, amount: number): Promise<boolean> {
    const idx = this.foods.findIndex(f => f.id === id);
    if (idx === -1) return false;
    if (amount <= 0) return false;
    if (this.foods[idx].quantity < amount) return false;
    const updated: FoodItem = copyFoodItem(this.foods[idx]);
    updated.quantity = updated.quantity - amount;
    updated.updatedAt = formatDate(new Date());
    this.foods[idx] = updated;
    await this.persist();
    return true;
  }

  // 批量扣减食材库存（"完成烹饪"使用）
  // 仅扣减 haveIt=true 的食材；菜谱用量无法解析（如"适量"）、库存为 0 时跳过
  // 库存不足时扣到 0（不删记录，由前端过滤 quantity>0 隐藏）
  // 原子性：在副本上批量修改，全部完成后才写回 this.foods 并持久化，中途异常不污染原数据
  // 单位换算：菜谱单位与库存单位不一致时，先尝试通过 UnitConverter 自动换算（如"2个"↔"100克"），
  //           换算成功则按换算后的数量扣减；换算失败才跳过并提示原因
  async consumeIngredients(ingredients: MatchedIngredient[], recipeName?: string): Promise<ConsumeResult> {
    const details: ConsumeDetail[] = [];
    // 仅处理库存拥有的食材
    const toConsume: MatchedIngredient[] = [];
    for (let i = 0; i < ingredients.length; i++) {
      if (ingredients[i].haveIt) {
        toConsume.push(ingredients[i]);
      } else {
        // 库存不拥有的食材也记录明细（便于用户了解为何该项未扣减）
        details.push({
          name: ingredients[i].name,
          recipeAmount: ingredients[i].quantity,
          recipeUnit: parseRecipeUnit(ingredients[i].quantity),
          inventoryUnit: '',
          beforeQty: 0,
          afterQty: 0,
          deductedAmount: 0,
          conversionNote: '',
          action: 'skipped_not_in_inventory',
        });
      }
    }
    if (toConsume.length === 0) {
      return { ok: false, deducted: 0, skipped: ingredients.length, insufficient: 0, details: details };
    }

    // 复制当前库存作为工作副本（保证原子性：失败也不污染 this.foods）
    const work: FoodItem[] = [];
    for (let i = 0; i < this.foods.length; i++) {
      work.push(copyFoodItem(this.foods[i]));
    }

    let deducted = 0;
    let skipped = 0;
    let insufficient = 0;

    for (let i = 0; i < toConsume.length; i++) {
      const ing = toConsume[i];
      const amount = parseRecipeAmount(ing.quantity);
      const recipeUnit = parseRecipeUnit(ing.quantity);
      if (amount <= 0) {
        // 用量无法量化（如"适量"），跳过不扣
        skipped++;
        details.push({
          name: ing.name, recipeAmount: ing.quantity, recipeUnit: recipeUnit,
          inventoryUnit: '', beforeQty: 0, afterQty: 0, deductedAmount: 0,
          conversionNote: '', action: 'skipped_no_amount',
        });
        continue;
      }
      const idx = work.findIndex(f => f.name === ing.name && f.quantity > 0);
      if (idx === -1) {
        skipped++;
        details.push({
          name: ing.name, recipeAmount: ing.quantity, recipeUnit: recipeUnit,
          inventoryUnit: '', beforeQty: 0, afterQty: 0, deductedAmount: 0,
          conversionNote: '', action: 'skipped_not_in_inventory',
        });
        continue;
      }

      // 计算实际需要扣减的数量（按库存单位计）
      // 默认单位一致，直接用 amount；单位不一致时尝试自动换算
      let deductAmount = amount;
      let conversionNote = '';
      let isConverted = false;
      const inventoryUnit = work[idx].unit;
      if (recipeUnit.length > 0 && inventoryUnit.length > 0 && recipeUnit !== inventoryUnit) {
        // 单位不一致：尝试自动换算
        const conv = convertAmount(amount, recipeUnit, inventoryUnit, ing.name);
        if (!conv.ok) {
          // 换算失败：跳过并记录失败原因，便于用户排查
          skipped++;
          details.push({
            name: ing.name, recipeAmount: ing.quantity, recipeUnit: recipeUnit,
            inventoryUnit: inventoryUnit, beforeQty: work[idx].quantity, afterQty: work[idx].quantity,
            deductedAmount: 0, conversionNote: conv.reason || '', action: 'skipped_unit_mismatch',
          });
          continue;
        }
        deductAmount = conv.value;
        conversionNote = conv.note || '';
        isConverted = true;
      }

      const beforeQty = work[idx].quantity;
      if (work[idx].quantity >= deductAmount) {
        work[idx].quantity = work[idx].quantity - deductAmount;
        deducted++;
        details.push({
          name: ing.name, recipeAmount: ing.quantity, recipeUnit: recipeUnit,
          inventoryUnit: inventoryUnit, beforeQty: beforeQty, afterQty: work[idx].quantity,
          deductedAmount: deductAmount, conversionNote: conversionNote,
          action: isConverted ? 'deducted_converted' : 'deducted',
        });
      } else {
        // 库存不足：扣到 0（"只扣库存有的"语义，不产生负数）
        work[idx].quantity = 0;
        insufficient++;
        deducted++;
        details.push({
          name: ing.name, recipeAmount: ing.quantity, recipeUnit: recipeUnit,
          inventoryUnit: inventoryUnit, beforeQty: beforeQty, afterQty: 0,
          deductedAmount: beforeQty, conversionNote: conversionNote, action: 'insufficient',
        });
      }
      work[idx].updatedAt = formatDate(new Date());
    }

    if (deducted === 0) {
      return { ok: false, deducted: 0, skipped: skipped, insufficient: 0, details: details };
    }

    // 全部扣减成功，写回并持久化
    this.foods = work;
    await this.persist();

    // 记录完成烹饪历史（仅内存）
    const now = new Date();
    const ts = `${formatDate(now)} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
    this.consumeLogs.push({
      timestamp: ts,
      recipeName: recipeName || '未知菜谱',
      deducted: deducted,
      skipped: skipped,
      insufficient: insufficient,
      details: details,
    });
    // 仅保留最近 20 条
    if (this.consumeLogs.length > 20) {
      this.consumeLogs = this.consumeLogs.slice(-20);
    }

    return { ok: true, deducted: deducted, skipped: skipped, insufficient: insufficient, details: details };
  }

  // 重置为种子数据（任务1：增加错误处理与重置计数）
  // 成功后递增 resetCount，供监听页面清理自身状态（收藏、已完成菜谱等）
  async resetToSeed(): Promise<boolean> {
    try {
      this.foods = buildSeedFoods();
      await this.persist();
      this.resetCount++;
      return true;
    } catch (e) {
      // 重置失败：保持当前数据不变，返回 false 供调用方提示
      return false;
    }
  }

  // 按名称估算保质期
  estimateDays(name: string, categoryId: number): number {
    return estimateExpiryDays(name, categoryId);
  }
}
