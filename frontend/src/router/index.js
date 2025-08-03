import {createRouter, createWebHistory} from 'vue-router';
import Home from '@/views/Home.vue';
import Mobile_Cico_0_CharacterSheet from "../views/sheets/cico/character/Mobile_Cico_0_CharacterSheet.vue";

const routes = [
  {path: '/', component: Home},
  {
    path: '/scheda/:id',
    name: 'Scheda',
    component: Mobile_Cico_0_CharacterSheet,
    props: route => ({idPersonaggio: Number(route.params.id)})
  }
];


export default createRouter({
  history: createWebHistory(),
  routes,
});
