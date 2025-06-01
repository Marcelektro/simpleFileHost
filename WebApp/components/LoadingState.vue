<template>
  <div class="loading-container">
    <v-progress-circular
        :size="size"
        :width="width"
        color="primary"
        indeterminate
    />
    <p v-if="message" class="mt-4 text-center">{{ message }}</p>
  </div>
</template>

<script setup lang="ts">
interface Props {
  size?: number
  width?: number
  message?: string
}

withDefaults(defineProps<Props>(), {
  size: 40,
  width: 4,
  message: 'Loading...'
})
</script>

<style scoped>
.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 2rem;
}
</style>
