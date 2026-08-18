// 过期状态计算工具（对应原应用 He()、Qe()、sh()、Cc()）

import { FoodItem, getCategoryColor, getCategoryLabel } from '../model/DataModel';

// 过期状态类型
export type ExpiryStatus = 'green' | 'yellow' | 'red' | 'expired';

// 状态优先级接口（用于排序：expired < red < yellow < green）
export interface StatusPriorityMap {
  expired: number;
  red: number;
  yellow: number;
  green: number;
}

export interface StatusStringMap {
  green: string;
  yellow: string;
  red: string;
  expired: string;
}

// 状态优先级（用于排序：expired < red < yellow < green）
export const STATUS_PRIORITY: StatusPriorityMap = {
  expired: 0,
  red: 1,
  yellow: 2,
  green: 3,
};

// 状态对应样式名（与原 dotClass 映射一致）
export const STATUS_CLASS: StatusStringMap = {
  green: 'fresh',
  yellow: 'warn',
  red: 'danger',
  expired: 'expired',
};

// 状态对应颜色
export const STATUS_COLOR: StatusStringMap = {
  green: '#7C9473',
  yellow: '#D4A55B',
  red: '#C75D4D',
  expired: '#9A8E80',
};

// 状态对应背景色
export const STATUS_BG: StatusStringMap = {
  green: '#EDF1E8',
  yellow: '#FBF1DE',
  red: '#FAE6E2',
  expired: '#EDEAE5',
};

// 获取状态优先级
export function getStatusPriority(status: ExpiryStatus): number {
  switch (status) {
    case 'expired': return 0;
    case 'red': return 1;
    case 'yellow': return 2;
    case 'green': return 3;
    default: return 99;
  }
}

// 获取状态颜色
export function getStatusColor(status: ExpiryStatus): string {
  switch (status) {
    case 'green': return STATUS_COLOR.green;
    case 'yellow': return STATUS_COLOR.yellow;
    case 'red': return STATUS_COLOR.red;
    case 'expired': return STATUS_COLOR.expired;
    default: return '#9A8E80';
  }
}

// 获取状态背景色
export function getStatusBg(status: ExpiryStatus): string {
  switch (status) {
    case 'green': return STATUS_BG.green;
    case 'yellow': return STATUS_BG.yellow;
    case 'red': return STATUS_BG.red;
    case 'expired': return STATUS_BG.expired;
    default: return '#EDEAE5';
  }
}

// 获取状态样式名
export function getStatusClass(status: ExpiryStatus): string {
  switch (status) {
    case 'green': return STATUS_CLASS.green;
    case 'yellow': return STATUS_CLASS.yellow;
    case 'red': return STATUS_CLASS.red;
    case 'expired': return STATUS_CLASS.expired;
    default: return 'expired';
  }
}

// 计算距过期天数（负数表示已过期）
export function daysUntilExpiry(expiryDate: string): number {
  const today = new Date();
  today.setHours(0, 0, 0, 0);
  const expiry = new Date(expiryDate);
  expiry.setHours(0, 0, 0, 0);
  const diff = expiry.getTime() - today.getTime();
  return Math.floor(diff / (24 * 60 * 60 * 1000));
}

// 根据天数判定状态（对应原 Qe()）
// 阈值：已过期(<0) / 紧急(0-1天) / 临期(2-3天) / 新鲜(>3天)
export function getExpiryStatus(days: number): ExpiryStatus {
  if (days < 0) return 'expired';
  if (days <= 1) return 'red';
  if (days <= 3) return 'yellow';
  return 'green';
}

// 获取天数描述文本（对应原 Cc().daysText）
export function getDaysText(days: number): string {
  if (days < 0) return `已过期 ${Math.abs(days)}天`;
  if (days === 0) return '今天过期';
  return `剩 ${days} 天`;
}

// 食材卡片展示信息（对应原 Cc()）
export interface FoodCardInfo {
  status: ExpiryStatus;
  statusClass: string;
  statusColor: string;
  statusBg: string;
  daysText: string;
  catDot: string;
  catLabel: string;
}

// 计算食材卡片展示信息
export function getFoodCardInfo(food: FoodItem): FoodCardInfo {
  const days = daysUntilExpiry(food.expiryDate);
  const status = getExpiryStatus(days);
  return {
    status,
    statusClass: getStatusClass(status),
    statusColor: getStatusColor(status),
    statusBg: getStatusBg(status),
    daysText: getDaysText(days),
    catDot: getCategoryColor(food.categoryId),
    catLabel: getCategoryLabel(food.categoryId),
  };
}

// 判断是否为临期食材（yellow/red/expired）
export function isExpiring(food: FoodItem): boolean {
  const status = getExpiryStatus(daysUntilExpiry(food.expiryDate));
  return status === 'yellow' || status === 'red' || status === 'expired';
}
