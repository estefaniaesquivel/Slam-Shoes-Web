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

    <!-- Accesos por nombre de oferta (BUEN FIN, etc.) -->
    <div v-if="tipo === 'ofertas' && nombresOferta.length > 0" class="d-flex flex-wrap gap-2 mb-3">
      <router-link v-for="n in nombresOferta" :key="n" :to="{ path: '/ofertas', query: { nombre: n } }" class="badge rounded-pill bg-danger text-decoration-none py-2 px-3">
        {{ n }}
      </router-link>
    </div>

    <div class="row g-4">
      <div class="col-6 col-md-3" v-for="item in items" :key="item.idProducto">
        <ProductoCardSimple
          :nombre="item.nombre"
          :imagen-url="item.imagenUrl"
          :precio="item.precio"
          :precio-final="item.precioFinal"
          :descuento="item.descuento"
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
    const nombresOferta = ref([]);

    const titulo = computed(() => ({ ofertas: 'Ofertas', novedades: 'Novedades', tendencias: 'Tendencias' }[props.tipo]));
    const ruta = computed(() => ({ ofertas: '/ofertas', novedades: '/novedades' }[props.tipo] || null));

    const cargar = async () => {
      try {
        if (props.tipo === 'ofertas') {
          const [res, resNombres] = await Promise.all([
            axios.get(`${API}/ofertas/productos`),
            axios.get(`${API}/ofertas/nombres`).catch(() => ({ data: [] }))
          ]);
          nombresOferta.value = resNombres.data;
          items.value = res.data.slice(0, MAXIMO).map((o) => ({
            idProducto: o.producto.idProducto,
            nombre: o.producto.nombre,
            imagenUrl: o.producto.imagenUrl,
            precio: o.producto.precio,
            precioFinal: o.precioConDescuento,
            descuento: o.porcentajeDescuento,
            subtitulo: o.nombre || ''
          }));
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
      precioFinal: null,
      descuento: null,
      subtitulo: p.marca || ''
    });

    const irADetalle = (id) => router.push(`/producto/${id}`);

    onMounted(cargar);

    return { items, nombresOferta, titulo, ruta, irADetalle };
  }
};
</script>
