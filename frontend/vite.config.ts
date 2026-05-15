import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 预留新的前后端分离 API 前缀，后续比赛库接口会放在 /api 下。
      '/api': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      // 兼容当前后端已有接口，先保证旧 static 页面能力完整迁移。
      '/match': {
        target: 'http://127.0.0.1:8080',
        changeOrigin: true
      },
      '/ws': {
        target: 'ws://127.0.0.1:8080',
        ws: true,
        changeOrigin: true
      }
    }
  }
})
