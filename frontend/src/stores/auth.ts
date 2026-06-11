import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import api from '../service/api'
import {LoginResponse, UtenteAuth} from '../models/dto/Auth'

const TOKEN_KEY = 'auth_token'
const UTENTE_KEY = 'auth_utente'

function loadUtente(): UtenteAuth | null {
    try {
        return JSON.parse(localStorage.getItem(UTENTE_KEY) ?? 'null')
    } catch {
        return null
    }
}

export const useAuthStore = defineStore('auth', () => {
    const token = ref<string | null>(localStorage.getItem(TOKEN_KEY))
    const utente = ref<UtenteAuth | null>(loadUtente())

    const isAuthenticated = computed(() => !!token.value)

    async function login(username: string, password: string): Promise<void> {
        const res = await api.post<LoginResponse>('/auth/login', {username, password})
        token.value = res.data.token
        utente.value = res.data.utente
        localStorage.setItem(TOKEN_KEY, res.data.token)
        localStorage.setItem(UTENTE_KEY, JSON.stringify(res.data.utente))
    }

    function logout(): void {
        token.value = null
        utente.value = null
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(UTENTE_KEY)
    }

    return {token, utente, isAuthenticated, login, logout}
})
