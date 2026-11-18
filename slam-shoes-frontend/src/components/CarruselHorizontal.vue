<template>
  <!-- Carrusel horizontal (estilo Way of Wade): flechas cuadradas naranjas, se desliza con el dedo, el trackpad o las flechas.
       Cada hijo del slot debe ocupar una sola "casilla"; su ancho lo fija la prop anchoItem. -->
  <div class="carrusel position-relative">
    <button v-show="puedeIzquierda" type="button" class="flecha izquierda" aria-label="Anterior" @click="mover(-1)">
      <i class="bi bi-chevron-left"></i>
    </button>

    <div ref="pista" class="pista" :style="{ '--ancho-item': anchoItem }" @scroll.passive="actualizar">
      <slot />
    </div>

    <button v-show="puedeDerecha" type="button" class="flecha derecha" aria-label="Siguiente" @click="mover(1)">
      <i class="bi bi-chevron-right"></i>
    </button>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from 'vue';

defineProps({
  // Ancho de cada casilla (CSS), por ejemplo "240px"
  anchoItem: { type: String, default: '240px' }
});

const pista = ref(null);
const puedeIzquierda = ref(false);
const puedeDerecha = ref(false);
let observador = null;

function actualizar() {
  const el = pista.value;
  if (!el) return;
  puedeIzquierda.value = el.scrollLeft > 4;
  puedeDerecha.value = el.scrollLeft + el.clientWidth < el.scrollWidth - 4;
}

function mover(direccion) {
  const el = pista.value;
  if (!el) return;
  el.scrollBy({ left: direccion * el.clientWidth * 0.85, behavior: 'smooth' });
}

onMounted(() => {
  actualizar();
  // Los productos llegan después de montar: se vuelven a medir las flechas cuando cambia el contenido
  observador = new MutationObserver(actualizar);
  observador.observe(pista.value, { childList: true, subtree: true });
  window.addEventListener('resize', actualizar);
});

onBeforeUnmount(() => {
  if (observador) observador.disconnect();
  window.removeEventListener('resize', actualizar);
});
</script>

<style scoped>
.pista {
  display: flex;
  gap: 1rem;
  overflow-x: auto;
  scroll-snap-type: x proximity;
  scrollbar-width: none;
  padding-bottom: 4px;
}
.pista::-webkit-scrollbar { display: none; }
.pista > :deep(*) {
  flex: 0 0 var(--ancho-item);
  max-width: var(--ancho-item);
  scroll-snap-align: start;
}

.flecha {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  z-index: 5;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-acento);
  background: rgba(0, 0, 0, 0.55);
  border: 1px solid var(--color-acento);
  cursor: pointer;
  transition: background-color 0.2s ease, color 0.2s ease;
}
.flecha:hover { background: var(--color-acento); color: #000; }
.izquierda { left: 0; }
.derecha { right: 0; }
</style>
