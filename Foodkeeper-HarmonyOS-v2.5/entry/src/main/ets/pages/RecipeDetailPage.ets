// 食谱详情页（任务4：从 RecipePage 卡片点击进入）
// 展示完整食谱信息：食材清单、步骤说明、烹饪时间、热量、难度等
// 内嵌"完成烹饪"按钮，位置醒目，风格与详情页协调
// 完成烹饪后调用 InventoryStore.consumeIngredients 扣减库存，并提供详细反馈

import { router, promptAction } from '@kit.ArkUI';
import { THEME } from '../model/DataModel';
import { InventoryStore, ConsumeDetail, ConsumeResult } from '../store/InventoryStore';
import { DisplayRecipe, MatchedIngredient, RecipeHolder, RecipeStep } from '../model/RecipeEngine';

// 分类配色（与 RecipePage 保持一致）
const CAT_COLORS: string[] = ['#7C9473', '#C97B5C', '#6E8CA8'];
const CAT_BG: string[] = ['#EDF1E8', '#F7E8DF', '#E8EEF4'];
const CAT_LABELS: string[] = ['快手懒人餐', '家常正餐', '减脂轻食餐'];

// 扣减明细动作的展示信息
interface DetailActionInfo {
  text: string;
  color: string;
  bg: string;
}

@Entry
@Component
struct RecipeDetailPage {
  @State recipe: DisplayRecipe | null = null;
  @State consuming: boolean = false;
  @State showConsumeDetail: boolean = false;
  @State lastResult: ConsumeResult | null = null;

  aboutToAppear(): void {
    // 从 Holder 获取 RecipePage 传入的菜谱数据
    this.recipe = RecipeHolder.get();
    if (this.recipe === null) {
      // 数据异常（如直接通过路由进入）：提示并返回
      promptAction.showToast({ message: '菜谱数据加载失败', duration: 1500 });
      setTimeout(() => { router.back(); }, 800);
    }
  }

  // 任务5：完成烹饪 —— 批量扣减该菜谱所需食材（含单位检查与详细反馈）
  async onCompleteCooking(): Promise<void> {
    if (this.consuming) return;
    if (this.recipe === null) return;
    const recipe = this.recipe as DisplayRecipe;

    // 无任何库存拥有的食材，无可扣减
    if (recipe.haveCount <= 0) {
      promptAction.showToast({ message: '该菜谱暂无库存食材可扣减', duration: 1500 });
      return;
    }
    this.consuming = true;

    try {
      const result = await InventoryStore.getInstance().consumeIngredients(recipe.ingredients, recipe.name);
      this.lastResult = result;

      if (result.ok) {
        // 标记最近完成烹饪的菜谱名（供 RecipePage 返回后标记 completedSet）
        RecipeHolder.setLastCompleted(recipe.name);

        // 显示详细扣减结果
        this.showConsumeDetail = true;
        let msg = `已完成烹饪，扣减 ${result.deducted} 项库存`;
        if (result.insufficient > 0) {
          msg += `（${result.insufficient} 项库存不足已扣到0）`;
        }
        if (result.skipped > 0) {
          msg += `（${result.skipped} 项跳过）`;
        }
        promptAction.showToast({ message: msg, duration: 2000 });
      } else {
        // 全部食材都无法扣减
        this.showConsumeDetail = true;
        promptAction.showToast({ message: '扣减失败：未匹配到可扣减的库存', duration: 1800 });
      }
    } catch (e) {
      promptAction.showToast({ message: '完成烹饪异常：' + String(e), duration: 2000 });
    } finally {
      this.consuming = false;
    }
  }

  // 返回上一页（完成烹饪后由用户手动返回，便于查看扣减明细）
  onBack(): void {
    router.back();
  }

  // 食材剩余天数颜色
  getDaysColor(days: number): string {
    if (days < 0) return THEME.danger;
    if (days <= 1) return THEME.danger;
    if (days <= 3) return THEME.warn;
    return THEME.fresh;
  }

  getDaysText(days: number): string {
    if (days < 0) return `已过期${Math.abs(days)}天`;
    if (days === 0) return '今天过期';
    return `剩${days}天`;
  }

  // 扣减明细的动作文案与颜色
  getDetailActionInfo(action: string): DetailActionInfo {
    if (action === 'deducted') {
      const info: DetailActionInfo = { text: '已扣减', color: THEME.fresh, bg: THEME.freshBg };
      return info;
    }
    if (action === 'deducted_converted') {
      const info: DetailActionInfo = { text: '已换算扣减', color: THEME.cold, bg: THEME.coldBg };
      return info;
    }
    if (action === 'insufficient') {
      const info: DetailActionInfo = { text: '库存不足', color: THEME.warn, bg: THEME.warnBg };
      return info;
    }
    if (action === 'skipped_no_amount') {
      const info: DetailActionInfo = { text: '用量无法量化', color: THEME.text3, bg: THEME.section };
      return info;
    }
    if (action === 'skipped_not_in_inventory') {
      const info: DetailActionInfo = { text: '库存无匹配', color: THEME.text3, bg: THEME.section };
      return info;
    }
    if (action === 'skipped_unit_mismatch') {
      const info: DetailActionInfo = { text: '单位无法换算', color: THEME.danger, bg: THEME.dangerBg };
      return info;
    }
    const info: DetailActionInfo = { text: '未知', color: THEME.text3, bg: THEME.section };
    return info;
  }

  build(): void {
    Column() {
      // 顶部导航栏
      Row() {
        Text('←')
          .fontSize(22)
          .fontColor(THEME.text)
          .onClick(() => this.onBack())
        Text('食谱详情')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text('⭐')
          .fontSize(18)
          .opacity(0)
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .backgroundColor(THEME.card)
      .border({ width: { bottom: 1 }, color: THEME.border })

      if (this.recipe !== null) {
        // 顶部固定操作栏：完成烹饪按钮置顶，浏览食谱时始终可见
        Row() {
          if (this.lastResult !== null && (this.lastResult as ConsumeResult).ok) {
            Text('✓ 已完成烹饪 · 扣减明细见下方')
              .fontSize(13)
              .fontWeight(FontWeight.Bold)
              .fontColor(THEME.fresh)
              .layoutWeight(1)
              .textAlign(TextAlign.Center)
          } else {
            Button(this.consuming ? '处理中...' : ((this.recipe as DisplayRecipe).haveCount > 0 ? '🍳 完成烹饪' : '暂无库存食材'))
              .fontSize(14)
              .fontWeight(FontWeight.Bold)
              .fontColor('#FFFEFB')
              .backgroundColor((this.recipe as DisplayRecipe).haveCount > 0 && !this.consuming ? THEME.fresh : THEME.text3)
              .borderRadius(20)
              .height(40)
              .layoutWeight(1)
              .enabled((this.recipe as DisplayRecipe).haveCount > 0 && !this.consuming)
              .onClick(() => this.onCompleteCooking())
          }
        }
        .width('100%')
        .padding({ left: 14, right: 14, top: 8, bottom: 8 })
        .backgroundColor(THEME.card)
        .border({ width: { bottom: 1 }, color: THEME.border })

        Scroll() {
          Column() {
            // 食谱标题区
            Column() {
              Text((this.recipe as DisplayRecipe).name)
                .fontSize(22)
                .fontWeight(FontWeight.Bold)
                .fontColor(THEME.text)
                .textAlign(TextAlign.Center)
                .width('100%')

              // 分类 + 元数据
              Row() {
                Text(CAT_LABELS[(this.recipe as DisplayRecipe).category])
                  .fontSize(11)
                  .fontColor(CAT_COLORS[(this.recipe as DisplayRecipe).category])
                  .backgroundColor(CAT_BG[(this.recipe as DisplayRecipe).category])
                  .borderRadius(6)
                  .padding({ left: 8, right: 8, top: 3, bottom: 3 })
                Text(`⏱ ${(this.recipe as DisplayRecipe).time}`)
                  .fontSize(12)
                  .fontColor(THEME.text2)
                  .margin({ left: 10 })
                Text(`🔥 ${(this.recipe as DisplayRecipe).calories}`)
                  .fontSize(12)
                  .fontColor(THEME.text2)
                  .margin({ left: 10 })
                Text(`📊 ${(this.recipe as DisplayRecipe).difficulty}`)
                  .fontSize(12)
                  .fontColor(THEME.text2)
                  .margin({ left: 10 })
              }
              .margin({ top: 12 })

              // 匹配率 + 临期标记
              Row() {
                Text(`已备 ${(this.recipe as DisplayRecipe).haveCount}/${(this.recipe as DisplayRecipe).totalCount}`)
                  .fontSize(12)
                  .fontColor((this.recipe as DisplayRecipe).haveCount === (this.recipe as DisplayRecipe).totalCount ? THEME.fresh : THEME.warn)
                  .backgroundColor((this.recipe as DisplayRecipe).haveCount === (this.recipe as DisplayRecipe).totalCount ? THEME.freshBg : THEME.warnBg)
                  .borderRadius(8)
                  .padding({ left: 8, right: 8, top: 3, bottom: 3 })
                if ((this.recipe as DisplayRecipe).expiringCount > 0) {
                  Text(`⚠️ 含${(this.recipe as DisplayRecipe).expiringCount}个临期食材`)
                    .fontSize(11)
                    .fontColor(THEME.danger)
                    .backgroundColor(THEME.dangerBg)
                    .borderRadius(6)
                    .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                    .margin({ left: 8 })
                }
                if ((this.recipe as DisplayRecipe).needBuyNames.length > 0) {
                  Text(`需买${(this.recipe as DisplayRecipe).needBuyNames.length}项`)
                    .fontSize(11)
                    .fontColor(THEME.accent)
                    .backgroundColor(THEME.accentBg)
                    .borderRadius(6)
                    .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                    .margin({ left: 8 })
                }
              }
              .margin({ top: 10 })
            }
            .width('100%')
            .padding({ left: 20, right: 20, top: 20, bottom: 20 })
            .backgroundColor(THEME.card)
            .borderRadius(12)
            .border({ width: 1, color: THEME.border })
            .margin({ top: 14 })

            // 食材清单区
            Column() {
              Text('🥬 食材清单')
                .fontSize(15)
                .fontWeight(FontWeight.Bold)
                .fontColor(THEME.text)
                .width('100%')

              ForEach((this.recipe as DisplayRecipe).ingredients, (ing: MatchedIngredient) => {
                Row() {
                  // 食材名称
                  Text(ing.name)
                    .fontSize(14)
                    .fontWeight(FontWeight.Medium)
                    .fontColor(THEME.text)
                    .layoutWeight(1)
                  // 用量
                  Text(ing.quantity)
                    .fontSize(13)
                    .fontColor(THEME.text2)
                    .margin({ left: 8 })
                  // 状态标签
                  if (ing.haveIt) {
                    Text(this.getDaysText(ing.days))
                      .fontSize(11)
                      .fontColor(this.getDaysColor(ing.days))
                      .margin({ left: 8 })
                    if (ing.isExpiring) {
                      Text('临期')
                        .fontSize(10)
                        .fontColor('#FFFEFB')
                        .backgroundColor(ing.days <= 1 ? THEME.danger : THEME.warn)
                        .borderRadius(4)
                        .padding({ left: 4, right: 4, top: 1, bottom: 1 })
                        .margin({ left: 6 })
                    }
                  } else {
                    Text('需购买')
                      .fontSize(11)
                      .fontColor(THEME.accent)
                      .backgroundColor(THEME.accentBg)
                      .borderRadius(4)
                      .padding({ left: 4, right: 4, top: 1, bottom: 1 })
                      .margin({ left: 8 })
                  }
                }
                .width('100%')
                .padding({ top: 10, bottom: 10 })
                .border({ width: { bottom: 1 }, color: THEME.border })
              }, (ing: MatchedIngredient) => ing.name)
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 14, bottom: 14 })
            .backgroundColor(THEME.card)
            .borderRadius(12)
            .border({ width: 1, color: THEME.border })
            .margin({ top: 12 })

            // 烹饪步骤区（结构化：标题+详细说明+火候/用时标签+小贴士）
            Column() {
              Text('👨‍🍳 烹饪步骤')
                .fontSize(15)
                .fontWeight(FontWeight.Bold)
                .fontColor(THEME.text)
                .width('100%')
                .margin({ bottom: 4 })

              Text(`共 ${(this.recipe as DisplayRecipe).steps} 步 · 按顺序操作即可完成`)
                .fontSize(11)
                .fontColor(THEME.text3)
                .width('100%')
                .margin({ bottom: 10 })

              ForEach((this.recipe as DisplayRecipe).stepList, (step: RecipeStep, idx: number) => {
                Column() {
                  // 步骤头部：序号 + 标题
                  Row() {
                    Text(`${idx + 1}`)
                      .fontSize(13)
                      .fontWeight(FontWeight.Bold)
                      .fontColor('#FFFEFB')
                      .backgroundColor(THEME.primary)
                      .borderRadius(11)
                      .width(22)
                      .height(22)
                      .textAlign(TextAlign.Center)
                      .margin({ right: 8 })
                    Text(step.title)
                      .fontSize(14)
                      .fontWeight(FontWeight.Bold)
                      .fontColor(THEME.text)
                      .layoutWeight(1)
                  }
                  .width('100%')
                  .alignItems(VerticalAlign.Center)

                  // 火候 + 用时 标签
                  Row() {
                    Text(`🔥 ${step.heat}`)
                      .fontSize(11)
                      .fontColor(THEME.accent)
                      .backgroundColor(THEME.accentBg)
                      .borderRadius(6)
                      .padding({ left: 7, right: 7, top: 2, bottom: 2 })
                    Text(`⏱ ${step.duration}`)
                      .fontSize(11)
                      .fontColor(THEME.text2)
                      .backgroundColor(THEME.section)
                      .borderRadius(6)
                      .padding({ left: 7, right: 7, top: 2, bottom: 2 })
                      .margin({ left: 6 })
                  }
                  .width('100%')
                  .margin({ top: 8, left: 30 })

                  // 详细操作说明
                  Text(step.detail)
                    .fontSize(13)
                    .fontColor(THEME.text2)
                    .lineHeight(21)
                    .width('100%')
                    .margin({ top: 8, left: 30 })

                  // 小贴士 / 注意事项
                  if (step.tip.length > 0) {
                    Row() {
                      Column() {
                        Text('💡 小贴士')
                          .fontSize(11)
                          .fontWeight(FontWeight.Bold)
                          .fontColor(THEME.warn)
                          .margin({ bottom: 3 })
                        Text(step.tip)
                          .fontSize(12)
                          .fontColor(THEME.text2)
                          .lineHeight(19)
                        }
                      .layoutWeight(1)
                    }
                    .width('100%')
                    .backgroundColor(THEME.warnBg)
                    .borderRadius(8)
                    .border({ width: { left: 3 }, color: THEME.warn })
                    .padding({ left: 10, right: 10, top: 8, bottom: 8 })
                    .margin({ top: 8, left: 30 })
                  }
                }
                .width('100%')
                .padding({ left: 12, right: 12, top: 12, bottom: 12 })
                .backgroundColor(THEME.bg)
                .borderRadius(10)
                .border({ width: 1, color: THEME.border })
                .margin({ top: 10 })
              }, (step: RecipeStep, idx: number) => `${idx}-${step.title}`)
            }
            .width('100%')
            .padding({ left: 16, right: 16, top: 14, bottom: 14 })
            .backgroundColor(THEME.card)
            .borderRadius(12)
            .border({ width: 1, color: THEME.border })
            .margin({ top: 12 })

            // 任务5：扣减明细（完成烹饪后展示）
            if (this.showConsumeDetail && this.lastResult !== null) {
              Column() {
                Text('📋 库存扣减明细')
                  .fontSize(15)
                  .fontWeight(FontWeight.Bold)
                  .fontColor(THEME.text)
                  .width('100%')
                  .margin({ bottom: 8 })

                ForEach((this.lastResult as ConsumeResult).details, (d: ConsumeDetail, idx: number) => {
                  Row() {
                    Text(d.name)
                      .fontSize(13)
                      .fontWeight(FontWeight.Medium)
                      .fontColor(THEME.text)
                      .layoutWeight(1)
                    Text(this.getDetailActionInfo(d.action).text)
                      .fontSize(11)
                      .fontColor(this.getDetailActionInfo(d.action).color)
                      .backgroundColor(this.getDetailActionInfo(d.action).bg)
                      .borderRadius(4)
                      .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                      .margin({ left: 8 })
                  }
                  .width('100%')
                  .padding({ top: 8, bottom: 8 })
                  .border({ width: { bottom: 1 }, color: THEME.border })

                  // 扣减数量详情（含已扣减、已换算扣减、库存不足三种）
                  if (d.action === 'deducted' || d.action === 'deducted_converted' || d.action === 'insufficient') {
                    Row() {
                      Text(`  ${d.beforeQty}${d.inventoryUnit} → ${d.afterQty}${d.inventoryUnit}（扣减 ${d.deductedAmount}${d.inventoryUnit}）`)
                        .fontSize(11)
                        .fontColor(THEME.text3)
                        .layoutWeight(1)
                    }
                    .width('100%')
                    .padding({ left: 8, bottom: 4 })
                    // 展示换算依据（如"按 1个≈150克 折算"），便于用户核对
                    if (d.conversionNote.length > 0) {
                      Row() {
                        Text(`  💡 菜谱「${d.recipeAmount}」换算 → ${d.deductedAmount}${d.inventoryUnit}（${d.conversionNote}）`)
                          .fontSize(10)
                          .fontColor(THEME.cold)
                          .layoutWeight(1)
                      }
                      .width('100%')
                      .padding({ left: 8, bottom: 6 })
                    }
                  } else if (d.action === 'skipped_unit_mismatch') {
                    // 单位无法换算：展示菜谱单位、库存单位及失败原因，引导用户手动处理
                    Row() {
                      Text(`  菜谱「${d.recipeAmount}」需 ${d.recipeUnit}，库存为 ${d.beforeQty}${d.inventoryUnit}`)
                        .fontSize(11)
                        .fontColor(THEME.danger)
                        .layoutWeight(1)
                    }
                    .width('100%')
                    .padding({ left: 8, bottom: 2 })
                    Row() {
                      Text(`  ⚠️ ${d.conversionNote.length > 0 ? d.conversionNote : '单位无法自动换算，请手动调整库存或菜谱单位后重试'}`)
                        .fontSize(10)
                        .fontColor(THEME.danger)
                        .layoutWeight(1)
                    }
                    .width('100%')
                    .padding({ left: 8, bottom: 6 })
                  }
                }, (d: ConsumeDetail, idx: number) => `${d.name}-${idx}`)
              }
              .width('100%')
              .padding({ left: 16, right: 16, top: 14, bottom: 14 })
              .backgroundColor(THEME.card)
              .borderRadius(12)
              .border({ width: 1, color: THEME.border })
              .margin({ top: 12 })
            }
          }
          .width('100%')
          .padding({ bottom: 40 })
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
      } else {
        // 数据加载中/异常
        Column() {
          Text('菜谱数据加载中...')
            .fontSize(14)
            .fontColor(THEME.text3)
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(THEME.bg)
  }
}
