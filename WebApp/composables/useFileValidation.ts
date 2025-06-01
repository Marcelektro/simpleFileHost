import {appConfig} from "~/config/app"
import {useFileUtils} from "~/composables/useFileUtils"

export const useFileValidation = () => {

    const {formatFileSize} = useFileUtils()

    const validateFile = (file: File): { valid: boolean; error?: string } => {
        if (file.size > appConfig.upload.maxFileSize) {
            return {
                valid: false,
                error: `File size exceeds maximum limit of ${formatFileSize(appConfig.upload.maxFileSize)}`,
            }
        }

        const isAllowedType = appConfig.upload.allowedTypes.some((type) => {
            if (type.endsWith("/*")) {
                const baseType = type.replace("/*", "")
                return file.type.startsWith(baseType)
            }
            return file.type === type
        })

        if (!isAllowedType) {
            return {
                valid: false,
                error: "File type not allowed",
            }
        }

        return {valid: true}
    }

    const validateFiles = (files: File[]): { valid: File[]; invalid: { file: File; error: string }[] } => {
        const valid: File[] = []
        const invalid: { file: File; error: string }[] = []

        files.forEach((file) => {
            const validation = validateFile(file)
            if (validation.valid) {
                valid.push(file)
            } else {
                invalid.push({file, error: validation.error!})
            }
        })

        return {valid, invalid}
    }

    return {
        validateFile,
        validateFiles,
        maxFileSize: appConfig.upload.maxFileSize,
        allowedTypes: appConfig.upload.allowedTypes,
    }
}
