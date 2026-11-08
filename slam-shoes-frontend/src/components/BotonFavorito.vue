<template>
  <!-- Corazón de favoritos: relleno rojo si el producto ya está en favoritos del cliente. Va dentro de un contenedor position-relative. -->
  <button
    v-if="!admin"
    class="btn btn-sm btn-dark position-absolute top-0 end-0 m-2 rounded-circle shadow boton-favorito"
    :title="activo ? 'Quitar de favoritos' : 'Añadir a favoritos'"
    @click.stop="alternar"
  >
    <i :class="activo ? 'bi bi-heart-fill text-danger' : 'bi bi-heart text-white'"></i>
  </button>
</template>

<script>
import { ref, onMounted, onBeforeUnmount } from 'vue';
import { alternarFavorito, esFavorito } from '@/utils/favoritos.js';
import { esAdmin } from '@/utils/auth.js';

export default {
  name: 'BotonFavorito',
  props: {
    // { idProducto, nombre, imagenUrl, precio, marca }
    producto: { type: Object, required: true }
  },
  setup(props) {
    const admin = esAdmin();
    const activo = ref(esFavorito(props.producto.idProducto));

    const refrescar = () => {
      activo.value = esFavorito(props.producto.idProducto);
    };

    const alternar = async () => {
      await alternarFavorito({
        idProducto: props.producto.idProducto,
        nombre: props.producto.nombre,
        imagenUrl: props.producto.imagenUrl,
        precio: props.producto.precio,
        marca: props.producto.marca
      });
      refrescar();
    };

    onMounted(() => window.addEventListener('storage', refrescar));
    onBeforeUnmount(() => window.removeEventListener('storage', refrescar));

    return { admin, activo, alternar };
  }
};
</script>

<style scoped>
.boton-favorito { z-index: 3; }
</style>
