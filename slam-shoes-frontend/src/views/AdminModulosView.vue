<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-5" style="max-width: 900px;">
      <h2 class="mb-1"><i class="bi bi-pencil-square me-2"></i>Módulos de edición</h2>
      <p class="text-secondary mb-4">Elige qué quieres crear o editar.</p>

      <div class="row g-4">
        <div class="col-sm-6" v-for="m in modulos" :key="m.clave">
          <router-link :to="m.ruta || `/admin/modulo/${m.clave}`" class="modulo-card d-flex align-items-center gap-3 border border-secondary rounded-3 bg-black p-4 text-decoration-none text-light">
            <i :class="['bi', m.icono, 'fs-1']"></i>
            <div>
              <h5 class="mb-1">{{ m.nombre }}</h5>
              <p class="text-secondary small mb-0">{{ m.descripcion }}</p>
            </div>
          </router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

const modulos = [
  { clave: 'productos', ruta: '/admin/productos', nombre: 'Productos', descripcion: 'Crear y editar productos del catálogo', icono: 'bi-box2' },
  { clave: 'banners', nombre: 'Banners de inicio', descripcion: 'Imágenes del carrusel principal', icono: 'bi-images' },
  { clave: 'ofertas', nombre: 'Ofertas', descripcion: 'Descuentos por producto y periodo', icono: 'bi-tag' },
  { clave: 'colecciones', nombre: 'Colecciones', descripcion: 'Colecciones con imagen y banner', icono: 'bi-collection' }
];
</script>

<style scoped>
.modulo-card { transition: transform 0.2s ease, border-color 0.2s ease; }
.modulo-card:hover { transform: translateY(-4px); border-color: #fff !important; }
</style>
