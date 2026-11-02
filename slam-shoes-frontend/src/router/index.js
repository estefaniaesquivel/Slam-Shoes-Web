import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import ProductDetailView from '../views/ProductDetailView.vue' // <--- Importar la vista

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
    path: '/',
    name: 'profile',
    component: ProfileView
  },
  {
    path: '/producto/:id',  // <--- Ruta dinámica para el detalle
    name: 'product-detail',
    component: ProductDetailView
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router