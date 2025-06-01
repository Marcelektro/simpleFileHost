import {useApi} from "~/composables/useApi"
import {useNotification} from "~/composables/useNotification"
import type {ListShareLinksResponse, SharedFileValidation, ShareLink, ShareLinkRequest} from "~/types"

export const useSharingStore = defineStore("sharing", () => {
    const {api} = useApi()
    const notification = useNotification()

    const createShareLink = async (request: ShareLinkRequest): Promise<ShareLink> => {
        try {
            const response = await api<ShareLink>("/sharing", {
                method: "POST",
                body: request,
            })

            notification.success("Share link created successfully!")
            return response
        } catch (error) {
            throw error
        }
    }

    const updateShareLink = async (linkId: string, updates: Partial<ShareLinkRequest>): Promise<ShareLink> => {
        try {
            const response = await api<ShareLink>(`/sharing/${linkId}`, {
                method: "PUT",
                body: updates,
            })

            notification.success("Share link updated successfully!")
            return response
        } catch (error) {
            throw error
        }
    }

    const deleteShareLink = async (linkId: string): Promise<void> => {
        try {
            await api(`/sharing/${linkId}`, {
                method: "DELETE",
            })

            notification.success("Share link deleted successfully!")
        } catch (error) {
            throw error
        }
    }

    const validateSharedFile = async (linkId: string, password?: string): Promise<SharedFileValidation> => {
        try {
            const params = new URLSearchParams()

            if (password)
                params.append("password", password)

            return await api<SharedFileValidation>(`/sharing/${linkId}/validate?${params}`)
        } catch (error) {
            throw error
        }
    }

    const getShareLinks = async (fileId: string): Promise<ListShareLinksResponse> => {
        try {
            return await api<ListShareLinksResponse>(`/files/${fileId}/shareLinks`)
        } catch (error) {
            throw error
        }
    }

    return {
        createShareLink,
        updateShareLink,
        deleteShareLink,
        validateSharedFile,
        getShareLinks
    }
})
