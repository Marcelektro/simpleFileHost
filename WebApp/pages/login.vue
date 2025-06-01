<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="4">
        <v-card class="elevation-8">
          <v-toolbar color="primary" dark flat>
            <v-toolbar-title>
              <v-icon left>mdi-cloud</v-icon>
              simpleFileHost - Login
            </v-toolbar-title>
          </v-toolbar>

          <v-card-text>
            <v-form @submit.prevent="login">
              <v-text-field
                  v-model="form.username"
                  label="Username"
                  prepend-icon="mdi-account"
                  :rules="[rules.required]"
                  required
              />

              <v-text-field
                  v-model="form.password"
                  label="Password"
                  :type="showPassword ? 'text' : 'password'"
                  prepend-icon="mdi-lock"
                  :append-icon="showPassword ? 'mdi-eye' : 'mdi-eye-off'"
                  :rules="[rules.required]"
                  @click:append="showPassword = !showPassword"
                  required
              />

              <v-btn
                  color="primary"
                  block
                  type="submit"
                  :disabled="loading"
                  :loading="loading"
                  @click="login"
              >
                Login
              </v-btn>
            </v-form>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import {definePageMeta, ref, watchEffect, navigateTo} from '#imports'
import {useAuthStore} from '~/stores/auth'
import {useNotification} from '~/composables/useNotification'
import type {ApiErrorResponse} from '~/types/'

definePageMeta({
  layout: false
})

const authStore = useAuthStore()
const notification = useNotification()

const form = ref({
  username: '',
  password: ''
})

const loading = ref(false)
const showPassword = ref(false)

const rules = {
  required: (value: string) => !!value || 'This field is required',
}

const login = async () => {
  if (!form.value.username || !form.value.password) {
    notification.error('Please fill in all fields')
    return
  }

  loading.value = true
  try {
    await authStore.login(form.value)
  } catch (error) {
    //@ts-ignore
    if (error && 'errorType' in error) {
      const apiError = error as ApiErrorResponse
      switch (apiError.errorType) {
        case 'UNAUTHORIZED':
          notification.error('Invalid username or password. Please try again.')

          break
        default:
          notification.error(`An error occurred: ${apiError.errorType || 'Unknown error'}: ${apiError.errorMessage || 'Please try again.'}`)
      }
    } else {
      notification.error('An unexpected error occurred. Please try again.')
    }

    console.error('Login failed:', error)
  } finally {
    loading.value = false
  }
}

watchEffect(() => {
  if (authStore.isAuthenticated) {
    navigateTo('/')
  }
})
</script>
