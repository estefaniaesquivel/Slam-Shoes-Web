<template>
  <!-- Barra lateral de filtros (solo tallas y colores), al estilo de la referencia -->
  <aside class="panel-filtros">
    <div class="d-flex justify-content-between align-items-center mb-3">
      <h6 class="mb-0 text-uppercase fw-bold">Filtros</h6>
      <button v-if="hayFiltros" type="button" class="btn btn-link btn-sm text-secondary p-0" @click="$emit('limpiar')">Limpiar</button>
    </div>

    <h6 class="text-light mb-2">Tallas (cm)</h6>
    <div class="tallas">
      <button
        v-for="t in TALLAS" :key="t" type="button" class="talla"
        :class="{ activa: tallas.includes(t) }" @click="alternar('tallas', t)"
      >{{ t }}</button>
    </div>

    <h6 class="text-light mt-4 mb-2">Colores</h6>
    <div class="colores">
      <button
        v-for="c in COLORES" :key="c.nombre" type="button" class="color"
        :class="{ activo: colores.includes(c.nombre) }"
        :style="{ background: c.fondo }" :data-nombre="c.nombre" :aria-label="c.nombre"
        @click="alternar('colores', c.nombre)"
      >
        <i v-if="colores.includes(c.nombre)" class="bi bi-check-lg" :class="c.claro ? 'text-dark' : 'text-white'"></i>
      </button>
    </div>
  </aside>
</template>

<script setup>
import { computed } from 'vue';
import { COLORES, TALLAS } from '@/utils/paleta.js';

const props = defineProps({
  tallas: { type: Array, default: () => [] },
  colores: { type: Array, default: () => [] }
});
const emit = defineEmits(['update:tallas', 'update:colores', 'limpiar']);

const hayFiltros = computed(() => props.tallas.length > 0 || props.colores.length > 0);

function alternar(campo, valor) {
  const actual = props[campo];
  emit(`update:${campo}`, actual.includes(valor) ? actual.filter((v) => v !== valor) : [...actual, valor]);
}
</script>

<style scoped>
.panel-filtros { background: #1a1a1a; border: 1px solid #333; border-radius: 12px; padding: 1.25rem; }
.tallas { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; }
.talla {
  background: #000; color: #fff; border: 1px solid #555; border-radius: 6px;
  padding: 6px 0; font-size: 0.8rem; cursor: pointer;
}
.talla:hover { border-color: #fff; }
.talla.activa { background: #fff; color: #000; border-color: #fff; font-weight: 700; }

.colores { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.color {
  position: relative; aspect-ratio: 1 / 1; border-radius: 8px; border: 1px solid rgba(255, 255, 255, 0.8);
  display: flex; align-items: center; justify-content: center; cursor: pointer; padding: 0;
}
.color.activo { outline: 3px solid #fff; outline-offset: 2px; }
.color::after {
  content: attr(data-nombre); position: absolute; bottom: 110%; left: 50%; transform: translateX(-50%);
  background: #fff; color: #000; font-size: 0.7rem; padding: 2px 6px; border-radius: 6px;
  white-space: nowrap; opacity: 0; pointer-events: none; transition: opacity 0.15s ease; z-index: 5;
}
.color:hover::after { opacity: 1; }
</style>
