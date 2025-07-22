import {defineConfig} from 'vite'
import vue from '@vitejs/plugin-vue'
import {VitePWA} from "vite-plugin-pwa";

export default defineConfig({
  plugins: [
    vue(),
    VitePWA({
      registerType: 'autoUpdate',
      includeAssets: ['favicon.ico', 'apple-touch-icon.png'],
      manifest: {
        name: 'D&D Companion',
        short_name: 'D&D',
        description: 'Scheda personaggio D&D 3.5',
        theme_color: '#ffffff',
        icons: [
          {
            src: '/icon-192.png',
            sizes: '192x192',
            type: 'image/png',
          },
          {
            src: '/icon-512.png',
            sizes: '512x512',
            type: 'image/png',
          }
        ],
        start_url: '/',
        display: 'standalone',
        background_color: '#ffffff'
      }
    })
  ],
  resolve: {
    alias: {
      '@': '/src'
    }
  }
})
