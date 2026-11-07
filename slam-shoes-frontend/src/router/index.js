import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import FavoritosView from '../views/FavoritosView.vue' // <--- 1. Importar la vista
import PlayerView from '../views/PlayerView.vue'
import CollectionView from '../views/CollectionView.vue'
import CatalogoView from '../views/CatalogoView.vue'

const routes = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/profile',
    name: 'profile',
    component: ProfileView
  },
  {
    path: '/producto/:id',
    name: 'product-detail',
    component: ProductDetailView
  },
  {
    path: '/jugador/:id',
    name: 'jugador',
    component: PlayerView
  },
  {
    path: '/coleccion/:id',
    name: 'coleccion',
    component: CollectionView
  },
  {
    path: '/marca/:id',
    name: 'marca',
    component: CatalogoView,
    meta: { tipo: 'marca' }
  },
  {
    path: '/categoria/:id',
    name: 'categoria',
    component: CatalogoView,
    meta: { tipo: 'categoria' }
  },
  {
    path: '/favoritos',      // <--- 2. Definir la ruta
    name: 'favoritos',
    component: FavoritosView
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router