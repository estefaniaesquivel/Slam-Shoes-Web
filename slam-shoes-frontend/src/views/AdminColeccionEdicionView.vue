<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 900px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
        <h2 class="mb-0"><i class="bi bi-pencil-square me-2"></i>Editar colección</h2>
        <router-link to="/admin/colecciones/editar" class="btn btn-outline-light btn-sm">Volver</router-link>
      </div>

      <div v-if="cargando" class="text-muted">Cargando...</div>
      <div v-else-if="!inicial" class="text-center text-muted py-5">No se encontró la colección.</div>
      <ColeccionFormulario v-else :inicial="inicial" :enviando="enviando" texto-boton="Subir colección al catálogo" @enviar="actualizar" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRoute, useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import ColeccionFormulario from '@/components/ColeccionFormulario.vue';
import { limpiarCatalogos } from '@/utils/catalogos.js';

const route = useRoute();
const router = useRouter();
const API = 'http://localhost:8080/api/admin/colecciones';

const inicial = ref(null);
const cargando = ref(true);
const enviando = ref(false);

onMounted(async () => {
  try {
    const d = (await axios.get(`${API}/${route.params.id}`)).data;
    inicial.value = { ...d, idColeccion: d.idColeccion };
  } catch (e) {
    inicial.value = null;
  } finally {
    cargando.value = false;
  }
});

async function actualizar(formulario) {
  enviando.value = true;
  try {
    const res = await axios.put(`${API}/${route.params.id}`, formulario);
    limpiarCatalogos();
    alert(`¡Cambios de "${res.data.nombre}" guardados en el catálogo!`);
    router.push('/admin/colecciones/editar');
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo guardar la colección. Revisa que el backend esté activo.');
  } finally {
    enviando.value = false;
  }
}
</script>
