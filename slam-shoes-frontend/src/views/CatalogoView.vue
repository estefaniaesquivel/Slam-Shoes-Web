<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <!-- Encabezado (Navbar) compartido -->
    <NavbarPrincipal />

    <div class="flex-grow-1">

      <!-- Cargando -->
      <div v-if="cargando" class="text-center text-muted py-5">
        <p>Cargando...</p>
      </div>

      <!-- Colección no encontrada -->
      <div v-else-if="!coleccion" class="text-center py-5">
        <h4 class="text-danger mb-3">No se encontró lo solicitado.</h4>
        <router-link to="/" class="btn btn-outline-light">Volver al inicio</router-link>
      </div>

      <template v-else>

        <!-- Encabezado de la colección: nombre y descripción (la imagen se ve completa en el Home) -->
        <div class="container-fluid px-4 my-4">
          <div class="text-center border border-secondary rounded bg-black py-5 px-4">
            <span class="badge bg-secondary text-uppercase mb-2">{{ etiquetaTipo }}</span>
            <h1 class="text-white fw-bold text-uppercase mb-2">{{ coleccion.nombre }}</h1>
            <p v-if="coleccion.descripcion" class="text-light opacity-75 mb-1">{{ coleccion.descripcion }}</p>
            <p class="text-secondary small mb-0">
              {{ productos.length }} {{ productos.length === 1 ? 'modelo disponible' : 'modelos disponibles' }}
            </p>
          </div>
        </div>

        <!-- Catálogo de tenis indexados a la colección -->
        <div class="container-fluid px-4 my-5 pt-3">
          <h2 class="mb-4 text-center text-uppercase">{{ coleccion.nombre }}</h2>

          <CatalogoFiltrable :productos="productos" vacio="Aún no hay tenis registrados aquí." />
        </div>

      </template>
    </div>

    <!-- Pie -->
    <footer class="border-top border-secondary bg-dark pt-4 pb-2 mt-5 w-100">
      <div class="container-fluid text-center px-4">
        <div class="row mb-3">
          <div class="col">
            <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 1]</a>
            <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 2]</a>
            <a href="#" class="mx-2 text-secondary text-decoration-none">[Enlace pie 3]</a>
          </div>
        </div>
        <p class="mb-0 text-muted">[Texto xd sit seven]</p>
      </div>
    </footer>

    <!-- Componente del Carrito Desplegable -->
    <CarritoComponent />

  </div>
</template>

<script>
import { ref, computed, watch } from 'vue';
import { useRoute } from 'vue-router';
import axios from 'axios';
import CatalogoFiltrable from '@/components/CatalogoFiltrable.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

export default {
  name: 'CatalogoView',
  components: {
    CatalogoFiltrable,
    CarritoComponent,
    NavbarPrincipal
  },
  setup() {
    const route = useRoute();

    const coleccion = ref(null); // marca o categoría que se está mostrando
    const tipo = computed(() => route.meta.tipo); // 'marca' | 'categoria'
    const etiquetaTipo = computed(() => (tipo.value === 'marca' ? 'Marca' : 'Categoría'));
    const productos = ref([]);
    const cargando = ref(true);

    const cargarColeccion = async (id) => {
      const ruta = route.fullPath;
      const base = `http://localhost:8080/api/${tipo.value === 'marca' ? 'marcas' : 'categorias'}`;
      cargando.value = true;
      coleccion.value = null;
      productos.value = [];

      try {
        const [resColeccion, resProductos] = await Promise.all([
          axios.get(`${base}/${id}`),
          axios.get(`${base}/${id}/productos`)
        ]);

        // Si el usuario ya cambió de colección mientras cargaba, se descarta esta respuesta
        if (route.fullPath !== ruta) return;

        coleccion.value = resColeccion.data;
        productos.value = resProductos.data;
      } catch (error) {
        console.error('Error al cargar la colección:', error);
      } finally {
        if (route.fullPath === ruta) {
          cargando.value = false;
          window.scrollTo({ top: 0 });
        }
      }
    };

    // Se vuelve a cargar cuando se elige otra marca/categoría desde el menú (misma vista, otro id)
    watch(
      () => route.fullPath,
      () => {
        if (route.params.id) cargarColeccion(route.params.id);
      },
      { immediate: true }
    );

    return {
      coleccion,
      etiquetaTipo,
      productos,
      cargando
    };
  }
};
</script>

