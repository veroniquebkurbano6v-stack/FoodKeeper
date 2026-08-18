// 小艺开放平台智能体网关
// 职责：1) 管理 agentId；2) 检测智能体可用性（真机 + agentId 已配置）；
//       3) 基于库存构造 queryText，把食材上下文喂给智能体；
//       4) 持有降级信号，供 AgentRecipePage 出错时通知 RecipePage 走本地引擎。
//
// 安全说明：本文件不 import @kit.AgentFrameworkKit，避免在应用启动/Tab 切换时
// 加载智能体 HSP。AgentFrameworkKit 仅在 AgentRecipePage（独立路由页）内静态 import，
// 按需加载，模拟器不进入该页即不会触发 HSP 加载崩溃。

import bundleManager from '@ohos.bundle.bundleManager';
import { FoodItem } from '../model/DataModel';
import { daysUntilExpiry } from './ExpiryHelper';

// 小艺开放平台智能体 ID（在开发者后台创建智能体后获取）
const AGENT_ID = 'agent194f695d05434d089034fbabfc07bf32';

// 小艺智能体 HSP 包名（缺失时 import @kit.AgentFrameworkKit 会 SIGABRT 崩溃）
const HSP_BUNDLE_NAME = 'com.huawei.hmsapp.hiai.hsp';

// 降级信号 Holder（AgentRecipePage 出错时置位，RecipePage 检测后走本地引擎）
export class AgentHolder {
  private static fallbackFlag: boolean = false;
  static setFallback(): void { AgentHolder.fallbackFlag = true; }
  static consumeFallback(): boolean {
    const v = AgentHolder.fallbackFlag;
    AgentHolder.fallbackFlag = false;
    return v;
  }
}

export class AgentBridge {
  // 获取配置的智能体 ID
  static getAgentId(): string {
    return AGENT_ID;
  }

  // 检测智能体是否可用：agentId 已配置即可调用
  // 运行时失败（HSP 不可用/网络异常）由 AgentRecipePage.onError 兜底降级到本地引擎
  static isAvailable(): boolean {
    return AGENT_ID.length > 0;
  }

  // 异步检测小艺智能体 HSP 是否已安装
  // 必须在 router.pushUrl 导航到 AgentRecipePage 之前调用：
  //   HSP 缺失时，AgentRecipePage 顶部的 import @kit.AgentFrameworkKit 会触发
  //   LoadJSPandaFile 失败 → SIGABRT 致命崩溃（无法被 JS try-catch 捕获）
  // 通过 bundleManager 查询 HSP 包名是否存在于设备上，缺失则降级本地引擎
  static async checkHspAvailable(): Promise<boolean> {
    if (AGENT_ID.length === 0) {
      return false;
    }
    try {
      await bundleManager.getBundleInfo(HSP_BUNDLE_NAME,
        bundleManager.BundleFlag.GET_BUNDLE_INFO_DEFAULT);
      return true;
    } catch (e) {
      // HSP 未安装或无权限查询：保守视为不可用，避免导航后崩溃
      return false;
    }
  }

  // 基于库存构造智能体 queryText（把食材清单 + 临期信息喂给小艺）
  static buildQuery(foods: FoodItem[]): string {
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
    if (available.length === 0) {
      return '我的库存为空，请推荐一些适合日常采购的菜谱';
    }
    const expText = expiring.length > 0 ? `。其中临期食材：${expiring.join('、')}，请优先用临期食材` : '';
    return `我的库存有：${available.join('、')}${expText}。请推荐 5-7 道菜谱，包含菜名、食材用量和简要步骤`;
  }
}
