import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/admin/',
  server: {
    host: true,
    port: 5173,
    allowedHosts: ['ran-crm-admin', 'ran-crm-backend', 'localhost', '127.0.0.1'],
    proxy: {
      '/auth': 'http://ran-crm-app:3000',
      '/users': 'http://ran-crm-app:3000',
      '/contacts': 'http://ran-crm-app:3000',
      '/calls': 'http://ran-crm-app:3000',
      '/sync-audit': 'http://ran-crm-app:3000',
    }
  },
  build: {
    outDir: '../public/admin',
    emptyOutDir: true
  }
})
