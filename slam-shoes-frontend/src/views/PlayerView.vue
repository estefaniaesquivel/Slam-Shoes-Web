<template>
  <div class="bg-dark text-light min-vh-100 w-100 overflow-x-hidden d-flex flex-column">

    <!-- Encabezado (Navbar) compartido -->
    <NavbarPrincipal />

    <div class="flex-grow-1">

      <!-- Cargando -->
      <div v-if="cargando" class="text-center text-muted py-5">
        <p>Cargando jugador...</p>
      </div>

      <!-- Jugador no encontrado -->
      <div v-else-if="!jugador" class="text-center py-5">
        <h4 class="text-danger mb-3">No se encontró al jugador solicitado.</h4>
        <router-link to="/" class="btn btn-outline-light">Volver al inicio</router-link>
      </div>

      <template v-else>

        <!-- Portada del jugador (jugadores.imagen_url): pegada al encabezado, a todo el ancho, imagen completa 3:1 sin recortar -->
        <div class="player-banner position-relative w-100 bg-black">
          <img
            v-if="jugador.imagenUrl && !bannerFallido"
            :src="jugador.imagenUrl"
            :alt="'Portada de ' + jugador.nombre"
            class="player-banner-img d-block w-100"
            @error="bannerFallido = true"
          />
          <div class="player-banner-shade position-absolute top-0 start-0 w-100 h-100"></div>
        </div>

        <!-- Catálogo de tenis indexados al jugador -->
        <div class="container-fluid px-4 my-5 pt-3">
          <div v-if="productos.length === 0" class="text-center text-muted py-4">
            <p>Aún no hay tenis registrados para este jugador.</p>
          </div>

          <div class="row g-4" v-else>
            <div class="col-md-3" v-for="producto in productos" :key="producto.idProducto">
              <div class="border border-secondary bg-black p-3 rounded-3 text-center d-flex flex-column align-items-center justify-content-between position-relative">

                <!-- Botón Favorito -->
                <button class="btn btn-sm btn-dark position-absolute top-0 end-0 m-2 rounded-circle text-white shadow" @click="marcarFavorito(producto)" title="Añadir a favoritos" style="z-index: 3;">
                  <i class="bi bi-heart"></i>
                </button>

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
import { verificarSesionYAccionar } from '@/utils/auth.js';
import CarritoComponent from '@/components/CarritoComponent.vue';
import NavbarPrincipal from '@/components/NavbarPrincipal.vue';

export default {
  name: 'PlayerView',
  components: {
    CarritoComponent,
    NavbarPrincipal
  },
  setup() {
    const route = useRoute();
    const router = useRouter();

    const jugador = ref(null);
    const productos = ref([]);
    const cargando = ref(true);
    const bannerFallido = ref(false);

    const cargarJugador = async (id) => {
      cargando.value = true;
      bannerFallido.value = false;
      jugador.value = null;
      productos.value = [];

      try {
        const [resJugador, resProductos] = await Promise.all([
          axios.get(`http://localhost:8080/api/jugadores/${id}`),
          axios.get(`http://localhost:8080/api/jugadores/${id}/productos`)
        ]);

        // Si el usuario ya cambió de jugador mientras cargaba, se descarta esta respuesta
        if (String(route.params.id) !== String(id)) return;

        jugador.value = resJugador.data;
        productos.value = resProductos.data.map((p) => ({
          ...p,
          imagenActual: p.imagenUrl,
          segundaImagen: null
        }));
      } catch (error) {
        console.error('Error al cargar la página del jugador:', error);
      } finally {
        if (String(route.params.id) === String(id)) {
          cargando.value = false;
          window.scrollTo({ top: 0 });
        }
      }
    };

    // Se vuelve a cargar cuando se elige otro jugador desde el menú (misma vista, otro id)
    watch(
      () => route.params.id,
      (id) => {
        if (id) cargarJugador(id);
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
        alert('Este producto ya se encuentra en tus favoritos.');
      }
    };

    return {
      jugador,
      productos,
      cargando,
      bannerFallido,
      cargarSegundaImagen,
      restaurarImagen,
      irADetalle,
      marcarFavorito
    };
  }
};
</script>

<style scoped>
/*
  Portada: ocupa todo el ancho, pegada al encabezado (sin márgenes ni bordes).
  La proporción 3:1 (2400x800) se respeta con aspect-ratio y la imagen se muestra
  completa, sin recortes. Si falla la carga, queda un bloque oscuro de la misma proporción.
*/
.player-banner {
  aspect-ratio: 3 / 1;
}
.player-banner-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.player-banner-shade {
  pointer-events: none;
  background: linear-gradient(to top, rgba(0, 0, 0, 0.85) 0%, rgba(0, 0, 0, 0.35) 45%, rgba(0, 0, 0, 0) 75%);
}

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
