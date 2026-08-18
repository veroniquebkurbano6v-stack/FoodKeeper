// 识别结果传递 Holder —— 相机识别页把选中的食材名称写回 AddFoodPage
// 模式与 AgentHolder 一致：跨页面传递一次性信号。

export class RecognitionHolder {
  private static result: string = '';

  static set(name: string): void {
    RecognitionHolder.result = name;
  }

  static consume(): string {
    const v = RecognitionHolder.result;
    RecognitionHolder.result = '';
    return v;
  }
}