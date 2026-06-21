import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import api from '../service/api'
import {LoginResponse, UtenteAuth} from '../models/dto/Auth'

const TOKEN_KEY = 'auth_token'
const UTENTE_KEY = 'auth_utente'
const MUST_SET_KEY = 'auth_must_set_password'

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
    const mustSetPassword = ref<boolean>(localStorage.getItem(MUST_SET_KEY) === '1')

    const isAuthenticated = computed(() => !!token.value)

    // applica una sessione (login o impersonazione)
    function applySession(res: LoginResponse): boolean {
        token.value = res.token
        utente.value = res.utente
        mustSetPassword.value = !!res.mustSetPassword
        localStorage.setItem(TOKEN_KEY, res.token)
        localStorage.setItem(UTENTE_KEY, JSON.stringify(res.utente))
        localStorage.setItem(MUST_SET_KEY, res.mustSetPassword ? '1' : '0')
        return mustSetPassword.value
    }

    // ritorna true se l'utente deve impostare la password
    async function login(username: string, password: string): Promise<boolean> {
        const res = await api.post<LoginResponse>('/auth/login', {username, password})
        return applySession(res.data)
    }

    function setPasswordDone() {
        mustSetPassword.value = false
        localStorage.setItem(MUST_SET_KEY, '0')
    }

    function logout(): void {
        token.value = null
        utente.value = null
        mustSetPassword.value = false
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(UTENTE_KEY)
        localStorage.removeItem(MUST_SET_KEY)
    }

    return {token, utente, mustSetPassword, isAuthenticated, login, applySession, setPasswordDone, logout}
})
