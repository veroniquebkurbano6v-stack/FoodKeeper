// 相机识别页 —— 端云协同食材识别入口（独立路由页）
// 流程：
//   1) 实时取帧：预览输出一路到 XComponent 屏幕显示，一路到 ImageReceiver 抽帧 →
//      LocalDetector 端侧检测 → 声明式 Rect 覆盖层实时显示 YOLO 标注框
//      （@State detections 驱动自动重绘；模型未就绪时无框）。
//   2) 拍照定格：点击识别时把照片定格为「最后上传给云端的图片」展示。
//   3) 云端识别：果蔬优先，命中非果蔬兜底类/低置信度时降级通用识别 → Top-N 回填表单。
// 说明：Camera Kit / MindSpore Lite 需真机运行；模拟器走降级（预览/检测不可用，仍可打开）。

import { router, promptAction } from '@kit.ArkUI';
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
  private isFrameBusy: boolean = false;

  // —— 定格展示 ——
  private frozenPixelMap: image.PixelMap | null = null;

  // —— 兜底单接收器模式（设备仅一个预览档位时启用）——
  @State singleMode: boolean = false;                     // true=单接收器显示预览
  @State singlePreview: image.PixelMap | null = null;     // 单接收器模式当前显示帧

  @State isCameraReady: boolean = false;
  @State isRecognizing: boolean = false;
  @State candidates: RecognizedFood[] = [];
  @State statusText: string = '对准食材，点击下方按钮识别';
  @State detections: Detection[] = [];        // 实时 YOLO 框（归一化）
  @State showFrozen: boolean = false;         // 是否定格到拍照画面
  @State frameState: string = '端侧检测未就绪'; // 实时检测状态提示

  async aboutToAppear(): Promise<void> {
    // 端侧模型初始化（缺失自动降级，不影响云端识别主链路）
    try {
      await LocalDetector.getInstance().init(getContext(this) as common.UIAbilityContext);
      if (LocalDetector.getInstance().isAvailable()) {
        this.frameState = '端侧检测已就绪';
      }
    } catch (e) {
      // 忽略：端侧检测不可用，走整帧上传
    }
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

      if (profiles.length >= 2) {
        // 主方案：双 preview —— 一路到 XComponent 显示，一路到 ImageReceiver 抽帧检测。
        // 创建不依赖当前模型是否已加载，规避 onLoad 与 aboutToAppear 异步 init 之间的竞态。
        this.singleMode = false;
        const displayProfile = profiles[0];
        this.previewOutput = this.cameraManager.createPreviewOutput(displayProfile, this.surfaceId);
        const frameProfile = profiles[1];
        this.frameReceiver = image.createImageReceiver(frameProfile.size.width, frameProfile.size.height, 4, 8);
        const recvSurfaceId = await this.frameReceiver.getReceivingSurfaceId();
        this.framePreview = this.cameraManager.createPreviewOutput(frameProfile, recvSurfaceId);
        this.frameReceiver.on('imageArrival', () => this.onFrameArrival());
        this.frameState = '端侧检测通道已就绪（双通道）';
        hilog.info(DOMAIN, TAG, 'dual preview ready, frame=%{public}dx%{public}d',
          frameProfile.size.width, frameProfile.size.height);
      } else {
        // 兜底：仅一个预览档位 → 单 ImageReceiver 作唯一 preview，同一帧既显示又检测画框
        this.singleMode = true;
        const frameProfile = profiles[0];
        this.frameReceiver = image.createImageReceiver(frameProfile.size.width, frameProfile.size.height, 4, 8);
        const recvSurfaceId = await this.frameReceiver.getReceivingSurfaceId();
        this.framePreview = this.cameraManager.createPreviewOutput(frameProfile, recvSurfaceId);
        this.frameReceiver.on('imageArrival', () => this.onSingleFrameArrival());
        this.frameState = '端侧检测通道已就绪（单接收器）';
        hilog.warn(DOMAIN, TAG, 'previewProfiles=%{public}d, single imageReceiver fallback', profiles.length);
      }

      // 拍照输出
      const photoProfile = caps.photoProfiles[0];
      this.photoOutput = this.cameraManager.createPhotoOutput(photoProfile);
      this.photoOutput.on('photoAvailable', (err: BusinessError, photo: camera.Photo) => {
        this.onPhotoAvailable(err, photo);
      });

      this.session = this.cameraManager.createSession(camera.SceneMode.NORMAL_PHOTO) as camera.PhotoSession;
      this.session.beginConfig();
      this.session.addInput(this.cameraInput);
      if (this.singleMode) {
        // 单接收器模式：唯一 preview 输出到 ImageReceiver（不另加 XComponent 显示输出）
        this.session.addOutput(this.framePreview!);
      } else {
        // 双通道模式：XComponent 显示 + ImageReceiver 抽帧
        this.session.addOutput(this.previewOutput!);
        if (this.framePreview) {
          this.session.addOutput(this.framePreview);
        }
      }
      this.session.addOutput(this.photoOutput);
      await this.session.commitConfig();
      await this.session.start();
      this.isCameraReady = true;
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

  // ImageReceiver 抽帧 → 端侧检测 → 更新 @State detections（声明式渲染框层）。
  // 模型未就绪时跳过本帧；每帧只处理一次（isFrameBusy 节流）。
  private onFrameArrival(): void {
    if (this.isFrameBusy || this.frameReceiver === null || this.showFrozen) {
      return;   // 上一帧未处理完 / 已定格时不继续抽帧
    }
    if (!LocalDetector.getInstance().isAvailable()) {
      return;   // 模型尚未加载完成，下一帧再试（避免竞态丢通道）
    }
    this.isFrameBusy = true;
    this.frameReceiver.readLatestImage((err: BusinessError, img: image.Image) => {
      if (err || img === undefined) {
        this.isFrameBusy = false;
        return;
      }
      img.getComponent(image.ComponentType.JPEG, (e: BusinessError, component: image.Component) => {
        try {
          if (!e && component && component.byteBuffer) {
            const src = image.createImageSource(component.byteBuffer as ArrayBuffer);
            src.createPixelMap().then(async (pm: image.PixelMap) => {
              try {
                const res = await LocalDetector.getInstance().detect(pm);
                pm.release();
                if (res.available) {
                  // 更新 detections 会触发声明式 UI 重绘检测框层
                  this.detections = res.detections;
                  this.frameState = res.detections.length > 0 ? '已框选目标' : '检测中，未命中目标';
                  hilog.info(DOMAIN, TAG, 'dual detect: boxes=%{public}d', res.detections.length);
                }
              } catch (e2) {
                // 忽略单帧异常
              }
              src.release();
            }).catch(() => {
              src.release();
            });
          }
        } catch (e2) {
          // 忽略
        } finally {
          img.release();
          this.isFrameBusy = false;
        }
      });
    });
  }

  // 兜底单接收器模式：同一帧先用于显示预览（singlePreview），再做端侧检测更新画框。
  // 仅在设备提供单个预览档位时启用（见 openCamera）。
  private onSingleFrameArrival(): void {
    if (this.isFrameBusy || this.frameReceiver === null || this.showFrozen) {
      return;
    }
    this.isFrameBusy = true;
    this.frameReceiver.readLatestImage((err: BusinessError, img: image.Image) => {
      if (err || img === undefined) {
        this.isFrameBusy = false;
        return;
      }
      img.getComponent(image.ComponentType.JPEG, (e: BusinessError, component: image.Component) => {
        try {
          if (!e && component && component.byteBuffer) {
            const src = image.createImageSource(component.byteBuffer as ArrayBuffer);
            src.createPixelMap().then(async (pm: image.PixelMap) => {
              try {
                // 更新显示帧；释放上一帧避免句柄堆积
                const prev = this.singlePreview;
                this.singlePreview = pm;
                if (prev !== null && prev !== pm) {
                  prev.release();
                }
                // 模型就绪时同步检测画框（detect 内部用副本，不污染显示帧）
                if (LocalDetector.getInstance().isAvailable()) {
                  const res = await LocalDetector.getInstance().detect(pm);
                  if (res.available) {
                    this.detections = res.detections;
                    this.frameState = res.detections.length > 0 ? '已框选目标' : '检测中，未命中目标';
                    hilog.info(DOMAIN, TAG, 'single detect: boxes=%{public}d', res.detections.length);
                  }
                }
              } catch (e2) {
                // 忽略单帧异常
              }
              src.release();
            }).catch(() => {
              src.release();
            });
          }
        } catch (e2) {
          // 忽略
        } finally {
          img.release();
          this.isFrameBusy = false;
        }
      });
    });
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
    if (this.singlePreview) {
      this.singlePreview.release();
      this.singlePreview = null;
    }
    this.singleMode = false;
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

  // 选择结果回填
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
        } else if (this.singleMode) {
          // 兜底模式：用 ImageReceiver 帧直接显示预览
          if (this.singlePreview) {
            Image(this.singlePreview)
              .width('100%').height('100%')
              .objectFit(ImageFit.Cover)
          }
          // 实时 YOLO 标注框覆盖层（声明式响应式）
          ForEach(this.detections, (d: Detection, idx: number) => {
            Rect()
              .width(`${(d.width * 100).toFixed(2)}%`)
              .height(`${(d.height * 100).toFixed(2)}%`)
              .position({ x: `${(d.left * 100).toFixed(2)}%`, y: `${(d.top * 100).toFixed(2)}%` })
              .fill(Color.Transparent)
              .stroke('#FF4D4F')
              .strokeWidth(3)
          }, (d: Detection, idx: number) => `${idx}_${d.left.toFixed(3)}_${d.top.toFixed(3)}`)
        } else {
          XComponent({ id: 'cameraPreview', type: XComponentType.SURFACE, controller: this.xComponentController })
            .width('100%').height('100%')
            .onLoad(() => {
              this.surfaceId = this.xComponentController.getXComponentSurfaceId();
              this.openCamera();
            })
          // 实时 YOLO 标注框覆盖层（双通道模式）
          ForEach(this.detections, (d: Detection, idx: number) => {
            Rect()
              .width(`${(d.width * 100).toFixed(2)}%`)
              .height(`${(d.height * 100).toFixed(2)}%`)
              .position({ x: `${(d.left * 100).toFixed(2)}%`, y: `${(d.top * 100).toFixed(2)}%` })
              .fill(Color.Transparent)
              .stroke('#FF4D4F')
              .strokeWidth(3)
          }, (d: Detection, idx: number) => `${idx}_${d.left.toFixed(3)}_${d.top.toFixed(3)}`)
        }
      }
      .width('100%')
      .layoutWeight(1)
      .backgroundColor('#000000')

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