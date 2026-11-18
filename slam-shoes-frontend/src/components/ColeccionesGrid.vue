<template>
  <!-- Colecciones: carrusel de tarjetas pequeñas (imagen 1:1 con el nombre encima); clic → vista de la colección -->
  <div class="container-fluid px-4 my-5">
    <h3 class="seccion-titulo">Colecciones</h3>

    <CarruselHorizontal v-if="colecciones.length > 0" ancho-item="230px">
      <figure v-for="col in colecciones" :key="col.idColeccion" class="coleccion-card m-0" @click="irAColeccion(col.idColeccion)">
        <div class="square-box">
          <img v-if="col.imagenUrl" :src="col.imagenUrl" class="coleccion-img" :alt="col.nombre">
          <div class="velo"></div>
          <figcaption class="nombre">{{ col.nombre }}</figcaption>
        </div>
        <p v-if="col.descripcion" class="descripcion text-secondary small mb-0">{{ col.descripcion }}</p>
      </figure>
    </CarruselHorizontal>

    <div v-else class="text-center text-muted py-3">
      <p>{{ cargando ? 'Cargando colecciones...' : 'Aún no hay colecciones registradas.' }}</p>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { obtenerColecciones } from '@/utils/catalogos.js';
import CarruselHorizontal from '@/components/CarruselHorizontal.vue';

export default {
  name: 'ColeccionesGrid',
  components: { CarruselHorizontal },
  setup() {
    const router = useRouter();
    const colecciones = ref([]);
    const cargando = ref(true);

    onMounted(async () => {
      try {
        colecciones.value = await obtenerColecciones();
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
.coleccion-card { cursor: pointer; }
.square-box { position: relative; width: 100%; aspect-ratio: 1 / 1; overflow: hidden; background: #111; }
.coleccion-img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform 0.6s ease; }
.coleccion-card:hover .coleccion-img { transform: scale(1.06); }
.velo { position: absolute; inset: 0; background: linear-gradient(to top, rgba(0, 0, 0, 0.65), rgba(0, 0, 0, 0) 55%); }
.nombre {
  position: absolute; left: 0; right: 0; bottom: 0; padding: 0.8rem 0.9rem;
  color: #fff; font-weight: 600; font-size: 1.05rem; text-shadow: 0 2px 8px rgba(0, 0, 0, 0.6);
}
.descripcion {
  margin-top: 0.5rem; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden;
}
</style>
