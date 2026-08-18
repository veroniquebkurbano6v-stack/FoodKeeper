// 食材卡片组件（对应原应用 sk-food-card）
// 展示：分类色条 + 状态点/天数 + 名称 + 数量·分类

import { FoodItem, THEME } from '../model/DataModel';
import { getFoodCardInfo } from '../utils/ExpiryHelper';

@Component
export struct FoodCard {
  @Prop food: FoodItem;
  // 点击回调
  onTap: (id: number) => void = () => {};

  build() {
    Row() {
      Column() {
        // 顶部状态徽章：带状态背景色，移动端一眼可辨
        Row() {
          Text(getFoodCardInfo(this.food).daysText)
            .fontSize(10)
            .fontColor(getFoodCardInfo(this.food).statusColor)
            .fontWeight(FontWeight.Medium)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
        }
        .padding({ left: 6, right: 6, top: 2, bottom: 2 })
        .borderRadius(8)
        .backgroundColor(getFoodCardInfo(this.food).statusBg)

        // 食材名称（核心信息，14px Medium 保证可读性）
        Text(this.food.name)
          .fontSize(14)
          .fontWeight(FontWeight.Medium)
          .fontColor(THEME.text)
          .maxLines(1)
          .textOverflow({ overflow: TextOverflow.Ellipsis })
          .width('100%')
          .margin({ top: 6 })

        // 底部：数量·分类（次要信息，11px 紧凑排版）
        Row() {
          Text(`${this.food.quantity}${this.food.unit}`)
            .fontSize(11)
            .fontColor(THEME.text2)
            .maxLines(1)
          Text('·')
            .fontSize(11)
            .fontColor(THEME.text3)
            .margin({ left: 4, right: 4 })
          Text(getFoodCardInfo(this.food).catLabel)
            .fontSize(11)
            .fontColor(THEME.text2)
            .maxLines(1)
            .textOverflow({ overflow: TextOverflow.Ellipsis })
            .layoutWeight(1)
        }
        .width('100%')
        .margin({ top: 4 })
      }
      .layoutWeight(1)
      .padding({ left: 10, right: 10, top: 8, bottom: 8 })
      .alignItems(HorizontalAlign.Start)
    }
    .width('100%')
    .constraintSize({ minHeight: 72 })
    .backgroundColor(THEME.card)
    .borderRadius(10)
    .border({
      width: { left: 3, right: 1, top: 1, bottom: 1 },
      color: { left: getFoodCardInfo(this.food).catDot, right: THEME.border, top: THEME.border, bottom: THEME.border }
    })
    .onClick(() => {
      this.onTap(this.food.id);
    })
  }
}
