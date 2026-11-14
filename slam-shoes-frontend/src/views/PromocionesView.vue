<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">
    <NavbarPrincipal />

    <div class="flex-grow-1 container-fluid px-4 my-4">
      <h2 class="text-center text-uppercase fw-bold mb-4">{{ titulo }}</h2>

      <div v-if="cargando" class="text-center text-muted py-5"><p>Cargando...</p></div>

      <CatalogoFiltrable
        v-else
        :productos="items"
        :vacio="esOfertas ? 'No hay ofertas por el momento.' : 'No hay novedades por el momento.'"
      />
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
import { useRoute } from 'vue-router';
import axios from 'axios';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';
import CatalogoFiltrable from '@/components/CatalogoFiltrable.vue';

const API = 'http://localhost:8080/api';

export default {
  name: 'PromocionesView',
  components: { CarritoComponent, NavbarPrincipal, CatalogoFiltrable },
  setup() {
    const route = useRoute();

    const items = ref([]);
    const cargando = ref(true);

    const esOfertas = computed(() => route.meta.tipo === 'ofertas');
    // /ofertas?nombre=BUEN FIN muestra solo las ofertas con ese nombre
    const nombreOferta = computed(() => (esOfertas.value && route.query.nombre ? String(route.query.nombre) : ''));
    const titulo = computed(() => (esOfertas.value ? (nombreOferta.value ? `Ofertas: ${nombreOferta.value}` : 'Ofertas') : 'Novedades'));

    const cargar = async () => {
      const ruta = route.fullPath;
      cargando.value = true;
      items.value = [];
      try {
        if (esOfertas.value) {
          // TODAS las ofertas activas que no han terminado (vigentes y programadas)
          const res = await axios.get(`${API}/ofertas/productos`, { params: nombreOferta.value ? { nombre: nombreOferta.value } : {} });
          if (route.fullPath !== ruta) return;
          items.value = res.data.map((o) => o.producto);
        } else {
          const res = await axios.get(`${API}/novedades/productos`);
          if (route.fullPath !== ruta) return;
          items.value = res.data;
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

    return { items, cargando, esOfertas, titulo };
  }
};
</script>
