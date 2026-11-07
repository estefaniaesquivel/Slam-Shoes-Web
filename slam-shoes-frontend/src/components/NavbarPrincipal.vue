<template>
  <!-- Encabezado (Navbar) compartido por HomeView y PlayerView -->
  <nav class="navbar navbar-expand-lg navbar-dark bg-dark border-bottom border-secondary w-100 px-4">
    <div class="container-fluid px-0">
      <router-link class="navbar-brand" to="/">
        <img src="/logo-slamshoes-blanco.png" alt="Slam Shoes Logo" style="height: 45px; width: auto;" class="object-contain">
      </router-link>

      <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#menuPrincipal" aria-controls="menuPrincipal" aria-expanded="false" aria-label="Toggle navigation">
        <span class="navbar-toggler-icon"></span>
      </button>

      <div class="collapse navbar-collapse justify-content-center" id="menuPrincipal">
        <ul class="navbar-nav mb-2 mb-lg-0">
          <li class="nav-item"><a class="nav-link text-light" href="#">Novedades</a></li>

          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownMarcas" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              Marcas
            </a>
            <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownMarcas">
              <li><a class="dropdown-item" href="#">Nike</a></li>
              <li><a class="dropdown-item" href="#">Adidas</a></li>
              <li><a class="dropdown-item" href="#">Li-ning</a></li>
              <li><a class="dropdown-item" href="#">Under Armour</a></li>
              <li><a class="dropdown-item" href="#">ANTA</a></li>
              <li><hr class="dropdown-divider border-secondary"></li>
              <li><a class="dropdown-item" href="#">Ver todas las marcas</a></li>
            </ul>
          </li>

          <!-- Jugadores: solo los registrados en la tabla "jugadores" -->
          <li class="nav-item dropdown">
            <a class="nav-link dropdown-toggle text-light" href="#" id="dropdownPlayer" role="button" data-bs-toggle="dropdown" aria-expanded="false">
              Por Jugador
            </a>
            <ul class="dropdown-menu dropdown-menu-dark bg-dark border-secondary" aria-labelledby="dropdownPlayer">
              <li v-for="jugador in jugadores" :key="jugador.idJugador">
                <router-link class="dropdown-item" :to="`/jugador/${jugador.idJugador}`">
                  {{ jugador.nombre }}
                </router-link>
              </li>
              <li v-if="jugadores.length === 0">
                <span class="dropdown-item-text text-secondary small">
                  {{ cargandoJugadores ? 'Cargando jugadores...' : 'Sin jugadores registrados' }}
                </span>
              </li>
            </ul>
          </li>

          <li class="nav-item"><a class="nav-link text-light" href="#">Accesorios</a></li>
          <li class="nav-item"><a class="nav-link text-light" href="#">Ofertas</a></li>
        </ul>
      </div>

      <!-- Iconos de la derecha -->
      <div class="d-flex align-items-center gap-3">
        <a href="#" class="text-light fs-5"><i class="bi bi-search"></i></a>

        <!-- Icono de Favoritos con contador -->
        <a href="#" class="position-relative text-light text-decoration-none fs-5" @click.prevent="irAFavoritos" aria-label="Favoritos">
          <i class="bi bi-heart"></i>
          <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
            {{ totalItemsFavoritos }}
          </span>
        </a>

        <!-- Icono de Usuario -->
        <a href="#" class="text-light fs-5 text-decoration-none" @click.prevent="manejarClickUsuario" aria-label="Perfil de usuario">
          <i class="bi bi-person"></i>
        </a>

        <!-- Botón de Carrito Desplegable (el offcanvas lo aporta CarritoComponent en cada vista) -->
        <a href="#offcanvasCarrito" data-bs-toggle="offcanvas" class="position-relative text-light text-decoration-none fs-5" aria-label="Abrir carrito">
          <i class="bi bi-bag"></i>
          <span class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style="font-size: 0.6rem;">
            {{ totalItemsCarrito }}
          </span>
        </a>
      </div>
    </div>
  </nav>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import axios from 'axios';
import { verificarSesionYAccionar } from '@/utils/auth.js';

export default {
  name: 'NavbarPrincipal',
  setup() {
    const router = useRouter();

    const jugadores = ref([]);
    const cargandoJugadores = ref(true);
    const favoritos = ref(JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || []);
    const carrito = ref(JSON.parse(localStorage.getItem('slam_shoes_cart')) || []);

    const cargarJugadores = async () => {
      try {
        const respuesta = await axios.get('http://localhost:8080/api/jugadores');
        jugadores.value = respuesta.data;
      } catch (error) {
        console.error('Error al cargar los jugadores:', error);
      } finally {
        cargandoJugadores.value = false;
      }
    };

    // Los contadores se vuelven a leer cuando cualquier vista dispara el evento 'storage'
    const refrescarContadores = () => {
      favoritos.value = JSON.parse(localStorage.getItem('slam_shoes_favoritos')) || [];
      carrito.value = JSON.parse(localStorage.getItem('slam_shoes_cart')) || [];
    };

    const manejarClickUsuario = () => {
      const usuarioGuardado = localStorage.getItem('usuario');
      router.push(usuarioGuardado ? '/profile' : '/login');
    };

    const irAFavoritos = () => {
      const usuarioActivo = verificarSesionYAccionar();
      if (!usuarioActivo) return;
      router.push('/favoritos');
    };

    const totalItemsFavoritos = computed(() => favoritos.value.length);
    const totalItemsCarrito = computed(() =>
      carrito.value.reduce((suma, item) => suma + item.cantidad, 0)
    );

    onMounted(() => {
      cargarJugadores();
      window.addEventListener('storage', refrescarContadores);
    });

    onBeforeUnmount(() => {
      window.removeEventListener('storage', refrescarContadores);
    });

    return {
      jugadores,
      cargandoJugadores,
      manejarClickUsuario,
      irAFavoritos,
      totalItemsFavoritos,
      totalItemsCarrito
    };
  }
};
</script>
