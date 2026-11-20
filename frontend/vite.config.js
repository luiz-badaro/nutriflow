import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// As chamadas a /api sao repassadas ao backend Spring Boot (porta definida em application.properties).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5180,
    strictPort: true,
    proxy: { '/api': 'http://localhost:8765' },
  },
})
