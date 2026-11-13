<template>
  <!-- Sección del inicio: ofertas, novedades o tendencias (datos reales de la BD). Se oculta si no hay nada. -->
  <div class="container-fluid px-4 my-4" v-if="items.length > 0">
    <div class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
      <router-link v-if="ruta" :to="ruta" class="text-decoration-none text-light">
        <h3 class="fw-bold mb-0 text-uppercase">{{ titulo }} <i class="bi bi-chevron-right fs-6"></i></h3>
      </router-link>
      <h3 v-else class="fw-bold mb-0 text-uppercase">{{ titulo }}</h3>

      <router-link v-if="ruta" :to="ruta" class="btn btn-outline-light btn-sm">Ver todas</router-link>
    </div>

    <div class="row g-4">
      <div class="col-6 col-md-3" v-for="item in items" :key="item.idProducto">
        <ProductoCardSimple
          :id-producto="item.idProducto"
          :marca="item.marca"
          :nombre="item.nombre"
          :imagen-url="item.imagenUrl"
          :precio="item.precio"
          :subtitulo="item.subtitulo"
          @abrir="irADetalle(item.idProducto)"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import ProductoCardSimple from '@/components/ProductoCardSimple.vue';

const API = 'http://localhost:8080/api';
const MAXIMO = 4;

export default {
  name: 'SeccionProductos',
  components: { ProductoCardSimple },
  props: {
    // 'ofertas' | 'novedades' | 'tendencias'
    tipo: { type: String, required: true }
  },
  setup(props) {
    const router = useRouter();
    const items = ref([]);
    
    const titulo = computed(() => ({ ofertas: 'Ofertas', novedades: 'Novedades', tendencias: 'Tendencias' }[props.tipo]));
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
          const res = await axios.get(`${API}/productos`);
          items.value = res.data
            .filter((p) => p.esTendencia && p.activo !== false)
            .slice(0, MAXIMO)
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

    const irADetalle = (id) => router.push(`/producto/${id}`);

    onMounted(cargar);

    return { items, titulo, ruta, irADetalle };
  }
};
</script>
