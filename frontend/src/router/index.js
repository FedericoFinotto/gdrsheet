import {createRouter, createWebHistory} from 'vue-router';
import Home from '@/views/Home.vue';
import Login from '@/views/Login.vue';
import Party from '@/views/Party.vue';
import PartyItems from '@/views/PartyItems.vue';
import PartyBanche from '@/views/PartyBanche.vue';
import BancaDetail from '@/views/BancaDetail.vue';
import Mobile_Cico_0_CharacterSheet from "../views/sheets/cico/character/Cico/Sheet/Mobile_Cico_0_CharacterSheet.vue";
import ItemEditor from "../views/sheets/cico/character/Cico/Editor/ItemEditor.vue";
import ItemCreate from "../views/sheets/cico/character/Cico/Editor/ItemCreate.vue";

const routes = [
  {path: '/', component: Home},
  {path: '/login', name: 'Login', component: Login, meta: {public: true}},
  {path: '/party/:id', name: 'Party', component: Party},
  {path: '/party/:id/items', name: 'PartyItems', component: PartyItems},
  {path: '/party/:id/banche', name: 'PartyBanche', component: PartyBanche},
  {path: '/banca/:id', name: 'BancaDetail', component: BancaDetail},
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
  return true;
});

export default router;
