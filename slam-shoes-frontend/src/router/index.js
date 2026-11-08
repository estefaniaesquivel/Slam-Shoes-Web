import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import ProfileView from '../views/ProfileView.vue'
import ProductDetailView from '../views/ProductDetailView.vue'
import FavoritosView from '../views/FavoritosView.vue' // <--- 1. Importar la vista
import PlayerView from '../views/PlayerView.vue'
import CollectionView from '../views/CollectionView.vue'
import CatalogoView from '../views/CatalogoView.vue'
import PromocionesView from '../views/PromocionesView.vue'
import RegistroView from '../views/RegistroView.vue'
import MiPerfilView from '../views/MiPerfilView.vue'
import ResumenPedidoView from '../views/ResumenPedidoView.vue'
import HomeAdminView from '../views/HomeAdminView.vue'
import AdminModulosView from '../views/AdminModulosView.vue'
import AdminPlaceholderView from '../views/AdminPlaceholderView.vue'
import AdminPedidosView from '../views/AdminPedidosView.vue'
import MisPedidosView from '../views/MisPedidosView.vue'
import AdminProductosView from '../views/AdminProductosView.vue'
import AdminProductoNuevoView from '../views/AdminProductoNuevoView.vue'
import AdminProductoEditarView from '../views/AdminProductoEditarView.vue'
import { obtenerUsuario } from '../utils/auth.js'

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
  { path: '/ofertas', name: 'ofertas', component: PromocionesView, meta: { tipo: 'ofertas' } },
  { path: '/novedades', name: 'novedades', component: PromocionesView, meta: { tipo: 'novedades' } },
  { path: '/registro', name: 'registro', component: RegistroView },
  { path: '/mi-perfil', name: 'mi-perfil', component: MiPerfilView, meta: { requiereSesion: true } },
  { path: '/mis-pedidos', name: 'mis-pedidos', component: MisPedidosView, meta: { requiereSesion: true } },
  { path: '/checkout', name: 'checkout', component: ResumenPedidoView, meta: { requiereSesion: true } },
  // Módulos del administrador
  { path: '/admin', name: 'admin-home', component: HomeAdminView, meta: { requiereAdmin: true } },
  { path: '/admin/modulos', name: 'admin-modulos', component: AdminModulosView, meta: { requiereAdmin: true } },
  { path: '/admin/productos', name: 'admin-productos', component: AdminProductosView, meta: { requiereAdmin: true } },
  { path: '/admin/productos/nuevo', name: 'admin-producto-nuevo', component: AdminProductoNuevoView, meta: { requiereAdmin: true } },
  { path: '/admin/productos/editar', name: 'admin-producto-editar', component: AdminProductoEditarView, meta: { requiereAdmin: true } },
  { path: '/admin/modulo/:modulo', name: 'admin-modulo', component: AdminPlaceholderView, meta: { requiereAdmin: true } },
  { path: '/admin/pedidos', name: 'admin-pedidos', component: AdminPedidosView, meta: { requiereAdmin: true } },
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

// Protección de rutas en el frontend (la validación real de roles en el servidor es una mejora pendiente)
router.beforeEach((to) => {
  const usuario = obtenerUsuario();
  // El administrador tiene su propio inicio
  if (to.path === '/' && usuario && usuario.rol === 'ADMIN') return '/admin';
  if (to.meta.requiereAdmin && (!usuario || usuario.rol !== 'ADMIN')) return '/login';
  if (to.meta.requiereSesion && !usuario) return '/login';
});

export default router