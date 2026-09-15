import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],

  server: {
    proxy: {
      '/dashboard': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      '/demands': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },

      '/import': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})