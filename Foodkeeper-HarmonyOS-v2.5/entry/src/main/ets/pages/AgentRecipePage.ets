// AI 智能体菜谱推荐页（独立路由页）
// 单独成页原因：仅在本页静态 import AgentFrameworkKit，路由按需加载，
//   避免应用启动时加载 com.huawei.hmsapp.hiai.hsp/agentKitHsp 导致模拟器崩溃。
// 使用：RecipePage 检测到智能体支持后，通过 router.pushUrl 携带 agentId 与 queryText 跳转至此。
// 真机 + 真实 agentId 即可运行，无需改动本页代码。
//
// 降级机制：智能体运行出错（agentId 失效/网络异常/HSP 不可用）时，
//   通过 AgentHolder.setFallback() 置位降级信号，延迟返回 RecipePage，
//   RecipePage 检测到信号后自动走本地 RecipeEngine 生成菜谱，保证用户无感知中断。

import { router, promptAction } from '@kit.ArkUI';
import { BusinessError } from '@kit.BasicServicesKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { FunctionComponent, FunctionController } from '@kit.AgentFrameworkKit';
import { THEME } from '../model/DataModel';
import { AgentHolder } from '../utils/AgentBridge';

const DOMAIN = 0x0001;
const TAG = 'AgentRecipePage';

@Entry
@Component
struct AgentRecipePage {
  private agentId: string = '';
  private queryText: string = '';
  private controller: FunctionController = new FunctionController();
  @State hasError: boolean = false;

  aboutToAppear(): void {
    const params = router.getParams() as Record<string, string>;
    if (params) {
      this.agentId = params.agentId ?? '';
      this.queryText = params.queryText ?? '';
    }
  }

  goBack(): void {
    router.back();
  }

  // 智能体出错：记录日志 + 置位降级信号 + 提示 + 延迟返回
  onError(err: BusinessError): void {
    hilog.error(DOMAIN, TAG, `agent error: code=${err.code}, msg=${err.message}`);
    this.hasError = true;
    AgentHolder.setFallback();
    promptAction.showToast({ message: '智能体调用失败，已切换本地引擎', duration: 1800 });
    // 延迟返回，让用户看到提示
    setTimeout(() => {
      router.back();
    }, 1000);
  }

  build(): void {
    Column() {
      // 顶部标题栏
      Row() {
        Text('‹')
          .fontSize(22)
          .fontColor(THEME.text)
          .onClick(() => this.goBack())
          .width(32)
          .height(32)
          .textAlign(TextAlign.Center)
        Text('AI 智能菜谱')
          .fontSize(18)
          .fontWeight(FontWeight.Bold)
          .fontColor(THEME.text)
          .layoutWeight(1)
          .textAlign(TextAlign.Center)
        Text('')
          .width(32)
      }
      .width('100%')
      .height(48)
      .padding({ left: 8, right: 8 })
      .backgroundColor(THEME.card)
      .border({ width: { bottom: 1 }, color: THEME.border })

      if (this.agentId.length === 0) {
        // 参数错误：agentId 缺失
        Column() {
          Text('⚠️ 参数错误')
            .fontSize(14)
            .fontColor(THEME.danger)
          Text('未获取到 agentId，请返回重试')
            .fontSize(12)
            .fontColor(THEME.text3)
            .margin({ top: 6 })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else if (this.hasError) {
        // 已出错：展示降级提示，等待自动返回
        Column() {
          Text('🔌')
            .fontSize(32)
          Text('智能体暂时不可用')
            .fontSize(15)
            .fontWeight(FontWeight.Bold)
            .fontColor(THEME.text)
            .margin({ top: 10 })
          Text('正在为你切换本地菜谱引擎...')
            .fontSize(12)
            .fontColor(THEME.text3)
            .margin({ top: 6 })
          LoadingProgress()
            .width(28)
            .height(28)
            .color(THEME.primary)
            .margin({ top: 16 })
        }
        .width('100%')
        .layoutWeight(1)
        .justifyContent(FlexAlign.Center)
      } else {
        // 正常：内嵌小艺智能体 FunctionComponent
        FunctionComponent({
          agentId: this.agentId,
          onError: (err: BusinessError) => this.onError(err),
          options: {
            title: '问 AI 推荐菜谱',
            queryText: this.queryText,
            isShowShadow: true
          },
          controller: this.controller
        })
      }
    }
    .width('100%')
    .height('100%')
    .backgroundColor(THEME.bg)
  }
}
