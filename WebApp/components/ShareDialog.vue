<template>
  <v-dialog v-model="dialog" max-width="800px" theme="light" @after-leave="clearFormValues()">
    <v-card>
      <v-card-title>Share File</v-card-title>

      <v-card-text>
        <div>
          <v-skeleton-loader
              v-if="linksLoading"
              type="table"
              class="mb-2"
          />
          <template v-else-if="shareLinks.length">
            <div class="mb-2 font-weight-bold">Existing Share Links</div>
            <v-table density="compact">
              <thead>
              <tr>
                <th>Link</th>
                <th>Expires</th>
                <th>Password-protected</th>
                <th class="text-right">Actions</th>
              </tr>
              </thead>
              <tbody>
              <tr v-for="link in shareLinks" :key="link.shareLinkId">
                <td>
                  <a :href="shareLinkUrl(link.shareLinkId)" target="_blank" class="text-primary">{{
                      link.shareLinkId
                    }}</a>
                </td>
                <td>
                  <span v-if="link.expiry">{{ link.expiry }}</span>
                  <span v-else>Never</span>
                </td>
                <td>
                  <span v-if="link.password">Yes</span>
                  <span v-else>No</span>
                </td>
                <td class="text-right">
                  <v-btn
                      icon
                      @click="copyShareLink(shareLinkUrl(link.shareLinkId))"
                      :loading="loading"
                      variant="text"
                      size="small"
                  >
                    <v-icon>mdi-content-copy</v-icon>
                  </v-btn>
                  <v-btn
                      icon
                      @click="startEdit(link)"
                      v-if="!isEditing(link)"
                      variant="text"
                      size="small"
                  >
                    <v-icon>mdi-pencil</v-icon>
                  </v-btn>
                  <v-btn
                      icon
                      @click="revokeLink(link.shareLinkId)"
                      :loading="deletingId === link.shareLinkId"
                      variant="text"
                      size="small"
                  >
                    <v-icon>mdi-trash-can-outline</v-icon>
                  </v-btn>
                </td>
              </tr>
              </tbody>
            </v-table>
            <v-divider class="my-4"/>
          </template>
        </div>

        <template v-if="editingLink">
          <div class="mb-2 font-weight-bold">Editing link: {{ editingLink.shareLinkId }}</div>
          <v-form @submit.prevent>
            <v-text-field
                v-model="editForm.password"
                label="Password (optional)"
                type="password"
                prepend-icon="mdi-lock"
                dense
            />
            <v-text-field
                v-model="editForm.expiry"
                label="Expires At (optional)"
                type="datetime-local"
                prepend-icon="mdi-calendar"
                dense
            />
            <div class="d-flex justify-end mt-2">
              <v-btn
                  color="primary"
                  size="small"
                  @click="updateLink"
                  :loading="updating"
                  class="mr-2"
              >Update
              </v-btn>
              <v-btn
                  size="small"
                  @click="cancelEdit"
              >Cancel
              </v-btn>
            </div>
          </v-form>
        </template>

        <template v-if="!editingLink && !linksLoading">
          <div class="mb-2 font-weight-bold">Create New Share Link</div>
          <v-form @submit.prevent>
            <v-text-field
                v-model="form.password"
                :disabled="shareLink !== ''"
                label="Password (optional)"
                type="password"
                prepend-icon="mdi-lock"
            />
            <v-text-field
                v-model="form.expiry"
                :disabled="shareLink !== ''"
                label="Expires At (optional)"
                type="datetime-local"
                prepend-icon="mdi-calendar"
            />
            <v-text-field
                v-if="shareLink"
                v-model="shareLink"
                label="Share Link"
                readonly
                prepend-icon="mdi-link"
                append-icon="mdi-content-copy"
                @click:append="copyShareLink()"
            />
          </v-form>
        </template>
      </v-card-text>
      <v-card-actions>
        <v-spacer/>
        <v-btn @click="close">
          <template v-if="editingLink || shareLink === ''">Cancel</template>
          <template v-else>Close</template>
        </v-btn>
        <v-btn
            color="primary"
            :loading="loading"
            @click="createShare"
            v-if="!editingLink && !linksLoading && shareLink === ''"
        >
          Create Share Link
        </v-btn>
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>

<script setup lang="ts">
import type {FileItem, FileShareLink} from '~/types'
import {useSharingStore} from '~/stores/sharing'
import {useNotification} from '~/composables/useNotification'
import {computed, ref, watch} from 'vue'

interface Props {
  modelValue: boolean
  file: FileItem | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  'update:modelValue': [value: boolean]
}>()

const sharingStore = useSharingStore()
const notification = useNotification()

const dialog = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const form = ref({
  password: '',
  expiry: ''
})

const shareLink = ref('')
const loading = ref(false)
const shareLinks = ref<FileShareLink[]>([])
const linksLoading = ref(false)
const deletingId = ref<string | null>(null)

const editingLink = ref<FileShareLink | null>(null)
const editForm = ref({password: '', expiry: ''})
const updating = ref(false)

const fetchShareLinks = async () => {
  if (!props.file) return
  linksLoading.value = true
  try {
    const res = await sharingStore.getShareLinks(props.file.fileId)
    shareLinks.value = res.links
  } catch (e) {
    shareLinks.value = []
  } finally {
    linksLoading.value = false
  }
}

const createShare = async () => {
  if (!props.file) return

  loading.value = true
  try {
    const request = {
      fileId: props.file.fileId,
      ...(form.value.password && {password: form.value.password}),
      ...(form.value.expiry && {expiry: form.value.expiry})
    }

    const result = await sharingStore.createShareLink(request)
    shareLink.value = shareLinkUrl(result.shareLinkId)

    notification.success('Share link created successfully!')

    await fetchShareLinks()
  } catch (error) {
    notification.error('Failed to create share link')
    console.error('Failed to create share link:', error)
  } finally {
    loading.value = false
    props.file.sharedLinksCount = (props.file.sharedLinksCount || 0) + 1
  }
}

const copyShareLink = async (value: string = shareLink.value) => {
  try {
    await navigator.clipboard.writeText(value)
    notification.success('Share link copied to clipboard!')
  } catch (error) {
    notification.error('Failed to copy link to clipboard')
  }
}

const revokeLink = async (linkId: string) => {
  if (!props.file) return

  deletingId.value = linkId
  try {
    await sharingStore.deleteShareLink(linkId)
    notification.success('Share link revoked')
    await fetchShareLinks()
  } catch (e) {
    notification.error('Failed to revoke link')
    console.error('Failed to revoke link:', e)
  } finally {
    deletingId.value = null
    shareLink.value = ''
    props.file.sharedLinksCount = (props.file.sharedLinksCount || 0) - 1
  }
}

const startEdit = (link: FileShareLink) => {
  editingLink.value = link
  editForm.value = {
    password: link.password || '',
    expiry: link.expiry || ''
  }
}

const isEditing = (link: FileShareLink) =>
    editingLink.value && editingLink.value.shareLinkId === link.shareLinkId

const cancelEdit = () => {
  editingLink.value = null
  editForm.value = {password: '', expiry: ''}
}

const updateLink = async () => {
  if (!editingLink.value) return
  updating.value = true
  try {
    await sharingStore.updateShareLink(editingLink.value.shareLinkId, {
      password: editForm.value.password || undefined,
      expiry: editForm.value.expiry || undefined
    })
    notification.success('Share link updated')
    editingLink.value = null
    await fetchShareLinks()
  } catch (e) {
    notification.error('Failed to update link')
    console.error('Failed to update link:', e)
  } finally {
    updating.value = false
  }
}

const close = () => {
  dialog.value = false
}

const clearFormValues = () => {
  form.value = {password: '', expiry: ''}
  shareLink.value = ''
  shareLinks.value = []
  editingLink.value = null
  editForm.value = {password: '', expiry: ''}
}


const shareLinkUrl = (id: string) => `${window.location.origin}/shared?linkId=${id}`

watch(dialog, async (newValue) => {
  if (newValue && props.file) {
    await fetchShareLinks()
  }
  if (!newValue) {
    close()
  }
})
</script>
