import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { VitePWA } from 'vite-plugin-pwa'

export default defineConfig({
    base: '/',
    plugins: [
        vue(),
        VitePWA({
            registerType: 'autoUpdate',
            strategies: 'generateSW',
            includeAssets: ['favicon.ico', 'apple-touch-icon.png', 'robots.txt'],
            devOptions: {
                enabled: true
            },
            manifest: {
                name: 'D&D Companion',
                short_name: 'D&D',
                description: 'Scheda personaggio D&D 3.5',
                theme_color: '#ffffff',
                background_color: '#ffffff',
                display: 'standalone',
                start_url: '/',
                icons: [
                    { src: 'icon-192.png', sizes: '192x192', type: 'image/png', purpose: 'any maskable' },
                    { src: 'icon-512.png', sizes: '512x512', type: 'image/png', purpose: 'any maskable' }
                ]
            },
            workbox: {
                globPatterns: ['**/*.{js,css,html,png,svg}'],
                runtimeCaching: [
                    {
                        urlPattern: /\/_nuxt\//,
                        handler: 'NetworkFirst',
                        options: {
                            cacheName: 'assets-cache'
                        }
                    }
                ]
            }
        })
    ],
    resolve: {
        alias: {
            '@': '/src'
        }
    },
    server: {
        host: true,
        port: 5173,
        strictPort: true,
        allowedHosts: ['dndlocal'],
        proxy: {
            '/api': {
                target: 'http://localhost:8080',
                changeOrigin: true,
                secure: false,
                rewrite: path => path.replace(/^\/api/, '/api')
            }
        }
    }
})
