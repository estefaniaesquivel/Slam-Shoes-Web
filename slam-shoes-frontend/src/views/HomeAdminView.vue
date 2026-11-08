<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden">

    <!-- Encabezado del administrador: edición y pedidos en lugar de favoritos y carrito -->
    <NavbarPrincipal modo-admin />

    <!-- Mismo contenido del inicio, sin el botón de favoritos -->
    <HomeContenido modo-admin />

  </div>
</template>

<script>
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import HomeContenido from '@/components/HomeContenido.vue';

export default {
  name: 'HomeAdminView',
  components: { NavbarPrincipal, HomeContenido }
};
</script>
