import {useState} from "#app"

export const useNotification = () => {
    const snackbar = useState("snackbar", () => ({
        show: false,
        message: "",
        color: "success" as "success" | "error" | "warning" | "info",
        timeout: 4000,
    }))

    const show = (message: string, color: "success" | "error" | "warning" | "info" = "success", timeout: number = 4_000) => {
        console.log(`Notification: ${message} (Color: ${color})`)

        const doShow = () => snackbar.value = {
            show: true,
            message,
            color,
            timeout: timeout,
        }

        if (snackbar.value.show) {
            snackbar.value.show = false
            setTimeout(doShow, 180) // let the animation finish (TODO: better way?)
        } else {
            doShow()
        }
    }

    const success = (message: string, timeout: number = 3_000) => show(message, "success", timeout)
    const error = (message: string, timeout: number = 4_000) => show(message, "error", timeout)
    const warning = (message: string, timeout: number = 4_000) => show(message, "warning", timeout)
    const info = (message: string, timeout: number = 3_000) => show(message, "info", timeout)

    const hide = () => {
        snackbar.value.show = false
    }

    return {
        snackbar: snackbar,
        show,
        success,
        error,
        warning,
        info,
        hide,
    }
}
