<template>
  <v-container class="fill-height" fluid>
    <v-row align="center" justify="center">
      <v-col cols="12" sm="8" md="6">
        <v-card class="elevation-12">
          <v-toolbar color="primary" dark flat>
            <v-toolbar-title>Shared File</v-toolbar-title>
          </v-toolbar>

          <v-card-text v-if="loading" class="text-center">
            <v-progress-circular indeterminate/>
            <p class="mt-4">Validating share link...</p>
          </v-card-text>

          <v-card-text v-else-if="!isValidLink" class="text-center">
            <v-icon size="64" color="error">mdi-alert-circle</v-icon>
            <h3 class="mt-4">Invalid or Expired Link</h3>
            <p>This share link is no longer valid.</p>
          </v-card-text>

          <v-card-text v-else-if="showPasswordInput" class="text-center">
            <v-icon size="64" color="warning">mdi-lock</v-icon>
            <h3 class="mt-4">Password Required</h3>

            <v-form @submit.prevent="submitPassword" class="mt-4">
              <v-text-field
                  v-model="password"
                  label="Enter password"
                  type="password"
                  prepend-icon="mdi-lock"
                  :error-messages="passwordError"
              />

              <v-btn
                  color="primary"
                  :loading="validating"
                  @click="submitPassword"
                  block
              >
                Submit
              </v-btn>
            </v-form>
          </v-card-text>

          <v-card-text v-else-if="validation && isValidLink" class="text-center">
            <v-icon size="64" color="success">mdi-file</v-icon>
            <h3 class="mt-4">{{ validation.filename }}</h3>
            <p class="text-subtitle-1">{{ formatFileSize(validation.fileSize || 0) }}</p>

            <div v-if="validation.expiry" class="mt-4">
              <v-chip color="warning" size="small">
                Expires: {{ new Date(validation.expiry).toLocaleString() }}
              </v-chip>
            </div>

            <v-btn
                color="primary"
                size="large"
                class="mt-6"
                prepend-icon="mdi-download"
                @click="downloadFile"
            >
              Download File
            </v-btn>
          </v-card-text>
        </v-card>
      </v-col>
    </v-row>
  </v-container>
</template>

<script setup lang="ts">
import {definePageMeta, useRoute, onMounted} from '#imports'
import {ref, computed} from 'vue'
import type {ApiErrorResponse, SharedFileValidation} from '~/types'
import {useSharingStore} from '~/stores/sharing'
import {useFileUtils} from '~/composables/useFileUtils'
import {useRuntimeConfig} from '#app'
import {useNotification} from '~/composables/useNotification'

const notification = useNotification()

definePageMeta({
  layout: false
})

const route = useRoute()
const sharingStore = useSharingStore()
const {formatFileSize} = useFileUtils()

const isValidLink = ref(false)

// const linkId = computed(() => route.params.linkId as string)
// For now, get link from query param, cuz we're actually gonna make it static generated.
// (it can't statically generate it with /[linkId], because it doesn't know the page name at compile time)
// so we're gonna take the link ID from the query parameter instead.
const linkId = computed(() => route.query.linkId as string)

const checkLinkPresence = () => {
  if (linkId.value)
    return;
  notification.error('No share link provided.')
  isValidLink.value = false
}

const validation = ref<SharedFileValidation | null>(null)
const loading = ref(true)
const validating = ref(false)
const password = ref('')
const passwordError = ref('')
const showPasswordInput = ref(false)

const hasTriedPassword = ref(false)

const validateFile = async () => {
  loading.value = true
  passwordError.value = ''

  try {
    const result = await sharingStore.validateSharedFile(linkId.value, password.value)
    validation.value = result

    if (result.hasPassword && !result.validPassword) {
      isValidLink.value = true
      showPasswordInput.value = true

      if (hasTriedPassword.value)
        passwordError.value = 'Invalid password. Please try again.'
    } else if (result.hasPassword && result.validPassword) {
      isValidLink.value = true;
      showPasswordInput.value = false;
    }

    if (result.hasExpired) {
      isValidLink.value = false;
      notification.error('This share link has expired.');
    } else {
      isValidLink.value = true;
    }

    hasTriedPassword.value = true

  } catch (error: any) {

    //@ts-ignore
    if (error && 'errorType' in error) {
      const apiError = error as ApiErrorResponse
      switch (apiError.errorType) {
        case 'LINK_NOT_FOUND':

          isValidLink.value = false;

          break
        default:
          notification.error(`An error occurred: ${apiError.errorType || 'Unknown error'}: ${apiError.errorMessage || 'Please try again.'}`)
      }
    } else {
      notification.error('An unexpected error occurred. Please try again.')
    }

  } finally {
    loading.value = false
    validating.value = false
  }
}

const submitPassword = async () => {
  if (!password.value) {
    passwordError.value = 'Password is required'
    return
  }

  validating.value = true
  await validateFile()
}

const downloadFile = () => {
  const config = useRuntimeConfig()
  const link = document.createElement('a')

  let apiBase = config.public.apiBase

  // if the apiBase doesn't include full URL, prepend it (in embedded mode, it might be just a path)
  // noinspection HttpUrlsUsage
  if (!apiBase.startsWith('http://') && !apiBase.startsWith('https://')) {
    const baseUrl = window.location.origin
    apiBase = new URL(apiBase, baseUrl).toString()
  }

  const url = new URL(`/api/sharing/${linkId.value}`, apiBase)
  if (password.value) {
    url.searchParams.append('password', password.value)
  }

  link.href = url.toString()
  link.click()
}


onMounted(() => {
  checkLinkPresence()
  validateFile()
})
</script>
