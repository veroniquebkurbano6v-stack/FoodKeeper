// 设置页（对应原应用 Settings 组件 kO 的简化版）

import { promptAction } from '@kit.ArkUI';
import { THEME } from '../model/DataModel';
import { InventoryStore } from '../store/InventoryStore';

@Component
export struct SettingsPage {
  @State foodCount: number = 0;

  aboutToAppear(): void {
    this.foodCount = InventoryStore.getInstance().getFoods().length;
  }

  async resetData(): Promise<void> {
    await InventoryStore.getInstance().resetToSeed();
    this.foodCount = InventoryStore.getInstance().getFoods().length;
    promptAction.showToast({ message: '已重置为初始数据', duration: 1500 });
  }

  build(): void {
    Column() {
      // 顶部导航栏
      Row() {
        Text('系统设置')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
        Blank()
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .backgroundColor(THEME.card)
      .border({ width: { bottom: 1 }, color: THEME.border })

      Scroll() {
        Column() {
          // 数据管理
          this.SectionTitle('数据管理')
          this.SettingItem('当前食材数量', this.foodCount.toString(), false, () => {})
          this.SettingItem('重置为初始数据', '恢复15条示例', true, () => this.resetData())

          // 关于
          this.SectionTitle('关于应用')
          this.SettingItem('应用名称', '食库管家', false, () => {})
          this.SettingItem('版本', 'v1.0.0', false, () => {})
          this.SettingItem('技术栈', 'HarmonyOS ArkTS', false, () => {})

          // 说明卡片
          Column() {
            Text('食库管家')
              .fontSize(18)
              .fontWeight(FontWeight.Bold)
              .fontColor(THEME.text)
            Text('让食材管理更简单')
              .fontSize(13)
              .fontColor(THEME.text2)
              .margin({ top: 6 })
            Text('家庭冰箱食材进销存管理应用，所有数据存储在本地设备中，不会上传到服务器。')
              .fontSize(12)
              .fontColor(THEME.text3)
              .margin({ top: 12 })
              .textAlign(TextAlign.Center)
          }
          .width('100%')
          .padding(24)
          .backgroundColor(THEME.card)
          .borderRadius(14)
          .border({ width: 1, color: THEME.border })
          .margin({ top: 24 })
          .alignItems(HorizontalAlign.Center)

          Blank().height(40)
        }
        .width('100%')
        .padding({ left: 16, right: 16, top: 16 })
      }
      .layoutWeight(1)
      .scrollBar(BarState.Off)
    }
    .width('100%')
    .height('100%')
    .backgroundColor(THEME.bg)
  }

  @Builder
  SectionTitle(title: string): void {
    Text(title)
      .fontSize(13)
      .fontColor(THEME.text2)
      .margin({ top: 16, bottom: 8 })
  }

  @Builder
  SettingItem(title: string, value: string, isAction: boolean, action: () => void): void {
    Row() {
      Text(title)
        .fontSize(15)
        .fontColor(THEME.text)
      Blank()
      Text(value)
        .fontSize(14)
        .fontColor(isAction ? THEME.danger : THEME.text3)
    }
    .width('100%')
    .padding({ left: 16, right: 16, top: 16, bottom: 16 })
    .backgroundColor(THEME.card)
    .borderRadius(10)
    .border({ width: 1, color: THEME.border })
    .margin({ bottom: 8 })
    .onClick(() => action())
  }
}
