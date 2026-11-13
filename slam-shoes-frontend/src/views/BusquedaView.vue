<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 container-fluid px-4 my-4">
      <h2 class="text-center fw-bold mb-1">Resultados de la búsqueda</h2>
      <p class="text-center text-secondary mb-4">
        <template v-if="consulta">“{{ consulta }}” · {{ resultados.length }} {{ resultados.length === 1 ? 'resultado' : 'resultados' }}</template>
        <template v-else>Escribe el nombre, la marca o el modelo en el buscador del encabezado.</template>
      </p>

      <div v-if="cargando" class="text-center text-muted py-5"><p>Buscando...</p></div>
      <CatalogoFiltrable
        v-else-if="consulta"
        :productos="resultados"
        :buscador="false"
        vacio="No encontramos tenis con ese nombre, marca o modelo."
      />
    </div>

    <footer class="border-top border-secondary bg-dark pt-4 pb-2 mt-5 w-100">
      <div class="container-fluid text-center px-4">
        <p class="mb-0 text-muted">[Texto xd sit seven]</p>
      </div>
    </footer>

    <CarritoComponent />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
import CatalogoFiltrable from '@/components/CatalogoFiltrable.vue';
import { coincideTexto } from '@/utils/busqueda.js';

const route = useRoute();
const productos = ref([]);
const cargando = ref(true);

const consulta = computed(() => (route.query.q ? String(route.query.q).trim() : ''));
// El filtrado se recalcula al cambiar ?q= sin recargar la lista
const resultados = computed(() =>
  productos.value.filter((p) => p.activo !== false && coincideTexto(p, consulta.value))
);

onMounted(async () => {
  try {
    productos.value = (await axios.get('http://localhost:8080/api/productos')).data;
  } catch (error) {
    console.error('Error al buscar productos:', error);
  } finally {
    cargando.value = false;
  }
});
</script>
