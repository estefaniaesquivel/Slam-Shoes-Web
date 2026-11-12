<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 1000px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-3">
        <h2 class="mb-0"><i class="bi bi-pencil-square me-2"></i>Editar productos</h2>
        <router-link to="/admin/productos" class="btn btn-outline-light btn-sm">Volver</router-link>
      </div>

      <input type="text" class="form-control bg-dark text-light border-secondary mb-3" style="max-width: 420px;"
        placeholder="Buscar por ID o nombre..." v-model="busqueda" />

      <div v-if="cargando" class="text-muted">Cargando...</div>
      <div v-else-if="filtrados.length === 0" class="text-center text-muted py-5">No se encontraron productos.</div>

      <div v-else class="table-responsive">
        <table class="table table-dark table-bordered align-middle">
          <thead>
            <tr>
              <th style="width: 70px;">ID</th>
              <th style="width: 90px;">Portada</th>
              <th>Nombre</th>
              <th>Marca</th>
              <th style="width: 150px;"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="p in filtrados" :key="p.idProducto">
              <td>{{ p.idProducto }}</td>
              <td>
                <img :src="p.imagenUrl || '/logo-slamshoes-blanco.png'" class="rounded bg-white" style="width: 64px; height: 64px; object-fit: cover;" :alt="p.nombre" />
              </td>
              <td>
                {{ p.nombre }}
                <span v-if="p.activo === false" class="badge bg-secondary ms-1">Inactivo</span>
              </td>
              <td>{{ p.marca }}</td>
              <td class="text-end text-nowrap">
                <button class="btn btn-outline-danger btn-sm me-2" @click="aEliminar = p" title="Eliminar producto">
                  <i class="bi bi-trash"></i>
                </button>
                <router-link :to="`/admin/productos/editar/${p.idProducto}`" class="btn btn-light btn-sm" title="Editar producto">
                  <i class="bi bi-pencil-square me-1"></i>Editar
                </router-link>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- Componente de verificación de eliminación -->
    <ConfirmarEliminacion :producto="aEliminar" :eliminando="eliminando" @cancelar="aEliminar = null" @confirmar="eliminar" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import ConfirmarEliminacion from '@/components/ConfirmarEliminacion.vue';

const productos = ref([]);
const cargando = ref(true);
const busqueda = ref('');
const aEliminar = ref(null);
const eliminando = ref(false);

const filtrados = computed(() => {
  const q = busqueda.value.trim().toLowerCase();
  if (!q) return productos.value;
  return productos.value.filter((p) => String(p.idProducto) === q || (p.nombre || '').toLowerCase().includes(q));
});

onMounted(async () => {
  try {
    const lista = (await axios.get('http://localhost:8080/api/productos')).data;
    productos.value = [...lista].sort((a, b) => a.idProducto - b.idProducto);
  } catch (e) {
    console.error('Error al cargar productos:', e);
  } finally {
    cargando.value = false;
  }
});

async function eliminar() {
  const p = aEliminar.value;
  eliminando.value = true;
  try {
    await axios.delete(`http://localhost:8080/api/admin/productos/${p.idProducto}`);
    productos.value = productos.value.filter((x) => x.idProducto !== p.idProducto);
    aEliminar.value = null;
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo eliminar el producto.');
    aEliminar.value = null;
  } finally {
    eliminando.value = false;
  }
}
</script>