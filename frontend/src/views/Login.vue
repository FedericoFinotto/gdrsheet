<script setup lang="ts">
import {ref} from 'vue'
import {useRoute, useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const username = ref('')
const password = ref('')
const busy = ref(false)
const errorMsg = ref<string | null>(null)

async function onSubmit() {
  if (!username.value.trim()) return
  busy.value = true
  errorMsg.value = null
  try {
    const mustSet = await auth.login(username.value.trim(), password.value)
    if (mustSet) {
      router.replace('/set-password')
      return
    }
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/'
    router.replace(redirect)
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 401
        ? 'Credenziali non valide'
        : 'Errore di connessione, riprova'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <form class="login-card" @submit.prevent="onSubmit">
      <h1>GDR Sheet</h1>

      <label class="field">
        <span class="lbl">Username</span>
        <input v-model="username" type="text" autocomplete="username" required autofocus/>
      </label>

      <label class="field">
        <span class="lbl">Password</span>
        <input v-model="password" type="password" autocomplete="current-password"/>
        <span class="hint">Lascia vuoto al primo accesso se non hai ancora una password.</span>
      </label>

      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <button type="submit" class="btn primary" :disabled="busy || !username.trim()">
        {{ busy ? 'Accesso…' : 'Accedi' }}
      </button>
    </form>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  background: #f3f4f6;
  padding: 1rem;
}

.login-card {
  width: 100%;
  max-width: 22rem;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: .75rem;
  padding: 1.5rem;
  display: grid;
  gap: .9rem;
}

h1 {
  margin: 0 0 .25rem;
  font-size: 1.3rem;
  text-align: center;
}

.field { display: grid; gap: .35rem; }
.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }
.hint { font-size: .75rem; opacity: .6; }

input {
  width: 100%;
  padding: .55rem .65rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
}

.error {
  margin: 0;
  padding: .5rem .7rem;
  border-radius: .5rem;
  color: #991b1b;
  background: #fef2f2;
  border: 1px solid #fecaca;
  font-size: .85rem;
}

.btn {
  padding: .6rem .9rem;
  border-radius: .5rem;
  border: 1px solid transparent;
  cursor: pointer;
}
.btn.primary { background: #2563eb; color: #fff; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
