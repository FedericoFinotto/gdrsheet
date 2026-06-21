<script setup lang="ts">
import {computed, ref} from 'vue'
import {useRouter} from 'vue-router'
import {useAuthStore} from '../stores/auth'
import {changePassword} from '../service/AuthService'

const router = useRouter()
const auth = useAuthStore()

// se l'utente deve impostarla per la prima volta non serve la vecchia
const primaVolta = computed(() => auth.mustSetPassword)

const oldPassword = ref('')
const newPassword = ref('')
const confirm = ref('')
const busy = ref(false)
const errorMsg = ref<string | null>(null)

const canSubmit = computed(() =>
    !busy.value &&
    newPassword.value.length >= 4 &&
    newPassword.value === confirm.value &&
    (primaVolta.value || oldPassword.value.length > 0)
)

async function onSubmit() {
  if (!canSubmit.value) return
  busy.value = true
  errorMsg.value = null
  try {
    await changePassword(newPassword.value, primaVolta.value ? undefined : oldPassword.value)
    auth.setPasswordDone()
    router.replace('/')
  } catch (e: any) {
    errorMsg.value = e?.response?.status === 403
        ? 'Password attuale non corretta'
        : 'Errore nel salvataggio'
  } finally {
    busy.value = false
  }
}

function annulla() {
  router.replace('/')
}
</script>

<template>
  <div class="setpw-page">
    <form class="setpw-card" @submit.prevent="onSubmit">
      <h1>{{ primaVolta ? 'Imposta la password' : 'Cambia password' }}</h1>
      <p v-if="primaVolta" class="muted">Imposta una password per il tuo account.</p>

      <label v-if="!primaVolta" class="field">
        <span class="lbl">Password attuale</span>
        <input v-model="oldPassword" type="password" autocomplete="current-password"/>
      </label>

      <label class="field">
        <span class="lbl">Nuova password</span>
        <input v-model="newPassword" type="password" autocomplete="new-password" autofocus/>
      </label>

      <label class="field">
        <span class="lbl">Conferma password</span>
        <input v-model="confirm" type="password" autocomplete="new-password"/>
      </label>

      <p v-if="newPassword && confirm && newPassword !== confirm" class="error">Le password non coincidono</p>
      <p v-if="errorMsg" class="error">{{ errorMsg }}</p>

      <div class="actions">
        <button v-if="!primaVolta" type="button" class="btn ghost" @click="annulla">Annulla</button>
        <button type="submit" class="btn primary" :disabled="!canSubmit">
          {{ busy ? 'Salvataggio…' : 'Salva' }}
        </button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.setpw-page {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  background: #f3f4f6;
  padding: 1rem;
}

.setpw-card {
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
  margin: 0;
  font-size: 1.2rem;
  text-align: center;
}

.muted { margin: 0; opacity: .7; font-size: .85rem; text-align: center; }

.field {
  display: grid;
  gap: .35rem;
}

.lbl { font-size: .8rem; font-weight: 600; opacity: .85; }

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

.actions { display: flex; justify-content: flex-end; gap: .5rem; }

.btn {
  padding: .6rem .9rem;
  border-radius: .5rem;
  border: 1px solid #d0d5dd;
  background: #fff;
  cursor: pointer;
}
.btn.primary { background: #2563eb; color: #fff; border-color: #2563eb; }
.btn:disabled { opacity: .6; cursor: default; }
</style>
