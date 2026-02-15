import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  base: '/admin/',
  server: {
    host: true,
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:3000',
      '/users': 'http://localhost:3000',
      '/contacts': 'http://localhost:3000',
      '/calls': 'http://localhost:3000',
      '/sync-audit': 'http://localhost:3000',
    }
  },
  build: {
    outDir: '../public/admin', // Builds directly into backend's public folder
    emptyOutDir: true
  }
})
