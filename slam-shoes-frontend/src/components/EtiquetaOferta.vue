<template>
  <!-- Etiqueta roja de descuento (esquina superior izquierda de la tarjeta). Se oculta si el producto no tiene oferta. -->
  <span v-if="oferta" class="badge bg-danger position-absolute top-0 start-0 m-2 etiqueta" style="z-index: 2;">
    -{{ porcentajeTexto(oferta) }}%<template v-if="!oferta.vigente"> · desde {{ fechaCorta(oferta.fechaInicio) }}</template>
  </span>
</template>

<script setup>
import { computed } from 'vue';
import { cargarOfertas, ofertaDe, porcentajeTexto, fechaCorta } from '@/utils/ofertas.js';

const props = defineProps({ idProducto: { type: Number, default: null } });

cargarOfertas();
const oferta = computed(() => (props.idProducto ? ofertaDe(props.idProducto) : null));
</script>

<style scoped>
.etiqueta { font-size: 0.8rem; }
</style>
