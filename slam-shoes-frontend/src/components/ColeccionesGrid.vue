<template>
  <!-- Colecciones: imagen 1:1 completa con la descripción como pie de imagen; clic → vista de la colección -->
  <div class="container-fluid py-5 my-4 border-top border-bottom border-secondary px-4">
    <div class="mb-4">
      <h3 class="text-center fw-bold">COLECCIONES</h3>
    </div>
    <div class="row g-4" v-if="colecciones.length > 0">
      <div class="col-md-3" v-for="col in colecciones" :key="col.idColeccion">
        <figure class="coleccion-card m-0 border border-secondary bg-black rounded-3 overflow-hidden" @click="irAColeccion(col.idColeccion)">
          <div class="square-box bg-white">
            <img v-if="col.imagenUrl" :src="col.imagenUrl" class="coleccion-img" :alt="col.nombre">
          </div>
          <figcaption class="p-3 text-start">
            <h5 class="text-white mb-1">{{ col.nombre }}</h5>
            <p class="text-secondary small mb-0">{{ col.descripcion }}</p>
          </figcaption>
        </figure>
      </div>
    </div>
    <div v-else class="text-center text-muted py-3">
      <p>{{ cargando ? 'Cargando colecciones...' : 'Aún no hay colecciones registradas.' }}</p>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';

export default {
  name: 'ColeccionesGrid',
  setup() {
    const router = useRouter();
    const colecciones = ref([]);
    const cargando = ref(true);

    onMounted(async () => {
      try {
        colecciones.value = (await axios.get('http://localhost:8080/api/colecciones')).data;
      } catch (error) {
        console.error('Error al cargar las colecciones:', error);
      } finally {
        cargando.value = false;
      }
    });

    const irAColeccion = (id) => router.push(`/coleccion/${id}`);

    return { colecciones, cargando, irAColeccion };
  }
};
</script>

<style scoped>
.square-box { width: 100%; aspect-ratio: 1 / 1; }
.coleccion-card { cursor: pointer; transition: transform 0.3s ease; }
.coleccion-card:hover { transform: translateY(-5px); }
.coleccion-img { width: 100%; height: 100%; object-fit: contain; display: block; }
</style>
