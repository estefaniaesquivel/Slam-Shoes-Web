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

        <!-- Encabezado de la colección: nombre y descripción (la imagen se ve completa en el Home) -->
        <div class="container-fluid px-4 my-4">
          <div class="text-center border border-secondary rounded bg-black py-5 px-4">
            <span class="badge bg-secondary text-uppercase mb-2">Colección</span>
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

          <div v-if="productos.length === 0" class="text-center text-muted py-4">
            <p>Aún no hay tenis en esta colección.</p>
          </div>

          <div class="row g-4" v-else>
            <div class="col-md-3" v-for="producto in productos" :key="producto.idProducto">
              <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative">

                <!-- Botón Favorito -->
                <BotonFavorito :producto="producto" />

                <!-- Contenedor cuadrado 1:1 -->
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

                <!-- Nombre, marca y precio -->
                <div class="text-start w-100">
                  <h6 class="text-white text-truncate mb-1" style="cursor: pointer;" @click="irADetalle(producto.idProducto)">
                    {{ producto.nombre }}
                  </h6>
                  <p class="text-secondary small mb-1">{{ producto.marca }}</p>
                  <p class="text-success fw-bold mb-0">${{ producto.precio }} MXN</p>
                </div>
              </div>
            </div>
          </div>
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
import { ref, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import axios from 'axios';
import BotonFavorito from '@/components/BotonFavorito.vue';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

export default {
  name: 'CollectionView',
  components: {
    BotonFavorito,
    CarritoComponent,
    NavbarPrincipal
  },
  setup() {
    const route = useRoute();
    const router = useRouter();

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
        productos.value = resProductos.data.map((p) => ({
          ...p,
          imagenActual: p.imagenUrl,
          segundaImagen: null
        }));
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

    return {
      coleccion,
      productos,
      cargando,
      cargarSegundaImagen,
      restaurarImagen,
      irADetalle
    };
  }
};
</script>

<style scoped>
/* Mismo estilo de tarjetas que el catálogo de HomeView */
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
