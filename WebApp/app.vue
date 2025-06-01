<template>
  <v-app>
    <v-snackbar
        v-model="notification.snackbar.value.show"
        :color="notification.snackbar.value.color"
        :timeout="notification.snackbar.value.timeout"
        location="top"
    >
      {{ notification.snackbar.value.message }}

      <template v-slot:actions>
        <v-btn
            color="white"
            variant="text"
            @click="notification.hide()"
        >
          Close
        </v-btn>
      </template>
    </v-snackbar>

    <ErrorBoundary @retry="handleRetry">
      <NuxtPage/>
    </ErrorBoundary>
  </v-app>
</template>

<script setup lang="ts">
import {useNotification} from '~/composables/useNotification'
import {useAuthStore} from '~/stores/auth'
import {onMounted} from 'vue'
import ErrorBoundary from '~/components/ErrorBoundary.vue'

const notification = useNotification()
const authStore = useAuthStore()

const handleRetry = () => {
  window.location.reload()
}

onMounted(() => {
  authStore.initializeAuth()
})
</script>
