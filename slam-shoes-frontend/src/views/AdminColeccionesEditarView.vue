<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container-fluid px-4 my-4">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-2">
        <h2 class="mb-0"><i class="bi bi-pencil-square me-2"></i>Editar colección</h2>
        <router-link to="/admin/colecciones" class="btn btn-outline-light btn-sm">Volver</router-link>
      </div>
      <p class="text-secondary small mb-4">Elige la colección que quieres editar.</p>

      <div v-if="cargando" class="text-muted">Cargando...</div>
      <div v-else-if="colecciones.length === 0" class="text-center text-muted py-5">Aún no hay colecciones registradas.</div>

      <div v-else class="row g-4">
        <div class="col-6 col-md-3" v-for="col in colecciones" :key="col.idColeccion">
          <figure class="coleccion-card m-0 border border-secondary bg-black rounded-3 overflow-hidden position-relative"
            @click="editar(col.idColeccion)">
            <span class="lapiz" title="Editar colección"><i class="bi bi-pencil-fill"></i></span>
            <span v-if="col.activa === false" class="badge bg-secondary position-absolute top-0 start-0 m-2" style="z-index: 2;">Inactiva</span>
            <div class="square-box bg-white">
              <img v-if="col.imagenUrl" :src="col.imagenUrl" class="coleccion-img" :alt="col.nombre" />
            </div>
            <figcaption class="p-3 text-start">
              <h5 class="text-white mb-1">{{ col.nombre }}</h5>
              <p class="text-secondary small mb-0">{{ col.descripcion }}</p>
            </figcaption>
          </figure>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

const router = useRouter();
const colecciones = ref([]);
const cargando = ref(true);

onMounted(async () => {
  try {
    colecciones.value = (await axios.get('http://localhost:8080/api/admin/colecciones')).data;
  } catch (e) {
    alert('No se pudieron cargar las colecciones. Revisa que el backend esté activo.');
  } finally {
    cargando.value = false;
  }
});

const editar = (id) => router.push(`/admin/colecciones/editar/${id}`);
</script>

<style scoped>
.coleccion-card { cursor: pointer; transition: transform 0.2s ease; }
.coleccion-card:hover { transform: translateY(-4px); }
.square-box { aspect-ratio: 1 / 1; }
.coleccion-img { width: 100%; height: 100%; object-fit: cover; }
.lapiz {
  position: absolute; top: 10px; right: 10px; z-index: 2;
  width: 38px; height: 38px; border-radius: 50%;
  background: rgba(0, 0, 0, 0.75); color: #fff; border: 1px solid #fff;
  display: flex; align-items: center; justify-content: center;
}
.coleccion-card:hover .lapiz { background: #fff; color: #000; }
</style>
