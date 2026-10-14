import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// In dev, /api calls are forwarded to the Spring Boot backend.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5174,
    strictPort: true,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
