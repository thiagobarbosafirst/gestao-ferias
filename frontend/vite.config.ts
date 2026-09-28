import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Em desenvolvimento (npm run dev) os pedidos a /api são reencaminhados para o backend Spring Boot.
// Assim o browser fala sempre com a mesma origem e não é preciso configurar CORS.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
});
