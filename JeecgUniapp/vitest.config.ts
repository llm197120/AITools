import { defineConfig } from 'vitest/config'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url)),
    },
  },
  define: {
    __UNI_PLATFORM__: '"h5"',
  },
  test: {
    include: ['src/pages-homeai/{utils,platform,offline}/**/*.test.ts'],
    environment: 'node',
  },
})
