// vitest/config: the same defineConfig, plus the typed "test" section
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// In development the API runs on :8042. The proxy makes it same-origin for the browser,
// so no CORS is involved while developing.
const backend = 'http://localhost:8042';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': backend,
      '/uploads': backend,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
    restoreMocks: true,
  },
});
