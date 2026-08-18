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
import { LOCAL_MODEL_NAME, DETECT_CONF_THRESHOLD } from '../model/CloudConfig';

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

export class LocalDetector {
  private static instance: LocalDetector | null = null;
  private model: mindSporeLite.Model | null = null;
  private available: boolean = false;

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
    } catch (e) {
      this.available = false;
      this.model = null;
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
        return { available: true, detections: [] };
      }
      const inputBuffer = await this.preprocess(src);
      inputs[0].setData(inputBuffer);
      const outputs = await this.model.predict(inputs);
      return { available: true, detections: this.postprocess(outputs) };
    } catch (e) {
      return { available: false, detections: [] };
    }
  }

  // 图像 → 模型输入（640×640 RGB 归一化，NCHW Float32）
  private async preprocess(src: image.PixelMap): Promise<ArrayBuffer> {
    const info = await src.getImageInfo();
    const ratioX = MODEL_INPUT_SIZE / info.size.width;
    const ratioY = MODEL_INPUT_SIZE / info.size.height;
    if (ratioX !== 1 || ratioY !== 1) {
      await src.scale(ratioX, ratioY); // 原地缩放到模型输入尺寸
    }
    const pixelBuffer = new ArrayBuffer(MODEL_INPUT_SIZE * MODEL_INPUT_SIZE * 4);
    const area: image.PositionArea = {
      pixels: pixelBuffer,
      offset: 0,
      stride: MODEL_INPUT_SIZE * 4,
      region: { x: 0, y: 0, size: { width: MODEL_INPUT_SIZE, height: MODEL_INPUT_SIZE } }
    };
    await src.readPixels(area);
    const rgba = new Uint8Array(pixelBuffer);
    const planeSize = MODEL_INPUT_SIZE * MODEL_INPUT_SIZE;
    const input = new Float32Array(3 * planeSize);
    for (let i = 0; i < planeSize; i++) {
      input[i] = rgba[i * 4] / 255.0;                 // R
      input[planeSize + i] = rgba[i * 4 + 1] / 255.0; // G
      input[2 * planeSize + i] = rgba[i * 4 + 2] / 255.0; // B
    }
    return input.buffer;
  }

  // 后处理：YOLOv8 输出 [1, 4+CLASSES, anchors]，取全局最高类别置信度的框并归一化。
  // 说明：具体输出形状以后 concerto 转换得到的 .ms 模型为准，此处按标准 YOLOv8 NCHW 输出解析。
  private postprocess(outputs: mindSporeLite.MSTensor[]): Detection[] {
    if (outputs.length === 0) return [];
    const data = outputs[0].getData();
    if (data.byteLength === 0) return [];
    const arr = new Float32Array(data);
    const stride = 4 + NUM_CLASSES;
    const numBoxes = Math.floor(arr.length / stride);
    let bestBox = -1;
    let bestConf = 0;
    for (let i = 0; i < numBoxes; i++) {
      const base = i * stride;
      for (let c = 0; c < NUM_CLASSES; c++) {
        const conf = arr[base + 4 + c];
        if (conf > bestConf) {
          bestConf = conf;
          bestBox = i;
        }
      }
    }
    if (bestBox < 0 || bestConf < DETECT_CONF_THRESHOLD) {
      return [];
    }
    const base = bestBox * stride;
    const cx = arr[base] / MODEL_INPUT_SIZE;
    const cy = arr[base + 1] / MODEL_INPUT_SIZE;
    const w = arr[base + 2] / MODEL_INPUT_SIZE;
    const h = arr[base + 3] / MODEL_INPUT_SIZE;
    const det: Detection = {
      left: cx - w / 2,
      top: cy - h / 2,
      width: w,
      height: h,
      confidence: bestConf
    };
    return [det];
  }
}