import vue from '@vitejs/plugin-vue'
import { defineConfig } from 'vite'

// Vite 横幅固定打印服务根地址，这里在启动后补一行可直接点击的登录页地址
function loginUrlHint() {
  return {
    name: 'login-url-hint',
    configureServer(server) {
      server.httpServer?.once('listening', () => {
        setTimeout(() => {
          const local = server.resolvedUrls?.local[0] ?? 'http://localhost:5173/'
          server.config.logger.info(`  ➜  登录页:  ${local}login`)
        }, 30)
      })
    },
  }
}

export default defineConfig({
  plugins: [vue(), loginUrlHint()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})