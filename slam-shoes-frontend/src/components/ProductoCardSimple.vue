<template>
  <!-- Tarjeta cuadrada 1:1 reutilizable (secciones del inicio y paneles de ofertas/novedades) -->
  <div class="border border-secondary bg-black p-3 rounded-3 d-flex flex-column position-relative tarjeta" @click="$emit('abrir')">
    <BotonFavorito v-if="idProducto" :producto="{ idProducto, nombre, imagenUrl, precio, marca }" />
    <span v-if="descuento" class="badge bg-danger position-absolute top-0 start-0 m-2" style="z-index: 2;">-{{ descuentoTexto }}%</span>

    <div class="bg-white w-100 rounded-3 overflow-hidden square-box mb-3">
      <img :src="imagenUrl || '/logo-slamshoes-blanco.png'" class="w-100 h-100 object-fit-cover" :alt="nombre" />
    </div>

    <h6 class="text-white text-truncate mb-1" :title="nombre">{{ nombre }}</h6>
    <p v-if="subtitulo" class="text-secondary small mb-1">{{ subtitulo }}</p>

    <p class="mb-0 fw-bold">
      <template v-if="precioFinal != null && precioFinal < precio">
        <span class="text-success">${{ precioFinal }} MXN</span>
        <span class="text-secondary text-decoration-line-through small ms-2">${{ precio }}</span>
      </template>
      <span v-else class="text-success">${{ precio }} MXN</span>
    </p>
  </div>
</template>

<script>
import { computed } from 'vue';
import BotonFavorito from '@/components/BotonFavorito.vue';

export default {
  name: 'ProductoCardSimple',
  components: { BotonFavorito },
  props: {
    idProducto: { type: Number, default: null },
    marca: { type: String, default: '' },
    nombre: { type: String, default: '' },
    imagenUrl: { type: String, default: '' },
    precio: { type: Number, default: 0 },
    precioFinal: { type: Number, default: null },
    descuento: { type: Number, default: null },
    subtitulo: { type: String, default: '' }
  },
  emits: ['abrir'],
  setup(props) {
    const descuentoTexto = computed(() => Number(props.descuento).toString());
    return { descuentoTexto };
  }
};
</script>

<style scoped>
.tarjeta { cursor: pointer; transition: transform 0.2s ease; }
.tarjeta:hover { transform: translateY(-4px); }
.square-box { aspect-ratio: 1 / 1; }
</style>
