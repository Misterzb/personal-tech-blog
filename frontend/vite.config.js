import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
      '/uploads': { target: 'http://localhost:8080', changeOrigin: true },
      '/rss.xml': { target: 'http://localhost:8080', changeOrigin: true },
      '/sitemap.xml': { target: 'http://localhost:8080', changeOrigin: true },
      '/robots.txt': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
})
