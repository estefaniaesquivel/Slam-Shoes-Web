<template>
  <!-- Marcas (al final de la página, como SoleRetriever): recuadros pequeños con el logo y el nombre debajo -->
  <div class="container-fluid px-4 my-5" v-if="marcas.length > 0">
    <h3 class="seccion-titulo">Encuentra tenis por marca</h3>
    <div class="marcas-grid">
      <router-link v-for="marca in marcas" :key="marca.idMarca" :to="`/marca/${marca.idMarca}`" class="marca-item" :title="marca.nombre">
        <div class="marca-cuadro">
          <img v-if="marca.logoUrl" :src="marca.logoUrl" :alt="marca.nombre" />
          <span v-else class="marca-sin-logo">{{ marca.nombre }}</span>
        </div>
        <span class="marca-nombre">{{ marca.nombre }}</span>
      </router-link>
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
.marcas-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(130px, 1fr)); gap: 1rem 0.9rem; }
.marca-item { text-decoration: none; color: #fff; }
.marca-cuadro {
  aspect-ratio: 2 / 1; background-color: #000; border-radius: 3px; overflow: hidden;
  display: flex; align-items: center; justify-content: center; transition: filter 0.2s ease, transform 0.2s ease;
}
.marca-item:hover .marca-cuadro { filter: brightness(1.2); transform: translateY(-2px); }
.marca-cuadro img { width: 100%; height: 100%; object-fit: contain; display: block; }
.marca-sin-logo { font-weight: 700; padding: 0 0.5rem; text-align: center; font-size: 0.95rem; }
.marca-nombre { display: block; margin-top: 0.45rem; font-size: 0.85rem; font-weight: 500; text-align: left; }
</style>
