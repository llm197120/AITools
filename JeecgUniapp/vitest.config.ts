import { defineConfig } from 'vitest/config'

export default defineConfig({
  test: {
    include: ['src/pages-homeai/{utils,platform}/**/*.test.ts'],
    environment: 'node',
  },
})
