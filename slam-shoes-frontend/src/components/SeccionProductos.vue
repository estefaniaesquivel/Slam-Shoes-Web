<template>
  <!-- Sección del inicio: ofertas, novedades o tendencias (datos reales de la BD). Se oculta si no hay nada.
       Las tendencias son un carrusel deslizable; ofertas y novedades, una fila corta. -->
  <div class="container-fluid px-4 my-4" v-if="items.length > 0">
    <div class="d-flex justify-content-between align-items-end mb-1 flex-wrap gap-2">
      <router-link v-if="ruta" :to="ruta" class="text-decoration-none">
        <h3 class="seccion-titulo mb-3">{{ titulo }}</h3>
      </router-link>
      <h3 v-else class="seccion-titulo mb-3">{{ titulo }}</h3>

      <router-link v-if="ruta" :to="ruta" class="btn btn-outline-light btn-sm mb-3">Ver todas</router-link>
    </div>

    <CarruselHorizontal v-if="esCarrusel" ancho-item="230px">
      <div v-for="item in items" :key="item.idProducto">
        <ProductoCardCatalogo :producto="item" :subtitulo="item.subtitulo" />
      </div>
    </CarruselHorizontal>

    <div v-else class="row g-4">
      <div class="col-6 col-md-3" v-for="item in items" :key="item.idProducto">
        <ProductoCardCatalogo :producto="item" :subtitulo="item.subtitulo" />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import axios from 'axios';
import ProductoCardCatalogo from '@/components/ProductoCardCatalogo.vue';
import CarruselHorizontal from '@/components/CarruselHorizontal.vue';
import { obtenerProductos } from '@/utils/catalogos.js';

const API = 'http://localhost:8080/api';
const MAXIMO = 4;

export default {
  name: 'SeccionProductos',
  components: { ProductoCardCatalogo, CarruselHorizontal },
  props: {
    // 'ofertas' | 'novedades' | 'tendencias'
    tipo: { type: String, required: true }
  },
  setup(props) {
    const items = ref([]);
    
    const titulo = computed(() => ({ ofertas: 'Ofertas', novedades: 'Novedades', tendencias: 'Tendencias' }[props.tipo]));
    const esCarrusel = computed(() => props.tipo === 'tendencias');
    const ruta = computed(() => ({ ofertas: '/ofertas', novedades: '/novedades' }[props.tipo] || null));

    const cargar = async () => {
      try {
        if (props.tipo === 'ofertas') {
          const res = await axios.get(`${API}/ofertas/productos`);
          items.value = res.data.slice(0, MAXIMO).map((o) => aItem(o.producto));
        } else if (props.tipo === 'novedades') {
          const res = await axios.get(`${API}/novedades/productos`);
          items.value = res.data.slice(0, MAXIMO).map(aItem);
        } else {
          const lista = await obtenerProductos();
          // Carrusel: se muestran todas las tendencias (el usuario las desliza)
          items.value = lista
            .filter((p) => p.esTendencia && p.activo !== false)
            .map(aItem);
        }
      } catch (error) {
        console.error(`Error al cargar la sección ${props.tipo}:`, error);
      }
    };

    const aItem = (p) => ({
      idProducto: p.idProducto,
      nombre: p.nombre,
      imagenUrl: p.imagenUrl,
      precio: p.precio,
      marca: p.marca || '',
      subtitulo: p.marca || ''
    });

    onMounted(cargar);

    return { items, titulo, ruta, esCarrusel };
  }
};
</script>
