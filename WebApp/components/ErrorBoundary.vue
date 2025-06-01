<template>
  <div v-if="hasError" class="error-container">
    <v-alert
        type="error"
        variant="tonal"
        class="mb-4"
    >
      <v-alert-title>Something went wrong</v-alert-title>
      <div>{{ errorMessage }}</div>
    </v-alert>

    <v-btn
        color="primary"
        @click="retry"
    >
      Try Again
    </v-btn>
  </div>

  <slot v-else/>
</template>

<script setup lang="ts">
import {ref, onErrorCaptured} from 'vue'

const hasError = ref(false)
const errorMessage = ref('')

const emit = defineEmits<{
  retry: []
}>()

onErrorCaptured((error) => {
  hasError.value = true
  errorMessage.value = error.message || 'An unexpected error occurred'
  return false
})

const retry = () => {
  hasError.value = false
  errorMessage.value = ''
  emit('retry')
}
</script>

<style scoped>
.error-container {
  padding: 2rem;
  text-align: center;
}
</style>
