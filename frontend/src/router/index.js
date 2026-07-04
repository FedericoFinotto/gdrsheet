import {createRouter, createWebHistory} from 'vue-router';
import Home from '@/views/Home.vue';
import Login from '@/views/Login.vue';
import Party from '@/views/Party.vue';
import PartyItems from '@/views/PartyItems.vue';
import PartyBanche from '@/views/PartyBanche.vue';
import BancaDetail from '@/views/BancaDetail.vue';
import Compendio from '@/views/Compendio.vue';
import Users from '@/views/Users.vue';
import StatsAdmin from '@/views/StatsAdmin.vue';
import SetPassword from '@/views/SetPassword.vue';
import Mobile_Cico_0_CharacterSheet from "../views/sheets/cico/character/Cico/Sheet/Mobile_Cico_0_CharacterSheet.vue";
import ItemEditor from "../views/sheets/cico/character/Cico/Editor/ItemEditor.vue";
import ItemCreate from "../views/sheets/cico/character/Cico/Editor/ItemCreate.vue";
import GestisciGradi from "../views/sheets/cico/character/Cico/Editor/GestisciGradi.vue";
import Account from "@/views/Account.vue";
import GestisciGruppi from "@/views/GestisciGruppi.vue";

const routes = [
  {path: '/', component: Home},
  {path: '/login', name: 'Login', component: Login, meta: {public: true}},
  {path: '/party/:id', name: 'Party', component: Party},
  {path: '/party/:id/items', name: 'PartyItems', component: PartyItems},
  {path: '/party/:id/banche', name: 'PartyBanche', component: PartyBanche},
  {path: '/party/:id/gruppi', name: 'GestisciGruppi', component: GestisciGruppi},
  {path: '/banca/:id', name: 'BancaDetail', component: BancaDetail},
  {path: '/compendio', name: 'Compendio', component: Compendio},
  {path: '/users', name: 'Users', component: Users},
  {path: '/stats-admin', name: 'StatsAdmin', component: StatsAdmin},
  {path: '/set-password', name: 'SetPassword', component: SetPassword},
  {path: '/account', name: 'Account', component: Account},
  {
    path: '/scheda/:id',
    name: 'Scheda',
    component: Mobile_Cico_0_CharacterSheet,
    props: route => ({idPersonaggio: Number(route.params.id)})
  },
  {
    path: '/itemeditor/:id',
    name: 'ItemEditor',
    component: ItemEditor,
    props: route => ({idItem: Number(route.params.id)})
  },
  {
    path: '/itemcreate/:tipo?',
    name: 'ItemCreate',
    component: ItemCreate
  },
  {
    path: '/gestisci-gradi/:idPersonaggio',
    name: 'GestisciGradi',
    component: GestisciGradi,
    props: route => ({idPersonaggio: Number(route.params.idPersonaggio)})
  }
];


const router = createRouter({
  history: createWebHistory(),
  routes,
});

// guard: senza token si va solo sulle rotte pubbliche
router.beforeEach((to) => {
  const isPublic = !!to.meta.public;
  const token = localStorage.getItem('auth_token');
  if (!isPublic && !token) {
    return {path: '/login', query: {redirect: to.fullPath}};
  }
  if (to.path === '/login' && token) {
    return {path: '/'};
  }
  // utente che deve impostare la password: forzato sulla pagina dedicata
  const mustSetPassword = localStorage.getItem('auth_must_set_password') === '1';
  if (token && mustSetPassword && to.path !== '/set-password') {
    return {path: '/set-password'};
  }
  return true;
});

export default router;
