// 相机识别页 —— 端云协同食材识别入口（独立路由页）
// 流程：
//   1) 双路预览：XComponent 硬件直绘显示 + ImageReceiver 取帧 →
//      LocalDetector 端侧检测 → 声明式 Rect 覆盖层实时叠加 YOLO 标注框。
//   2) 拍照定格：点击识别时把照片定格为「最后上传给云端的图片」展示。
//   3) 云端识别：果蔬优先，命中非果蔬兜底类/低置信度时降级通用识别 → Top-N 回填表单。
// 说明：Camera Kit / MindSpore Lite 需真机运行；模拟器走降级（预览/检测不可用，仍可打开）。

import { router, promptAction, display } from '@kit.ArkUI';
import { camera } from '@kit.CameraKit';
import { image } from '@kit.ImageKit';
import { common, abilityAccessCtrl, Permissions } from '@kit.AbilityKit';
import { BusinessError } from '@kit.BasicServicesKit';
import { hilog } from '@kit.PerformanceAnalysisKit';
import { THEME } from '../model/DataModel';
import { LocalDetector, Detection } from '../service/LocalDetector';
import { BaiduRecognizer, RecognizeResult, RecognizedFood } from '../service/BaiduRecognizer';
import { ImageCodec } from '../utils/ImageCodec';
import { RecognitionHolder } from '../service/RecognitionHolder';

const DOMAIN = 0x0001;
const TAG = 'CameraRecognitionPage';

@Entry
@Component
struct CameraRecognitionPage {
  // —— 相机资源 ——
  private xComponentController: XComponentController = new XComponentController();
  private surfaceId: string = '';
  private cameraManager: camera.CameraManager | null = null;
  private cameraInput: camera.CameraInput | null = null;
  private previewOutput: camera.PreviewOutput | null = null;   // XComponent 屏幕显示
  private framePreview: camera.PreviewOutput | null = null;    // ImageReceiver 取帧检测
  private frameReceiver: image.ImageReceiver | null = null;
  private photoOutput: camera.PhotoOutput | null = null;
  private session: camera.PhotoSession | null = null;
  private pendingResolve: ((p: image.PixelMap | null) => void) | null = null;

  // —— 定格展示 ——
  private frozenPixelMap: image.PixelMap | null = null;

  @State isCameraReady: boolean = false;
  @State isRecognizing: boolean = false;
  @State candidates: RecognizedFood[] = [];
  @State statusText: string = '对准食材，点击下方按钮识别';
  @State detections: Detection[] = [];        // 实时 YOLO 框（归一化）
  @State showFrozen: boolean = false;         // 是否定格到拍照画面
  @State frameState: string = '端侧检测未就绪'; // 实时检测状态提示
  // 相机预览实际显示区宽高（px，onAreaChange 动态测量），用于把检测框坐标换算到画面，避免写死比例
  @State previewW: number = 0;
  @State previewH: number = 0;
  // 取帧尺寸（px，动态取自 ImageReceiver 帧），作为检测框的基准坐标系
  @State frameW: number = 0;
  @State frameH: number = 0;
  // 预览旋转角（摄像机横屏帧在竖屏下旋转显示的角度，remember 由 getPreviewRotation 动态得出）
  private previewRotation: number = 90;
  // 相机预览区域宽高比（= 帧经旋转后的宽高比，动态计算），保证 XComponent 与画面同比例、
  // Cover 无放大裁剪、检测框坐标可直接线性映射，杜绝偏移/越界
  @State previewAspect: number = 9 / 16;

  async aboutToAppear(): Promise<void> {
    // 端侧模型初始化改为后台异步，不 await 阻塞页面渲染与相机启动：
    // 相机先打开预览（用户立刻看到画面），模型加载完成后实时框自动出现。
    // 取帧回调里已有「模型未就绪则跳过」的防护，晚到也不影响相机。
    LocalDetector.getInstance().init(getContext(this) as common.UIAbilityContext)
      .then(() => {
        if (LocalDetector.getInstance().isAvailable()) {
          this.frameState = '端侧检测已就绪';
        }
      })
      .catch((e: BusinessError) => {
        // 模型加载失败：忽略，端侧检测不可用，走整帧上传
        hilog.warn(DOMAIN, TAG, 'init detector failed: %{public}s', JSON.stringify(e));
      });
  }

  aboutToDisappear(): void {
    this.releaseCamera();
  }

  // 相机初始化入口（XComponent onLoad 回调）
  async openCamera(): Promise<void> {
    try {
      const context = getContext(this) as common.UIAbilityContext;
      const granted = await this.requestCameraPermission(context);
      if (!granted) {
        this.statusText = '未授予相机权限';
        return;
      }
      this.cameraManager = camera.getCameraManager(context);
      const cameras = this.cameraManager.getSupportedCameras();
      if (!cameras || cameras.length === 0) {
        this.statusText = '未检测到可用相机';
        return;
      }
      const device = cameras.find(c => c.cameraPosition === camera.CameraPosition.CAMERA_POSITION_BACK) ?? cameras[0];
      this.cameraInput = this.cameraManager.createCameraInput(device);
      await this.cameraInput.open();

      const caps = this.cameraManager.getSupportedOutputCapability(device, camera.SceneMode.NORMAL_PHOTO);
      const profiles = caps.previewProfiles;
      if (profiles.length === 0) {
        this.statusText = '无可用预览档位';
        return;
      }
      // 业界标准「双路预览」：用同一个 surface 能力分别创建两路 PreviewOutput——
      //   一路绑定 XComponent surfaceId（屏幕显示，硬件直绘，不黑屏）；
      //   一路绑定 ImageReceiver surfaceId（离屏取帧做检测）。
      // 这与 previewProfiles.length 无关：即使只有一个分辨率档位，也能建两路。
      const displayProfile = profiles[0];
      this.previewOutput = this.cameraManager.createPreviewOutput(displayProfile, this.surfaceId);

      // 取帧检测路：优先选与显示路相同格式的最小分辨率，降低推理开销
      const frameProfile = profiles[profiles.length - 1];
      // 计算旋转后的帧宽高比，作为相机显示区比例（保证 Cover 无放大裁剪 + 坐标线性映射）
      if (frameProfile.size.width > 0 && frameProfile.size.height > 0) {
        const hw = frameProfile.size.height / frameProfile.size.width;
        this.previewAspect = hw > 0 && hw !== 1 ? hw : 9 / 16;
      }
      // format 必须用 image.ImageFormat.JPEG（ImageReceiver 文档明确仅支持该值）。
      this.frameReceiver = image.createImageReceiver({ width: frameProfile.size.width, height: frameProfile.size.height },
        image.ImageFormat.JPEG, 8);
      const recvSurfaceId = await this.frameReceiver.getReceivingSurfaceId();
      this.framePreview = this.cameraManager.createPreviewOutput(frameProfile, recvSurfaceId);
      this.frameReceiver.on('imageArrival', () => {
        hilog.info(DOMAIN, TAG, 'imageArrival triggered');
        this.onFrameArrival();
      });
      this.frameState = '端侧检测通道已就绪';
      hilog.info(DOMAIN, TAG, 'dual preview ready, display=%{public}dx%{public}d frame=%{public}dx%{public}d',
        displayProfile.size.width, displayProfile.size.height,
        frameProfile.size.width, frameProfile.size.height);

      // 拍照输出
      const photoProfile = caps.photoProfiles[0];
      this.photoOutput = this.cameraManager.createPhotoOutput(photoProfile);
      this.photoOutput.on('photoAvailable', (err: BusinessError, photo: camera.Photo) => {
        this.onPhotoAvailable(err, photo);
      });

      this.session = this.cameraManager.createSession(camera.SceneMode.NORMAL_PHOTO) as camera.PhotoSession;
      this.session.beginConfig();
      this.session.addInput(this.cameraInput);
      this.session.addOutput(this.previewOutput!);       // XComponent 显示路
      this.session.addOutput(this.framePreview!);        // ImageReceiver 取帧检测路
      this.session.addOutput(this.photoOutput);          // 拍照路
      await this.session.commitConfig();
      await this.session.start();
      this.isCameraReady = true;
      // 获取预览实际旋转角并缓存：竖屏下相机预览帧(横屏传感器)会被旋转显示，
      // 检测框坐标基于未旋转帧，需在 toPreviewCoords 按此角度旋转到预览坐标系（业界做法）。
      try {
        const dispRot = display.getDefaultDisplaySync().rotation;
        const wantRot = dispRot * 90;
        const actualRot = this.previewOutput ? this.previewOutput.getPreviewRotation(wantRot) : 0;
        this.previewRotation = actualRot;
        hilog.info(DOMAIN, TAG, 'preview rotation want=%{public}d actual=%{public}d', wantRot, actualRot);
      } catch (e) {
        this.previewRotation = 90;   // 异常兜底：多数竖屏后摄为 90°
        hilog.warn(DOMAIN, TAG, 'get preview rotation failed, fallback 90');
      }
      this.statusText = '对准食材，点击下方按钮识别';
    } catch (e) {
      this.statusText = '相机不可用（需真机）';
    }
  }

  // 请求相机权限
  private async requestCameraPermission(context: common.UIAbilityContext): Promise<boolean> {
    try {
      const atManager = abilityAccessCtrl.createAtManager();
      const result = await atManager.requestPermissionsFromUser(context, ['ohos.permission.CAMERA'] as Permissions[]);
      return result.authResults.length > 0 && result.authResults[0] === 0;
    } catch (e) {
      return false;
    }
  }

  // ImageReceiver 取帧闭环（严格串行，一次 resolve 一帧）：
//   imageArrival → readNextImage → getComponent → createPixelMap(format映射) → detect → release。
// 注意：readNextImage 的 Image 必须 release 后才能接收下一帧；同一时刻只允许一个 readNextImage 在飞行。
  private processingFrame: boolean = false;
  private frameDetectCounter: number = 0;

  private onFrameArrival(): void {
    if (this.frameReceiver === null || this.showFrozen) {
      return;
    }
    if (this.processingFrame) {
      return;   // 上一帧仍在处理，本次 arrival 丢弃（下一帧再读），避免 buffer 堆积
    }
    this.processingFrame = true;
    this.frameReceiver.readNextImage((err: BusinessError, img: image.Image) => {
      if (err || img === undefined) {
        hilog.warn(DOMAIN, TAG, 'readNextImage failed: %{public}s', JSON.stringify(err));
        this.processingFrame = false;
        return;
      }
      this.processFrame(img).then(() => {
        this.processingFrame = false;
      }).catch((e: BusinessError) => {
        // e 可能无 enumerable 字段，JSON.stringify 会得到 {}，改用 code/message 明确输出
        const ec = (e && typeof e.code === 'number') ? e.code : 'n/a';
        const em = (e && typeof e.message === 'string') ? e.message : JSON.stringify(e);
        hilog.warn(DOMAIN, TAG, 'processFrame throw: code=%{public}s msg=%{public}s', String(ec), em);
        this.processingFrame = false;
      });
    });
  }

  // 处理一帧：转 PixelMap → （限频）检测 → 更新框 → 释放
  private async processFrame(img: image.Image): Promise<void> {
    try {
      // 记录取帧尺寸，作为检测框的基准坐标系（用于显示层换算，动态、非硬编码）
      if (this.frameW !== img.size.width || this.frameH !== img.size.height) {
        this.frameW = img.size.width;
        this.frameH = img.size.height;
      }
      // 隔帧处理，降低 CPU（官方 FAQ 用定时器限频；这里每 2 帧检测 1 次）
      this.frameDetectCounter = (this.frameDetectCounter + 1) % 2;
      const doDetect = this.frameDetectCounter === 0 && LocalDetector.getInstance().isAvailable();

      if (doDetect) {
        const pm = await this.createPixelMapFromComponent(img);
        if (pm !== null) {
          try {
            const res = await LocalDetector.getInstance().detect(pm);
            if (res.available) {
              this.detections = res.detections;
              this.frameState = res.detections.length > 0 ? '已框选目标' : '检测中，未命中目标';
              hilog.info(DOMAIN, TAG, 'detect: boxes=%{public}d', res.detections.length);
            }
          } finally {
            pm.release();
          }
        }
      }
    } finally {
      img.release();   // 必须先 release，ImageReceiver 才能继续接收下一帧
    }
  }

  // 按 Image 实际 format 转 PixelMap，并按每像素字节数正确处理 stride（对比字节，非像素数）。
  // 相机预览格式不固定：fmt=12→RGBA_8888(4B/px)，YUV_420_SP→NV21(1B/px for Y/UV行)。
  private async createPixelMapFromComponent(img: image.Image): Promise<image.PixelMap | null> {
    let component: image.Component;
    try {
      component = await img.getComponent(image.ComponentType.JPEG);
    } catch (e) {
      hilog.warn(DOMAIN, TAG, 'step getComponent failed: %{public}s', JSON.stringify(e));
      return null;
    }
    if (component === undefined || component.byteBuffer === undefined) {
      hilog.warn(DOMAIN, TAG, 'step component/null: buf absent');
      return null;
    }
    const width = img.size.width;
    const height = img.size.height;
    const stride = component.rowStride;
    // fmt: image 组件返回的实际像素格式（12=RGBA_8888, 25=NV21, 35/36=P010）
    const fmt = img.format;
    // 映射到 PixelMapFormat
    let pxf = image.PixelMapFormat.NV21;
    if (fmt === 12) {
      pxf = image.PixelMapFormat.RGBA_8888;   // 4 字节/像素
    }
    const bytesPerPx = (pxf === image.PixelMapFormat.RGBA_8888) ? 4 : 1;
    const rowBytes = width * bytesPerPx;       // 每行期望字节数

    try {
      // stride 与期望行字节数一致 → 直接创建（避免无谓拷贝）
      if (stride === rowBytes) {
        return await image.createPixelMap(component.byteBuffer, {
          size: { width: width, height: height },
          srcPixelFormat: pxf
        });
      }
      // stride > rowBytes：逐行剔除尾部填充后创建
      // RGBA：每行取 width*4 字节；NV21：Y/UV 行各取 width 字节（共 height*1.5 行）
      const totalRows = (pxf === image.PixelMapFormat.RGBA_8888) ? height : height * 1.5;
      const dstArr = new Uint8Array(rowBytes * height);
      for (let j = 0; j < totalRows; j++) {
        const srcBuf = new Uint8Array(component.byteBuffer, j * stride, rowBytes);
        dstArr.set(srcBuf, j * rowBytes);
      }
      // NV21 需保留 UV 平面（rowBytes*height 只覆盖 Y），改用完整 1.5 倍缓冲
      let finalBuf = dstArr.buffer;
      if (pxf === image.PixelMapFormat.NV21) {
        finalBuf = this.stripStrideNV21(component.byteBuffer, width, height, stride);
      }
      return await image.createPixelMap(finalBuf, {
        size: { width: width, height: height },
        srcPixelFormat: pxf
      });
    } catch (e) {
      const ec = (e && typeof (e as BusinessError).code === 'number') ? String((e as BusinessError).code) : 'n/a';
      const em = (e && typeof (e as BusinessError).message === 'string') ? (e as BusinessError).message : JSON.stringify(e);
      hilog.warn(DOMAIN, TAG, 'step createPixelMap failed fmt=%{public}d w=%{public}d h=%{public}d stride=%{public}d code=%{public}s msg=%{public}s',
        fmt, width, height, stride, ec, em);
      return null;
    }
  }

  // NV21 去 stride：Y 平面共 height 行，UV 平面共 height/2 行，逐行取 width 字节
  private stripStrideNV21(buf: ArrayBuffer, width: number, height: number, stride: number): ArrayBuffer {
    const dst = new Uint8Array(width * height * 1.5);
    const totalRows = height * 1.5;
    for (let j = 0; j < totalRows; j++) {
      const srcBuf = new Uint8Array(buf, j * stride, width);
      dst.set(srcBuf, j * width);
    }
    return dst.buffer;
  }

  // 释放相机资源
  private async releaseCamera(): Promise<void> {
    try {
      if (this.photoOutput) {
        this.photoOutput.off('photoAvailable');
      }
      if (this.frameReceiver) {
        this.frameReceiver.off('imageArrival');
      }
      if (this.session) {
        await this.session.stop();
        await this.session.release();
      }
    } catch (e) {
      // 忽略
    }
    this.session = null;
    this.previewOutput = null;
    this.framePreview = null;
    this.photoOutput = null;
    this.frameReceiver = null;
    if (this.cameraInput) {
      try {
        await this.cameraInput.close();
      } catch (e) {
        // 忽略
      }
    }
    this.cameraInput = null;
    this.cameraManager = null;
    this.isCameraReady = false;
  }

  // 拍照 → 定格画面 → 端云识别
  async captureAndRecognize(): Promise<void> {
    if (!this.isCameraReady || this.photoOutput === null) {
      promptAction.showToast({ message: '相机尚未就绪', duration: 1500 });
      return;
    }
    if (this.isRecognizing) {
      return;
    }
    // 恢复实时预览（避免上一次定格残留）
    this.showFrozen = false;
    this.frozenPixelMap = null;
    this.detections = [];   // 清空检测框（声明式 UI 自动同步）

    this.isRecognizing = true;
    this.statusText = '识别中...';
    try {
      const pixelMap = await this.captureFrame();
      if (pixelMap === null) {
        this.statusText = '拍照失败，请重试';
        return;
      }
      // 定格为最后上传给云端的图片
      this.frozenPixelMap = pixelMap;
      this.showFrozen = true;
      await this.recognize(pixelMap);
    } finally {
      this.isRecognizing = false;
    }
  }

  // 触发拍照，返回 Promise（由 photoAvailable 事件 resolve）
  private captureFrame(): Promise<image.PixelMap | null> {
    return new Promise<image.PixelMap | null>((resolve) => {
      if (this.photoOutput === null) {
        resolve(null);
        return;
      }
      this.pendingResolve = resolve;
      const setting: camera.PhotoCaptureSetting = {
        quality: camera.QualityLevel.QUALITY_LEVEL_MEDIUM,
        rotation: camera.ImageRotation.ROTATION_0
      };
      this.photoOutput.capture(setting, (err: BusinessError) => {
        if (err) {
          this.pendingResolve = null;
          resolve(null);
        }
      });
    });
  }

  // photoAvailable 回调：Photo → PixelMap
  private onPhotoAvailable(err: BusinessError, photo: camera.Photo): void {
    const resolve = this.pendingResolve;
    if (resolve === null) return;
    if (err || photo === undefined || photo.main === undefined) {
      this.pendingResolve = null;
      resolve(null);
      return;
    }
    const imageObj = photo.main;
    imageObj.getComponent(image.ComponentType.JPEG, async (errCode: BusinessError, component: image.Component) => {
      if (errCode || component === undefined || component.byteBuffer === undefined) {
        imageObj.release();
        this.pendingResolve = null;
        resolve(null);
        return;
      }
      try {
        const source = image.createImageSource(component.byteBuffer as ArrayBuffer);
        const pixelMap = await source.createPixelMap();
        source.release();
        this.pendingResolve = null;
        resolve(pixelMap);
      } catch (e) {
        this.pendingResolve = null;
        resolve(null);
      } finally {
        imageObj.release();
      }
    });
  }

  // 端云协同识别：端侧检测(可选，裁剪主体) → 图像预处理 → 云端识别
  private async recognize(pixelMap: image.PixelMap): Promise<void> {
    let uploadMap: image.PixelMap = pixelMap;
    const detResult = await LocalDetector.getInstance().detect(pixelMap);
    if (detResult.available && detResult.detections.length > 0) {
      uploadMap = await this.cropToDetection(pixelMap, detResult.detections[0]);
    }

    const base64Str = await ImageCodec.pixelMapToBase64(uploadMap);
    const result: RecognizeResult = await BaiduRecognizer.getInstance().recognize(base64Str);
    if (!result.ok || result.candidates.length === 0) {
      this.candidates = [];
      this.statusText = result.message.length > 0 ? result.message : '未识别到食材';
      return;
    }
    this.candidates = result.candidates;
    this.statusText = '已定格画面，点击下方结果填入表单';
  }

  // 按检测框裁剪主体区域（归一化框 → 像素坐标，原地裁剪）
  private async cropToDetection(src: image.PixelMap, det: Detection): Promise<image.PixelMap> {
    const info = await src.getImageInfo();
    const cropX = Math.max(0, Math.floor(det.left * info.size.width));
    const cropY = Math.max(0, Math.floor(det.top * info.size.height));
    const cropW = Math.min(info.size.width - cropX, Math.max(1, Math.ceil(det.width * info.size.width)));
    const cropH = Math.min(info.size.height - cropY, Math.max(1, Math.ceil(det.height * info.size.height)));
    const region: image.Region = {
      x: cropX,
      y: cropY,
      size: { width: cropW, height: cropH }
    };
    await src.crop(region);
    return src;
  }

  // 把「帧归一化坐标」换算为「预览方向归一化坐标」。
  // 相机 Stack 的宽高比已设为「帧旋转后的比例」(previewAspect)，
  // 故 XComponent 内画面与 Stack 完全同比例、无放大裁剪、无黑边；
  // 只需把检测框按 previewRotation 旋转到预览方向，再做线性缩放即可。
  // 全部依赖运行时测量的 frame W/H 与 previewRotation，无硬编码比例。
  private toPreviewCoords(d: Detection): Detection {
    const rot = ((this.previewRotation % 360) + 360) % 360;
    let x0 = d.left, y0 = d.top;
    let x1 = d.left + d.width, y1 = d.top + d.height;
    if (rot === 90) {
      const ox0 = 1 - y1, oy0 = x0;
      const ox1 = 1 - y0, oy1 = x1;
      x0 = ox0; y0 = oy0; x1 = ox1; y1 = oy1;
    } else if (rot === 270) {
      const ox0 = y0, oy0 = 1 - x1;
      const ox1 = y1, oy1 = 1 - x0;
      x0 = ox0; y0 = oy0; x1 = ox1; y1 = oy1;
    } else if (rot === 180) {
      x0 = 1 - (d.left + d.width); y0 = 1 - (d.top + d.height);
      x1 = 1 - d.left; y1 = 1 - d.top;
    }
    // 旋转后坐标已在 [0,1]，Stack 与画面同比例，直接返回作为百分比
    return {
      left: x0,
      top: y0,
      width: x1 - x0,
      height: y1 - y0,
      confidence: d.confidence
    };
  }

  // 选择结果回填
  @Builder
  box(d: Detection) {
    // 全部内联调用 toPreviewCoords，避免在 Builder 内声明局部变量（ArkTS UI 语法限制）
    Rect()
      .width(`${(this.toPreviewCoords(d).width * 100).toFixed(2)}%`)
      .height(`${(this.toPreviewCoords(d).height * 100).toFixed(2)}%`)
      .position({
        x: `${(this.toPreviewCoords(d).left * 100).toFixed(2)}%`,
        y: `${(this.toPreviewCoords(d).top * 100).toFixed(2)}%`
      })
      .fill(Color.Transparent)
      .stroke('#FF4D4F')
      .strokeWidth(3)
  }

  private onPreviewAreaChange(v: Area): void {
    const w = v.width as number;
    const h = v.height as number;
    if (w > 0 && h > 0) {
      this.previewW = w;
      this.previewH = h;
    }
  }

  selectResult(name: string): void {
    RecognitionHolder.set(name);
    promptAction.showToast({ message: `已识别：${name}`, duration: 1000 });
    setTimeout(() => router.back(), 400);
  }

  build(): void {
    Column() {
      Row() {
        Text('‹').fontSize(22).fontColor(THEME.text).width(32).height(32)
          .textAlign(TextAlign.Center).onClick(() => router.back())
        Text('拍照识别食材').fontSize(18).fontWeight(FontWeight.Bold).fontColor(THEME.text)
          .layoutWeight(1).textAlign(TextAlign.Center)
        Text('').width(32)
      }
      .width('100%').height(48).padding({ left: 8, right: 8 })
      .backgroundColor(THEME.card).border({ width: { bottom: 1 }, color: THEME.border })

      // 相机预览层 + YOLO 框覆盖层 / 定格画面
      // 相机画面自然充满（不变形）；框层用 onAreaChange 记录的显示区宽高，
      // 结合取帧宽高做 contain 换算，把检测框坐标精确映射到画面可视区域（全动态，非硬编码）。
      Stack() {
        if (this.showFrozen && this.frozenPixelMap) {
          // 定格显示最后上传给云端的图片
          Image(this.frozenPixelMap)
            .width('100%').height('100%')
            .objectFit(ImageFit.Cover)
          if (this.frozenPixelMap) {
            Text('画面已定格（识别中）')
              .fontSize(11).fontColor('#FFFFFF').backgroundColor('#66000000')
              .padding({ left: 8, right: 8, top: 3, bottom: 3 }).borderRadius(4)
              .margin({ top: 8 })
              .alignSelf(ItemAlign.Start)
          }
        } else {
          // 相机预览：始终用 XComponent 硬件直绘（不黑屏），框层叠加其上
          XComponent({ id: 'cameraPreview', type: XComponentType.SURFACE, controller: this.xComponentController })
            .width('100%').height('100%')
            .onLoad(() => {
              this.surfaceId = this.xComponentController.getXComponentSurfaceId();
              this.openCamera();
            })
          // 实时 YOLO 标注框覆盖层：把「帧归一化坐标」换算为「显示区归一化坐标」后绘制
          ForEach(this.detections, (d: Detection, idx: number) => {
            this.box(d)
          }, (d: Detection, idx: number) => `${idx}_${d.left.toFixed(3)}_${d.top.toFixed(3)}`)
        }
      }
      .width('100%')
      .aspectRatio(this.previewAspect)
      .backgroundColor('#000000')
      .onAreaChange((oldValue: Area, newValue: Area) => {
        this.onPreviewAreaChange(newValue);
      })

      // 实时检测状态
      Text(this.showFrozen ? this.statusText : this.frameState)
        .fontSize(13).fontColor(THEME.text2)
        .width('100%').padding({ left: 16, right: 16, top: 8, bottom: 4 })
        .textAlign(TextAlign.Center)

      if (this.candidates.length > 0) {
        List() {
          ForEach(this.candidates, (c: RecognizedFood, idx: number) => {
            ListItem() {
              Row() {
                Text(c.name).fontSize(15).fontColor(THEME.text).layoutWeight(1)
                Text(`${(c.confidence * 100).toFixed(0)}%`).fontSize(13).fontColor(THEME.primary)
                Text(c.sourceApi === 'ingredient' ? '果蔬' : '通用').fontSize(11).fontColor(THEME.text3)
                  .margin({ left: 8 })
              }
              .width('100%').padding(12)
            }
            .onClick(() => this.selectResult(c.name))
          }, (c: RecognizedFood, idx: number) => `${idx}_${c.name}`)
        }
        .height(140).scrollBar(BarState.Off)
      }

      Button(this.isRecognizing ? '识别中...' : '识别当前画面', { type: ButtonType.Capsule })
        .width('90%').height(48).fontSize(16).fontColor('#FFFEFB')
        .backgroundColor(THEME.primary)
        .margin({ top: 12, bottom: 20 })
        .enabled(!this.isRecognizing)
        .onClick(() => this.captureAndRecognize())
    }
    .width('100%').height('100%').backgroundColor(THEME.bg)
  }
}