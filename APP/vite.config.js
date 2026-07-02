import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// AI 服务商代理目标（开发环境通过 /ai-proxy/* 转发，规避浏览器 CORS 限制）
const AI_PROXY_TARGETS = {
  openai: 'https://api.openai.com',
  deepseek: 'https://api.deepseek.com',
  zhipu: 'https://open.bigmodel.cn',
  moonshot: 'https://api.moonshot.cn'
}

// 为每个服务商生成代理规则：/ai-proxy/<provider>/* -> 目标站点/*
function buildAiProxyRules() {
  const rules = {}
  for (const [provider, target] of Object.entries(AI_PROXY_TARGETS)) {
    rules[`^/ai-proxy/${provider}/(.*)$`] = {
      target,
      changeOrigin: true,
      rewrite: (path) => path.replace(new RegExp(`^/ai-proxy/${provider}`), '')
    }
  }
  return rules
}

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    host: '127.0.0.1',
    port: 5173,
    strictPort: false,
    proxy: buildAiProxyRules()
  }
})
