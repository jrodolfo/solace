import { defineConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'

/**
 * Vite configuration for the publisher UI.
 *
 * The test block enables React Testing Library under jsdom, while the build
 * block keeps third-party dependencies in a stable vendor chunk.
 */
export default defineConfig({
  plugins: [react()],
  test: {
    globals: true,
    environment: "jsdom",
    setupFiles: "./src/setupTests.js",
    css: true,
  },
  build: {
    target: 'es2015',
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            return 'vendor';
          }
        }
      }
    }
  }
})
