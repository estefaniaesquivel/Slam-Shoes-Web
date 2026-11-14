<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 1000px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h2 class="mb-0"><i class="bi bi-bookmark-star me-2"></i>Marcas</h2>
        <router-link to="/admin/modulos" class="btn btn-outline-light btn-sm">Volver a módulos</router-link>
      </div>
      <p class="text-secondary small">Nombre y logo de cada marca (los cuadros del inicio).</p>

      <AdminImagenNombreTabla tipo="marcas" />
    </div>
  </div>
</template>

<script setup>
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import AdminImagenNombreTabla from '@/components/AdminImagenNombreTabla.vue';
</script>
