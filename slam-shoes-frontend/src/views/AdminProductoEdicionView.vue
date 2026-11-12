<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 900px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
        <h2 class="mb-0"><i class="bi bi-pencil-square me-2"></i>Editar producto <span class="text-secondary" v-if="producto">#{{ producto.idProducto }}</span></h2>
        <router-link to="/admin/productos/editar" class="btn btn-outline-light btn-sm">Volver al listado</router-link>
      </div>

      <div v-if="cargando" class="text-muted">Cargando producto...</div>
      <div v-else-if="!producto" class="text-center py-5">
        <h5 class="text-danger mb-3">No se encontró el producto.</h5>
        <router-link to="/admin/productos/editar" class="btn btn-outline-light">Volver al listado</router-link>
      </div>

      <!-- Mismo formulario que "Agregar producto", con todo lo guardado ya cargado -->
      <ProductoFormulario
        v-else
        modo="editar"
        :inicial="producto"
        texto-boton="Confirmar la edición del producto"
        :enviando="enviando"
        @enviar="guardar"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useRoute, useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import ProductoFormulario from '@/components/ProductoFormulario.vue';

const API = 'http://localhost:8080/api/admin/productos';
const route = useRoute();
const router = useRouter();

const producto = ref(null);
const cargando = ref(true);
const enviando = ref(false);

onMounted(async () => {
  try {
    producto.value = (await axios.get(`${API}/${route.params.id}`)).data;
  } catch (e) {
    producto.value = null;
  } finally {
    cargando.value = false;
  }
});

async function guardar(formulario) {
  enviando.value = true;
  try {
    await axios.put(`${API}/${route.params.id}`, formulario);
    alert('El producto se actualizó correctamente.');
    router.push('/admin/productos/editar');
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo actualizar el producto. Revisa que el backend esté activo.');
  } finally {
    enviando.value = false;
  }
}
</script>