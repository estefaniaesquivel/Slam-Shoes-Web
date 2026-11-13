<template>
  <div>
    <!-- Banners (tabla banners) -->
    <BannerCarousel />

    <!-- Novedades y ofertas (bloques con portada) y tendencias (productos.es_tendencia) -->
    <PromoTiles />
    <SeccionProductos tipo="tendencias" />

    <!-- Marcas (tabla marcas) -->
    <MarcasGrid />

    <!-- Colecciones (tabla colecciones) -->
    <ColeccionesGrid />

    <!-- SECCIÓN: NUESTRO CATÁLOGO -->
    <div class="container-fluid px-4 my-5 pt-3">
        <h2 class="mb-4 text-center">NUESTRO CATÁLOGO</h2>
        
        <div v-if="productos.length === 0" class="text-center text-muted py-4">
            <p>Cargando productos de la base de datos o sin registros activos...</p>
        </div>

        <CatalogoFiltrable v-else :productos="productos" :buscador="false" :filtros="false" />
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
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import axios from 'axios';
import CatalogoFiltrable from '@/components/CatalogoFiltrable.vue';
import productoService from '@/services/productoService';
import BannerCarousel from '@/components/BannerCarousel.vue';
import PromoTiles from '@/components/PromoTiles.vue';
import SeccionProductos from '@/components/SeccionProductos.vue';
import MarcasGrid from '@/components/MarcasGrid.vue';
import ColeccionesGrid from '@/components/ColeccionesGrid.vue';

export default {
  name: 'HomeContenido',
  components: {
    CatalogoFiltrable, BannerCarousel, PromoTiles, SeccionProductos, MarcasGrid, ColeccionesGrid },
  props: {
    // En el inicio del administrador no se muestra el botón de favoritos
    modoAdmin: { type: Boolean, default: false }
  },
  setup() {
    const productos = ref([]);

    const cargarDatos = async () => {
      try {
        const respuestaProductos = await productoService.obtenerProductos();

        // Categorías reales de la BD (una sola petición) para mostrar el nombre en cada tarjeta
        const respuestaCategorias = await axios.get('http://localhost:8080/api/categorias').catch(() => ({ data: [] }));
        const nombresCategoria = {};
        respuestaCategorias.data.forEach(c => { nombresCategoria[c.idCategoria] = c.nombre; });

        productos.value = respuestaProductos.data.map(p => ({
          ...p,
          subtitulo: nombresCategoria[p.idCategoria] || ''
        }));
      } catch (error) {
        console.error("Error al cargar los datos de la API:", error);
      }
    };

    onMounted(() => {
      cargarDatos();
    });

    return { productos };
  }
};
</script>

