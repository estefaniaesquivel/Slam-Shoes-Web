<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 container-fluid px-4 my-4">
      <h2 class="text-center text-uppercase fw-bold mb-2">{{ titulo }}</h2>

      <!-- Filtros por nombre de oferta (BUEN FIN, etc.) -->
      <div v-if="esOfertas && nombres.length > 0" class="d-flex flex-wrap justify-content-center gap-2 mb-4">
        <router-link to="/ofertas" class="badge rounded-pill text-decoration-none py-2 px-3" :class="!nombreActivo ? 'bg-light text-dark' : 'bg-secondary'">Todas</router-link>
        <router-link v-for="n in nombres" :key="n" :to="{ path: '/ofertas', query: { nombre: n } }"
          class="badge rounded-pill text-decoration-none py-2 px-3" :class="nombreActivo === n ? 'bg-light text-dark' : 'bg-danger'">
          {{ n }}
        </router-link>
      </div>

      <div v-if="cargando" class="text-center text-muted py-5"><p>Cargando...</p></div>
      <div v-else-if="items.length === 0" class="text-center text-muted py-5">
        <p>{{ esOfertas ? 'No hay ofertas vigentes por el momento.' : 'No hay novedades por el momento.' }}</p>
      </div>

      <div v-else class="row g-4">
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

    <footer class="border-top border-secondary bg-dark pt-4 pb-2 mt-5 w-100">
      <div class="container-fluid text-center px-4">
        <p class="mb-0 text-muted">[Texto xd sit seven]</p>
      </div>
    </footer>

    <CarritoComponent />
  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import ProductoCardSimple from '@/components/ProductoCardSimple.vue';

const API = 'http://localhost:8080/api';

export default {
  name: 'PromocionesView',
  components: { CarritoComponent, NavbarPrincipal, ProductoCardSimple },
  setup() {
    const route = useRoute();
    const router = useRouter();

    const items = ref([]);
    const nombres = ref([]);
    const cargando = ref(true);

    const esOfertas = computed(() => route.meta.tipo === 'ofertas');
    const nombreActivo = computed(() => (route.query.nombre ? String(route.query.nombre) : ''));
    const titulo = computed(() => {
      if (!esOfertas.value) return 'Novedades';
      return nombreActivo.value ? `Ofertas: ${nombreActivo.value}` : 'Ofertas';
    });

    const cargar = async () => {
      const ruta = route.fullPath;
      cargando.value = true;
      items.value = [];
      try {
        if (esOfertas.value) {
          const [res, resNombres] = await Promise.all([
            axios.get(`${API}/ofertas/productos`, { params: nombreActivo.value ? { nombre: nombreActivo.value } : {} }),
            axios.get(`${API}/ofertas/nombres`).catch(() => ({ data: [] }))
          ]);
          if (route.fullPath !== ruta) return;
          nombres.value = resNombres.data;
          items.value = res.data.map((o) => ({
            idProducto: o.producto.idProducto,
            nombre: o.producto.nombre,
            imagenUrl: o.producto.imagenUrl,
            precio: o.producto.precio,
            precioFinal: o.precioConDescuento,
            descuento: o.porcentajeDescuento,
            subtitulo: o.nombre || ''
          }));
        } else {
          const res = await axios.get(`${API}/novedades/productos`);
          if (route.fullPath !== ruta) return;
          items.value = res.data.map((p) => ({
            idProducto: p.idProducto,
            nombre: p.nombre,
            imagenUrl: p.imagenUrl,
            precio: p.precio,
            precioFinal: null,
            descuento: null,
            subtitulo: p.marca || ''
          }));
        }
      } catch (error) {
        console.error('Error al cargar el panel:', error);
      } finally {
        if (route.fullPath === ruta) {
          cargando.value = false;
          window.scrollTo({ top: 0 });
        }
      }
    };

    watch(() => route.fullPath, cargar, { immediate: true });

    const irADetalle = (id) => router.push(`/producto/${id}`);

    return { items, nombres, cargando, esOfertas, nombreActivo, titulo, irADetalle };
  }
};
</script>
