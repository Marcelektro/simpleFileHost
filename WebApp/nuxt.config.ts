import vuetify, {transformAssetUrls} from 'vite-plugin-vuetify'

// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
    build: {
        transpile: ['vuetify'],
    },
    compatibilityDate: '2025-05-15',
    telemetry: false,
    devtools: {enabled: false},
    modules: [
        (_options, nuxt) => {
            nuxt.hooks.hook('vite:extendConfig', (config) => {
                // @ts-expect-error
                config.plugins.push(vuetify({autoImport: true}))
            })
        },
        '@nuxt/icon',
        '@pinia/nuxt'
    ],
    // Runtime config for environment variables
    runtimeConfig: {
        // Private keys (only available on server-side)
        debug: process.env.DEBUG || "false",

        // Public keys (exposed to client-side)
        public: {
            apiBase: process.env.API_BASE_URL || "/api",
            debug: process.env.DEBUG === "true",
        },
    },
    imports: {
        dirs: [
            "stores",
            "composables"
        ],
    },
    vite: {
        vue: {
            template: {
                transformAssetUrls,
            },
        },
        define: {
            "process.env.DEBUG": false,
        },
        ssr: {
            noExternal: ["vuetify"],
        },
    },
    pinia: {
        storesDirs: [
            './stores/**'
        ],
    }
})