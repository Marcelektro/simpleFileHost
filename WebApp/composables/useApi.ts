import {useCookie, useRuntimeConfig} from "#app"
import type {ApiErrorResponse} from "~/types"

export const useApi = () => {
    const config = useRuntimeConfig()
    const authCookie = useCookie("auth-token")

    const handleApiError = (response: any): ApiErrorResponse => {
        return {
            errorType: response._data?.errorType || "UNKNOWN_ERROR",
            errorMessage: response._data?.errorMessage || "An error occurred"
        }
    }

    const apiClient = $fetch.create({
        baseURL: config.public.apiBase,
        onRequest({request, options}) {
            const token = authCookie.value
            if (token) {
                options.headers = options.headers || {}
                options.headers.set("Authorization", token)
            }

            if (config.public.debug) {
                console.log("API Request:", request, options)
            }
        },
        onResponse({response}) {
            if (config.public.debug) {
                console.log("API Response:", response)
            }
        },
        onResponseError({response}) {
            if (config.public.debug) {
                console.error("API Error:", response)
            }

            throw handleApiError(response)

        },
    })

    return {api: apiClient}
}
