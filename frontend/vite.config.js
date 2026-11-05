import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

const isCloudflarePages = process.env.CLOUDFLARE_PAGES === '1'

// dev: фронт на :5173, запросы /api уходят на Spring (:8080) через прокси
// build: готовый сайт кладётся прямо в static-папку Spring Boot
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': 'http://localhost:8080' },
  },
  build: {
    outDir: isCloudflarePages ? 'dist' : '../src/main/resources/static',
    emptyOutDir: true,
  },
})
