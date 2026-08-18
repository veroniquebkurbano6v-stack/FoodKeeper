// 图像预处理 —— Image Kit 封装
// 职责：把相机帧 PixelMap 处理成「最长边封顶 640、不放大」的 JPEG，
//       再编码为去 data: 头的 base64，供云端识别上传。
// 注意：V5 Image Kit 中 PixelMap.scale(x, y) 为「原地缩放」，x/y 为缩放因子，返回 Promise<void>。

import image from '@ohos.multimedia.image';
import util from '@ohos.util';
import { MAX_EDGE_PX, JPEG_QUALITY } from '../model/CloudConfig';

export class ImageCodec {
  // PixelMap → 去 data: 头的 base64 字符串（原地缩放后打包为 JPEG）
  static async pixelMapToBase64(src: image.PixelMap): Promise<string> {
    const info = await src.getImageInfo();
    const maxEdge = Math.max(info.size.width, info.size.height);
    if (maxEdge > MAX_EDGE_PX) {
      const ratio = MAX_EDGE_PX / maxEdge;   // 缩放因子，仅缩小不放大
      await src.scale(ratio, ratio);
    }
    const packer = image.createImagePacker();
    const packOpts: image.PackingOption = {
      format: 'image/jpeg',
      quality: JPEG_QUALITY
    };
    const data: ArrayBuffer = await packer.packing(src, packOpts);
    packer.release();
    const bytes = new Uint8Array(data);
    const base64 = new util.Base64Helper();
    return base64.encodeToStringSync(bytes);
  }
}