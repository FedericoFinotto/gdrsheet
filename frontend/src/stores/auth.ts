import {defineStore} from 'pinia'
import {computed, ref} from 'vue'
import api from '../service/api'
import {LoginResponse, UtenteAuth} from '../models/dto/Auth'

const TOKEN_KEY      = 'auth_token'
const UTENTE_KEY     = 'auth_utente'
const MUST_SET_KEY   = 'auth_must_set_password'
const ADMIN_MODE_KEY = 'auth_admin_mode'

function loadUtente(): UtenteAuth | null {
    try {
        return JSON.parse(localStorage.getItem(UTENTE_KEY) ?? 'null')
    } catch {
        return null
    }
}

export const useAuthStore = defineStore('auth', () => {
    const token         = ref<string | null>(localStorage.getItem(TOKEN_KEY))
    const utente        = ref<UtenteAuth | null>(loadUtente())
    const mustSetPassword = ref<boolean>(localStorage.getItem(MUST_SET_KEY) === '1')
    // Parte sempre disabilitata: l'admin deve attivarsi esplicitamente
    const adminMode     = ref<boolean>(localStorage.getItem(ADMIN_MODE_KEY) === '1')

    const isAuthenticated = computed(() => !!token.value)

    // true se il ruolo reale dell'utente è ADMIN/SUPERUSER
    const isRealAdmin = computed(() => {
        const r = (utente.value?.ruolo ?? '').toUpperCase()
        return r === 'ADMIN' || r === 'SUPERUSER'
    })

    // ruolo effettivo: gli admin in modalità non-admin vengono trattati come GIOCATORE
    const effectiveRuolo = computed<string>(() => {
        if (!isRealAdmin.value) return utente.value?.ruolo ?? ''
        return adminMode.value ? (utente.value?.ruolo ?? '') : 'GIOCATORE'
    })

    function setAdminMode(value: boolean) {
        adminMode.value = value
        localStorage.setItem(ADMIN_MODE_KEY, value ? '1' : '0')
    }

    // applica una sessione (login o impersonazione)
    function applySession(res: LoginResponse): boolean {
        token.value = res.token
        utente.value = res.utente
        mustSetPassword.value = !!res.mustSetPassword
        adminMode.value = false  // sempre non-admin al login
        localStorage.setItem(TOKEN_KEY, res.token)
        localStorage.setItem(UTENTE_KEY, JSON.stringify(res.utente))
        localStorage.setItem(MUST_SET_KEY, res.mustSetPassword ? '1' : '0')
        localStorage.setItem(ADMIN_MODE_KEY, '0')
        return mustSetPassword.value
    }

    // ritorna true se l'utente deve impostare la password
    async function login(username: string, password: string): Promise<boolean> {
        const res = await api.post<LoginResponse>('/auth/login', {username, password})
        return applySession(res.data)
    }

    function setUtente(u: UtenteAuth) {
        utente.value = u
        localStorage.setItem(UTENTE_KEY, JSON.stringify(u))
    }

    function setPasswordDone() {
        mustSetPassword.value = false
        localStorage.setItem(MUST_SET_KEY, '0')
    }

    function logout(): void {
        token.value = null
        utente.value = null
        mustSetPassword.value = false
        adminMode.value = false
        localStorage.removeItem(TOKEN_KEY)
        localStorage.removeItem(UTENTE_KEY)
        localStorage.removeItem(MUST_SET_KEY)
        localStorage.removeItem(ADMIN_MODE_KEY)
    }

    return {
        token, utente, mustSetPassword, adminMode, isRealAdmin, effectiveRuolo,
        isAuthenticated, login, applySession, setPasswordDone, setUtente, setAdminMode, logout,
    }
})
