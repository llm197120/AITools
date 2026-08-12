<template>
  <div ref="swaggerUiRef" class="swagger-ui-container" style="height: 100%"></div>
</template>

<script lang="ts" setup>
  import { ref, onMounted } from 'vue';
  import { getOpenApiJson } from './OpenApi.api';

  const swaggerUiRef = ref<HTMLElement | null>(null);

  onMounted(async () => {
    try {
      //update-begin---author:copilot ---date:2026-08-11 for：【P2】Swagger UI 按需动态加载，避免打入首屏包-----------
      const [{ default: SwaggerUI }] = await Promise.all([
        import('swagger-ui-dist/swagger-ui-bundle'),
        import('swagger-ui-dist/swagger-ui.css'),
      ]);
      //update-end---author:copilot ---date:2026-08-11 for：【P2】Swagger UI 按需动态加载，避免打入首屏包-----------
      const openApiJson = await getOpenApiJson();
      if (swaggerUiRef.value) {
        SwaggerUI({
          domNode: swaggerUiRef.value,
          spec: openApiJson,
        });
      }
    } catch (error) {
      console.error('Failed to fetch OpenAPI JSON:', error);
    }
  });
</script>

<style scoped>
  .swagger-ui-container {
    height: 100%;
  }
</style>
