// 库存数据存储层 —— 基于 @ohos.data.preferences 实现本地持久化
// 对应原应用 localStorage('foods') 的读写逻辑

import dataPreferences from '@ohos.data.preferences';
import common from '@ohos.app.ability.common';
import { FoodItem, StorageZone, formatDate, estimateExpiryDays } from '../model/DataModel';

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
  { name: '青菜', categoryId: 1, quantity: 2, unit: '把', expiryOffset: 1, storageZone: 'refrigerator', price: 4, totalPrice: 8 },
  { name: '土豆', categoryId: 1, quantity: 5, unit: '个', expiryOffset: 15, storageZone: 'room', price: 2, totalPrice: 10 },
  { name: '鱼', categoryId: 3, quantity: 1, unit: '条', expiryOffset: 3, storageZone: 'refrigerator', price: 25, totalPrice: 25 },
  { name: '虾仁', categoryId: 3, quantity: 200, unit: '克', expiryOffset: 5, storageZone: 'freezer', price: 35, totalPrice: 35 },
  { name: '酸奶', categoryId: 8, quantity: 4, unit: '杯', expiryOffset: -1, storageZone: 'refrigerator', price: 5, totalPrice: 20 },
  { name: '大米', categoryId: 4, quantity: 5, unit: '公斤', expiryOffset: 365, storageZone: 'room', price: 8, totalPrice: 40 },
  { name: '速冻水饺', categoryId: 5, quantity: 1, unit: '袋', expiryOffset: 180, storageZone: 'freezer', price: 18, totalPrice: 18 },
  { name: '青椒', categoryId: 1, quantity: 2, unit: '个', expiryOffset: 3, storageZone: 'refrigerator', price: 3, totalPrice: 6 },
  { name: '洋葱', categoryId: 1, quantity: 3, unit: '个', expiryOffset: 20, storageZone: 'room', price: 3, totalPrice: 9 },
  { name: '食用油', categoryId: 4, quantity: 1, unit: '桶', expiryOffset: 365, storageZone: 'room', price: 68, totalPrice: 68 },
  { name: '酱油', categoryId: 6, quantity: 1, unit: '瓶', expiryOffset: 365, storageZone: 'room', price: 12, totalPrice: 12 },
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

// 单例库存存储
export class InventoryStore {
  private static instance: InventoryStore | null = null;
  private prefs: dataPreferences.Preferences | null = null;
  private foods: FoodItem[] = [];
  private initialized: boolean = false;
  private version: number = 0;

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
  async loadFoods(): Promise<void> {
    if (this.prefs === null) return;
    const json = await this.prefs.get(KEY_FOODS, '') as string;
    if (json && json.length > 0) {
      try {
        const parsed = JSON.parse(json) as Object[];
        const items = parsed.map((item: Object) => toFoodItem(item as Record<string, Object>));
        // 校验解析结果：过滤掉 id 无效的项
        this.foods = items.filter(f => !isNaN(f.id) && f.id > 0 && f.name.length > 0);
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

  // 重置为种子数据
  async resetToSeed(): Promise<void> {
    this.foods = buildSeedFoods();
    await this.persist();
  }

  // 按名称估算保质期
  estimateDays(name: string, categoryId: number): number {
    return estimateExpiryDays(name, categoryId);
  }
}
