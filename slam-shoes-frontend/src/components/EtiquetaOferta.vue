<template>
  <!-- Etiqueta roja de descuento (esquina superior izquierda de la tarjeta) con las fechas de la oferta.
       Se muestra en todos los tenis que tengan oferta (vigente o programada); se oculta si no tiene. -->
  <span v-if="oferta" class="badge bg-danger position-absolute top-0 start-0 m-2 etiqueta text-start" style="z-index: 2;">
    <span class="d-block fw-bold">-{{ porcentajeTexto(oferta) }}%</span>
    <span class="d-block fecha">{{ rangoFechas }}</span>
  </span>
</template>

<script setup>
import { computed } from 'vue';
import { cargarOfertas, ofertaDe, porcentajeTexto, fechaCorta } from '@/utils/ofertas.js';

const props = defineProps({ idProducto: { type: Number, default: null } });

cargarOfertas();
const oferta = computed(() => (props.idProducto ? ofertaDe(props.idProducto) : null));

// Ej.: "13 nov - 20 nov" (ya vigente o por empezar, siempre se ve el periodo completo)
const rangoFechas = computed(() => {
  const o = oferta.value;
  if (!o) return '';
  const inicio = o.fechaInicio ? fechaCorta(o.fechaInicio) : '';
  const fin = o.fechaFin ? fechaCorta(o.fechaFin) : '';
  if (inicio && fin) return `${inicio} - ${fin}`;
  return fin ? `hasta ${fin}` : inicio ? `desde ${inicio}` : '';
});
</script>

<style scoped>
.etiqueta { font-size: 0.8rem; line-height: 1.2; }
.fecha { font-size: 0.68rem; font-weight: 500; opacity: 0.95; }
</style>
