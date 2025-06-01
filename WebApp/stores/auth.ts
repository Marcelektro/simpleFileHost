import {computed, ref} from "vue"
import {useApi} from "~/composables/useApi"
import {useNotification} from "~/composables/useNotification"
import {useCookie} from "#app"
import {navigateTo} from "#app"
import type {ApiErrorResponse, LoginRequest, LoginResponse, MeResponse} from "~/types"

export const useAuthStore = defineStore("auth", () => {
    const {api} = useApi()
    const notification = useNotification()

    const user = ref<MeResponse | null>(null)
    const token = useCookie("auth-token", {
        default: (): string | null => null,
        secure: true,
        sameSite: "strict",
    })

    const login = async (credentials: LoginRequest): Promise<void> => {
        try {
            const response = await api<LoginResponse>("/auth/login", {
                method: "POST",
                body: credentials,
            })

            token.value = response.token
            user.value = {userId: response.userId}
            notification.success("Login successful!", 1000)

            await navigateTo("/")
        } catch (error) {
            throw error
        }
    }

    const logout = async () => {
        token.value = null
        user.value = null
        notification.info("Logged out successfully")
        await navigateTo("/login")
    }

    const isAuthenticated = computed(() => !!token.value)


    const initializeAuth = async () => {
        if (token.value && !user.value) {
            try {
                user.value = await api<MeResponse>("/auth/me")
            } catch (error) {

                //@ts-ignore
                if (error && 'errorType' in error) {
                    const apiError = error as ApiErrorResponse
                    switch (apiError.errorType) {
                        case 'EXPIRED_TOKEN':
                        case 'TOKEN_VALIDATION_FAILURE':
                            notification.error('Session expired. Please login again!')
                            token.value = null
                            navigateTo("/login")
                            break

                        default:
                            notification.error(`An error occurred: ${apiError.errorType || 'Unknown error'}: ${apiError.errorMessage || 'Please try again.'}`)
                    }
                } else {
                    console.error("Error initializing auth:", error)
                    notification.error('An unexpected error occurred. Please try again.')
                }

            }
        }
    }

    return {
        user: computed(() => user.value),
        token: computed(() => token.value),
        login,
        logout,
        isAuthenticated,
        initializeAuth,
    }
})
