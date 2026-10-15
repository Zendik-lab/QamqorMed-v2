import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

function createApiProxy(target) {
  return {
    '/api': {
      target,
      changeOrigin: true,
      secure: false,
      configure(proxy) {
        proxy.on('proxyReq', (proxyRequest) => {
          // The browser and Spring are same-origin from the browser's point
          // of view. Do not forward the phone's LAN Origin to Spring CORS;
          // direct cross-origin calls still remain protected by CorsConfig.
          proxyRequest.removeHeader('origin')
        })
      },
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backendTarget = (env.VITE_DEV_API_TARGET || 'http://127.0.0.1:8080').replace(/\/+$/, '')
  const apiProxy = createApiProxy(backendTarget)

  return {
    plugins: [react()],
    // Reachable on the local network for phone testing. API calls remain
    // same-origin in the browser and are forwarded to Spring Boot.
    server: {
      host: '0.0.0.0',
      proxy: apiProxy,
    },
    // Keep `vite preview` usable from a phone too. Production deployment
    // should provide /api at the web origin instead of relying on this proxy.
    preview: {
      host: '0.0.0.0',
      proxy: apiProxy,
    },
  }
})
