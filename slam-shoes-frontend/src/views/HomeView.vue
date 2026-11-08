<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden">

    <!-- Encabezado (Navbar) compartido -->
    <NavbarPrincipal />

    <!-- Contenido del inicio (banners, ofertas, novedades, tendencias, marcas, colecciones y catálogo) -->
    <HomeContenido />

    <!-- Componente del Carrito Desplegable -->
    <CarritoComponent />

  </div>
</template>

<script>
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import HomeContenido from '@/components/HomeContenido.vue';

export default {
  name: 'HomeView',
  components: { CarritoComponent, NavbarPrincipal, HomeContenido }
};
</script>
