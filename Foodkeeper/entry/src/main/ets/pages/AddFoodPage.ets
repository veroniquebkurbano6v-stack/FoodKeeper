// 新增/编辑食材页（对应原应用 AddFood/EditFood 组件）
// 表单：名称 + 分类 + 数量 + 单位 + 存储区 + 单价 + 总价 + 购买日期 + 保质期天数

import { router, promptAction } from '@kit.ArkUI';
import { FoodItem, THEME, CATEGORIES, STORAGE_ZONES, UNITS, CategoryInfo, StorageZoneInfo, StorageZone, formatDate, estimateExpiryDays } from '../model/DataModel';
import { InventoryStore, AddFoodInput, UpdateFoodPatch } from '../store/InventoryStore';
import { matchFood } from '../model/FoodKnowledgeBase';

@Entry
@Component
struct AddFoodPage {
  @State editId: number = -1;            // -1 表示新增
  @State name: string = '';
  @State categoryId: number = 1;
  @State quantity: string = '1';
  @State unit: string = '个';
  @State storageZone: StorageZone = 'refrigerator';
  @State price: string = '0';
  @State totalPrice: string = '0';
  @State purchaseDate: string = formatDate(new Date());
  @State expiryDays: string = '7';
  @State isEdit: boolean = false;
  // 知识库匹配状态（仅新增模式下生效）
  @State matchStatus: string = 'idle';   // 'idle' | 'matched' | 'nomatch'
  @State matchedName: string = '';
  @State knowledgeTip: string = '';
  // 用户手动修改标记：命中知识库后，被用户手动改过的字段不再被自动覆盖
  @State manualCategory: boolean = false;
  @State manualUnit: boolean = false;
  @State manualStorage: boolean = false;
  @State manualExpiry: boolean = false;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, Object>;
    if (params && params['id'] !== undefined) {
      const id = Number(params['id']);
      this.editId = id;
      this.isEdit = true;
      this.loadExisting(id);
    } else {
      // 新增时默认估算保质期
      this.estimateDays();
    }
  }

  loadExisting(id: number): void {
    const food = InventoryStore.getInstance().getFoods().find(f => f.id === id);
    if (food) {
      this.name = food.name;
      this.categoryId = food.categoryId;
      this.quantity = food.quantity.toString();
      this.unit = food.unit;
      this.storageZone = food.storageZone;
      this.price = food.price.toString();
      this.totalPrice = food.totalPrice.toString();
      this.purchaseDate = food.purchaseDate;
      // 反推保质期天数
      const days = Math.floor((new Date(food.expiryDate).getTime() - new Date(food.purchaseDate).getTime()) / (24 * 60 * 60 * 1000));
      this.expiryDays = days.toString();
    }
  }

  // 根据名称和分类估算保质期（回退方案：未命中知识库时使用）
  estimateDays(): void {
    if (this.name.length > 0) {
      this.expiryDays = estimateExpiryDays(this.name, this.categoryId).toString();
    } else {
      this.expiryDays = estimateExpiryDays('', this.categoryId).toString();
    }
  }

  // 根据输入名称匹配知识库，命中则自动填入分类/单位/存储分区/保质期
  // 用户已手动修改过的字段不会被覆盖（与原应用 p 函数逻辑一致）
  tryMatchKnowledge(): void {
    const input = this.name.trim();
    if (input.length === 0) {
      this.matchStatus = 'idle';
      this.matchedName = '';
      this.knowledgeTip = '';
      this.estimateDays();
      return;
    }
    const k = matchFood(input);
    if (k === null) {
      this.matchStatus = 'nomatch';
      this.matchedName = '';
      this.knowledgeTip = '';
      // 未命中知识库时按名称+分类估算保质期（保留原逻辑）
      this.estimateDays();
      return;
    }
    this.matchStatus = 'matched';
    this.matchedName = k.name;
    this.knowledgeTip = k.tip;
    if (!this.manualCategory) {
      this.categoryId = k.categoryId;
    }
    if (!this.manualUnit) {
      this.unit = k.unit;
    }
    if (!this.manualStorage) {
      this.storageZone = k.storageZone;
    }
    if (!this.manualExpiry) {
      this.expiryDays = k.expiryDays.toString();
    }
  }

  onNameChange(val: string): void {
    this.name = val;
    if (this.isEdit) return;
    this.tryMatchKnowledge();
  }

  onCategoryChange(id: number): void {
    this.categoryId = id;
    this.manualCategory = true;
    // 仅在未命中知识库时按分类估算保质期（命中时保质期由知识库提供）
    if (!this.isEdit && this.matchStatus !== 'matched') {
      this.estimateDays();
    }
  }

  onQuantityChange(val: string): void {
    this.quantity = val;
    this.recalcTotal();
  }

  onPriceChange(val: string): void {
    this.price = val;
    this.recalcTotal();
  }

  // 自动计算总价 = 单价 × 数量
  recalcTotal(): void {
    const q = parseFloat(this.quantity) || 0;
    const p = parseFloat(this.price) || 0;
    this.totalPrice = (q * p).toFixed(2);
  }

  // 保存
  async save(): Promise<void> {
    if (!this.name.trim()) {
      promptAction.showToast({ message: '请输入食材名称', duration: 1500 });
      return;
    }
    const q = parseFloat(this.quantity);
    if (isNaN(q) || q <= 0) {
      promptAction.showToast({ message: '数量必须大于0', duration: 1500 });
      return;
    }
    const store = InventoryStore.getInstance();
    if (this.isEdit) {
      // 编辑模式：重新计算到期日并保存所有字段
      const DAY_MS = 24 * 60 * 60 * 1000;
      const expiryDate = formatDate(new Date(new Date(this.purchaseDate).getTime() + (parseInt(this.expiryDays) || 0) * DAY_MS));
      const patch: UpdateFoodPatch = {
        name: this.name.trim(),
        categoryId: this.categoryId,
        quantity: q,
        unit: this.unit,
        storageZone: this.storageZone,
        price: parseFloat(this.price) || 0,
        totalPrice: parseFloat(this.totalPrice) || 0,
        purchaseDate: this.purchaseDate,
        expiryDate: expiryDate,
      };
      await store.updateFood(this.editId, patch);
      promptAction.showToast({ message: '已保存', duration: 1000 });
    } else {
      const input: AddFoodInput = {
        name: this.name.trim(),
        categoryId: this.categoryId,
        quantity: q,
        unit: this.unit,
        storageZone: this.storageZone,
        price: parseFloat(this.price) || 0,
        totalPrice: parseFloat(this.totalPrice) || 0,
        expiryDays: parseInt(this.expiryDays) || 0,
        purchaseDate: this.purchaseDate,
      };
      const newId = await store.addFood(input);
      if (newId > 0) {
        promptAction.showToast({ message: '已添加', duration: 1000 });
      } else {
        promptAction.showToast({ message: '添加失败，请检查输入', duration: 1500 });
        return;
      }
    }
    setTimeout(() => router.back(), 600);
  }

  // 删除
  async deleteFood(): Promise<void> {
    await InventoryStore.getInstance().deleteFood(this.editId);
    promptAction.showToast({ message: '已删除', duration: 1000 });
    setTimeout(() => router.back(), 600);
  }

  build(): void {
    Column() {
      // 顶部导航栏
      Row() {
        Text('←')
          .fontSize(22)
          .fontColor(THEME.text)
          .onClick(() => router.back())
        Text(this.isEdit ? '编辑食材' : '新增食材')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
          .margin({ left: 8 })
        Blank()
        Text('保存')
          .fontSize(15)
          .fontColor(THEME.primary)
          .fontWeight(FontWeight.Medium)
          .onClick(() => this.save())
      }
      .width('100%')
      .height(52)
      .padding({ left: 16, right: 16 })
      .backgroundColor(THEME.card)
      .border({ width: { bottom: 1 }, color: THEME.border })

      Scroll() {
        Column() {
          // 食材名称
          this.FieldLabel('食材名称')
          TextInput({ placeholder: '如：白菜/番茄/土豆/牛肉', text: this.name })
            .width('100%')
            .height(46)
            .fontSize(15)
            .backgroundColor(THEME.card)
            .borderRadius(10)
            .border({ width: 1, color: THEME.border })
            .onChange((val: string) => this.onNameChange(val))

          // 知识库匹配状态条（与原应用 knowledge-status 一致）
          if (this.matchStatus === 'matched') {
            Row() {
              Text('✓')
                .fontSize(13)
                .fontColor(THEME.fresh)
                .margin({ right: 6 })
              Column() {
                Text(`已匹配「${this.matchedName}」，下方字段已自动填充，可手动修改`)
                  .fontSize(12)
                  .fontColor(THEME.fresh)
                  .maxLines(2)
                if (this.knowledgeTip.length > 0) {
                  Text(this.knowledgeTip)
                    .fontSize(11)
                    .fontColor(THEME.text3)
                    .margin({ top: 2 })
                    .maxLines(1)
                }
              }
              .alignItems(HorizontalAlign.Start)
              .layoutWeight(1)
            }
            .width('100%')
            .padding({ left: 12, right: 12, top: 8, bottom: 8 })
            .backgroundColor(THEME.freshBg)
            .borderRadius(8)
            .border({ width: 1, color: THEME.fresh })
            .margin({ top: 8 })
          } else if (this.matchStatus === 'nomatch') {
            Row() {
              Text('ℹ')
                .fontSize(13)
                .fontColor(THEME.text3)
                .margin({ right: 6 })
              Text('未匹配到知识库条目，请手动填写下方信息')
                .fontSize(12)
                .fontColor(THEME.text3)
                .layoutWeight(1)
            }
            .width('100%')
            .padding({ left: 12, right: 12, top: 8, bottom: 8 })
            .backgroundColor(THEME.section)
            .borderRadius(8)
            .margin({ top: 8 })
          }

          // 分类
          this.FieldLabel('分类')
          Scroll() {
            Row() {
              ForEach(CATEGORIES, (cat: CategoryInfo) => {
                Text(cat.label)
                  .fontSize(13)
                  .fontColor(this.categoryId === cat.id ? '#FFFEFB' : THEME.text2)
                  .fontWeight(this.categoryId === cat.id ? FontWeight.Bold : FontWeight.Normal)
                  .padding({ left: 14, right: 14, top: 8, bottom: 8 })
                  .margin({ right: 8 })
                  .borderRadius(999)
                  .backgroundColor(this.categoryId === cat.id ? cat.color : THEME.card)
                  .border({ width: 1, color: this.categoryId === cat.id ? cat.color : THEME.border })
                  .onClick(() => this.onCategoryChange(cat.id))
              }, (cat: CategoryInfo) => cat.value)
            }
          }
          .scrollable(ScrollDirection.Horizontal)
          .scrollBar(BarState.Off)
          .width('100%')

          // 数量 + 单位
          this.FieldLabel('数量')
          Row() {
            TextInput({ text: this.quantity })
              .width('40%')
              .height(46)
              .fontSize(15)
              .type(InputType.Number)
              .backgroundColor(THEME.card)
              .borderRadius(10)
              .border({ width: 1, color: THEME.border })
              .onChange((val: string) => this.onQuantityChange(val))
            Blank()
            Text(this.unit)
              .fontSize(14)
              .fontColor(THEME.text)
              .padding({ left: 14, right: 14, top: 12, bottom: 12 })
              .backgroundColor(THEME.section)
              .borderRadius(8)
          }
          .width('100%')

          // 单位选择
          this.FieldLabel('单位')
          Scroll() {
            Row() {
              ForEach(UNITS, (u: string) => {
                Text(u)
                  .fontSize(13)
                  .fontColor(this.unit === u ? '#FFFEFB' : THEME.text2)
                  .padding({ left: 12, right: 12, top: 6, bottom: 6 })
                  .margin({ right: 6 })
                  .borderRadius(999)
                  .backgroundColor(this.unit === u ? THEME.primary : THEME.card)
                  .border({ width: 1, color: this.unit === u ? THEME.primary : THEME.border })
                  .onClick(() => { this.unit = u; this.manualUnit = true; })
              }, (u: string) => u)
            }
          }
          .scrollable(ScrollDirection.Horizontal)
          .scrollBar(BarState.Off)
          .width('100%')

          // 存储分区
          this.FieldLabel('存储分区')
          Row() {
            ForEach(STORAGE_ZONES, (z: StorageZoneInfo) => {
              Column() {
                Text(this.zoneIcon(z.value))
                  .fontSize(20)
                Text(z.label)
                  .fontSize(12)
                  .fontColor(this.storageZone === z.value ? THEME.cold : THEME.text2)
                  .margin({ top: 4 })
              }
              .layoutWeight(1)
              .padding({ top: 12, bottom: 12 })
              .backgroundColor(this.storageZone === z.value ? THEME.coldBg : THEME.card)
              .borderRadius(10)
              .border({ width: 1, color: this.storageZone === z.value ? THEME.cold : THEME.border })
              .onClick(() => { this.storageZone = z.value; this.manualStorage = true; })
            }, (z: StorageZoneInfo) => z.value)
          }
          .width('100%')

          // 单价 + 总价
          this.FieldLabel('单价（元）')
          TextInput({ text: this.price })
            .width('100%')
            .height(46)
            .fontSize(15)
            .type(InputType.Number)
            .backgroundColor(THEME.card)
            .borderRadius(10)
            .border({ width: 1, color: THEME.border })
            .onChange((val: string) => this.onPriceChange(val))

          this.FieldLabel('总价（元）')
          TextInput({ text: this.totalPrice })
            .width('100%')
            .height(46)
            .fontSize(15)
            .type(InputType.Number)
            .backgroundColor(THEME.card)
            .borderRadius(10)
            .border({ width: 1, color: THEME.border })
            .onChange((val: string) => { this.totalPrice = val; })

          // 购买日期
          this.FieldLabel('购买日期')
          Row() {
            Text(this.purchaseDate)
              .fontSize(15)
              .fontColor(THEME.text)
              .layoutWeight(1)
            Text('选择')
              .fontSize(13)
              .fontColor(THEME.primary)
              .onClick(() => this.showDatePicker())
          }
          .width('100%')
          .padding({ left: 14, right: 14, top: 14, bottom: 14 })
          .backgroundColor(THEME.card)
          .borderRadius(10)
          .border({ width: 1, color: THEME.border })

          // 保质期天数
          this.FieldLabel('保质期天数')
          Row() {
            TextInput({ text: this.expiryDays })
              .width('60%')
              .height(46)
              .fontSize(15)
              .type(InputType.Number)
              .backgroundColor(THEME.card)
              .borderRadius(10)
              .border({ width: 1, color: THEME.border })
              .onChange((val: string) => { this.expiryDays = val; this.manualExpiry = true; })
            Blank()
            if (this.expiryDays.length > 0) {
              Text(`到期日：${this.computeExpiryDate()}`)
                .fontSize(11)
                .fontColor(THEME.text3)
                .maxLines(1)
            }
          }
          .width('100%')

          // 操作按钮
          Button(this.isEdit ? '保存修改' : '添加入库', { type: ButtonType.Capsule })
            .width('100%')
            .height(48)
            .fontSize(16)
            .fontColor('#FFFEFB')
            .backgroundColor(THEME.primary)
            .margin({ top: 24 })
            .onClick(() => this.save())

          if (this.isEdit) {
            Button('删除食材', { type: ButtonType.Capsule })
              .width('100%')
              .height(48)
              .fontSize(16)
              .fontColor(THEME.danger)
              .backgroundColor(THEME.dangerBg)
              .margin({ top: 10, bottom: 30 })
              .onClick(() => this.deleteFood())
          } else {
            Blank().height(30)
          }
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

  // 计算到期日显示
  computeExpiryDate(): string {
    const days = parseInt(this.expiryDays) || 0;
    const d = new Date(this.purchaseDate);
    d.setDate(d.getDate() + days);
    return formatDate(d);
  }

  // 日期选择器
  showDatePicker(): void {
    const d = new Date(this.purchaseDate);
    DatePickerDialog.show({
      start: new Date('2020-01-01'),
      end: new Date('2099-12-31'),
      selected: d,
      onAccept: (value: DatePickerResult) => {
        this.purchaseDate = formatDate(new Date(value.year, value.month, value.day));
      }
    });
  }

  zoneIcon(zone: string): string {
    if (zone === 'refrigerator') return '❄';
    if (zone === 'freezer') return '🧊';
    return '🌡';
  }

  @Builder
  FieldLabel(label: string): void {
    Text(label)
      .fontSize(13)
      .fontColor(THEME.text2)
      .margin({ top: 16, bottom: 8 })
  }
}
