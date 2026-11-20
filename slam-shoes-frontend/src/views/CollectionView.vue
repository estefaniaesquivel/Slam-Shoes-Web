<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <!-- Encabezado (Navbar) compartido -->
    <NavbarPrincipal />

    <div class="flex-grow-1">

      <!-- Cargando -->
      <div v-if="cargando" class="text-center text-muted py-5">
        <p>Cargando colección...</p>
      </div>

      <!-- Colección no encontrada -->
      <div v-else-if="!coleccion" class="text-center py-5">
        <h4 class="text-danger mb-3">No se encontró la colección solicitada.</h4>
        <router-link to="/" class="btn btn-outline-light">Volver al inicio</router-link>
      </div>

      <template v-else>

        <!-- Título y descripción, sin recuadros -->
        <div class="container-fluid px-4 mt-4">
          <h1 class="h2 text-white fw-bold text-uppercase mb-1">{{ coleccion.nombre }}</h1>
          <p v-if="coleccion.descripcion" class="text-secondary small mb-0">{{ coleccion.descripcion }}</p>
        </div>

        <!-- Catálogo de tenis indexados a la colección -->
        <div class="container-fluid px-4 mt-4 mb-5">
          <CatalogoFiltrable :productos="productos" vacio="Aún no hay tenis registrados aquí." />
        </div>

      </template>
    </div>

    <!-- Componente del Carrito Desplegable -->
    <CarritoComponent />

  </div>
</template>

<script>
import { ref, watch } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import CatalogoFiltrable from '@/components/CatalogoFiltrable.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

export default {
  name: 'CollectionView',
  components: {
    CatalogoFiltrable,
    CarritoComponent,
    NavbarPrincipal
  },
  setup() {
    const route = useRoute();

    const coleccion = ref(null);
    const productos = ref([]);
    const cargando = ref(true);

    const cargarColeccion = async (id) => {
      cargando.value = true;
      coleccion.value = null;
      productos.value = [];

      try {
        const [resColeccion, resProductos] = await Promise.all([
          axios.get(`http://localhost:8080/api/colecciones/${id}`),
          axios.get(`http://localhost:8080/api/colecciones/${id}/productos`)
        ]);

        // Si el usuario ya cambió de colección mientras cargaba, se descarta esta respuesta
        if (String(route.params.id) !== String(id)) return;

        coleccion.value = resColeccion.data;
        productos.value = resProductos.data;
      } catch (error) {
        console.error('Error al cargar la colección:', error);
      } finally {
        if (String(route.params.id) === String(id)) {
          cargando.value = false;
          window.scrollTo({ top: 0 });
        }
      }
    };

    // Se vuelve a cargar cuando se elige otra colección desde el menú (misma vista, otro id)
    watch(
      () => route.params.id,
      (id) => {
        if (id) cargarColeccion(id);
      },
      { immediate: true }
    );

    return {
      coleccion,
      productos,
      cargando
    };
  }
};
</script>

