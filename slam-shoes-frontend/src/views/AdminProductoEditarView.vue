<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container-fluid px-4 my-4">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h2 class="mb-0"><i class="bi bi-pencil-square me-2"></i>Editar productos</h2>
        <router-link to="/admin/productos" class="btn btn-outline-light btn-sm">Volver</router-link>
      </div>

      <input type="text" class="form-control bg-dark text-light border-secondary mb-3" style="max-width: 400px;" placeholder="Buscar por nombre..." v-model="busqueda" />

      <div v-if="cargando" class="text-muted">Cargando...</div>
      <div v-else class="table-responsive">
        <table class="table table-dark table-bordered align-middle small">
          <thead>
            <tr><th>ID</th><th>Imagen</th><th>Nombre</th><th style="width: 140px;">Precio (MXN)</th><th>Activo</th><th>Tendencia</th><th></th></tr>
          </thead>
          <tbody>
            <tr v-for="p in filtrados" :key="p.idProducto">
              <td>{{ p.idProducto }}</td>
              <td><img :src="p.imagenUrl" style="width: 48px; height: 48px; object-fit: cover;" class="rounded bg-white" :alt="p.nombre" /></td>
              <td>{{ p.nombre }}</td>
              <td><input type="number" min="0" step="0.01" class="form-control form-control-sm bg-dark text-light border-secondary" v-model.number="p.precio" /></td>
              <td class="text-center"><input type="checkbox" class="form-check-input" v-model="p.activo" /></td>
              <td class="text-center"><input type="checkbox" class="form-check-input" v-model="p.esTendencia" /></td>
              <td><button class="btn btn-light btn-sm" @click="guardar(p)">Guardar</button></td>
            </tr>
          </tbody>
        </table>
      </div>
      <p class="text-secondary small">Por ahora se pueden editar precio, estado y tendencia. La edición completa (colores, tallas e imágenes) se agregará después.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

const productos = ref([]);
const cargando = ref(true);
const busqueda = ref('');

const filtrados = computed(() => {
  const q = busqueda.value.trim().toLowerCase();
  return q ? productos.value.filter((p) => p.nombre.toLowerCase().includes(q)) : productos.value;
});

onMounted(async () => {
  try {
    productos.value = (await axios.get('http://localhost:8080/api/productos')).data;
  } catch (e) {
    console.error('Error al cargar productos:', e);
  } finally {
    cargando.value = false;
  }
});

async function guardar(p) {
  if (!(p.precio > 0)) {
    alert('El precio debe ser mayor a 0.');
    return;
  }
  try {
    await axios.put(`http://localhost:8080/api/productos/${p.idProducto}`, p);
    alert(`"${p.nombre}" se actualizó correctamente.`);
  } catch (e) {
    alert('No se pudo actualizar el producto.');
  }
}
</script>
