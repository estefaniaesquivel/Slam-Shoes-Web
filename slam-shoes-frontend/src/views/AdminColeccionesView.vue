<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-5" style="max-width: 800px;">
      <h2 class="mb-1"><i class="bi bi-collection me-2"></i>Colecciones</h2>
      <p class="text-secondary mb-4">¿Qué quieres hacer?</p>

      <div class="row g-4">
        <div class="col-sm-6">
          <router-link to="/admin/colecciones/nueva" class="opcion-card d-flex flex-column align-items-center text-center gap-2 border border-secondary rounded-3 bg-black p-5 text-decoration-none text-light">
            <i class="bi bi-plus-circle fs-1"></i>
            <h5 class="mb-0">Agregar colección</h5>
            <p class="text-secondary small mb-0">Crear una colección con su portada y sus tenis</p>
          </router-link>
        </div>
        <div class="col-sm-6">
          <router-link to="/admin/colecciones/editar" class="opcion-card d-flex flex-column align-items-center text-center gap-2 border border-secondary rounded-3 bg-black p-5 text-decoration-none text-light">
            <i class="bi bi-pencil-square fs-1"></i>
            <h5 class="mb-0">Editar colección</h5>
            <p class="text-secondary small mb-0">Cambiar nombre, descripción, portada o los tenis de una colección</p>
          </router-link>
        </div>
      </div>

      <router-link to="/admin/modulos" class="btn btn-outline-light mt-4">Volver a módulos</router-link>
    </div>
  </div>
</template>

<script setup>
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
</script>

<style scoped>
.opcion-card { transition: transform 0.2s ease, border-color 0.2s ease; }
.opcion-card:hover { transform: translateY(-4px); border-color: #fff !important; }
</style>
