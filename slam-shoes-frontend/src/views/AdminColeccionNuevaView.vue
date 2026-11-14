<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 900px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
        <h2 class="mb-0"><i class="bi bi-plus-circle me-2"></i>Agregar colección</h2>
        <router-link to="/admin/colecciones" class="btn btn-outline-light btn-sm">Volver</router-link>
      </div>

      <ColeccionFormulario :enviando="enviando" texto-boton="Subir colección al catálogo" @enviar="crear" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import ColeccionFormulario from '@/components/ColeccionFormulario.vue';
import { limpiarCatalogos } from '@/utils/catalogos.js';

const router = useRouter();
const enviando = ref(false);

async function crear(formulario) {
  enviando.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/admin/colecciones', formulario);
    limpiarCatalogos();
    alert(`¡Colección "${res.data.nombre}" subida al catálogo!`);
    router.push('/admin/colecciones');
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo subir la colección. Revisa que el backend esté activo.');
  } finally {
    enviando.value = false;
  }
}
</script>
