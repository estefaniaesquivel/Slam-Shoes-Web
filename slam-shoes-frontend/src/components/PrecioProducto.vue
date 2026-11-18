<template>
  <!-- Precio. Todas las ofertas se ven igual: precio de oferta, precio normal tachado y la leyenda con sus fechas -->
  <p class="mb-0 fw-bold">
    <template v-if="oferta">
      <span class="text-success">${{ oferta.precioConDescuento }} MXN</span>
      <span class="text-secondary text-decoration-line-through small ms-2">${{ precio }}</span>
    </template>
    <span v-else class="text-success">${{ precio }} MXN</span>
  </p>
  <p v-if="oferta" class="small text-warning mb-0">{{ leyenda }}</p>
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

// "Oferta del 1 nov al 30 nov" (si todavía no empieza se agrega "· próximamente")
const leyenda = computed(() => {
  const o = oferta.value;
  if (!o) return '';
  const inicio = o.fechaInicio ? fechaCorta(o.fechaInicio) : '';
  const fin = o.fechaFin ? fechaCorta(o.fechaFin) : '';
  const periodo = inicio && fin ? `del ${inicio} al ${fin}` : fin ? `hasta el ${fin}` : inicio ? `desde el ${inicio}` : '';
  return `Oferta ${periodo}${o.vigente ? '' : ' · próximamente'}`.trim();
});
</script>
