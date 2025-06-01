import {useApi} from "~/composables/useApi"

export const useFileUtils = () => {
    const {api} = useApi()
    const notification = useNotification()

    const formatFileSize = (bytes: number): string => {
        if (bytes === 0) return "0 Bytes"
        const k = 1024
        const sizes = ["Bytes", "KB", "MB", "GB", "TB"]
        const i = Math.floor(Math.log(bytes) / Math.log(k))
        return Number.parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + " " + sizes[i]
    }

    const formatDate = (dateString: string): string => {
        return new Date(dateString).toLocaleDateString("en-US", {
            year: "numeric",
            month: "2-digit",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
            second: "2-digit",
        })
    }

    const downloadFile = async (fileId: string, fileName: string) => {
        try {
            const blob = await api(`/files/${fileId}`, {
                method: "GET",
                responseType: "blob",
            })

            if (!(blob instanceof Blob)) {
                console.error("Failed to download file: response is not a Blob. Got: ", blob)
                notification.error("An unexpected error occurred while downloading the file.")
                return
            }

            const url = window.URL.createObjectURL(blob)
            const link = document.createElement("a")
            link.href = url
            link.download = fileName
            document.body.appendChild(link)
            link.click()
            document.body.removeChild(link)
            window.URL.revokeObjectURL(url)

            notification.success(`File "${fileName}" downloaded successfully!`)

        } catch (error) {
            console.error("Download failed", error)
            notification.error("Failed to download file. Please try again.")
        }
    }

    return {
        formatFileSize,
        formatDate,
        downloadFile,
    }
}
