export interface LoginRequest {
    username: string
    password: string
}

export interface LoginResponse {
    token: string
    userId: number
}

export interface User {
    name: string
}

export interface MeResponse {
    userId: number
}

export interface FileItem {
    fileId: string
    filename: string
    size: number
    uploadedAt: string
    sharedLinksCount: number
}

export interface FilesResponse {
    files: FileItem[]
}

export interface ShareLinkRequest {
    fileId: string
    password?: string
    expiry?: string
}

export interface ShareLink {
    shareLinkId: string
}

export interface SharedFileValidation {
    linkId: string,
    fileId: string,
    filename: string,
    fileSize: number,
    hasPassword: boolean,
    validPassword: boolean
    expiry: string | null
    hasExpired: boolean
}

export interface ApiError {
    message: string
    statusCode: number
}

export interface ApiErrorResponse {
    errorType: string
    errorMessage: string
}

export interface FileShareLink {
    shareLinkId: string
    expiry?: string
    password?: string
}

export interface ListShareLinksResponse {
    fileId: string
    links: FileShareLink[]
}
