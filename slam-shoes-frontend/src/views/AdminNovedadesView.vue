<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 1100px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h2 class="mb-0"><i class="bi bi-stars me-2"></i>Novedades</h2>
        <router-link to="/admin/modulos" class="btn btn-outline-light btn-sm">Volver a módulos</router-link>
      </div>
      <p class="text-secondary small">Marca productos como novedad por un periodo: mientras está vigente aparecen en el bloque Novedades del inicio y en su panel.</p>

      <PromocionProductosTabla tipo="novedades" />
    </div>
  </div>
</template>

<script setup>
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import PromocionProductosTabla from '@/components/PromocionProductosTabla.vue';
</script>
