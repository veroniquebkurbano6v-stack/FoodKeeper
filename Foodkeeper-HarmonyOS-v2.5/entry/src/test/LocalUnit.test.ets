import { describe, beforeAll, beforeEach, afterEach, afterAll, it, expect } from '@ohos/hypium';
import { convertAmount, canConvert } from '../main/ets/utils/UnitConverter';

export default function localUnitTest() {
  describe('localUnitTest', () => {
    beforeAll(() => {
    });
    beforeEach(() => {
    });
    afterEach(() => {
    });
    afterAll(() => {
    });

    // ===== 通用重量单位换算测试 =====
    describe('通用重量单位换算', () => {
      it('克与公斤互转应正确', 0, () => {
        // 1公斤 = 1000克
        const r1 = convertAmount(1, '公斤', '克', '');
        expect(r1.ok).assertTrue();
        expect(r1.value).assertEqual(1000);
        // 1000克 = 1公斤
        const r2 = convertAmount(1000, '克', '公斤', '');
        expect(r2.ok).assertTrue();
        expect(r2.value).assertEqual(1);
      });

      it('克与斤互转应正确', 0, () => {
        // 1斤 = 500克
        const r1 = convertAmount(1, '斤', '克', '');
        expect(r1.ok).assertTrue();
        expect(r1.value).assertEqual(500);
        // 500克 = 1斤
        const r2 = convertAmount(500, '克', '斤', '');
        expect(r2.ok).assertTrue();
        expect(r2.value).assertEqual(1);
      });

      it('克与两互转应正确', 0, () => {
        // 1两 = 50克
        const r1 = convertAmount(1, '两', '克', '');
        expect(r1.ok).assertTrue();
        expect(r1.value).assertEqual(50);
        // 100克 = 2两
        const r2 = convertAmount(100, '克', '两', '');
        expect(r2.ok).assertTrue();
        expect(r2.value).assertEqual(2);
      });

      it('公斤与斤互转应正确', 0, () => {
        // 1公斤 = 2斤
        const r1 = convertAmount(1, '公斤', '斤', '');
        expect(r1.ok).assertTrue();
        expect(r1.value).assertEqual(2);
      });

      it('同单位应直接返回原值', 0, () => {
        const r = convertAmount(200, '克', '克', '');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(200);
      });

      it('单位别名归一化（千克=公斤，g=克）', 0, () => {
        const r1 = convertAmount(1, '千克', '克', '');
        expect(r1.ok).assertTrue();
        expect(r1.value).assertEqual(1000);
        const r2 = convertAmount(500, 'g', '斤', '');
        expect(r2.ok).assertTrue();
        expect(r2.value).assertEqual(1);
      });
    });

    // ===== 食材专属单位换算测试 =====
    describe('食材专属单位换算', () => {
      it('猪肉 斤→克 应正确换算', 0, () => {
        // 菜谱"1斤"猪肉，库存以"克"计 → 500克
        const r = convertAmount(1, '斤', '克', '猪肉');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(500);
      });

      it('猪肉 克→斤 应正确换算', 0, () => {
        // 菜谱"200克"猪肉，库存以"斤"计 → 0.4斤
        const r = convertAmount(200, '克', '斤', '猪肉');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(0.4);
      });

      it('鸡蛋 个→克 应正确换算', 0, () => {
        // 菜谱"3个"鸡蛋，库存以"克"计 → 150克
        const r = convertAmount(3, '个', '克', '鸡蛋');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(150);
      });

      it('鸡蛋 克→个 应正确换算（反向）', 0, () => {
        // 菜谱"100克"鸡蛋，库存以"个"计 → 2个
        const r = convertAmount(100, '克', '个', '鸡蛋');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(2);
      });

      it('金针菇 袋→克 应正确换算', 0, () => {
        // 库存"1袋"金针菇，菜谱需"200克" → 库存扣减200克（袋→克）
        const r = convertAmount(1, '袋', '克', '金针菇');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(200);
      });

      it('金针菇 克→袋 应正确换算（典型场景）', 0, () => {
        // 菜谱"200克"金针菇，库存以"袋"计（1袋=200克）→ 1袋
        const r = convertAmount(200, '克', '袋', '金针菇');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(1);
      });

      it('西红柿 个→克 应正确换算', 0, () => {
        // 菜谱"2个"西红柿，库存以"克"计 → 300克
        const r = convertAmount(2, '个', '克', '西红柿');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(300);
      });

      it('大蒜 瓣→克 应正确换算', 0, () => {
        // 菜谱"5瓣"大蒜，库存以"克"计 → 25克
        const r = convertAmount(5, '瓣', '克', '大蒜');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(25);
      });

      it('生姜 片→克 应正确换算', 0, () => {
        // 菜谱"3片"生姜，库存以"克"计 → 9克
        const r = convertAmount(3, '片', '克', '生姜');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(9);
      });

      it('换算成功应附带换算备注', 0, () => {
        const r = convertAmount(2, '个', '克', '鸡蛋');
        expect(r.ok).assertTrue();
        expect(r.note !== undefined && r.note.length > 0).assertTrue();
        expect(r.note as string).assertContain('鸡蛋');
      });

      it('通用重量单位换算不应附带备注', 0, () => {
        const r = convertAmount(1, '公斤', '克', '');
        expect(r.ok).assertTrue();
        expect(r.note === undefined || r.note.length === 0).assertTrue();
      });
    });

    // ===== 通用估算（兜底）换算测试 =====
    describe('通用估算换算（兜底）', () => {
      it('未配置食材的"个"单位应按通用估算换算', 0, () => {
        // "某新食材"未在表中，1个按100克估算
        const r = convertAmount(2, '个', '克', '某新食材');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(200);
      });

      it('未配置食材的"把"单位应按通用估算换算', 0, () => {
        // "某新食材"未在表中，1把按150克估算
        const r = convertAmount(1, '把', '克', '某新食材');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(150);
      });
    });

    // ===== 换算失败场景测试 =====
    describe('换算失败场景', () => {
      it('两端单位均为未知时应换算失败', 0, () => {
        // "适量"和"少许"都不是已知单位
        const r = convertAmount(1, '适量', '少许', '猪肉');
        expect(r.ok).assertFalse();
        expect(r.reason !== undefined && r.reason.length > 0).assertTrue();
      });

      it('换算失败原因应包含食材名与单位信息', 0, () => {
        const r = convertAmount(1, '适量', '少许', '牛肉');
        expect(r.ok).assertFalse();
        const reason = r.reason as string;
        expect(reason).assertContain('牛肉');
        expect(reason).assertContain('适量');
        expect(reason).assertContain('少许');
      });

      it('负数数量应换算失败', 0, () => {
        const r = convertAmount(-5, '克', '公斤', '');
        expect(r.ok).assertFalse();
      });
    });

    // ===== canConvert 预判函数测试 =====
    describe('canConvert 预判', () => {
      it('同单位应可换算', 0, () => {
        expect(canConvert('克', '克', '')).assertTrue();
      });

      it('通用重量单位间应可换算', 0, () => {
        expect(canConvert('克', '公斤', '')).assertTrue();
        expect(canConvert('斤', '两', '')).assertTrue();
      });

      it('食材专属单位间应可换算', 0, () => {
        expect(canConvert('个', '克', '鸡蛋')).assertTrue();
        expect(canConvert('袋', '克', '金针菇')).assertTrue();
      });

      it('未知单位应不可换算', 0, () => {
        expect(canConvert('适量', '克', '猪肉')).assertFalse();
      });
    });

    // ===== 典型业务场景集成测试 =====
    describe('典型业务场景', () => {
      it('场景1: 库存猪肉500克 vs 菜谱200克 → 应扣减200克', 0, () => {
        // 模拟库存：猪肉 500克；菜谱：猪肉 200克
        // 同单位直接扣减
        const r = convertAmount(200, '克', '克', '猪肉');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(200);
        // 扣减后剩余：500 - 200 = 300克
      });

      it('场景2: 库存猪肉1斤 vs 菜谱200克 → 应扣减0.4斤', 0, () => {
        // 模拟库存：猪肉 1斤（=500克）；菜谱：猪肉 200克
        // 单位不一致，斤→克 换算
        const r = convertAmount(200, '克', '斤', '猪肉');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(0.4);
        // 库存 1斤 - 0.4斤 = 0.6斤
      });

      it('场景3: 库存金针菇1袋 vs 菜谱200克 → 应扣减1袋', 0, () => {
        // 模拟库存：金针菇 1袋（=200克）；菜谱：金针菇 200克
        // 单位不一致，克→袋 换算
        const r = convertAmount(200, '克', '袋', '金针菇');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(1);
        // 库存 1袋 - 1袋 = 0袋
      });

      it('场景4: 库存鸡蛋10个 vs 菜谱3个 → 同单位扣减3个', 0, () => {
        const r = convertAmount(3, '个', '个', '鸡蛋');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(3);
        // 库存 10个 - 3个 = 7个
      });

      it('场景5: 库存鸡蛋10个 vs 菜谱100克 → 应扣减2个', 0, () => {
        // 模拟库存：鸡蛋 10个；菜谱：鸡蛋 100克
        // 单位不一致，克→个 换算（1个=50克）
        const r = convertAmount(100, '克', '个', '鸡蛋');
        expect(r.ok).assertTrue();
        expect(r.value).assertEqual(2);
        // 库存 10个 - 2个 = 8个
      });

      it('场景6: 库存大米5公斤 vs 菜谱1碗 → 应扣减0.2公斤', 0, () => {
        // 模拟库存：大米 5公斤；菜谱：大米 1碗（=200克）
        // 单位不一致，碗→公斤 换算
        const r = convertAmount(1, '碗', '公斤', '大米');
        expect(r.ok).assertTrue();
        // 1碗=200克=0.2公斤
        expect(r.value).assertEqual(0.2);
      });

      it('场景7: 库存生抽1瓶 vs 菜谱2勺 → 应扣减30克（按克计）或0.06瓶', 0, () => {
        // 模拟库存：生抽 1瓶（=500克）；菜谱：生抽 2勺（=30克）
        // 单位不一致，勺→瓶 换算
        const r = convertAmount(2, '勺', '瓶', '生抽');
        expect(r.ok).assertTrue();
        // 2勺=30克，1瓶=500克 → 0.06瓶
        expect(Math.abs(r.value - 0.06) < 0.001).assertTrue();
      });
    });

    // 原有断言测试保留
    it('assertContain', 0, () => {
      let a = 'abc';
      let b = 'b';
      expect(a).assertContain(b);
      expect(a).assertEqual(a);
    });
  });
}
