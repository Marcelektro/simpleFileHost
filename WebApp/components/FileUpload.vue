<template>
  <div>
    <v-btn
        class="bg-green-darken-2"
        prepend-icon="mdi-upload"
        @click="$refs.fileInput.click()"
    >
      Upload Files
    </v-btn>

    <input
        ref="fileInput"
        type="file"
        multiple
        :accept="allowedTypes.join(',')"
        style="display: none"
        @change="onFileSelect"
    />
  </div>
</template>

<script setup lang="ts">
import {useFilesStore} from '~/stores/files'
import {useFileValidation} from '~/composables/useFileValidation'
import {useNotification} from '~/composables/useNotification'
import {ref} from 'vue'

const filesStore = useFilesStore()
const {validateFiles, allowedTypes} = useFileValidation()
const notification = useNotification()
const fileInput = ref<HTMLInputElement | null>(null)

const onFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  const files = target.files

  if (files && files.length > 0) {
    uploadFiles(Array.from(files))
  }

  if (target) {
    target.value = ''
  }
}

const uploadFiles = async (files: File[]) => {
  const {valid, invalid} = validateFiles(files)

  invalid.forEach(({file, error}) => {
    notification.error(`${file.name}: ${error}`)
  })

  // upload one by one
  for (const file of valid) {
    try {
      await filesStore.uploadFile(file)
    } catch (error) {
      console.error('Upload failed:', error)
    }
  }
}

defineExpose({
  uploadFiles
})
</script>
