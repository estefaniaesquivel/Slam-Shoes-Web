<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-5 text-center" style="max-width: 700px;">
      <i class="bi bi-tools fs-1 text-secondary"></i>
      <h2 class="mt-3 text-capitalize">{{ nombre }}</h2>
      <p class="text-secondary">Este módulo se habilitará próximamente.</p>
      <router-link to="/admin/modulos" class="btn btn-outline-light">Volver a módulos</router-link>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

const route = useRoute();
const nombres = { productos: 'Productos', banners: 'Banners de inicio', ofertas: 'Ofertas', colecciones: 'Colecciones', jugadores: 'Jugadores', marcas: 'Marcas', novedades: 'Novedades' };
const nombre = computed(() => nombres[route.params.modulo] || 'Módulo');
</script>
