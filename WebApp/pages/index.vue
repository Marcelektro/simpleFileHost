<template>
  <div>
    <v-app-bar color="primary" dark>
      <v-toolbar-title>
        <v-icon left>mdi-cloud</v-icon>
        simpleFileHost
      </v-toolbar-title>
      <v-spacer/>

      <FileUpload @upload="onFileUpload"/>

      <v-menu>
        <template #activator="{ props }">
          <v-btn
              v-bind="props"
              icon="mdi-account-circle"
              class="ml-4"
          />
        </template>

        <v-list>
          <v-list-item>
            <v-list-item-title>Hello, user #{{ authStore.user?.userId }}</v-list-item-title>
          </v-list-item>
          <v-divider/>
          <v-list-item @click="authStore.logout">
            <v-list-item-title class="text-danger">
              <v-icon left>mdi-logout</v-icon>
              Logout
            </v-list-item-title>
          </v-list-item>
        </v-list>
      </v-menu>
    </v-app-bar>

    <v-main>
      <v-container fluid>
        <FileTable
            @share="onFileShare"
            @upload="onFileUpload"
        />
      </v-container>
    </v-main>

    <ShareDialog
        v-model="shareDialog"
        :file="selectedFile"
    />
  </div>
</template>

<script setup lang="ts">
import {definePageMeta, onMounted, ref} from '#imports'
import type {FileItem} from '~/types'
import {useFilesStore} from '~/stores/files'
import {useAuthStore} from '~/stores/auth'
import FileUpload from '~/components/FileUpload.vue'
import FileTable from '~/components/FileTable.vue'
import ShareDialog from '~/components/ShareDialog.vue'

definePageMeta({
  middleware: 'auth'
})

const authStore = useAuthStore()
const filesStore = useFilesStore()

const shareDialog = ref(false)
const selectedFile = ref<FileItem | null>(null)

const onFileShare = (file: FileItem) => {
  selectedFile.value = file
  shareDialog.value = true
}

const onFileUpload = async (files: File[]) => {
  for (const file of files) {
    try {
      await filesStore.uploadFile(file)
    } catch (error) {
      console.error('Upload failed:', error)
    }
  }
}

onMounted(() => {
  filesStore.fetchFiles()
})
</script>
