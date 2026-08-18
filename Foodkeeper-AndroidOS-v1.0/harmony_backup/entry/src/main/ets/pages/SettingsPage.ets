// 设置页（对应原应用 Settings 组件 kO 的简化版）

import { promptAction } from '@kit.ArkUI';
import { THEME } from '../model/DataModel';
import { InventoryStore, ConsumeLog } from '../store/InventoryStore';

@Component
export struct SettingsPage {
  @State foodCount: number = 0;
  @State version: number = 0;
  @State resetCount: number = 0;
  @State logCount: number = 0;
  @State resetting: boolean = false;
  @State recentLogs: ConsumeLog[] = [];
  @State showLogs: boolean = false;

  aboutToAppear(): void {
    this.refreshStats();
  }

  // 刷新本地存储统计信息
  refreshStats(): void {
    const info = InventoryStore.getInstance().getStorageInfo();
    this.foodCount = info.foodCount;
    this.version = info.version;
    this.resetCount = info.resetCount;
    this.logCount = info.logCount;
    this.recentLogs = InventoryStore.getInstance().getConsumeLogs();
  }

  // 任务1：重置为初始数据（带二次确认 + 错误处理 + 操作反馈）
  async resetData(): Promise<void> {
    if (this.resetting) return;
    // 二次确认对话框，避免误触
    promptAction.showDialog({
      title: '重置数据',
      message: '将清除所有库存数据并恢复为 15 条示例食材。\n收藏的菜谱、已完成烹饪记录也会被清空。\n该操作不可撤销，确认继续？',
      buttons: [
        { text: '取消', color: THEME.text3 },
        { text: '确认重置', color: THEME.danger },
      ],
    }).then((result) => {
      if (result.index === 1) {
        this.doReset();
      }
    }).catch(() => {
      // 对话框异常时不执行重置
      promptAction.showToast({ message: '操作已取消', duration: 1200 });
    });
  }

  // 执行重置（带 try/catch 错误处理与反馈）
  async doReset(): Promise<void> {
    this.resetting = true;
    try {
      const ok = await InventoryStore.getInstance().resetToSeed();
      if (ok) {
        // 重置成功：刷新本地统计并提示
        this.refreshStats();
        promptAction.showToast({
          message: `已重置为初始数据（${this.foodCount} 条食材）`,
          duration: 1800,
        });
      } else {
        // resetToSeed 内部异常
        promptAction.showToast({ message: '重置失败，请稍后重试', duration: 1800 });
      }
    } catch (e) {
      // 未预期异常：提示用户但不崩溃
      promptAction.showToast({ message: '重置异常：' + String(e), duration: 2000 });
    } finally {
      this.resetting = false;
    }
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
          // 任务2：本地数据存储信息（强调数据存储在本地设备）
          this.SectionTitle('本地数据存储')
          Column() {
            Row() {
              Text('存储位置')
                .fontSize(14)
                .fontColor(THEME.text)
              Blank()
              Text('设备本地（沙箱）')
                .fontSize(13)
                .fontColor(THEME.fresh)
            }
            .width('100%')
            .padding({ top: 8, bottom: 6 })

            Row() {
              Text('数据上传')
                .fontSize(14)
                .fontColor(THEME.text)
              Blank()
              Text('不上传任何服务器')
                .fontSize(13)
                .fontColor(THEME.fresh)
            }
            .width('100%')
            .padding({ top: 6, bottom: 6 })

            Row() {
              Text('当前食材数量')
                .fontSize(14)
                .fontColor(THEME.text)
              Blank()
              Text(this.foodCount.toString())
                .fontSize(13)
                .fontColor(THEME.text2)
            }
            .width('100%')
            .padding({ top: 6, bottom: 6 })

            Row() {
              Text('数据版本')
                .fontSize(14)
                .fontColor(THEME.text)
              Blank()
              Text(`v${this.version} · 重置 ${this.resetCount} 次`)
                .fontSize(13)
                .fontColor(THEME.text2)
            }
            .width('100%')
            .padding({ top: 6, bottom: 8 })
          }
          .width('100%')
          .padding({ left: 16, right: 16 })
          .backgroundColor(THEME.card)
          .borderRadius(10)
          .border({ width: 1, color: THEME.border })

          // 数据管理
          this.SectionTitle('数据管理')
          this.SettingItem('重置为初始数据', '恢复15条示例', true, () => this.resetData())
          this.SettingItem(
            this.resetting ? '正在重置...' : '重置状态',
            this.resetting ? '处理中' : '正常',
            false,
            () => {}
          )

          // 完成烹饪历史
          this.SectionTitle('完成烹饪记录')
          if (this.logCount > 0) {
            this.SettingItem('历史记录', `${this.logCount} 条`, false, () => {
              this.showLogs = !this.showLogs;
            })
            if (this.showLogs) {
              ForEach(this.recentLogs.slice().reverse(), (log: ConsumeLog, idx: number) => {
                Column() {
                  Row() {
                    Text(log.recipeName)
                      .fontSize(13)
                      .fontWeight(FontWeight.Medium)
                      .fontColor(THEME.text)
                      .layoutWeight(1)
                    Text(log.timestamp)
                      .fontSize(11)
                      .fontColor(THEME.text3)
                  }
                  .width('100%')
                  Row() {
                    Text(`扣减 ${log.deducted} 项`)
                      .fontSize(11)
                      .fontColor(THEME.fresh)
                      .backgroundColor(THEME.freshBg)
                      .borderRadius(6)
                      .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                    if (log.insufficient > 0) {
                      Text(`不足 ${log.insufficient} 项`)
                        .fontSize(11)
                        .fontColor(THEME.warn)
                        .backgroundColor(THEME.warnBg)
                        .borderRadius(6)
                        .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                        .margin({ left: 6 })
                    }
                    if (log.skipped > 0) {
                      Text(`跳过 ${log.skipped} 项`)
                        .fontSize(11)
                        .fontColor(THEME.text3)
                        .backgroundColor(THEME.section)
                        .borderRadius(6)
                        .padding({ left: 6, right: 6, top: 2, bottom: 2 })
                        .margin({ left: 6 })
                    }
                  }
                  .width('100%')
                  .margin({ top: 6 })
                }
                .width('100%')
                .padding({ left: 12, right: 12, top: 10, bottom: 10 })
                .backgroundColor(THEME.section)
                .borderRadius(8)
                .margin({ bottom: 6 })
              }, (log: ConsumeLog, idx: number) => `${log.timestamp}-${idx}`)
            }
          } else {
            this.SettingItem('历史记录', '暂无', false, () => {})
          }

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
            Text('家庭冰箱食材进销存管理应用，所有数据存储在本地设备沙箱中，不会上传到任何服务器。卸载应用后数据将自动清除。')
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
