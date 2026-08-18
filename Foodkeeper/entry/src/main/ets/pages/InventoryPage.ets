// 库存列表页（对应原应用 Inventory 组件 rA）
// 布局：标题栏(+新增) + 搜索 + 分类Tab + 排序切换 + 食材网格 + 空状态

import { router, promptAction } from '@kit.ArkUI';
import { FoodItem, THEME, CATEGORIES, CategoryInfo } from '../model/DataModel';
import { daysUntilExpiry, getExpiryStatus, getStatusPriority, ExpiryStatus } from '../utils/ExpiryHelper';
import { InventoryStore } from '../store/InventoryStore';
import { FoodCard } from '../components/FoodCard';

@Component
export struct InventoryPage {
  @State foods: FoodItem[] = [];
  @State keyword: string = '';
  @State activeCategory: string = 'all';   // 'all' | category value
  @State sortByExpiry: boolean = true;     // true=按保质期, false=按入库时间
  @State loading: boolean = false;
  // 派生状态：使用 @State 而非 getter，避免 ArkTS 依赖收集无法追踪 getter 导致 undefined 崩溃
  @State filteredFoods: FoodItem[] = [];
  @State emptyText: string = '';
  @State categoryCounts: Record<string, number> = {};

  aboutToAppear(): void {
    this.loadFoods();
  }

  onPageShow(): void {
    this.loadFoods();
  }

  loadFoods(): void {
    this.loading = true;
    // 同步获取，模拟原应用的异步加载
    setTimeout(() => {
      this.foods = InventoryStore.getInstance().getFoods();
      this.loading = false;
      this.recomputeDerived();
    }, 50);
  }

  // 重新计算所有派生状态（替代 getter）
  private recomputeDerived(): void {
    // 有效食材（名称存在且数量>0）
    const valid: FoodItem[] = [];
    for (let i = 0; i < this.foods.length; i++) {
      const f = this.foods[i];
      if (f.name && f.name.length > 0 && f.quantity > 0) {
        valid.push(f);
      }
    }

    // 各分类计数（含 'all'）
    const counts: Record<string, number> = {};
    counts['all'] = valid.length;
    for (let i = 0; i < CATEGORIES.length; i++) {
      counts[CATEGORIES[i].value] = 0;
    }
    for (let i = 0; i < valid.length; i++) {
      const f = valid[i];
      const cat = CATEGORIES.find(c => c.id === f.categoryId);
      if (cat) {
        counts[cat.value] = (counts[cat.value] || 0) + 1;
      }
    }
    this.categoryCounts = counts;

    // 过滤+排序后的食材列表
    let list = valid;
    // 关键词过滤
    if (this.keyword.length > 0) {
      list = list.filter(f => f.name.includes(this.keyword));
    }
    // 分类过滤
    if (this.activeCategory !== 'all') {
      list = list.filter(f => {
        const cat = CATEGORIES.find(c => c.value === this.activeCategory);
        return cat ? f.categoryId === cat.id : false;
      });
    }
    // 排序
    const sorted = list.slice();
    if (this.sortByExpiry) {
      sorted.sort((a, b) => {
        const sa = getExpiryStatus(daysUntilExpiry(a.expiryDate));
        const sb = getExpiryStatus(daysUntilExpiry(b.expiryDate));
        const pa = getStatusPriority(sa);
        const pb = getStatusPriority(sb);
        if (pa !== pb) return pa - pb;
        return daysUntilExpiry(a.expiryDate) - daysUntilExpiry(b.expiryDate);
      });
    } else {
      sorted.sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime());
    }
    this.filteredFoods = sorted;

    // 空状态描述
    if (this.keyword.length > 0) {
      this.emptyText = `未找到包含「${this.keyword}」的食材`;
    } else if (this.activeCategory === 'all') {
      this.emptyText = '暂无食材，点击右上角「新增」添加';
    } else {
      const cat = CATEGORIES.find(c => c.value === this.activeCategory);
      this.emptyText = `「${cat ? cat.label : this.activeCategory}」分类暂无食材`;
    }
  }

  // 当前分类下的数量（从缓存读取，避免在 build() 中调用方法计算）
  countByCategory(catValue: string): number {
    return this.categoryCounts[catValue] || 0;
  }

  goAddFood(): void {
    router.pushUrl({ url: 'pages/AddFoodPage' });
  }

  goEditFood(id: number): void {
    router.pushUrl({ url: 'pages/AddFoodPage', params: { id: id } });
  }

  toggleSort(): void {
    this.sortByExpiry = !this.sortByExpiry;
    this.recomputeDerived();
  }

  onKeywordChange(val: string): void {
    this.keyword = val;
    this.recomputeDerived();
  }

  onCategoryChange(val: string): void {
    this.activeCategory = val;
    this.recomputeDerived();
  }

  build(): void {
    Column() {
      // 顶部标题栏 + 新增按钮
      Row() {
        Text('库存清单')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
        Blank()
        Row() {
          Text('+')
            .fontSize(14)
            .fontColor(THEME.primary)
          Text('新增')
            .fontSize(13)
            .fontColor(THEME.primary)
            .margin({ left: 2 })
        }
        .padding({ left: 12, right: 12, top: 6, bottom: 6 })
        .borderRadius(999)
        .backgroundColor(THEME.primaryBg)
        .onClick(() => this.goAddFood())
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .backgroundColor(THEME.card)
      .border({ width: { bottom: 1 }, color: THEME.border })

      // 搜索栏
      Row() {
        TextInput({ placeholder: '搜索食材名称', text: this.keyword })
          .width('100%')
          .height(38)
          .fontSize(14)
          .backgroundColor(THEME.section)
          .borderRadius(19)
          .onChange((val: string) => {
            this.onKeywordChange(val);
          })
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 12, bottom: 8 })

      // 分类标签栏（横向滚动）
      Scroll() {
        Row() {
          this.CategoryTab('all', '全部', THEME.primary)
          ForEach(CATEGORIES, (cat: CategoryInfo) => {
            this.CategoryTab(cat.value, cat.label, cat.color)
          }, (cat: CategoryInfo) => cat.value)
        }
        .padding({ left: 16, right: 16 })
      }
      .scrollable(ScrollDirection.Horizontal)
      .scrollBar(BarState.Off)
      .width('100%')
      .height(40)

      // 排序栏
      Row() {
        Text(this.sortByExpiry ? '按保质期排序' : '按入库时间排序')
          .fontSize(12)
          .fontColor(THEME.primary)
          .padding({ left: 12, right: 12, top: 5, bottom: 5 })
          .borderRadius(999)
          .backgroundColor(THEME.primaryBg)
          .onClick(() => this.toggleSort())
        Blank()
        if (!this.loading) {
          Text(`共 ${this.filteredFoods.length} 项`)
            .fontSize(12)
            .fontColor(THEME.text3)
        }
      }
      .width('100%')
      .padding({ left: 16, right: 16, top: 4, bottom: 8 })

      // 食材网格 / 空状态
      if (this.loading) {
        Column() {
          Text('加载中...')
            .fontSize(14)
            .fontColor(THEME.text3)
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else if (this.filteredFoods.length === 0) {
        Column() {
          Text('📭')
            .fontSize(48)
          Text(this.emptyText)
            .fontSize(14)
            .fontColor(THEME.text3)
            .margin({ top: 12 })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        Grid() {
          ForEach(this.filteredFoods, (f: FoodItem) => {
            GridItem() {
              FoodCard({ food: f, onTap: (id: number) => this.goEditFood(id) })
            }
          }, (f: FoodItem) => f.id.toString())
        }
        .columnsTemplate('1fr 1fr')
        .rowsGap(10)
        .columnsGap(10)
        .layoutWeight(1)
        .width('100%')
        .padding({ left: 16, right: 16, bottom: 80 })
        .scrollBar(BarState.Off)
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(THEME.bg)
  }

  @Builder
  CategoryTab(value: string, label: string, color: string): void {
    Row() {
      Text(label)
        .fontSize(13)
        .fontColor(this.activeCategory === value ? THEME.primary : THEME.text2)
        .fontWeight(this.activeCategory === value ? FontWeight.Bold : FontWeight.Normal)
      if (this.countByCategory(value) > 0) {
        Text(this.countByCategory(value).toString())
          .fontSize(10)
          .fontColor(this.activeCategory === value ? THEME.primary : THEME.text3)
          .margin({ left: 4 })
          .padding({ left: 5, right: 5, top: 1, bottom: 1 })
          .backgroundColor(this.activeCategory === value ? THEME.primaryBg : THEME.section)
          .borderRadius(8)
      }
    }
    .padding({ left: 12, right: 12, top: 7, bottom: 7 })
    .margin({ right: 8 })
    .borderRadius(999)
    .backgroundColor(this.activeCategory === value ? THEME.primaryBg : THEME.card)
    .border({ width: 1, color: this.activeCategory === value ? THEME.primary : THEME.border })
    .onClick(() => {
      this.onCategoryChange(value);
    })
  }
}
