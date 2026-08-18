// 食库管家数据模型与常量定义

// 食材分类值
export type CategoryValue = 'vegetable' | 'meat' | 'seafood' | 'dried' | 'frozen' | 'seasoning' | 'fruit' | 'drink';
// 存储分区
export type StorageZone = 'refrigerator' | 'freezer' | 'room';

// 食材项数据结构（与原应用一致）
export interface FoodItem {
  id: number;
  name: string;
  categoryId: number;
  quantity: number;
  unit: string;
  purchaseDate: string;   // YYYY-MM-DD
  expiryDate: string;     // YYYY-MM-DD
  storageZone: StorageZone;
  price: number;
  totalPrice: number;
  createdAt: string;
  updatedAt: string;
}

// 分类信息
export interface CategoryInfo {
  value: CategoryValue;
  label: string;
  id: number;
  color: string;   // 圆点色
  bg: string;      // 背景色
}

// 存储分区信息
export interface StorageZoneInfo {
  value: StorageZone;
  label: string;
}

// 八大分类（与原应用 Sl() 配色一致）
export const CATEGORIES: CategoryInfo[] = [
  { value: 'vegetable', label: '蔬菜', id: 1, color: '#7C9473', bg: '#EDF1E8' },
  { value: 'meat', label: '肉类', id: 2, color: '#C97B6E', bg: '#F5E6E2' },
  { value: 'seafood', label: '海鲜', id: 3, color: '#6E8CA8', bg: '#E8EEF4' },
  { value: 'dried', label: '干货', id: 4, color: '#A88B6A', bg: '#F0E9DC' },
  { value: 'frozen', label: '冷冻', id: 5, color: '#8BA0B0', bg: '#E9EEF1' },
  { value: 'seasoning', label: '调料', id: 6, color: '#B5765A', bg: '#F0E2DC' },
  { value: 'fruit', label: '水果', id: 7, color: '#C75D4D', bg: '#F5E3DF' },
  { value: 'drink', label: '饮料', id: 8, color: '#9CB088', bg: '#EDF0E4' },
];

// 三大存储分区
export const STORAGE_ZONES: StorageZoneInfo[] = [
  { value: 'refrigerator', label: '冷藏室' },
  { value: 'freezer', label: '冷冻层' },
  { value: 'room', label: '常温' },
];

// 常用单位
export const UNITS: string[] = ['个', '克', '公斤', '把', '条', '瓶', '袋', '桶', '杯', '颗', '块', '盒'];

// 主题色接口（与原应用 --sk-* CSS 变量一致）
export interface Theme {
  bg: string;
  card: string;
  section: string;
  primary: string;
  primaryDark: string;
  primaryBg: string;
  accent: string;
  accentBg: string;
  cold: string;
  coldBg: string;
  text: string;
  text2: string;
  text3: string;
  border: string;
  fresh: string;
  freshBg: string;
  warn: string;
  warnBg: string;
  danger: string;
  dangerBg: string;
  expired: string;
  expiredBg: string;
}

export const THEME: Theme = {
  bg: '#FAF6EF',
  card: '#FFFEFB',
  section: '#F5EFE4',
  primary: '#7C9473',
  primaryDark: '#5E7A56',
  primaryBg: '#EDF1E8',
  accent: '#C97B5C',
  accentBg: '#F7E8DF',
  cold: '#6E8CA8',
  coldBg: '#E8EEF4',
  text: '#3D352E',
  text2: '#8B7E6F',
  text3: '#B5A99A',
  border: '#ECE4D6',
  fresh: '#7C9473',
  freshBg: '#EDF1E8',
  warn: '#D4A55B',
  warnBg: '#FBF1DE',
  danger: '#C75D4D',
  dangerBg: '#FAE6E2',
  expired: '#9A8E80',
  expiredBg: '#EDEAE5',
};

// 按分类默认保质期天数（简化版，覆盖原应用 i 字典常用项）
export const DEFAULT_EXPIRY_DAYS: Record<string, number> = {
  // 蔬菜
  '西红柿': 7, '黄瓜': 7, '土豆': 15, '胡萝卜': 14, '青椒': 5, '青菜': 3, '白菜': 7,
  '洋葱': 30, '大蒜': 30, '生姜': 14, '西兰花': 5, '芹菜': 7, '蘑菇': 3, '茄子': 7,
  '豆角': 5, '菠菜': 3, '生菜': 2, '番茄': 7, '冬瓜': 14, '南瓜': 30,
  // 肉类
  '猪肉': 5, '牛肉': 7, '羊肉': 5, '鸡肉': 5, '鸡蛋': 14, '鸭蛋': 14,
  '牛奶': 7, '酸奶': 7, '奶酪': 30, '黄油': 90,
  // 海鲜
  '鱼': 3, '虾': 3, '蟹': 3, '贝类': 2, '鱿鱼': 3, '三文鱼': 3, '带鱼': 3, '虾仁': 7, '扇贝': 2, '生蚝': 1,
  // 干货
  '大米': 365, '面粉': 180, '酱油': 365, '醋': 365, '盐': 365, '糖': 365,
  '食用油': 365, '花椒': 365, '八角': 365, '桂皮': 365, '干辣椒': 365, '豆瓣酱': 180,
  '豆腐乳': 180, '木耳': 365, '香菇': 365,
  // 冷冻
  '速冻水饺': 180, '速冻包子': 180, '速冻汤圆': 180, '速冻馄饨': 180, '冰淇淋': 180, '冻鱼': 90, '冻肉': 90,
  // 调料
  '料酒': 365, '蚝油': 180, '番茄酱': 180, '沙拉酱': 180, '芝麻油': 365, '孜然': 365, '五香粉': 365,
  // 水果
  '苹果': 10, '香蕉': 5, '橙子': 14, '柠檬': 20, '葡萄': 5, '草莓': 3, '西瓜': 3, '梨': 10, '桃子': 5,
  // 饮料
  '矿泉水': 365, '可乐': 365, '果汁': 30, '啤酒': 180, '功能饮料': 365, '茶饮料': 180,
};

// 分类默认保质期（找不到具体名称时使用）
export const CATEGORY_DEFAULT_DAYS: Record<number, number> = {
  1: 7,   // 蔬菜
  2: 5,   // 肉类
  3: 3,   // 海鲜
  4: 180, // 干货
  5: 90,  // 冷冻
  6: 365, // 调料
  7: 10,  // 水果
  8: 30,  // 饮料
};

// 根据分类值查找
export function getCategoryByValue(value: string): CategoryInfo | undefined {
  return CATEGORIES.find(c => c.value === value);
}

// 根据分类ID查找
export function getCategoryById(id: number): CategoryInfo | undefined {
  return CATEGORIES.find(c => c.id === id);
}

// 获取分类圆点色
export function getCategoryColor(categoryId: number): string {
  const cat = getCategoryById(categoryId);
  return cat ? cat.color : '#B5A99A';
}

// 获取分类标签
export function getCategoryLabel(categoryId: number): string {
  const cat = getCategoryById(categoryId);
  return cat ? cat.label : '其他';
}

// 获取存储分区标签
export function getStorageZoneLabel(zone: string): string {
  const z = STORAGE_ZONES.find(s => s.value === zone);
  return z ? z.label : zone;
}

// 格式化日期为 YYYY-MM-DD
export function formatDate(date: Date): string {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

// 估算保质期天数：优先按名称，其次按分类
export function estimateExpiryDays(name: string, categoryId: number): number {
  if (DEFAULT_EXPIRY_DAYS[name]) {
    return DEFAULT_EXPIRY_DAYS[name];
  }
  return CATEGORY_DEFAULT_DAYS[categoryId] ?? 7;
}
