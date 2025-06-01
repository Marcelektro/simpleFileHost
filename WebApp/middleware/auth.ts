import {defineNuxtRouteMiddleware} from "#app"
import {useAuthStore} from "~/stores/auth"
import {navigateTo} from "#app"

export default defineNuxtRouteMiddleware((to) => {
    const authStore = useAuthStore()

    if (!authStore.isAuthenticated) {
        return navigateTo("/login")
    }
})
