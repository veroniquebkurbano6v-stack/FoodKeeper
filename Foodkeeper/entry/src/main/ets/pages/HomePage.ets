// 首页（对应原应用 Home 组件 jE）
// 布局：标题栏 + 渐变头部卡 + 4列统计网格 + 新增按钮 + 临期提醒 + 8宫格快捷操作
// 响应式：基于媒体查询，宽屏(>=600vp)放大字号与间距，窄屏紧凑排版；4列结构始终保留

import { router, promptAction, mediaquery } from '@kit.ArkUI';
import { FoodItem, THEME, CATEGORIES } from '../model/DataModel';
import { daysUntilExpiry, getExpiryStatus } from '../utils/ExpiryHelper';
import { InventoryStore } from '../store/InventoryStore';
import { FoodCard } from '../components/FoodCard';

@Component
export struct HomePage {
  @State foods: FoodItem[] = [];
  // 派生状态：使用 @State 而非 getter，避免 ArkTS 依赖收集无法追踪 getter 导致 undefined 崩溃
  @State totalCategories: number = 0;
  @State totalQuantity: number = 0;
  @State expiringCount: number = 0;
  @State expiredCount: number = 0;
  @State expiringFoods: FoodItem[] = [];
  // 响应式：宽屏(>=600vp)标识，控制4列网格的字号与间距
  @State isWideScreen: boolean = false;
  private mediaQueryListener?: mediaquery.MediaQueryListener;
  onSwitchTab: (index: number) => void = () => {};

  aboutToAppear(): void {
    // 注册媒体查询：宽屏(>=600vp，如平板/桌面)放大显示，窄屏(手机)紧凑排版
    this.mediaQueryListener = mediaquery.matchMediaSync('(min-width: 600vp)');
    this.isWideScreen = this.mediaQueryListener.matches;
    this.mediaQueryListener.on('change', (result: mediaquery.MediaQueryResult) => {
      this.isWideScreen = result.matches;
    });
    this.refreshFoods();
  }

  aboutToDisappear(): void {
    if (this.mediaQueryListener) {
      this.mediaQueryListener.off('change');
    }
  }

  onPageShow(): void {
    this.refreshFoods();
  }

  refreshFoods(): void {
    this.foods = InventoryStore.getInstance().getFoods();
    this.recomputeDerived();
  }

  // 重新计算所有派生状态（替代 getter）
  private recomputeDerived(): void {
    // 食材品类数（不同分类数）
    const categorySet = new Set<number>();
    this.foods.forEach(f => categorySet.add(f.categoryId));
    this.totalCategories = categorySet.size;

    // 总数量
    let total = 0;
    for (let i = 0; i < this.foods.length; i++) {
      total += this.foods[i].quantity;
    }
    this.totalQuantity = total;

    // 临期预警数（yellow + red）& 已过期数
    let expiring = 0;
    let expired = 0;
    for (let i = 0; i < this.foods.length; i++) {
      const status = getExpiryStatus(daysUntilExpiry(this.foods[i].expiryDate));
      if (status === 'yellow' || status === 'red') {
        expiring++;
      } else if (status === 'expired') {
        expired++;
      }
    }
    this.expiringCount = expiring;
    this.expiredCount = expired;

    // 临期食材列表（最多5条，按过期天数升序）
    const expiringList: FoodItem[] = [];
    for (let i = 0; i < this.foods.length; i++) {
      const f = this.foods[i];
      const status = getExpiryStatus(daysUntilExpiry(f.expiryDate));
      if (status === 'yellow' || status === 'red' || status === 'expired') {
        expiringList.push(f);
      }
    }
    expiringList.sort((a, b) => daysUntilExpiry(a.expiryDate) - daysUntilExpiry(b.expiryDate));
    this.expiringFoods = expiringList.slice(0, 5);
  }

  // 跳转新增食材
  goAddFood(): void {
    router.pushUrl({ url: 'pages/AddFoodPage' });
  }

  // 跳转编辑食材
  goEditFood(id: number): void {
    router.pushUrl({ url: 'pages/AddFoodPage', params: { id: id } });
  }

  // 跳转设置（切到"我的"标签）
  goSettings(): void {
    this.onSwitchTab(3);
  }

  // 提示功能开发中
  toastComingSoon(name: string): void {
    promptAction.showToast({ message: `${name}功能开发中`, duration: 1500 });
  }

  build(): void {
    Column() {
      // 顶部标题栏（收紧高度）
      Row() {
        Text('食库管家')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
      }
      .width('100%')
      .height(48)
      .padding({ left: 16, right: 16 })
      .justifyContent(FlexAlign.Center)
      .backgroundColor(THEME.card)
      .border({ width: { bottom: 1 }, color: THEME.border })

      Scroll() {
        Column() {
          // 渐变头部卡片（收紧高度，减少留白）
          Stack({ alignContent: Alignment.End }) {
            Column() {
              Text('🥬')
                .fontSize(30)
              Text('食库管家')
                .fontSize(20)
                .fontWeight(FontWeight.Bold)
                .fontColor('#FFFEFB')
                .margin({ top: 4 })
              Text('让食材管理更简单')
                .fontSize(12)
                .fontColor('#FFFEFBCC')
                .margin({ top: 2 })
            }
            .width('100%')
            .height(96)
            .padding({ left: 18, top: 16 })
            .alignItems(HorizontalAlign.Start)
            .justifyContent(FlexAlign.Start)
            .linearGradient({
              angle: 135,
              colors: [['#7C9473', 0], ['#6E8CA8', 1]]
            })
            .borderRadius(14)

            Text('❄')
              .fontSize(20)
              .fontColor('#FFFEFB99')
              .margin({ right: 16, top: 10 })
          }
          .width('100%')
          .margin({ top: 12, bottom: 12 })

          // 3列统计网格
          Grid() {
            GridItem() { this.StatCard('食材品类', this.totalCategories.toString(), THEME.primary, THEME.primaryBg) }
            GridItem() { this.StatCard('临期预警', this.expiringCount.toString(), THEME.warn, THEME.warnBg) }
            GridItem() { this.StatCard('已过期', this.expiredCount.toString(), THEME.danger, THEME.dangerBg) }
          }
          .columnsTemplate('1fr 1fr 1fr')
          .rowsGap(8)
          .columnsGap(this.isWideScreen ? 10 : 6)
          .width('100%')
          .height(this.isWideScreen ? 92 : 76)
          .margin({ bottom: 12 })

          // 新增食材按钮（收紧高度）
          Button({ type: ButtonType.Capsule }) {
            Text('+ 新增食材入库')
              .fontSize(15)
              .fontColor('#FFFEFB')
              .fontWeight(FontWeight.Medium)
          }
          .width('100%')
          .height(44)
          .backgroundColor(THEME.primary)
          .margin({ bottom: 14 })
          .onClick(() => this.goAddFood())

          // 临期提醒区域
          if (this.expiringFoods.length > 0) {
            Row() {
              Text('⚠ 临期提醒')
                .fontSize(14)
                .fontWeight(FontWeight.Bold)
                .fontColor(THEME.text)
              Blank()
              Text('查看全部')
                .fontSize(12)
                .fontColor(THEME.primary)
                .onClick(() => this.onSwitchTab(1))
            }
            .width('100%')
            .margin({ bottom: 8 })

            Column() {
              ForEach(this.expiringFoods, (f: FoodItem) => {
                FoodCard({ food: f, onTap: (id: number) => this.goEditFood(id) })
                  .margin({ bottom: 8 })
              }, (f: FoodItem) => f.id.toString())
            }
            .width('100%')
            .margin({ bottom: 14 })
          }

          // 快捷操作标题
          Row() {
            Text('快捷操作')
              .fontSize(14)
              .fontWeight(FontWeight.Bold)
              .fontColor(THEME.text)
          }
          .width('100%')
          .margin({ bottom: 8 })

          // 8宫格快捷操作（4列2行，收紧高度与间距）
          Grid() {
            GridItem() { this.QuickAction('📦', '库存管理', () => this.onSwitchTab(1)) }
            GridItem() { this.QuickAction('⬇', '食材出库', () => this.toastComingSoon('食材出库')) }
            GridItem() { this.QuickAction('🍳', 'AI菜谱', () => this.onSwitchTab(2)) }
            GridItem() { this.QuickAction('📊', '数据报表', () => this.toastComingSoon('数据报表')) }
            GridItem() { this.QuickAction('⏰', '临期汇总', () => this.onSwitchTab(1)) }
            GridItem() { this.QuickAction('📝', '出库日志', () => this.toastComingSoon('出库日志')) }
            GridItem() { this.QuickAction('👤', '口味偏好', () => this.toastComingSoon('口味偏好')) }
            GridItem() { this.QuickAction('⚙', '系统设置', () => this.goSettings()) }
          }
          .columnsTemplate('1fr 1fr 1fr 1fr')
          .rowsTemplate('1fr 1fr')
          .rowsGap(8)
          .columnsGap(6)
          .width('100%')
          .height(148)

          Blank().height(16)
        }
        .width('100%')
        .padding({ left: 14, right: 14 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
      .align(Alignment.Top)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(THEME.bg)
  }

  // 统计卡片：4列网格单元，宽屏放大字号与内边距
  @Builder
  StatCard(label: string, value: string, color: string, bg: string): void {
    Column() {
      Text(value)
        .fontSize(this.isWideScreen ? 24 : 20)
        .fontWeight(FontWeight.Bold)
        .fontColor(color)
      Text(label)
        .fontSize(this.isWideScreen ? 11 : 10)
        .fontColor(THEME.text2)
        .margin({ top: 2 })
    }
    .width('100%')
    .height('100%')
    .padding(this.isWideScreen ? 12 : 8)
    .justifyContent(FlexAlign.Center)
    .backgroundColor(THEME.card)
    .borderRadius(10)
    .border({ width: 1, color: bg })
  }

  // 快捷操作格：4列网格单元，紧凑居中
  @Builder
  QuickAction(icon: string, label: string, action: () => void): void {
    Column() {
      Text(icon)
        .fontSize(22)
      Text(label)
        .fontSize(10)
        .fontColor(THEME.text2)
        .margin({ top: 4 })
    }
    .width('100%')
    .height('100%')
    .padding({ top: 10, bottom: 10 })
    .justifyContent(FlexAlign.Center)
    .backgroundColor(THEME.card)
    .borderRadius(10)
    .border({ width: 1, color: THEME.border })
    .onClick(() => action())
  }
}
