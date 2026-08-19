// 端云协同——云端识别与端侧检测配置
//
// ── 百度 AI 开放平台凭证配置说明 ───────────────────────────
// 1. 本文件顶部常量 BAIDU_API_KEY / BAIDU_SECRET_KEY 用于换取 Access Token，
//    调用「果蔬识别」与「通用物体识别」接口（见 3 处接口常量）。
// 2. 密钥属敏感信息，默认不在此提交真实值：把申请到的凭证填入 local.properties 中的
//    `baidu.api-key` / `baidu.secret-key` 字段（local.properties 已被 .gitignore 排除，
//    不会提交到仓库），再复制到下方常量；或保持不变，运行时通过服务端代理换取 Token。
// 3. 申请地址：百度智能云 → 视觉技术 → 图像识别 → 果蔬识别/通用物体和场景识别，
//    创建应用后取得 API Key 与 Secret Key（详见 local.properties.example「百度 AI」小节）。
// 4. 安全提醒：请勿把真实密钥提交到公开仓库，以免造成额度盗刷。
// ───────────────────────────────────────────────────────────

// 换成你自己的凭证后即可启用云端识别（留空则云端识别返回「未识别到有效结果」）。
// 本工程真实凭证存于 local.properties（被 gitignore，不进仓库）；提交公开 PR 必须保持此处置空。
export const BAIDU_API_KEY: string = '';
export const BAIDU_SECRET_KEY: string = '';

// 百度 AI 开放平台接口
export const BAIDU_TOKEN_URL: string = 'https://aip.baidubce.com/oauth/2.0/token';
export const BAIDU_INGREDIENT_URL: string = 'https://aip.baidubce.com/rest/2.0/image-classify/v1/classify/ingredient';
export const BAIDU_GENERAL_URL: string = 'https://aip.baidubce.com/rest/2.0/image-classify/v2/advanced_general';

// 端侧检测置信度阈值（低于则丢弃该帧，不上传不标注）
// 采用 YOLO 惯例 0.25：COCO 预训练模型对食材类多为弱检出，0.7 会让食材几乎永远不出框。
export const DETECT_CONF_THRESHOLD: number = 0.25;
// 云端果蔬识别置信度阈值（低于则降级到通用物体识别）
export const INGREDIENT_CONF_THRESHOLD: number = 0.5;

// 非果蔬兜底类关键词：百度果蔬识别把「无法归入果蔬的物体」命中和兜底类，
// 即便置信度极高也非食材，命中即降级到通用物体识别。
export const NON_VEGETABLE_KEYWORDS: string[] = [
  '非果蔬', '非蔬菜', '非食材', '其它', '其他', '未知', '未识别', '背景', '盘子', '碗', '桌子', '手',
  'non', 'other', 'background'
];

// 图像预处理：最长边封顶（不放大，保住小目标细节）
export const MAX_EDGE_PX: number = 640;
export const JPEG_QUALITY: number = 85;

// 端侧检测模型文件名（rawfile 内）
export const LOCAL_MODEL_NAME: string = 'yolov8n.ms';
// 网络请求超时（毫秒）
export const HTTP_TIMEOUT_MS: number = 10000;