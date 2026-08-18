// 云端识别服务 —— 百度 AI 开放平台
// 职责：1) 用 API Key/Secret 换取并缓存 Access Token；
//       2) 果蔬识别（classify/ingredient）优先，低置信度/失败时降级通用物体识别（advanced_general）；
//       3) 统一返回 {name, confidence, sourceApi}，供 App 回填表单。
//
// 降级链（方案 B）：端侧不判类别，全量先走果蔬识别，再用通用识别兜底。

import http from '@ohos.net.http';
import {
  BAIDU_API_KEY, BAIDU_SECRET_KEY, BAIDU_TOKEN_URL, BAIDU_INGREDIENT_URL,
  BAIDU_GENERAL_URL, INGREDIENT_CONF_THRESHOLD, NON_VEGETABLE_KEYWORDS, HTTP_TIMEOUT_MS
} from '../model/CloudConfig';

// 单条识别结果
export interface RecognizedFood {
  name: string;
  confidence: number;
  sourceApi: string; // 'ingredient' | 'general'
}

// 识别返回（candidates 按置信度降序）
export interface RecognizeResult {
  ok: boolean;
  candidates: RecognizedFood[];
  message: string;
}

export class BaiduRecognizer {
  private static instance: BaiduRecognizer | null = null;
  private accessToken: string = '';
  private tokenExpireAt: number = 0;

  static getInstance(): BaiduRecognizer {
    if (BaiduRecognizer.instance === null) {
      BaiduRecognizer.instance = new BaiduRecognizer();
    }
    return BaiduRecognizer.instance;
  }

  // 获取 Access Token（缓存 + 提前 5 分钟过期）
  async ensureAccessToken(): Promise<string> {
    const now = Date.now();
    if (this.accessToken.length > 0 && now < this.tokenExpireAt) {
      return this.accessToken;
    }
    const url = `${BAIDU_TOKEN_URL}?grant_type=client_credentials&client_id=${BAIDU_API_KEY}&client_secret=${BAIDU_SECRET_KEY}`;
    const resp = await this.httpGet(url);
    if (resp.length === 0) {
      throw new Error('获取 Access Token 失败：空响应');
    }
    const obj = JSON.parse(resp) as Record<string, Object>;
    const token = String(obj['access_token'] ?? '');
    if (token.length === 0) {
      throw new Error('获取 Access Token 失败：缺少 access_token');
    }
    const expiresIn = Number(obj['expires_in'] ?? 0);
    this.accessToken = token;
    this.tokenExpireAt = now + (expiresIn - 300) * 1000;
    return token;
  }

  // 果蔬识别（主用）
  private async recognizeIngredient(imageBase64: string): Promise<RecognizedFood[]> {
    const token = await this.ensureAccessToken();
    const url = `${BAIDU_INGREDIENT_URL}?access_token=${token}`;
    const body = `image=${encodeURIComponent(imageBase64)}&top_num=5`;
    const resp = await this.httpPost(url, body);
    if (resp.length === 0) return [];
    const obj = JSON.parse(resp) as Record<string, Object>;
    const result = obj['result'] as Record<string, Object>[];
    if (!Array.isArray(result)) return [];
    const list: RecognizedFood[] = [];
    for (let i = 0; i < result.length; i++) {
      list.push({
        name: String(result[i]['name'] ?? ''),
        confidence: Number(result[i]['score'] ?? 0),
        sourceApi: 'ingredient',
      });
    }
    list.sort((a, b) => b.confidence - a.confidence);
    return list;
  }

  // 通用物体和场景识别（兜底）
  private async recognizeGeneral(imageBase64: string): Promise<RecognizedFood[]> {
    const token = await this.ensureAccessToken();
    const url = `${BAIDU_GENERAL_URL}?access_token=${token}`;
    const body = `image=${encodeURIComponent(imageBase64)}`;
    const resp = await this.httpPost(url, body);
    if (resp.length === 0) return [];
    const obj = JSON.parse(resp) as Record<string, Object>;
    const result = obj['result'] as Record<string, Object>[];
    if (!Array.isArray(result)) return [];
    const list: RecognizedFood[] = [];
    for (let i = 0; i < result.length; i++) {
      list.push({
        name: String(result[i]['keyword'] ?? ''),
        confidence: Number(result[i]['score'] ?? 0),
        sourceApi: 'general',
      });
    }
    list.sort((a, b) => b.confidence - a.confidence);
    return list;
  }

  // 判断某条结果是否命中非果蔬兜底类（忽略大小写、模糊包含匹配）
  private isNonVegetable(food: RecognizedFood): boolean {
    const name = food.name.toLowerCase();
    for (let i = 0; i < NON_VEGETABLE_KEYWORDS.length; i++) {
      if (name.includes(NON_VEGETABLE_KEYWORDS[i].toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  // 果蔬识别结果是否可信：
  //   出现任一非果蔬兜底类（即使置信度极高）→ 不可信，须降级通用识别
  private ingredientResultTrusted(ingredient: RecognizedFood[]): boolean {
    if (ingredient.length === 0) {
      return false;
    }
    const top = ingredient[0];
    if (this.isNonVegetable(top)) {
      return false;
    }
    return top.confidence >= INGREDIENT_CONF_THRESHOLD;
  }

  // 统一识别入口：果蔬优先 → 命中华而不实度低/非果蔬兜底类时降级通用识别
  async recognize(imageBase64: string): Promise<RecognizeResult> {
    try {
      const ingredient = await this.recognizeIngredient(imageBase64);
      if (this.ingredientResultTrusted(ingredient)) {
        return { ok: true, candidates: ingredient, message: '果蔬识别命中' };
      }
      // 置信度低 / 最高项为非果蔬兜底类 或调用失败 → 降级通用识别
      const general = await this.recognizeGeneral(imageBase64);
      if (general.length > 0) {
        return { ok: true, candidates: general, message: '已降级通用识别' };
      }
      return { ok: false, candidates: [], message: '未识别到有效结果' };
    } catch (e) {
      return { ok: false, candidates: [], message: '云端识别异常' };
    }
  }

  private async httpGet(url: string): Promise<string> {
    const req = http.createHttp();
    try {
      const resp = await req.request(url, {
        method: http.RequestMethod.GET,
        connectTimeout: HTTP_TIMEOUT_MS,
        readTimeout: HTTP_TIMEOUT_MS,
      });
      return typeof resp.result === 'string' ? resp.result : '';
    } finally {
      req.destroy();
    }
  }

  private async httpPost(url: string, body: string): Promise<string> {
    const req = http.createHttp();
    try {
      const resp = await req.request(url, {
        method: http.RequestMethod.POST,
        header: { 'Content-Type': 'application/x-www-form-urlencoded' },
        extraData: body,
        connectTimeout: HTTP_TIMEOUT_MS,
        readTimeout: HTTP_TIMEOUT_MS,
      });
      return typeof resp.result === 'string' ? resp.result : '';
    } finally {
      req.destroy();
    }
  }
}