import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  // sockjs-client references `global`, which doesn't exist in the browser/Vite.
  define: {
    global: 'globalThis',
  },
  server: {
    port: 5173,
    open: true
  }
})
