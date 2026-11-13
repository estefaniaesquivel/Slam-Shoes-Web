<template>
  <!-- Precio con descuento (si la oferta ya está vigente) o precio normal -->
  <p class="mb-0 fw-bold">
    <template v-if="oferta && oferta.vigente">
      <span class="text-success">${{ oferta.precioConDescuento }} MXN</span>
      <span class="text-secondary text-decoration-line-through small ms-2">${{ precio }}</span>
    </template>
    <span v-else class="text-success">${{ precio }} MXN</span>
  </p>
  <p v-if="oferta && !oferta.vigente" class="small text-warning mb-0">
    Oferta desde el {{ fechaCorta(oferta.fechaInicio) }}: ${{ oferta.precioConDescuento }}
  </p>
</template>

<script setup>
import { computed } from 'vue';
import { cargarOfertas, ofertaDe, fechaCorta } from '@/utils/ofertas.js';

const props = defineProps({
  idProducto: { type: Number, default: null },
  precio: { type: Number, default: 0 }
});

cargarOfertas();
const oferta = computed(() => (props.idProducto ? ofertaDe(props.idProducto) : null));
</script>
