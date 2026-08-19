// 端侧检测服务 —— MindSpore Lite + YOLOv8n
// 职责：1) 从 rawfile 加载 .ms 模型（缺失/失败时自动降级）；
//       2) 对相机帧做端侧推理，输出检测框 + 置信度；
//       3) 模型不可用时返回 available=false，上层（相机页）走「整帧上传云端」兜底。
//
// 注意：MindSpore Lite 需真机运行（模拟器不支持）。为实现按需加载，本模块仅在
//       CameraRecognitionPage（独立路由页）内被引用，应用启动时不会触发加载。

import mindSporeLite from '@ohos.ai.mindSporeLite';
import image from '@ohos.multimedia.image';
import { common } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { LOCAL_MODEL_NAME, DETECT_CONF_THRESHOLD } from '../model/CloudConfig';

const DOMAIN = 0x0001;
const TAG = 'LocalDetector';

// 归一化检测框（相对值 0-1）
export interface Detection {
  left: number;
  top: number;
  width: number;
  height: number;
  confidence: number;
}

export interface DetectResult {
  available: boolean;
  detections: Detection[];
}

// YOLOv8n 输入边长与 COCO 类别数
const MODEL_INPUT_SIZE: number = 640;
const NUM_CLASSES: number = 80;
// YOLOv8n 每输出锚点数 = 3 个检测头(8/16/32 下采样) anchor 数之和：640*640*(1/64 + 1/256 + 1/1024)=8400
const NUM_ANCHORS: number = 8400;

export class LocalDetector {
  private static instance: LocalDetector | null = null;
  private model: mindSporeLite.Model | null = null;
  private available: boolean = false;
  // letterbox 映射参数：模型 640×640 坐标 ↔ 原始帧坐标 的换算依据
  private origW: number = 1;
  private origH: number = 1;
  private scale: number = 1;      // 原始帧 → 640 画布的等比缩放系数
  private padX: number = 0;       // 640 画布上水平居中偏移
  private padY: number = 0;       // 640 画布上垂直居中偏移

  static getInstance(): LocalDetector {
    if (LocalDetector.instance === null) {
      LocalDetector.instance = new LocalDetector();
    }
    return LocalDetector.instance;
  }

  // 加载模型（相机页 aboutToAppear 调用）。缺失/失败一律降级，不抛异常。
  async init(context: common.UIAbilityContext): Promise<void> {
    try {
      const raw = await context.resourceManager.getRawFileContent(LOCAL_MODEL_NAME);
      const buffer = raw.buffer.slice(raw.byteOffset, raw.byteOffset + raw.byteLength) as ArrayBuffer;
      this.model = await mindSporeLite.loadModelFromBuffer(buffer);
      this.available = true;
      hilog.info(DOMAIN, TAG, 'yolov8n.ms loaded ok');
    } catch (e) {
      this.available = false;
      this.model = null;
      // 打印加载失败原因（如模型版本与设备 MindSpore Lite runtime 不匹配等），
      // 便于在 DevEco Log / hilog 中定位。
      hilog.error(DOMAIN, TAG, 'load model failed, msg=%{public}s', JSON.stringify(e));
    }
  }

  isAvailable(): boolean {
    return this.available;
  }

  // 端侧推理：预处理 → predict → 后处理（最高置信度单框）。
  async detect(src: image.PixelMap): Promise<DetectResult> {
    if (!this.available || this.model === null) {
      return { available: false, detections: [] };
    }
    try {
      const inputs = this.model.getInputs();
      if (inputs.length === 0) {
        hilog.warn(DOMAIN, TAG, 'detect: model has no inputs');
        return { available: true, detections: [] };
      }
      const inputBuffer = await this.preprocess(src);
      inputs[0].setData(inputBuffer);
      const outputs = await this.model.predict(inputs);
      return { available: true, detections: this.postprocess(outputs) };
    } catch (e) {
      // JSON.stringify(BusinessError) 可能是 {}，改用 code/message 输出以定位
      const ec = (e && typeof (e as BusinessError).code === 'number') ? String((e as BusinessError).code) : 'n/a';
      const em = (e && typeof (e as BusinessError).message === 'string') ? (e as BusinessError).message : JSON.stringify(e);
      hilog.error(DOMAIN, TAG, 'detect failed: code=%{public}s msg=%{public}s', ec, em);
      return { available: false, detections: [] };
    }
  }

  // 图像 → 模型输入（640×640 RGB 归一化，NCHW Float32）。
  // 采用 letterbox：等比缩放并居中放入 640 画布、四周灰边，避免拉伸扭曲宽高比，
  // 并记录 scale/pad 供 postprocess 把模型坐标还原回原始帧坐标。
  private async preprocess(src: image.PixelMap): Promise<ArrayBuffer> {
    const info = await src.getImageInfo();
    this.origW = info.size.width;
    this.origH = info.size.height;
    // 等比缩放系数（保持宽高比）
    const scale = Math.min(MODEL_INPUT_SIZE / this.origW, MODEL_INPUT_SIZE / this.origH);
    this.scale = scale;
    const newW = Math.round(this.origW * scale);
    const newH = Math.round(this.origH * scale);
    // 640 画布上居中偏移
    this.padX = Math.round((MODEL_INPUT_SIZE - newW) / 2);
    this.padY = Math.round((MODEL_INPUT_SIZE - newH) / 2);

    // 等比缩放到目标尺寸
    if (scale !== 1) {
      await src.scale(newW / this.origW, newH / this.origH);
    }
    const scaledBuffer = new ArrayBuffer(newW * newH * 4);
    await src.readPixels({
      pixels: scaledBuffer,
      offset: 0,
      stride: newW * 4,
      region: { x: 0, y: 0, size: { width: newW, height: newH } }
    });
    const rgba = new Uint8Array(scaledBuffer);

    // 拷贝到 640×640 画布并归一化为 NCHW Float32（灰边=114 / 255 ≈ 0.447）
    const input = new Float32Array(3 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE);
    const padVal = 114 / 255.0;
    for (let y = 0; y < MODEL_INPUT_SIZE; y++) {
      for (let x = 0; x < MODEL_INPUT_SIZE; x++) {
        const sy = y - this.padY;
        const sx = x - this.padX;
        let r = padVal, g = padVal, b = padVal;
        if (sy >= 0 && sy < newH && sx >= 0 && sx < newW) {
          const idx = (sy * newW + sx) * 4;
          r = rgba[idx] / 255.0;
          g = rgba[idx + 1] / 255.0;
          b = rgba[idx + 2] / 255.0;
        }
        const oi = y * MODEL_INPUT_SIZE + x;
        input[oi] = r;
        input[MODEL_INPUT_SIZE * MODEL_INPUT_SIZE + oi] = g;
        input[2 * MODEL_INPUT_SIZE * MODEL_INPUT_SIZE + oi] = b;
      }
    }
    return input.buffer;
  }

  // 后处理：模型输出 [1, 4+NUM_CLASSES, NUM_ANCHORS]，channel-major（每个 channel 一行 8400 个锚点）。
  //   - 前 4 个 channel：cx、cy、w、h（单位：像素，相对模型输入 640×640）
  //   - 其后 80 个 channel：各类别 logits（需 sigmoid 得置信度）
  // 按全局最高置信度取单框，并用 preprocess 记录的 letterbox 参数还原到原始帧坐标（返回归一化 [0,1]）。
  private postprocess(outputs: mindSporeLite.MSTensor[]): Detection[] {
    if (outputs.length === 0) return [];
    const data = outputs[0].getData();
    if (data.byteLength === 0) return [];
    const arr = new Float32Array(data);
    const stride = 4 + NUM_CLASSES;
    const numAnchors = NUM_ANCHORS;
    if (arr.length < stride * numAnchors) return [];

    let bestAnchor = -1;
    let bestConf = 0;
    for (let i = 0; i < numAnchors; i++) {
      for (let c = 0; c < NUM_CLASSES; c++) {
        // sigmoid 将 logits 映射到 [0,1]
        const conf = 1 / (1 + Math.exp(-arr[(4 + c) * numAnchors + i]));
        if (conf > bestConf) {
          bestConf = conf;
          bestAnchor = i;
        }
      }
    }
    if (bestAnchor < 0 || bestConf < DETECT_CONF_THRESHOLD) {
      return [];
    }
    // 模型 640 系坐标（像素）
    const modelCx = arr[bestAnchor];
    const modelCy = arr[1 * numAnchors + bestAnchor];
    const modelW = arr[2 * numAnchors + bestAnchor];
    const modelH = arr[3 * numAnchors + bestAnchor];
    // 还原到原始帧像素坐标：orig = (model - pad) / scale
    const ox = (modelCx - this.padX) / this.scale;
    const oy = (modelCy - this.padY) / this.scale;
    const ow = modelW / this.scale;
    const oh = modelH / this.scale;
    const det: Detection = {
      left: (ox - ow / 2) / this.origW,
      top: (oy - oh / 2) / this.origH,
      width: ow / this.origW,
      height: oh / this.origH,
      confidence: bestConf
    };
    // 越界防护：框内坐标夹取到 [0,1]，避免百分比越界盖到画面外（如下方按钮区）
    const clamped = this.clampToUnit(det);
    hilog.info(DOMAIN, TAG, 'det: l=%{public}s t=%{public}s w=%{public}s h=%{public}s c=%{public}s',
      clamped.left.toFixed(3), clamped.top.toFixed(3), clamped.width.toFixed(3), clamped.height.toFixed(3),
      clamped.confidence.toFixed(3));
    return [clamped];
  }

  // 把归一化矩形限制在 [0,1] 内（并裁剪超出部分），杜绝越界渲染
  private clampToUnit(d: Detection): Detection {
    const left = Math.min(1.0, Math.max(0.0, d.left));
    const top = Math.min(1.0, Math.max(0.0, d.top));
    const right = Math.min(1.0, Math.max(0.0, d.left + d.width));
    const bottom = Math.min(1.0, Math.max(0.0, d.top + d.height));
    return {
      left: left,
      top: top,
      width: Math.max(0.0, right - left),
      height: Math.max(0.0, bottom - top),
      confidence: d.confidence
    };
  }
}