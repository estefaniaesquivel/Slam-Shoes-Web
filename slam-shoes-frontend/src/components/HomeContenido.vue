<template>
  <div>
    <!-- Banners (tabla banners) -->
    <BannerCarousel />

    <!-- Ofertas, novedades y tendencias (tablas ofertas, novedades y productos.es_tendencia) -->
    <SeccionProductos tipo="ofertas" />
    <SeccionProductos tipo="novedades" />
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

        <div class="row g-4" v-else>
            <div class="col-md-3" v-for="producto in productos" :key="producto.idProducto">
                <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative">
                    
                    <!-- Botón Favorito -->
                    <button v-if="!modoAdmin" class="btn btn-sm btn-dark position-absolute top-0 end-0 m-2 rounded-circle text-white shadow" @click="marcarFavorito(producto)" title="Añadir a favoritos" style="z-index: 3;">
                        <i class="bi bi-heart"></i>
                    </button>

                    <!-- Contenedor Cuadrado Perfecto 1:1 sin sobrantes negros -->
                    <div 
                        class="bg-white w-100 rounded-3 d-flex align-items-center justify-content-center overflow-hidden position-relative square-box mb-3" 
                        style="cursor: pointer;" 
                        @click="irADetalle(producto.idProducto)"
                        @mouseenter="cargarSegundaImagen(producto)"
                        @mouseleave="restaurarImagen(producto)"
                    >
                        <img 
                            :src="producto.imagenActual || producto.imagenUrl || '/logo-slamshoes-blanco.png'" 
                            class="img-fluid rounded catalog-img fade-transition" 
                            :alt="producto.nombre" 
                        />
                    </div>

                    <!-- Nombre del producto, Categoría real y Precio -->
                    <div class="text-start w-100">
                        <h6 class="text-white text-truncate mb-1" style="cursor: pointer;" @click="irADetalle(producto.idProducto)">
                            {{ producto.nombre }}
                        </h6>
                        <p class="text-secondary small mb-1">{{ producto.categoriaNombre }}</p>
                        <p class="text-success fw-bold mb-0">${{ producto.precio }} MXN</p>
                    </div>
                </div>
            </div>
        </div>
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
import { useRouter } from 'vue-router';
import axios from 'axios';
import productoService from '@/services/productoService';
import { verificarSesionYAccionar } from '@/utils/auth.js';
import BannerCarousel from '@/components/BannerCarousel.vue';
import SeccionProductos from '@/components/SeccionProductos.vue';
import MarcasGrid from '@/components/MarcasGrid.vue';
import ColeccionesGrid from '@/components/ColeccionesGrid.vue';

export default {
  name: 'HomeContenido',
  components: { BannerCarousel, SeccionProductos, MarcasGrid, ColeccionesGrid },
  props: {
    // En el inicio del administrador no se muestra el botón de favoritos
    modoAdmin: { type: Boolean, default: false }
  },
  setup() {
    const router = useRouter();
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
          categoriaNombre: nombresCategoria[p.idCategoria] || '',
          imagenActual: p.imagenUrl,
          segundaImagen: null
        }));
      } catch (error) {
        console.error("Error al cargar los datos de la API:", error);
      }
    };

    const cargarSegundaImagen = async (producto) => {
      if (producto.segundaImagen) {
        producto.imagenActual = producto.segundaImagen;
        return;
      }
      try {
        const resImgs = await axios.get(`http://localhost:8080/api/producto-imagenes/producto/${producto.idProducto}`);
        const imgOrden2 = resImgs.data.find(img => img.orden === 2);
        if (imgOrden2) {
          producto.segundaImagen = imgOrden2.urlImagen;
          producto.imagenActual = producto.segundaImagen;
        } else {
          producto.segundaImagen = producto.imagenUrl;
        }
      } catch (error) {
        producto.segundaImagen = producto.imagenUrl;
      }
    };

    const restaurarImagen = (producto) => {
      producto.imagenActual = producto.imagenUrl;
    };

    const irADetalle = (id) => {
      router.push(`/producto/${id}`);
    };

    const marcarFavorito = (producto) => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;

      let favs = JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || [];
      const existe = favs.some(p => p.idProducto === producto.idProducto);

      if (!existe) {
        favs.push(producto);
        localStorage.setItem('slam_shoes_favoritos', JSON.stringify(favs));
        // Avisa al encabezado para que actualice el contador de favoritos
        window.dispatchEvent(new Event('storage'));
        alert(`¡"${producto.nombre}" fue agregado a tus favoritos con éxito!`);
      } else {
        alert(`Este producto ya se encuentra en tus favoritos.`);
      }
    };

    onMounted(() => {
      cargarDatos();
    });

    return {
      productos,
      irADetalle,
      cargarSegundaImagen,
      restaurarImagen,
      marcarFavorito
    };
  }
};
</script>

<style scoped>
.square-box {
  width: 100%;
  aspect-ratio: 1 / 1;
}
.catalog-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.fade-transition {
  transition: opacity 0.4s ease-in-out;
}
</style>
