// import this after install `@mdi/font` package
import '@mdi/font/css/materialdesignicons.css'

import 'vuetify/styles'
import { createVuetify } from 'vuetify'

export default defineNuxtPlugin((app) => {
    const vuetify = createVuetify({
        theme: {
            defaultTheme: 'dark',
            // the marCloud coloring theme (marcloud.net)
            themes: {
                light: {
                    colors: {
                        primary: '#0082c9',
                        secondary: '#6ec4f1',
                        background: '#f0f3f5',
                        surface: '#ffffff'
                    },
                },
                dark: {
                    colors: {
                        primary: '#0082c9',
                        secondary: '#6ec4f1',
                        background: '#212f3f',
                        surface: '#19222f',
                    },
                }
            },
        },
    })
    app.vueApp.use(vuetify)
})
