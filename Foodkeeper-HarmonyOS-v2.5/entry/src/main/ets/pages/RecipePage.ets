// AI 菜谱推荐页面（底部导航第3个 Tab）
// 自动基于库存食材生成 5-7 道菜谱，临期食材全覆盖优先（任务3）
// 卡片点击进入食谱详情页（任务4），完成烹饪按钮已移至详情页
// 收藏/重置等状态在本地维护，重置时清空（任务1）

import { router, promptAction } from '@kit.ArkUI';
import { FoodItem, THEME } from '../model/DataModel';
import { daysUntilExpiry } from '../utils/ExpiryHelper';
import { InventoryStore } from '../store/InventoryStore';
import { DisplayRecipe, MatchedIngredient, generateRecipes, RecipeHolder } from '../model/RecipeEngine';
import { AgentBridge, AgentHolder } from '../utils/AgentBridge';

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
  // 任务4：父组件 onPageShow 时自增，用于检测从详情页返回后库存是否变更
  @Prop @Watch('onRefreshSignalChange') refreshSignal: number = 0;

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
  // 记录上一次生成的菜谱名单，重新生成时作为 excludeNames 传入，保证结果与上一次完全不同
  @State lastRecipeNames: string[] = [];
  // 已"完成烹饪"的菜谱名单，用于 UI 灰显
  @State completedSet: Set<string> = new Set();
  // 任务1：上次见到的重置计数，用于检测重置事件并清空本地状态
  @State lastSeenResetCount: number = -1;

  // ===== 生命周期 =====

  aboutToAppear(): void {
    this.lastSeenResetCount = InventoryStore.getInstance().getResetCount();
    this.loadData();
    this.autoGenerateRecipes();
  }

  // Tab 切换时自动检测库存变更/重置事件并重新生成
  onTabChange(): void {
    if (this.activeTabIndex === 2) {
      // 任务1：检测重置事件，清空收藏/已完成/上次菜谱名单
      const resetCount = InventoryStore.getInstance().getResetCount();
      if (resetCount !== this.lastSeenResetCount) {
        this.favoriteSet = new Set<string>();
        this.completedSet = new Set<string>();
        this.lastRecipeNames = [];
        this.lastSeenResetCount = resetCount;
        this.loadData();
        this.autoGenerateRecipes();
        return;
      }
      // 检测库存版本变更
      const version = InventoryStore.getInstance().getVersion();
      if (version !== this.lastGeneratedVersion) {
        this.autoGenerateRecipes();
      }
    }
  }

  // 任务4：从详情页返回时检测库存变更（完成烹饪扣减了库存）
  onRefreshSignalChange(): void {
    // 智能体降级信号：从 AgentRecipePage 出错返回时，走本地引擎生成
    if (AgentHolder.consumeFallback()) {
      this.loadData();
      this.autoGenerateRecipes();
      return;
    }
    // 检查是否有刚完成烹饪的菜谱（由详情页通过 RecipeHolder 传递）
    const lastCompleted = RecipeHolder.getLastCompleted();
    if (lastCompleted !== null) {
      const next = new Set<string>(this.completedSet);
      next.add(lastCompleted);
      this.completedSet = next;
      RecipeHolder.clearLastCompleted();
    }
    // 检测库存版本变更，若变更则重新生成菜谱
    const version = InventoryStore.getInstance().getVersion();
    if (version !== this.lastGeneratedVersion) {
      this.loadData();
      this.autoGenerateRecipes();
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
    // 任务3：传入上次菜谱名单作为排除项 + 收藏名单作为偏好加成
    const favoriteArr: string[] = [];
    // 手动遍历 Set（避免 ArkTS 对 Set.forEach 的限制）
    this.favoriteSet.forEach((name: string) => {
      favoriteArr.push(name);
    });
    const newRecipes = generateRecipes(this.foods, {
      excludeNames: this.lastRecipeNames,
      favoriteNames: favoriteArr,
    });
    // 更新上次名单，供下次"重新生成"使用
    const names: string[] = [];
    for (let i = 0; i < newRecipes.length; i++) {
      names.push(newRecipes[i].name);
    }
    this.lastRecipeNames = names;
    this.recipes = newRecipes;
    this.loading = false;
  }

  // ===== 交互 =====

  onRegenerate(): void {
    if (this.loading) {
      return;
    }
    this.autoGenerateRecipes();
  }

  // 调用小艺智能体推荐菜谱（HSP 未安装时降级到本地引擎）
  async onAskAgent(): Promise<void> {
    if (this.loading) {
      return;
    }
    // 异步检测 HSP 是否已安装：缺失时导航会 SIGABRT 崩溃，必须预检
    const available = await AgentBridge.checkHspAvailable();
    if (!available) {
      promptAction.showToast({ message: '当前设备未安装小艺智能体服务，使用本地引擎', duration: 1800 });
      this.autoGenerateRecipes();
      return;
    }
    const agentId = AgentBridge.getAgentId();
    const queryText = AgentBridge.buildQuery(this.foods);
    router.pushUrl({ url: 'pages/AgentRecipePage', params: { agentId: agentId, queryText: queryText } });
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

  // 任务4：点击卡片进入食谱详情页（通过 RecipeHolder 传递数据）
  onOpenDetail(recipe: DisplayRecipe): void {
    RecipeHolder.set(recipe);
    router.pushUrl({ url: 'pages/RecipeDetailPage' });
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

      // 3. 双按钮：本地重新生成 + 问 AI 推荐（小艺智能体）
      Row() {
        Button(this.loading ? '生成中...' : '🔄  重新生成')
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor('#FFFEFB')
          .backgroundColor(this.loading ? THEME.text3 : THEME.primary)
          .borderRadius(24)
          .height(44)
          .layoutWeight(1)
          .enabled(!this.loading)
          .onClick(() => this.onRegenerate())

        Button('🤖  问 AI 推荐')
          .fontSize(14)
          .fontWeight(FontWeight.Bold)
          .fontColor('#FFFEFB')
          .backgroundColor(THEME.accent)
          .borderRadius(24)
          .height(44)
          .layoutWeight(1)
          .margin({ left: 10 })
          .enabled(!this.loading)
          .onClick(() => this.onAskAgent())
      }
      .width('100%')
      .margin({ left: 14, right: 14, bottom: 8 })

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
          Text('临期食材将被优先覆盖')
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

  // ===== 菜谱卡片（任务4：卡片仅展示概要，点击进入详情页查看完整信息与完成烹饪） =====

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

        // 已完成烹饪徽章
        if (this.completedSet.has(recipe.name)) {
          Text('✓ 已完成')
            .fontSize(10)
            .fontColor('#FFFEFB')
            .backgroundColor(THEME.text3)
            .borderRadius(8)
            .padding({ left: 6, right: 6, top: 2, bottom: 2 })
            .margin({ left: 6 })
        }

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

      // 食材标签预览
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

      // 底部提示行（收藏单独拦截不冒泡，避免触发卡片点击）
      Row() {
        Text(`共 ${recipe.steps} 步 · 点击查看详情 →`)
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
    .onClick(() => this.onOpenDetail(recipe))
  }
}
