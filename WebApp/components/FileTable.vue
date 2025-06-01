<template>
  <div
      class="drop-zone"
      :class="{ 'drag-over': dragOver }"
      @dragover.prevent="dragOver = true"
      @dragleave="dragOver = false"
      @drop="onDrop"
  >
    <v-data-table
        :headers="headers"
        :items="filesStore.files"
        :loading="filesStore.loading"
        class="elevation-1"
        @click:row="onRowClick"
    >
      <template #item.filename="{ item }">
        <v-icon icon="mdi-file"/>
        {{ item.filename }}
      </template>

      <template #item.size="{ item }">
        {{ formatFileSize(item.size) }}
      </template>

      <template #item.uploadedAt="{ item }">
        {{ formatDate(item.uploadedAt) }}
      </template>

      <template #item.actions="{ item }">
        <v-btn
            icon="mdi-share"
            size="small"
            variant="text"
            @click.stop="$emit('share', item)"
        >
          <template v-if="item.sharedLinksCount > 0">
            <v-icon icon="mdi-link-variant"/>
            {{ item.sharedLinksCount }}
          </template>
          <v-icon v-else icon="mdi-share"/>
        </v-btn>
        <v-btn
            icon="mdi-delete"
            size="small"
            variant="text"
            color="error"
            @click.stop="filesStore.deleteFile(item.fileId)"
        />
      </template>
    </v-data-table>
  </div>
</template>

<script setup lang="ts">
import {ref} from 'vue'
import {useFilesStore} from '~/stores/files'
import {useFileUtils} from '~/composables/useFileUtils'
import type {FileItem} from '~/types'

const emit = defineEmits<{
  share: [file: FileItem]
  upload: [files: File[]]
}>()

const filesStore = useFilesStore()
const {formatFileSize, formatDate, downloadFile} = useFileUtils()

const dragOver = ref(false)

const headers = [
  {title: 'Name', key: 'filename', sortable: true},
  {title: 'Size', key: 'size', sortable: true},
  {title: 'Upload Date', key: 'uploadedAt', sortable: true},
  {title: 'Actions', key: 'actions', sortable: false}
]

const onDrop = (event: DragEvent) => {
  event.preventDefault()
  dragOver.value = false

  if (event.dataTransfer?.files) {
    const files = Array.from(event.dataTransfer.files)
    emit('upload', files)
  }
}

const onRowClick = (event: Event, {item}: { item: FileItem }) => {
  downloadFile(item.fileId, item.filename);
}
</script>

<style scoped>
.drop-zone {
  min-height: 400px;
  transition: background-color 0.3s ease;
}

.drop-zone.drag-over {
  background-color: rgba(210, 151, 25, 0.1);
  border: 2px dashed #c6961a;
  border-radius: 8px;
}

:deep(.v-data-table tbody tr) {
  cursor: pointer;
}

:deep(.v-data-table tbody tr:hover) {
  background-color: rgba(0, 0, 0, 0.04);
}
</style>
