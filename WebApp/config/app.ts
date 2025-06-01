export const appConfig = {
    name: "simpleFileHost",
    description: "A simple file hosting platform written in Java using Javalin",
    author: "Marcelektro",
    version: "1.0.0",

    // upload limits
    upload: {
        maxFileSize: 100 * 1024 * 1024, // 100MB
        allowedTypes: [
            "image/*",
            "application/pdf",
            "text/*"
        ],
    }
}

export default appConfig
