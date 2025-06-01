import type {FileItem, FilesResponse} from "~/types"
import {useApi} from "~/composables/useApi"
import {useNotification} from "~/composables/useNotification"
import {ref, readonly} from "vue"

export const useFilesStore = defineStore("files", () => {
    const {api} = useApi()
    const notification = useNotification()

    const files = ref<FileItem[]>([])
    const loading = ref(false)

    const fetchFiles = async () => {
        loading.value = true
        try {

            const response = await api<FilesResponse>(`/files`)
            files.value = response.files
        } catch (error) {
            console.error("Failed to fetch files:", error)
        } finally {
            loading.value = false
        }
    }

    const uploadFile = async (file: File): Promise<void> => {
        const formData = new FormData()
        formData.append("file", file)

        try {
            await api("/files/upload", {
                method: "POST",
                body: formData,
            })

            notification.success(`File "${file.name}" uploaded successfully!`)
            await fetchFiles()
        } catch (error) {
            throw error
        }
    }

    const deleteFile = async (fileId: string): Promise<void> => {
        try {
            await api(`/files/${fileId}`, {
                method: "DELETE",
            })

            notification.success("File deleted successfully")
            await fetchFiles()
        } catch (error) {
            throw error
        }
    }

    return {
        files: readonly(files),
        loading: readonly(loading),
        fetchFiles,
        uploadFile,
        deleteFile,
    }
})
