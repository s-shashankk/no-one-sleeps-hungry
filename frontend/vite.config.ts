import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import os from 'node:os'
import path from 'node:path'

// In development, /api calls are forwarded to the Spring Boot backend, so the browser sees one origin
// (the same setup is used in production behind a reverse proxy).
export default defineConfig({
  plugins: [react()],
  // Keep Vite's dependency cache outside the project: OneDrive locks files inside it and causes EPERM errors.
  cacheDir: path.join(os.tmpdir(), 'mealup-vite-cache'),
  server: {
    port: 5174,
    strictPort: true,
    proxy: { '/api': 'http://localhost:8080' },
  },
})
