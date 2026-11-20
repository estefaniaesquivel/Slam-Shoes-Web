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

        <!-- Título y descripción, sin recuadros -->
        <div class="container-fluid px-4 mt-4">
          <h1 class="h2 text-white fw-bold text-uppercase mb-1">{{ coleccion.nombre }}</h1>
          <p v-if="coleccion.descripcion" class="text-secondary small mb-0">{{ coleccion.descripcion }}</p>
        </div>

        <!-- Catálogo de tenis (con buscador, filtros y conteo en existencia) -->
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
    const productos = ref([]);
    const cargando = ref(true);

    // "Ver todo": todo el catálogo activo, sin marca ni categoría
    const cargarTodo = async () => {
      const ruta = route.fullPath;
      cargando.value = true;
      productos.value = [];
      coleccion.value = { nombre: 'Ver todo' };
      try {
        const res = await axios.get('http://localhost:8080/api/productos');
        if (route.fullPath !== ruta) return;
        productos.value = res.data.filter((p) => p.activo !== false);
      } catch (error) {
        console.error('Error al cargar el catálogo:', error);
      } finally {
        if (route.fullPath === ruta) {
          cargando.value = false;
          window.scrollTo({ top: 0 });
        }
      }
    };

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
        if (tipo.value === 'todo') cargarTodo();
        else if (route.params.id) cargarColeccion(route.params.id);
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

