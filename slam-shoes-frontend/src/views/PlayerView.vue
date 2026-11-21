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
        <div v-if="jugador.imagenUrl && !bannerFallido" class="player-banner position-relative w-100 bg-black">
          <img
            :src="jugador.imagenUrl"
            :alt="'Portada de ' + jugador.nombre"
            class="player-banner-img d-block w-100"
            @error="bannerFallido = true"
          />
          <div class="player-banner-shade position-absolute top-0 start-0 w-100 h-100"></div>
        </div>

        <!-- Sin portada: solo el nombre, sin recuadro vacío -->
        <div v-else class="container-fluid px-4 mt-4">
          <h1 class="h2 text-white fw-bold text-uppercase mb-0">{{ jugador.nombre }}</h1>
        </div>

        <!-- Catálogo de tenis indexados al jugador -->
        <div class="container-fluid px-4 mt-4 mb-5">
          <CatalogoFiltrable :productos="productos" vacio="Aún no hay tenis registrados para este jugador." />
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
  name: 'PlayerView',
  components: {
    CatalogoFiltrable,
    CarritoComponent,
    NavbarPrincipal
  },
  setup() {
    const route = useRoute();

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
        productos.value = resProductos.data;
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

    return {
      jugador,
      productos,
      cargando,
      bannerFallido
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

</style>
