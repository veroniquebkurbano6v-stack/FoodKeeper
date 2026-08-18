// 主入口：底部四标签（首页 / 库存 / AI菜谱 / 我的）
// 使用 Tabs 组件承载四个核心页面

import { THEME } from '../model/DataModel';
import { HomePage } from './HomePage';
import { InventoryPage } from './InventoryPage';
import { RecipePage } from './RecipePage';
import { SettingsPage } from './SettingsPage';

@Entry
@Component
struct Index {
  @State currentIndex: number = 0;
  // 页面显示信号：每次 onPageShow 自增，通过 @Prop @Watch 通知子组件刷新
  // （子组件是 @Component，自身的 onPageShow 不会触发，需父组件代为通知）
  @State refreshSignal: number = 0;

  onPageShow(): void {
    this.refreshSignal++;
  }

  build(): void {
    Column() {
      Tabs({ barPosition: BarPosition.End, index: this.currentIndex }) {
        TabContent() {
          HomePage({
            refreshSignal: this.refreshSignal,
            activeTabIndex: this.currentIndex,
            onSwitchTab: (index: number) => {
              this.currentIndex = index;
            }
          })
        }
        .tabBar(this.TabBuilder('首页', 0, '🏠'))

        TabContent() {
          InventoryPage({ refreshSignal: this.refreshSignal, activeTabIndex: this.currentIndex })
        }
        .tabBar(this.TabBuilder('库存', 1, '📦'))

        TabContent() {
          RecipePage({ activeTabIndex: this.currentIndex, refreshSignal: this.refreshSignal })
        }
        .tabBar(this.TabBuilder('AI菜谱', 2, '🍳'))

        TabContent() {
          SettingsPage()
        }
        .tabBar(this.TabBuilder('我的', 3, '👤'))
      }
      .onChange((index: number) => {
        this.currentIndex = index;
      })
      .barHeight(58)
      .scrollable(false)
      .animationDuration(200)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(THEME.bg)
  }

  @Builder
  TabBuilder(title: string, index: number, icon: string): void {
    Column() {
      Text(icon)
        .fontSize(22)
        .opacity(this.currentIndex === index ? 1 : 0.5)
      Text(title)
        .fontSize(11)
        .fontColor(this.currentIndex === index ? THEME.primary : THEME.text3)
        .margin({ top: 2 })
    }
    .width('100%')
    .height('100%')
    .justifyContent(FlexAlign.Center)
  }
}
