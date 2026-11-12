<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal modo-admin />

    <div class="flex-grow-1 container my-4" style="max-width: 900px;">
      <div class="d-flex justify-content-between align-items-center flex-wrap gap-2 mb-4">
        <h2 class="mb-0"><i class="bi bi-plus-circle me-2"></i>Agregar producto</h2>
        <router-link to="/admin/productos" class="btn btn-outline-light btn-sm">Volver</router-link>
      </div>

      <ProductoFormulario modo="crear" texto-boton="Añadir producto al catálogo" :enviando="enviando" @enviar="crear" />
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import axios from 'axios';
import { useRouter } from 'vue-router';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import ProductoFormulario from '@/components/ProductoFormulario.vue';

const router = useRouter();
const enviando = ref(false);

async function crear(formulario) {
  enviando.value = true;
  try {
    const res = await axios.post('http://localhost:8080/api/admin/productos', formulario);
    alert(`¡Producto añadido al catálogo! (id ${res.data.idProducto})`);
    router.push('/admin/productos');
  } catch (e) {
    alert(e.response?.data?.error || 'No se pudo añadir el producto. Revisa que el backend esté activo.');
  } finally {
    enviando.value = false;
  }
}
</script>
