<template>
  <!-- Marcas: cuadros 1:1 con el logo (2000x2000) a todo el recuadro, sin nombre; clic → catálogo de la marca -->
  <div class="container-fluid px-4 my-5" v-if="marcas.length > 0">
    <h3 class="fw-bold mb-4">Encuentra tenis por marca</h3>
    <div class="row g-3">
      <div class="col-6 col-md-3" v-for="marca in marcas" :key="marca.idMarca">
        <router-link :to="`/marca/${marca.idMarca}`" class="marca-cuadro d-block rounded overflow-hidden" :title="marca.nombre">
          <img v-if="marca.logoUrl" :src="marca.logoUrl" class="d-block w-100" :alt="marca.nombre" />
          <div v-else class="marca-sin-logo d-flex align-items-center justify-content-center text-white fw-bold fs-4">{{ marca.nombre }}</div>
        </router-link>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { obtenerMarcas } from '@/utils/catalogos.js';

export default {
  name: 'MarcasGrid',
  setup() {
    const marcas = ref([]);
    onMounted(async () => {
      try {
        marcas.value = await obtenerMarcas();
      } catch (error) {
        console.error('Error al cargar las marcas:', error);
      }
    });
    return { marcas };
  }
};
</script>

<style scoped>
.marca-cuadro { aspect-ratio: 1 / 1; transition: transform 0.2s ease, filter 0.2s ease; }
.marca-cuadro:hover { transform: translateY(-4px); filter: brightness(1.15); }
.marca-cuadro img { aspect-ratio: 1 / 1; object-fit: cover; }
.marca-sin-logo { aspect-ratio: 1 / 1; background-color: #2b2b3b; }
</style>
