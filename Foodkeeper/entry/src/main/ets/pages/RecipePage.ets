// AI 菜谱推荐页面（底部导航第3个 Tab）
// 自动基于库存食材生成 5-7 道菜谱，临期食材优先使用
// 移除了 AI 对话交互界面，替换为醒目的"重新生成食谱"按钮
// 每次进入页面若库存有变更，自动触发菜谱生成

import { promptAction } from '@kit.ArkUI';
import { FoodItem, THEME } from '../model/DataModel';
import { daysUntilExpiry } from '../utils/ExpiryHelper';
import { InventoryStore } from '../store/InventoryStore';
import { DisplayRecipe, MatchedIngredient, generateRecipes } from '../model/RecipeEngine';

// 分类配色（0=快手懒人餐, 1=家常正餐, 2=减脂轻食餐）
const CAT_COLORS: string[] = ['#7C9473', '#C97B5C', '#6E8CA8'];
const CAT_BG: string[] = ['#EDF1E8', '#F7E8DF', '#E8EEF4'];
const CAT_LABELS: string[] = ['快手懒人餐', '家常正餐', '减脂轻食餐'];

// 分类导航标签
const NAV_TABS: string[] = ['全部', '快手懒人餐', '减脂轻食餐', '家常正餐', '我的菜谱'];

@Component
export struct RecipePage {
  // ===== 从父组件接收当前 Tab 索引，用于检测切换进入 =====
  @Prop @Watch('onTabChange') activeTabIndex: number = 0;

  // ===== 状态 =====
  @State foods: FoodItem[] = [];
  @State recipes: DisplayRecipe[] = [];
  @State availableCount: number = 0;
  @State expiringCount: number = 0;
  @State expiredCount: number = 0;
  @State currentNav: number = 0;
  @State loading: boolean = true;
  @State lastGeneratedVersion: number = -1;
  @State favoriteSet: Set<string> = new Set();
  @State expandedSet: Set<string> = new Set();

  // ===== 生命周期 =====

  aboutToAppear(): void {
    this.loadData();
    this.autoGenerateRecipes();
  }

  // Tab 切换时自动检测库存变更并重新生成
  onTabChange(): void {
    if (this.activeTabIndex === 2) {
      const version = InventoryStore.getInstance().getVersion();
      if (version !== this.lastGeneratedVersion) {
        this.autoGenerateRecipes();
      }
    }
  }

  // ===== 数据加载 =====

  loadData(): void {
    this.foods = InventoryStore.getInstance().getFoods();
    this.updateStats();
  }

  // 更新食材统计（可用 / 临期 / 已过期）
  updateStats(): void {
    let available = 0;
    let expiring = 0;
    let expired = 0;
    for (let i = 0; i < this.foods.length; i++) {
      const f = this.foods[i];
      if (f.name.length > 0 && f.quantity > 0) {
        available++;
        const days = daysUntilExpiry(f.expiryDate);
        if (days < 0) {
          expired++;
        } else if (days <= 3) {
          expiring++;
        }
      }
    }
    this.availableCount = available;
    this.expiringCount = expiring;
    this.expiredCount = expired;
  }

  // ===== 自动生成菜谱（模拟 AI 调用，含加载延迟） =====

  async autoGenerateRecipes(): Promise<void> {
    this.loading = true;
    this.lastGeneratedVersion = InventoryStore.getInstance().getVersion();

    // 模拟 AI 处理延迟
    await new Promise<void>(resolve => setTimeout(resolve, 1200));

    // 重新加载最新库存数据
    this.loadData();
    this.recipes = generateRecipes(this.foods);
    this.loading = false;
  }

  // ===== 交互 =====

  onRegenerate(): void {
    if (this.loading) {
      return;
    }
    this.autoGenerateRecipes();
  }

  onNavTap(index: number): void {
    this.currentNav = index;
  }

  onFavorite(name: string): void {
    const next = new Set<string>(this.favoriteSet);
    if (next.has(name)) {
      next.delete(name);
      this.favoriteSet = next;
    } else {
      next.add(name);
      this.favoriteSet = next;
      promptAction.showToast({ message: '已收藏', duration: 1500 });
    }
  }

  onToggleExpand(name: string): void {
    const next = new Set<string>(this.expandedSet);
    if (next.has(name)) {
      next.delete(name);
    } else {
      next.add(name);
    }
    this.expandedSet = next;
  }

  // 当前分类过滤后的菜谱
  getFilteredRecipes(): DisplayRecipe[] {
    if (this.currentNav === 0) {
      return this.recipes;
    } else if (this.currentNav === 1) {
      return this.recipes.filter((r: DisplayRecipe) => r.category === 0);
    } else if (this.currentNav === 2) {
      return this.recipes.filter((r: DisplayRecipe) => r.category === 2);
    } else if (this.currentNav === 3) {
      return this.recipes.filter((r: DisplayRecipe) => r.category === 1);
    } else {
      return this.recipes.filter((r: DisplayRecipe) => this.favoriteSet.has(r.name));
    }
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

  // ===== UI =====

  build(): void {
    Column() {
      // 1. 顶部标题栏
      Row() {
        Text('AI 菜谱推荐')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)

        Text(`v3`)
          .fontSize(10)
          .fontColor(THEME.text3)
          .margin({ right: 14 })
      }
      .width('100%')
      .height(48)
      .padding({ left: 14, right: 14 })
      .backgroundColor(THEME.card)
      .border({ width: { bottom: 1 }, color: THEME.border })

      // 2. 食材统计条
      Row() {
        Text(`可用 ${this.availableCount}`)
          .fontSize(11)
          .fontColor(THEME.primary)
          .backgroundColor(THEME.primaryBg)
          .borderRadius(10)
          .padding({ left: 8, right: 8, top: 3, bottom: 3 })

        if (this.expiringCount > 0) {
          Text(`临期 ${this.expiringCount}`)
            .fontSize(11)
            .fontColor(THEME.warn)
            .backgroundColor(THEME.warnBg)
            .borderRadius(10)
            .padding({ left: 8, right: 8, top: 3, bottom: 3 })
            .margin({ left: 6 })
        }

        Blank()

        if (this.expiredCount > 0) {
          Text(`已过期 ${this.expiredCount}`)
            .fontSize(11)
            .fontColor(THEME.danger)
            .backgroundColor(THEME.dangerBg)
            .borderRadius(10)
            .padding({ left: 8, right: 8, top: 3, bottom: 3 })
        }
      }
      .width('100%')
      .padding({ left: 14, right: 14, top: 8, bottom: 8 })

      // 3. 重新生成按钮（醒目，带视觉反馈）
      Button(this.loading ? 'AI 正在生成菜谱...' : '🔄  重新生成食谱')
        .fontSize(15)
        .fontWeight(FontWeight.Bold)
        .fontColor('#FFFEFB')
        .backgroundColor(this.loading ? THEME.text3 : THEME.primary)
        .borderRadius(24)
        .height(44)
        .width('100%')
        .margin({ left: 14, right: 14, bottom: 8 })
        .enabled(!this.loading)
        .onClick(() => this.onRegenerate())

      // 4. 分类导航
      Scroll() {
        Row() {
          ForEach(NAV_TABS, (tab: string, index: number) => {
            Column() {
              Text(tab)
                .fontSize(13)
                .fontWeight(this.currentNav === index ? FontWeight.Bold : FontWeight.Regular)
                .fontColor(this.currentNav === index ? THEME.text : THEME.text3)
              Divider()
                .strokeWidth(this.currentNav === index ? 2 : 0)
                .color(THEME.primary)
                .width(24)
                .margin({ top: 4 })
            }
            .padding({ left: 4, right: 4, top: 6, bottom: 4 })
            .margin({ right: 16 })
            .onClick(() => this.onNavTap(index))
          }, (tab: string) => tab)
        }
        .padding({ left: 14, right: 14 })
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .width('100%')
      .border({ width: { bottom: 1 }, color: THEME.border })

      // 5. 菜谱卡片列表
      if (this.loading) {
        Column() {
          LoadingProgress().width(40).height(40).color(THEME.primary)
          Text('正在分析库存食材，生成菜谱...')
            .fontSize(13)
            .fontColor(THEME.text3)
            .margin({ top: 12 })
          Text('临期食材将被优先使用')
            .fontSize(11)
            .fontColor(THEME.text3)
            .margin({ top: 4 })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        Scroll() {
          Column() {
            ForEach(this.getFilteredRecipes(), (recipe: DisplayRecipe) => {
              this.RecipeCard(recipe)
            }, (recipe: DisplayRecipe) => recipe.name)

            // 空状态
            if (this.getFilteredRecipes().length === 0) {
              Column() {
                Text('🍽️')
                  .fontSize(28)
                Text(this.currentNav === 4 ? '还没有收藏的菜谱' : '该分类暂无菜谱')
                  .fontSize(13)
                  .fontColor(THEME.text3)
                  .margin({ top: 8 })
              }
              .width('100%')
              .padding({ top: 32, bottom: 32 })
              .alignItems(HorizontalAlign.Center)
            }
          }
          .width('100%')
          .padding({ left: 14, right: 14, top: 10, bottom: 12 })
        }
        .layoutWeight(1)
        .scrollBar(BarState.Off)
        .align(Alignment.TopStart)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(THEME.bg)
  }

  // ===== 菜谱卡片 =====

  @Builder
  RecipeCard(recipe: DisplayRecipe): void {
    Column() {
      // 标题行
      Row() {
        Text(recipe.name)
          .fontSize(17)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
          .layoutWeight(1)

        // 匹配率徽章
        Text(`已备 ${recipe.haveCount}/${recipe.totalCount}`)
          .fontSize(10)
          .fontColor(recipe.haveCount === recipe.totalCount ? THEME.fresh : THEME.warn)
          .backgroundColor(recipe.haveCount === recipe.totalCount ? THEME.freshBg : THEME.warnBg)
          .borderRadius(8)
          .padding({ left: 6, right: 6, top: 2, bottom: 2 })
          .margin({ left: 6 })
      }
      .width('100%')

      // 分类 + 元数据 + 临期标记（合并一行）
      Row() {
        Text(CAT_LABELS[recipe.category])
          .fontSize(10)
          .fontColor(CAT_COLORS[recipe.category])
          .backgroundColor(CAT_BG[recipe.category])
          .borderRadius(6)
          .padding({ left: 5, right: 5, top: 1, bottom: 1 })
        Text(`⏱ ${recipe.time}`).fontSize(11).fontColor(THEME.text3).margin({ left: 8 })
        Text(`🔥 ${recipe.calories}`).fontSize(11).fontColor(THEME.text3).margin({ left: 8 })
        Text(`📊 ${recipe.difficulty}`).fontSize(11).fontColor(THEME.text3).margin({ left: 8 })
        if (recipe.expiringCount > 0) {
          Text(`⚠️ 含${recipe.expiringCount}个临期食材`)
            .fontSize(10)
            .fontColor(THEME.danger)
            .backgroundColor(THEME.dangerBg)
            .borderRadius(6)
            .padding({ left: 5, right: 5, top: 1, bottom: 1 })
            .margin({ left: 6 })
        }
        if (recipe.needBuyNames.length > 0) {
          Text(`需买${recipe.needBuyNames.length}`)
            .fontSize(10)
            .fontColor(THEME.accent)
            .backgroundColor(THEME.accentBg)
            .borderRadius(6)
            .padding({ left: 5, right: 5, top: 1, bottom: 1 })
            .margin({ left: 6 })
        }
      }
      .width('100%')
      .margin({ top: 8 })

      // 食材标签
      Flex({ wrap: FlexWrap.Wrap }) {
        ForEach(recipe.ingredients, (ing: MatchedIngredient) => {
          Row() {
            Text(`${ing.name} ${ing.quantity}`)
              .fontSize(12)
              .fontColor(THEME.text)
            if (ing.haveIt) {
              Text(this.getDaysText(ing.days))
                .fontSize(10)
                .fontColor(this.getDaysColor(ing.days))
                .margin({ left: 4 })
              // 临期食材标记
              if (ing.isExpiring) {
                Text('临期')
                  .fontSize(9)
                  .fontColor('#FFFEFB')
                  .backgroundColor(ing.days <= 1 ? THEME.danger : THEME.warn)
                  .borderRadius(4)
                  .padding({ left: 3, right: 3, top: 1, bottom: 1 })
                  .margin({ left: 4 })
              }
            } else {
              Text('需购买')
                .fontSize(10)
                .fontColor(THEME.accent)
                .margin({ left: 4 })
            }
          }
          .backgroundColor(ing.haveIt ? (ing.isExpiring ? THEME.warnBg : THEME.section) : THEME.accentBg)
          .borderRadius(6)
          .padding({ left: 8, right: 8, top: 4, bottom: 4 })
          .margin({ right: 6, bottom: 6 })
        }, (ing: MatchedIngredient) => ing.name)
      }
      .margin({ top: 8 })

      // 步骤展开区
      if (this.expandedSet.has(recipe.name)) {
        Column() {
          ForEach(recipe.stepList, (step: string, idx: number) => {
            Row() {
              Text(`${idx + 1}`)
                .fontSize(11)
                .fontColor(THEME.primary)
                .backgroundColor(THEME.primaryBg)
                .borderRadius(8)
                .width(18)
                .height(18)
                .textAlign(TextAlign.Center)
                .margin({ right: 8 })
              Text(step)
                .fontSize(12)
                .fontColor(THEME.text2)
                .layoutWeight(1)
            }
            .width('100%')
            .margin({ top: 5 })
          }, (step: string, idx: number) => `${recipe.name}-${idx}`)
        }
        .width('100%')
        .margin({ top: 5 })
        .backgroundColor(THEME.section)
        .borderRadius(8)
        .padding({ left: 10, right: 10, top: 6, bottom: 6 })
      }

      // 底部提示行（整卡可点击展开/收起，收藏单独拦截不冒泡）
      Row() {
        Text(this.expandedSet.has(recipe.name) ? '收起步骤' : `共 ${recipe.steps} 步 · 点击卡片展开`)
          .fontSize(11)
          .fontColor(THEME.text3)
          .layoutWeight(1)
        Text(this.favoriteSet.has(recipe.name) ? '★ 已收藏' : '☆ 收藏')
          .fontSize(12)
          .fontColor(this.favoriteSet.has(recipe.name) ? THEME.accent : THEME.primary)
          .padding({ left: 6, right: 6, top: 2, bottom: 2 })
          .hitTestBehavior(HitTestMode.Block)
          .onClick(() => this.onFavorite(recipe.name))
      }
      .margin({ top: 8 })
      .width('100%')
    }
    .width('100%')
    .padding({ left: 14, right: 14, top: 14, bottom: 14 })
    .backgroundColor(THEME.card)
    .borderRadius(12)
    .border({
      width: { left: 3, top: 1, bottom: 1, right: 1 },
      color: { left: CAT_COLORS[recipe.category], top: THEME.border, bottom: THEME.border, right: THEME.border }
    })
    .margin({ bottom: 12 })
    .onClick(() => this.onToggleExpand(recipe.name))
  }
}
