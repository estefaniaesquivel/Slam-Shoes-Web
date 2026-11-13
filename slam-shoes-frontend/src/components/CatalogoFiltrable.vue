<template>
  <!-- Rejilla de productos con buscador y barra de filtros (tallas y colores) desplegable a la izquierda -->
  <div>
    <div v-if="buscador || filtros" class="d-flex flex-wrap align-items-center gap-2 mb-4">
      <button v-if="filtros" type="button" class="btn btn-outline-light" @click="abierto = !abierto">
        <i class="bi bi-sliders me-1"></i>Filtros
        <span v-if="totalFiltros" class="badge bg-danger ms-1">{{ totalFiltros }}</span>
      </button>

      <div v-if="buscador" class="input-group buscador">
        <span class="input-group-text bg-black text-secondary border-secondary"><i class="bi bi-search"></i></span>
        <input
          v-model="texto" type="search" class="form-control bg-black text-light border-secondary"
          placeholder="Buscar por nombre, marca o modelo..." aria-label="Buscar en esta página"
        />
      </div>

      <span class="text-secondary small ms-auto">{{ filtrados.length }} de {{ productos.length }} productos</span>
    </div>

    <div class="d-flex flex-column flex-lg-row gap-4 align-items-lg-start">
      <div v-if="filtros && abierto" class="columna-filtros">
        <PanelFiltros v-model:tallas="tallasSel" v-model:colores="coloresSel" @limpiar="limpiar" />
      </div>

      <div class="flex-grow-1 w-100">
        <div v-if="filtrados.length === 0" class="text-center text-muted py-5">
          <p class="mb-2">{{ productos.length === 0 ? vacio : 'Ningún producto coincide con lo que buscas.' }}</p>
          <button v-if="productos.length > 0" type="button" class="btn btn-outline-light btn-sm" @click="limpiar(true)">Quitar búsqueda y filtros</button>
        </div>

        <div v-else class="row g-4">
          <div v-for="p in filtrados" :key="p.idProducto" class="col-6" :class="abierto && filtros ? 'col-md-4 col-xl-3' : 'col-md-3'">
            <ProductoCardCatalogo :producto="p" :subtitulo="p.subtitulo || ''" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue';
import PanelFiltros from '@/components/PanelFiltros.vue';
import ProductoCardCatalogo from '@/components/ProductoCardCatalogo.vue';
import { cargarVariantes, variantesDe, variantesCargadas } from '@/utils/variantes.js';
import { coincideTexto } from '@/utils/busqueda.js';

const props = defineProps({
  productos: { type: Array, default: () => [] },
  buscador: { type: Boolean, default: true },
  filtros: { type: Boolean, default: true },
  vacio: { type: String, default: 'Aún no hay tenis registrados aquí.' }
});

const texto = ref('');
const tallasSel = ref([]);
const coloresSel = ref([]);
const abierto = ref(false);

cargarVariantes();

const totalFiltros = computed(() => tallasSel.value.length + coloresSel.value.length);

const coincideVariante = (p) => {
  if (totalFiltros.value === 0) return true;
  // Debe existir una variante con stock que cumpla la talla Y el color elegidos
  return variantesDe(p.idProducto).some((v) =>
    (tallasSel.value.length === 0 || tallasSel.value.some((t) => Number(t) === Number(v.talla))) &&
    (coloresSel.value.length === 0 || coloresSel.value.includes(v.color))
  );
};

const filtrados = computed(() => {
  // Mientras llegan las variantes no se descarta nada por talla/color
  const aplicarVariantes = variantesCargadas.value;
  return props.productos.filter((p) =>
    coincideTexto(p, texto.value) && (!aplicarVariantes || coincideVariante(p))
  );
});

function limpiar(tambienTexto = false) {
  tallasSel.value = [];
  coloresSel.value = [];
  if (tambienTexto === true) texto.value = '';
}
</script>

<style scoped>
.buscador { max-width: 420px; }
.columna-filtros { flex: 0 0 270px; width: 270px; max-width: 100%; }
@media (max-width: 991.98px) { .columna-filtros { width: 100%; flex-basis: auto; } }
</style>
