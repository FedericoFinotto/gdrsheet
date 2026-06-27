<script setup lang="ts">
import {ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {updateMe} from '../service/AuthService'

const router = useRouter()
const auth = useAuthStore()

const username = ref(auth.utente?.username ?? '')
const name = ref(auth.utente?.name ?? '')
const busy = ref(false)
const errorMsg = ref<string | null>(null)
const successMsg = ref<string | null>(null)

async function salva() {
  if (busy.value) return
  errorMsg.value = null
  successMsg.value = null
  if (!username.value.trim() || !name.value.trim()) {
    errorMsg.value = 'Username e nome sono obbligatori'
    return
  }
  busy.value = true
  try {
    const res = await updateMe(username.value.trim(), name.value.trim())
    auth.setUtente(res.data as any)
    successMsg.value = 'Profilo aggiornato'
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message ?? e?.message ?? 'Errore nel salvataggio'
  } finally {
    busy.value = false
  }
}
</script>

<template>
  <div class="account">
    <header class="head">
      <button class="btn ghost" @click="router.back()">←</button>
      <h1>Il mio account</h1>
    </header>

    <div class="card">
      <h2>Dati personali</h2>
      <label class="field">
        <span class="lbl">Nome</span>
        <input v-model="name" type="text" placeholder="Nome visualizzato"/>
      </label>
      <label class="field">
        <span class="lbl">Username</span>
        <input v-model="username" type="text" placeholder="Username"/>
      </label>
      <p v-if="errorMsg" class="msg error">{{ errorMsg }}</p>
      <p v-if="successMsg" class="msg ok">{{ successMsg }}</p>
      <button class="btn primary" :disabled="busy" @click="salva">
        {{ busy ? 'Salvataggio…' : 'Salva modifiche' }}
      </button>
    </div>

    <div class="card">
      <h2>Password</h2>
      <p class="muted">Per cambiare la password accedi alla pagina dedicata.</p>
      <button class="btn ghost" @click="router.push('/set-password')">Cambia password</button>
    </div>
  </div>
</template>

<style scoped>
.account {
  width: 100%;
  max-width: 32rem;
  margin: 0 auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.head { display: flex; align-items: center; gap: .6rem; }
.head h1 { margin: 0; font-size: 1.2rem; }
.card {
  border: 1px solid #e5e7eb;
  border-radius: .6rem;
  padding: 1rem;
  background: #fff;
  display: flex;
  flex-direction: column;
  gap: .6rem;
}
.card h2 { margin: 0; font-size: 1rem; }
.field { display: flex; flex-direction: column; gap: .3rem; }
.lbl { font-size: .75rem; font-weight: 600; opacity: .8; }
input[type="text"] {
  padding: .45rem .6rem;
  border: 1px solid #d0d5dd;
  border-radius: .5rem;
  width: 100%;
  box-sizing: border-box;
}
.btn { padding: .5rem .9rem; border-radius: .5rem; border: 1px solid transparent; cursor: pointer; align-self: flex-start; }
.btn.ghost { border-color: #d0d5dd; background: #fff; }
.btn.primary { background: #2563eb; color: #fff; }
.btn:disabled { opacity: .6; cursor: default; }
.msg { margin: 0; font-size: .85rem; padding: .4rem .6rem; border-radius: .4rem; }
.msg.error { color: #991b1b; background: #fef2f2; }
.msg.ok { color: #166534; background: #f0fdf4; }
.muted { font-size: .85rem; opacity: .65; margin: 0; }
</style>
